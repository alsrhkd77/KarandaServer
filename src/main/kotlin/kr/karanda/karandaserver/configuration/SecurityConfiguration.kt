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
            "https://hammuu1112.github.io",
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

    @Bean
    fun formLoginFilterChain(http: HttpSecurity): SecurityFilterChain {
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
        }
        return http.build()
    }

    @Bean
    fun authorizationFilterChain(http: HttpSecurity): SecurityFilterChain {
        val whitePathMatcher = object : RequestMatcher {
            override fun matches(request: HttpServletRequest?): Boolean {
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
                    "/adventurer-hub/post",
                    "/adventurer-hub/posts",
                )
                if (request == null) return false
                val matcher = AntPathMatcher()
                for(path in paths) {
                    if(matcher.match(path, request.requestURI)) return true
                }
                return false
            }
        }
        authorizationFilter.setWhitePathMatcher(whitePathMatcher)
        http {
            authorizeRequests {
                authorize(whitePathMatcher, permitAll)
                authorize(anyRequest, authenticated)
            }
            addFilterBefore<UsernamePasswordAuthenticationFilter>(authorizationFilter)
        }
        return http.build()
    }

    @Bean
    fun qualificationFilterChain(http: HttpSecurity): SecurityFilterChain {
        http {
            addFilterBefore<UsernamePasswordAuthenticationFilter>(qualificationFilter)
        }
        return http.build()
    }
}
