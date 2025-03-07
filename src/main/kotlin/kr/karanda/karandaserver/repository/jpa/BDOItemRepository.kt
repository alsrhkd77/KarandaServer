package kr.karanda.karandaserver.repository.jpa

import kr.karanda.karandaserver.entity.BDOItem
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.transaction.annotation.Transactional

interface BDOItemRepository: JpaRepository<BDOItem, Long> {
    @Transactional(readOnly = true)
    fun findAllByItemNumIsInAndTradeAble(itemNums: List<Int>, tradeAble: Boolean = true): List<BDOItem>
    @Transactional(readOnly = true)
    fun findByItemNumAndTradeAble(itemNum: Int, tradeAble: Boolean = true): BDOItem?
    @Transactional(readOnly = true)
    fun findFirstByIdGreaterThanAndTradeAble(id: Int, tradeAble: Boolean = true): BDOItem?
    @Transactional(readOnly = true)
    fun findFirstByTradeAble(tradeAble: Boolean = true): BDOItem?
    fun findByItemNum(itemNum: Int): BDOItem?
}