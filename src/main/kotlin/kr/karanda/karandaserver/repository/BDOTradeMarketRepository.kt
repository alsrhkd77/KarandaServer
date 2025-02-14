package kr.karanda.karandaserver.repository

import jakarta.annotation.PostConstruct
import kr.karanda.karandaserver.data.MarketWaitItem
import kr.karanda.karandaserver.data.MarketItem
import kr.karanda.karandaserver.data.TradeMarketProperties
import kr.karanda.karandaserver.exception.BDOApiNotAvailable
import org.springframework.http.HttpRequest
import org.springframework.stereotype.Repository
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClient.RequestHeadersSpec.ConvertibleClientHttpResponse
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.math.min

@Repository
class BDOTradeMarketRepository(private val defaultDataRepository: DefaultDataRepository) {

    val properties:TradeMarketProperties
        get() = defaultDataRepository.getTradeMarketProperties()
    private lateinit var client:RestClient

    @PostConstruct
    fun initialize(){
        client = RestClient.builder()
            .baseUrl(properties.api)
            .defaultHeaders { headers ->
                run {
                    for (header in properties.headers) {
                        headers.add(header.key, header.value)
                    }
                }
            }
            .build()
    }

    fun getWaitList(): List<MarketWaitItem> {
        val result = mutableListOf<MarketWaitItem>()
        val response = client.post()
            .uri("/GetWorldMarketWaitList")
            .body(emptyMap<String, String>())
            .exchange { request, response ->
                if (response.statusCode.is2xxSuccessful) {
                    val data = response.bodyTo(BDOResponse::class.java)
                    data!!
                } else {
                    println(request.uri)
                    println(request.headers)
                    throw BDOApiNotAvailable()
                }
            }
        response.toWaitItems().let { result.addAll(it) }
        return result
    }

    fun getSubList(mainKey: Int): List<MarketItem> {
        val result = mutableListOf<MarketItem>()
        val payload = mapOf(
            "keyType" to properties.keyType,
            "mainKey" to mainKey.toString(),
        )
        val response = client.post()
            .uri("/GetWorldMarketSubList")
            .body(payload)
            .exchange { request, response ->
                if (response.statusCode.is2xxSuccessful) {
                    val data = response.bodyTo(BDOResponse::class.java)
                    data!!
                } else {
                    throw BDOApiNotAvailable()
                }
            }
        response.toSubListItems().let { result.addAll(it) }
        return result
    }

    fun getSearchList(items: List<String>): List<MarketItem> {
        if (items.isEmpty()) throw Exception("Item list is empty")
        val result = mutableListOf<MarketItem>()

        for(i in 0 until (items.size / 120) + 1) {
            result.addAll(searchList(items.subList(i * 120, min(i * 120 + 120, items.size))))
        }

        return result
    }

    private fun searchList(items: List<String>): List<MarketItem> {
        if (items.isEmpty()) throw Exception("Item list is empty")
        val result = mutableListOf<MarketItem>()
        val payload = mapOf(
            "keyType" to properties.keyType,
            "searchResult" to items.joinToString(separator = ","),
        )
        val response = client.post()
            .uri("/GetWorldMarketSearchList")
            .body(payload)
            .exchange { request, response -> exchange(request, response) }
        response.toSearchListItems().let { result.addAll(it) }
        return result
    }

    fun getPriceInfo(mainKey: Int, subKey: Int = 0): List<Long> {
        val result = mutableListOf<Long>()
        val payload = mapOf(
            "keyType" to properties.keyType,
            "mainKey" to mainKey.toString(),
            "subKey" to subKey.toString(),
        )
        val response = client.post()
            .uri("/GetMarketPriceInfo")
            .body(payload)
            .exchange { request, response -> exchange(request, response) }
        response.toPriceInfo().let { result.addAll(it) }
        return result
    }

    private fun exchange(request: HttpRequest, response: ConvertibleClientHttpResponse): BDOResponse {
        if (response.statusCode.is2xxSuccessful) {
            val data = response.bodyTo(BDOResponse::class.java)
            return data!!
        } else {
            throw BDOApiNotAvailable()
        }
    }
}

private data class BDOResponse(val resultCode: Int, val resultMsg: String) {
    fun toWaitItems(): List<MarketWaitItem> {
        val result = mutableListOf<MarketWaitItem>()
        try {
            for (line in resultMsg.trim().split("|")) {
                if(line.isEmpty()) continue
                val item = line.split("-")
                val epochSeconds = item[3].toLong()
                result.add(
                    MarketWaitItem(
                        itemNum = item[0].toInt(),
                        enhancementLevel = item[1].toInt(),
                        price = item[2].toLong(),
                        targetTime = Instant.ofEpochSecond(epochSeconds).atZone(ZoneId.of("Asia/Seoul")),
                    )
                )
            }
        } catch (e: Exception) {
            println(e)
            println("status code: $resultCode")
            println(resultMsg)
        }
        return result
    }

    fun toSubListItems(): List<MarketItem> {
        val result = mutableListOf<MarketItem>()
        for (line in resultMsg.trim().split("|")) {
            try {
                if(line.isEmpty()) continue
                val item = line.split("-")
                result.add(
                    MarketItem(
                        itemNum = item[0].toInt(),
                        enhancementLevel = item[1].toInt(),
                        price = item[3].toLong(),
                        currentStock = item[4].toLong(),
                        cumulativeVolume = item[5].toLong(),
                        date = ZonedDateTime.now(ZoneId.of("Asia/Seoul"))
                    )
                )
            } catch (e: Exception) {
                println(e)
                println(line)
            }
        }
        return result
    }

    fun toSearchListItems(): List<MarketItem> {
        val result = mutableListOf<MarketItem>()
        for (line in resultMsg.trim().split("|")) {
            if(line.isEmpty()) continue
            val item = line.split("-")
            result.add(
                MarketItem(
                    itemNum = item[0].toInt(),
                    currentStock = item[1].toLong(),
                    price = item[2].toLong(),
                    cumulativeVolume = item[3].toLong(),
                    date = ZonedDateTime.now(ZoneId.of("Asia/Seoul"))
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
