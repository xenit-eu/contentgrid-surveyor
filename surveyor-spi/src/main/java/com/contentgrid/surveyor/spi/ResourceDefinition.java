package com.contentgrid.surveyor.spi;

import com.contentgrid.surveyor.spi.resources.Metric;
import com.contentgrid.surveyor.spi.resources.ResourceIdentity;
import com.contentgrid.surveyor.values.MetricName;
import com.contentgrid.surveyor.values.ResourceId;
import com.contentgrid.surveyor.values.ResourceType;
import com.contentgrid.surveyor.values.SourceName;
import java.util.Map;

public record ResourceDefinition(
        SourceName sourceSystem,
        ResourceType resourceType,
        MetricName metricName
) {

    public Metric createMetric(ResourceId resourceId, Map<String, String> tags) {
        return new Metric(
                new ResourceIdentity(
                        sourceSystem,
                        resourceType,
                        resourceId
                ),
                metricName,
                tags
        );
    }

}
