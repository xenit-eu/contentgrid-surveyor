package com.contentgrid.surveyor.drivers.web;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.core.Relation;

@Value
@Relation(itemRelation = "metric", collectionRelation = "metrics")
@EqualsAndHashCode(callSuper = true)
public class MetricRepresentationModel extends RepresentationModel<MetricRepresentationModel> {

    String metric;
    List<MetricData> data;

    public record MetricData(
            Instant startTime,
            Instant endTime,
            BigDecimal value
    ) {

    }
}
