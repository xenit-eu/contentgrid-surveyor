package com.contentgrid.surveyor.infrastructure.source.pegman;

import com.contentgrid.surveyor.spi.MetricSourceSystemType;
import com.contentgrid.surveyor.values.SourceName;
import com.fasterxml.jackson.databind.ObjectMapper;
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
            List<SurveyorPegmanSourceProperties> pegmanSourceProperties,
            WebClient.Builder webclientBuilder,
            HypermediaWebClientConfigurer webClientConfigurer,
            ObjectMapper objectMapper
    ) {
        return pegmanSourceProperties.stream()
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
            SourceName name,
            MetricSourceSystemType type,
            URI url,
            Map<String, String> headers,
            String username,
            String password,
            String bearer
    ) {

    }
}
