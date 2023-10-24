package com.contentgrid.surveyor.infrastructure.config.spring.properties;

import com.contentgrid.surveyor.spi.storage.aggregation.AggregationOperation;
import com.contentgrid.surveyor.values.MetricName;
import com.contentgrid.surveyor.spi.MetricSourceSystemType;
import com.contentgrid.surveyor.values.ResourceType;
import java.time.Duration;
import java.util.List;
import lombok.Value;
import lombok.experimental.Accessors;

@Value
@Accessors(fluent = true)
public class SurveyorMetricProperties {

    MetricSourceSystemType type;
    ResourceType resourceType;
    String metric;
    SurveyorMetricQueryProperties query;
    List<SurveyorMetricAggregrationProperties> insights;
    List<SurveyorMetricAggregrationProperties> billing;

    public MetricName metric() {
        return MetricName.of(resourceType, metric);
    }

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
