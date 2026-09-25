package com.kfokam48.attendance.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.containers.PostgreSQLContainer;

import java.util.Map;

/**
 * Base of every integration test (B6 + ENF5): one PostgreSQL container shared by all
 * test classes (singleton pattern), so the Spring context is cached and started once.
 * Flyway applies V1 (schema) and V2 (demo data) inside the container.
 * Tests must not rely on row counts beyond the demo data: the database is shared.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@ContextConfiguration(initializers = DockerDesktopApiVersionConfig.class)
public abstract class AbstractPostgresIntegrationTest {

    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine");

    static {
        POSTGRES.start();
    }

    /** Promotion created by the V2 demo-data migration. */
    protected static final long DEMO_PROMOTION_ID = 1L;

    @Autowired
    protected TestRestTemplate rest;

    /** Opens a fresh session of the demo promotion and returns its 201 body. */
    protected Map<String, Object> openSession() {
        return rest.postForEntity("/api/sessions",
                Map.of("titre", "Test session", "promotionId", DEMO_PROMOTION_ID), Map.class).getBody();
    }

    protected ResponseEntity<Map> markAttendance(Object code, long studentId) {
        return rest.postForEntity("/api/presences", Map.of("code", code, "etudiantId", studentId), Map.class);
    }
}
