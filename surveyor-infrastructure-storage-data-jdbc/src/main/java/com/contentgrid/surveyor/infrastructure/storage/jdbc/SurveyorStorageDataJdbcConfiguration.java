package com.contentgrid.surveyor.infrastructure.storage.jdbc;

import com.contentgrid.surveyor.values.MetricName;
import com.contentgrid.surveyor.values.ResourceId;
import com.contentgrid.surveyor.values.ResourceType;
import com.contentgrid.surveyor.values.SourceName;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.data.jdbc.core.convert.JdbcCustomConversions;
import org.springframework.data.jdbc.repository.config.AbstractJdbcConfiguration;
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

@EnableJdbcRepositories(basePackageClasses = SurveyorStorageDataJdbcConfiguration.class)
@Configuration(proxyBeanMethods = false)
public class SurveyorStorageDataJdbcConfiguration extends AbstractJdbcConfiguration {

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


    @Override
    protected List<?> userConverters() {
        return List.of(
                new SourceNameToStringConverter(),
                new StringToSourceNameConverter(),
                new MetricNameToStringConverter(),
                new ResourceTypeToStringConverter(),
                new StringToResourceTypeConverter(),
                new ResourceIdToStringConverter(),
                new StringToResourceIdConverter()
        );
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
