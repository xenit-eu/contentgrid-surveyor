package com.contentgrid.surveyor.infrastructure.config.spring;

import com.contentgrid.surveyor.infrastructure.config.spring.properties.SurveyorMetricProperties;
import com.contentgrid.surveyor.infrastructure.config.spring.properties.SurveyorProperties;
import com.contentgrid.surveyor.spi.ResourceDefinition;
import com.contentgrid.surveyor.spi.config.FindCollectionConfigurationsSpiPort;
import com.contentgrid.surveyor.spi.config.FindResourceDefinitionsSpiPort;
import com.contentgrid.surveyor.spi.source.EventMetricsSource;
import com.contentgrid.surveyor.spi.config.MeasurementCollectionConfig;
import com.contentgrid.surveyor.spi.MetricSourceSystemType;
import com.contentgrid.surveyor.values.ResourceType;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(SurveyorProperties.class)
public class SurveyorSpringConfiguration {

    @Bean
    SpringConfigurationGateway springConfigurationGateway(SurveyorProperties surveyorProperties) {
        return new SpringConfigurationGateway(surveyorProperties.metrics());
    }

    @Bean
    FindResourceDefinitionsSpiPort findResourceDefinitionsSpiPort(
            FindCollectionConfigurationsSpiPort findCollectionConfigurationsSpiPort,
            List<? extends EventMetricsSource> metricsSources) {
        return new ResourceDefinitionsGateway(findCollectionConfigurationsSpiPort, metricsSources);
    }

    @Bean
    FindCollectionConfigurationsSpiPort findCollectionConfigurationsSpiPort(SurveyorProperties surveyorProperties) {
        return new CollectionConfigurationsGateway(surveyorProperties.metrics());
    }

    @RequiredArgsConstructor
    private static class ResourceDefinitionsGateway implements FindResourceDefinitionsSpiPort {

        private final FindCollectionConfigurationsSpiPort findCollectionConfigurationsSpiPort;
        private final List<? extends EventMetricsSource> metricsSources;

        @Override
        public List<ResourceDefinition> findResourceDefinitions(ResourceType resourceType) {
            return metricsSources.stream()
                    .flatMap(metricsSource -> findCollectionConfigurationsSpiPort.findConfigurationsFor(
                                    metricsSource.getSystemType()).stream()
                            .filter(config -> Objects.equals(config.resourceType(), resourceType))
                            .flatMap(config -> metricsSource.resourceDefinition(config).stream())
                    )
                    .toList();
        }
    }

    @RequiredArgsConstructor
    private static class CollectionConfigurationsGateway implements FindCollectionConfigurationsSpiPort {

        private final List<SurveyorMetricProperties> properties;

        @Override
        public List<MeasurementCollectionConfig> findConfigurationsFor(MetricSourceSystemType sourceSystemType) {
            return properties.stream()
                    .filter(props -> Objects.equals(props.type(), sourceSystemType))
                    .map(this::createConfig)
                    .toList();
        }

        private MeasurementCollectionConfig createConfig(SurveyorMetricProperties properties) {
            return MeasurementCollectionConfig.builder()
                    .type(properties.type())
                    .resourceType(properties.resourceType())
                    .metric(properties.metric())
                    .interval(properties.query().interval())
                    .query(properties.query().query())
                    .resourceIdLabel(properties.query().resourceIdLabel())
                    .build();
        }
    }
}
