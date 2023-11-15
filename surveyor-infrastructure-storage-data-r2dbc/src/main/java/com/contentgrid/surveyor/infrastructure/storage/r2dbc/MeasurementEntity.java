package com.contentgrid.surveyor.infrastructure.storage.r2dbc;

import com.contentgrid.surveyor.infrastructure.storage.r2dbc.MeasurementEntity.MeasurementEntityId;
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

@Table("measurement")
@Getter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
public class MeasurementEntity implements Persistable<MeasurementEntityId> {

    @NonNull
    Long metricId;
    @NonNull
    Instant startTime;
    @NonNull
    Instant endTime;
    @NonNull
    BigDecimal value;

    @Override
    public MeasurementEntityId getId() {
        return new MeasurementEntityId(
                metricId,
                startTime,
                endTime
        );
    }

    @Override
    public boolean isNew() {
        return true;
    }

    record MeasurementEntityId(
            Long metricId,
            Instant startTime,
            Instant endTime
    ) {

    }
}
