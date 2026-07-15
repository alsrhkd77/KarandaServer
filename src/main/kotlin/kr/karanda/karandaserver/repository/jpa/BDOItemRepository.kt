package kr.karanda.karandaserver.repository.jpa

import kr.karanda.karandaserver.entity.BDOItem
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.transaction.annotation.Transactional

interface BDOItemRepository : JpaRepository<BDOItem, Long> {
    // 서빙 쿼리: 삭제된 아이템(deleted=true)은 항상 제외한다.
    @Transactional(readOnly = true)
    fun findAllByItemNumIsInAndTradeAbleAndDeletedFalse(itemNums: List<Int>, tradeAble: Boolean = true): List<BDOItem>

    @Transactional(readOnly = true)
    fun findByItemNumAndTradeAbleAndDeletedFalse(itemNum: Int, tradeAble: Boolean = true): BDOItem?

    @Transactional(readOnly = true)
    fun findFirstByItemNumGreaterThanAndTradeAbleAndDeletedFalse(itemNum: Int, tradeAble: Boolean = true): BDOItem?

    @Transactional(readOnly = true)
    fun findFirstByTradeAbleAndDeletedFalse(tradeAble: Boolean = true): BDOItem?

    fun findByItemNum(itemNum: Int): BDOItem?

    // 보강 대상(정보 갱신 필요) 1건.
    @Transactional(readOnly = true)
    fun findFirstByNeedsUpdateTrueAndDeletedFalse(): BDOItem?

    /**
     * 원자적 claim: 외부 호출(상세페이지 fetch) 전에 단일 인스턴스만 보강을 진행하도록 한다.
     * 영향 행 수가 1이면 claim 성공, 0이면 다른 인스턴스가 이미 가져간 것.
     */
    @Modifying
    @Transactional
    @Query("UPDATE bdo_item b SET b.needsUpdate = false WHERE b.itemNum = :itemNum AND b.needsUpdate = true")
    fun claimForEnrichment(@Param("itemNum") itemNum: Int): Int

    /**
     * claim 해제: 보강 실패 시 needsUpdate를 되돌려 다음 실행에서 재시도되게 한다.
     */
    @Modifying
    @Transactional
    @Query("UPDATE bdo_item b SET b.needsUpdate = true WHERE b.itemNum = :itemNum AND b.needsUpdate = false AND b.deleted = false")
    fun releaseEnrichmentClaim(@Param("itemNum") itemNum: Int): Int

    /**
     * getMarketList에 나타난(=거래되는) 기존 아이템을 거래 가능으로 전환한다. 멱등.
     */
    @Modifying
    @Transactional
    @Query("UPDATE bdo_item b SET b.tradeAble = true WHERE b.itemNum = :itemNum AND b.tradeAble = false AND b.deleted = false")
    fun markTradeableIfListed(@Param("itemNum") itemNum: Int): Int

    /**
     * 발견된 카테고리가 저장값과 다르면 갱신한다. 카테고리 컬럼만 원자적으로 UPDATE해 다른 필드를 clobber하지 않는다.
     */
    @Modifying
    @Transactional
    @Query(
        "UPDATE bdo_item b SET b.mainCategory = :mainCategory, b.subCategory = :subCategory " +
            "WHERE b.itemNum = :itemNum AND b.deleted = false " +
            "AND (b.mainCategory <> :mainCategory OR b.subCategory <> :subCategory)"
    )
    fun updateCategoryIfChanged(
        @Param("itemNum") itemNum: Int,
        @Param("mainCategory") mainCategory: Int,
        @Param("subCategory") subCategory: Int,
    ): Int

    /**
     * 크래시 복구: claim(needsUpdate=false) 이후 보강을 끝내지 못해 이름이 비어있는 채 남은 행을 다시 보강 대상으로 표시한다.
     */
    @Modifying
    @Transactional
    @Query("UPDATE bdo_item b SET b.needsUpdate = true WHERE b.itemNameEn IS NULL AND b.needsUpdate = false AND b.deleted = false")
    fun reflagOrphanedEnrichments(): Int

    // 이미지 미업로드 행 1건.
    @Transactional(readOnly = true)
    fun findFirstByImageStoredFalseAndDeletedFalse(): BDOItem?

    /**
     * 이미지 업로드 원자적 claim. imageStored를 미리 true로 세팅해 단일 인스턴스만 CDN 다운로드/업로드를 수행한다.
     */
    @Modifying
    @Transactional
    @Query("UPDATE bdo_item b SET b.imageStored = true WHERE b.itemNum = :itemNum AND b.imageStored = false")
    fun claimImageUpload(@Param("itemNum") itemNum: Int): Int

    /**
     * 이미지 업로드 claim 해제. 업로드 실패 시 되돌려 다음 실행에서 재시도되게 한다.
     */
    @Modifying
    @Transactional
    @Query("UPDATE bdo_item b SET b.imageStored = false WHERE b.itemNum = :itemNum AND b.imageStored = true")
    fun releaseImageUpload(@Param("itemNum") itemNum: Int): Int
}
