package com.contentgrid.surveyor.spi.storage;

import com.contentgrid.surveyor.spi.TimeInterval;
import java.math.BigDecimal;
import lombok.NonNull;
import lombok.Value;

@Value
public class EventCountMetric {

    @NonNull
    TimeInterval measureInterval;
    @NonNull
    Resource resource;
    @NonNull
    BigDecimal value;
}
