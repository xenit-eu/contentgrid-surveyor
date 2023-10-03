package com.contentgrid.surveyor.spi.storage;

import java.util.List;

public interface StoreGaugeMetricSpiPort {

    void storeGaugeMetric(GaugeMetric gaugeMetric);

    default void storeGaugeMetrics(List<GaugeMetric> metrics) {
        metrics.forEach(this::storeGaugeMetric);
    }
}
