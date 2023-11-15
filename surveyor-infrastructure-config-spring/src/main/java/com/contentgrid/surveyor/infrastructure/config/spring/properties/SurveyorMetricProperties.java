package com.contentgrid.surveyor.infrastructure.config.spring.properties;

import com.contentgrid.surveyor.spi.storage.aggregation.AggregationOperation;
import com.contentgrid.surveyor.values.MetricName;
import com.contentgrid.surveyor.spi.MetricCollectorSystemType;
import com.contentgrid.surveyor.values.ResourceType;
import java.time.Duration;
import java.util.List;
import lombok.Value;
import lombok.experimental.Accessors;

@Value
@Accessors(fluent = true)
public class SurveyorMetricProperties {

    MetricCollectorSystemType type;
    ResourceType resourceType;
    MetricName metric;
    SurveyorMetricQueryProperties query;
    List<SurveyorMetricAggregrationProperties> insights;
    List<SurveyorMetricAggregrationProperties> billing;

    public record SurveyorMetricQueryProperties(
            String resourceIdLabel,
            String query,
            Duration interval
    ) {

    }

    public record SurveyorMetricAggregrationProperties(
            Duration period,
            AggregationOperation operation
    ) {

    }

}
