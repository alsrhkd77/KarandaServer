package kr.karanda.karandaserver.entity

import jakarta.persistence.*
import kr.karanda.karandaserver.dto.ReportHubReportDTO
import kr.karanda.karandaserver.enums.BDORegion
import kr.karanda.karandaserver.enums.ReportKind
import kr.karanda.karandaserver.enums.ReportTarget
import java.time.ZonedDateTime

/**
 * Reporter Hub 제보 원장. 취소해도 지우지 않는다(취소 후 재계산의 원본).
 *
 * `reporter_id`는 users.id 값이지만 FK를 두지 않는다. 회원 탈퇴가 users 행을 실제로 삭제하므로
 * FK가 있으면 탈퇴가 막힌다. 탈퇴 사용자의 표시 정보는 [ReportHubReporter] 삭제로 사라진다.
 */
@Entity
@Table(
    name = "report_hub_report",
    indexes = [
        Index(name = "idx_rh_report_key", columnList = "region,target,channel,event_at"),
        Index(name = "idx_rh_report_reporter", columnList = "reporter_id,reported_at"),
        Index(name = "idx_rh_report_region_reported", columnList = "region,reported_at"),
        Index(name = "idx_rh_report_session", columnList = "session_id"),
        Index(name = "idx_rh_report_maintenance", columnList = "maintenance_id"),
    ]
)
class ReportHubReport(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "region", length = 10, nullable = false)
    val region: BDORegion,

    /** 점검 제보면 null */
    @Enumerated(EnumType.STRING)
    @Column(name = "target", length = 30)
    val target: ReportTarget?,

    /** 점검 제보면 null */
    @Column(name = "channel", length = 40)
    val channel: String?,

    @Enumerated(EnumType.STRING)
    @Column(name = "kind", length = 30, nullable = false)
    val kind: ReportKind,

    @Column(name = "event_at", nullable = false)
    val eventAt: ZonedDateTime,

    @Column(name = "reported_at", nullable = false)
    val reportedAt: ZonedDateTime,

    @Column(name = "reporter_id", nullable = false)
    val reporterId: Long,

    /** SHARED 출현/처치 제보가 속한 회차 */
    @Column(name = "session_id")
    var sessionId: Long? = null,

    /** 점검 제보가 붙은 점검 기록 */
    @Column(name = "maintenance_id")
    var maintenanceId: Long? = null,

    @Column(name = "canceled", nullable = false)
    var canceled: Boolean = false,

    @Column(name = "cancel_reason", length = 200)
    var cancelReason: String? = null,
) {
    fun toDTO(): ReportHubReportDTO = ReportHubReportDTO(
        id = id!!,
        region = region,
        target = target,
        channel = channel,
        kind = kind,
        eventAt = eventAt,
        reportedAt = reportedAt,
        reporterId = reporterId,
        canceled = canceled,
    )
}
