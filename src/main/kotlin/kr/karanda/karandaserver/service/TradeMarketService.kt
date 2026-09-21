package kr.karanda.karandaserver.service

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kr.karanda.karandaserver.api.TradeMarketApi
import kr.karanda.karandaserver.dto.MarketBiddingInfo
import kr.karanda.karandaserver.dto.MarketItem
import kr.karanda.karandaserver.dto.MarketItemDetail
import kr.karanda.karandaserver.dto.MarketWaitItem
import kr.karanda.karandaserver.entity.MarketPriceData
import kr.karanda.karandaserver.enums.BDORegion
import kr.karanda.karandaserver.exception.InvalidArgumentException
import kr.karanda.karandaserver.repository.jpa.BDOItemRepository
import kr.karanda.karandaserver.repository.jpa.MarketPriceDataRepository
import kr.karanda.karandaserver.util.SingleFlightExecutor
import kr.karanda.karandaserver.util.difference
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.scheduling.annotation.Async
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import org.springframework.stereotype.Service
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionException

/**
 * 거래소 가격 서비스.
 *
 * DB(market_price_data)는 외부 거래소 API 호출을 줄이기 위한 캐시로만 사용한다.
 * 조회 요청 시 저장된 데이터가 stale([STALE_MINUTES] 초과)이면 외부 API로 갱신한다(온디맨드 캐시).
 *
 * - 라이트 갱신(/latest): basePrice·currentStock·cumulativeVolume·updatedAt만 갱신.
 * - 상세 갱신(/detail): 위 필드에 더해 priceHistory(90일)·buyOrders·detailUpdatedAt까지 갱신.
 */
@Service
class TradeMarketService(
    val tradeMarketApi: TradeMarketApi,
    val bdoItemRepository: BDOItemRepository,
    val marketPriceDataRepository: MarketPriceDataRepository,
    val messagingTemplate: SimpMessagingTemplate,
    val singleFlightExecutor: SingleFlightExecutor,
    @param:Qualifier("tradeMarketFetchExecutor") val fetchExecutor: ThreadPoolTaskExecutor,
) {

    val logger: Logger = LoggerFactory.getLogger(TradeMarketService::class.java)

    fun getWaitList(region: BDORegion): List<MarketWaitItem> {
        return tradeMarketApi.getWaitList(region)
    }

    @Async
    fun publishWaitList() {
        for (region in BDORegion.tradeMarketRegions) {
            try {
                tradeMarketApi.getWaitList(region).let {
                    messagingTemplate.convertAndSend(
                        "/live-data/trade-market/${region.name}/wait-list",
                        Json.encodeToString(it)
                    )
                }
            } catch (e: Exception) {
                // 10초 주기로 호출되므로 단발 실패는 흔하다. 연속 실패(=연동 중단) 판정은 GCP 알림 정책의 빈도 임계치가 담당한다.
                // 스택트레이스는 붙이지 않는다. 매 실패마다 남기면 장애가 길어질수록 로그가 폭증하는데,
                // 네트워크 호출 실패의 스택은 항상 같고 실제로 달라지는 정보는 예외 타입·메시지뿐이다.
                logger.warn(
                    "Trade market wait-list fetch failed #${region.name}: ${e::class.simpleName}: ${e.message}"
                )
            }
        }
    }

    /**
     * 요청 아이템들의 최신 가격 정보를 반환한다.
     *
     * updatedAt이 stale인 아이템만 외부 API(SearchList/SubList)로 라이트 갱신한다.
     * priceHistory·buyOrders·detailUpdatedAt은 상세 갱신(/detail) 전용이므로 건드리지 않는다.
     */
    fun getLatestPriceData(target: List<Int>, region: BDORegion): List<MarketItem> {
        val now = ZonedDateTime.now(ZoneId.of("UTC"))
        val items = bdoItemRepository.findAllByItemNumIsInAndTradeAbleAndDeletedFalse(target, tradeAble = true)
            .map { it.toDTO() }
        val itemNums = items.map { it.itemNum }
        val cached = marketPriceDataRepository.findAllByItemNumIsInAndRegion(itemNums, region.name)
            .groupBy { it.itemNum }

        val needUpdateItems = items.filter { item ->
            val rows = cached[item.itemNum]
            rows.isNullOrEmpty() || rows.any { it.updatedAt.difference(now).toMinutes() > STALE_MINUTES }
        }

        val latestData = mutableListOf<MarketItem>()
        latestData.addAll(
            tradeMarketApi.getSearchList(
                items = needUpdateItems.filter { (it.maxEnhancementLevel ?: 0) == 0 }.map { it.itemNum.toString() },
                region = region,
            )
        )
        for (item in needUpdateItems.filter { (it.maxEnhancementLevel ?: 0) != 0 }) {
            latestData.addAll(tradeMarketApi.getSubList(mainKey = item.itemNum, region = region))
        }

        if (latestData.isNotEmpty()) {
            saveWithRetry { mergeLightData(latestData, region, now) }
        }

        return marketPriceDataRepository.findAllByItemNumIsInAndRegion(itemNums, region.name)
            .map { it.toLatestDTO() }
    }

    /**
     * 특정 아이템의 강화 단계별 상세 정보(90일 가격·총 거래량·판매 대기(=재고)·구매 대기·갱신 시각)를 반환한다.
     *
     * detailUpdatedAt이 stale이면 전체 갱신: SubList(단계별 현재가·재고·거래량) +
     * 단계별 PriceInfo(90일 가격) + BiddingInfoList(해당 단계의 구매 대기 수량 합).
     * 외부 호출 실패 시 예외를 전파하고 부분 데이터를 저장하지 않는다(정확성 우선).
     *
     * 동일 아이템에 대한 동시 요청은 [singleFlightExecutor]로 묶어 리더 하나만 외부 갱신하고 결과를 공유한다.
     */
    fun getPriceDetail(itemNum: Int, region: BDORegion): List<MarketItemDetail> {
        val item = bdoItemRepository.findByItemNumAndTradeAbleAndDeletedFalse(itemNum = itemNum, tradeAble = true)?.toDTO()
            ?: throw InvalidArgumentException()
        val rows = marketPriceDataRepository.findAllByItemNumAndRegion(item.itemNum, region.name)
        if (!isDetailStale(rows)) {
            return rows.sortedBy { it.enhancementLevel }.map { it.toDetailDTO() }
        }
        return singleFlightExecutor.execute("detail:${item.itemNum}:${region.name}") {
            refreshPriceDetail(item.itemNum, region)
        }
    }

    private fun isDetailStale(rows: List<MarketPriceData>): Boolean {
        if (rows.isEmpty()) return true
        val now = ZonedDateTime.now(ZoneId.of("UTC"))
        return rows.any { row ->
            val detailUpdatedAt = row.detailUpdatedAt
            detailUpdatedAt == null || detailUpdatedAt.difference(now).toMinutes() > STALE_MINUTES
        }
    }

    /**
     * 상세 정보를 외부 API로 갱신한다. single-flight 리더만 진입한다.
     * 리더 획득을 기다리는 사이 다른 리더가 이미 갱신했으면 재조회 후 캐시를 그대로 사용한다.
     */
    private fun refreshPriceDetail(itemNum: Int, region: BDORegion): List<MarketItemDetail> {
        val now = ZonedDateTime.now(ZoneId.of("UTC"))
        val rows = marketPriceDataRepository.findAllByItemNumAndRegion(itemNum, region.name)
        if (!isDetailStale(rows)) {
            return rows.sortedBy { it.enhancementLevel }.map { it.toDetailDTO() }
        }

        val subList = tradeMarketApi.getSubList(mainKey = itemNum, region = region)
        if (subList.isEmpty()) {
            // 외부 응답에 항목이 없으면 기존 캐시를 그대로 반환한다(캐시 임의 삭제 금지).
            return rows.sortedBy { it.enhancementLevel }.map { it.toDetailDTO() }
        }
        val refreshed = fetchDetailData(itemNum, region, subList)
        saveWithRetry { mergeDetailData(refreshed, itemNum, region, now) }

        // 강화 단계 구성이 바뀐 경우 옛 단계 행을 제거한다.
        // 남겨두면 detailUpdatedAt이 갱신되지 않아 stale 판정이 항상 참이 되어 매 요청 전체 갱신이 발생한다.
        val validLevels = refreshed.map { it.latest.enhancementLevel }.toSet()
        val (valid, orphans) = marketPriceDataRepository.findAllByItemNumAndRegion(itemNum, region.name)
            .partition { it.enhancementLevel in validLevels }
        if (orphans.isNotEmpty()) {
            marketPriceDataRepository.deleteAll(orphans)
        }

        return valid.sortedBy { it.enhancementLevel }.map { it.toDetailDTO() }
    }

    /**
     * 강화 단계별 PriceInfo·BiddingInfo 호출을 [fetchExecutor]로 병렬 수행한다.
     * 한 단계라도 실패하면 원 예외를 전파하고 부분 결과를 저장하지 않는다(정확성 우선).
     */
    private fun fetchDetailData(
        itemNum: Int,
        region: BDORegion,
        subList: List<MarketItem>,
    ): List<MarketDetailRefreshData> {
        val futures = subList.map { latest ->
            CompletableFuture.supplyAsync({
                MarketDetailRefreshData(
                    latest = latest,
                    priceHistory = tradeMarketApi.getPriceInfo(
                        mainKey = itemNum,
                        subKey = latest.enhancementLevel,
                        region = region,
                    ),
                    orderBook = tradeMarketApi.getBiddingInfoList(
                        mainKey = itemNum,
                        subKey = latest.enhancementLevel,
                        region = region,
                    ),
                )
            }, fetchExecutor)
        }
        try {
            return futures.map { it.join() }
        } catch (e: CompletionException) {
            throw e.cause ?: e
        }
    }

    /**
     * 병합 결과를 저장한다.
     *
     * 동시 요청이 같은 키(itemNum, enhancementLevel, region)를 먼저 INSERT한 경우
     * 유니크 제약 위반이 발생하므로, 병합을 다시 수행해 UPDATE로 폴백한다.
     */
    private fun saveWithRetry(merge: () -> List<MarketPriceData>) {
        try {
            marketPriceDataRepository.saveAll(merge())
        } catch (e: DataIntegrityViolationException) {
            logger.info("Concurrent insert detected on market_price_data. Falling back to update.")
            marketPriceDataRepository.saveAll(merge())
        }
    }

    /**
     * 라이트 갱신 병합: basePrice·currentStock·cumulativeVolume·updatedAt만 반영한다.
     * priceHistory·buyOrders·detailUpdatedAt은 상세 갱신(/detail) 전용이므로 유지한다.
     */
    private fun mergeLightData(
        latestData: List<MarketItem>,
        region: BDORegion,
        now: ZonedDateTime,
    ): List<MarketPriceData> {
        val rows = marketPriceDataRepository
            .findAllByItemNumIsInAndRegion(latestData.map { it.itemNum }.distinct(), region.name)
            .associateBy { it.itemNum to it.enhancementLevel }
        return latestData.map { latest ->
            rows[latest.itemNum to latest.enhancementLevel]?.apply {
                basePrice = latest.price
                currentStock = latest.currentStock
                cumulativeVolume = latest.cumulativeVolume
                updatedAt = now
            } ?: MarketPriceData(
                itemNum = latest.itemNum,
                enhancementLevel = latest.enhancementLevel,
                region = region.name,
                basePrice = latest.price,
                currentStock = latest.currentStock,
                cumulativeVolume = latest.cumulativeVolume,
                updatedAt = now,
            )
        }
    }

    /**
     * 상세 갱신 병합: 라이트 필드에 더해 priceHistory·buyOrders·detailUpdatedAt까지 반영한다.
     */
    private fun mergeDetailData(
        refreshed: List<MarketDetailRefreshData>,
        itemNum: Int,
        region: BDORegion,
        now: ZonedDateTime,
    ): List<MarketPriceData> {
        val rows = marketPriceDataRepository.findAllByItemNumAndRegion(itemNum, region.name)
            .associateBy { it.enhancementLevel }
        return refreshed.map { data ->
            rows[data.latest.enhancementLevel]?.apply {
                basePrice = data.latest.price
                currentStock = data.latest.currentStock
                cumulativeVolume = data.latest.cumulativeVolume
                priceHistory = data.priceHistory
                buyOrders = data.buyOrders
                orderBook = data.orderBook
                updatedAt = now
                detailUpdatedAt = now
            } ?: MarketPriceData(
                itemNum = itemNum,
                enhancementLevel = data.latest.enhancementLevel,
                region = region.name,
                basePrice = data.latest.price,
                priceHistory = data.priceHistory,
                cumulativeVolume = data.latest.cumulativeVolume,
                currentStock = data.latest.currentStock,
                buyOrders = data.buyOrders,
                orderBook = data.orderBook,
                updatedAt = now,
                detailUpdatedAt = now,
            )
        }
    }

    companion object {
        /** 캐시 stale 판정 임계값(분). */
        private const val STALE_MINUTES = 15L
    }
}

/** 상세 갱신 시 외부 API에서 수집한 강화 단계별 데이터 묶음. */
private data class MarketDetailRefreshData(
    val latest: MarketItem,
    val priceHistory: List<Long>,
    val orderBook: List<MarketBiddingInfo>,
) {
    /** 구매 대기 수량은 호가창에서 파생된다. 따로 받아 오지 않는다. */
    val buyOrders: Long get() = orderBook.sumOf { it.buyOrders }
}
