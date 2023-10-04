package com.contentgrid.surveyor.application.configuration;

import com.contentgrid.surveyor.api.pull.PullMetrics;
import com.contentgrid.surveyor.application.configuration.properties.SurveyorProperties;
import com.contentgrid.surveyor.drivers.schedule.ScheduledPullMetricsComponent;
import com.contentgrid.surveyor.infrastructure.source.prometheus.PrometheusApiConfig;
import com.contentgrid.surveyor.infrastructure.source.prometheus.PrometheusEventMetricsSource;
import com.contentgrid.surveyor.spi.source.EventMetricsSource;
import com.contentgrid.surveyor.spi.source.MetricCollectionConfig;
import com.contentgrid.surveyor.spi.storage.LastEventCountMetricSpiPort;
import com.contentgrid.surveyor.spi.storage.StoreEventCountMetricSpiPort;
import com.contentgrid.surveyor.usecase.pull.PullMetricsUseCase;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.reactive.function.client.WebClient;

@EnableScheduling
@EnableConfigurationProperties(SurveyorProperties.class)
@Configuration(proxyBeanMethods = false)
public class SurveyorConfiguration {
    @Bean
    ScheduledPullMetricsComponent scheduledPullMetricsComponent(PullMetrics pullMetrics) {
        return new ScheduledPullMetricsComponent(pullMetrics);
    }

    @Bean
    List<MetricCollectionConfig> metricCollectionConfigList(SurveyorProperties surveyorProperties) {
        return surveyorProperties.metrics();
    }

    @Bean
    List<PrometheusEventMetricsSource> prometheusEventMetricsSources(SurveyorProperties surveyorProperties, WebClient.Builder webclientBuilder) {
        return surveyorProperties.systems().prometheus()
                .stream()
                .map(prometheusProperties -> {
                    var apiConfig = PrometheusApiConfig.builder()
                            .url(prometheusProperties.url())
                            .headers(Optional.ofNullable(prometheusProperties.headers()).orElse(Map.of()))
                            .username(prometheusProperties.username())
                            .password(prometheusProperties.password())
                            .bearer(prometheusProperties.bearer())
                            .build();
                    return new PrometheusEventMetricsSource(webclientBuilder, apiConfig, prometheusProperties.name(), prometheusProperties.type());
                })
                .toList();
    }

    @Bean
    PullMetrics pullMetrics(List<MetricCollectionConfig> collectionConfigs, List<? extends EventMetricsSource> metricsSources, StoreEventCountMetricSpiPort storeEventCountMetricSpiPort, LastEventCountMetricSpiPort lastEventCountMetricSpiPort) {
        return new PullMetricsUseCase(metricsSources, collectionConfigs, storeEventCountMetricSpiPort, lastEventCountMetricSpiPort);
    }
}
