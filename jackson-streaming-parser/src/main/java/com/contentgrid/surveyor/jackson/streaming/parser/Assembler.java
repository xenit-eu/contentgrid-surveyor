package com.contentgrid.surveyor.jackson.streaming.parser;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.ResolvedType;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.util.TokenBuffer;
import java.io.IOException;
import java.util.Optional;
import reactor.core.publisher.Flux;

@FunctionalInterface
public interface Assembler<T> {

    Optional<TokenAssembler<T>> forPath(JsonPath tokenList);

    @FunctionalInterface
    interface TokenAssembler<T> {

        Flux<T> consume(JsonParser parser) throws IOException;

        static <T> TokenAssembler<T> fromParser(TokenBufferParser<T> parser) {
            return new TokenBufferParserAssembler<>(parser);
        }

    }

    @FunctionalInterface
    interface TokenBufferParser<T> {

        T parse(TokenBuffer buffer) throws IOException;

        static <T> TokenBufferParser<T> readValue(ObjectMapper objectMapper, Class<T> type) {
            return buffer -> objectMapper.readValue(buffer.asParser(objectMapper), type);
        }

        static <T> TokenBufferParser<T> readValue(ObjectMapper objectMapper, TypeReference<T> type) {
            return buffer -> objectMapper.readValue(buffer.asParser(objectMapper), type);
        }

        static <T> TokenBufferParser<T> readValue(ObjectMapper objectMapper, ResolvedType type) {
            return buffer -> objectMapper.readValue(buffer.asParser(objectMapper), type);
        }
    }
}
