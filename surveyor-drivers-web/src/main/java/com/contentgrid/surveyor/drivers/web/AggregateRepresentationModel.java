package com.contentgrid.surveyor.drivers.web;

import java.math.BigDecimal;
import java.time.Instant;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.core.Relation;

@Value
@Relation(itemRelation = "aggregate", collectionRelation = "aggregates")
@EqualsAndHashCode(callSuper = true)
public class AggregateRepresentationModel extends RepresentationModel<AggregateRepresentationModel> {

    String metric;
    Instant startTime;
    Instant endTime;
    BigDecimal value;
}
