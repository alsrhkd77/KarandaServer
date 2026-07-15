package kr.karanda.karandaserver.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import kr.karanda.karandaserver.dto.BdoNewsDTO
import kr.karanda.karandaserver.dto.BdoNewsHighlightsDTO
import kr.karanda.karandaserver.dto.BdoNewsNotifyRequest
import kr.karanda.karandaserver.dto.BdoNewsNotifyResponse
import kr.karanda.karandaserver.enums.BDORegion
import kr.karanda.karandaserver.enums.BdoNewsCategory
import kr.karanda.karandaserver.exception.InvalidArgumentException
import kr.karanda.karandaserver.service.BdoNewsService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.Locale

@Tag(name = "BDO News", description = "BDO news API")
@RestController
@RequestMapping("/bdo-news")
class BdoNewsController(private val bdoNewsService: BdoNewsService) {

    @GetMapping
    @Operation(summary = "Get news list (sorted by published date, descending)")
    fun getNews(
        @RequestParam(name = "region", required = true) region: String,
        @RequestParam(name = "category", required = false) category: String?,
    ): List<BdoNewsDTO> {
        return bdoNewsService.getNews(
            region = parseRegion(region),
            category = category?.let { parseCategory(it) },
        )
    }

    @GetMapping("/highlights")
    @Operation(summary = "Get the latest major update & lab update for the home screen")
    fun getHighlights(
        @RequestParam(name = "region", required = true) region: String,
    ): BdoNewsHighlightsDTO {
        return bdoNewsService.getHighlights(parseRegion(region))
    }

    /**
     * 크롤러 전용 Webhook.
     *
     * 다른 뉴스 API(Qualification)와 달리 `BdoNewsWebhookAuthFilter`(utilKey Bearer 인증)를
     * 적용받는다. 인증 분리는 필터가 경로 단위로 처리하므로 같은 컨트롤러에 두어도 무방하며,
     * Swagger에서는 메서드 레벨 `@Tag`로 별도 그룹으로 노출한다.
     */
    @Tag(name = "BDO News Webhook", description = "Crawler-only webhook (utilKey bearer auth)")
    @PostMapping("/notify")
    @Operation(summary = "Receive created/updated news from the crawler")
    fun notify(@RequestBody request: BdoNewsNotifyRequest): BdoNewsNotifyResponse {
        val response = bdoNewsService.processNotify(request)
        // 응답 반환 후 백그라운드에서 보존 기간이 지난 항목 정리 (@Async)
        bdoNewsService.cleanupOldNews()
        return response
    }

    private fun parseRegion(value: String): BDORegion {
        return try {
            BDORegion.valueOf(value.uppercase(Locale.getDefault()))
        } catch (e: IllegalArgumentException) {
            throw InvalidArgumentException()
        }
    }

    private fun parseCategory(value: String): BdoNewsCategory {
        return try {
            BdoNewsCategory.valueOf(value.uppercase(Locale.getDefault()))
        } catch (e: IllegalArgumentException) {
            throw InvalidArgumentException()
        }
    }
}
