package kr.karanda.karandaserver.data

data class DiscordUserDataResponse(
    val id: String,
    val username: String,
    val discriminator: String,
    val avatar: String? = null,
    val banner: String? = null,
    val locale: String? = null,
)
