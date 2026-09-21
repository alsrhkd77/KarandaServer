package kr.karanda.karandaserver.entity

import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import kr.karanda.karandaserver.dto.MarketBiddingInfo
import kr.karanda.karandaserver.dto.MarketItem
import kr.karanda.karandaserver.dto.MarketItemDetail
import kr.karanda.karandaserver.enums.BDORegion
import kr.karanda.karandaserver.util.BiddingInfoListJsonConverter
import kr.karanda.karandaserver.util.LongListJsonConverter
import java.time.ZonedDateTime

/**
 * 거래소 아이템 가격 캐시.
 *
 * (itemNum, enhancementLevel, region)당 1행을 유지하며 제자리 갱신한다.
 * 외부 거래소 API 호출을 줄이기 위한 캐시 용도이며, 조회 요청 시 stale이면 갱신된다.
 */
@Entity
@Table(
    name = "market_price_data",
    uniqueConstraints = [UniqueConstraint(columnNames = ["item_num", "enhancement_level", "region"])],
)
class MarketPriceData(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    val itemNum: Int,
    val enhancementLevel: Int = 0,
    val region: String = BDORegion.KR.name,
    /** 현재 기준가. */
    var basePrice: Long = 0,
    /** 90일 일별 가격(index 0 = 오늘). 상세 갱신 전에는 빈 리스트. */
    @Convert(converter = LongListJsonConverter::class)
    @Column(columnDefinition = "TEXT")
    var priceHistory: List<Long> = emptyList(),
    /** 총(누적) 거래량. */
    var cumulativeVolume: Long = 0,
    /** 재고 = 판매 대기 수량. */
    var currentStock: Long = 0,
    /** 해당 강화 단계의 구매 대기 수량. 0 = 상세 갱신 전 미수집 또는 실제 0. */
    var buyOrders: Long = 0,
    /** 가격대별 판매/구매 주문. 상세 갱신 전에는 빈 리스트. */
    @Convert(converter = BiddingInfoListJsonConverter::class)
    @Column(columnDefinition = "TEXT")
    var orderBook: List<MarketBiddingInfo> = emptyList(),
    /** 라이트 필드(basePrice·currentStock·cumulativeVolume) 갱신 시각 (UTC). */
    var updatedAt: ZonedDateTime,
    /** 상세 필드(priceHistory·buyOrders) 갱신 시각 (UTC). null = 상세 미수집. */
    var detailUpdatedAt: ZonedDateTime? = null,
) {
    fun toLatestDTO() = MarketItem(
        itemNum = itemNum,
        enhancementLevel = enhancementLevel,
        price = basePrice,
        currentStock = currentStock,
        cumulativeVolume = cumulativeVolume,
        date = updatedAt.withZoneSameInstant(BDORegion.valueOf(region).timezone),
    )

    fun toDetailDTO() = MarketItemDetail(
        itemNum = itemNum,
        enhancementLevel = enhancementLevel,
        priceHistory = priceHistory,
        cumulativeVolume = cumulativeVolume,
        currentStock = currentStock,
        buyOrders = buyOrders,
        orderBook = orderBook,
        updatedAt = detailUpdatedAt ?: updatedAt,
    )
}
