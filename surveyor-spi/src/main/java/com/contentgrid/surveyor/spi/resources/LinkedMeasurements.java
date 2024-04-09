package com.contentgrid.surveyor.spi.resources;

import com.contentgrid.surveyor.spi.storage.Measurement;
import com.contentgrid.surveyor.values.MetricName;
import java.util.Map;

public record LinkedMeasurements(Map<MetricName, Measurement> measurements, ResourceLinkage linkage) {}
