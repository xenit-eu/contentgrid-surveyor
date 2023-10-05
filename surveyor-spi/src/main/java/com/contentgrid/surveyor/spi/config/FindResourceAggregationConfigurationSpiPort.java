package com.contentgrid.surveyor.spi.config;

import com.contentgrid.surveyor.spi.ResourceDefinition;
import com.contentgrid.surveyor.spi.storage.AggregateEventCountMetricSpiPort.AggregationConfiguration;
import java.time.Duration;

public interface FindResourceAggregationConfigurationSpiPort {
    AggregationConfiguration getInsightsAggregationConfiguration(ResourceDefinition resourceDefinition, Duration aggregationSize);
    AggregationConfiguration getBillingAggregationConfiguration(ResourceDefinition resourceDefinition, Duration aggregationSize);
}
