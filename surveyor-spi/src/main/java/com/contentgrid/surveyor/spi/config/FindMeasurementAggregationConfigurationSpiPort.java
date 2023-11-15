package com.contentgrid.surveyor.spi.config;

import com.contentgrid.surveyor.spi.ResourceDefinition;
import com.contentgrid.surveyor.spi.storage.aggregation.AggregationConfiguration;
import java.time.Duration;

public interface FindMeasurementAggregationConfigurationSpiPort {

    AggregationConfiguration getInsightsAggregationConfiguration(ResourceDefinition resourceDefinition,
            Duration aggregationSize);

    AggregationConfiguration getBillingAggregationConfiguration(ResourceDefinition resourceDefinition,
            Duration aggregationSize);
}
