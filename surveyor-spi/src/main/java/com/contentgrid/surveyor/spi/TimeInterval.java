package com.contentgrid.surveyor.spi;

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

    public static TimeInterval before(Instant endTime, Duration duration) {
        return between(endTime.minus(duration), endTime);
    }

    public Overlap contains(TimeInterval container) {
        var containsStartTime = contains(container.getStartTime());
        var containsEndTime = container.getEndTime().isAfter(startTime) && (container.getEndTime().isBefore(endTime)
                || container.getEndTime().equals(endTime));
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

    public TimeInterval shiftedBy(Duration duration) {
        return TimeInterval.between(startTime.plus(duration), endTime.plus(duration));
    }

    public TimeInterval alignedToMultipleOf(Duration duration) {
        long multiple = getDuration().dividedBy(duration);
        Duration newDuration = duration.multipliedBy(multiple);
        // If the new duration is less than our new duration, add another one,
        // we want the aligned interval to be *larger than* the current one
        if (newDuration.compareTo(duration) < 0) {
            newDuration = newDuration.plus(duration);
        }

        return TimeInterval.after(startTime, newDuration);
    }

    public Duration getDuration() {
        return Duration.between(startTime, endTime);
    }

    public TimeInterval nextInterval() {
        return TimeInterval.after(endTime, getDuration());
    }

    public Stream<TimeInterval> chunkedBy(Duration chunkDuration) {
        return Stream.iterate(
                TimeInterval.after(startTime, chunkDuration),
                time -> this.contains(time).isContained(),
                TimeInterval::nextInterval
        );
    }

    public String toString() {
        return "[" + startTime + ", " + endTime + ")";
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
