package com.contentgrid.surveyor.infrastructure.config.spring.properties;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "surveyor")
public record SurveyorProperties(
        List<SurveyorMetricProperties> metrics
) {

}
