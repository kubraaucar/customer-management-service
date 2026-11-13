package com.example.customermanagementservice.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger UI (/swagger-ui.html): adds an "Authorize" button where you paste the token
 * returned by /api/auth/login, so the other endpoints can be tried out.
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(title = "Customer Management API", version = "v1"),
        security = @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH))
@SecurityScheme(name = OpenApiConfig.BEARER_AUTH, type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
public class OpenApiConfig {

    static final String BEARER_AUTH = "bearerAuth";
}
