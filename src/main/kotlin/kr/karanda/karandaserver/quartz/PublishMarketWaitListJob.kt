package kr.karanda.karandaserver.quartz

import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kr.karanda.karandaserver.service.TradeMarketService
import org.quartz.JobExecutionContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.scheduling.quartz.QuartzJobBean
import org.springframework.stereotype.Component

@Component
class PublishMarketWaitListJob: QuartzJobBean() {

    @Autowired
    private var tradeMarketService: TradeMarketService? = null

    @Autowired
    private var messagingTemplate: SimpMessagingTemplate? = null

    override fun executeInternal(context: JobExecutionContext) {
        tradeMarketService?.getWaitList().let {
            messagingTemplate?.convertAndSend("/live-data/trade-market/wait-list", Json.encodeToString(it))
        }
    }
}