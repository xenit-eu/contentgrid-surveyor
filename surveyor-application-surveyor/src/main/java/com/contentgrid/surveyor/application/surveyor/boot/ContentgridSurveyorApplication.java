package com.contentgrid.surveyor.application.surveyor.boot;

import com.contentgrid.surveyor.api.pull.PullMetrics;
import com.contentgrid.surveyor.api.resources.LinkResources;
import com.contentgrid.surveyor.application.surveyor.autoconfigure.OptionalR2dbcAutoConfiguration;
import com.contentgrid.surveyor.drivers.schedule.SurveyorSchedulerConfiguration;
import com.contentgrid.surveyor.drivers.web.SurveyorWebConfiguration;
import com.contentgrid.surveyor.infrastructure.config.spring.SurveyorSpringConfiguration;
import com.contentgrid.surveyor.infrastructure.resourcelinkage.captain.SurveyorResourceLinkageCaptainConfiguration;
import com.contentgrid.surveyor.infrastructure.collector.pegman.SurveyorMeasurementCollectorPegmanConfiguration;
import com.contentgrid.surveyor.spi.config.FindCollectionConfigurationsSpiPort;
import com.contentgrid.surveyor.spi.config.FindMeasurementAggregationConfigurationSpiPort;
import com.contentgrid.surveyor.spi.config.FindResourceDefinitionsSpiPort;
import com.contentgrid.surveyor.spi.resources.FindUnlinkedResourcesSpiPort;
import com.contentgrid.surveyor.spi.resources.LinkResourceSpiPort;
import com.contentgrid.surveyor.spi.resources.LookupResourceLinkSpiPort;
import com.contentgrid.surveyor.spi.collector.MeasurementCollector;
import com.contentgrid.surveyor.spi.storage.AggregateMeasurementsSpiPort;
import com.contentgrid.surveyor.spi.storage.LastMeasurementSpiPort;
import com.contentgrid.surveyor.spi.storage.StoreMeasurementSpiPort;
import com.contentgrid.surveyor.usecase.metrics.FindMetricsUseCase;
import com.contentgrid.surveyor.usecase.pull.PullMetricsUseCase;
import com.contentgrid.surveyor.usecase.resources.LinkResourcesUseCase;
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
        SurveyorMeasurementCollectorPegmanConfiguration.class,
        SurveyorResourceLinkageCaptainConfiguration.class
})
@ImportAutoConfiguration(value = OptionalR2dbcAutoConfiguration.class, exclude = {
        R2dbcAutoConfiguration.class})
public class ContentgridSurveyorApplication {

    public static void main(String[] args) {
        SpringApplication.run(ContentgridSurveyorApplication.class, args);
    }

    @Bean
    PullMetrics pullMetrics(FindCollectionConfigurationsSpiPort findCollectionConfigurationsSpiPort,
            List<? extends MeasurementCollector> measurementCollectors,
            StoreMeasurementSpiPort storeMeasurementSpiPort,
            LastMeasurementSpiPort lastMeasurementSpiPort) {
        return new PullMetricsUseCase(measurementCollectors, findCollectionConfigurationsSpiPort,
                storeMeasurementSpiPort,
                lastMeasurementSpiPort);
    }

    @Bean
    LinkResources linkResources(
            FindUnlinkedResourcesSpiPort findUnlinkedResourcesSpiPort,
            LinkResourceSpiPort linkResourceSpiPort,
            LookupResourceLinkSpiPort lookupResourceLinkSpiPort
    ) {
        return new LinkResourcesUseCase(
                findUnlinkedResourcesSpiPort,
                linkResourceSpiPort,
                lookupResourceLinkSpiPort
        );
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
