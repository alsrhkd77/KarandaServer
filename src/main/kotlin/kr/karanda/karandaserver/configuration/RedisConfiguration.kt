package kr.karanda.karandaserver.configuration

import kr.karanda.karandaserver.dto.properties.RedisProperties
import kr.karanda.karandaserver.repository.DefaultDataProvider
import kr.karanda.karandaserver.infrastructure.redis.BroadcastMessageListener
import kr.karanda.karandaserver.infrastructure.redis.RedisChannelProvider
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.connection.RedisPassword
import org.springframework.data.redis.connection.RedisStandaloneConfiguration
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.listener.ChannelTopic
import org.springframework.data.redis.listener.RedisMessageListenerContainer
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter

@Configuration
class RedisConfiguration(private val defaultDataProvider: DefaultDataProvider) {
    val redisProperties: RedisProperties
        get() = defaultDataProvider.getRedisProperties()

    @Bean
    fun redisConnectionFactory(): LettuceConnectionFactory {
        val configuration = RedisStandaloneConfiguration()
        configuration.hostName = redisProperties.host
        configuration.port = redisProperties.port
        configuration.password = RedisPassword.of(redisProperties.password)
        return LettuceConnectionFactory(configuration)
    }

    @Bean
    fun redisTemplate(connectionFactory: RedisConnectionFactory): StringRedisTemplate {
        return StringRedisTemplate(connectionFactory)
    }

    @Bean
    fun broadcastMessageListenerAdapter(listener: BroadcastMessageListener): MessageListenerAdapter {
        return MessageListenerAdapter(listener)
    }

    @Bean
    fun broadcastMessageListenerContainer(
        connectionFactory: RedisConnectionFactory,
        broadcastMessageListenerAdapter: MessageListenerAdapter,
        redisChannelProvider: RedisChannelProvider
    ): RedisMessageListenerContainer {
        return RedisMessageListenerContainer().apply {
            setConnectionFactory(connectionFactory)
            addMessageListener(broadcastMessageListenerAdapter, ChannelTopic(redisChannelProvider.broadcastChannel))
        }
    }
}