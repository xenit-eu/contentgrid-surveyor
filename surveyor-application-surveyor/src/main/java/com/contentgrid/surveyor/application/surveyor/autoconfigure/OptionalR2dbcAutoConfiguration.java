package com.contentgrid.surveyor.application.surveyor.autoconfigure;

import com.contentgrid.surveyor.application.surveyor.autoconfigure.OptionalR2dbcAutoConfiguration.DatabaseUrlOrR2dbcConnectionDetailsCondition;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.AnyNestedCondition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.data.r2dbc.R2dbcDataAutoConfiguration;
import org.springframework.boot.autoconfigure.r2dbc.R2dbcAutoConfiguration;
import org.springframework.boot.autoconfigure.r2dbc.R2dbcConnectionDetails;
import org.springframework.context.annotation.Conditional;

@AutoConfiguration
@AutoConfigureBefore(R2dbcDataAutoConfiguration.class)
@Conditional(DatabaseUrlOrR2dbcConnectionDetailsCondition.class)
public class OptionalR2dbcAutoConfiguration extends R2dbcAutoConfiguration {

    static class DatabaseUrlOrR2dbcConnectionDetailsCondition extends AnyNestedCondition {

        public DatabaseUrlOrR2dbcConnectionDetailsCondition() {
            super(ConfigurationPhase.REGISTER_BEAN);
        }

        @ConditionalOnProperty(value = "spring.r2dbc.url")
        static class OnR2dbcDatabaseUrl {

        }

        @ConditionalOnBean(R2dbcConnectionDetails.class)
        static class OnR2dbcConnectionDetails {

        }
    }

}
