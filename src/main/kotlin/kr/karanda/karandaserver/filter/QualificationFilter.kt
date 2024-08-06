package kr.karanda.karandaserver.filter

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import kr.karanda.karandaserver.util.TokenFactory
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Component
import org.springframework.util.AntPathMatcher
import org.springframework.web.filter.OncePerRequestFilter

@Component
class QualificationFilter(private val tokenFactory: TokenFactory) : OncePerRequestFilter() {

    val whiteList = listOf("/swagger-ui/*", "/docs")

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        var result = false
        if (request.headerNames.toList().contains("Qualification")) {
            val token: String = request.getHeader("Qualification")
            tokenFactory.validateQualificationToken(token).apply {
                result = this
            }
        }
        if (result) {
            filterChain.doFilter(request, response)
        } else {
            response.status = HttpServletResponse.SC_UNAUTHORIZED
            return    //이후 로직 실행 없이 반환
        }
    }

    override fun shouldNotFilter(request: HttpServletRequest): Boolean {
        // Allow preflight
        if (HttpMethod.OPTIONS.matches(request.method)) {
            return true
        }

        val path = request.requestURI
        val pathMatcher = AntPathMatcher()
        for (white in whiteList) {
            if (pathMatcher.match(white, path)) {
                return true
            }
        }
        return false
        //return super.shouldNotFilter(request)
    }
}