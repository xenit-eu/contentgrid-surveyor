package com.contentgrid.surveyor.infrastructure.collector.prometheus.transport;

import com.fasterxml.jackson.annotation.JsonUnwrapped;

public record PrometheusScalarResult(
        @JsonUnwrapped
        PrometheusSample sample
) implements PrometheusResult {

}
