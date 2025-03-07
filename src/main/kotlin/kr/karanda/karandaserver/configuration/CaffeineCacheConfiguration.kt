package kr.karanda.karandaserver.configuration

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.Scheduler
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cache.caffeine.CaffeineCacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.concurrent.TimeUnit

@Configuration
@EnableCaching
class CaffeineCacheConfiguration {
    @Bean
    fun cacheManager(): CacheManager {
        val caffeineCacheManager = CaffeineCacheManager()
        caffeineCacheManager.registerCustomCache(
            "TradeMarketWaitList",
            buildCacheConfig(expire = 29, initialCapacity = 1, maximumSize = 1)
        )
        return caffeineCacheManager
    }

    private fun buildCacheConfig(
        expire: Long? = null,
        initialCapacity: Int? = null,
        maximumSize: Long? = null
    ): Cache<Any, Any> {
        val builder = Caffeine.newBuilder()
        expire?.let { builder.expireAfterWrite(it, TimeUnit.SECONDS) }
        maximumSize?.let { builder.maximumSize(it) }
        initialCapacity?.let { builder.initialCapacity(it) }
        builder.scheduler(Scheduler.systemScheduler())
        return builder.build()
    }
}