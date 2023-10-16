package com.contentgrid.surveyor.surveyor.boot.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(value = "spring.datasource.url")
@AutoConfigureBefore(JdbcTemplateAutoConfiguration.class)
public class OptionalDataSourceAutoConfiguration extends DataSourceAutoConfiguration {

}
