package com.lukianchykov.ordermanagementapplication.configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {
    
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Order Management API")
                        .version("1.0.0")
                        .description("REST API для управления заказами и клиентами")
                        .contact(new Contact()
                                .name("Development Team")
                                .email("dev@company.com")));
    }
}