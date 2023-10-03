package com.contentgrid.surveyor.spi.storage;

import com.contentgrid.surveyor.spi.ResourceDefinition;
import lombok.Value;

@Value
public class Resource {

    ResourceDefinition definition;
    String resourceId;
}
