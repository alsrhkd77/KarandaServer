package kr.karanda.karandaserver.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import kr.karanda.karandaserver.dto.MarketItem
import kr.karanda.karandaserver.dto.MarketWaitItem
import kr.karanda.karandaserver.enums.BDORegion
import kr.karanda.karandaserver.service.TradeMarketService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.*

@Tag(name = "Trade market", description = "Trade market API")
@RestController
@RequestMapping("/trade-market")
class TradeMarketController(val tradeMarketService: TradeMarketService) {

    @GetMapping("/wait-list")
    @Operation(summary = "Get a list of products waiting to be listed on the trade market")
    fun getWaitList(@RequestParam(name = "region", required = true) region: String): List<MarketWaitItem> {
        return tradeMarketService.getWaitList(BDORegion.valueOf(region.uppercase(Locale.getDefault())))
    }

    @GetMapping("/latest")
    @Operation(summary = "Get the latest pricing information for requested items")
    fun getLatest(
        @RequestParam(name = "target_list", required = true) target: List<String>,
        @RequestParam(name = "region", required = true) region: String
    ): List<MarketItem> {
        if (target.isEmpty() || target.size > 100) throw Exception()
        val targetItems = mutableListOf<Set<String>>()
        val itemNumList = mutableListOf<Int>()

        for (targetItem in target) {
            targetItem.split("_").let {
                itemNumList.add(it.first().toInt())
                targetItems.add(setOf(it.first(), it.last()))
            }
        }
        val data = tradeMarketService.getLatestPriceData(
            target = itemNumList,
            region = BDORegion.valueOf(region.uppercase(Locale.getDefault()))
        )
        return data.filter { targetItems.contains(setOf(it.itemNum.toString(), it.enhancementLevel.toString())) }
    }

    @GetMapping("/detail")
    @Operation(summary = "Get pricing details for a requested item")
    fun getDetail(
        @RequestParam(name = "code", required = true) itemCode: Int,
        @RequestParam(name = "region", required = true) region: String
    ): List<MarketItem> {
        return tradeMarketService.getPriceDetail(
            itemNum = itemCode,
            region = BDORegion.valueOf(region.uppercase(Locale.getDefault()))
        )
    }
}