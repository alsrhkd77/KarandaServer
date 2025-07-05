package kr.karanda.karandaserver.infrastructure.redis

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kr.karanda.karandaserver.dto.BroadcastMessage
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component

@Component
class RedisPublisher(
    private val redisTemplate: StringRedisTemplate,
    private val redisChannelProvider: RedisChannelProvider
) {
    @Async
    fun broadcast(message: BroadcastMessage) {
        redisTemplate.convertAndSend(redisChannelProvider.broadcastChannel, Json.encodeToString(listOf(message)))
    }

    @Async
    fun broadcast(messages: List<BroadcastMessage>) {
        redisTemplate.convertAndSend(redisChannelProvider.broadcastChannel, Json.encodeToString(messages))
    }
}