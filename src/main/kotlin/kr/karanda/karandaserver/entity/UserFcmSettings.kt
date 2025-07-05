package kr.karanda.karandaserver.entity

import jakarta.persistence.*
import kr.karanda.karandaserver.dto.UserFcmSettingsDTO
import kr.karanda.karandaserver.enums.BDORegion
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
        )
    }
}