package com.contentgrid.surveyor.spi.source;

import com.contentgrid.surveyor.spi.ResourceDefinition;
import com.contentgrid.surveyor.spi.TimeInterval;
import com.contentgrid.surveyor.values.ResourceId;
import java.math.BigDecimal;
import java.util.Map;

public record CollectedMetric(
        ResourceDefinition resourceDefinition,
        ResourceId resourceId,
        Map<String, String> tags,

        TimeInterval timeInterval,
        BigDecimal value
) {

}
