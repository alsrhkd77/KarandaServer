package kr.karanda.karandaserver.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import kr.karanda.karandaserver.dto.MarketItem
import kr.karanda.karandaserver.dto.MarketItemDetail
import kr.karanda.karandaserver.dto.MarketWaitItem
import kr.karanda.karandaserver.enums.BDORegion
import kr.karanda.karandaserver.exception.InvalidArgumentException
import kr.karanda.karandaserver.service.TradeMarketService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.*

// 거래소 조회 API는 SecurityConfiguration whitePath에 포함돼 사용자 로그인(Authorization)이
// 필요 없다. 다만 QualificationFilter는 이 경로를 통과시키지 않으므로 production에서는
// Qualification 헤더가 여전히 필요하다 → 전역 보안 요구를 Qualifications 하나로 좁힌다.
@Tag(name = "Trade market", description = "Trade market API")
@SecurityRequirement(name = "Qualifications")
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
        // 잘못된 요청이므로 500이 아니라 400으로 응답한다.
        if (target.isEmpty() || target.size > MAX_LATEST_ITEMS) throw InvalidArgumentException()
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

    companion object {
        /** 한 번에 조회할 수 있는 아이템 수. 클라이언트의 템플릿 배치 조회가 이 값에 맞춰져 있다. */
        const val MAX_LATEST_ITEMS = 100
    }

    @GetMapping("/detail")
    @Operation(summary = "Get pricing details for a requested item")
    fun getDetail(
        @RequestParam(name = "code", required = true) itemCode: Int,
        @RequestParam(name = "region", required = true) region: String
    ): List<MarketItemDetail> {
        return tradeMarketService.getPriceDetail(
            itemNum = itemCode,
            region = BDORegion.valueOf(region.uppercase(Locale.getDefault()))
        )
    }
}