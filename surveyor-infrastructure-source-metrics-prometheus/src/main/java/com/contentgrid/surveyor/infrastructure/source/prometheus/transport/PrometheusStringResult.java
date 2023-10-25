package com.contentgrid.surveyor.infrastructure.source.prometheus.transport;

import java.time.Instant;

public record PrometheusStringResult(
        Instant timestamp,
        String value
) implements PrometheusResult {

}
