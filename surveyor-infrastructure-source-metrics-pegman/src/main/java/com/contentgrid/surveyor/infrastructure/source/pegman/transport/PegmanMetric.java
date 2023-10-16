package com.contentgrid.surveyor.infrastructure.source.pegman.transport;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import lombok.Value;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.core.Relation;

@Relation(itemRelation = "metric", collectionRelation = "metrics")
@Value
public class PegmanMetric extends RepresentationModel<PegmanMetric> {
    PegmanResource resource;
    List<MetricMeasuredValue> data;

    public record MetricMeasuredValue (
        Instant startTime,
        Instant endTime,
        BigDecimal value
    ) {
    }

    public record PegmanResource (
            String resource,
            String metric,
            String resourceId
    ) {

    }

}
