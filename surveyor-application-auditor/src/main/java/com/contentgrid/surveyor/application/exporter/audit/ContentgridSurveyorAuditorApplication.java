package com.contentgrid.surveyor.application.exporter.audit;

import static org.springframework.security.authorization.AuthorityReactiveAuthorizationManager.hasAuthority;

import com.contentgrid.surveyor.infrastructure.config.spring.SurveyorSpringConfiguration;
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
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtGrantedAuthoritiesConverterAdapter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.util.matcher.AndServerWebExchangeMatcher;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher;

@SpringBootApplication
@Import({
        RabbitmqConfiguration.class,
//        SurveyorWebConfiguration.class,
        SurveyorSpringConfiguration.class,
//        SurveyorStoragePullthroughConfiguration.class,
//        SurveyorMeasurementCollectorPrometheusConfiguration.class
})
public class ContentgridSurveyorAuditorApplication {

    public static void main(String[] args) {
        SpringApplication.run(ContentgridSurveyorAuditorApplication.class, args);
    }

    @Bean
    SecurityFilterChain chain(HttpSecurity http) throws Exception {
        return http.authorizeHttpRequests(request -> request.anyRequest().permitAll()).build();
    }

//    @Bean
//    ReactiveJwtAuthenticationConverter reactiveJwtAuthenticationConverter() {
//        var grantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
//        grantedAuthoritiesConverter.setAuthorityPrefix("ENTITLEMENT_");
//        grantedAuthoritiesConverter.setAuthoritiesClaimName("entitlements");
//
//        var converter = new ReactiveJwtAuthenticationConverter();
//        converter.setJwtGrantedAuthoritiesConverter(
//                new ReactiveJwtGrantedAuthoritiesConverterAdapter(grantedAuthoritiesConverter));
//        return converter;
//    }
//
//    @Bean
//    SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http,
//            ReactiveJwtAuthenticationConverter reactiveJwtAuthenticationConverter) {
//        http
//                .authorizeExchange(exchanges -> exchanges.anyExchange().permitAll());
//                .authorizeExchange(exchanges -> exchanges
//                        // requests to the actuators /info, /health, /metrics, and /prometheus are allowed unauthenticated
//                        .matchers(EndpointRequest.to(
//                                InfoEndpoint.class,
//                                HealthEndpoint.class,
//                                MetricsEndpoint.class,
//                                PrometheusScrapeEndpoint.class
//                        )).permitAll()
//                        // requests FROM localhost to actuator endpoints are all permitted
//                        .matchers(new AndServerWebExchangeMatcher(
//                                EndpointRequest.toAnyEndpoint(),
//                                mgmtExchange -> {
//                                    var remoteAddress = mgmtExchange.getRequest().getRemoteAddress();
//                                    if (remoteAddress != null && remoteAddress.getAddress().isLoopbackAddress()) {
//                                        return ServerWebExchangeMatcher.MatchResult.match();
//                                    }
//                                    return ServerWebExchangeMatcher.MatchResult.notMatch();
//                                })
//                        ).permitAll()
//                        // All other GET requests must have entitlement surveyor:auditor:read
//                        .pathMatchers(HttpMethod.GET).access(hasAuthority("ENTITLEMENT_surveyor:auditor:read"))
//                        .anyExchange().denyAll()
//                )
//                .oauth2ResourceServer(oauth2ResourceServer -> {
//                    oauth2ResourceServer.jwt(jwtSpec -> {
//                        jwtSpec.jwtAuthenticationConverter(reactiveJwtAuthenticationConverter);
//                    });
//                });
//        return http.build();
//    }
}
