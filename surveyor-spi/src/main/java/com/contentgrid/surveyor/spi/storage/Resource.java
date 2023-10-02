package com.contentgrid.surveyor.spi.storage;

import lombok.Value;

@Value
public class Resource {
    String resourceType;
    String resourceId;
    String metricName;
}
