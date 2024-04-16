package com.contentgrid.surveyor.drivers.billing;

import com.contentgrid.surveyor.spi.resources.LinkedMeasurements;
import com.contentgrid.surveyor.values.MetricName;
import java.math.BigDecimal;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.core.Relation;

@Value
@Relation(itemRelation = "application_metric", collectionRelation = "application_metrics")
@EqualsAndHashCode(callSuper = true)
class BillingRecordRepresentationModel extends RepresentationModel<BillingRecordRepresentationModel> {

    String org;
    String project;
    String app;
    BigDecimal requests;
    BigDecimal objects;
    BigDecimal bytes;
    BigDecimal records;

    static BillingRecordRepresentationModel from(LinkedMeasurements linkedMeasurements) {
        return new BillingRecordRepresentationModel(
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
