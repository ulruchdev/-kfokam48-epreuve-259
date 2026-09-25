package com.kfokam48.attendance.api;

import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Docker Desktop 29+ requires a Docker API version >= 1.40, while the docker-java
 * client embedded in Testcontainers negotiates 1.32 by default and gets rejected
 * with "client version 1.32 is too old". This initializer pins the API version
 * for every test JVM through system properties, which docker-java honors before
 * its own defaults.
 *
 * It also pins the Docker host to the Docker Desktop Linux engine pipe
 * (the legacy docker_engine pipe answers with an empty-info 400).
 */
public class DockerDesktopApiVersionConfig
        implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Override
    public void initialize(ConfigurableApplicationContext context) {
        TestPropertyValues.of(
                "DOCKER_HOST=npipe:////./pipe/dockerDesktopLinuxEngine",
                "api.version=1.44",
                "DOCKER_API_VERSION=1.44"
        ).applyTo(context.getEnvironment());
        // Expose them as JVM system properties too (docker-java reads those on class load).
        System.setProperty("api.version", "1.44");
        System.setProperty("DOCKER_API_VERSION", "1.44");
        System.setProperty("DOCKER_HOST", "npipe:////./pipe/dockerDesktopLinuxEngine");
    }
}
