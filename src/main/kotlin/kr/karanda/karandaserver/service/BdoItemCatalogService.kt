package kr.karanda.karandaserver.service

import kr.karanda.karandaserver.api.TradeMarketApi
import kr.karanda.karandaserver.entity.BDOItem
import kr.karanda.karandaserver.enums.BDORegion
import kr.karanda.karandaserver.repository.jpa.BDOItemRepository
import kr.karanda.karandaserver.util.BdoMarketCategory
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service

/**
 * `bdo_item` 카탈로그 반자동 동기화.
 *
 * - [discoverNextCategory] : 로그인 불필요 거래소 `getMarketList`로 카테고리 하나를 조회해 신규 itemNum을 skeleton 행으로
 *   삽입하고, 다시 거래되는 기존 아이템을 거래 가능으로 전환한다. 매 실행마다 서브카테고리 하나씩 순회한다(시각 기반 결정론적 선택).
 * - [enrichNext]           : 보강이 필요한 행 1건을 원자적 claim 후, 상세페이지·거래소 API로 메타데이터를 채운다.
 *
 * 다중 인스턴스(Cloud Run)에서 중복 실행되어도 안전하도록 멱등성 + DB 원자적 claim으로 설계했다.
 */
@Service
class BdoItemCatalogService(
    val tradeMarketApi: TradeMarketApi,
    val bdoItemRepository: BDOItemRepository,
    val itemImageStorage: ItemImageStorage,
) {
    val logger: Logger = LoggerFactory.getLogger(BdoItemCatalogService::class.java)

    /**
     * 카테고리 순회 발견. 매 실행마다 (mainCategory, subCategory) 페어 하나를 시각 기반으로 결정론적으로 골라 조회한다.
     * `index = 현재_epoch_시각(시간) % 전체_페어수` 이므로 모든 인스턴스가 같은 시간대에 같은 페어를 골라 레이스가 없다.
     */
    fun discoverNextCategory() {
        val pairs = BdoMarketCategory.pairs
        if (pairs.isEmpty()) return

        val index = ((System.currentTimeMillis() / 1000 / 3600) % pairs.size).toInt()
        val (mainCategory, subCategory) = pairs[index]

        val items = try {
            tradeMarketApi.getMarketList(mainCategory = mainCategory, subCategory = subCategory, region = BDORegion.KR)
        } catch (e: Exception) {
            logger.warn("Catalog discovery: getMarketList failed for ($mainCategory, $subCategory).", e)
            return
        }

        for (item in items) {
            val itemNum = item.itemNum
            val existing = bdoItemRepository.findByItemNum(itemNum)
            if (existing == null) {
                try {
                    bdoItemRepository.save(
                        BDOItem(
                            itemNum = itemNum,
                            tradeAble = true,
                            needsUpdate = true,
                            mainCategory = mainCategory,
                            subCategory = subCategory,
                        )
                    )
                    logger.info("Catalog discovery: new item #$itemNum in ($mainCategory, $subCategory).")
                } catch (e: DataIntegrityViolationException) {
                    // 다른 인스턴스가 먼저 삽입 → 무해 (itemNum unique 제약)
                }
            } else {
                // 거래되는 것이 확인됐으므로 거래 가능으로 전환하고, 카테고리가 바뀌었으면 갱신한다.
                // (두 쿼리 모두 deleted=false 조건 + 값이 이미 맞으면 0행 → 삭제/수동 행은 건드리지 않음, 멱등)
                bdoItemRepository.markTradeableIfListed(itemNum)
                bdoItemRepository.updateCategoryIfChanged(itemNum, mainCategory, subCategory)
            }
        }
    }

    /**
     * 크래시 복구: claim 이후 보강을 끝내지 못해 남은 행을 다시 보강 대상으로 표시한다. 멱등하므로 어느 인스턴스가 호출해도 무해.
     */
    fun reflagOrphanedEnrichments() {
        bdoItemRepository.reflagOrphanedEnrichments()
    }

    /**
     * 공통 보강: 보강 필요 행 1건을 원자적 claim 후 채운다(실행당 1건, 요청량 최소화).
     * claim에 성공한 단일 인스턴스만 외부 상세페이지를 fetch하므로 중복 크롤이 발생하지 않는다.
     */
    fun enrichNext() {
        val candidate = bdoItemRepository.findFirstByNeedsUpdateTrueAndDeletedFalse() ?: return
        val itemNum = candidate.itemNum

        // 외부 호출 전에 원자적으로 claim. 영향 행 0이면 다른 인스턴스가 이미 가져간 것.
        if (bdoItemRepository.claimForEnrichment(itemNum) == 0) return

        try {
            val metadata = tradeMarketApi.fetchItemMetadata(itemNum)
            // 강화 최대 레벨은 거래 가능한 아이템에 한해 거래소 API에서 도출(거래 불가/미상장이면 채우지 않음).
            val maxEnhancementLevel =
                if (candidate.tradeAble) deriveMaxEnhancementLevel(itemNum) else candidate.maxEnhancementLevel

            candidate.itemNameKr = metadata.nameKr
            candidate.itemNameEn = metadata.nameEn
            candidate.grade = metadata.grade
            candidate.maxEnhancementLevel = maxEnhancementLevel
            candidate.needsUpdate = false
            bdoItemRepository.save(candidate)
            logger.info("Catalog enrich: item #$itemNum (${metadata.nameKr}).")
        } catch (e: Exception) {
            // 실패 시 claim 해제 → 다음 실행에서 재시도. 추측으로 값을 채우지 않는다.
            bdoItemRepository.releaseEnrichmentClaim(itemNum)
            logger.warn("Catalog enrich: failed for item #$itemNum, will retry.", e)
        }
    }

    /**
     * 이미지 미업로드 행 1건을 원자적 claim 후 CDN에서 받아 GCP Storage에 업로드한다(실행당 1건).
     * claim에 성공한 단일 인스턴스만 다운로드/업로드하며, 실패 시 claim을 해제해 다음 실행에서 재시도한다.
     * 메타데이터 보강과 분리되어 있어 이미지 실패가 이름/등급 보강에 영향을 주지 않는다.
     */
    fun uploadNextImage() {
        val candidate = bdoItemRepository.findFirstByImageStoredFalseAndDeletedFalse() ?: return
        val itemNum = candidate.itemNum

        if (bdoItemRepository.claimImageUpload(itemNum) == 0) return

        try {
            itemImageStorage.store(itemNum)
            logger.info("Catalog image: uploaded item #$itemNum.")
        } catch (e: Exception) {
            bdoItemRepository.releaseImageUpload(itemNum)
            logger.warn("Catalog image: upload failed for item #$itemNum, will retry.", e)
        }
    }

    private fun deriveMaxEnhancementLevel(itemNum: Int): Int? {
        return try {
            tradeMarketApi.getSubList(mainKey = itemNum, region = BDORegion.KR)
                .maxOfOrNull { it.enhancementLevel }
        } catch (e: Exception) {
            logger.warn("Catalog enrich: deriving max enhancement level failed for item #$itemNum.", e)
            null
        }
    }
}
