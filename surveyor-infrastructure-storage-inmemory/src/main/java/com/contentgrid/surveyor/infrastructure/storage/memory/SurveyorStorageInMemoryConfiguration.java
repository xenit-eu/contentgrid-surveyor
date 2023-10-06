package com.contentgrid.surveyor.infrastructure.storage.memory;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class SurveyorStorageInMemoryConfiguration {

    @Bean
    InMemoryMetricsGateway inMemoryMetricsGateway() {
        return new InMemoryMetricsGateway();
    }
}
