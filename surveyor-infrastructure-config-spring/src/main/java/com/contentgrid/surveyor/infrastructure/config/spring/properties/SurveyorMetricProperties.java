package com.contentgrid.surveyor.infrastructure.config.spring.properties;

import com.contentgrid.surveyor.spi.storage.aggregation.AggregationOperation;
import java.time.Duration;
import java.util.List;

public record SurveyorMetricProperties(
        String type,
        String resourceType,
        String metric,
        SurveyorMetricQueryProperties query,
        List<SurveyorMetricAggregrationProperties> insights,
        List<SurveyorMetricAggregrationProperties> billing
) {

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
