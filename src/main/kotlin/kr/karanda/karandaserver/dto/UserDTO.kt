package kr.karanda.karandaserver.dto

import kotlinx.serialization.Serializable

@Serializable
data class UserDTO(
    var discordId: String = "",
    var username: String = "",
    var avatar: String? = null,
    var bdoFamily: BDOFamilyDTO? = null,
)
