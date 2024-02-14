package com.contentgrid.surveyor.application.exporter.cgapp;

import io.prometheus.metrics.exporter.common.PrometheusScrapeHandler;
import io.prometheus.metrics.model.registry.PrometheusRegistry;
import java.net.InetAddress;
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
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AndRequestMatcher;

@EnableScheduling
@SpringBootApplication
@Import({RabbitmqConfiguration.class})
public class ContentgridSurveyorExporterCgappApplication {

    public static void main(String[] args) {
        SpringApplication.run(ContentgridSurveyorExporterCgappApplication.class, args);
    }

    @Bean
    @ConditionalOnBean(MetricsCollector.class)
    PrometheusRegistry myMeterRegistry(MetricsCollector metricsCollector) {
        var registry = new PrometheusRegistry();
        registry.register(metricsCollector);
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
