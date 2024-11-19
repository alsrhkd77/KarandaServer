package kr.karanda.karandaserver.data

import kr.karanda.karandaserver.dto.BDOFamily

data class AuthorizationResponse(
    var token: String? = null,
    var refreshToken: String? = null,
    var avatar: String = "",
    var discordId: String = "",
    var username: String = "",
    var mainFamily: BDOFamily? = null,
)
