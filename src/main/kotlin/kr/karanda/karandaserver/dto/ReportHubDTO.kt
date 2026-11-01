package kr.karanda.karandaserver.dto

import kotlinx.serialization.Serializable
import kr.karanda.karandaserver.enums.BDORegion
import kr.karanda.karandaserver.enums.ReportHubAlertReason
import kr.karanda.karandaserver.enums.ReportHubRelationType
import kr.karanda.karandaserver.enums.ReportKind
import kr.karanda.karandaserver.enums.ReportMechanism
import kr.karanda.karandaserver.enums.ReportTarget
import kr.karanda.karandaserver.util.ZonedDateTimeSerializer
import java.time.ZonedDateTime

/**
 * 제보자 공개 정보. [id]는 users.id다.
 * userUUID·discordId는 절대 담지 않는다(UUID가 새면 개인 WS 채널 구독이 가능해짐).
 */
@Serializable
data class ReporterSummaryDTO(
    val id: Long,
    val name: String,
    val avatar: String?,
    val reportCount: Long,
    val followerCount: Long,
    @Serializable(with = ZonedDateTimeSerializer::class)
    val lastReportedAt: ZonedDateTime?,
    /** 리더보드에서만: 최근 7일 유효 제보 수 */
    val recentReportCount: Long?,
)

@Serializable
data class ReportHubSessionDTO(
    val id: Long,
    val region: BDORegion,
    val target: ReportTarget,
    @Serializable(with = ZonedDateTimeSerializer::class)
    val anchorAt: ZonedDateTime,
    val confirmCount: Int,
    val reporterId: Long,
    val canceled: Boolean,
    @Serializable(with = ZonedDateTimeSerializer::class)
    val updatedAt: ZonedDateTime,
    val version: Long,
)

@Serializable
data class ReportHubChannelStatusDTO(
    val id: Long,
    val region: BDORegion,
    val target: ReportTarget,
    val channel: String,
    /** null = 정보 없음 */
    val kind: ReportKind?,
    @Serializable(with = ZonedDateTimeSerializer::class)
    val eventAt: ZonedDateTime?,
    @Serializable(with = ZonedDateTimeSerializer::class)
    val updatedAt: ZonedDateTime,
    val confirmCount: Int,
    val reporterId: Long?,
    val sessionId: Long?,
    val version: Long,
)

@Serializable
data class ReportHubMaintenanceDTO(
    val id: Long,
    val region: BDORegion,
    @Serializable(with = ZonedDateTimeSerializer::class)
    val startedAt: ZonedDateTime?,
    @Serializable(with = ZonedDateTimeSerializer::class)
    val endedAt: ZonedDateTime?,
    val startConfirmCount: Int,
    val endConfirmCount: Int,
    val confirmed: Boolean,
    val canceled: Boolean,
    @Serializable(with = ZonedDateTimeSerializer::class)
    val updatedAt: ZonedDateTime,
    val version: Long,
)

/**
 * 현황 스냅샷과 실시간 델타가 같은 형태를 쓴다. 클라이언트는 행별 `version`이 큰 쪽을 채택해 병합한다.
 * 행은 제보자를 id로만 참조하고 제보자 정보는 [reporters]에 한 번씩만 담는다.
 * kotlinx 직렬화는 기본값과 같은 필드를 생략하므로, 클라이언트가 의존하는 필드에는 기본값을 두지 않는다.
 */
@Serializable
data class ReportHubStatusDTO(
    val region: BDORegion,
    @Serializable(with = ZonedDateTimeSerializer::class)
    val serverTime: ZonedDateTime,
    val sessions: List<ReportHubSessionDTO>,
    val channels: List<ReportHubChannelStatusDTO>,
    val maintenances: List<ReportHubMaintenanceDTO>,
    val reporters: List<ReporterSummaryDTO>,
)

@Serializable
data class ReportHubReportDTO(
    val id: Long,
    val region: BDORegion,
    val target: ReportTarget?,
    val channel: String?,
    val kind: ReportKind,
    @Serializable(with = ZonedDateTimeSerializer::class)
    val eventAt: ZonedDateTime,
    @Serializable(with = ZonedDateTimeSerializer::class)
    val reportedAt: ZonedDateTime,
    val reporterId: Long,
    val canceled: Boolean,
)

@Serializable
data class ReportHubReportListDTO(
    val reports: List<ReportHubReportDTO>,
    val reporters: List<ReporterSummaryDTO>,
)

/** 제보 결과: 저장된 제보(취소용 id)와 이 제보로 바뀐 상태(델타) */
@Serializable
data class ReportHubReportResultDTO(
    val report: ReportHubReportDTO,
    val status: ReportHubStatusDTO,
)

@Serializable
data class ReporterProfileDTO(
    val reporter: ReporterSummaryDTO,
    val recentReports: List<ReportHubReportDTO>,
)

@Serializable
data class ReportTargetPolicyDTO(
    val target: ReportTarget,
    val mechanism: ReportMechanism,
    val kinds: List<ReportKind>,
    val respawnMinMinutes: Long?,
    val respawnMaxMinutes: Long?,
    val respawnDelayMinutes: Long?,
    val lifetimeMinutes: Long?,
)

@Serializable
data class ReportHubPolicyDTO(
    val region: BDORegion,
    val enabled: Boolean,
    val timezone: String,
    /**
     * 응답 시점의 지역 UTC 오프셋(분). 클라이언트에 시간대 DB가 없어 "그 지역 기준 오늘 요일" 판단에 쓴다.
     * 서머타임 전환은 정책을 다시 받을 때 반영된다.
     */
    val utcOffsetMinutes: Int,
    /** java.time.DayOfWeek 이름(예: WEDNESDAY). null = 미상 */
    val regularMaintenanceDay: String?,
    val channels: List<String>,
    val targets: List<ReportTargetPolicyDTO>,
    val verifiedThreshold: Int,
)

/**
 * 제보 요청.
 * - 보스·마레타·점검: [eventAt] 필수(클라이언트가 방금/5분 전/10분 전 칩으로 계산)
 * - 버프: [remainingMinutes] 필수(인게임에 보이는 남은 시간). 서버가 시작 시각을 역산한다.
 * - 점검: [target]·[channel]은 null
 */
data class ReportHubReportRequest(
    val region: BDORegion,
    val target: ReportTarget? = null,
    val channel: String? = null,
    val kind: ReportKind,
    val eventAt: ZonedDateTime? = null,
    val remainingMinutes: Int? = null,
)

/**
 * 알림 페이로드. FCM 데이터 메시지와 Windows WS 알림이 같은 형태를 쓴다.
 * 문구는 담지 않는다 — 사용자가 앱에서 설정한 언어로 기기에서 만든다.
 */
@Serializable
data class ReportHubAlertDTO(
    /** 항상 [TYPE]. FCM 데이터 메시지에서 다른 알림과 구분하는 값 */
    val type: String,
    val eventKey: String,
    val region: BDORegion,
    /** null = 점검 */
    val target: ReportTarget?,
    val channel: String?,
    val kind: ReportKind,
    @Serializable(with = ZonedDateTimeSerializer::class)
    val eventAt: ZonedDateTime,
    val reason: ReportHubAlertReason,
    val reporterId: Long,
    val reporterName: String?,
) {
    companion object {
        const val TYPE = "REPORT_HUB"
    }
}

@Serializable
data class ReportHubRelationDTO(
    val reporterId: Long,
    val type: ReportHubRelationType,
    /** 이 지역에서 제보한 적 없거나 탈퇴했으면 null */
    val reporter: ReporterSummaryDTO?,
)

data class ReportHubRelationRequest(
    val type: ReportHubRelationType,
)
