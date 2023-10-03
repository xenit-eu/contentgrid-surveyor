package com.contentgrid.surveyor.spi;

public record ResourceDefinition(
        String sourceSystem,
        String resourceType,
        String metricName
) {

}
