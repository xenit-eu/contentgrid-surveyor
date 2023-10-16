package com.contentgrid.surveyor.infrastructure.source.prometheus;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration(proxyBeanMethods = false)
public class SurveyorSourceMetricsPrometheusConfiguration {

    @Bean
    @ConfigurationProperties(prefix = "surveyor.systems.prometheus")
    List<SurveyorPrometheusSourceProperties> surveyorPrometheusSourcePropertiesList(){
        return new ArrayList<>();
    }

    @Bean
    List<PrometheusEventMetricsSource> prometheusEventMetricsSource(List<SurveyorPrometheusSourceProperties> prometheusSourceProperties, WebClient.Builder webclientBuilder) {
        return prometheusSourceProperties.stream()
                .map(prometheusProperties -> {
                    var apiConfig = PrometheusApiConfig.builder()
                            .url(prometheusProperties.url())
                            .headers(Optional.ofNullable(prometheusProperties.headers()).orElse(Map.of()))
                            .username(prometheusProperties.username())
                            .password(prometheusProperties.password())
                            .bearer(prometheusProperties.bearer())
                            .build();
                    return new PrometheusEventMetricsSource(webclientBuilder, apiConfig, prometheusProperties.name(),
                            prometheusProperties.type());
                })
                .toList();
    }


    record SurveyorPrometheusSourceProperties(
            String name,
            String type,
            URI url,
            Map<String, String> headers,
            String username,
            String password,
            String bearer
    ) {

    }
}
