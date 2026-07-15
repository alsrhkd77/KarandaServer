package kr.karanda.karandaserver

import kr.karanda.karandaserver.repository.jpa.MarketPriceDataRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import kotlin.test.Test

@SpringBootTest
class TradeMarketTests {
    @Autowired
    var marketPriceDataRepository: MarketPriceDataRepository? = null

    @Test
    fun `test apis`() {
        println(":")
    }
}
