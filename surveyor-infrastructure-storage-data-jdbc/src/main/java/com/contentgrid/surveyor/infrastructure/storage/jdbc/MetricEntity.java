package com.contentgrid.surveyor.infrastructure.storage.jdbc;

import com.contentgrid.surveyor.infrastructure.storage.jdbc.MetricEntity.MetricEntityId;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Table;

@Table("metric_events")
@Getter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
public class MetricEntity implements Persistable<MetricEntityId> {

    @NonNull
    Long resourceId;
    @NonNull
    Instant startTime;
    @NonNull
    Instant endTime;
    @NonNull
    BigDecimal value;

    @Override
    public MetricEntityId getId() {
        return new MetricEntityId(
                resourceId,
                startTime,
                endTime
        );
    }

    @Override
    public boolean isNew() {
        return true;
    }

    record MetricEntityId(
            Long resourceId,
            Instant startTime,
            Instant endTime
    ) {

    }
}
