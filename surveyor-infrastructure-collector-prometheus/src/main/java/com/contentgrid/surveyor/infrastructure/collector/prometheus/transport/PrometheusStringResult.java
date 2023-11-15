package com.contentgrid.surveyor.infrastructure.collector.prometheus.transport;

import java.time.Instant;

public record PrometheusStringResult(
        Instant timestamp,
        String value
) implements PrometheusResult {

}
