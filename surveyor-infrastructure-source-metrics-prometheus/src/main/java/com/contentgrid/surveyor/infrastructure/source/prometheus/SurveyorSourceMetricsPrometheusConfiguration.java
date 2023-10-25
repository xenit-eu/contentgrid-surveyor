package com.contentgrid.surveyor.infrastructure.source.prometheus;

import com.contentgrid.surveyor.spi.MetricSourceSystemType;
import com.contentgrid.surveyor.values.SourceName;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.NonNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration(proxyBeanMethods = false)
public class SurveyorSourceMetricsPrometheusConfiguration {

    @Bean
    @ConfigurationProperties(prefix = "surveyor.systems.prometheus")
    Map<String, SurveyorPrometheusSourceProperties> surveyorPrometheusSourcePropertiesList() {
        return new LinkedHashMap<>();
    }

    @Bean
    List<PrometheusEventMetricsSource> prometheusEventMetricsSource(
            Map<String, SurveyorPrometheusSourceProperties> prometheusSourceProperties,
            WebClient.Builder webclientBuilder,
            ObjectMapper objectMapper
    ) {
        return prometheusSourceProperties.values().stream()
                .map(prometheusProperties -> {
                    var apiConfig = PrometheusApiConfig.builder()
                            .url(prometheusProperties.url())
                            .headers(Optional.ofNullable(prometheusProperties.headers()).orElse(Map.of()))
                            .username(prometheusProperties.username())
                            .password(prometheusProperties.password())
                            .bearer(prometheusProperties.bearer())
                            .build();
                    return new PrometheusEventMetricsSource(webclientBuilder, objectMapper, apiConfig,
                            prometheusProperties.name(),
                            prometheusProperties.type());
                })
                .toList();
    }


    record SurveyorPrometheusSourceProperties(
            @NonNull
            SourceName name,
            @NonNull
            MetricSourceSystemType type,
            @NonNull
            URI url,
            Map<String, String> headers,
            String username,
            String password,
            String bearer
    ) {

    }
}
