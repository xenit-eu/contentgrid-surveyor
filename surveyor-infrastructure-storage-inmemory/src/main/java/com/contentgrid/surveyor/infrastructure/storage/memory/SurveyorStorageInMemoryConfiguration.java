package com.contentgrid.surveyor.infrastructure.storage.memory;

import com.contentgrid.surveyor.spi.resources.CreateMetricSpiPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class SurveyorStorageInMemoryConfiguration {

    @Bean
    InMemoryResourceGateway inMemoryResourceLinkGateway() {
        return new InMemoryResourceGateway();
    }

    @Bean
    InMemoryMeasurementGateway inMemoryMetricsGateway(CreateMetricSpiPort createMetricSpiPort) {
        return new InMemoryMeasurementGateway(createMetricSpiPort);
    }
}
