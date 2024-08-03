package kr.karanda.karandaserver.data

data class TokenProperties(
    var algorithm: String = "",
    var expire: Int = 60,
    var refreshExpire: Int = 1440,
    var platformKey: String = "",
    var secretKey: String = "",
    var refreshKey: String = "",
)
