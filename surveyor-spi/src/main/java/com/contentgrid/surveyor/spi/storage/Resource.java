package com.contentgrid.surveyor.spi.storage;

import com.contentgrid.surveyor.spi.ResourceDefinition;
import com.contentgrid.surveyor.values.ResourceId;
import lombok.Value;

@Value
public class Resource {

    ResourceDefinition definition;
    ResourceId resourceId;
}
