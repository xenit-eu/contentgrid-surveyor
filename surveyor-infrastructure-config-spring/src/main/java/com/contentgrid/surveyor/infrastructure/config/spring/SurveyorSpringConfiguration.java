package com.contentgrid.surveyor.infrastructure.config.spring;

import com.contentgrid.surveyor.infrastructure.config.spring.properties.SurveyorProperties;
import com.contentgrid.surveyor.spi.ResourceDefinition;
import com.contentgrid.surveyor.spi.config.FindResourceDefinitionsSpiPort;
import com.contentgrid.surveyor.spi.source.EventMetricsSource;
import com.contentgrid.surveyor.spi.source.MetricCollectionConfig;
import java.util.List;
import java.util.Objects;
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
    List<MetricCollectionConfig> metricCollectionConfigList(SurveyorProperties surveyorProperties) {
        return surveyorProperties.metrics().stream()
                .map(metric -> MetricCollectionConfig.builder()
                        .type(metric.type())
                        .metric(metric.metric())
                        .resourceType(metric.resourceType())
                        .interval(metric.query().interval())
                        .query(metric.query().query())
                        .resourceIdLabel(metric.query().resourceIdLabel())
                        .build())
                .toList();
    }

    @Bean
    FindResourceDefinitionsSpiPort findResourceDefinitionsSpiPort(List<MetricCollectionConfig> collectionConfigs,
            List<? extends EventMetricsSource> metricsSources) {
        return new FindResourceDefinitionsSpiPort() {
            @Override
            public List<ResourceDefinition> findResourceDefinitions(String resourceType) {
                return metricsSources.stream()
                        .flatMap(metricsSource -> collectionConfigs.stream()
                                .filter(config -> Objects.equals(config.resourceType(), resourceType))
                                .flatMap(config -> metricsSource.resourceDefinition(config).stream())
                        )
                        .toList();
            }
        };
    }

}
