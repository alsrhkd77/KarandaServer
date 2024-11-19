package kr.karanda.karandaserver.quartz

import kr.karanda.karandaserver.service.TradeMarketService
import org.quartz.JobExecutionContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.quartz.QuartzJobBean
import org.springframework.stereotype.Component

@Component
class TranslateMarketDataJob:QuartzJobBean() {

    override fun executeInternal(context: JobExecutionContext) {
        TODO("Not yet implemented")
    }

}