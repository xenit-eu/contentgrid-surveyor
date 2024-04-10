package com.contentgrid.surveyor.spi.resources;

import com.contentgrid.surveyor.spi.storage.Measurement;
import com.contentgrid.surveyor.values.MetricName;
import java.math.BigDecimal;
import java.util.Map;

public record LinkedMeasurements(Map<MetricName, Measurement> measurements, ResourceLinkage linkage) {
    public BigDecimal valueOrDefault(MetricName metric, BigDecimal _default) {
        var measurement = measurements.get(metric);
        if (measurement == null) {
            return _default;
        }
        return measurement.getValue();
    }
}
