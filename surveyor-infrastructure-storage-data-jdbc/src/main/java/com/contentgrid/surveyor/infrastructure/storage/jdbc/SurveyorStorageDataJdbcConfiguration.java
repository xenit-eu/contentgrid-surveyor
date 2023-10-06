package com.contentgrid.surveyor.infrastructure.storage.jdbc;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.data.jdbc.core.convert.JdbcCustomConversions;
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

@EnableJdbcRepositories(basePackageClasses = SurveyorStorageDataJdbcConfiguration.class)
@Configuration(proxyBeanMethods = false)
public class SurveyorStorageDataJdbcConfiguration {

    @Bean
    DataJdbcMetricsGateway dataJdbcMetricsGateway(ResourceRepository resourceRepository,
            MetricRepository metricRepository) {
        return new DataJdbcMetricsGateway(resourceRepository, metricRepository);
    }

    @Bean
    DataJdbcAggregationGateway dataJdbcAggregationGateway(ResourceRepository resourceRepository,
            NamedParameterJdbcTemplate jdbcTemplate, JdbcCustomConversions customConversions) {
        var conversionService = new DefaultConversionService();
        customConversions.registerConvertersIn(conversionService);
        return new DataJdbcAggregationGateway(resourceRepository, jdbcTemplate, conversionService);
    }


}
