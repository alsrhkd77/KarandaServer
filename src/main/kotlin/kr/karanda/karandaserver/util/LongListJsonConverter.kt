package kr.karanda.karandaserver.util

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

/**
 * `List<Long>` ↔ JSON 문자열 변환기.
 *
 * `market_price_data.price_history`처럼 항상 통째로 읽고 쓰는 숫자 배열 컬럼에 사용한다.
 */
@Converter
class LongListJsonConverter : AttributeConverter<List<Long>, String> {

    override fun convertToDatabaseColumn(attribute: List<Long>?): String {
        return Json.encodeToString(ListSerializer(Long.serializer()), attribute ?: emptyList())
    }

    override fun convertToEntityAttribute(dbData: String?): List<Long> {
        if (dbData.isNullOrBlank()) return emptyList()
        return Json.decodeFromString(ListSerializer(Long.serializer()), dbData)
    }
}
