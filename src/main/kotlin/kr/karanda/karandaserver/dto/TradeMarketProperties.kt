package kr.karanda.karandaserver.dto

data class TradeMarketProperties(
    var api: String = "",
    var kr: String = "",
    var na: String = "",
    var eu: String = "",
    var jp: String = "",
    var headers: Map<String, String> = mutableMapOf(),
    var keyType: String = "",
    var parse: String = "",
)
