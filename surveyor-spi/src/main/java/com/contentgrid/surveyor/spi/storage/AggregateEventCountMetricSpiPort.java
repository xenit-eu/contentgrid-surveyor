package com.contentgrid.surveyor.spi.storage;

import com.contentgrid.surveyor.spi.ResourceDefinition;
import com.contentgrid.surveyor.spi.TimeInterval;
import com.contentgrid.surveyor.spi.storage.aggregation.AggregationConfiguration;
import org.reactivestreams.Publisher;

public interface AggregateEventCountMetricSpiPort {

    default Publisher<EventCountMetric> getAggregatedEventCountMetric(Resource resource, TimeInterval interval,
            AggregationConfiguration aggregationConfiguration) {
        return Util.onlyValue(findEventCountMetrics(resource, interval, aggregationConfiguration));
    }

    Publisher<EventCountMetric> findEventCountMetrics(Resource resource, TimeInterval interval,
            AggregationConfiguration aggregationConfiguration);

    Publisher<EventCountMetric> findEventCountMetrics(ResourceDefinition resourceDefinition, TimeInterval interval,
            AggregationConfiguration aggregationConfiguration);


}
