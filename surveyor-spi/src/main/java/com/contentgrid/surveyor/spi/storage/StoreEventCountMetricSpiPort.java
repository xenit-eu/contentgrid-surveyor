package com.contentgrid.surveyor.spi.storage;

import java.util.List;

public interface StoreEventCountMetricSpiPort {
    void storeEventMetric(EventCountMetric metric);

    default void storeEventMetrics(List<EventCountMetric> metrics) {
        metrics.forEach(this::storeEventMetric);
    }

}
