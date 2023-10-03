package com.contentgrid.surveyor.spi.source;

import java.math.BigDecimal;
import lombok.Value;

@Value
public class CollectedMetric {

    String resourceType;
    String resourceId;
    String metricName;

    TimeInterval timeInterval;
    BigDecimal value;
}
