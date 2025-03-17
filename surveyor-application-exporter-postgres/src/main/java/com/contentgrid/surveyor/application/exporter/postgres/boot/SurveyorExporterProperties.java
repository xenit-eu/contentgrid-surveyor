package com.contentgrid.surveyor.application.exporter.postgres.boot;

import com.contentgrid.surveyor.application.exporter.postgres.queries.QueryMetricProperties;
import io.fabric8.kubernetes.client.Config;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "surveyor.exporter", ignoreUnknownFields = false)
@Data
public class SurveyorExporterProperties {

    private Config kubernetes = Config.autoConfigure(null);

    private Discovery discovery = new Discovery();

    private List<QueryMetricProperties> metrics = new ArrayList<>();

    @Data
    public static class Discovery {

        private Map<String, String> matchLabels = new HashMap<>();

        private Duration resync = Duration.ofMinutes(1);
    }
}
