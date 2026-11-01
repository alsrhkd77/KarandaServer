package kr.karanda.karandaserver.entity

import jakarta.persistence.*
import kr.karanda.karandaserver.dto.ReporterSummaryDTO
import kr.karanda.karandaserver.enums.BDORegion
import java.time.ZonedDateTime

/**
 * 지역별 제보자 통계(비정규화). 스냅샷·리더보드가 users·bdo_family를 조인하지 않도록
 * 표시명과 아바타를 제보할 때마다 여기에 갱신해 둔다.
 */
@Entity
@Table(
    name = "report_hub_reporter",
    uniqueConstraints = [UniqueConstraint(name = "uk_rh_reporter", columnNames = ["user_id", "region"])]
)
class ReportHubReporter(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "user_id", nullable = false)
    val userId: Long,

    @Enumerated(EnumType.STRING)
    @Column(name = "region", length = 10, nullable = false)
    val region: BDORegion,

    @Column(name = "display_name", length = 64, nullable = false)
    var displayName: String,

    @Column(name = "avatar_url", length = 256)
    var avatarUrl: String?,

    @Column(name = "report_count", nullable = false)
    var reportCount: Long = 0,

    @Column(name = "follower_count", nullable = false)
    var followerCount: Long = 0,

    @Column(name = "last_reported_at")
    var lastReportedAt: ZonedDateTime? = null,
) {
    fun toSummaryDTO(recentReportCount: Long? = null): ReporterSummaryDTO = ReporterSummaryDTO(
        id = userId,
        name = displayName,
        avatar = avatarUrl,
        reportCount = reportCount,
        followerCount = followerCount,
        lastReportedAt = lastReportedAt,
        recentReportCount = recentReportCount,
    )
}
