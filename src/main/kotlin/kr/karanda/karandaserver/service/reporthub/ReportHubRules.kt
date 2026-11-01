package kr.karanda.karandaserver.service.reporthub

import kr.karanda.karandaserver.enums.ReportKind
import kr.karanda.karandaserver.enums.ReportMechanism
import kr.karanda.karandaserver.enums.ReportTarget
import kr.karanda.karandaserver.exception.InvalidArgumentException
import java.time.Duration
import java.time.ZonedDateTime

/**
 * Reporter Hub 상태 전이 규칙.
 *
 * Spring·DB에 의존하지 않는 순수 함수로, 실시간 제보 처리와 취소 후 재계산이 같은 규칙을 쓴다.
 * 서버는 전이(동조 판정, 회차 묶기)만 알고 대기·출현 가능·남은 시간 같은 예측은 클라이언트가 계산한다.
 */
object ReportHubRules {
    /** 같은 사건으로 보는 eventAt 차이 */
    val CONFIRM_TOLERANCE: Duration = Duration.ofMinutes(5)

    /** SHARED 회차: 기준 출현 시각보다 이만큼 이른 출현 제보까지 같은 회차로 본다 */
    val SESSION_LEAD: Duration = Duration.ofMinutes(10)

    /** 점검 기록에 붙일 수 있는 최대 시간 범위 */
    val MAINTENANCE_LOOKBACK: Duration = Duration.ofHours(24)

    /** 같은 점검 시작/종료로 보는 시각 차이 */
    val MAINTENANCE_TOLERANCE: Duration = Duration.ofMinutes(30)

    /** 버프 외 제보의 eventAt 허용 범위 */
    val PAST_LIMIT: Duration = Duration.ofMinutes(15)
    val FUTURE_LIMIT: Duration = Duration.ofMinutes(1)

    /** 검증 알림·점검 확정에 필요한 서로 다른 제보자 수 */
    const val VERIFIED_THRESHOLD = 2

    // ---------------------------------------------------------------- 검증

    data class ValidatedReport(
        val target: ReportTarget?,
        val channel: String?,
        val kind: ReportKind,
        val eventAt: ZonedDateTime,
        val policy: ReportTargetPolicy?,
    )

    /**
     * 제보 검증. 실패하면 [InvalidArgumentException].
     *
     * BUFF는 인게임에 보이는 남은 시간([remainingMinutes])으로 받아 시작 시각을 역산한다.
     * 그 외는 클라이언트가 보낸 [eventAt]을 쓴다.
     */
    fun validate(
        config: ReportHubRegionConfig?,
        target: ReportTarget?,
        channel: String?,
        kind: ReportKind,
        eventAt: ZonedDateTime?,
        remainingMinutes: Int?,
        now: ZonedDateTime,
    ): ValidatedReport {
        if (config == null || !config.enabled) throw InvalidArgumentException()

        if (kind.isMaintenance) {
            if (target != null || channel != null || eventAt == null) throw InvalidArgumentException()
            if (!isWithin(eventAt, now.minus(PAST_LIMIT), now.plus(FUTURE_LIMIT))) throw InvalidArgumentException()
            return ValidatedReport(null, null, kind, eventAt, null)
        }

        val policy = target?.let { config.policyOf(it) } ?: throw InvalidArgumentException()
        if (channel == null || !config.hasChannel(channel)) throw InvalidArgumentException()
        if (kind !in policy.mechanism.kinds) throw InvalidArgumentException()

        val resolvedEventAt = if (policy.mechanism == ReportMechanism.BUFF) {
            val lifetimeMinutes = policy.lifetime?.toMinutes() ?: throw InvalidArgumentException()
            if (remainingMinutes == null || remainingMinutes !in 1..lifetimeMinutes) throw InvalidArgumentException()
            now.minusMinutes(lifetimeMinutes - remainingMinutes)
        } else {
            if (eventAt == null || !isWithin(eventAt, now.minus(PAST_LIMIT), now.plus(FUTURE_LIMIT))) {
                throw InvalidArgumentException()
            }
            eventAt
        }
        return ValidatedReport(policy.target, channel, kind, resolvedEventAt, policy)
    }

    // ---------------------------------------------------------------- 채널 상태

    data class ChannelState(
        val kind: ReportKind?,
        val eventAt: ZonedDateTime?,
        val confirmCount: Int,
        val sessionId: Long?,
    )

    enum class Decision { NEW_STATE, CONFIRM, IGNORE }

    /**
     * 채널 상태 전이 (MARETTA, BUFF, SHARED 채널 공통).
     *
     * - 같은 kind, eventAt 차이 5분 이내 → 동조
     * - 5분 넘게 늦으면 → 새 상태 (버프는 다시 활성 = 덮어쓰기)
     * - 5분 넘게 이르면 → 기존 상태가 미검증(동조 1)일 때만 교체, 아니면 원장에만 남김
     * - MARETTA 발견은 이후 사망이 없으면 시간과 관계없이 동조
     * - kind가 다르면 늦거나 거의 같은 시각일 때 새 상태(발견 → 사망 등)
     *
     * @param alreadyContributed 이 제보자가 이미 현재 상태에 기여했으면 동조로 세지 않는다.
     */
    fun decideChannel(
        state: ChannelState?,
        kind: ReportKind,
        eventAt: ZonedDateTime,
        sessionId: Long?,
        mechanism: ReportMechanism,
        alreadyContributed: Boolean,
    ): Decision {
        if (state?.kind == null || state.eventAt == null) return Decision.NEW_STATE
        if (mechanism == ReportMechanism.SHARED_SCHEDULE && state.sessionId != sessionId) return Decision.NEW_STATE

        val diff = Duration.between(state.eventAt.toInstant(), eventAt.toInstant())
        val earlierThanTolerance = diff < CONFIRM_TOLERANCE.negated()
        val confirm = if (alreadyContributed) Decision.IGNORE else Decision.CONFIRM
        val replaceIfUnverified = if (state.confirmCount <= 1) Decision.NEW_STATE else Decision.IGNORE

        if (kind != state.kind) {
            return if (earlierThanTolerance) replaceIfUnverified else Decision.NEW_STATE
        }
        if (mechanism == ReportMechanism.CHANNEL_RESPAWN && kind == ReportKind.SPOTTED && !earlierThanTolerance) {
            return confirm
        }
        return when {
            diff.abs() <= CONFIRM_TOLERANCE -> confirm
            diff > CONFIRM_TOLERANCE -> Decision.NEW_STATE
            else -> replaceIfUnverified
        }
    }

    data class ChannelReplayItem(val reportId: Long, val reporterId: Long, val kind: ReportKind, val eventAt: ZonedDateTime)

    data class ChannelReplayResult(
        val kind: ReportKind,
        val eventAt: ZonedDateTime,
        val confirmCount: Int,
        val sourceReportId: Long,
        val reporterId: Long,
    )

    /**
     * 취소 후 재계산. 유효 제보들을 접수 순서대로 [decideChannel]에 다시 흘려 상태를 만든다.
     * 실시간 처리와 같은 규칙을 쓰므로 결과가 어긋나지 않는다. 남은 제보가 없으면 null.
     */
    fun replayChannel(
        items: List<ChannelReplayItem>,
        mechanism: ReportMechanism,
        sessionId: Long?,
    ): ChannelReplayResult? {
        var result: ChannelReplayResult? = null
        val contributors = mutableSetOf<Long>()
        for (item in items) {
            val state = result?.let { ChannelState(it.kind, it.eventAt, it.confirmCount, sessionId) }
            when (decideChannel(state, item.kind, item.eventAt, sessionId, mechanism, item.reporterId in contributors)) {
                Decision.NEW_STATE -> {
                    result = ChannelReplayResult(item.kind, item.eventAt, 1, item.reportId, item.reporterId)
                    contributors.clear()
                    contributors.add(item.reporterId)
                }

                Decision.CONFIRM -> {
                    result = result!!.copy(confirmCount = result.confirmCount + 1)
                    contributors.add(item.reporterId)
                }

                Decision.IGNORE -> {}
            }
        }
        return result
    }

    // ---------------------------------------------------------------- SHARED 회차

    data class SessionState(val id: Long, val anchorAt: ZonedDateTime)

    enum class SessionDecision { JOIN, NEW, IGNORE }

    /**
     * SHARED 출현 제보를 어느 회차에 넣을지.
     * `[anchor - 10분, anchor + lifetime]`이면 같은 회차, 그 뒤면 새 회차.
     * 쿨타임보다 이른 출현도 새 회차로 받는다(점검으로 쿨타임이 초기화될 수 있음).
     */
    fun decideSession(current: SessionState?, eventAt: ZonedDateTime, lifetime: Duration): SessionDecision {
        if (current == null) return SessionDecision.NEW
        val start = current.anchorAt.minus(SESSION_LEAD)
        val end = current.anchorAt.plus(lifetime)
        return when {
            eventAt.isBefore(start) -> SessionDecision.IGNORE
            eventAt.isAfter(end) -> SessionDecision.NEW
            else -> SessionDecision.JOIN
        }
    }

    /** SHARED 처치 제보가 현재 회차에 속하는지 (소멸 직후 늦은 제보 허용) */
    fun isKillWithinSession(current: SessionState?, eventAt: ZonedDateTime, lifetime: Duration): Boolean {
        if (current == null) return false
        val start = current.anchorAt.minus(SESSION_LEAD)
        val end = current.anchorAt.plus(lifetime).plus(CONFIRM_TOLERANCE)
        return !eventAt.isBefore(start) && !eventAt.isAfter(end)
    }

    // ---------------------------------------------------------------- 점검

    data class MaintenanceState(val id: Long, val startedAt: ZonedDateTime?, val endedAt: ZonedDateTime?)

    sealed interface MaintenanceDecision {
        /** 비어 있는 시작/종료 시각을 채운다 */
        data class Attach(val id: Long) : MaintenanceDecision

        /** 이미 있는 시작/종료 시각에 동조 */
        data class Confirm(val id: Long) : MaintenanceDecision

        data object New : MaintenanceDecision
    }

    /**
     * 점검 제보를 기존 기록에 붙일지.
     * @param recent [eventAt] 기준 24시간 안의 유효 기록, 최신순
     */
    fun decideMaintenance(recent: List<MaintenanceState>, kind: ReportKind, eventAt: ZonedDateTime): MaintenanceDecision {
        val inRange = recent.filter { record ->
            val anchor = record.startedAt ?: record.endedAt ?: return@filter false
            Duration.between(anchor.toInstant(), eventAt.toInstant()).abs() <= MAINTENANCE_LOOKBACK
        }
        return when (kind) {
            ReportKind.MAINTENANCE_START -> {
                inRange.firstOrNull { it.startedAt != null && isNear(it.startedAt, eventAt, MAINTENANCE_TOLERANCE) }
                    ?.let { MaintenanceDecision.Confirm(it.id) }
                    ?: inRange.firstOrNull { it.startedAt == null && (it.endedAt == null || !eventAt.isAfter(it.endedAt)) }
                        ?.let { MaintenanceDecision.Attach(it.id) }
                    ?: MaintenanceDecision.New
            }

            ReportKind.SERVER_OPEN -> {
                inRange.firstOrNull { it.endedAt != null && isNear(it.endedAt, eventAt, MAINTENANCE_TOLERANCE) }
                    ?.let { MaintenanceDecision.Confirm(it.id) }
                    ?: inRange.firstOrNull { it.endedAt == null && (it.startedAt == null || !eventAt.isBefore(it.startedAt)) }
                        ?.let { MaintenanceDecision.Attach(it.id) }
                    ?: MaintenanceDecision.New
            }

            else -> throw IllegalArgumentException("Not a maintenance kind: $kind")
        }
    }

    /**
     * 점검 확정 여부. 그 지역 시간대 기준 정기점검 요일이면 1명, 아니거나 요일 미상이면 2명 이상.
     */
    fun isMaintenanceConfirmed(
        config: ReportHubRegionConfig,
        startedAt: ZonedDateTime?,
        endedAt: ZonedDateTime?,
        reporterCount: Int,
    ): Boolean {
        if (reporterCount <= 0) return false
        val regularDay = config.regularMaintenanceDay
        val onRegularDay = regularDay != null && listOfNotNull(startedAt, endedAt).any {
            it.withZoneSameInstant(config.timezone).dayOfWeek == regularDay
        }
        return if (onRegularDay) reporterCount >= 1 else reporterCount >= VERIFIED_THRESHOLD
    }

    // ---------------------------------------------------------------- util

    private fun isWithin(value: ZonedDateTime, start: ZonedDateTime, end: ZonedDateTime): Boolean =
        !value.isBefore(start) && !value.isAfter(end)

    private fun isNear(a: ZonedDateTime, b: ZonedDateTime, tolerance: Duration): Boolean =
        Duration.between(a.toInstant(), b.toInstant()).abs() <= tolerance
}
