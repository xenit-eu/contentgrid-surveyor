package com.contentgrid.surveyor.application.surveyor.boot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
class TestContentgridSurveyorApplication {

    private static final DockerImageName TIMESCALE_IMAGE = DockerImageName.parse("timescale/timescaledb:latest-pg14")
            .asCompatibleSubstituteFor("postgres");

    @Bean
    @ServiceConnection
    static PostgreSQLContainer postgresContainer() {
        return new PostgreSQLContainer<>(TIMESCALE_IMAGE);
    }


    public static void main(String[] args) {
        SpringApplication.from(ContentgridSurveyorApplication::main)
                .with(TestContentgridSurveyorApplication.class)
                .run(args);
    }

}
