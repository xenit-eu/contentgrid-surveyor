package com.contentgrid.surveyor.infrastructure.storage.pullthrough;

import com.contentgrid.surveyor.spi.config.FindCollectionConfigurationsSpiPort;
import com.contentgrid.surveyor.spi.source.EventMetricsSource;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class SurveyorStoragePullthroughConfiguration {

    @Bean
    PullthroughMetricsGateway pullthroughMetricsGateway(
            List<? extends EventMetricsSource> eventMetricsSources,
            FindCollectionConfigurationsSpiPort findCollectionConfigurationsSpiPort
    ) {
        return new PullthroughMetricsGateway(eventMetricsSources, findCollectionConfigurationsSpiPort);
    }
}
