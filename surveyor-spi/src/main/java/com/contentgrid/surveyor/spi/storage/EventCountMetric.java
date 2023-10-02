package com.contentgrid.surveyor.spi.storage;

import java.math.BigInteger;
import lombok.Value;

@Value
public
class EventCountMetric {
    TimeInterval measureInterval;
    Resource resource;
    BigInteger count;
}
