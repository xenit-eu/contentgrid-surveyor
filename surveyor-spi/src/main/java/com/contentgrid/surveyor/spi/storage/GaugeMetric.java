package com.contentgrid.surveyor.spi.storage;

import java.math.BigInteger;
import java.time.Instant;
import lombok.Value;

@Value
public
class GaugeMetric {

    Instant measureTime;
    Resource resource;
    BigInteger value;
}
