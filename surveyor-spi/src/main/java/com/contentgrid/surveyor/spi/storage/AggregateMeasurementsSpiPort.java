package com.contentgrid.surveyor.spi.storage;

import com.contentgrid.surveyor.spi.ResourceDefinition;
import com.contentgrid.surveyor.spi.TimeInterval;
import com.contentgrid.surveyor.spi.resources.Metric;
import com.contentgrid.surveyor.spi.storage.aggregation.AggregationConfiguration;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface AggregateMeasurementsSpiPort {

    default Mono<Measurement> getAggregatedMeasurements(Metric resource, TimeInterval interval,
            AggregationConfiguration aggregationConfiguration) {
        return Util.onlyValue(findMeasurements(resource, interval, aggregationConfiguration));
    }

    Flux<Measurement> findMeasurements(Metric resource, TimeInterval interval,
            AggregationConfiguration aggregationConfiguration);

    Flux<Measurement> findMeasurements(ResourceDefinition resourceDefinition, TimeInterval interval,
            AggregationConfiguration aggregationConfiguration);


}
