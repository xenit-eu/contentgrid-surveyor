package com.contentgrid.surveyor.api.metrics;

public record Resource(
        String system,
        String resource,
        String resourceId,
        String metric
) {

}
