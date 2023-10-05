package com.contentgrid.surveyor.infrastructure.config.spring;

import com.contentgrid.surveyor.infrastructure.config.spring.properties.SurveyorMetricProperties;
import com.contentgrid.surveyor.infrastructure.config.spring.properties.SurveyorMetricProperties.SurveyorMetricAggregrationProperties;
import com.contentgrid.surveyor.spi.ResourceDefinition;
import com.contentgrid.surveyor.spi.config.FindResourceAggregationConfigurationSpiPort;
import com.contentgrid.surveyor.spi.storage.AggregateEventCountMetricSpiPort.AggregationConfiguration;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class SpringConfigurationGateway implements FindResourceAggregationConfigurationSpiPort {

    private final List<SurveyorMetricProperties> metricProperties;

    @Override
    public AggregationConfiguration getInsightsAggregationConfiguration(ResourceDefinition resourceDefinition,
            Duration aggregationSize) {
        var property = findApplicableProperty(resourceDefinition);
        return createAggregationConfiguration(property.insights(), aggregationSize);
    }

    @Override
    public AggregationConfiguration getBillingAggregationConfiguration(ResourceDefinition resourceDefinition,
            Duration aggregationSize) {
        var property = findApplicableProperty(resourceDefinition);
        return createAggregationConfiguration(property.billing(), aggregationSize);
    }

    private static AggregationConfiguration createAggregationConfiguration(
            List<SurveyorMetricAggregrationProperties> configs, Duration aggregationSize) {
        var builder = AggregationConfiguration.builder();

        for (SurveyorMetricAggregrationProperties config : configs) {
            if (config.period() == null) {
                builder.thenGroup(config.operation(), aggregationSize);
            } else if (config.period().compareTo(aggregationSize) <= 0) {
                builder.thenGroup(config.operation(), config.period());
            } else {
                break;
            }
        }
        return builder.build();
    }

    private SurveyorMetricProperties findApplicableProperty(ResourceDefinition resourceDefinition) {
        return metricProperties.stream()
                .filter(property -> Objects.equals(property.resourceType(), resourceDefinition.resourceType()))
                .filter(property -> Objects.equals(property.metric(), resourceDefinition.metricName()))
                .reduce((a, b) -> {
                    throw new RuntimeException("Duplicate definition for %s".formatted(resourceDefinition));
                })
                .orElseThrow(() -> new RuntimeException("No definition for %s".formatted(resourceDefinition)));
    }

}
