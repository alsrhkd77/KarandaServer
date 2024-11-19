package kr.karanda.karandaserver.filter

import io.jsonwebtoken.ExpiredJwtException
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import kr.karanda.karandaserver.dto.User
import kr.karanda.karandaserver.util.TokenFactory
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
        val token = getToken(request.getHeader(headerName))
        if (request.requestURI.equals("/auth/discord/refresh")) {
            val refreshToken = getToken(request.getHeader("refresh-token"))
            if (token != null && refreshToken != null && tokenFactory.validateRefreshToken(refreshToken)) {
                var uuid: String? = null
                try {
                    tokenFactory.getAuthentication(token).let {
                        uuid = (it.principal as User).userUUID
                    }
                } catch (e: ExpiredJwtException) {
                    uuid = e.claims.subject
                } catch (e: Exception) {
                    //잘못된 토큰
                    print(e)
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized")
                    return
                }

                val authentication = tokenFactory.getAuthenticationFromRefreshToken(refreshToken)
                if (uuid != null && (authentication.principal as User).userUUID == uuid) {
                    SecurityContextHolder.getContext().authentication = authentication
                } else {
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized")
                    return
                }
            }
        } else {
            if (token != null && tokenFactory.validateAccessToken(token)) {
                tokenFactory.getAuthentication(token).let {
                    SecurityContextHolder.getContext().authentication = it
                }
            } else {
                println("토큰 검증 실패\n ${token}")
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized")
                return
            }
        }
        filterChain.doFilter(request, response)
    }

    override fun shouldNotFilter(request: HttpServletRequest): Boolean {
        if (request.method == "OPTIONS") return true
        if (whitePathMatcher != null && whitePathMatcher!!.matches(request)) return true
        return false
    }

    private fun getToken(value: String): String? {
        val items = value.split(" ")
        if (items.size == 2 && items.first() == "Bearer") {
            println("Token found")
            return items.last()
        }
        println("Token not found")
        return null
    }
}