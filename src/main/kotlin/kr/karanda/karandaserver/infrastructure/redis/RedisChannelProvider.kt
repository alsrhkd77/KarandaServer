package kr.karanda.karandaserver.infrastructure.redis

import org.springframework.core.env.Environment
import org.springframework.stereotype.Component

@Component
class RedisChannelProvider(private val environment: Environment) {
    val broadcastChannel: String
        get() {
            return if (environment.activeProfiles.contains("production")) {
                "broadcast"
            } else {
                "broadcast-dev"
            }
        }
}