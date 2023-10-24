package com.contentgrid.surveyor.infrastructure.config.spring;

import com.contentgrid.surveyor.infrastructure.config.spring.properties.SurveyorMetricProperties;
import com.contentgrid.surveyor.infrastructure.config.spring.properties.SurveyorProperties;
import com.contentgrid.surveyor.spi.ResourceDefinition;
import com.contentgrid.surveyor.spi.config.FindResourceDefinitionsSpiPort;
import com.contentgrid.surveyor.spi.source.EventMetricsSource;
import com.contentgrid.surveyor.spi.source.MetricCollectionConfig;
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
    FindResourceDefinitionsSpiPort findResourceDefinitionsSpiPort(SurveyorProperties surveyorProperties,
            List<? extends EventMetricsSource> metricsSources) {
        return new ResourceDefinitionsGateway(surveyorProperties.metrics(), metricsSources);
    }

    @RequiredArgsConstructor
    private static class ResourceDefinitionsGateway implements FindResourceDefinitionsSpiPort {

        private final List<SurveyorMetricProperties> metricProperties;
        private final List<? extends EventMetricsSource> metricsSources;

        @Override
        public List<ResourceDefinition> findResourceDefinitions(MetricSourceSystemType systemType) {
            return metricsSources.stream()
                    .flatMap(metricSource -> metricProperties.stream()
                            .filter(config -> Objects.equals(config.type(), systemType))
                            .flatMap(config -> metricSource.resourceDefinition(createConfig(config)).stream())
                    )
                    .toList();
        }

        @Override
        public List<ResourceDefinition> findResourceDefinitions(ResourceType resourceType) {
            return metricsSources.stream()
                    .flatMap(metricsSource -> metricProperties.stream()
                            .filter(config -> Objects.equals(config.resourceType(), resourceType))
                            .flatMap(config -> metricsSource.resourceDefinition(createConfig(config)).stream())
                    )
                    .toList();
        }

        private MetricCollectionConfig createConfig(SurveyorMetricProperties properties) {
            return MetricCollectionConfig.builder()
                    .type(properties.type())
                    .metric(properties.metric())
                    .interval(properties.query().interval())
                    .query(properties.query().query())
                    .resourceIdLabel(properties.query().resourceIdLabel())
                    .build();
        }
    }
}
