package kr.karanda.karandaserver.entity

import jakarta.persistence.*
import kr.karanda.karandaserver.dto.UserFcmSettingsDTO
import kr.karanda.karandaserver.enums.BDORegion
import kr.karanda.karandaserver.enums.BdoNewsCategory
import org.hibernate.annotations.ColumnDefault
import java.time.ZonedDateTime

@Entity(name = "user_fcm_settings")
class UserFcmSettings(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    var token: String,
    var region: String,
    var partyFinder: Boolean = false,
    var fieldBoss: Boolean = false,
    /** 뉴스 알림 수신 여부 (Web 전용 — 서버가 토큰 개별 발송 대상 판단에 사용) */
    @ColumnDefault("false")
    @Column(nullable = false)
    var newsNotify: Boolean = false,
    /** 뉴스 알림 카테고리 CSV (예: "NOTICE,UPDATE,LAB") */
    @ColumnDefault("''")
    @Column(length = 60, nullable = false)
    var newsCategories: String = "",
    /** Reporter Hub 알림 수신 여부 (옵트인, 기본 off). 갱신은 PATCH /fcm/settings/report-hub로만 한다 */
    @ColumnDefault("false")
    @Column(nullable = false)
    var reportHubNotify: Boolean = false,
    /** Reporter Hub 알림을 받을 대상 코드 CSV (ReportTarget 이름 + "MAINTENANCE") */
    @ColumnDefault("''")
    @Column(length = 200, nullable = false)
    var reportHubTargets: String = "",
    var lastUpdated: ZonedDateTime,
    @ManyToOne
    var owner: User
) {
    fun toDTO(): UserFcmSettingsDTO {
        return UserFcmSettingsDTO(
            token = token,
            region = BDORegion.valueOf(region),
            partyFinder = partyFinder,
            fieldBoss = fieldBoss,
            newsNotify = newsNotify,
            newsCategories = newsCategories.split(",").filter { it.isNotBlank() }.mapNotNull { name -> BdoNewsCategory.entries.find { it.name == name } },
            reportHubNotify = reportHubNotify,
            reportHubTargets = reportHubTargets.split(",").filter { it.isNotBlank() },
        )
    }
}