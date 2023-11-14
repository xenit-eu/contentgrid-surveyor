package com.contentgrid.surveyor.application.surveyor.boot;

import com.contentgrid.surveyor.api.pull.PullMetrics;
import com.contentgrid.surveyor.application.surveyor.autoconfigure.OptionalR2dbcAutoConfiguration;
import com.contentgrid.surveyor.drivers.schedule.SurveyorSchedulerConfiguration;
import com.contentgrid.surveyor.drivers.web.SurveyorWebConfiguration;
import com.contentgrid.surveyor.infrastructure.config.spring.SurveyorSpringConfiguration;
import com.contentgrid.surveyor.infrastructure.source.pegman.SurveyorSourceMetricsPegmanConfiguration;
import com.contentgrid.surveyor.spi.config.FindCollectionConfigurationsSpiPort;
import com.contentgrid.surveyor.spi.config.FindMeasurementAggregationConfigurationSpiPort;
import com.contentgrid.surveyor.spi.config.FindResourceDefinitionsSpiPort;
import com.contentgrid.surveyor.spi.source.EventMetricsSource;
import com.contentgrid.surveyor.spi.storage.AggregateMeasurementsSpiPort;
import com.contentgrid.surveyor.spi.storage.LastMeasurementSpiPort;
import com.contentgrid.surveyor.spi.storage.StoreMeasurementSpiPort;
import com.contentgrid.surveyor.usecase.metrics.FindMetricsUseCase;
import com.contentgrid.surveyor.usecase.pull.PullMetricsUseCase;
import java.util.List;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.r2dbc.R2dbcAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@Import({
        SurveyorWebConfiguration.class,
        SurveyorSchedulerConfiguration.class,
        SurveyorSpringConfiguration.class,
        SurveyorSourceMetricsPegmanConfiguration.class
})
@ImportAutoConfiguration(value = OptionalR2dbcAutoConfiguration.class, exclude = {
        R2dbcAutoConfiguration.class})
public class ContentgridSurveyorApplication {

    public static void main(String[] args) {
        SpringApplication.run(ContentgridSurveyorApplication.class, args);
    }

    @Bean
    PullMetrics pullMetrics(FindCollectionConfigurationsSpiPort findCollectionConfigurationsSpiPort,
            List<? extends EventMetricsSource> metricsSources,
            StoreMeasurementSpiPort storeMeasurementSpiPort,
            LastMeasurementSpiPort lastMeasurementSpiPort) {
        return new PullMetricsUseCase(metricsSources, findCollectionConfigurationsSpiPort, storeMeasurementSpiPort,
                lastMeasurementSpiPort);
    }

    @Bean
    FindMetricsUseCase findMetrics(
            FindMeasurementAggregationConfigurationSpiPort resourceAggregationConfigurationSpiPort,
            FindResourceDefinitionsSpiPort findResourceDefinitionsSpiPort,
            AggregateMeasurementsSpiPort aggregateMeasurementsSpiPort) {
        return new FindMetricsUseCase(resourceAggregationConfigurationSpiPort, aggregateMeasurementsSpiPort,
                findResourceDefinitionsSpiPort);
    }
}
