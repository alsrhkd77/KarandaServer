package kr.karanda.karandaserver.filter

import io.jsonwebtoken.ExpiredJwtException
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import kr.karanda.karandaserver.dto.TokenClaims
import kr.karanda.karandaserver.util.TokenUtils
import org.springframework.http.HttpMethod
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.util.matcher.RequestMatcher
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

/**
 * JWT 인증 필터.
 *
 * JWT에서 사용자 정보 (UUID)를 추출해 SecurityContext 저장.
 *
 * whitePath에 포함된 경로는 skip
 */
@Component
class AuthorizationFilter(private val tokenUtils: TokenUtils) : OncePerRequestFilter() {

    private var whitePathMatcher: RequestMatcher? = null

    fun setWhitePathMatcher(whitePathMatcher: RequestMatcher) {
        this.whitePathMatcher = whitePathMatcher
    }

    /**
     * 토큰 인증 로직.
     *
     * AccessToken에서 UUID를 추출해 SecurityContext에 넣음.
     *
     * token refresh path일 경우 AccessToken의 UUID와 RefreshToken의 UUID가 같을 경우에만 허용.
     * AccessToken이 만료된 경우에도 RefreshToken과 UUID가 같다면 허용.
     */
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val accessTokenHeaderName = request.headerNames.toList().find {
            it.equals("authorization", ignoreCase = true)
        }
        if (accessTokenHeaderName == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized")
            return
        }
        val accessToken = request.getHeader(accessTokenHeaderName)
        if (request.requestURI.equals("/auth/discord/refresh")) {
            val refreshTokenHeaderName = request.headerNames.toList().find {
                it.equals("refresh-token", ignoreCase = true)
            }
            if (refreshTokenHeaderName == null) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized")
                return
            }
            val uuid = try {
                (tokenUtils.validateAccessToken(accessToken).principal as TokenClaims).userUUID
            } catch (e: ExpiredJwtException) {
                e.claims.subject
            } catch (e: Exception) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized")
                return
            }
            try {
                val authentication = tokenUtils.validateRefreshToken(request.getHeader(refreshTokenHeaderName))
                if ((authentication.principal as TokenClaims).userUUID == uuid) {
                    SecurityContextHolder.getContext().authentication = authentication
                } else {
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized")
                    return
                }
            } catch (e: Exception) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized")
                return
            }
        } else {
            try {
                tokenUtils.validateAccessToken(accessToken).let {
                    SecurityContextHolder.getContext().authentication = it
                }
            } catch (e: Exception) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized")
                return
            }
        }
        filterChain.doFilter(request, response)
    }

    override fun shouldNotFilter(request: HttpServletRequest): Boolean {
        if (HttpMethod.OPTIONS.matches(request.method)) return true //Allow preflight
        if (whitePathMatcher != null && whitePathMatcher!!.matches(request)) return true
        return false
    }
}