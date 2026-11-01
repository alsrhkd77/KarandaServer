package kr.karanda.karandaserver.entity

import jakarta.persistence.*
import kr.karanda.karandaserver.dto.ReportHubSessionDTO
import kr.karanda.karandaserver.enums.BDORegion
import kr.karanda.karandaserver.enums.ReportTarget
import java.time.ZonedDateTime

/**
 * SHARED_SCHEDULE 대상의 출현 회차. 출현 타이밍은 서버 공통이고 출현 채널은 랜덤이다.
 * 채널별 출현/처치는 [ReportHubChannelStatus]가 이 회차 id를 참조해 담는다.
 */
@Entity
@Table(
    name = "report_hub_session",
    indexes = [Index(name = "idx_rh_session_target", columnList = "region,target,anchor_at")]
)
class ReportHubSession(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "region", length = 10, nullable = false)
    val region: BDORegion,

    @Enumerated(EnumType.STRING)
    @Column(name = "target", length = 30, nullable = false)
    val target: ReportTarget,

    /** 회차의 가장 이른 출현 시각 = 재출현 계산 기준 */
    @Column(name = "anchor_at", nullable = false)
    var anchorAt: ZonedDateTime,

    /** 출현을 제보한 서로 다른 제보자 수 */
    @Column(name = "confirm_count", nullable = false)
    var confirmCount: Int = 1,

    @Column(name = "source_report_id", nullable = false)
    var sourceReportId: Long,

    @Column(name = "reporter_id", nullable = false)
    var reporterId: Long,

    @Column(name = "canceled", nullable = false)
    var canceled: Boolean = false,

    @Column(name = "updated_at", nullable = false)
    var updatedAt: ZonedDateTime,

    /** 동시성 제어와 클라이언트 병합용 버전 */
    @Version
    @Column(name = "version", nullable = false)
    var version: Long = 0,
) {
    fun toDTO(): ReportHubSessionDTO = ReportHubSessionDTO(
        id = id!!,
        region = region,
        target = target,
        anchorAt = anchorAt,
        confirmCount = confirmCount,
        reporterId = reporterId,
        canceled = canceled,
        updatedAt = updatedAt,
        version = version,
    )
}
