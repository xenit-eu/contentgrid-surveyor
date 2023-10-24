package com.contentgrid.surveyor.spi.source;

import com.contentgrid.surveyor.spi.ResourceDefinition;
import com.contentgrid.surveyor.spi.TimeInterval;
import com.contentgrid.surveyor.values.ResourceId;
import java.math.BigDecimal;

public record CollectedMetric(
        ResourceDefinition resourceDefinition,
        ResourceId resourceId,

        TimeInterval timeInterval,
        BigDecimal value
) {

}
