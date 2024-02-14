package com.contentgrid.surveyor.application.exporter.audit;

import static org.springframework.security.authorization.AuthorityReactiveAuthorizationManager.hasAuthority;

import com.contentgrid.surveyor.infrastructure.config.spring.SurveyorSpringConfiguration;
import io.prometheus.metrics.exporter.common.PrometheusScrapeHandler;
import io.prometheus.metrics.model.registry.PrometheusRegistry;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.info.InfoEndpoint;
import org.springframework.boot.actuate.metrics.MetricsEndpoint;
import org.springframework.boot.actuate.metrics.export.prometheus.PrometheusScrapeEndpoint;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtGrantedAuthoritiesConverterAdapter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.util.matcher.AndServerWebExchangeMatcher;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher;
import org.springframework.security.web.util.matcher.AndRequestMatcher;

@EnableScheduling
@SpringBootApplication
@Import({RabbitmqConfiguration.class})
public class ContentgridSurveyorAuditorApplication {

    public static void main(String[] args) {
        SpringApplication.run(ContentgridSurveyorAuditorApplication.class, args);
    }

    @Bean
    @ConditionalOnBean(AuditMetricsCollector.class)
    PrometheusRegistry myMeterRegistry(AuditMetricsCollector auditMetricsCollector) {
        var registry = new PrometheusRegistry();
        registry.register(auditMetricsCollector);
        return registry;
    }

    @Bean
    @ConditionalOnBean(PrometheusRegistry.class)
    PrometheusScrapeHandler scrapeHandler(PrometheusRegistry registry) {
        return new PrometheusScrapeHandler(registry);
    }


    @Bean
    SecurityFilterChain chain(HttpSecurity http) throws Exception {
        return http
                .authorizeHttpRequests(request -> request
                        // requests to /metrics endpoint is permitted
                        .requestMatchers("/metrics").permitAll()
                        // requests to the actuators /info, /health, /metrics, and /prometheus are allowed unauthenticated
                        .requestMatchers(EndpointRequest.to(
                                InfoEndpoint.class,
                                HealthEndpoint.class,
                                MetricsEndpoint.class,
                                PrometheusScrapeEndpoint.class
                        )).permitAll()
                        // requests from localhost to actuator endpoints are permitted
                        .requestMatchers(new AndRequestMatcher(
                                EndpointRequest.toAnyEndpoint(),
                                servletRequest -> isLoopback(servletRequest.getRemoteAddr())
                        )).permitAll()
                ).build();
    }

    private static boolean isLoopback(String remoteAddr) {
        try {
            var inetAddress = InetAddress.getByName(remoteAddr);
            return inetAddress.isLoopbackAddress();
        } catch (UnknownHostException e) {
            return false;
        }
    }

}
