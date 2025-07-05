package kr.karanda.karandaserver.infrastructure.quartz

import kr.karanda.karandaserver.service.TradeMarketService
import org.quartz.JobExecutionContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.quartz.QuartzJobBean
import org.springframework.stereotype.Component

@Component
class PublishMarketWaitListJob: QuartzJobBean() {

    @Autowired
    private var tradeMarketService: TradeMarketService? = null

    override fun executeInternal(context: JobExecutionContext) {
        tradeMarketService?.publishWaitList()
    }
}