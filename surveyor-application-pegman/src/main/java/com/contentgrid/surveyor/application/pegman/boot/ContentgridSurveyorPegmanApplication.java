package com.contentgrid.surveyor.application.pegman.boot;

import com.contentgrid.surveyor.drivers.web.SurveyorWebConfiguration;
import com.contentgrid.surveyor.infrastructure.config.spring.SurveyorSpringConfiguration;
import com.contentgrid.surveyor.infrastructure.collector.prometheus.SurveyorMeasurementCollectorPrometheusConfiguration;
import com.contentgrid.surveyor.infrastructure.storage.pullthrough.SurveyorStoragePullthroughConfiguration;
import com.contentgrid.surveyor.spi.config.FindMeasurementAggregationConfigurationSpiPort;
import com.contentgrid.surveyor.spi.config.FindResourceDefinitionsSpiPort;
import com.contentgrid.surveyor.spi.storage.AggregateMeasurementsSpiPort;
import com.contentgrid.surveyor.usecase.metrics.FindMetricsUseCase;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

import static org.springframework.security.oauth2.core.authorization.OAuth2ReactiveAuthorizationManagers.hasScope;

@SpringBootApplication
@Import({
        SurveyorWebConfiguration.class,
        SurveyorSpringConfiguration.class,
        SurveyorStoragePullthroughConfiguration.class,
        SurveyorMeasurementCollectorPrometheusConfiguration.class
})
public class ContentgridSurveyorPegmanApplication {

    public static void main(String[] args) {
        SpringApplication.run(ContentgridSurveyorPegmanApplication.class, args);
    }

    @Bean
    FindMetricsUseCase findMetrics(
            FindMeasurementAggregationConfigurationSpiPort resourceAggregationConfigurationSpiPort,
            FindResourceDefinitionsSpiPort findResourceDefinitionsSpiPort,
            AggregateMeasurementsSpiPort aggregateMeasurementsSpiPort) {
        return new FindMetricsUseCase(resourceAggregationConfigurationSpiPort, aggregateMeasurementsSpiPort,
                findResourceDefinitionsSpiPort);
    }


    @Bean
    SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        http
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers(HttpMethod.GET).access(hasScope("surveyor:pegman:read"))
                        .anyExchange().denyAll()
                )
                .oauth2ResourceServer(oauth2ResourceServer -> {
                    oauth2ResourceServer.jwt(Customizer.withDefaults());
                });
        return http.build();
    }
}
