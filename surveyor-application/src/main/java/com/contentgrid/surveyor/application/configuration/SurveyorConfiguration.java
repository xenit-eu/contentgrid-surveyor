package com.contentgrid.surveyor.application.configuration;

import com.contentgrid.surveyor.api.metrics.FindMetrics;
import com.contentgrid.surveyor.api.pull.PullMetrics;
import com.contentgrid.surveyor.application.configuration.properties.SurveyorProperties;
import com.contentgrid.surveyor.drivers.schedule.ScheduledPullMetricsComponent;
import com.contentgrid.surveyor.infrastructure.source.prometheus.PrometheusApiConfig;
import com.contentgrid.surveyor.infrastructure.source.prometheus.PrometheusEventMetricsSource;
import com.contentgrid.surveyor.spi.ResourceDefinition;
import com.contentgrid.surveyor.spi.config.FindResourceDefinitionsSpiPort;
import com.contentgrid.surveyor.spi.source.EventMetricsSource;
import com.contentgrid.surveyor.spi.source.MetricCollectionConfig;
import com.contentgrid.surveyor.spi.storage.AggregateEventCountMetricSpiPort;
import com.contentgrid.surveyor.spi.storage.AggregateEventCountMetricSpiPort.GroupingConfiguration;
import com.contentgrid.surveyor.spi.storage.LastEventCountMetricSpiPort;
import com.contentgrid.surveyor.spi.storage.StoreEventCountMetricSpiPort;
import com.contentgrid.surveyor.usecase.metrics.FindMetricsUseCase;
import com.contentgrid.surveyor.usecase.metrics.FindMetricsUseCase.GroupingKey;
import com.contentgrid.surveyor.usecase.pull.PullMetricsUseCase;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
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
        return surveyorProperties.metrics().stream()
                .map(metric -> MetricCollectionConfig.builder()
                        .type(metric.type())
                        .metric(metric.metric())
                        .resourceType(metric.resourceType())
                        .interval(metric.query().interval())
                        .query(metric.query().query())
                        .resourceIdLabel(metric.query().resourceIdLabel())
                        .build())
                .toList();
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
                    return new PrometheusEventMetricsSource(webclientBuilder, apiConfig, prometheusProperties.name(),
                            prometheusProperties.type());
                })
                .toList();
    }

    @Bean
    Map<GroupingKey, GroupingConfiguration> metricsGroupingConfiguration(SurveyorProperties surveyorProperties) {
        return surveyorProperties.metrics().stream()
                .map(metric -> Map.entry(new GroupingKey(metric.resourceType(), metric.metric()),
                        GroupingConfiguration.builder()
                                .groupInterval(metric.aggregation().period())
                                .operation(metric.aggregation().operation())
                                .build()))
                .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
    }

    @Bean
    FindResourceDefinitionsSpiPort findResourceDefinitionsSpiPort(List<MetricCollectionConfig> collectionConfigs,
            List<PrometheusEventMetricsSource> metricsSources) {
        return new FindResourceDefinitionsSpiPort() {
            @Override
            public List<ResourceDefinition> findResourceDefinitions(String system, String resourceType) {
                return metricsSources.stream()
                        .flatMap(metricsSource -> collectionConfigs.stream()
                                .filter(config -> Objects.equals(config.resourceType(), resourceType))
                                .flatMap(config -> metricsSource.resourceDefinition(config).stream())
                        )
                        .filter(definition -> Objects.equals(definition.sourceSystem(), system))
                        .toList();
            }
        };
    }

    @Bean
    PullMetrics pullMetrics(List<MetricCollectionConfig> collectionConfigs,
            List<? extends EventMetricsSource> metricsSources,
            StoreEventCountMetricSpiPort storeEventCountMetricSpiPort,
            LastEventCountMetricSpiPort lastEventCountMetricSpiPort) {
        return new PullMetricsUseCase(metricsSources, collectionConfigs, storeEventCountMetricSpiPort,
                lastEventCountMetricSpiPort);
    }

    @Bean
    FindMetrics findMetrics(Map<GroupingKey, GroupingConfiguration> configuration,
            FindResourceDefinitionsSpiPort findResourceDefinitionsSpiPort,
            AggregateEventCountMetricSpiPort aggregateEventCountMetricSpiPort) {
        return new FindMetricsUseCase(configuration, aggregateEventCountMetricSpiPort, findResourceDefinitionsSpiPort);
    }
}
