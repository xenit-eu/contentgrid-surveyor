package com.contentgrid.surveyor.application.configuration.properties;

import java.net.URI;
import java.util.Map;

public record SurveyorPrometheusSourceProperties(
        String name,
        String type,
        URI url,
        Map<String, String> headers,
        String username,
        String password,
        String bearer
) {

}
