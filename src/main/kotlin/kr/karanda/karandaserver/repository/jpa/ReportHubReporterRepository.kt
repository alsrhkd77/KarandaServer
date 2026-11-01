package kr.karanda.karandaserver.repository.jpa

import kr.karanda.karandaserver.entity.ReportHubReporter
import kr.karanda.karandaserver.enums.BDORegion
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.ZonedDateTime

interface ReportHubReporterRepository : JpaRepository<ReportHubReporter, Long> {
    fun findByUserIdAndRegion(userId: Long, region: BDORegion): ReportHubReporter?
    fun findAllByRegionAndUserIdIn(region: BDORegion, userIds: Collection<Long>): List<ReportHubReporter>

    /** 제보 수 증가 + 표시 정보 갱신. 영향받은 행 수를 돌려준다(0이면 행이 없음). */
    @Modifying
    @Query(
        "update ReportHubReporter r set r.reportCount = r.reportCount + 1, r.lastReportedAt = :at, " +
                "r.displayName = :name, r.avatarUrl = :avatar where r.userId = :userId and r.region = :region"
    )
    fun recordReport(
        @Param("userId") userId: Long,
        @Param("region") region: BDORegion,
        @Param("at") at: ZonedDateTime,
        @Param("name") name: String,
        @Param("avatar") avatar: String?,
    ): Int

    @Modifying
    @Query(
        "update ReportHubReporter r set r.followerCount = r.followerCount + :delta " +
                "where r.userId = :userId and r.region = :region"
    )
    fun addFollowerCount(
        @Param("userId") userId: Long,
        @Param("region") region: BDORegion,
        @Param("delta") delta: Long,
    ): Int

    fun deleteAllByUserId(userId: Long)
}
