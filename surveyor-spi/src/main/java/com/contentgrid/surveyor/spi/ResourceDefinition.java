package com.contentgrid.surveyor.spi;

import com.contentgrid.surveyor.values.MetricName;
import com.contentgrid.surveyor.values.ResourceType;
import com.contentgrid.surveyor.values.SourceName;

public record ResourceDefinition(
        SourceName sourceSystem,
        MetricName metricName
) {

    public ResourceType resourceType() {
        return metricName.type();
    }

}
