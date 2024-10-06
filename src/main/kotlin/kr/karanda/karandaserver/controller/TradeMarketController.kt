package kr.karanda.karandaserver.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import kr.karanda.karandaserver.data.MarketItem
import kr.karanda.karandaserver.data.MarketWaitItem
import kr.karanda.karandaserver.service.TradeMarketService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Trade market", description = "Trade market API")
@RestController
@RequestMapping("/trade-market")
class TradeMarketController(val tradeMarketService: TradeMarketService) {

    @GetMapping("/wait-list")
    @Operation(summary = "Get a list of products waiting to be listed on the trade market")
    fun getWaitList(): List<MarketWaitItem> {
        return tradeMarketService.getWaitList()
    }

    @GetMapping("/latest")
    @Operation(summary = "Get the latest pricing information for requested items ")
    fun getLatest(@RequestParam(name = "target_list", required = true) target: List<String>): List<MarketItem> {
        if(target.isEmpty() || target.size > 100) throw Exception()
        val targetItems = mutableListOf<Set<String>>()
        val itemNumList = mutableListOf<Int>()

        for (targetItem in target) {
            targetItem.split("_").let {
                itemNumList.add(it.first().toInt())
                targetItems.add(setOf(it.first(), it.last()))
            }
        }
        val data = tradeMarketService.getLatest(target = itemNumList)
        return data.filter { targetItems.contains(setOf(it.itemNum.toString(), it.enhancementLevel.toString())) }
    }

    @GetMapping("/detail/{itemCode}")
    @Operation(summary = "Get pricing details for a requested item ")
    fun getDetail(@PathVariable itemCode: Int): List<MarketItem> {
        return tradeMarketService.getMarketDataList(itemNum = itemCode)
    }
}