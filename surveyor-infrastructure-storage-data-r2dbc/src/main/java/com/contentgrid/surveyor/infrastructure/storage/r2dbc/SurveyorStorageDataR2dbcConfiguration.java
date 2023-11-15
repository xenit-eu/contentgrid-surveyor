package com.contentgrid.surveyor.infrastructure.storage.r2dbc;

import com.contentgrid.surveyor.values.MetricName;
import com.contentgrid.surveyor.values.ResourceId;
import com.contentgrid.surveyor.values.ResourceType;
import com.contentgrid.surveyor.values.SourceName;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;
import org.springframework.r2dbc.core.DatabaseClient;

@EnableR2dbcRepositories(basePackageClasses = SurveyorStorageDataR2dbcConfiguration.class)
@Configuration(proxyBeanMethods = false)
public class SurveyorStorageDataR2dbcConfiguration {

    @Bean
    DataR2dbcMeasurementGateway dataJdbcMetricsGateway(ResourceIdentityRepository resourceIdentityRepository,
            MetricRepository metricRepository,
            MeasurementRepository measurementRepository) {
        return new DataR2dbcMeasurementGateway(resourceIdentityRepository, metricRepository, measurementRepository);
    }

    @Bean
    DataR2dbcAggregationGateway dataJdbcAggregationGateway(MetricRepository metricRepository,
            DatabaseClient databaseClient) {
        return new DataR2dbcAggregationGateway(metricRepository, databaseClient);
    }

    @Bean
    DataR2dbcResourceGateway dataJdbcResourceGateway(ResourceIdentityRepository resourceIdentityRepository,
            MetricRepository metricRepository) {
        return new DataR2dbcResourceGateway(resourceIdentityRepository, metricRepository);
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
