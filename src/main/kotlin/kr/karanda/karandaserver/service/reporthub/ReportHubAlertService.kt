package kr.karanda.karandaserver.service.reporthub

import jakarta.annotation.PreDestroy
import kr.karanda.karandaserver.dto.BroadcastMessage
import kr.karanda.karandaserver.dto.ReportHubAlertDTO
import kr.karanda.karandaserver.enums.BDORegion
import kr.karanda.karandaserver.enums.ReportHubAlertReason
import kr.karanda.karandaserver.infrastructure.redis.RedisPublisher
import kr.karanda.karandaserver.repository.jpa.ReportHubChannelStatusRepository
import kr.karanda.karandaserver.repository.jpa.ReportHubMaintenanceRepository
import kr.karanda.karandaserver.repository.jpa.ReportHubRelationRepository
import kr.karanda.karandaserver.repository.jpa.ReportHubRelationRepository.AlertRecipient
import kr.karanda.karandaserver.repository.jpa.ReportHubReportRepository
import kr.karanda.karandaserver.repository.jpa.ReportHubReporterRepository
import kr.karanda.karandaserver.repository.jpa.ReportHubSessionRepository
import kr.karanda.karandaserver.service.FcmSender
import kr.karanda.karandaserver.service.FcmService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.data.redis.connection.RedisConnection
import org.springframework.data.redis.core.RedisCallback
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Reporter Hub 알림 파이프라인.
 *
 * 커밋 후 받은 후보를 [DELAY] 뒤에 처리한다. 그 사이 제보가 취소되거나 상태가 바뀌었으면 버린다
 * (제보 직후 스낵바 취소로 잘못된 푸시가 나가지 않게). 예약은 인스턴스 메모리에만 있으므로
 * 인스턴스가 재시작되면 유실되는데, 이는 감수한다.
 *
 * - Windows: 지역 WS 토픽으로 이벤트를 브로드캐스트하고 클라이언트가 팔로우·설정으로 거른다.
 * - Web/Android: 서버가 수신자를 골라 FCM 데이터 메시지를 보낸다(문구는 기기에서 사용자 언어로 만든다).
 */
@Service
class ReportHubAlertService(
    private val reportRepository: ReportHubReportRepository,
    private val sessionRepository: ReportHubSessionRepository,
    private val channelStatusRepository: ReportHubChannelStatusRepository,
    private val maintenanceRepository: ReportHubMaintenanceRepository,
    private val relationRepository: ReportHubRelationRepository,
    private val reporterRepository: ReportHubReporterRepository,
    private val redisTemplate: StringRedisTemplate,
    private val redisPublisher: RedisPublisher,
    private val fcmSender: FcmSender,
    private val redisKeys: ReportHubRedisKeys,
) {
    private val logger: Logger = LoggerFactory.getLogger(ReportHubAlertService::class.java)

    private val executor = Executors.newSingleThreadScheduledExecutor { runnable ->
        Thread(runnable, "report-hub-alert").apply { isDaemon = true }
    }

    fun schedule(candidates: List<ReportHubAlertCandidate>) {
        for (candidate in candidates) {
            executor.schedule({ process(candidate) }, DELAY.toMillis(), TimeUnit.MILLISECONDS)
        }
    }

    @PreDestroy
    fun shutdown() {
        executor.shutdownNow()
    }

    private fun process(candidate: ReportHubAlertCandidate) {
        try {
            val report = reportRepository.findById(candidate.triggerReportId).orElse(null) ?: return
            if (report.canceled || !isStillValid(candidate)) return
            if (candidate.target != null && isMaintenanceInProgress(candidate.region)) return

            val reporterName = reporterRepository.findByUserIdAndRegion(candidate.reporterId, candidate.region)?.displayName
            val reasons = when {
                candidate.target == null -> listOf(ReportHubAlertReason.MAINTENANCE)
                candidate.reachedVerified -> listOf(ReportHubAlertReason.FOLLOWED, ReportHubAlertReason.VERIFIED)
                else -> listOf(ReportHubAlertReason.FOLLOWED)
            }
            for (reason in reasons) {
                val alert = ReportHubAlertDTO(
                    type = ReportHubAlertDTO.TYPE,
                    eventKey = candidate.eventKey,
                    region = candidate.region,
                    target = candidate.target,
                    channel = candidate.channel,
                    kind = candidate.kind,
                    eventAt = candidate.eventAt,
                    reason = reason,
                    reporterId = candidate.reporterId,
                    reporterName = reporterName,
                )
                redisPublisher.broadcast(BroadcastMessage.reportHubAlert(candidate.region, alert))
                sendFcm(alert, recipients(candidate, reason))
            }
        } catch (e: Exception) {
            logger.warn("Failed to process report hub alert (${candidate.eventKey}): ${e.message}")
        }
    }

    /** 발송 시점에도 이 이벤트가 유효한지 (취소·정정으로 바뀌지 않았는지) */
    private fun isStillValid(candidate: ReportHubAlertCandidate): Boolean {
        val parts = candidate.eventKey.split(":")
        val id = parts.getOrNull(1)?.toLongOrNull() ?: return false
        return when (parts.first()) {
            "S" -> sessionRepository.findById(id).orElse(null)?.canceled == false
            "C" -> channelStatusRepository.findById(id).orElse(null)?.let {
                it.kind == candidate.kind && it.eventAt?.toEpochSecond() == parts.getOrNull(2)?.toLongOrNull()
            } ?: false

            "M" -> maintenanceRepository.findById(id).orElse(null)?.let {
                !it.canceled && it.confirmed && it.endedAt != null
            } ?: false

            else -> false
        }
    }

    /** 확정된 점검이 진행 중(시작만 있고 종료가 없음)이면 보스·버프 알림을 보내지 않는다 */
    private fun isMaintenanceInProgress(region: BDORegion): Boolean {
        val now = ZonedDateTime.now(ZoneId.of("UTC"))
        return maintenanceRepository.findTop2ByRegionAndCanceledFalseOrderByIdDesc(region).any {
            it.confirmed && it.endedAt == null && it.startedAt?.isAfter(now.minus(ReportHubRules.MAINTENANCE_LOOKBACK)) == true
        }
    }

    private fun recipients(candidate: ReportHubAlertCandidate, reason: ReportHubAlertReason): List<AlertRecipient> {
        val regionName = candidate.region.name
        val code = candidate.target?.name ?: FcmService.REPORT_HUB_MAINTENANCE
        val all = when (reason) {
            ReportHubAlertReason.FOLLOWED -> relationRepository.findFollowerTokens(candidate.reporterId, candidate.region, regionName)
            ReportHubAlertReason.VERIFIED -> relationRepository.findVerifiedTokens(candidate.reporterId, candidate.region, regionName)
            ReportHubAlertReason.MAINTENANCE -> relationRepository.findRegionTokens(regionName)
        }
        val selected = all.filter { code in it.targets.split(",") && it.userId != candidate.reporterId }
        val newUsers = markAlerted(candidate.eventKey, selected.map { it.userId }.distinct())
        return selected.filter { it.userId in newUsers }
    }

    /**
     * 같은 사람이 같은 이벤트를 두 번 받지 않도록 이벤트별 수신자 집합에 추가하고,
     * 이번에 새로 추가된 사용자만 돌려준다. 파이프라인으로 한 번에 보낸다.
     */
    private fun markAlerted(eventKey: String, userIds: List<Long>): Set<Long> {
        if (userIds.isEmpty()) return emptySet()
        val key = "${redisKeys.prefix}:alerted:$eventKey"
        val results = redisTemplate.executePipelined(RedisCallback<Any?> { connection: RedisConnection ->
            val keyBytes = key.toByteArray()
            for (userId in userIds) {
                connection.setCommands().sAdd(keyBytes, userId.toString().toByteArray())
            }
            null
        })
        redisTemplate.expire(key, ALERTED_TTL)
        return userIds.filterIndexed { index, _ -> (results.getOrNull(index) as? Long ?: 0L) > 0L }.toSet()
    }

    private fun sendFcm(alert: ReportHubAlertDTO, recipients: List<AlertRecipient>) {
        if (recipients.isEmpty()) return
        val data = mapOf(
            "type" to alert.type,
            "eventKey" to alert.eventKey,
            "region" to alert.region.name,
            "target" to (alert.target?.name ?: ""),
            "channel" to (alert.channel ?: ""),
            "kind" to alert.kind.name,
            "eventAt" to alert.eventAt.toInstant().toString(),
            "reason" to alert.reason.name,
            "reporterId" to alert.reporterId.toString(),
            "reporterName" to (alert.reporterName ?: ""),
        )
        fcmSender.sendData(recipients.map { it.token }, data, FCM_TTL, "report-hub ${alert.eventKey}")
    }

    companion object {
        /** 제보 후 이만큼 기다렸다 보낸다(스낵바 취소 5초 + 여유) */
        private val DELAY: Duration = Duration.ofSeconds(20)

        /** 오래 오프라인이던 기기가 지난 알림을 받지 않도록 */
        private val FCM_TTL: Duration = Duration.ofMinutes(30)

        private val ALERTED_TTL: Duration = Duration.ofHours(2)
    }
}
