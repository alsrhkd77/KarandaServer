package kr.karanda.karandaserver.entity

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import kr.karanda.karandaserver.data.MarketItem
import java.time.ZonedDateTime

@Entity
@Table(
    name = "market_data",
    indexes = [Index(name = "item_num_enhancement_level_date_idx", columnList = "item_num, enhancement_level, date")]
)
class MarketData(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Int? = null,
    var itemNum: Int,
    var enhancementLevel: Int = 0,
    var price: Long,
    var cumulativeVolume: Long = 0,
    var currentStock: Long = 0,
    var date: ZonedDateTime
) {
    fun toDTO() = MarketItem(
        itemNum = itemNum,
        enhancementLevel = enhancementLevel,
        price = price,
        cumulativeVolume = cumulativeVolume,
        currentStock = currentStock,
        date = date
    )
}