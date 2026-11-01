package kr.karanda.karandaserver.service

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import kr.karanda.karandaserver.dto.BroadcastMessage
import kr.karanda.karandaserver.dto.ReportHubPolicyDTO
import kr.karanda.karandaserver.dto.ReportHubReportListDTO
import kr.karanda.karandaserver.dto.ReportHubReportRequest
import kr.karanda.karandaserver.dto.ReportHubReportResultDTO
import kr.karanda.karandaserver.dto.ReportHubStatusDTO
import kr.karanda.karandaserver.dto.ReportTargetPolicyDTO
import kr.karanda.karandaserver.dto.ReporterProfileDTO
import kr.karanda.karandaserver.dto.ReporterSummaryDTO
import kr.karanda.karandaserver.entity.ReportHubReporter
import kr.karanda.karandaserver.entity.User
import kr.karanda.karandaserver.enums.BDORegion
import kr.karanda.karandaserver.enums.ReportMechanism
import kr.karanda.karandaserver.enums.ReportTarget
import kr.karanda.karandaserver.exception.InvalidArgumentException
import kr.karanda.karandaserver.exception.UnknownUserException
import kr.karanda.karandaserver.infrastructure.redis.RedisPublisher
import kr.karanda.karandaserver.repository.jpa.ReportHubChannelStatusRepository
import kr.karanda.karandaserver.repository.jpa.ReportHubMaintenanceRepository
import kr.karanda.karandaserver.repository.jpa.ReportHubReportRepository
import kr.karanda.karandaserver.repository.jpa.ReportHubReporterRepository
import kr.karanda.karandaserver.repository.jpa.ReportHubSessionRepository
import kr.karanda.karandaserver.repository.jpa.UserRepository
import kr.karanda.karandaserver.service.reporthub.ReportHubAlertService
import kr.karanda.karandaserver.service.reporthub.ReportHubRateLimiter
import kr.karanda.karandaserver.service.reporthub.ReportHubRegionConfig
import kr.karanda.karandaserver.service.reporthub.ReportHubRegions
import kr.karanda.karandaserver.service.reporthub.ReportHubRules
import kr.karanda.karandaserver.service.reporthub.ReportHubStateWriter
import kr.karanda.karandaserver.util.SingleFlightExecutor
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.support.TransactionTemplate
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Reporter Hub: 사용자 제보로 보스·버프의 채널별 현황을 공유한다.
 *
 * 조회는 캐시하고, 쓰기는 [ReportHubStateWriter]가 트랜잭션 안에서 상태를 바꾼 뒤
 * 커밋 후에 델타 발행·캐시 무효화·알림 예약을 한다.
 */
@Service
class ReportHubService(
    private val userRepository: UserRepository,
    private val reportRepository: ReportHubReportRepository,
    private val sessionRepository: ReportHubSessionRepository,
    private val channelStatusRepository: ReportHubChannelStatusRepository,
    private val maintenanceRepository: ReportHubMaintenanceRepository,
    private val reporterRepository: ReportHubReporterRepository,
    private val stateWriter: ReportHubStateWriter,
    private val rateLimiter: ReportHubRateLimiter,
    private val alertService: ReportHubAlertService,
    private val redisPublisher: RedisPublisher,
    private val singleFlightExecutor: SingleFlightExecutor,
    transactionManager: PlatformTransactionManager,
) {
    private val logger: Logger = LoggerFactory.getLogger(ReportHubService::class.java)

    private val readOnly = TransactionTemplate(transactionManager).apply { isReadOnly = true }
    private val newTransaction = TransactionTemplate(transactionManager).apply {
        propagationBehavior = TransactionDefinition.PROPAGATION_REQUIRES_NEW
    }

    /**
     * 지역 스냅샷 캐시. 자기 인스턴스의 쓰기에는 즉시 무효화되고, 다른 인스턴스의 쓰기는 TTL만큼 늦을 수 있다.
     * 클라이언트가 WS 델타와 행별 version으로 병합하므로 짧은 지연은 문제되지 않는다.
     */
    private val statusCache: Cache<BDORegion, ReportHubStatusDTO> = Caffeine.newBuilder()
        .expireAfterWrite(STATUS_CACHE_TTL)
        .maximumSize(BDORegion.entries.size.toLong())
        .build()

    private val leaderboardCache: Cache<BDORegion, List<ReporterSummaryDTO>> = Caffeine.newBuilder()
        .expireAfterWrite(LEADERBOARD_CACHE_TTL)
        .maximumSize(BDORegion.entries.size.toLong())
        .build()

    // ---------------------------------------------------------------- 조회

    fun getEnabledRegions(): List<BDORegion> = ReportHubRegions.enabledRegions

    /** 지역 정책. 비활성 지역이면 `enabled = false`와 빈 목록을 돌려준다(클라이언트의 "준비 중" 화면). */
    fun getPolicy(region: BDORegion): ReportHubPolicyDTO {
        val config = ReportHubRegions.enabled(region) ?: return ReportHubPolicyDTO(
            region = region,
            enabled = false,
            timezone = region.timezone.id,
            utcOffsetMinutes = utcOffsetMinutes(region.timezone),
            regularMaintenanceDay = null,
            channels = emptyList(),
            targets = emptyList(),
            verifiedThreshold = ReportHubRules.VERIFIED_THRESHOLD,
        )
        return ReportHubPolicyDTO(
            region = region,
            enabled = true,
            timezone = config.timezone.id,
            utcOffsetMinutes = utcOffsetMinutes(config.timezone),
            regularMaintenanceDay = config.regularMaintenanceDay?.name,
            channels = config.channels,
            targets = config.targets.map {
                ReportTargetPolicyDTO(
                    target = it.target,
                    mechanism = it.mechanism,
                    kinds = it.mechanism.kinds.toList(),
                    respawnMinMinutes = it.respawnMin?.toMinutes(),
                    respawnMaxMinutes = it.respawnMax?.toMinutes(),
                    respawnDelayMinutes = it.respawnDelay?.toMinutes(),
                    lifetimeMinutes = it.lifetime?.toMinutes(),
                )
            },
            verifiedThreshold = ReportHubRules.VERIFIED_THRESHOLD,
        )
    }

    fun getStatus(region: BDORegion): ReportHubStatusDTO {
        val config = requireConfig(region)
        statusCache.getIfPresent(region)?.let { return it }
        return singleFlightExecutor.execute("report-hub:status:${region.name}") {
            statusCache.getIfPresent(region) ?: buildSnapshot(config).also { statusCache.put(region, it) }
        }
    }

    private fun buildSnapshot(config: ReportHubRegionConfig): ReportHubStatusDTO = readOnly.execute {
        val region = config.region
        val sessions = config.targets
            .filter { it.mechanism == ReportMechanism.SHARED_SCHEDULE }
            .mapNotNull { sessionRepository.findFirstByRegionAndTargetAndCanceledFalseOrderByAnchorAtDescIdDesc(region, it.target) }
        val channels = channelStatusRepository.findAllByRegion(region)
            .filter { config.policyOf(it.target) != null && config.hasChannel(it.channel) }
        val maintenances = maintenanceRepository.findTop2ByRegionAndCanceledFalseOrderByIdDesc(region)
        val reporterIds = sessions.map { it.reporterId } + channels.mapNotNull { it.reporterId }
        ReportHubStatusDTO(
            region = region,
            serverTime = now(),
            sessions = sessions.map { it.toDTO() },
            channels = channels.map { it.toDTO() },
            maintenances = maintenances.map { it.toDTO() },
            reporters = loadReporters(region, reporterIds),
        )
    }!!

    /** 특정 대상·채널의 최근 제보(상세 시트용) */
    fun getReports(region: BDORegion, target: ReportTarget, channel: String): ReportHubReportListDTO {
        val config = requireConfig(region)
        if (config.policyOf(target) == null || !config.hasChannel(channel)) throw InvalidArgumentException()
        return readOnly.execute {
            val reports = reportRepository.findTop20ByRegionAndTargetAndChannelOrderByReportedAtDesc(region, target, channel)
            ReportHubReportListDTO(
                reports = reports.map { it.toDTO() },
                reporters = loadReporters(region, reports.map { it.reporterId }),
            )
        }!!
    }

    /** 지역 리더보드: 최근 7일 유효 제보 수 순 */
    fun getReporters(region: BDORegion): List<ReporterSummaryDTO> {
        requireConfig(region)
        leaderboardCache.getIfPresent(region)?.let { return it }
        return singleFlightExecutor.execute("report-hub:reporters:${region.name}") {
            leaderboardCache.getIfPresent(region) ?: buildLeaderboard(region).also { leaderboardCache.put(region, it) }
        }
    }

    private fun buildLeaderboard(region: BDORegion): List<ReporterSummaryDTO> = readOnly.execute {
        val counts = reportRepository.countRecentByReporter(
            region, now().minus(LEADERBOARD_PERIOD), PageRequest.of(0, LEADERBOARD_SIZE),
        )
        val reporters = reporterRepository.findAllByRegionAndUserIdIn(region, counts.map { it.reporterId })
            .associateBy { it.userId }
        // 탈퇴한 사용자는 통계 행이 없으므로 빠진다
        counts.mapNotNull { reporters[it.reporterId]?.toSummaryDTO(recentReportCount = it.reportCount) }
    }!!

    fun getReporterProfile(region: BDORegion, reporterId: Long): ReporterProfileDTO {
        requireConfig(region)
        return readOnly.execute {
            val reporter = reporterRepository.findByUserIdAndRegion(reporterId, region) ?: throw InvalidArgumentException()
            ReporterProfileDTO(
                reporter = reporter.toSummaryDTO(),
                recentReports = reportRepository.findTop20ByRegionAndReporterIdOrderByReportedAtDesc(region, reporterId)
                    .map { it.toDTO() },
            )
        }!!
    }

    // ---------------------------------------------------------------- 쓰기

    fun addReport(userUUID: String, request: ReportHubReportRequest): ReportHubReportResultDTO {
        val user = userRepository.findByUserUUID(userUUID) ?: throw UnknownUserException()
        val now = now()
        val config = ReportHubRegions.enabled(request.region)
        val validated = ReportHubRules.validate(
            config, request.target, request.channel, request.kind, request.eventAt, request.remainingMinutes, now,
        )
        rateLimiter.acquire(request.region, user.id!!, validated.target, validated.channel, validated.kind)

        val outcome = stateWriter.apply(config!!, validated, user.id!!, now)
        recordReporter(user, config.region, now)
        val delta = toDelta(config.region, outcome)
        afterWrite(config.region, delta, outcome)
        return ReportHubReportResultDTO(report = outcome.report.toDTO(), status = delta)
    }

    /** 본인 제보를 제보 후 [CANCEL_WINDOW] 안에 취소한다. 취소 후 상태 델타를 돌려준다. */
    fun cancelReport(userUUID: String, reportId: Long, reason: String): ReportHubStatusDTO {
        val user = userRepository.findByUserUUID(userUUID) ?: throw UnknownUserException()
        val now = now()
        val report = reportRepository.findById(reportId).orElse(null) ?: throw InvalidArgumentException()
        if (report.reporterId != user.id || report.canceled) throw InvalidArgumentException()
        if (Duration.between(report.reportedAt.toInstant(), now.toInstant()) > CANCEL_WINDOW) throw InvalidArgumentException()
        val config = ReportHubRegions.enabled(report.region) ?: throw InvalidArgumentException()

        val outcome = stateWriter.cancel(config, reportId, report.target, reason, now)
        val delta = toDelta(config.region, outcome)
        afterWrite(config.region, delta, outcome)
        return delta
    }

    private fun toDelta(region: BDORegion, outcome: ReportHubStateWriter.Outcome): ReportHubStatusDTO {
        val reporterIds = listOf(outcome.report.reporterId) +
                outcome.sessions.map { it.reporterId } +
                outcome.channels.mapNotNull { it.reporterId }
        return ReportHubStatusDTO(
            region = region,
            serverTime = now(),
            sessions = outcome.sessions.map { it.toDTO() },
            channels = outcome.channels.map { it.toDTO() },
            maintenances = outcome.maintenances.map { it.toDTO() },
            reporters = readOnly.execute { loadReporters(region, reporterIds) }!!,
        )
    }

    private fun afterWrite(region: BDORegion, delta: ReportHubStatusDTO, outcome: ReportHubStateWriter.Outcome) {
        statusCache.invalidate(region)
        if (delta.sessions.isNotEmpty() || delta.channels.isNotEmpty() || delta.maintenances.isNotEmpty()) {
            redisPublisher.broadcast(BroadcastMessage.reportHubStatus(region, delta))
        }
        alertService.schedule(outcome.alerts)
    }

    /**
     * 제보자 통계 갱신. 상태 반영과 분리된 트랜잭션이다 — 통계 실패로 제보 자체가 실패하면 안 된다.
     * 첫 제보의 행 생성이 동시에 일어나면 한 번 더 갱신을 시도한다.
     */
    private fun recordReporter(user: User, region: BDORegion, now: ZonedDateTime) {
        val name = displayName(user, region)
        val avatar = user.toUserDTO().avatar
        try {
            newTransaction.executeWithoutResult {
                if (reporterRepository.recordReport(user.id!!, region, now, name, avatar) == 0) {
                    reporterRepository.saveAndFlush(
                        ReportHubReporter(
                            userId = user.id!!,
                            region = region,
                            displayName = name,
                            avatarUrl = avatar,
                            reportCount = 1,
                            lastReportedAt = now,
                        )
                    )
                }
            }
        } catch (_: DataIntegrityViolationException) {
            newTransaction.executeWithoutResult { reporterRepository.recordReport(user.id!!, region, now, name, avatar) }
        } catch (e: Exception) {
            logger.warn("Failed to update report hub reporter stats (user: ${user.id}, region: ${region.name}): ${e.message}")
        }
    }

    /** 가문이 인증되어 있고 제보 지역과 같으면 가문명, 아니면 디스코드 이름 */
    private fun displayName(user: User, region: BDORegion): String =
        user.bdoFamily?.takeIf { it.verified && it.region == region.name }?.familyName ?: user.userName

    private fun loadReporters(region: BDORegion, reporterIds: Collection<Long>): List<ReporterSummaryDTO> {
        val ids = reporterIds.toSet()
        if (ids.isEmpty()) return emptyList()
        return reporterRepository.findAllByRegionAndUserIdIn(region, ids).map { it.toSummaryDTO() }
    }

    private fun requireConfig(region: BDORegion): ReportHubRegionConfig =
        ReportHubRegions.enabled(region) ?: throw InvalidArgumentException()

    private fun now(): ZonedDateTime = ZonedDateTime.now(ZoneId.of("UTC"))

    private fun utcOffsetMinutes(zone: ZoneId): Int = zone.rules.getOffset(Instant.now()).totalSeconds / 60

    companion object {
        private val STATUS_CACHE_TTL: Duration = Duration.ofSeconds(5)
        private val LEADERBOARD_CACHE_TTL: Duration = Duration.ofMinutes(10)
        private val LEADERBOARD_PERIOD: Duration = Duration.ofDays(7)
        private const val LEADERBOARD_SIZE = 50
        private val CANCEL_WINDOW: Duration = Duration.ofMinutes(10)
    }
}
