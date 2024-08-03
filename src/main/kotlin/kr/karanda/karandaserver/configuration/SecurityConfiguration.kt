package kr.karanda.karandaserver.configuration

import kr.karanda.karandaserver.filter.AuthorizationFilter
import kr.karanda.karandaserver.util.TokenFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.invoke
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

@Configuration
@EnableWebSecurity
class SecurityConfiguration(val tokenFactory: TokenFactory) {

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val configuration = CorsConfiguration()
        configuration.allowedOrigins = listOf(
            "https://www.karanda.kr",
            "https://karanda.kr",
            "https://hammuu1112.github.io",
            "http://localhost:8082",
            "http://localhost:2345",
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
        http {
            addFilterBefore<UsernamePasswordAuthenticationFilter>(AuthorizationFilter(tokenFactory))
            authorizeRequests {
                authorize("/docs", permitAll)
                authorize("/swagger-ui/*", permitAll)
                authorize("/auth/*", permitAll)
                authorize("/chzzk/*", permitAll)
                authorize(anyRequest, authenticated)
            }
        }
        return http.build()
    }

}