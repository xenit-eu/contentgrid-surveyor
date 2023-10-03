package com.contentgrid.surveyor.infrastructure.source.prometheus.transport;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public record PrometheusMatrixResult(
        Map<String, String> metric,
        List<PrometheusSample> values
) implements PrometheusResult {

    public Stream<PrometheusVectorResult> asVectors() {
        return values.stream()
                .map(sample -> new PrometheusVectorResult(metric, sample));
    }
}
