package com.contentgrid.surveyor.spi.resources;

import com.contentgrid.surveyor.spi.ResourceDefinition;
import com.contentgrid.surveyor.values.MetricName;
import java.util.Map;
import lombok.Value;

@Value
public class Metric {
    ResourceIdentity resourceIdentity;
    MetricName metricName;
    Map<String, String> tags;

    public ResourceDefinition getResourceDefinition() {
        return new ResourceDefinition(
                resourceIdentity.getSourceSystem(),
                resourceIdentity.getResourceType(),
                getMetricName()
        );
    }
}
