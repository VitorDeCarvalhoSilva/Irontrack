package com.irontrack.api.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuração mínima do springdoc-openapi (07_ROADMAP_BACKEND.md §C.0, item
 * 7): documentação gerada automaticamente a partir das anotações dos
 * controllers (ainda vazios nesta sprint), acessível em /swagger-ui.html.
 * Já declara o esquema Bearer/JWT (03_CONTRATOS_API.md §1.3) para que os
 * controllers das próximas sprints não precisem repetir essa configuração.
 */
@Configuration
public class OpenApiConfig {

    private static final String BEARER_SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI irontrackOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("IronTrack API")
                        .description("Diário de bordo inteligente para musculação e hipertrofia.")
                        .version("v1"))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME_NAME))
                .components(new io.swagger.v3.oas.models.Components()
                        .addSecuritySchemes(BEARER_SCHEME_NAME, new SecurityScheme()
                                .name(BEARER_SCHEME_NAME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
