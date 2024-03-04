package com.contentgrid.surveyor.application.exporter.cgapp;

import io.prometheus.metrics.exporter.common.PrometheusScrapeHandler;
import io.prometheus.metrics.model.registry.PrometheusRegistry;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
@Import({RabbitmqConfiguration.class})
public class ContentgridSurveyorCgappApiExporterApplication {

    public static void main(String[] args) {
        SpringApplication.run(ContentgridSurveyorCgappApiExporterApplication.class, args);
    }

    @Bean
    MetricsCollector metricsCollector() {
        return new MetricsCollector();
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

}
