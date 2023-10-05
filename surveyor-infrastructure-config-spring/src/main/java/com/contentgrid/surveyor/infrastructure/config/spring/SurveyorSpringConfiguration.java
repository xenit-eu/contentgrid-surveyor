package com.contentgrid.surveyor.infrastructure.config.spring;

import com.contentgrid.surveyor.infrastructure.config.spring.properties.SurveyorProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(SurveyorProperties.class)
public class SurveyorSpringConfiguration {

    @Bean
    SpringConfigurationGateway springConfigurationGateway(SurveyorProperties surveyorProperties) {
        return new SpringConfigurationGateway(surveyorProperties.metrics());
    }

}
