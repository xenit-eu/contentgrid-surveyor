package com.contentgrid.surveyor.infrastructure.source.pegman;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.hateoas.config.HypermediaWebClientConfigurer;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration(proxyBeanMethods = false)
public class SurveyorSourceMetricsPegmanConfiguration {

    @Bean
    @ConfigurationProperties(prefix = "surveyor.systems.pegman")
    List<SurveyorPegmanSourceProperties> surveyorPegmanSourcePropertiesList(){
        return new ArrayList<>();
    }

    @Bean
    List<PegmanEventMetricsSource> pegmanEventMetricsSource(
            List<SurveyorPegmanSourceProperties> prometheusSourceProperties,
            WebClient.Builder webclientBuilder,
            HypermediaWebClientConfigurer webClientConfigurer
    ) {
        return prometheusSourceProperties.stream()
                .map(prometheusProperties -> {
                    var apiConfig = PegmanApiConfig.builder()
                            .url(prometheusProperties.url())
                            .headers(Optional.ofNullable(prometheusProperties.headers()).orElse(Map.of()))
                            .username(prometheusProperties.username())
                            .password(prometheusProperties.password())
                            .bearer(prometheusProperties.bearer())
                            .build();
                    return new PegmanEventMetricsSource(webclientBuilder, apiConfig, webClientConfigurer, prometheusProperties.name(),
                            prometheusProperties.type());
                })
                .toList();
    }


    record SurveyorPegmanSourceProperties(
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
