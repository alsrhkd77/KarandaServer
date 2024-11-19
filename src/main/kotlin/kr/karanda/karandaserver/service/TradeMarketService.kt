package kr.karanda.karandaserver.service

import kr.karanda.karandaserver.data.MarketItem
import kr.karanda.karandaserver.data.MarketWaitItem
import kr.karanda.karandaserver.dto.BDOItem
import kr.karanda.karandaserver.entity.MarketData
import kr.karanda.karandaserver.repository.BDOItemRepository
import kr.karanda.karandaserver.repository.BDOTradeMarketRepository
import kr.karanda.karandaserver.repository.MarketDataRepository
import kr.karanda.karandaserver.util.difference
import kr.karanda.karandaserver.util.isSameDayAs
import kr.karanda.karandaserver.util.toMidnight
import org.springframework.cache.annotation.Cacheable
import org.springframework.context.annotation.DependsOn
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import java.time.ZoneId
import java.time.ZonedDateTime

@Service
@DependsOn("TradeMarketAsyncService")
class TradeMarketService(
    val asyncService: TradeMarketAsyncService,
    val bdoTradeMarketRepository: BDOTradeMarketRepository,
    val bdoItemRepository: BDOItemRepository,
    val marketDataRepository: MarketDataRepository
) {

    @Cacheable(cacheNames = ["TradeMarketWaitList"])
    fun getWaitList(): List<MarketWaitItem> {
        return bdoTradeMarketRepository.getWaitList()
    }

    fun getLatest(target: List<Int>): List<MarketItem> {
        val itemData = getTradeAbleItems(target)
        val now = ZonedDateTime.now(ZoneId.of("Asia/Seoul"))
        val itemsNeedInit = mutableListOf<BDOItem>()
        val itemsNeedUpdate = mutableListOf<BDOItem>()
        val entitiesNeedUpdate = mutableListOf<MarketData>()

        val data =
            marketDataRepository.findAllByItemNumIsInAndDateIsAfter(itemData.map { it.itemNum }, now.toMidnight())
                .toMutableList()

        if (data.isEmpty()) {
            itemsNeedInit.addAll(itemData)
        } else {
            val grouped = data.groupBy { it.itemNum }
            for (item in itemData) {
                if (!grouped.containsKey(item.itemNum)) {
                    itemsNeedInit.add(item)
                }
            }
            for (key in grouped.keys) {
                for (price in grouped[key]!!) {
                    if (now.difference(price.date).toMinutes() > 15) {
                        itemsNeedUpdate.add(itemData.single { it.itemNum == price.itemNum })
                        entitiesNeedUpdate.addAll(grouped[key]!!)
                        data.removeAll(grouped[key]!!)
                        break
                    }
                }
            }
        }

        val result = data.map { it.toDTO() }.toMutableList()

        if (itemsNeedInit.isNotEmpty() || itemsNeedUpdate.isNotEmpty()) {
            val init = mutableListOf<MarketItem>()
            val enhanceAbleItems =
                (itemsNeedInit + itemsNeedUpdate).filter { it.maxEnhancementLevel == 0 }.map { it.itemNum.toString() }
            val nonEnhanceAbleItems =
                (itemsNeedInit + itemsNeedUpdate).filter { it.maxEnhancementLevel != 0 }.map { it.itemNum }
            val latest = mutableListOf<MarketItem>()

            if (enhanceAbleItems.isNotEmpty()) {
                latest.addAll(bdoTradeMarketRepository.getSearchList(items = enhanceAbleItems))
            }
            if (nonEnhanceAbleItems.isNotEmpty()) {
                for (key in nonEnhanceAbleItems) {
                    latest.addAll(bdoTradeMarketRepository.getSubList(mainKey = key))
                }
            }

            for (item in latest) {
                if (itemsNeedInit.any { bdoItem -> bdoItem.itemNum == item.itemNum }) {
                    init.add(item)
                } else {
                    entitiesNeedUpdate.find { bdoItem -> bdoItem.itemNum == item.itemNum && bdoItem.enhancementLevel == item.enhancementLevel }
                        ?.let { marketData ->
                            marketData.date = now
                            marketData.price = item.price
                            marketData.cumulativeVolume = item.cumulativeVolume
                            marketData.currentStock = item.currentStock
                        }
                }
            }

            if (init.isNotEmpty()) {
                asyncService.createMarketData(init)
                result.addAll(init)
            }

            if (entitiesNeedUpdate.isNotEmpty()) {
                asyncService.updateMarketData(entitiesNeedUpdate)
                result.addAll(entitiesNeedUpdate.map { it.toDTO() })
            }
        }

        return result
    }

    fun getMarketDataList(itemNum: Int): List<MarketItem> {
        getTradeAbleItem(itemNum)
        val now = ZonedDateTime.now(ZoneId.of("Asia/Seoul"))
        val data = marketDataRepository.findAllByItemNumOrderByDateDesc(itemNum = itemNum)
        if (data.isEmpty() || !now.isSameDayAs(data.first().date)) {    //데이터가 없거나 날짜가 지남
            val result = data.map { it.toDTO() }.toMutableList()
            val latest = bdoTradeMarketRepository.getSubList(itemNum)
            asyncService.createMarketData(latest)
            result.addAll(latest)
            result.sortBy { it.date }
            return result
        } else if (now.difference(data.first().date).toMinutes() > 15) {    // 15분 이상 지남
            val latestData = bdoTradeMarketRepository.getSubList(itemNum)
            val update = mutableListOf<MarketData>()
            for (latest in latestData) {
                data.find {
                    it.itemNum == latest.itemNum &&
                            it.enhancementLevel == latest.enhancementLevel &&
                            it.date.isSameDayAs(latest.date!!)
                }?.let {
                    it.price = latest.price
                    it.cumulativeVolume = latest.cumulativeVolume
                    it.currentStock = latest.currentStock
                    it.date = latest.date!!
                    update.add(it)
                }
            }
            asyncService.updateMarketData(update)
        }
        return data.map { it.toDTO() }
    }

    /*
    * parameter = updated item id at last time
    * return = updated item id in this time
    */
    fun updateNextItem(lastUpdated: Int): Int {
        val item = bdoItemRepository.findFirstByIdGreaterThanAndTradeAble(id = lastUpdated, tradeAble = true)
            ?: bdoItemRepository.findFirstByTradeAble(tradeAble = true)
        if (item == null) {
            return -1
        }
        val now = ZonedDateTime.now(ZoneId.of("Asia/Seoul"))
        val data = marketDataRepository.findAllByItemNumAndDateIsAfterOrderByDateDesc(itemNum = item.itemNum, date = now.minusDays(95))
            .groupBy { it.enhancementLevel }
            .toMutableMap()
        val create = mutableListOf<MarketData>()
        val update = mutableListOf<MarketData>()

        // update today data
        val latestData = bdoTradeMarketRepository.getSubList(item.itemNum)
        for (latest in latestData) {
            if (!data.containsKey(latest.enhancementLevel)) {
                val newEntity = MarketData(
                    itemNum = item.itemNum,
                    enhancementLevel = latest.enhancementLevel,
                    price = latest.price,
                    cumulativeVolume = latest.cumulativeVolume,
                    currentStock = latest.currentStock,
                    date = latest.date!!
                )
                data[latest.enhancementLevel] = listOf(newEntity)
                create.add(newEntity)
            }
            if (now.isSameDayAs(data[latest.enhancementLevel]?.first()!!.date)) {
                val today = data[latest.enhancementLevel]?.first()!!
                today.cumulativeVolume = latest.cumulativeVolume
                today.currentStock = latest.currentStock
                today.date = latest.date!!
                update.add(today)
            } else {
                val newEntity = MarketData(
                    itemNum = item.itemNum,
                    enhancementLevel = latest.enhancementLevel,
                    price = latest.price,
                    cumulativeVolume = latest.cumulativeVolume,
                    currentStock = latest.currentStock,
                    date = latest.date!!
                )
                create.add(newEntity)
            }
        }

        //update price data
        for(enhancementLevel in data.keys){
            val priceData = bdoTradeMarketRepository.getPriceInfo(mainKey = item.itemNum, subKey = enhancementLevel)
            for(i in 1 until priceData.size){
                val targetDate = now.minusDays(i.toLong())
                val target = data[enhancementLevel]?.find { targetDate.isSameDayAs(it.date) }
                if (target == null) {
                    val near = data[enhancementLevel]?.find { targetDate.isAfter(it.date) }
                    val newEntity = MarketData(
                        itemNum = item.itemNum,
                        enhancementLevel = enhancementLevel,
                        price = priceData[i],
                        cumulativeVolume = near?.cumulativeVolume ?: 0,
                        currentStock = near?.currentStock ?: 0,
                        date = targetDate.toMidnight()
                    )
                    create.add(newEntity)
                } else if(target.price != priceData[i]){
                    target.price = priceData[i]
                    target.date = targetDate.toMidnight()
                    update.add(target)
                }
            }
        }

        marketDataRepository.saveAll(create)
        marketDataRepository.saveAll(update)
        marketDataRepository.flush()

        return item.id!!
    }

    fun getTradeAbleItems(target: List<Int>): List<BDOItem> {
        return bdoItemRepository.findByItemNumIsInAndTradeAble(itemNums = target.distinct(), tradeAble = true)
            .map { it.toDTO() }
    }

    fun getTradeAbleItem(target: Int): BDOItem? {
        return bdoItemRepository.findByItemNumAndTradeAble(itemNum = target, tradeAble = true)?.toDTO()
    }
}

@Service("TradeMarketAsyncService")
class TradeMarketAsyncService(
    val marketDataRepository: MarketDataRepository
) {
    @Async
    fun createMarketData(data: List<MarketItem>) {
        val newEntities = data.map {
            MarketData(
                itemNum = it.itemNum,
                enhancementLevel = it.enhancementLevel,
                price = it.price,
                cumulativeVolume = it.cumulativeVolume,
                currentStock = it.currentStock,
                date = it.date!!
            )
        }
        marketDataRepository.saveAll(newEntities)
    }

    @Async
    fun updateMarketData(data: List<MarketData>) {
        marketDataRepository.saveAll(data)
        marketDataRepository.flush()
    }
}