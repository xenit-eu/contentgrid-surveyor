package com.contentgrid.surveyor.drivers.billing;

import com.contentgrid.surveyor.spi.resources.LinkedMeasurements;
import com.contentgrid.surveyor.values.MetricName;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.math.BigDecimal;

@JsonPropertyOrder({"org", "project", "app", "requests", "objects", "bytes", "records"})
record BillingCsvRecordModel (
        String org,
        String project,
        String app,
        BigDecimal requests,
        BigDecimal objects,
        BigDecimal bytes,
        BigDecimal records
) {
    static BillingCsvRecordModel from(LinkedMeasurements linkedMeasurements) {
        return new BillingCsvRecordModel(
                linkedMeasurements.linkage().getOrgRef(),
                linkedMeasurements.linkage().getProjectRef(),
                linkedMeasurements.linkage().getApplicationRef(),

                linkedMeasurements.valueOrDefault(MetricName.of("request_count"), BigDecimal.ZERO),
                linkedMeasurements.valueOrDefault(MetricName.of("objects_count"), BigDecimal.ZERO),
                linkedMeasurements.valueOrDefault(MetricName.of("stored_bytes"), BigDecimal.ZERO),
                linkedMeasurements.valueOrDefault(MetricName.of("estimated_count"), BigDecimal.ZERO)
        );
    }

}
