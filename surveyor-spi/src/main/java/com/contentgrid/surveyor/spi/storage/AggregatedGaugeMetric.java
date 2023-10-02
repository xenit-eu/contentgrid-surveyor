package com.contentgrid.surveyor.spi.storage;

import java.math.BigDecimal;
import java.math.BigInteger;
import lombok.Value;

@Value
public class AggregatedGaugeMetric {
    TimeInterval interval;
    Resource resource;
    BigDecimal aggregate;
}
