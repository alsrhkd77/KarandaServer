package kr.karanda.karandaserver.service.reporthub

import org.springframework.core.env.Environment
import org.springframework.stereotype.Component

/**
 * Reporter Hub Redis 키 공간. develop과 production이 같은 Redis를 쓸 수 있으므로
 * RedisChannelProvider와 같은 기준으로 접두어를 나눈다.
 */
@Component
class ReportHubRedisKeys(environment: Environment) {
    val prefix: String = if (environment.activeProfiles.contains("production")) "rh" else "rh-dev"
}
