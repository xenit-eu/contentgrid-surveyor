package com.contentgrid.surveyor.spi.source;

import java.time.Duration;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class MetricCollectionConfig {

    String metric;
    String resourceType;

    String query;
    Duration interval;
    String resourceIdLabel;
}
