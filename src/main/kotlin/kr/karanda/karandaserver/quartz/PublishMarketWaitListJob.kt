package kr.karanda.karandaserver.quartz

import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kr.karanda.karandaserver.enums.BDORegion
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
        tradeMarketService?.publishWaitList()
        //TODO: 클라이언트 업데이트 후 제거해야함
        tradeMarketService?.getWaitList(BDORegion.KR).let {
            messagingTemplate?.convertAndSend("/live-data/trade-market/wait-list", Json.encodeToString(it))
        }
    }
}