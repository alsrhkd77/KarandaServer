package kr.karanda.karandaserver.dto

import java.time.ZonedDateTime

/**
 * `/trade-market/detail` 응답 항목. 특정 아이템의 강화 단계별 상세 정보.
 */
data class MarketItemDetail(
    val itemNum: Int,
    val enhancementLevel: Int,
    /** 90일 일별 가격. index 0 = 오늘. */
    val priceHistory: List<Long>,
    /** 총(누적) 거래량. */
    val cumulativeVolume: Long,
    /** 재고 = 판매 대기 수량. */
    val currentStock: Long,
    /** 해당 강화 단계의 구매 대기 수량. */
    val buyOrders: Long,
    /** 가격대별 판매/구매 주문. 가격 내림차순. */
    val orderBook: List<MarketBiddingInfo> = emptyList(),
    /** 데이터 갱신 시각 (UTC). */
    val updatedAt: ZonedDateTime,
)
