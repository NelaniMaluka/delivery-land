package com.deliveryland.backend.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("DeliveryLand API")
                        .version("v1")
                        .description("API documentation for the DeliveryLand backend – " +
                                "a platform for managing food/goods delivery orders, drivers, customers and restaurants")
                        .contact(new Contact()
                                .name("Nelani Maluka")
                                .email("Malukanelani@gmail.com")
                                .url("https://github.com/NelaniMaluka/delivery-land"))
                        .license(new License()
                                .name("MIT")
                                .url("https://opensource.org/licenses/MIT"))
                )
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8080")
                                .description("Local development server"),
                        new Server()
                                .url("https://delivery-land-backend.onrender.com") // ← update to your actual Render / production URL
                                .description("Production server (Render)")
                ))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("JWT Authorization header using the Bearer scheme. " +
                                                "Example: \"Authorization: Bearer {token}\""))
                );
    }
}