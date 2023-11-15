package com.contentgrid.surveyor.infrastructure.collector.pegman.transport;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import lombok.Value;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.core.Relation;

@Relation(itemRelation = "metric", collectionRelation = "metrics")
@Value
@JsonIgnoreProperties(ignoreUnknown = true)
public class PegmanMetric extends RepresentationModel<PegmanMetric> {
    PegmanResource resource;
    List<MetricMeasuredValue> data;

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record MetricMeasuredValue (
            Instant startTime,
            Instant endTime,
            BigDecimal value
    ) {

    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PegmanResource (
            String resource,
            String metric,
            String resourceId
    ) {

    }

}
