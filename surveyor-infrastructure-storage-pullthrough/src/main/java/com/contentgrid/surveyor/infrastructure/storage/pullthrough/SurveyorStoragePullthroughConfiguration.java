package com.contentgrid.surveyor.infrastructure.storage.pullthrough;

import com.contentgrid.surveyor.spi.config.FindCollectionConfigurationsSpiPort;
import com.contentgrid.surveyor.spi.collector.MeasurementCollector;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class SurveyorStoragePullthroughConfiguration {

    @Bean
    PullthroughMetricsGateway pullthroughMetricsGateway(
            List<? extends MeasurementCollector> measurementCollectors,
            FindCollectionConfigurationsSpiPort findCollectionConfigurationsSpiPort
    ) {
        return new PullthroughMetricsGateway(measurementCollectors, findCollectionConfigurationsSpiPort);
    }
}
