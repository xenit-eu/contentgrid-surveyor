package com.contentgrid.surveyor.application.configuration.properties;

import java.time.Duration;

public record SurveyorMetricProperties(
        String type,
        String resourceType,
        String metric,
        String resourceIdLabel,
        String query,
        Duration queryInterval
) {


}
