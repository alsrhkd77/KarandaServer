package kr.karanda.karandaserver.data

data class TradeMarketProperties(
    var api: String = "",
    var headers: Map<String, String> = mutableMapOf(),
    var keyType: String = "",
)
