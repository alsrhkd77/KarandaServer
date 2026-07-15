package kr.karanda.karandaserver.configuration

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor

@Configuration
class TradeMarketConfiguration {

    /**
     * 거래소 외부 API 호출 병렬화용 스레드 풀.
     *
     * `/detail` cold 갱신에서 강화 단계별 PriceInfo·BiddingInfo 호출을 병렬로 수행한다.
     * 전역 공유 풀이므로 동시 외부 호출 수가 [MAX_POOL_SIZE]로 상한된다(거래소 API 과부하·차단 방지).
     */
    @Bean("tradeMarketFetchExecutor")
    fun tradeMarketFetchExecutor(): ThreadPoolTaskExecutor {
        return ThreadPoolTaskExecutor().apply {
            corePoolSize = MAX_POOL_SIZE
            maxPoolSize = MAX_POOL_SIZE
            setQueueCapacity(QUEUE_CAPACITY)
            setThreadNamePrefix("trade-market-fetch-")
            setAllowCoreThreadTimeOut(true)
            setWaitForTasksToCompleteOnShutdown(true)
            setAwaitTerminationSeconds(10)
            initialize()
        }
    }

    companion object {
        private const val MAX_POOL_SIZE = 8
        private const val QUEUE_CAPACITY = 256
    }
}
