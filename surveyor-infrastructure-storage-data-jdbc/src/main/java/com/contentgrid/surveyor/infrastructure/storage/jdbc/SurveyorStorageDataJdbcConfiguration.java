package com.contentgrid.surveyor.infrastructure.storage.jdbc;

import com.contentgrid.surveyor.values.MetricName;
import com.contentgrid.surveyor.values.ResourceId;
import com.contentgrid.surveyor.values.ResourceType;
import com.contentgrid.surveyor.values.SourceName;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.ConnectionFactoryOptions;
import io.r2dbc.spi.Option;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.flyway.FlywayConnectionDetails;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.JdbcConnectionDetails;
import org.springframework.boot.autoconfigure.r2dbc.R2dbcConnectionDetails;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.data.r2dbc.convert.R2dbcConverter;
import org.springframework.data.r2dbc.convert.R2dbcCustomConversions;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;
import org.springframework.r2dbc.core.DatabaseClient;

@EnableR2dbcRepositories(basePackageClasses = SurveyorStorageDataJdbcConfiguration.class)
@Configuration(proxyBeanMethods = false)
public class SurveyorStorageDataJdbcConfiguration {

    @Bean
    DataJdbcMeasurementGateway dataJdbcMetricsGateway(ResourceIdentityRepository resourceIdentityRepository,
            MetricRepository metricRepository,
            MeasurementRepository measurementRepository) {
        return new DataJdbcMeasurementGateway(resourceIdentityRepository, metricRepository, measurementRepository);
    }

    @Bean
    DataJdbcAggregationGateway dataJdbcAggregationGateway(MetricRepository metricRepository,
            DatabaseClient databaseClient, R2dbcCustomConversions customConversions) {
        var conversionService = new DefaultConversionService();
        customConversions.registerConvertersIn(conversionService);
        return new DataJdbcAggregationGateway(metricRepository, databaseClient, conversionService);
    }

    @WritingConverter
    private static class SourceNameToStringConverter implements Converter<SourceName, String> {

        @Override
        public String convert(SourceName sourceName) {
            return sourceName.sourceName();
        }
    }

    @ReadingConverter
    private static class StringToSourceNameConverter implements Converter<String, SourceName> {

        @Override
        public SourceName convert(String source) {
            return SourceName.of(source);
        }
    }

    @WritingConverter
    private static class MetricNameToStringConverter implements Converter<MetricName, String> {

        @Override
        public String convert(MetricName metricName) {
            return metricName.name();
        }
    }

    @WritingConverter
    private static class ResourceTypeToStringConverter implements Converter<ResourceType, String> {

        @Override
        public String convert(ResourceType resourceType) {
            return resourceType.resourceType();
        }
    }

    @ReadingConverter
    private static class StringToResourceTypeConverter implements Converter<String, ResourceType> {

        @Override
        public ResourceType convert(String source) {
            return ResourceType.of(source);
        }
    }

    @WritingConverter
    private static class ResourceIdToStringConverter implements Converter<ResourceId, String> {

        @Override
        public String convert(ResourceId resourceId) {
            return resourceId.resourceId();
        }
    }

    @ReadingConverter
    private static class StringToResourceIdConverter implements Converter<String, ResourceId> {

        @Override
        public ResourceId convert(String source) {
            return ResourceId.of(source);
        }
    }
}
