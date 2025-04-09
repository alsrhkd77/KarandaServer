package kr.karanda.karandaserver.repository.jpa

import jakarta.transaction.Transactional
import kr.karanda.karandaserver.entity.MarketData
import org.springframework.data.jpa.repository.JpaRepository
import java.time.ZonedDateTime

interface MarketDataRepository: JpaRepository<MarketData, Long> {
    fun findAllByItemNumAndRegionOrderByDateDesc(itemNum: Int, region: String): List<MarketData>
    fun findAllByItemNumAndRegionAndDateIsAfter(itemNum: Int, region: String, date: ZonedDateTime): List<MarketData>
    fun findAllByItemNumIsInAndRegionAndDateIsAfter(itemNums: List<Int>, region: String, date: ZonedDateTime): List<MarketData>
    @Transactional
    fun deleteAllByDateIsBefore(date: ZonedDateTime): List<MarketData>
}