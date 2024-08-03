package kr.karanda.karandaserver.filter

import io.jsonwebtoken.ExpiredJwtException
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import kr.karanda.karandaserver.dto.User
import kr.karanda.karandaserver.util.TokenFactory
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.filter.OncePerRequestFilter

class AuthorizationFilter(private val tokenFactory: TokenFactory) : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val token = getToken(request.getHeader("Authorization"))
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
                }

                val authentication = tokenFactory.getAuthenticationFromRefreshToken(refreshToken)
                if (uuid != null && (authentication.principal as User).userUUID == uuid) {
                    SecurityContextHolder.getContext().authentication = authentication
                }
            }
        } else {
            if (token != null && tokenFactory.validateAccessToken(token)) {
                tokenFactory.getAuthentication(token).let {
                    SecurityContextHolder.getContext().authentication = it
                }
            }
        }
        filterChain.doFilter(request, response)
    }

    private fun getToken(value: String): String? {
        val items = value.split(" ")
        if (items.size == 2 && items.first() == "Bearer") {
            return items.last()
        }
        return null
    }
}