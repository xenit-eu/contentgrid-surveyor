package com.contentgrid.surveyor.drivers.web;

import org.springframework.boot.web.codec.CodecCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.hateoas.config.EnableHypermediaSupport;
import org.springframework.hateoas.config.EnableHypermediaSupport.HypermediaType;
import org.springframework.hateoas.support.WebStack;

@Configuration(proxyBeanMethods = false)
@ComponentScan(basePackageClasses = SurveyorWebConfiguration.class)
@EnableHypermediaSupport(type = {HypermediaType.HAL}, stacks = WebStack.WEBFLUX)
public class SurveyorWebConfiguration {
    @Bean

    CodecCustomizer csvCodecCustomizer() {
        return (configurer) -> {
            configurer.customCodecs().register(new Jackson2CsvEncoder());
        };
    }

}
