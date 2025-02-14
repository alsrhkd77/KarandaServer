package kr.karanda.karandaserver.quartz

import kr.karanda.karandaserver.repository.SynchronizationDataRepository
import kr.karanda.karandaserver.service.TradeMarketService
import org.quartz.JobExecutionContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.quartz.QuartzJobBean
import org.springframework.stereotype.Component

@Component
class MarketDataUpdateJob : QuartzJobBean() {

    @Autowired
    private var tradeMarketService: TradeMarketService? = null

    @Autowired
    private var synchronizationDataRepository:SynchronizationDataRepository? = null


    override fun executeInternal(context: JobExecutionContext) {
        var updated = synchronizationDataRepository?.getTradeMarketLastUpdated()
        updated = updated?.let { tradeMarketService?.updateNextItem(it) }
        updated?.let { synchronizationDataRepository?.setTradeMarketLastUpdated(it) }
    }
}