package com.kfokam48.attendance.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Metadata of the generated OpenAPI document shown in Swagger UI (issue #37). */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI attendanceOpenApi() {
        return new OpenAPI().info(new Info()
                .title("KFOKAM48 — Attendance & Peer Review Tracker")
                .version("1.2")
                .description("Generated from the code. The frozen contract api/contrat.yaml is the source of truth; "
                        + "every error body is {\"code\", \"message\"} with its real HTTP status (DEC-9)."));
    }
}
