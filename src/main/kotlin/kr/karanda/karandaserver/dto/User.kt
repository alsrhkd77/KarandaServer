package kr.karanda.karandaserver.dto

import kotlinx.serialization.Serializable

@Serializable
data class User(
    var avatar: String? = null,
    var discordId: String = "",
    var username: String = "",
)
