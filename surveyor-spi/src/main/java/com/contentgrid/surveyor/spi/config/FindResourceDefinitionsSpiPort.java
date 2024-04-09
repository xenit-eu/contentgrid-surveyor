package com.contentgrid.surveyor.spi.config;

import com.contentgrid.surveyor.spi.ResourceDefinition;
import com.contentgrid.surveyor.values.MetricName;
import com.contentgrid.surveyor.values.ResourceAndMetric;
import com.contentgrid.surveyor.values.ResourceType;
import com.contentgrid.surveyor.values.SourceName;
import java.util.List;
import java.util.Objects;

public interface FindResourceDefinitionsSpiPort {

    default List<ResourceDefinition> findResourceDefinitions(SourceName sourceSystem,
            ResourceType resourceType) {
        return findResourceDefinitions(resourceType).stream()
                .filter(definition -> Objects.equals(definition.sourceSystem(), sourceSystem))
                .toList();
    }

    List<ResourceDefinition> findResourceDefinitions(ResourceType resourceType);

    List<ResourceDefinition> findResourceDefinitions(List<ResourceAndMetric> resourceTypes);

    default List<ResourceDefinition> findResourceDefinitions(ResourceType resourceType, MetricName metricName) {
        return findResourceDefinitions(resourceType)
                .stream()
                .filter(def -> Objects.equals(def.metricName(), metricName))
                .toList();
    }
}
