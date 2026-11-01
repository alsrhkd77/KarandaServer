package kr.karanda.karandaserver.entity

import jakarta.persistence.*
import kr.karanda.karandaserver.dto.ReportHubChannelStatusDTO
import kr.karanda.karandaserver.enums.BDORegion
import kr.karanda.karandaserver.enums.ReportKind
import kr.karanda.karandaserver.enums.ReportTarget
import java.time.ZonedDateTime

/**
 * 대상·채널별 현재 상태. 행을 지우지 않는다 — 버전이 다시 0부터 시작하면 클라이언트 병합이 깨지므로
 * 남은 제보가 없으면 [kind]를 null로 비운다.
 */
@Entity
@Table(
    name = "report_hub_channel_status",
    uniqueConstraints = [UniqueConstraint(name = "uk_rh_channel_status", columnNames = ["region", "target", "channel"])]
)
class ReportHubChannelStatus(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "region", length = 10, nullable = false)
    val region: BDORegion,

    @Enumerated(EnumType.STRING)
    @Column(name = "target", length = 30, nullable = false)
    val target: ReportTarget,

    @Column(name = "channel", length = 40, nullable = false)
    val channel: String,

    /** null = 정보 없음(유효 제보가 모두 취소됨) */
    @Enumerated(EnumType.STRING)
    @Column(name = "kind", length = 30)
    var kind: ReportKind?,

    @Column(name = "event_at")
    var eventAt: ZonedDateTime?,

    /** 마지막으로 새 상태가 되거나 동조된 시각 (신선도 표시용) */
    @Column(name = "updated_at", nullable = false)
    var updatedAt: ZonedDateTime,

    @Column(name = "confirm_count", nullable = false)
    var confirmCount: Int = 0,

    @Column(name = "source_report_id")
    var sourceReportId: Long? = null,

    @Column(name = "reporter_id")
    var reporterId: Long? = null,

    /** SHARED 대상만: 이 상태가 속한 회차 */
    @Column(name = "session_id")
    var sessionId: Long? = null,

    @Version
    @Column(name = "version", nullable = false)
    var version: Long = 0,
) {
    fun toDTO(): ReportHubChannelStatusDTO = ReportHubChannelStatusDTO(
        id = id!!,
        region = region,
        target = target,
        channel = channel,
        kind = kind,
        eventAt = eventAt,
        updatedAt = updatedAt,
        confirmCount = confirmCount,
        reporterId = reporterId,
        sessionId = sessionId,
        version = version,
    )
}
