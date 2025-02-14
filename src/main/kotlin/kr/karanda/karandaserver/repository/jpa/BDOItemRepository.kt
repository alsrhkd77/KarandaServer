package kr.karanda.karandaserver.repository.jpa

import kr.karanda.karandaserver.entity.BDOItem
import org.springframework.data.jpa.repository.JpaRepository

interface BDOItemRepository: JpaRepository<BDOItem, Long> {
    fun findByItemNumIsInAndTradeAble(itemNums: List<Int>, tradeAble: Boolean = true): List<BDOItem>
    fun findByItemNumAndTradeAble(itemNum: Int, tradeAble: Boolean = true): BDOItem?
    fun findFirstByIdGreaterThanAndTradeAble(id: Int, tradeAble: Boolean = true): BDOItem?
    fun findFirstByTradeAble(tradeAble: Boolean = true): BDOItem?
}