package com.contentgrid.surveyor.spi.storage;

import com.contentgrid.surveyor.spi.TimeInterval;
import com.contentgrid.surveyor.spi.storage.aggregation.AggregationConfiguration;
import java.math.BigDecimal;
import java.util.List;

public interface AggregateEventCountMetricSpiPort {

    default EventCountMetric getAggregatedEventCountMetric(Resource resource, TimeInterval interval,
            AggregationConfiguration aggregationConfiguration) {
        return Util.onlyValue(
                findEventCountMetrics(resource, interval, aggregationConfiguration),
                () -> new EventCountMetric(interval, resource, BigDecimal.ZERO)
        );
    }

    List<EventCountMetric> findEventCountMetrics(Resource resource, TimeInterval interval,
            AggregationConfiguration aggregationConfiguration);


}
