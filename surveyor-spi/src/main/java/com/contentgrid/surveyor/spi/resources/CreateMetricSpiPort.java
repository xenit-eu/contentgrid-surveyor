package com.contentgrid.surveyor.spi.resources;

import reactor.core.publisher.Mono;

public interface CreateMetricSpiPort {
    Mono<Void> createMetric(Metric metric);
}
