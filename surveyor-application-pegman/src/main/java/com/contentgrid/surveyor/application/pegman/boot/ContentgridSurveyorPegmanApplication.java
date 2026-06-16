package com.contentgrid.surveyor.application.pegman.boot;

import static org.springframework.security.authorization.AuthorityReactiveAuthorizationManager.hasAuthority;

import com.contentgrid.common.spring.actuators.ExposedActuatorEndpoint;
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
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtGrantedAuthoritiesConverterAdapter;
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
    ReactiveJwtAuthenticationConverter reactiveJwtAuthenticationConverter() {
        var grantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
        grantedAuthoritiesConverter.setAuthorityPrefix("ENTITLEMENT_");
        grantedAuthoritiesConverter.setAuthoritiesClaimName("entitlements");

        var converter = new ReactiveJwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(
                new ReactiveJwtGrantedAuthoritiesConverterAdapter(grantedAuthoritiesConverter));
        return converter;
    }

    @Bean
    ExposedActuatorEndpoint exposedMetricsEndpoint() {
        return new ExposedActuatorEndpoint(MetricsEndpoint.class);
    }

    @Bean
    SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http,
            ReactiveJwtAuthenticationConverter reactiveJwtAuthenticationConverter) {
        http
                .authorizeExchange(exchanges -> exchanges
                        // All other GET requests must have entitlement surveyor:pegman:read
                        .pathMatchers(HttpMethod.GET).access(hasAuthority("ENTITLEMENT_surveyor:pegman:read"))
                        .anyExchange().denyAll()
                )
                .oauth2ResourceServer(oauth2ResourceServer -> {
                    oauth2ResourceServer.jwt(jwtSpec -> {
                        jwtSpec.jwtAuthenticationConverter(reactiveJwtAuthenticationConverter);
                    });
                });
        return http.build();
    }
}
