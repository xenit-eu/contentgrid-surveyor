package com.contentgrid.surveyor.spi.storage;

import com.contentgrid.surveyor.spi.TimeInterval;
import java.math.BigDecimal;
import lombok.Value;

@Value
public
class EventCountMetric {

    TimeInterval measureInterval;
    Resource resource;
    BigDecimal value;
}
