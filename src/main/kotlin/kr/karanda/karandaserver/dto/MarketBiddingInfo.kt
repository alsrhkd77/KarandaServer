package kr.karanda.karandaserver.dto

import kotlinx.serialization.Serializable

/**
 * GetBiddingInfoList 응답의 개별 호가 정보. 특정 아이템·강화단계의 가격대별 주문 수량이다.
 */
@Serializable
data class MarketBiddingInfo(
    val price: Long,
    /** 해당 가격의 판매 주문 수량. */
    val sellOrders: Long,
    /** 해당 가격의 구매 주문 수량. */
    val buyOrders: Long,
)
