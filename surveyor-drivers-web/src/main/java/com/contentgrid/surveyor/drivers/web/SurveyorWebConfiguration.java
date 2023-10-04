package com.contentgrid.surveyor.drivers.web;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@ComponentScan(basePackageClasses = SurveyorWebConfiguration.class)
public class SurveyorWebConfiguration {

}
