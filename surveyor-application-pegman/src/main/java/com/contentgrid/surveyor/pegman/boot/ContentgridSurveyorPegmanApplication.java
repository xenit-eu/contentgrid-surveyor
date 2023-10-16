package com.contentgrid.surveyor.pegman.boot;

import com.contentgrid.surveyor.drivers.web.SurveyorWebConfiguration;
import com.contentgrid.surveyor.infrastructure.config.spring.SurveyorSpringConfiguration;
import com.contentgrid.surveyor.infrastructure.source.prometheus.SurveyorSourceMetricsPrometheusConfiguration;
import com.contentgrid.surveyor.infrastructure.storage.pullthrough.SurveyorStoragePullthroughConfiguration;
import com.contentgrid.surveyor.spi.config.FindResourceAggregationConfigurationSpiPort;
import com.contentgrid.surveyor.spi.config.FindResourceDefinitionsSpiPort;
import com.contentgrid.surveyor.spi.storage.AggregateEventCountMetricSpiPort;
import com.contentgrid.surveyor.usecase.metrics.FindMetricsUseCase;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@Import({
        SurveyorWebConfiguration.class,
        SurveyorSpringConfiguration.class,
        SurveyorStoragePullthroughConfiguration.class,
        SurveyorSourceMetricsPrometheusConfiguration.class
})
public class ContentgridSurveyorPegmanApplication {

    public static void main(String[] args) {
        SpringApplication.run(ContentgridSurveyorPegmanApplication.class, args);
    }

    @Bean
    FindMetricsUseCase findMetrics(FindResourceAggregationConfigurationSpiPort resourceAggregationConfigurationSpiPort,
            FindResourceDefinitionsSpiPort findResourceDefinitionsSpiPort,
            AggregateEventCountMetricSpiPort aggregateEventCountMetricSpiPort) {
        return new FindMetricsUseCase(resourceAggregationConfigurationSpiPort, aggregateEventCountMetricSpiPort,
                findResourceDefinitionsSpiPort);
    }
}
