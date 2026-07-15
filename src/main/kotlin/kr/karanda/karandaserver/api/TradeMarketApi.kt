package kr.karanda.karandaserver.api

import kr.karanda.karandaserver.dto.MarketBiddingInfo
import kr.karanda.karandaserver.dto.MarketHotItem
import kr.karanda.karandaserver.dto.MarketItem
import kr.karanda.karandaserver.dto.MarketWaitItem
import kr.karanda.karandaserver.dto.ParsedItem
import kr.karanda.karandaserver.dto.properties.TradeMarketProperties
import kr.karanda.karandaserver.enums.BDORegion
import kr.karanda.karandaserver.exception.ExternalApiException
import kr.karanda.karandaserver.exception.InvalidArgumentException
import kr.karanda.karandaserver.repository.DefaultDataProvider
import kr.karanda.karandaserver.util.TradeMarketBinaryDecoder
import kr.karanda.karandaserver.util.WebUtils
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import java.time.Instant
import java.time.ZonedDateTime
import kotlin.math.min

@Component
class TradeMarketApi(private val defaultDataProvider: DefaultDataProvider) {
    val client: RestClient
        get() {
            return RestClient.builder()
                .defaultHeaders { headers ->
                    run {
                        for (header in properties.headers) {
                            headers.add(header.key, header.value)
                        }
                    }
                }
                .build()
        }

    val properties: TradeMarketProperties
        get() = defaultDataProvider.getTradeMarketProperties()

    @Cacheable("TradeMarketWaitList")
    fun getWaitList(region: BDORegion): List<MarketWaitItem> {
        try {
            val uri = getBaseURL(region) + "/GetWorldMarketWaitList"
            val response = client.post()
                .uri(uri)
                .body(emptyMap<String, String>())
                .retrieve()
                .body(TradeMarketResponse::class.java) ?: throw ExternalApiException()
            return response.toWaitItems(region)
        } catch (e: Exception) {
            throw ExternalApiException()
        }
    }

    fun getSubList(mainKey: Int, region: BDORegion): List<MarketItem> {
        val uri = getBaseURL(region) + "/GetWorldMarketSubList"
        val payload = mapOf(
            "keyType" to properties.keyType,
            "mainKey" to mainKey.toString(),
        )
        try {
            val response = client.post()
                .uri(uri)
                .body(payload)
                .retrieve()
                .body(TradeMarketResponse::class.java) ?: throw ExternalApiException()
            return response.toSubListItems(region)
        } catch (e: Exception) {
            throw ExternalApiException()
        }
    }

    fun getSearchList(items: List<String>, region: BDORegion): List<MarketItem> {
        if (items.isEmpty()) return emptyList()
        val result = mutableListOf<MarketItem>()

        for (i in 0 until (items.size / 120) + 1) {
            result.addAll(searchList(items.subList(i * 120, min(i * 120 + 120, items.size)), region))
        }

        return result
    }

    private fun searchList(items: List<String>, region: BDORegion): List<MarketItem> {
        if (items.isEmpty()) return emptyList()
        val uri = getBaseURL(region) + "/GetWorldMarketSearchList"
        val payload = mapOf(
            "keyType" to properties.keyType,
            "searchResult" to items.joinToString(separator = ","),
        )
        try {
            val response = client.post()
                .uri(uri)
                .body(payload)
                .retrieve()
                .body(TradeMarketResponse::class.java) ?: throw ExternalApiException()
            return response.toSearchListItems(region)
        } catch (e: Exception) {
            return emptyList()
        }
    }

    fun getPriceInfo(mainKey: Int, subKey: Int = 0, region: BDORegion): List<Long> {
        val uri = getBaseURL(region) + "/GetMarketPriceInfo"
        val payload = mapOf(
            "keyType" to properties.keyType,
            "mainKey" to mainKey.toString(),
            "subKey" to subKey.toString(),
        )
        try {
            val response = client.post()
                .uri(uri)
                .body(payload)
                .retrieve()
                .body(TradeMarketResponse::class.java) ?: throw ExternalApiException()
            return response.toPriceInfo()
        } catch (e: Exception) {
            throw ExternalApiException()
        }
    }

    /**
     * 거래소 핫 아이템 목록을 조회한다.
     *
     * 응답은 Huffman 압축 바이너리이므로 [TradeMarketBinaryDecoder]로 복원 후 파싱한다.
     */
    fun getHotList(region: BDORegion): List<MarketHotItem> {
        val uri = getBaseURL(region) + "/GetWorldMarketHotList"
        try {
            val response = fetchBinary(uri, emptyMap<String, String>())
            return response.toHotItems(region)
        } catch (e: Exception) {
            throw ExternalApiException()
        }
    }

    /**
     * 카테고리(및 선택적 서브카테고리)에 속한 아이템 목록을 조회한다.
     *
     * 응답은 Huffman 압축 바이너리이므로 [TradeMarketBinaryDecoder]로 복원 후 파싱한다.
     */
    fun getMarketList(mainCategory: Int, subCategory: Int? = null, region: BDORegion): List<MarketItem> {
        val uri = getBaseURL(region) + "/GetWorldMarketList"
        val payload = buildMap {
            put("keyType", properties.keyType)
            put("mainCategory", mainCategory.toString())
            subCategory?.let { put("subCategory", it.toString()) }
        }
        try {
            val response = fetchBinary(uri, payload)
            return response.toMarketListItems(region)
        } catch (e: Exception) {
            throw ExternalApiException()
        }
    }

    /**
     * 특정 아이템·강화단계의 가격대별 호가(매수/매도 주문 수량)를 조회한다.
     *
     * 응답은 Huffman 압축 바이너리이므로 [TradeMarketBinaryDecoder]로 복원 후 파싱한다.
     */
    fun getBiddingInfoList(mainKey: Int, subKey: Int = 0, region: BDORegion): List<MarketBiddingInfo> {
        val uri = getBaseURL(region) + "/GetBiddingInfoList"
        val payload = mapOf(
            "keyType" to properties.keyType,
            "mainKey" to mainKey.toString(),
            "subKey" to subKey.toString(),
        )
        try {
            val response = fetchBinary(uri, payload)
            return response.toBiddingInfo()
        } catch (e: Exception) {
            throw ExternalApiException()
        }
    }

    /**
     * Huffman 압축 바이너리를 반환하는 엔드포인트를 호출해 [TradeMarketResponse]로 복원한다.
     */
    private fun fetchBinary(uri: String, payload: Any): TradeMarketResponse {
        val bytes = client.post()
            .uri(uri)
            .body(payload)
            .retrieve()
            .body(ByteArray::class.java) ?: throw ExternalApiException()
        return TradeMarketResponse(resultCode = 0, resultMsg = TradeMarketBinaryDecoder.decode(bytes))
    }

    /**
     * 아이템 상세페이지에서 메타데이터(한글명·영문명·등급)를 파싱한다.
     * 파싱 실패 시 예외를 전파한다(호출부에서 처리).
     */
    fun fetchItemMetadata(itemNum: Int): ParsedItem {
        return WebUtils().getItemMetadata(url = "${properties.parse}${itemNum}/")
    }

    private fun getBaseURL(region: BDORegion): String {
        return when(region) {
            BDORegion.KR -> properties.kr
            BDORegion.NA -> properties.na
            BDORegion.EU -> properties.eu
            BDORegion.JP -> properties.jp
            // LAB은 거래소 미서비스 리전 (뉴스 전용)
            BDORegion.LAB -> throw InvalidArgumentException()
        }
    }
}

private data class TradeMarketResponse(val resultCode: Int, val resultMsg: String) {
    fun toWaitItems(region: BDORegion): List<MarketWaitItem> {
        val result = mutableListOf<MarketWaitItem>()
        for (line in resultMsg.trim().split("|")) {
            if (line.isEmpty()) continue
            val item = line.split("-")
            val epochSeconds = item[3].toLong()
            result.add(
                MarketWaitItem(
                    itemNum = item[0].toInt(),
                    enhancementLevel = item[1].toInt(),
                    price = item[2].toLong(),
                    targetTime = Instant.ofEpochSecond(epochSeconds).atZone(region.timezone),
                )
            )
        }
        return result
    }

    fun toSubListItems(region: BDORegion): List<MarketItem> {
        val result = mutableListOf<MarketItem>()
        for (line in resultMsg.trim().split("|")) {
            if (line.isEmpty()) continue
            val item = line.split("-")
            result.add(
                MarketItem(
                    itemNum = item[0].toInt(),
                    enhancementLevel = item[1].toInt(),
                    price = item[3].toLong(),
                    currentStock = item[4].toLong(),
                    cumulativeVolume = item[5].toLong(),
                    date = ZonedDateTime.now(region.timezone)
                )
            )
        }
        return result
    }

    fun toSearchListItems(region: BDORegion): List<MarketItem> {
        val result = mutableListOf<MarketItem>()
        for (line in resultMsg.trim().split("|")) {
            if (line.isEmpty()) continue
            val item = line.split("-")
            result.add(
                MarketItem(
                    itemNum = item[0].toInt(),
                    currentStock = item[1].toLong(),
                    price = item[2].toLong(),
                    cumulativeVolume = item[3].toLong(),
                    date = ZonedDateTime.now(region.timezone)
                )
            )
        }
        return result
    }

    fun toPriceInfo(): List<Long> {
        // 첫 번째 값이 오늘이 되도록 Reverse
        return resultMsg.trim().split("-").reversed().map { it.toLong() }
    }

    fun toHotItems(region: BDORegion): List<MarketHotItem> {
        val result = mutableListOf<MarketHotItem>()
        for (line in resultMsg.trim().split("|")) {
            if (line.isEmpty()) continue
            val item = line.split("-")
            result.add(
                MarketHotItem(
                    itemNum = item[0].toInt(),
                    minEnhancementLevel = item[1].toInt(),
                    maxEnhancementLevel = item[2].toInt(),
                    basePrice = item[3].toLong(),
                    currentStock = item[4].toLong(),
                    cumulativeVolume = item[5].toLong(),
                    priceChangeDirection = item[6].toInt(),
                    priceChangeValue = item[7].toLong(),
                    priceCapMin = item[8].toLong(),
                    priceCapMax = item[9].toLong(),
                    lastSalePrice = item[10].toLong(),
                    lastSaleTime = Instant.ofEpochSecond(item[11].toLong()).atZone(region.timezone),
                )
            )
        }
        return result
    }

    fun toMarketListItems(region: BDORegion): List<MarketItem> {
        // 0: 아이템 ID, 1: 재고, 2: 누적 거래량, 3: 기준가
        val result = mutableListOf<MarketItem>()
        for (line in resultMsg.trim().split("|")) {
            if (line.isEmpty()) continue
            val item = line.split("-")
            result.add(
                MarketItem(
                    itemNum = item[0].toInt(),
                    currentStock = item[1].toLong(),
                    cumulativeVolume = item[2].toLong(),
                    price = item[3].toLong(),
                    date = ZonedDateTime.now(region.timezone)
                )
            )
        }
        return result
    }

    fun toBiddingInfo(): List<MarketBiddingInfo> {
        // 0: 가격, 1: 판매 주문 수량, 2: 구매 주문 수량
        val result = mutableListOf<MarketBiddingInfo>()
        for (line in resultMsg.trim().split("|")) {
            if (line.isEmpty()) continue
            val item = line.split("-")
            result.add(
                MarketBiddingInfo(
                    price = item[0].toLong(),
                    sellOrders = item[1].toLong(),
                    buyOrders = item[2].toLong(),
                )
            )
        }
        return result
    }
}