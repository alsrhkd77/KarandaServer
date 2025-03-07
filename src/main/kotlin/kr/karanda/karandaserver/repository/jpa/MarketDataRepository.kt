package kr.karanda.karandaserver.repository.jpa

import kr.karanda.karandaserver.entity.MarketData
import org.springframework.data.jpa.repository.JpaRepository
import java.time.ZonedDateTime

interface MarketDataRepository: JpaRepository<MarketData, Long> {
    fun findAllByItemNumAndRegionOrderByDateDesc(itemNum: Int, region: String): List<MarketData>
    fun findAllByItemNumAndRegionAndDateIsAfterOrderByDateDesc(itemNum: Int, region: String, date: ZonedDateTime): List<MarketData>
    fun findAllByItemNumIsInAndRegionAndDateIsAfter(itemNums: List<Int>, region: String, date: ZonedDateTime): List<MarketData>
}