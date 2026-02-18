package com.meli.productapi.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI / Swagger configuration for the Product API.
 * Defines API metadata, contact info, the API Key security scheme,
 * and explicit server URLs for HTTP/HTTPS environments.
 */
@Configuration
public class OpenApiConfig {

    private static final String API_KEY_SCHEME_NAME = "ApiKeyAuth";

    @Value("${api.server.url:http://localhost:8080}")
    private String serverUrl;

    @Bean
    public OpenAPI productApiOpenAPI() {
        Server server = new Server()
                .url(serverUrl)
                .description("Product API Server");

        return new OpenAPI()
                .servers(List.of(server))
                .info(new Info()
                        .title("Product API — Mercado Libre Challenge")
                        .description("REST API for product management, filtering, pagination, and multi-product comparison by type.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Bruno Henrique Pedrosa Tufy Melo")
                                .email("brunohpedrosa@gmail.com")))
                .addSecurityItem(new SecurityRequirement().addList(API_KEY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(API_KEY_SCHEME_NAME, new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name("X-API-KEY")
                                .description("API key required to access the endpoints. Pass via the X-API-KEY header.")));
    }
}
