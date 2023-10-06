package com.contentgrid.surveyor.application.boot;

import com.contentgrid.surveyor.application.boot.autoconfigure.OptionalDataSourceAutoConfiguration;
import com.contentgrid.surveyor.application.configuration.SurveyorConfiguration;
import com.contentgrid.surveyor.drivers.schedule.SurveyorSchedulerConfiguration;
import com.contentgrid.surveyor.drivers.web.SurveyorWebConfiguration;
import com.contentgrid.surveyor.infrastructure.config.spring.SurveyorSpringConfiguration;
import com.contentgrid.surveyor.infrastructure.storage.jdbc.SurveyorStorageDataJdbcConfiguration;
import com.contentgrid.surveyor.infrastructure.storage.memory.SurveyorStorageInMemoryConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@Import({
        SurveyorConfiguration.class,
        SurveyorWebConfiguration.class,
        SurveyorSchedulerConfiguration.class,
        SurveyorSpringConfiguration.class
})
@ImportAutoConfiguration(value = OptionalDataSourceAutoConfiguration.class, exclude = {
        DataSourceAutoConfiguration.class})
public class ContentgridSurveyorApplication {

    public static void main(String[] args) {
        SpringApplication.run(ContentgridSurveyorApplication.class, args);
    }

    @ConditionalOnProperty("spring.datasource.url")
    @Import(SurveyorStorageDataJdbcConfiguration.class)
    @Configuration(proxyBeanMethods = false)
    static class StorageDatabase {

    }

    @ConditionalOnProperty(value = "spring.datasource.url", havingValue = "none", matchIfMissing = true)
    @Import(SurveyorStorageInMemoryConfiguration.class)
    @Configuration(proxyBeanMethods = false)
    class StorageInMemory {

    }
}
