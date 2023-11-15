package com.contentgrid.surveyor.infrastructure.storage.r2dbc;

import com.contentgrid.surveyor.spi.resources.Metric;
import java.util.Map;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table("metric")
@Getter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MetricEntity {

    @Id
    Long id;

    @NonNull
    Long resourceIdentityId;

    @NonNull
    String metricName;

    @NonNull
    Map<String, String> tags;

    public static MetricEntity from(Long resourceIdentityId, Metric metric) {
        return new MetricEntity(null, resourceIdentityId, metric.getMetricName().name(), metric.getTags());
    }

}
