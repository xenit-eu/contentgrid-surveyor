package com.contentgrid.surveyor.infrastructure.collector.pegman;

import com.contentgrid.surveyor.spi.MetricCollectorSystemType;
import com.contentgrid.surveyor.values.SourceName;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.NonNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.hateoas.config.HypermediaWebClientConfigurer;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration(proxyBeanMethods = false)
public class SurveyorMeasurementCollectorPegmanConfiguration {

    @Bean
    @ConfigurationProperties(prefix = "surveyor.systems.pegman")
    Map<String, SurveyorPegmanSourceProperties> surveyorPegmanSourcePropertiesList() {
        return new LinkedHashMap<>();
    }

    @Bean
    List<PegmanMeasurementCollector> pegmanMeasurementCollectors(
            ReactiveOAuth2AuthorizedClientManager oAuth2AuthorizedClientManager,
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
                            .authorizedClientManager(oAuth2AuthorizedClientManager)
                            .build();
                    return new PegmanMeasurementCollector(webclientBuilder, apiConfig, webClientConfigurer,
                            objectMapper, pegmanProperties.name(),
                            pegmanProperties.type());
                })
                .toList();
    }


    record SurveyorPegmanSourceProperties(
            @NonNull
            SourceName name,
            @NonNull
            MetricCollectorSystemType type,
            @NonNull
            URI url,
            Map<String, String> headers,
            String username,
            String password,
            String bearer
    ) {

    }
}
