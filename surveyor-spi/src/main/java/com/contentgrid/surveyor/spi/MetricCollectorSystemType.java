package com.contentgrid.surveyor.spi;

import lombok.Value;
import lombok.experimental.Accessors;

@Value(staticConstructor = "of")
@Accessors(fluent = true)
public class MetricCollectorSystemType {

    String sourceType;
}
