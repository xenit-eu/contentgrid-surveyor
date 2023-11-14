package com.contentgrid.surveyor.spi.resources;


import com.contentgrid.surveyor.values.ResourceId;
import com.contentgrid.surveyor.values.ResourceType;
import com.contentgrid.surveyor.values.SourceName;
import lombok.Value;

@Value
public class ResourceIdentity {
    SourceName sourceSystem;
    ResourceType resourceType;
    ResourceId resourceId;
}
