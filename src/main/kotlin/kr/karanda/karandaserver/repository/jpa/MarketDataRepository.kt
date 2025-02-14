package kr.karanda.karandaserver.repository.jpa

import kr.karanda.karandaserver.entity.MarketData
import org.springframework.data.jpa.repository.JpaRepository
import java.time.ZonedDateTime

interface MarketDataRepository: JpaRepository<MarketData, Long> {
    fun findAllByItemNumOrderByDateDesc(itemNum: Int): List<MarketData>
    fun findAllByItemNumAndDateIsAfterOrderByDateDesc(itemNum: Int, date: ZonedDateTime): List<MarketData>
    fun findAllByItemNumIsInAndDateIsAfter(itemNums: List<Int>, date: ZonedDateTime): List<MarketData>
}