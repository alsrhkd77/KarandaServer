package kr.karanda.karandaserver.entity

import jakarta.persistence.*
import kr.karanda.karandaserver.enums.BDORegion
import kr.karanda.karandaserver.enums.ReportHubRelationType
import java.time.ZonedDateTime

/**
 * 제보자 팔로우/차단. 지역 단위다 — 신뢰는 같은 서버 안에서 쌓인다.
 * 한 쌍(user, reporter, region)은 FOLLOW와 BLOCK 중 하나만 가진다.
 * users에 FK를 두지 않는다(회원 탈퇴 시 서비스에서 함께 정리).
 */
@Entity
@Table(
    name = "report_hub_relation",
    uniqueConstraints = [UniqueConstraint(name = "uk_rh_relation", columnNames = ["user_id", "reporter_id", "region"])],
    indexes = [Index(name = "idx_rh_relation_reporter", columnList = "region,reporter_id,type")]
)
class ReportHubRelation(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "user_id", nullable = false)
    val userId: Long,

    @Column(name = "reporter_id", nullable = false)
    val reporterId: Long,

    @Enumerated(EnumType.STRING)
    @Column(name = "region", length = 10, nullable = false)
    val region: BDORegion,

    @Enumerated(EnumType.STRING)
    @Column(name = "type", length = 10, nullable = false)
    var type: ReportHubRelationType,

    @Column(name = "created_at", nullable = false)
    var createdAt: ZonedDateTime,
)
