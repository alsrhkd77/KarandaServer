package kr.karanda.karandaserver.filter

import io.jsonwebtoken.ExpiredJwtException
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import kr.karanda.karandaserver.dto.TokenClaims
import kr.karanda.karandaserver.util.TokenFactory
import org.springframework.http.HttpMethod
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.util.matcher.RequestMatcher
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class AuthorizationFilter(private val tokenFactory: TokenFactory) : OncePerRequestFilter() {

    private var whitePathMatcher: RequestMatcher? = null

    fun setWhitePathMatcher(whitePathMatcher: RequestMatcher) {
        this.whitePathMatcher = whitePathMatcher
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val headerName = request.headerNames.toList().find {
            it.equals("authorization", ignoreCase = true)
        }
        if(headerName == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized")
            return
        }
        val token = request.getHeader(headerName).removePrefix("Bearer ")
        if (request.requestURI.equals("/auth/discord/refresh")) {
            if(request.headerNames.toList().find { it.equals("refresh-token", ignoreCase = true) } == null) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized")
                return
            }
            val refreshToken = request.getHeader("refresh-token").removePrefix("Bearer ")
            if (tokenFactory.validateRefreshToken(refreshToken)) {
                var uuid: String?
                try {
                    tokenFactory.getAuthentication(token).let {
                        uuid = (it.principal as TokenClaims).userUUID
                    }
                } catch (e: ExpiredJwtException) {
                    uuid = e.claims.subject
                } catch (e: Exception) {
                    //잘못된 토큰
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized")
                    return
                }

                val authentication = tokenFactory.getAuthenticationFromRefreshToken(refreshToken)
                if (uuid != null && (authentication.principal as TokenClaims).userUUID == uuid) {
                    SecurityContextHolder.getContext().authentication = authentication
                } else {
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized")
                    return
                }
            }
        } else {
            if (tokenFactory.validateAccessToken(token)) {
                tokenFactory.getAuthentication(token).let {
                    SecurityContextHolder.getContext().authentication = it
                }
            } else {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized")
                return
            }
        }
        filterChain.doFilter(request, response)
    }

    override fun shouldNotFilter(request: HttpServletRequest): Boolean {
        // Allow preflight
        if (HttpMethod.OPTIONS.matches(request.method)) return true
        if (whitePathMatcher != null && whitePathMatcher!!.matches(request)) return true
        return false
    }
}