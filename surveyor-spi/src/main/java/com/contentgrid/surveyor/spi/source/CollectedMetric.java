package com.contentgrid.surveyor.spi.source;

import com.contentgrid.surveyor.spi.ResourceDefinition;
import com.contentgrid.surveyor.spi.TimeInterval;
import java.math.BigDecimal;

public record CollectedMetric(
        ResourceDefinition resourceDefinition,
        String resourceId,

        TimeInterval timeInterval,
        BigDecimal value
) {

}
