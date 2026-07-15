package kr.karanda.karandaserver

import kr.karanda.karandaserver.entity.BDOItem
import kr.karanda.karandaserver.enums.BDORegion
import kr.karanda.karandaserver.repository.jpa.BDOItemRepository
import kr.karanda.karandaserver.repository.jpa.MarketPriceDataRepository
import kr.karanda.karandaserver.service.TradeMarketService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * 거래소 온디맨드 캐시(market_price_data) 동작 검증.
 *
 * 외부 라이브 거래소 API를 호출하므로 네트워크가 필요하며,
 * [TradeMarketAPITest]처럼 필요할 때 수동으로 실행하는 것을 권장한다.
 *
 * 검증 항목:
 * 1. /detail 경로: 강화 단계별 90일 가격·구매 대기 수집, (itemNum, level, region)당 단일 행.
 * 2. warm 캐시: stale(15분) 이내 재조회 시 갱신 없이 동일 데이터 반환.
 * 3. /latest 라이트 갱신: 상세 필드(priceHistory·buyOrders·detailUpdatedAt)를 건드리지 않음.
 */
@SpringBootTest
class TradeMarketCacheVerificationTests {

    @Autowired
    private lateinit var tradeMarketService: TradeMarketService

    @Autowired
    private lateinit var marketPriceDataRepository: MarketPriceDataRepository

    @Autowired
    private lateinit var bdoItemRepository: BDOItemRepository

    private val region = BDORegion.KR
    private val enhanceableItem = 10007  // 강화 단계가 있는 아이템 (SubList 다중 그룹)
    private val plainItem = 10210        // 강화 불가 아이템 (SearchList 배치 경로)

    @BeforeEach
    fun seedItems() {
        // dev H2에 아이템 카탈로그가 없을 수 있어 검증 대상만 시드한다.
        seedItem(enhanceableItem, maxEnhancementLevel = 20)
        seedItem(plainItem, maxEnhancementLevel = 0)
    }

    private fun seedItem(itemNum: Int, maxEnhancementLevel: Int) {
        if (bdoItemRepository.findByItemNumAndTradeAbleAndDeletedFalse(itemNum) == null) {
            bdoItemRepository.save(
                BDOItem(itemNum = itemNum, tradeAble = true, maxEnhancementLevel = maxEnhancementLevel)
            )
        }
    }

    @Test
    fun `detail refresh collects per-level data and keeps single row per key`() {
        marketPriceDataRepository.deleteAll(
            marketPriceDataRepository.findAllByItemNumAndRegion(enhanceableItem, region.name)
        )

        val first = tradeMarketService.getPriceDetail(enhanceableItem, region)
        assertTrue(first.isNotEmpty(), "detail response is empty")
        assertTrue(first.all { it.priceHistory.isNotEmpty() }, "priceHistory not collected")
        assertTrue(first.all { it.buyOrders >= 0 }, "buyOrders not collected")

        // (itemNum, enhancementLevel, region)당 단일 행
        val rows = marketPriceDataRepository.findAllByItemNumAndRegion(enhanceableItem, region.name)
        assertEquals(rows.map { it.enhancementLevel }.toSet().size, rows.size, "duplicated rows detected")
        assertTrue(rows.all { it.detailUpdatedAt != null }, "detailUpdatedAt not set")

        // warm 캐시: 15분 이내 재조회는 외부 갱신 없이 동일 시각 반환
        val second = tradeMarketService.getPriceDetail(enhanceableItem, region)
        assertEquals(
            first.map { it.updatedAt.toInstant() },
            second.map { it.updatedAt.toInstant() },
            "warm cache triggered an unexpected refresh",
        )
    }

    @Test
    fun `latest light refresh does not touch detail-only fields`() {
        marketPriceDataRepository.deleteAll(
            marketPriceDataRepository.findAllByItemNumAndRegion(plainItem, region.name)
        )

        val latest = tradeMarketService.getLatestPriceData(listOf(plainItem), region)
        assertTrue(latest.isNotEmpty(), "latest response is empty")

        val row = marketPriceDataRepository.findAllByItemNumAndRegion(plainItem, region.name).first()
        assertTrue(row.basePrice > 0, "basePrice not updated")
        assertEquals(emptyList(), row.priceHistory, "light refresh must not fill priceHistory")
        assertEquals(0L, row.buyOrders, "light refresh must not fill buyOrders")
        assertNull(row.detailUpdatedAt, "light refresh must not set detailUpdatedAt")
    }
}
