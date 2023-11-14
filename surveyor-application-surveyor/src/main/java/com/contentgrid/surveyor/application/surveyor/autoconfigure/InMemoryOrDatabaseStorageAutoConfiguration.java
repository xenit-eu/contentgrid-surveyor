package com.contentgrid.surveyor.application.surveyor.autoconfigure;

import com.contentgrid.surveyor.infrastructure.storage.jdbc.SurveyorStorageDataJdbcConfiguration;
import com.contentgrid.surveyor.infrastructure.storage.memory.SurveyorStorageInMemoryConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.data.r2dbc.R2dbcDataAutoConfiguration;
import org.springframework.boot.autoconfigure.r2dbc.R2dbcConnectionDetails;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@AutoConfiguration
@AutoConfigureAfter({R2dbcDataAutoConfiguration.class})
public class InMemoryOrDatabaseStorageAutoConfiguration {

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnBean(R2dbcConnectionDetails.class)
    @Import(SurveyorStorageDataJdbcConfiguration.class)
    public class DatabaseStorageConfiguration {

    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnMissingBean(R2dbcConnectionDetails.class)
    @Import(SurveyorStorageInMemoryConfiguration.class)
    public class InMemoryStorageConfiguration {

    }

}
