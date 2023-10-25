package com.contentgrid.surveyor.api.metrics;

import com.contentgrid.surveyor.values.MetricName;
import com.contentgrid.surveyor.values.ResourceId;
import com.contentgrid.surveyor.values.SourceName;

public record Resource(
        SourceName system,
        MetricName metric,
        ResourceId resourceId
) {

}
