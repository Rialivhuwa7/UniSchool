package co.za.unicard.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

/**
 * Describes the API for the generated spec at /v3/api-docs. Every endpoint expects the JWT
 * from POST /api/auth/login as a bearer token; in Swagger UI press "Authorize" and paste it.
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(title = "UniSchool API", version = "v1", description = "UniSchool by Unicard: one app for everything school"),
        security = @SecurityRequirement(name = OpenApiConfig.BEARER))
@SecurityScheme(name = OpenApiConfig.BEARER, type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
public class OpenApiConfig {
    public static final String BEARER = "bearer-jwt";
}
