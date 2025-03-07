package kr.karanda.karandaserver.dto

data class RefreshTokenResponse(
    val token: String,
    val refreshToken: String,
    val avatar: String?,
    val discordId: String,
    val username: String,
){
    constructor(user: UserDTO, tokens: Tokens) : this(
        token = tokens.accessToken,
        refreshToken = tokens.refreshToken,
        discordId = user.discordId,
        username = user.username,
        avatar = user.avatar,
    )
}
