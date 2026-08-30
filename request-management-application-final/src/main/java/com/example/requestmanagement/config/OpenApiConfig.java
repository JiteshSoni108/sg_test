package com.example.requestmanagement.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class OpenApiConfig {

    @Bean
    OpenAPI requestManagementOpenAPI() {
        return new OpenAPI().info(new Info().title("Request Management API").version("1.0.0").description("REST API for managing the lifecycle of requests.").contact(new Contact().name("Request Management Team")).license(new License().name("Coding Exercise"))).components(new Components().addSecuritySchemes("basicAuth", new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("basic")));
    }
}
