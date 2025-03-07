package kr.karanda.karandaserver.dto

import java.time.ZonedDateTime

data class MarketItem(
    val itemNum: Int,
    val enhancementLevel: Int = 0,
    val price: Long,
    val currentStock: Long,
    val cumulativeVolume: Long,
    val date: ZonedDateTime? = null,
)
