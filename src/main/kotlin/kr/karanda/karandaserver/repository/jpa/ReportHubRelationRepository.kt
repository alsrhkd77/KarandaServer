package kr.karanda.karandaserver.repository.jpa

import kr.karanda.karandaserver.entity.ReportHubRelation
import kr.karanda.karandaserver.enums.BDORegion
import kr.karanda.karandaserver.enums.ReportHubRelationType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface ReportHubRelationRepository : JpaRepository<ReportHubRelation, Long> {
    fun findAllByUserIdAndRegion(userId: Long, region: BDORegion): List<ReportHubRelation>
    fun findByUserIdAndReporterIdAndRegion(userId: Long, reporterId: Long, region: BDORegion): ReportHubRelation?
    fun countByUserIdAndRegionAndType(userId: Long, region: BDORegion, type: ReportHubRelationType): Long
    fun findAllByUserIdOrReporterId(userId: Long, reporterId: Long): List<ReportHubRelation>

    /**
     * 팔로워 알림 수신 토큰: [reporterId]를 팔로우하고 옵트인한 사용자의 같은 지역 토큰.
     * 한 쌍은 FOLLOW/BLOCK 중 하나만 가지므로 팔로우 중이면 차단은 아니다.
     * owner를 로드하지 않도록(EAGER N+1 방지) 필요한 컬럼만 고른다.
     */
    @Query(
        "select s.token as token, s.owner.id as userId, s.reportHubTargets as targets " +
                "from user_fcm_settings s, ReportHubRelation r " +
                "where r.userId = s.owner.id and r.reporterId = :reporterId and r.region = :region " +
                "and r.type = kr.karanda.karandaserver.enums.ReportHubRelationType.FOLLOW " +
                "and s.reportHubNotify = true and s.region = :regionName"
    )
    fun findFollowerTokens(
        @Param("reporterId") reporterId: Long,
        @Param("region") region: BDORegion,
        @Param("regionName") regionName: String,
    ): List<AlertRecipient>

    /** 검증 알림 수신 토큰: 이 지역에서 아무도 팔로우하지 않았고 [reporterId]를 차단하지 않은 옵트인 사용자 */
    @Query(
        "select s.token as token, s.owner.id as userId, s.reportHubTargets as targets " +
                "from user_fcm_settings s " +
                "where s.reportHubNotify = true and s.region = :regionName " +
                "and not exists (select r.id from ReportHubRelation r where r.userId = s.owner.id and r.region = :region " +
                "and r.type = kr.karanda.karandaserver.enums.ReportHubRelationType.FOLLOW) " +
                "and not exists (select b.id from ReportHubRelation b where b.userId = s.owner.id and b.region = :region " +
                "and b.reporterId = :reporterId and b.type = kr.karanda.karandaserver.enums.ReportHubRelationType.BLOCK)"
    )
    fun findVerifiedTokens(
        @Param("reporterId") reporterId: Long,
        @Param("region") region: BDORegion,
        @Param("regionName") regionName: String,
    ): List<AlertRecipient>

    /** 서버 열림 알림 수신 토큰: 같은 지역 옵트인 사용자 전원 */
    @Query(
        "select s.token as token, s.owner.id as userId, s.reportHubTargets as targets " +
                "from user_fcm_settings s where s.reportHubNotify = true and s.region = :regionName"
    )
    fun findRegionTokens(@Param("regionName") regionName: String): List<AlertRecipient>

    interface AlertRecipient {
        val token: String
        val userId: Long
        val targets: String
    }
}
