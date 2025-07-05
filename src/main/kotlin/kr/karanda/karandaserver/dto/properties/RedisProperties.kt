package kr.karanda.karandaserver.dto.properties

data class RedisProperties(
    var host: String = "",
    var port: Int = 6379,
    var password: String = "",
)
