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
        version = "0.0.1",
        description = "Karanda Server Docs"
    )
)

@Configuration
class SwaggerConfiguration {
    val TOKEN_PREFIX = "Bearer"

    @Bean
    fun openApi(): OpenAPI {
        val securityRequirements = SecurityRequirement().addList("Qualifications", "Authorization")

        val qualificationScheme = SecurityScheme()
            .type(SecurityScheme.Type.APIKEY)
            .`in`(SecurityScheme.In.HEADER)
            .name("Qualifications")
            .scheme(TOKEN_PREFIX)
            .bearerFormat("JWT")

        val authenticationScheme = SecurityScheme()
            .type(SecurityScheme.Type.APIKEY)
            .`in`(SecurityScheme.In.HEADER)
            .name("Authorization")
            .scheme(TOKEN_PREFIX)
            .bearerFormat("JWT")

        val components = Components()
            .addSecuritySchemes("Qualifications", qualificationScheme)
            .addSecuritySchemes("Authorization", authenticationScheme)

        return OpenAPI().addSecurityItem(securityRequirements).components(components)
    }
}