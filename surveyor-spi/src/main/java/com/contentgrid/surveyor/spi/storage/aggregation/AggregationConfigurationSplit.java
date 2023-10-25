package com.contentgrid.surveyor.spi.storage.aggregation;

import java.util.Objects;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public final class AggregationConfigurationSplit implements AggregationConfiguration {

    @NonNull
    private final Operation operation;

    @NonNull
    private final AggregationConfiguration configuration;

    @Override
    public boolean isEmpty() {
        return configuration.isEmpty();
    }

    @Override
    public AggregationConfigurationSplit splitLeft() {
        return configuration.splitLeft();
    }

    @Override
    public AggregationConfigurationSplit splitRight() {
        return configuration.splitRight();
    }

    public @NonNull Operation operation() {
        return operation;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (AggregationConfigurationSplit) obj;
        return Objects.equals(this.operation, that.operation) &&
                Objects.equals(this.configuration, that.configuration);
    }

    @Override
    public int hashCode() {
        return Objects.hash(operation, configuration);
    }

    @Override
    public String toString() {
        return "AggregationConfigurationSplit[" +
                "operation=" + operation + ", " +
                "configuration=" + configuration + ']';
    }

}
