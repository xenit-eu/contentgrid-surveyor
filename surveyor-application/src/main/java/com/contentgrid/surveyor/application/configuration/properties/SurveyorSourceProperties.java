package com.contentgrid.surveyor.application.configuration.properties;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "surveyor.systems")
public record SurveyorSourceProperties(
        List<SurveyorPrometheusSourceProperties> prometheus
) {

}
