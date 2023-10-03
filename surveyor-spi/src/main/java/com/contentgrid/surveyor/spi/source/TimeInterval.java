package com.contentgrid.surveyor.spi.source;

import java.time.Duration;
import java.time.Instant;
import java.util.stream.Stream;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.experimental.FieldDefaults;

@Value(staticConstructor = "between")
public class TimeInterval {

    Instant startTime; // inclusive
    Instant endTime; // exclusive

    public static TimeInterval after(Instant startTime, Duration duration) {
        return between(startTime, startTime.plus(duration));
    }

    public Overlap contains(TimeInterval container) {
        var containsStartTime = contains(container.getStartTime());
        var containsEndTime = contains(container.getEndTime());
        if (containsStartTime && containsEndTime) {
            return Overlap.FULLY;
        }
        if (containsStartTime && !containsEndTime) {
            return Overlap.PARTIAL_AFTER;
        }
        if (!containsStartTime && containsEndTime) {
            return Overlap.PARTIAL_BEFORE;
        }
        if (startTime.isAfter(container.endTime) || endTime.isBefore(container.startTime)) {
            return Overlap.NOT;
        }
        return Overlap.PARTIAL;
    }

    public boolean contains(Instant instant) {
        return (startTime.equals(instant) || startTime.isBefore(instant)) && endTime.isAfter(instant);
    }

    public TimeInterval alignedToMultipleOf(Duration duration) {
        var timeDelta = Duration.between(startTime, endTime);
        long multiple = timeDelta.dividedBy(duration);
        var recalculatedEndTime = startTime.plus(duration.multipliedBy(multiple));
        return new TimeInterval(startTime, recalculatedEndTime);
    }

    public Stream<com.contentgrid.surveyor.spi.storage.TimeInterval> chunkedBy(Duration chunkDuration) {
        return Stream.iterate(
                startTime,
                time -> time.isBefore(endTime),
                time -> time.plus(chunkDuration)
        ).map(chunkStartTime -> new com.contentgrid.surveyor.spi.storage.TimeInterval(chunkStartTime,
                chunkStartTime.plus(chunkDuration)));
    }

    @RequiredArgsConstructor
    @Getter
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    public enum Overlap {
        NOT(false, false, false),
        PARTIAL_BEFORE(false, true, false),
        PARTIAL_AFTER(false, false, true),
        PARTIAL(false, true, true),
        FULLY(true, false, false),
        ;

        boolean contained;
        boolean partiallyBefore;
        boolean partiallyAfter;

    }


}
