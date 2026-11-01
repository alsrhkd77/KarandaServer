package kr.karanda.karandaserver.entity

import jakarta.persistence.*
import kr.karanda.karandaserver.dto.ReportHubMaintenanceDTO
import kr.karanda.karandaserver.enums.BDORegion
import java.time.ZonedDateTime

/**
 * 서버 전체 점검 기록. 점검 시작 제보와 서버 열림 제보가 하나의 기록에 붙는다(어느 쪽이 먼저 와도 생성).
 * 확정된 점검은 그 이전의 예측을 무효로 만드는 경계선이고, 종료 시각은 점검 후 재출현 기준점이다.
 */
@Entity
@Table(
    name = "report_hub_maintenance",
    indexes = [Index(name = "idx_rh_maintenance_region", columnList = "region,updated_at")]
)
class ReportHubMaintenance(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "region", length = 10, nullable = false)
    val region: BDORegion,

    @Column(name = "started_at")
    var startedAt: ZonedDateTime? = null,

    @Column(name = "ended_at")
    var endedAt: ZonedDateTime? = null,

    @Column(name = "start_confirm_count", nullable = false)
    var startConfirmCount: Int = 0,

    @Column(name = "end_confirm_count", nullable = false)
    var endConfirmCount: Int = 0,

    @Column(name = "confirmed", nullable = false)
    var confirmed: Boolean = false,

    /** 붙은 제보가 모두 취소되면 true */
    @Column(name = "canceled", nullable = false)
    var canceled: Boolean = false,

    @Column(name = "updated_at", nullable = false)
    var updatedAt: ZonedDateTime,

    @Version
    @Column(name = "version", nullable = false)
    var version: Long = 0,
) {
    fun toDTO(): ReportHubMaintenanceDTO = ReportHubMaintenanceDTO(
        id = id!!,
        region = region,
        startedAt = startedAt,
        endedAt = endedAt,
        startConfirmCount = startConfirmCount,
        endConfirmCount = endConfirmCount,
        confirmed = confirmed,
        canceled = canceled,
        updatedAt = updatedAt,
        version = version,
    )
}
