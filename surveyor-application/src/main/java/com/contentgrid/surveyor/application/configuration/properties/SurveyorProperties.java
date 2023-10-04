package com.contentgrid.surveyor.application.configuration.properties;

import com.contentgrid.surveyor.spi.source.MetricCollectionConfig;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "surveyor")
public record SurveyorProperties(
    SurveyorSourceProperties systems,
    List<MetricCollectionConfig> metrics
) {
}
