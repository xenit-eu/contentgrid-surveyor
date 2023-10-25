package com.contentgrid.surveyor.infrastructure.source.prometheus.transport;

import java.util.Map;

public record PrometheusVectorResult(

        Map<String, String> metric,
        PrometheusSample value
) implements PrometheusResult {

}
