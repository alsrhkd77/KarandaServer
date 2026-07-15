package kr.karanda.karandaserver.infrastructure.scheduler

import kr.karanda.karandaserver.service.TradeMarketService
import org.springframework.context.annotation.Profile
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/**
 * 거래소 대기 목록(wait-list)을 주기적으로 조회해 WebSocket으로 브로드캐스트한다.
 *
 * WebSocket이 컨테이너 로컬 인메모리 SimpleBroker([kr.karanda.karandaserver.configuration.WebSocketConfiguration])를
 * 사용하므로, 브로드캐스트는 해당 컨테이너에 연결된 클라이언트에게만 전달된다.
 * 따라서 이 작업은 Quartz 클러스터링(단일 노드 실행) 대상이 아니라
 * Spring @Scheduled로 각 컨테이너에서 독립 실행되어야 모든 클라이언트가 갱신을 받는다.
 */
@Profile("production")
@Component
class MarketWaitListScheduler(
    val tradeMarketService: TradeMarketService,
) {

    @Scheduled(fixedRate = 10_000)
    fun publishWaitList() {
        tradeMarketService.publishWaitList()
    }
}
