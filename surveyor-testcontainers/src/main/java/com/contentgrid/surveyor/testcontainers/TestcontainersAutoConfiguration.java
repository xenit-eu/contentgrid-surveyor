package com.contentgrid.surveyor.testcontainers;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.r2dbc.R2dbcAutoConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.testcontainers.service.connection.ServiceConnectionAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@AutoConfiguration
@AutoConfigureBefore(ServiceConnectionAutoConfiguration.class)
@Profile("testcontainers")
@ImportAutoConfiguration(R2dbcAutoConfiguration.class)
public class TestcontainersAutoConfiguration {

    private static final DockerImageName TIMESCALE_IMAGE = DockerImageName.parse("timescale/timescaledb:latest-pg14")
            .asCompatibleSubstituteFor("postgres");

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgresTestContainer() {
        return new PostgreSQLContainer(TIMESCALE_IMAGE);
    }
}
