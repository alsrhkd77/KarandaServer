package kr.karanda.karandaserver.filter

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import kr.karanda.karandaserver.repository.DefaultDataProvider
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.security.MessageDigest

/**
 * 크롤러 Webhook(`POST /bdo-news/notify`) 전용 인증 필터.
 *
 * 크롤러는 Karanda 클라이언트가 아니므로 Qualification 토큰을 가질 수 없다.
 * 대신 `TokenProperties.utilKey`를 Bearer 토큰으로 검증한다.
 * (해당 경로는 [QualificationFilter] whiteList에서 제외되어 있음)
 *
 * prefix가 아닌 정확한 경로+메서드 매치로만 동작해 `/bdo-news`의 다른 공개
 * 엔드포인트(`GET /bdo-news`, `GET /bdo-news/highlights`)에는 영향을 주지 않는다.
 */
@Component
class BdoNewsWebhookAuthFilter(
    private val defaultDataProvider: DefaultDataProvider,
) : OncePerRequestFilter() {

    private val log: Logger = LoggerFactory.getLogger(BdoNewsWebhookAuthFilter::class.java)

    override fun shouldNotFilter(request: HttpServletRequest): Boolean {
        return !(request.requestURI == "/bdo-news/notify" && request.method == "POST")
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val header = request.getHeader("Authorization")
        val token = header?.removePrefix("Bearer")?.trim()
        val utilKey = defaultDataProvider.getTokenProperties().utilKey

        // 상수 시간 비교로 타이밍 공격 방지. 실패 로그에 토큰 값을 남기지 않는다.
        val valid = !token.isNullOrEmpty() && utilKey.isNotEmpty() && MessageDigest.isEqual(
            token.toByteArray(Charsets.UTF_8),
            utilKey.toByteArray(Charsets.UTF_8)
        )

        if (!valid) {
            log.warn("BdoNews webhook authentication failed")
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized")
            return
        }

        filterChain.doFilter(request, response)
    }
}
