package com.contentgrid.surveyor.infrastructure.source.prometheus.transport;

import com.fasterxml.jackson.annotation.JsonUnwrapped;

public record PrometheusScalarResult(
        @JsonUnwrapped
        PrometheusSample sample
) implements PrometheusResult {

}
