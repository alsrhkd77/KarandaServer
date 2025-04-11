package kr.karanda.karandaserver.dto

data class DiscordUserData(
    var id: String,
    var username: String = "unknown",
    var discriminator: String,
    var avatar: String? = null,
    var banner: String? = null,
    var locale: String? = null,
)
