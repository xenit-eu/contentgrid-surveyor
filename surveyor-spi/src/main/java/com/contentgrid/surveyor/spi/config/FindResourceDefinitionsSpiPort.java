package com.contentgrid.surveyor.spi.config;

import com.contentgrid.surveyor.spi.ResourceDefinition;
import java.util.List;
import java.util.Objects;

public interface FindResourceDefinitionsSpiPort {

    default List<ResourceDefinition> findResourceDefinitions(String system, String resourceType) {
        return findResourceDefinitions(resourceType).stream()
                .filter(definition -> Objects.equals(definition.sourceSystem(), system))
                .toList();
    }

    List<ResourceDefinition> findResourceDefinitions(String resourceType);
}
