package kr.karanda.karandaserver.configuration

import jakarta.servlet.http.HttpServletRequest
import kr.karanda.karandaserver.filter.AuthorizationFilter
import kr.karanda.karandaserver.filter.QualificationFilter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.invoke
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.security.web.util.matcher.RequestMatcher
import org.springframework.util.AntPathMatcher
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

@Configuration
@EnableWebSecurity
class SecurityConfiguration(val authorizationFilter: AuthorizationFilter, val qualificationFilter: QualificationFilter) {

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val configuration = CorsConfiguration()
        configuration.allowedOrigins = listOf(
            "https://www.karanda.kr",
            "https://karanda.kr",
            //"https://hammuu1112.github.io",
            "http://localhost:8082",
            "http://localhost:2345",
            "https://karanda-384102.web.app",
            "https://karanda-384102.firebaseapp.com"
        )
        configuration.allowedMethods = listOf("*")
        configuration.allowedHeaders = listOf("*")
        //configuration.allowCredentials = true
        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", configuration)
        return source
    }

    /**
     * Spring Security 7은 securityMatcher 없이 "any request"에 매칭되는 SecurityFilterChain이
     * 둘 이상이면 기동을 거부한다. (기존 3개 체인 → 단일 체인으로 통합)
     *
     * 필터 적용 순서: 기본 설정(cors/csrf/formLogin/session) → qualification → authorization.
     */
    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        val whitePathMatcher = object : RequestMatcher {
            override fun matches(request: HttpServletRequest): Boolean {
                val paths = listOf(
                    "/docs",
                    "/api-docs",
                    "/api-docs/*",
                    "/swagger-ui/*",
                    "/auth/discord/authenticate/**",
                    "/chzzk/*",
                    "/live-channel",
                    "/live-data/**",
                    "/trade-market/**",
                    "/party-finder/post",
                    "/party-finder/posts",
                )
                val matcher = AntPathMatcher()
                for(path in paths) {
                    if(matcher.match(path, request.requestURI)) return true
                }
                return false
            }
        }
        authorizationFilter.setWhitePathMatcher(whitePathMatcher)
        http {
            cors {
                configurationSource = corsConfigurationSource()
            }
            csrf {
                disable()
            }
            formLogin {
                disable()
            }
            sessionManagement {
                sessionCreationPolicy = SessionCreationPolicy.STATELESS
            }
            authorizeHttpRequests {
                authorize(whitePathMatcher, permitAll)
                authorize(anyRequest, authenticated)
            }
            // authorizationFilter를 먼저 등록해 순서 기준점으로 삼고,
            // qualificationFilter를 그 앞에 배치 → 적용 순서: qualification → authorization
            addFilterBefore<UsernamePasswordAuthenticationFilter>(authorizationFilter)
            addFilterBefore<AuthorizationFilter>(qualificationFilter)
        }
        return http.build()
    }
}
