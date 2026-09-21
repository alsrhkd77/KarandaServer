package kr.karanda.karandaserver.util

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kr.karanda.karandaserver.dto.MarketBiddingInfo

/**
 * `List<MarketBiddingInfo>` ↔ JSON 문자열 변환기.
 *
 * `market_price_data.order_book`처럼 항상 통째로 읽고 쓰는 호가 목록 컬럼에 사용한다.
 * [LongListJsonConverter]와 같은 이유로 별도 테이블을 두지 않는다 —
 * 가격대별 행을 개별 조회하거나 갱신할 일이 없다.
 */
@Converter
class BiddingInfoListJsonConverter : AttributeConverter<List<MarketBiddingInfo>, String> {

    private val serializer = ListSerializer(MarketBiddingInfo.serializer())

    override fun convertToDatabaseColumn(attribute: List<MarketBiddingInfo>?): String {
        return Json.encodeToString(serializer, attribute ?: emptyList())
    }

    override fun convertToEntityAttribute(dbData: String?): List<MarketBiddingInfo> {
        if (dbData.isNullOrBlank()) return emptyList()
        return Json.decodeFromString(serializer, dbData)
    }
}
