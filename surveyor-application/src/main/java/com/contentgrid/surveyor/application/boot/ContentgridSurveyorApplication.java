package com.contentgrid.surveyor.application.boot;

import com.contentgrid.surveyor.application.configuration.SurveyorConfiguration;
import com.contentgrid.surveyor.infrastructure.storage.memory.MetricsGateway;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@Import(SurveyorConfiguration.class)
public class ContentgridSurveyorApplication {

    public static void main(String[] args) {
        SpringApplication.run(ContentgridSurveyorApplication.class, args);
    }

    @Bean
    MetricsGateway metricsGateway() {
        return new MetricsGateway();
    }
}
