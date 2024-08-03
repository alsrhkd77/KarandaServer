package kr.karanda.karandaserver.filter

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Component
import org.springframework.util.AntPathMatcher
import org.springframework.web.filter.OncePerRequestFilter

@Component
class QualificationFilter: OncePerRequestFilter() {

    val whiteList = listOf("/swagger-ui/*", "/docs")

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        //TODO: 1. check header
        if(request.headerNames.toList().contains("Qualifications")){
            println("Has Qualifications")
        } else {
            println("No Qualifications")
        }
        //TODO: 2. get token from header
        //TODO: 3. validate token
        //response.status = HttpServletResponse.SC_UNAUTHORIZED
        //return    //이후 로직 실행 없이 리턴
        filterChain.doFilter(request, response)
    }

    override fun shouldNotFilter(request: HttpServletRequest): Boolean {
        // Allow preflight
        if (HttpMethod.OPTIONS.matches(request.method)) {
            return true
        }

        val path = request.requestURI
        val pathMatcher = AntPathMatcher()
        for(white in whiteList) {
            if(pathMatcher.match(white, path)){
                return true
            }
        }
        return false
        //return super.shouldNotFilter(request)
    }
}