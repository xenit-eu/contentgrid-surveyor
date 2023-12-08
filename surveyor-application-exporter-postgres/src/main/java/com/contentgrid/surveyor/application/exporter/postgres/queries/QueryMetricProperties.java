package com.contentgrid.surveyor.application.exporter.postgres.queries;

import java.util.List;
import lombok.Data;

@Data
public class QueryMetricProperties {

    private String metricName;
    private List<String> keyLabels;
    private List<String> values;

    private String query;
}
