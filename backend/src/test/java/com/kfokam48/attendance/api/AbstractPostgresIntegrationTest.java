package com.kfokam48.attendance.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.containers.PostgreSQLContainer;

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
}
