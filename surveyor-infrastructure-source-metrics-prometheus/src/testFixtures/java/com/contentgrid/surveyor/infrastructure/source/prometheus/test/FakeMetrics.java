package com.contentgrid.surveyor.infrastructure.source.prometheus.test;

import static org.awaitility.Awaitility.await;

import com.contentgrid.surveyor.infrastructure.source.prometheus.transport.PrometheusResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Singular;
import lombok.Value;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.testcontainers.images.builder.Transferable;

@Value
@Slf4j
public class FakeMetrics {
    Instant startTime;
    Duration sampleInterval;

    List<Metric> metrics = new ArrayList<>();
    Map<MetricDefinition, Instant> lastRecorded = new HashMap<>();

    private Instant nextTimestampFor(MetricDefinition definition) {
        var last = lastRecorded.get(definition);
        if(last != null) {
            return last.plus(sampleInterval);
        } else {
            return startTime;
        }
    }

    public FakeMetrics record(MetricDefinition definition, BigDecimal value) {
        return recordAt(definition, value, nextTimestampFor(definition));
    }

    public FakeMetrics recordAt(MetricDefinition definition, BigDecimal value, Instant timestamp) {
        recordInternal(definition, value, timestamp);
        return this;
    }

    private Metric recordInternal(MetricDefinition definition, BigDecimal value, Instant timestamp) {
        var metric = new Metric(definition, value, timestamp);
        metrics.add(metric);
        lastRecorded.put(definition, timestamp);
        return metric;
    }

    public FakeMetrics recordAllUntil(MetricDefinition definition, Function<RecordingContext, BigDecimal> valueGenerator, Instant endTime) {
        var ts = nextTimestampFor(definition);
        var context = new RecordingContextImpl(ts);
        while(endTime.isAfter(ts)) {
            var metric = recordInternal(definition, valueGenerator.apply(context), ts);
            context.previousMetric.set(metric);
            ts = nextTimestampFor(definition);
        }
        return this;
    }

    public interface RecordingContext {
        Duration durationSinceStart();
        Duration durationSinceRecording();
        Optional<Metric> previous();
    }

    @RequiredArgsConstructor
    private class RecordingContextImpl implements RecordingContext {
        private final Instant recordingStartTime;
        private final AtomicReference<Metric> previousMetric = new AtomicReference<>();

        @Override
        public Duration durationSinceStart() {
            return previous()
                    .map(m -> Duration.between(startTime, m.timestamp()).plus(sampleInterval))
                    .orElseGet(() -> Duration.between(startTime, recordingStartTime));
        }

        @Override
        public Duration durationSinceRecording() {
            return previous()
                    .map(m -> Duration.between(recordingStartTime, m.timestamp).plus(sampleInterval))
                    .orElseGet(() -> Duration.ZERO);
        }

        @Override
        public Optional<Metric> previous() {
            return Optional.ofNullable(previousMetric.get());
        }
    }

    private record Metric(
            MetricDefinition name,
            BigDecimal value,
            Instant timestamp
    ) {
        String encode() {
            return name.encode() + " "+value.toPlainString()+" "+(timestamp.toEpochMilli()/1000d);
        }

    }

    @Value
    public static class MetricDefinition {
        @NonNull
        String name;

        @Singular
        Map<String, String> labels;

        @Getter(value = AccessLevel.NONE)
        @NonFinal
        String encoded = null;

        @Builder
        private MetricDefinition(@NonNull String name, @Singular Map<String, String> labels) {
            this.name = name;
            this.labels = labels;
        }

         String encode() {
            if(encoded == null) {
                encoded = name+"{"+labels.entrySet()
                .stream()
                .map(entry -> entry.getKey()+"=\""+entry.getValue()+"\"")
                .collect(Collectors.joining(","))+"}";
            }
            return encoded;
        }
    }

    public byte[] toOpenMetrics() {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();

        var printStream = new PrintStream(stream);

        for (Metric metric : metrics) {
            printStream.println(metric.encode());
        }
        printStream.println("# EOF");

        return stream.toByteArray();
    }

    public void injectInto(PrometheusContainer prometheusContainer) throws IOException, InterruptedException {
        var filename = "/tmp/"+ UUID.randomUUID().toString();
        prometheusContainer.copyFileToContainer(Transferable.of(toOpenMetrics()), filename);
        var result = prometheusContainer.execInContainer("promtool", "tsdb", "create-blocks-from", "openmetrics", "--max-block-duration=1d", filename);
        if(result.getExitCode() != 0) {
            throw new IOException("Failed to inject metrics into container: %s".formatted(result.getStderr()));
        } else {
            log.debug("Injected metrics into container: {}", result.getStdout());
        }

        var webClient = prometheusContainer.getClient();
        var maybeLastMetric = lastRecorded.entrySet()
                .stream()
                .reduce((a, b) -> a.getValue().isAfter(b.getValue())?a:b);

        if(maybeLastMetric.isEmpty()) {
            return;
        }

        var lastMetric = maybeLastMetric.orElseThrow();

        await()
                .atMost(1, TimeUnit.MINUTES)
                .until(() -> {
                    return webClient
                            .get()
                            .uri(uri -> uri.path("api/v1/query")
                                    .queryParam("query", "{query}")
                                    .queryParam("time", "{time}")
                                    .build(Map.of(
                                            "query", lastMetric.getKey().encode(),
                                            "time", lastMetric.getValue().toString()
                                    ))
                            )
                            .retrieve()
                            .toEntity(PrometheusResponse.class)
                            .block()
                            .getBody();
                }, body -> !body.data().getResult().isEmpty());
    }
}
