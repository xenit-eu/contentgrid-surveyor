package com.contentgrid.surveyor.infrastructure.collector.prometheus.transport;

import com.contentgrid.surveyor.infrastructure.collector.prometheus.transport.PrometheusResultAssembler.AssemblyResult;
import com.contentgrid.surveyor.jackson.streaming.parser.Assembler;
import com.contentgrid.surveyor.jackson.streaming.parser.JsonPath;
import com.contentgrid.surveyor.jackson.streaming.parser.JsonPath.PathPart;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
public class PrometheusResultAssembler<T extends PrometheusResult> implements Assembler<AssemblyResult<T>> {

    private final ObjectMapper objectMapper;
    private final Class<T> resultType;

    private final ErrorAssemblyResult errorResult = new ErrorAssemblyResult();

    @Override
    public Optional<TokenAssembler<AssemblyResult<T>>> forPath(JsonPath tokenList) {
        if (tokenList.eq(PathPart.object("data"), PathPart.object("result"), PathPart.array())) {
            return Optional.of(
                    TokenAssembler.fromParser(TokenBufferParser.readValue(objectMapper, resultType))
                            .map(DataAssemblyResult::new)
            );
        }

        if (tokenList.eq(PathPart.object("errorType"))) {
            return Optional.of(
                    TokenAssembler.fromParser(TokenBufferParser.readValue(objectMapper, String.class))
                            .andThen(flux -> flux.flatMap(errorType -> {
                                errorResult.setErrorType(errorType);
                                return errorResult.toMono();
                            }))
            );
        }

        if (tokenList.eq(PathPart.object("error"))) {
            return Optional.of(
                    TokenAssembler.fromParser(TokenBufferParser.readValue(objectMapper, String.class))
                            .andThen(flux -> flux.flatMap(error -> {
                                errorResult.setError(error);
                                return errorResult.toMono();
                            }))
            );
        }

        if (tokenList.eq(PathPart.object("warnings"))) {
            return Optional.of(
                    TokenAssembler.fromParser(
                                    TokenBufferParser.readValue(objectMapper, new TypeReference<List<String>>() {
                                    }))
                            .map(WarningsAssemblyResult::new)
            );
        }

        return Optional.empty();
    }

    public sealed interface AssemblyResult<T> {

    }

    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    public final class WarningsAssemblyResult implements AssemblyResult<T> {

        @Getter
        private final List<String> warnings;
    }

    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    @Getter
    public final class DataAssemblyResult implements AssemblyResult<T> {

        private final T data;
    }

    @Getter
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    @Setter(value = AccessLevel.PRIVATE)
    public final class ErrorAssemblyResult implements AssemblyResult<T> {

        private String errorType;
        private String error;

        private ErrorAssemblyResult() {
            this(null, null);
        }

        private Mono<ErrorAssemblyResult> toMono() {
            if (errorType != null && error != null) {
                return Mono.just(this);
            } else {
                return Mono.empty();
            }
        }
    }
}
