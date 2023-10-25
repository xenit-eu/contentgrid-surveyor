package com.contentgrid.surveyor.api.metrics;

import java.math.BigDecimal;
import java.time.Instant;

public record Metric(
        Instant startTime,
        Instant endTime,
        BigDecimal value
) {

}
