package kr.karanda.karandaserver.service.reporthub

import kr.karanda.karandaserver.enums.BDORegion
import kr.karanda.karandaserver.enums.ReportKind
import kr.karanda.karandaserver.enums.ReportTarget
import kr.karanda.karandaserver.exception.TooManyRequestsException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component
import java.time.Duration

/**
 * 제보 레이트 리밋. 여러 인스턴스에서도 정확해야 하므로 Redis를 쓴다.
 *
 * - 같은 지역·대상·채널·종류: 60초에 1건 (더블탭·핑퐁 방지)
 * - 사용자별: 10분에 20건
 *
 * Redis 장애 시에는 제보를 막지 않고 경고만 남긴다(실시간 전파도 Redis에 의존하므로 장애는 별도로 드러난다).
 */
@Component
class ReportHubRateLimiter(
    private val redisTemplate: StringRedisTemplate,
    redisKeys: ReportHubRedisKeys,
) {
    private val logger: Logger = LoggerFactory.getLogger(ReportHubRateLimiter::class.java)

    private val prefix = redisKeys.prefix

    fun acquire(region: BDORegion, userId: Long, target: ReportTarget?, channel: String?, kind: ReportKind) {
        val allowed = try {
            val keyLimit = "$prefix:rl:${region.name}:$userId:${target?.name ?: "-"}:${channel ?: "-"}:${kind.name}"
            val firstForKey = redisTemplate.opsForValue().setIfAbsent(keyLimit, "1", KEY_WINDOW) ?: true
            if (!firstForKey) {
                false
            } else {
                val userLimit = "$prefix:rl:${region.name}:$userId"
                val count = redisTemplate.opsForValue().increment(userLimit) ?: 1L
                if (count == 1L) redisTemplate.expire(userLimit, USER_WINDOW)
                count <= USER_MAX
            }
        } catch (e: Exception) {
            logger.warn("Report hub rate limiter unavailable, allowing report: ${e.message}")
            true
        }
        if (!allowed) throw TooManyRequestsException()
    }

    companion object {
        private val KEY_WINDOW: Duration = Duration.ofSeconds(60)
        private val USER_WINDOW: Duration = Duration.ofMinutes(10)
        private const val USER_MAX = 20L
    }
}
