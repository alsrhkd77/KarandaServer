package kr.karanda.karandaserver.quartz

import kr.karanda.karandaserver.service.FireStoreService
import kr.karanda.karandaserver.service.TradeMarketService
import org.quartz.JobExecutionContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.DependsOn
import org.springframework.scheduling.quartz.QuartzJobBean
import org.springframework.stereotype.Component

@Component
@DependsOn("fireStoreService")
class MarketDataUpdateJob : QuartzJobBean() {

    @Autowired
    private var tradeMarketService: TradeMarketService? = null

    @Autowired
    private var fireStoreService: FireStoreService? = null


    override fun executeInternal(context: JobExecutionContext) {
        var updated = fireStoreService?.getTradeMarketLastUpdated()
        updated = updated?.let { tradeMarketService?.updateNextItem(it) }
        updated?.let { fireStoreService?.setTradeMarketLastUpdated(it) }
    }
}