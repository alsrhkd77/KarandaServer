package kr.karanda.karandaserver.dto

import kotlinx.serialization.Serializable
import kr.karanda.karandaserver.dto.BDOFamilyDTO

@Serializable
data class UserDTO(
    var discordId: String = "",
    var username: String = "",
    var avatar: String? = null,
    var mainFamily: BDOFamilyDTO? = null,
)
