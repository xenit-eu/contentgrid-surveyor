package com.contentgrid.surveyor.spi.storage;

import com.contentgrid.surveyor.spi.ResourceDefinition;
import com.contentgrid.surveyor.spi.TimeInterval;
import reactor.core.publisher.Mono;

public interface LastMeasurementSpiPort {

    Mono<TimeInterval> getLastMeasurementInterval(ResourceDefinition resourceDefinition);
}
