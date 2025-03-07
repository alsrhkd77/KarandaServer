package kr.karanda.karandaserver.service

import com.sun.org.slf4j.internal.Logger
import com.sun.org.slf4j.internal.LoggerFactory
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kr.karanda.karandaserver.api.TradeMarketApi
import kr.karanda.karandaserver.dto.MarketItem
import kr.karanda.karandaserver.dto.MarketWaitItem
import kr.karanda.karandaserver.dto.BDOItem
import kr.karanda.karandaserver.entity.MarketData
import kr.karanda.karandaserver.enums.BDORegion
import kr.karanda.karandaserver.exception.InvalidArgumentException
import kr.karanda.karandaserver.repository.jpa.BDOItemRepository
import kr.karanda.karandaserver.repository.SynchronizationDataRepository
import kr.karanda.karandaserver.repository.jpa.MarketDataRepository
import kr.karanda.karandaserver.util.difference
import kr.karanda.karandaserver.util.isSameDayAs
import kr.karanda.karandaserver.util.toMidnight
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import java.time.ZonedDateTime

@Service
class TradeMarketService(
    val tradeMarketApi: TradeMarketApi,
    val bdoItemRepository: BDOItemRepository,
    val marketDataRepository: MarketDataRepository,
    val synchronizationDataRepository: SynchronizationDataRepository,
    val messagingTemplate: SimpMessagingTemplate
) {

    val logger: Logger = LoggerFactory.getLogger(TradeMarketService::class.java)

    fun getWaitList(region: BDORegion): List<MarketWaitItem> {
        return tradeMarketApi.getWaitList(region)
    }

    @Async
    fun publishWaitList() {
        for (region in BDORegion.entries) {
            try {
                tradeMarketApi.getWaitList(region).let {
                    messagingTemplate.convertAndSend(
                        "/live-data/trade-market/${region.name}/wait-list",
                        Json.encodeToString(it)
                    )
                }
            } catch (e: Exception) {
                logger.error("Fetching a waiting item failed #${region.name}", e)
            }
        }
    }

    fun updateLatestPriceData() {
        val lastUpdated = synchronizationDataRepository.getTradeMarketLastUpdated()
        val item = bdoItemRepository.findFirstByIdGreaterThanAndTradeAble(id = lastUpdated, tradeAble = true)?.toDTO()
            ?: bdoItemRepository.findFirstByTradeAble(tradeAble = true)?.toDTO()
        if (item != null) {
            for (region in BDORegion.entries) {
                try {
                    updateLatestPrice(item = item, region = region)
                } catch (e: Exception) {
                    logger.error("Updating latest price failed #${region.name}", e)
                }
            }
            synchronizationDataRepository.setTradeMarketLastUpdated(item.itemNum)
        } else {
            synchronizationDataRepository.setTradeMarketLastUpdated(-1)
        }
    }

    @Async
    fun updateLatestPrice(item: BDOItem, region: BDORegion) {
        val now = ZonedDateTime.now(region.timezone)
        val data = marketDataRepository.findAllByItemNumAndRegionAndDateIsAfterOrderByDateDesc(
            itemNum = item.itemNum,
            region = region.name,
            date = now.toMidnight()
        ).toMutableList()
        val priceData = tradeMarketApi.getSubList(item.itemNum, region)
        for (latest in priceData) {
            data.find { it.enhancementLevel == latest.enhancementLevel }?.apply {
                this.cumulativeVolume = latest.cumulativeVolume
                this.currentStock = latest.currentStock
                this.price = latest.price
                this.date = now
            } ?: data.add(
                MarketData(
                    itemNum = item.itemNum,
                    enhancementLevel = latest.enhancementLevel,
                    price = latest.price,
                    cumulativeVolume = latest.cumulativeVolume,
                    currentStock = latest.currentStock,
                    date = now,
                    region = region.name,
                )
            )
        }
        marketDataRepository.saveAll(data)
    }

    fun updateHistoricalPriceData() {
        val lastUpdated = synchronizationDataRepository.getTradeMarketPriceLastUpdated()
        val item = bdoItemRepository.findFirstByIdGreaterThanAndTradeAble(id = lastUpdated, tradeAble = true)?.toDTO()
            ?: bdoItemRepository.findFirstByTradeAble(tradeAble = true)?.toDTO()
        if (item != null) {
            for (region in BDORegion.entries) {
                updateHistoricalPrice(item = item, region = region)
            }
            synchronizationDataRepository.setTradeMarketPriceLastUpdated(item.itemNum)
        } else {
            synchronizationDataRepository.setTradeMarketPriceLastUpdated(-1)
        }
    }

    @Async
    fun updateHistoricalPrice(item: BDOItem, region: BDORegion) {
        val now = ZonedDateTime.now(region.timezone)
        val data: MutableMap<Int, MutableList<MarketData>> = mutableMapOf()
        marketDataRepository.findAllByItemNumAndRegionAndDateIsAfterOrderByDateDesc(
            itemNum = item.itemNum,
            region = region.name,
            date = now.minusDays(95)
        ).groupByTo(data) { it.enhancementLevel }
        for (enhancementLevel in data.keys) {
            val priceData = tradeMarketApi.getPriceInfo(item.itemNum, enhancementLevel, region)
            for (index in 1 until priceData.size) {
                val targetDate = now.minusDays(index.toLong()).toMidnight()
                val target = data[enhancementLevel]?.find { it.date.isSameDayAs(targetDate) }
                if (target == null) {
                    val near = data[enhancementLevel]?.find { it.date.isBefore(targetDate) }
                    data[enhancementLevel]?.add(
                        MarketData(
                            itemNum = item.itemNum,
                            enhancementLevel = enhancementLevel,
                            price = priceData[index],
                            cumulativeVolume = near?.cumulativeVolume ?: 0,
                            currentStock = near?.currentStock ?: 0,
                            date = targetDate,
                            region = region.name
                        )
                    )
                } else if (target.price != priceData[index]) {
                    target.price = priceData[index]
                    target.date = targetDate
                }
            }

        }
        marketDataRepository.saveAll(data.values.flatten())
    }

    fun getLatestPriceData(target: List<Int>, region: BDORegion): List<MarketItem> {
        val now = ZonedDateTime.now(region.timezone)
        val items = bdoItemRepository.findAllByItemNumIsInAndTradeAble(target, tradeAble = true).map { it.toDTO() }
        val data: MutableMap<Int, MutableList<MarketData>> = mutableMapOf()
        marketDataRepository.findAllByItemNumIsInAndRegionAndDateIsAfter(
            itemNums = items.map { it.itemNum },
            region = region.name,
            date = now.toMidnight()
        ).groupByTo(data) { it.itemNum }

        val needUpdateItems = items.filter {
            !data.containsKey(it.itemNum) || data[it.itemNum]?.any { item ->
                now.difference(item.date).toMinutes() > 15
            } ?: false
        }

        val latestData: MutableList<MarketItem> =
            tradeMarketApi.getSearchList(items = needUpdateItems.filter { it.maxEnhancementLevel == 0 }
                .map { it.itemNum.toString() }, region = region).toMutableList()

        for (item in needUpdateItems.filter { it.maxEnhancementLevel != 0 }) {
            latestData.addAll(tradeMarketApi.getSubList(mainKey = item.itemNum, region = region))
        }

        for (latest in latestData) {
            if (!data.containsKey(latest.itemNum)) {
                data[latest.itemNum] = mutableListOf()
            }
            data[latest.itemNum]?.find {
                it.enhancementLevel == latest.enhancementLevel
            }?.apply {
                price = latest.price
                cumulativeVolume = latest.cumulativeVolume
                currentStock = latest.currentStock
                date = now
            } ?: data[latest.itemNum]?.add(
                MarketData(
                    itemNum = latest.itemNum,
                    enhancementLevel = latest.enhancementLevel,
                    price = latest.price,
                    cumulativeVolume = latest.cumulativeVolume,
                    currentStock = latest.currentStock,
                    date = now,
                    region = region.name
                )
            )
        }

        return marketDataRepository.saveAll(data.values.flatten()).map { it.toDTO() }
    }

    fun getPriceDetail(itemNum: Int, region: BDORegion): List<MarketItem> {
        val now = ZonedDateTime.now(region.timezone)
        val item = bdoItemRepository.findByItemNumAndTradeAble(itemNum = itemNum, tradeAble = true)?.toDTO()
            ?: throw InvalidArgumentException()
        val data = marketDataRepository.findAllByItemNumAndRegionOrderByDateDesc(item.itemNum, region.name)
            .toMutableList()

        if (data.isEmpty() || !now.isSameDayAs(data.first().date)) {    //데이터가 없거나 날짜가 지남
            tradeMarketApi.getSubList(item.itemNum, region).map {
                MarketData(
                    itemNum = it.itemNum,
                    enhancementLevel = it.enhancementLevel,
                    currentStock = it.currentStock,
                    cumulativeVolume = it.cumulativeVolume,
                    price = it.price,
                    date = now,
                    region = region.name
                )
            }.let { data.addAll(it) }
            marketDataRepository.saveAll(data)
        } else if (now.difference(data.first().date).toMinutes() > 15) {    //15분 이상 지남
            val latestData = tradeMarketApi.getSubList(item.itemNum, region)
            for (latest in latestData) {
                data.find { now.isSameDayAs(it.date) && it.enhancementLevel == latest.enhancementLevel }?.apply {
                    currentStock = latest.currentStock
                    cumulativeVolume = latest.cumulativeVolume
                    price = latest.price
                    date = now
                }
            }
        }

        return data.map { it.toDTO() }
    }
}
