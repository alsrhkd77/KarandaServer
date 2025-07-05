package kr.karanda.karandaserver.dto

import kotlinx.serialization.Serializable
import kr.karanda.karandaserver.enums.BDORegion

@Serializable
data class UserFcmSettingsDTO(
    var token: String,
    var region: BDORegion,
    var partyFinder: Boolean = false,
    var fieldBoss: Boolean = false,
)
