package com.smartinventory.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Smart Inventory Management System (SIMS) API")
                        .version("1.0.0")
                        .description("REST API documentation for Smart Inventory Management System")
                        .contact(new Contact()
                                .name("SIMS Engineering Team")
                                .email("support@smartinventory.com"))
                        .license(new License()
                                .name("Proprietary")
                                .url("https://smartinventory.com")));
    }
}
