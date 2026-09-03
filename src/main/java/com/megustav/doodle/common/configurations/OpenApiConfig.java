package com.megustav.doodle.common.configurations;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI doodleOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Doodle Scheduling API")
                .description("Time slot management and meeting scheduling")
                .version("v1"));
    }

}
