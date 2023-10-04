package com.contentgrid.surveyor.spi.config;

import com.contentgrid.surveyor.spi.ResourceDefinition;
import java.util.List;

public interface FindResourceDefinitionsSpiPort {

    List<ResourceDefinition> findResourceDefinitions(String system, String resourceType);
}
