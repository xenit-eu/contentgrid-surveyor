package com.contentgrid.surveyor.application.pegman.boot;

import static org.springframework.security.oauth2.core.authorization.OAuth2ReactiveAuthorizationManagers.hasScope;

import com.contentgrid.surveyor.drivers.web.SurveyorWebConfiguration;
import com.contentgrid.surveyor.infrastructure.collector.prometheus.SurveyorMeasurementCollectorPrometheusConfiguration;
import com.contentgrid.surveyor.infrastructure.config.spring.SurveyorSpringConfiguration;
import com.contentgrid.surveyor.infrastructure.storage.pullthrough.SurveyorStoragePullthroughConfiguration;
import com.contentgrid.surveyor.spi.config.FindMeasurementAggregationConfigurationSpiPort;
import com.contentgrid.surveyor.spi.config.FindResourceDefinitionsSpiPort;
import com.contentgrid.surveyor.spi.storage.AggregateMeasurementsSpiPort;
import com.contentgrid.surveyor.usecase.metrics.FindMetricsUseCase;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.autoconfigure.security.reactive.EndpointRequest;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.info.InfoEndpoint;
import org.springframework.boot.actuate.metrics.MetricsEndpoint;
import org.springframework.boot.actuate.metrics.export.prometheus.PrometheusScrapeEndpoint;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.util.matcher.AndServerWebExchangeMatcher;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher;

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
                        // requests to the actuators /info, /health, /metrics, and /prometheus are allowed unauthenticated
                        .matchers(EndpointRequest.to(
                                InfoEndpoint.class,
                                HealthEndpoint.class,
                                MetricsEndpoint.class,
                                PrometheusScrapeEndpoint.class
                        )).permitAll()
                        // requests FROM localhost to actuator endpoints are all permitted
                        .matchers(new AndServerWebExchangeMatcher(
                                EndpointRequest.toAnyEndpoint(),
                                mgmtExchange -> {
                                    var remoteAddress = mgmtExchange.getRequest().getRemoteAddress();
                                    if (remoteAddress != null && remoteAddress.getAddress().isLoopbackAddress()) {
                                        return ServerWebExchangeMatcher.MatchResult.match();
                                    }
                                    return ServerWebExchangeMatcher.MatchResult.notMatch();
                                })
                        ).permitAll()
                        // All other GET requests must have scope surveyor:pegman:read
                        .pathMatchers(HttpMethod.GET).access(hasScope("surveyor:pegman:read"))
                        .anyExchange().denyAll()
                )
                .oauth2ResourceServer(oauth2ResourceServer -> {
                    oauth2ResourceServer.jwt(Customizer.withDefaults());
                });
        return http.build();
    }
}
