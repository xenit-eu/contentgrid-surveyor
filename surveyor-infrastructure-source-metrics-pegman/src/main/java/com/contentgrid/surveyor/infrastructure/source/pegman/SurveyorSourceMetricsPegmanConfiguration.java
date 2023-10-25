package com.contentgrid.surveyor.infrastructure.source.pegman;

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
import org.springframework.hateoas.config.HypermediaWebClientConfigurer;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration(proxyBeanMethods = false)
public class SurveyorSourceMetricsPegmanConfiguration {

    @Bean
    @ConfigurationProperties(prefix = "surveyor.systems.pegman")
    Map<String, SurveyorPegmanSourceProperties> surveyorPegmanSourcePropertiesList() {
        return new LinkedHashMap<>();
    }

    @Bean
    List<PegmanEventMetricsSource> pegmanEventMetricsSource(
            Map<String, SurveyorPegmanSourceProperties> pegmanSourceProperties,
            WebClient.Builder webclientBuilder,
            HypermediaWebClientConfigurer webClientConfigurer,
            ObjectMapper objectMapper
    ) {
        return pegmanSourceProperties.values()
                .stream()
                .map(pegmanProperties -> {
                    var apiConfig = PegmanApiConfig.builder()
                            .url(pegmanProperties.url())
                            .headers(Optional.ofNullable(pegmanProperties.headers()).orElse(Map.of()))
                            .username(pegmanProperties.username())
                            .password(pegmanProperties.password())
                            .bearer(pegmanProperties.bearer())
                            .build();
                    return new PegmanEventMetricsSource(webclientBuilder, apiConfig, webClientConfigurer,
                            objectMapper, pegmanProperties.name(),
                            pegmanProperties.type());
                })
                .toList();
    }


    record SurveyorPegmanSourceProperties(
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
