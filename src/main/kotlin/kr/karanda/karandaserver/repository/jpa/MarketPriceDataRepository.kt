package kr.karanda.karandaserver.repository.jpa

import kr.karanda.karandaserver.entity.MarketPriceData
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.transaction.annotation.Transactional

interface MarketPriceDataRepository : JpaRepository<MarketPriceData, Long> {

    @Transactional(readOnly = true)
    fun findAllByItemNumAndRegion(itemNum: Int, region: String): List<MarketPriceData>

    @Transactional(readOnly = true)
    fun findAllByItemNumIsInAndRegion(itemNums: List<Int>, region: String): List<MarketPriceData>
}
