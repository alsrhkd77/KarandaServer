package kr.karanda.karandaserver.enums

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.ZoneId

@Serializable
enum class BDORegion(val timezone: ZoneId) {
    @SerialName("KR")
    KR(ZoneId.of("Asia/Seoul")),

    @SerialName("NA")
    NA(ZoneId.of("America/Los_Angeles")),

    @SerialName("EU")
    EU(ZoneId.of("Europe/Berlin")),

    @SerialName("JP")
    JP(ZoneId.of("Asia/Tokyo")),

    @SerialName("LAB")
    LAB(ZoneId.of("Asia/Seoul"));

    companion object {
        /**
         * 거래소가 서비스되는 리전.
         *
         * JP는 뉴스 전용으로 추가된 리전이고, LAB(연구소)은 실제 게임 리전이 아니므로
         * 거래소 등 리전 순회 로직에서는 `entries` 대신 이 목록을 사용해야 한다.
         */
        val tradeMarketRegions = listOf(KR, NA, EU)
    }
}
