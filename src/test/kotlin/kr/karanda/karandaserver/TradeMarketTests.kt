package kr.karanda.karandaserver

import kr.karanda.karandaserver.repository.jpa.MarketPriceDataRepository
import org.junit.jupiter.api.Tag
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import kotlin.test.Test

@Tag("integration")
@SpringBootTest
class TradeMarketTests {
    @Autowired
    var marketPriceDataRepository: MarketPriceDataRepository? = null

    @Test
    fun `test apis`() {
        println(":")
    }
}
