package com.contentgrid.surveyor.api.metrics;

import org.reactivestreams.Publisher;

public interface ExportedMetrics {

    Resource resource();

    Publisher<Metric> metrics();
}
