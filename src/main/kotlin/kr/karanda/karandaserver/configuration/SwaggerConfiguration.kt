package kr.karanda.karandaserver.configuration

import io.swagger.v3.oas.annotations.OpenAPIDefinition
import io.swagger.v3.oas.annotations.info.Info
import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@OpenAPIDefinition(
    info = Info(
        title = "Karanda Server",
        version = "1.0.0",
        description = "Karanda Server Docs"
    )
)

@Configuration
class SwaggerConfiguration {
    val TOKEN_PREFIX = "Bearer"

    @Bean
    fun openApi(): OpenAPI {
        val securityRequirements = SecurityRequirement().addList("Qualifications")
        val securityScheme = SecurityScheme()
            .type(SecurityScheme.Type.HTTP)
            .`in`(SecurityScheme.In.HEADER)
            .name("Qualifications")
            .scheme(TOKEN_PREFIX)
            .bearerFormat("JWT")
        val components = Components().addSecuritySchemes("Qualifications", securityScheme)
        return OpenAPI().addSecurityItem(securityRequirements).components(components)
    }
}