package kr.karanda.karandaserver.service.reporthub

import kr.karanda.karandaserver.enums.BDORegion
import kr.karanda.karandaserver.enums.ReportKind
import kr.karanda.karandaserver.enums.ReportTarget
import java.time.ZonedDateTime

/**
 * 커밋 후 알림 파이프라인으로 넘기는 알림 후보.
 *
 * @property eventKey 알림 이벤트 식별자. 같은 사람에게 같은 이벤트를 두 번 보내지 않는 기준.
 *   SHARED 회차 `S:{sessionId}`, 채널 상태 `C:{statusId}:{eventAt epoch}`, 점검 `M:{maintenanceId}`
 * @property kind 이벤트 종류. SHARED 회차는 SPOTTED, MARETTA는 KILLED, 버프는 ACTIVATED, 점검은 SERVER_OPEN
 * @property triggerReportId 이 후보를 만든 제보. 발송 시점에 취소됐으면 버린다.
 * @property reporterId 이벤트를 만들거나 동조한 제보자 (팔로워 알림 기준)
 * @property reachedVerified 이 제보로 서로 다른 제보자 수가 검증 기준에 도달했는지
 */
data class ReportHubAlertCandidate(
    val eventKey: String,
    val region: BDORegion,
    val target: ReportTarget?,
    val channel: String?,
    val kind: ReportKind,
    val eventAt: ZonedDateTime,
    val triggerReportId: Long,
    val reporterId: Long,
    val reachedVerified: Boolean,
)
