package com.contentgrid.surveyor.infrastructure.storage.r2dbc;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@SpringBootApplication(proxyBeanMethods = false)
@Import(SurveyorStorageDataR2dbcConfiguration.class)
class TestApplicationConfiguration {

    @Bean
    @ServiceConnection
    PostgreSQLContainer<?> postgresContainer() {
        return new PostgreSQLContainer<>(DockerImageName.parse("timescale/timescaledb:latest-pg14")
                .asCompatibleSubstituteFor("postgres"));
    }

}
