package kr.karanda.karandaserver.api

import kr.karanda.karandaserver.dto.MarketItem
import kr.karanda.karandaserver.dto.MarketWaitItem
import kr.karanda.karandaserver.dto.TradeMarketProperties
import kr.karanda.karandaserver.enums.BDORegion
import kr.karanda.karandaserver.exception.ExternalApiException
import kr.karanda.karandaserver.repository.DefaultDataRepository
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import java.time.Instant
import java.time.ZonedDateTime
import kotlin.math.min

@Component
class TradeMarketApi(private val defaultDataRepository: DefaultDataRepository) {
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
        get() = defaultDataRepository.getTradeMarketProperties()

    @Cacheable(cacheNames = ["TradeMarketWaitList"])
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

    private fun getBaseURL(region: BDORegion): String {
        return when(region) {
            BDORegion.KR -> properties.kr
            BDORegion.NA -> properties.na
            BDORegion.EU -> properties.eu
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
}