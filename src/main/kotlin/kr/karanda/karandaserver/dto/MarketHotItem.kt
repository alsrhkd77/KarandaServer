package kr.karanda.karandaserver.dto

import java.time.ZonedDateTime

/**
 * GetWorldMarketHotList 응답의 개별 핫 아이템.
 *
 * 가격 관련 필드가 여러 개(기준가·등락폭·가격 상·하한·최근 거래가)이므로 [MarketItem]과 달리
 * 이름을 명시적으로 구분한다.
 */
data class MarketHotItem(
    val itemNum: Int,
    val minEnhancementLevel: Int,
    val maxEnhancementLevel: Int,
    val basePrice: Long,
    val currentStock: Long,
    val cumulativeVolume: Long,
    /** 가격 등락 방향. 1: 하락, 2: 상승. */
    val priceChangeDirection: Int,
    val priceChangeValue: Long,
    /** 가격 하한(price hard cap - min). */
    val priceCapMin: Long,
    /** 가격 상한(price hard cap - max). */
    val priceCapMax: Long,
    val lastSalePrice: Long,
    val lastSaleTime: ZonedDateTime,
)
