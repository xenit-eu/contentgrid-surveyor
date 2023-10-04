package com.contentgrid.surveyor.application.configuration.properties;

import com.contentgrid.surveyor.spi.storage.AggregateEventCountMetricSpiPort.GroupingConfiguration.GroupOperation;
import java.time.Duration;

public record SurveyorMetricProperties(
        String type,
        String resourceType,
        String metric,
        SurveyorMetricQueryProperties query,
        SurveyorMetricAggregrationProperties aggregation
) {

    public record SurveyorMetricQueryProperties(
            String resourceIdLabel,
            String query,
            Duration interval
    ) {

    }

    public record SurveyorMetricAggregrationProperties(
            Duration period,
            GroupOperation operation
    ) {

    }


}
