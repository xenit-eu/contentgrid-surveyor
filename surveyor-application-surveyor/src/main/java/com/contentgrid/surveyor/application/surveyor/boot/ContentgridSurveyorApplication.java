package com.contentgrid.surveyor.application.surveyor.boot;

import com.contentgrid.surveyor.api.pull.PullMetrics;
import com.contentgrid.surveyor.drivers.schedule.SurveyorSchedulerConfiguration;
import com.contentgrid.surveyor.drivers.web.SurveyorWebConfiguration;
import com.contentgrid.surveyor.infrastructure.config.spring.SurveyorSpringConfiguration;
import com.contentgrid.surveyor.infrastructure.source.pegman.SurveyorSourceMetricsPegmanConfiguration;
import com.contentgrid.surveyor.infrastructure.storage.jdbc.SurveyorStorageDataJdbcConfiguration;
import com.contentgrid.surveyor.infrastructure.storage.memory.SurveyorStorageInMemoryConfiguration;
import com.contentgrid.surveyor.spi.config.FindResourceAggregationConfigurationSpiPort;
import com.contentgrid.surveyor.spi.config.FindResourceDefinitionsSpiPort;
import com.contentgrid.surveyor.spi.source.EventMetricsSource;
import com.contentgrid.surveyor.spi.source.MetricCollectionConfig;
import com.contentgrid.surveyor.spi.storage.AggregateEventCountMetricSpiPort;
import com.contentgrid.surveyor.spi.storage.LastEventCountMetricSpiPort;
import com.contentgrid.surveyor.spi.storage.StoreEventCountMetricSpiPort;
import com.contentgrid.surveyor.application.surveyor.boot.autoconfigure.OptionalDataSourceAutoConfiguration;
import com.contentgrid.surveyor.usecase.metrics.FindMetricsUseCase;
import com.contentgrid.surveyor.usecase.pull.PullMetricsUseCase;
import java.util.List;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@Import({
        SurveyorWebConfiguration.class,
        SurveyorSchedulerConfiguration.class,
        SurveyorSpringConfiguration.class,
        SurveyorSourceMetricsPegmanConfiguration.class
})
@ImportAutoConfiguration(value = OptionalDataSourceAutoConfiguration.class, exclude = {
        DataSourceAutoConfiguration.class})
public class ContentgridSurveyorApplication {

    public static void main(String[] args) {
        SpringApplication.run(ContentgridSurveyorApplication.class, args);
    }

    @Bean
    PullMetrics pullMetrics(List<MetricCollectionConfig> collectionConfigs,
            List<? extends EventMetricsSource> metricsSources,
            StoreEventCountMetricSpiPort storeEventCountMetricSpiPort,
            LastEventCountMetricSpiPort lastEventCountMetricSpiPort) {
        return new PullMetricsUseCase(metricsSources, collectionConfigs, storeEventCountMetricSpiPort,
                lastEventCountMetricSpiPort);
    }

    @Bean
    FindMetricsUseCase findMetrics(FindResourceAggregationConfigurationSpiPort resourceAggregationConfigurationSpiPort,
            FindResourceDefinitionsSpiPort findResourceDefinitionsSpiPort,
            AggregateEventCountMetricSpiPort aggregateEventCountMetricSpiPort) {
        return new FindMetricsUseCase(resourceAggregationConfigurationSpiPort, aggregateEventCountMetricSpiPort,
                findResourceDefinitionsSpiPort);
    }

    @ConditionalOnProperty("spring.datasource.url")
    @Import(SurveyorStorageDataJdbcConfiguration.class)
    @Configuration(proxyBeanMethods = false)
    static class StorageDatabase {

    }

    @ConditionalOnProperty(value = "spring.datasource.url", havingValue = "none", matchIfMissing = true)
    @Import(SurveyorStorageInMemoryConfiguration.class)
    @Configuration(proxyBeanMethods = false)
    class StorageInMemory {

    }
}
