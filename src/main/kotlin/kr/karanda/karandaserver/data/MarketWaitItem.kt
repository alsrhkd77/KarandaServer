package kr.karanda.karandaserver.data

import kotlinx.serialization.Serializable
import kr.karanda.karandaserver.util.ZonedDateTimeSerializer
import java.time.ZonedDateTime

@Serializable
data class MarketWaitItem(
    val itemNum: Int,
    val enhancementLevel: Int,
    val price: Long,
    @Serializable(with = ZonedDateTimeSerializer::class)
    val targetTime: ZonedDateTime
)
