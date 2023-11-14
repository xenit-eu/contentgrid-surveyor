package com.contentgrid.surveyor.spi.storage;

import java.util.List;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface StoreMeasurementSpiPort {

    Mono<Void> storeMeasurement(Measurement measurement);

    default Mono<Void> storeMeasurements(List<Measurement> measurements) {
        return Flux.fromIterable(measurements)
                .map(this::storeMeasurement)
                .flatMap(Mono::flux)
                .ignoreElements();
    }

}
