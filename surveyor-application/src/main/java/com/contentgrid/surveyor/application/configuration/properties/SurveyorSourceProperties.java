package com.contentgrid.surveyor.application.configuration.properties;

import java.util.List;

public record SurveyorSourceProperties (
    List<SurveyorPrometheusSourceProperties> prometheus
) {

}
