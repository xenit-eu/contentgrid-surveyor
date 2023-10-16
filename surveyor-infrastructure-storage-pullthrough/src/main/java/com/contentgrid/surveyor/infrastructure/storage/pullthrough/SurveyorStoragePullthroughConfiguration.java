package com.contentgrid.surveyor.infrastructure.storage.pullthrough;

import com.contentgrid.surveyor.spi.source.EventMetricsSource;
import com.contentgrid.surveyor.spi.source.MetricCollectionConfig;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class SurveyorStoragePullthroughConfiguration {

    @Bean
    PullthroughMetricsGateway pullthroughMetricsGateway(
            List<? extends EventMetricsSource> eventMetricsSources,
            List<MetricCollectionConfig> metricCollectionConfigs
    ) {
        return new PullthroughMetricsGateway(eventMetricsSources, metricCollectionConfigs);
    }
}
