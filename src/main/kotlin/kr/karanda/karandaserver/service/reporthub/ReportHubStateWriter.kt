package kr.karanda.karandaserver.service.reporthub

import kr.karanda.karandaserver.entity.ReportHubChannelStatus
import kr.karanda.karandaserver.entity.ReportHubLock
import kr.karanda.karandaserver.entity.ReportHubMaintenance
import kr.karanda.karandaserver.entity.ReportHubReport
import kr.karanda.karandaserver.entity.ReportHubSession
import kr.karanda.karandaserver.enums.BDORegion
import kr.karanda.karandaserver.enums.ReportKind
import kr.karanda.karandaserver.enums.ReportMechanism
import kr.karanda.karandaserver.enums.ReportTarget
import kr.karanda.karandaserver.repository.jpa.ReportHubChannelStatusRepository
import kr.karanda.karandaserver.repository.jpa.ReportHubLockRepository
import kr.karanda.karandaserver.repository.jpa.ReportHubMaintenanceRepository
import kr.karanda.karandaserver.repository.jpa.ReportHubReportRepository
import kr.karanda.karandaserver.repository.jpa.ReportHubSessionRepository
import kr.karanda.karandaserver.service.reporthub.ReportHubRules.ChannelReplayItem
import kr.karanda.karandaserver.service.reporthub.ReportHubRules.ChannelState
import kr.karanda.karandaserver.service.reporthub.ReportHubRules.Decision
import kr.karanda.karandaserver.service.reporthub.ReportHubRules.MaintenanceDecision
import kr.karanda.karandaserver.service.reporthub.ReportHubRules.MaintenanceState
import kr.karanda.karandaserver.service.reporthub.ReportHubRules.SessionDecision
import kr.karanda.karandaserver.service.reporthub.ReportHubRules.SessionState
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Component
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.support.TransactionTemplate
import java.time.Duration
import java.time.ZonedDateTime

/**
 * 제보·취소를 한 트랜잭션 안에서 상태에 반영한다.
 *
 * 같은 지역·대상(점검은 지역 단위)의 처리는 [ReportHubLock] 행을 `FOR UPDATE`로 잡아 직렬화한다.
 * 락이 트랜잭션의 첫 문장이므로 이후 읽기는 앞선 처리가 커밋한 최신 상태를 본다.
 */
@Component
class ReportHubStateWriter(
    private val reportRepository: ReportHubReportRepository,
    private val sessionRepository: ReportHubSessionRepository,
    private val channelStatusRepository: ReportHubChannelStatusRepository,
    private val maintenanceRepository: ReportHubMaintenanceRepository,
    private val lockRepository: ReportHubLockRepository,
    transactionManager: PlatformTransactionManager,
) {
    private val transaction = TransactionTemplate(transactionManager)
    private val newTransaction = TransactionTemplate(transactionManager).apply {
        propagationBehavior = TransactionDefinition.PROPAGATION_REQUIRES_NEW
    }

    /** 반영 결과. 바뀐 행만 담는다(델타 발행용). */
    class Outcome(
        val report: ReportHubReport,
        val sessions: List<ReportHubSession>,
        val channels: List<ReportHubChannelStatus>,
        val maintenances: List<ReportHubMaintenance>,
        val alerts: List<ReportHubAlertCandidate>,
    )

    private class Changes {
        val sessions = linkedMapOf<Long, ReportHubSession>()
        val channels = linkedMapOf<Long, ReportHubChannelStatus>()
        val maintenances = linkedMapOf<Long, ReportHubMaintenance>()
        val alerts = mutableListOf<ReportHubAlertCandidate>()

        fun add(session: ReportHubSession) { sessions[session.id!!] = session }
        fun add(status: ReportHubChannelStatus) { channels[status.id!!] = status }
        fun add(maintenance: ReportHubMaintenance) { maintenances[maintenance.id!!] = maintenance }

        fun toOutcome(report: ReportHubReport) =
            Outcome(report, sessions.values.toList(), channels.values.toList(), maintenances.values.toList(), alerts)
    }

    // ---------------------------------------------------------------- 제보

    fun apply(
        config: ReportHubRegionConfig,
        report: ReportHubRules.ValidatedReport,
        reporterId: Long,
        now: ZonedDateTime,
    ): Outcome = transaction.execute {
        lock(lockKey(config.region, report.target))
        val saved = reportRepository.save(
            ReportHubReport(
                region = config.region,
                target = report.target,
                channel = report.channel,
                kind = report.kind,
                eventAt = report.eventAt,
                reportedAt = now,
                reporterId = reporterId,
            )
        )
        val changes = Changes()
        val policy = report.policy
        when (policy?.mechanism) {
            null -> applyMaintenance(config, saved, now, changes)
            ReportMechanism.SHARED_SCHEDULE -> applyShared(policy, saved, now, changes)
            else -> applyChannel(policy, saved, null, now, changes)
        }
        changes.toOutcome(saved)
    }!!

    private fun applyChannel(
        policy: ReportTargetPolicy,
        report: ReportHubReport,
        sessionId: Long?,
        now: ZonedDateTime,
        changes: Changes,
    ) {
        val target = report.target!!
        val channel = report.channel!!
        val status = channelStatusRepository.findByRegionAndTargetAndChannel(report.region, target, channel)
        val state = status?.let { ChannelState(it.kind, it.eventAt, it.confirmCount, it.sessionId) }
        val contributed = status?.kind != null && status.eventAt != null && hasContributed(status, report, sessionId)

        val decision = ReportHubRules.decideChannel(state, report.kind, report.eventAt, sessionId, policy.mechanism, contributed)
        val updated = when (decision) {
            Decision.NEW_STATE -> {
                val row = status ?: ReportHubChannelStatus(
                    region = report.region,
                    target = target,
                    channel = channel,
                    kind = null,
                    eventAt = null,
                    updatedAt = now,
                )
                row.kind = report.kind
                row.eventAt = report.eventAt
                row.updatedAt = now
                row.confirmCount = 1
                row.sourceReportId = report.id
                row.reporterId = report.reporterId
                row.sessionId = sessionId
                channelStatusRepository.save(row)
            }

            Decision.CONFIRM -> {
                status!!.confirmCount += 1
                status.updatedAt = now
                status
            }

            Decision.IGNORE -> return
        }
        changes.add(updated)

        if (isChannelAlertEvent(policy.mechanism, updated.kind)) {
            changes.alerts += ReportHubAlertCandidate(
                eventKey = "C:${updated.id}:${updated.eventAt!!.toEpochSecond()}",
                region = report.region,
                target = target,
                channel = channel,
                kind = updated.kind!!,
                eventAt = updated.eventAt!!,
                triggerReportId = report.id!!,
                reporterId = report.reporterId,
                reachedVerified = decision == Decision.CONFIRM && updated.confirmCount == ReportHubRules.VERIFIED_THRESHOLD,
            )
        }
    }

    private fun hasContributed(status: ReportHubChannelStatus, report: ReportHubReport, sessionId: Long?): Boolean {
        return if (sessionId != null) {
            reportRepository.existsBySessionIdAndChannelAndReporterIdAndKindAndCanceledFalseAndIdNot(
                sessionId, report.channel!!, report.reporterId, report.kind, report.id!!,
            )
        } else {
            reportRepository.existsByRegionAndTargetAndChannelAndReporterIdAndKindAndCanceledFalseAndEventAtGreaterThanEqualAndIdNot(
                report.region, report.target!!, report.channel!!, report.reporterId, report.kind,
                status.eventAt!!.minus(ReportHubRules.CONFIRM_TOLERANCE), report.id!!,
            )
        }
    }

    /** 채널 단위 알림 이벤트: 마레타 사망, 버프 활성 (SHARED는 회차 단위로 따로 처리) */
    private fun isChannelAlertEvent(mechanism: ReportMechanism, kind: ReportKind?): Boolean =
        (mechanism == ReportMechanism.CHANNEL_RESPAWN && kind == ReportKind.KILLED) ||
                (mechanism == ReportMechanism.BUFF && kind == ReportKind.ACTIVATED)

    private fun applyShared(policy: ReportTargetPolicy, report: ReportHubReport, now: ZonedDateTime, changes: Changes) {
        val lifetime = policy.lifetime ?: return
        val current = sessionRepository.findFirstByRegionAndTargetAndCanceledFalseOrderByAnchorAtDescIdDesc(
            report.region, policy.target,
        )
        val currentState = current?.let { SessionState(it.id!!, it.anchorAt) }

        val session: ReportHubSession = when (report.kind) {
            ReportKind.SPOTTED -> when (ReportHubRules.decideSession(currentState, report.eventAt, lifetime)) {
                SessionDecision.NEW -> {
                    val created = sessionRepository.save(
                        ReportHubSession(
                            region = report.region,
                            target = policy.target,
                            anchorAt = report.eventAt,
                            sourceReportId = report.id!!,
                            reporterId = report.reporterId,
                            updatedAt = now,
                        )
                    )
                    changes.alerts += sessionAlert(created, report, reachedVerified = false)
                    created
                }

                SessionDecision.JOIN -> {
                    val joined = current!!
                    val contributed = reportRepository.existsBySessionIdAndReporterIdAndKindAndCanceledFalseAndIdNot(
                        joined.id!!, report.reporterId, ReportKind.SPOTTED, report.id!!,
                    )
                    if (report.eventAt.isBefore(joined.anchorAt)) {
                        joined.anchorAt = report.eventAt
                        joined.sourceReportId = report.id!!
                        joined.reporterId = report.reporterId
                    }
                    if (!contributed) {
                        joined.confirmCount += 1
                        changes.alerts += sessionAlert(
                            joined, report,
                            reachedVerified = joined.confirmCount == ReportHubRules.VERIFIED_THRESHOLD,
                        )
                    }
                    joined.updatedAt = now
                    joined
                }

                SessionDecision.IGNORE -> return
            }

            ReportKind.KILLED -> {
                if (!ReportHubRules.isKillWithinSession(currentState, report.eventAt, lifetime)) return
                current!!
            }

            else -> return
        }
        changes.add(session)
        report.sessionId = session.id
        applyChannel(policy, report, session.id, now, changes)
    }

    private fun sessionAlert(session: ReportHubSession, report: ReportHubReport, reachedVerified: Boolean) =
        ReportHubAlertCandidate(
            eventKey = "S:${session.id}",
            region = session.region,
            target = session.target,
            channel = report.channel,
            kind = ReportKind.SPOTTED,
            eventAt = session.anchorAt,
            triggerReportId = report.id!!,
            reporterId = report.reporterId,
            reachedVerified = reachedVerified,
        )

    private fun applyMaintenance(
        config: ReportHubRegionConfig,
        report: ReportHubReport,
        now: ZonedDateTime,
        changes: Changes,
    ) {
        val recent = maintenanceRepository.findAllByRegionAndCanceledFalseAndUpdatedAtAfterOrderByIdDesc(
            config.region, now.minus(ReportHubRules.MAINTENANCE_LOOKBACK.multipliedBy(2)),
        )
        val decision = ReportHubRules.decideMaintenance(
            recent.map { MaintenanceState(it.id!!, it.startedAt, it.endedAt) }, report.kind, report.eventAt,
        )
        val record = when (decision) {
            is MaintenanceDecision.New -> maintenanceRepository.save(
                ReportHubMaintenance(region = config.region, updatedAt = now)
            )

            is MaintenanceDecision.Attach -> recent.first { it.id == decision.id }
            is MaintenanceDecision.Confirm -> recent.first { it.id == decision.id }
        }
        if (decision !is MaintenanceDecision.Confirm) {
            if (report.kind == ReportKind.MAINTENANCE_START) record.startedAt = report.eventAt else record.endedAt = report.eventAt
        }
        report.maintenanceId = record.id
        reportRepository.save(report)
        val wasConfirmed = record.confirmed
        recountMaintenance(config, record, now)
        changes.add(record)

        if (record.confirmed && record.endedAt != null && (!wasConfirmed || report.kind == ReportKind.SERVER_OPEN)) {
            changes.alerts += ReportHubAlertCandidate(
                eventKey = "M:${record.id}",
                region = config.region,
                target = null,
                channel = null,
                kind = ReportKind.SERVER_OPEN,
                eventAt = record.endedAt!!,
                triggerReportId = report.id!!,
                reporterId = report.reporterId,
                reachedVerified = true,
            )
        }
    }

    /** 점검 기록의 동조 수와 확정 여부를 원장 기준으로 다시 센다(서로 다른 제보자 기준). */
    private fun recountMaintenance(config: ReportHubRegionConfig, record: ReportHubMaintenance, now: ZonedDateTime) {
        val reports = reportRepository.findAllByMaintenanceIdAndCanceledFalse(record.id!!)
        record.startConfirmCount = reports.filter { it.kind == ReportKind.MAINTENANCE_START }.map { it.reporterId }.toSet().size
        record.endConfirmCount = reports.filter { it.kind == ReportKind.SERVER_OPEN }.map { it.reporterId }.toSet().size
        val reporterCount = reports.map { it.reporterId }.toSet().size
        record.confirmed = ReportHubRules.isMaintenanceConfirmed(config, record.startedAt, record.endedAt, reporterCount)
        record.updatedAt = now
    }

    // ---------------------------------------------------------------- 취소

    /**
     * 제보를 취소하고 원장 기준으로 관련 상태를 다시 계산한다.
     *
     * [target]은 호출 측이 권한 확인 때 읽은 값을 받는다. 락보다 먼저 일반 읽기를 하면 REPEATABLE READ에서
     * 그 시점 스냅샷이 고정되어 락 이후에도 최신 상태를 못 보기 때문이다.
     */
    fun cancel(
        config: ReportHubRegionConfig,
        reportId: Long,
        target: ReportTarget?,
        reason: String,
        now: ZonedDateTime,
    ): Outcome = transaction.execute {
        lock(lockKey(config.region, target))
        val report = reportRepository.findById(reportId).orElseThrow()
        report.canceled = true
        report.cancelReason = reason.take(200)
        reportRepository.save(report)

        val changes = Changes()
        val policy = report.target?.let { config.policyOf(it) }
        when (policy?.mechanism) {
            null -> report.maintenanceId?.let { recomputeMaintenance(config, it, now, changes) }
            ReportMechanism.SHARED_SCHEDULE -> {
                report.sessionId?.let { recomputeSession(it, now, changes) }
                recomputeChannel(policy, report, now, changes)
            }

            else -> recomputeChannel(policy, report, now, changes)
        }
        changes.toOutcome(report)
    }!!

    private fun recomputeChannel(policy: ReportTargetPolicy, report: ReportHubReport, now: ZonedDateTime, changes: Changes) {
        val channel = report.channel ?: return
        val status = channelStatusRepository.findByRegionAndTargetAndChannel(report.region, policy.target, channel)
            ?: return
        val sessionId = if (policy.mechanism == ReportMechanism.SHARED_SCHEDULE) {
            // 취소된 제보가 지금 상태의 회차와 무관하면 채널 상태는 그대로다
            if (status.sessionId == null || status.sessionId != report.sessionId) return
            status.sessionId
        } else null

        val reports = if (sessionId != null) {
            reportRepository.findAllBySessionIdAndChannelAndCanceledFalseOrderByReportedAtAscIdAsc(sessionId, channel)
        } else {
            reportRepository.findAllByRegionAndTargetAndChannelAndCanceledFalseAndReportedAtGreaterThanEqualOrderByReportedAtAscIdAsc(
                report.region, policy.target, channel, now.minus(REPLAY_WINDOW),
            )
        }
        val result = ReportHubRules.replayChannel(
            reports.map { ChannelReplayItem(it.id!!, it.reporterId, it.kind, it.eventAt) },
            policy.mechanism,
            sessionId,
        )
        status.kind = result?.kind
        status.eventAt = result?.eventAt
        status.confirmCount = result?.confirmCount ?: 0
        status.sourceReportId = result?.sourceReportId
        status.reporterId = result?.reporterId
        status.updatedAt = now
        changes.add(channelStatusRepository.save(status))
    }

    private fun recomputeSession(sessionId: Long, now: ZonedDateTime, changes: Changes) {
        val session = sessionRepository.findById(sessionId).orElse(null) ?: return
        val spotted = reportRepository.findAllBySessionIdAndKindAndCanceledFalseOrderByEventAtAscIdAsc(sessionId, ReportKind.SPOTTED)
        if (spotted.isEmpty()) {
            session.canceled = true
        } else {
            val first = spotted.first()
            session.anchorAt = first.eventAt
            session.sourceReportId = first.id!!
            session.reporterId = first.reporterId
            session.confirmCount = spotted.map { it.reporterId }.toSet().size
        }
        session.updatedAt = now
        changes.add(sessionRepository.save(session))
        if (session.canceled) {
            // 이 회차가 사라지면 클라이언트는 직전 회차로 돌아가야 하므로 함께 보낸다
            sessionRepository.findFirstByRegionAndTargetAndCanceledFalseOrderByAnchorAtDescIdDesc(session.region, session.target)
                ?.let { changes.add(it) }
        }
    }

    private fun recomputeMaintenance(config: ReportHubRegionConfig, maintenanceId: Long, now: ZonedDateTime, changes: Changes) {
        val record = maintenanceRepository.findById(maintenanceId).orElse(null) ?: return
        val reports = reportRepository.findAllByMaintenanceIdAndCanceledFalse(maintenanceId)
        record.startedAt = reports.filter { it.kind == ReportKind.MAINTENANCE_START }.minOfOrNull { it.eventAt }
        record.endedAt = reports.filter { it.kind == ReportKind.SERVER_OPEN }.minOfOrNull { it.eventAt }
        record.canceled = reports.isEmpty()
        recountMaintenance(config, record, now)
        changes.add(maintenanceRepository.save(record))
    }

    // ---------------------------------------------------------------- 락

    private fun lockKey(region: BDORegion, target: ReportTarget?): String =
        "${region.name}:${target?.name ?: "MAINTENANCE"}"

    private fun lock(key: String) {
        if (lockRepository.lock(key) != null) return
        try {
            newTransaction.executeWithoutResult { lockRepository.saveAndFlush(ReportHubLock(key)) }
        } catch (_: DataIntegrityViolationException) {
            // 다른 요청이 동시에 만들었다
        }
        lockRepository.lock(key) ?: throw IllegalStateException("Report hub lock row missing: $key")
    }

    companion object {
        /** 채널 상태 재계산 시 살펴볼 원장 범위. 마레타 재출현(1시간)·버프 지속(1시간)보다 넉넉하게. */
        private val REPLAY_WINDOW: Duration = Duration.ofHours(6)
    }
}
