package com.contentgrid.surveyor.application.pegman.boot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.TestConfiguration;

@TestConfiguration(proxyBeanMethods = false)
public class TestContentgridSurveyorPegmanApplication {

    public static void main(String[] args) {
        SpringApplication.from(ContentgridSurveyorPegmanApplication::main)
                .with(TestContentgridSurveyorPegmanApplication.class)
                .run(args);
    }

}
