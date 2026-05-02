package com.linkforge.urlshortener.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// OpenAPI / Swagger UI configuration for LinkForge API documentation
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI linkForgeOpenAPI() {
        // Define the Bearer JWT security scheme
        SecurityScheme bearerScheme = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("Enter your JWT access token. Obtain it from POST /api/v1/auth/login");

        // Apply the security scheme globally to all endpoints
        SecurityRequirement securityRequirement = new SecurityRequirement()
                .addList("bearerAuth");

        return new OpenAPI()
                .info(new Info()
                        .title("LinkForge URL Shortener API")
                        .version("1.0.0")
                        .description("Production-grade URL Shortener REST API with JWT authentication, " +
                                "custom aliases, expiration, click limits, and export features.")
                        .contact(new Contact()
                                .name("LinkForge")
                                .email("support@linkforge.io")))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", bearerScheme))
                .addSecurityItem(securityRequirement);
    }
}
