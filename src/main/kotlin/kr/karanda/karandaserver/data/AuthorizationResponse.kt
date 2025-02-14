package kr.karanda.karandaserver.data

data class AuthorizationResponse(
    var token: String? = null,
    var refreshToken: String? = null,
    var avatar: String? = null,
    var discordId: String = "",
    var username: String = "",
)
