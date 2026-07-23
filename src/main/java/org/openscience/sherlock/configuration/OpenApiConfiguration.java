package org.openscience.sherlock.configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfiguration {

    public static final String BASIC_AUTH_SCHEME = "basicAuth";
    private static final String PUBLIC_GROUP = "public";
    private static final String INTERNAL_GROUP = "internal";

    @Bean
    public OpenAPI sherlockOpenApi(@Value("${sherlock.version}") final String sherlockVersion) {
        return new OpenAPI()
                .info(new Info()
                        .title("Sherlock REST API")
                        .version(sherlockVersion)
                        .description(
                                "REST endpoints for Sherlock query processing, result retrieval, dataset management, and statistics services."))
                .components(new Components().addSecuritySchemes(
                        BASIC_AUTH_SCHEME,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("basic")
                                .description(
                                        "Authenticate with the configured Spring Security username and password to access protected endpoints.")));
    }

    @Bean
    public GroupedOpenApi publicOpenApi(@Value("${sherlock.version}") final String sherlockVersion) {
        return GroupedOpenApi.builder()
                .group(PUBLIC_GROUP)
                .pathsToMatch(WebSecurityConfiguration.PUBLIC_SERVICE_PATHS)
                .addOpenApiCustomizer(openApi -> openApi.info(buildInfo(
                        "Sherlock Core Service API",
                        sherlockVersion,
                        "OpenAPI description for the public Sherlock core service endpoints.")))
                .build();
    }

    @Bean
    public GroupedOpenApi internalOpenApi(@Value("${sherlock.version}") final String sherlockVersion) {
        return GroupedOpenApi.builder()
                .group(INTERNAL_GROUP)
                .pathsToExclude(WebSecurityConfiguration.PUBLIC_SERVICE_PATHS)
                .addOpenApiCustomizer(openApi -> openApi.info(buildInfo(
                        "Sherlock Protected REST API",
                        sherlockVersion,
                        "OpenAPI description for internal, authenticated Sherlock REST endpoints.")))
                .build();
    }

    @Bean
    public OpenApiCustomizer securedOperationsCustomizer() {
        return openApi -> {
            if (openApi.getPaths() == null) {
                return;
            }

            openApi.getPaths().forEach((path, pathItem) -> {
                if (isPublicServicePath(path)) {
                    return;
                }

                pathItem.readOperations().forEach(operation -> {
                    if (operation.getSecurity() == null || operation.getSecurity().isEmpty()) {
                        operation.addSecurityItem(new SecurityRequirement().addList(BASIC_AUTH_SCHEME));
                    }
                    operation.getResponses().putIfAbsent("401",
                            new ApiResponse().description("Authentication is required to access this endpoint."));
                    operation.getResponses().putIfAbsent("403",
                            new ApiResponse()
                                    .description("The authenticated user is not allowed to access this endpoint."));
                });
            });
        };
    }

    private boolean isPublicServicePath(final String path) {
        for (final String publicServicePath : WebSecurityConfiguration.PUBLIC_SERVICE_PATHS) {
            if (publicServicePath.equals(path)) {
                return true;
            }
        }
        return false;
    }

    private Info buildInfo(final String title, final String version, final String description) {
        return new Info()
                .title(title)
                .version(version)
                .description(description);
    }
}