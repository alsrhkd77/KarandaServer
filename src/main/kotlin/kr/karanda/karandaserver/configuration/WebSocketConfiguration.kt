package kr.karanda.karandaserver.configuration

import kr.karanda.karandaserver.dto.TokenClaims
import kr.karanda.karandaserver.util.TokenUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Lazy
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.messaging.Message
import org.springframework.messaging.MessageChannel
import org.springframework.messaging.simp.config.ChannelRegistration
import org.springframework.messaging.simp.config.MessageBrokerRegistry
import org.springframework.messaging.simp.stomp.StompCommand
import org.springframework.messaging.simp.stomp.StompHeaderAccessor
import org.springframework.messaging.support.ChannelInterceptor
import org.springframework.messaging.support.MessageHeaderAccessor
import org.springframework.scheduling.TaskScheduler
import org.springframework.stereotype.Component
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker
import org.springframework.web.socket.config.annotation.StompEndpointRegistry
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration

@Configuration
@EnableWebSocketMessageBroker
class WebSocketConfiguration : WebSocketMessageBrokerConfigurer {

    @Autowired
    @Lazy
    private var messageBrokerTaskScheduler: TaskScheduler? = null

    @Autowired
    private var inBoundInterceptor: WebSocketChannelInterceptor? = null

    override fun configureMessageBroker(registry: MessageBrokerRegistry) {
        //registry.setApplicationDestinationPrefixes("/app");
        registry
            .enableSimpleBroker("/live-data")
            .setHeartbeatValue(arrayOf(30000L, 40000L).toLongArray())   //Interval [from server, from client]
            .setTaskScheduler(messageBrokerTaskScheduler!!)
    }

    override fun configureClientInboundChannel(registration: ChannelRegistration) {
        registration.interceptors(inBoundInterceptor)
    }

    override fun registerStompEndpoints(registry: StompEndpointRegistry) {
        registry
            .addEndpoint("/live-channel")
            .setAllowedOrigins(
                "https://www.karanda.kr",
                "https://karanda.kr",
                "https://hammuu1112.github.io",
                "http://localhost:8082",
                "http://localhost:2345"
            )
    }

    override fun configureWebSocketTransport(registry: WebSocketTransportRegistration) {
        registry.setMessageSizeLimit(4 * 1024 * 1024)
    }
}

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 99)
class WebSocketChannelInterceptor(val tokenUtils: TokenUtils) : ChannelInterceptor {

    override fun preSend(message: Message<*>, channel: MessageChannel): Message<*>? {
        val accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor::class.java) ?: return null
        //println(accessor.command)
        //println(accessor.messageHeaders)
        if (StompCommand.CONNECT == accessor.command) {
            val qualification = accessor.getNativeHeader("Qualification")?.firstOrNull() ?: return null
            return if (tokenUtils.validateQualificationToken(qualification)) {
                message
            } else {
                null
            }
        } else if (StompCommand.SUBSCRIBE == accessor.command) {
            //println("SUBSCRIBE ${accessor.destination}")
            val destination: String = accessor.destination ?: return null
            if (destination.contains("/user-private/")) {
                val authorization = accessor.getNativeHeader("Authorization")?.firstOrNull() ?: return null
                try {
                    val authentication = tokenUtils.validateAccessToken(authorization).principal as TokenClaims
                    accessor.destination = destination.replace("/user-private/", "/${authentication.userUUID}/")
                    return message
                } catch (e: Exception) {
                    return null
                }
            }
        }
        return message
    }
}