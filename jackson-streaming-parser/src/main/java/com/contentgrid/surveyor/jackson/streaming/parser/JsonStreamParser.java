package com.contentgrid.surveyor.jackson.streaming.parser;

import com.contentgrid.surveyor.jackson.streaming.parser.JsonPath.PathPart;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.async.ByteBufferFeeder;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import reactor.core.Exceptions;
import reactor.core.publisher.Flux;

public class JsonStreamParser<T> {
    private final JsonParser jsonParser;
    private final Assembler<T> assembler;
    private ParserState<T> currentState;

    public JsonStreamParser(JsonParser jsonParser, Assembler<T> assembler) {
        this.jsonParser = jsonParser;
        this.assembler = assembler;
        this.currentState = new PathUpdatingParserState();
    }

    private Optional<ParserState<T>> createParser(JsonPathImpl currentPath, ParserState<T> parentState) {
        return assembler.forPath(currentPath)
                .map(tokenAssembler -> new ActiveAssemblerParserState(
                        currentPath,
                        parentState,
                        tokenAssembler
                ));
    }

    public Flux<ParserState<T>> parse(DataBuffer buffer) {
        var inputFeeder = jsonParser.getNonBlockingInputFeeder();
        try {
            if (inputFeeder instanceof ByteBufferFeeder byteBufferFeeder) {
                try (DataBuffer.ByteBufferIterator iterator = buffer.readableByteBuffers()) {
                    while (iterator.hasNext()) {
                        byteBufferFeeder.feedInput(iterator.next());
                    }
                }
            }

            return parseTokens();

        } catch (IOException ex) {
            throw Exceptions.propagate(ex);
        } finally {
            DataBufferUtils.release(buffer);
        }
    }

    public Flux<ParserState<T>> endOfInput() {
        return Flux.defer(() -> {
            jsonParser.getNonBlockingInputFeeder().endOfInput();
            return parseTokens();
        });
    }


    private Flux<ParserState<T>> parseTokens() {
        return Flux.push(emitter -> {
            while(!this.jsonParser.isClosed()) {
                JsonToken token = null;
                try {
                    token = jsonParser.nextToken();

                    if(token == JsonToken.NOT_AVAILABLE || token == null) {
                        // We are out of data, keep the current buffer and wait for the next block of data to be fed to the parser
                        break;
                    }

                    currentState = currentState.consume(jsonParser, emitter::next);
                } catch (IOException e) {
                    emitter.error(e);
                }
            }
            emitter.complete();
        });
    }


    public static <T> Flux<T> parse(Flux<DataBuffer> buffer, Assembler<T> assembler, ObjectMapper objectMapper) {
        try {
            var parser = objectMapper.getFactory().createNonBlockingByteBufferParser();

            var streamParser = new JsonStreamParser<T>(parser, assembler);

            return buffer.flatMap(streamParser::parse)
                    .concatWith(streamParser.endOfInput())
                    .flatMap(ParserState::materialize);

        } catch (IOException e) {
            return Flux.error(e);
        }
    }

    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    @EqualsAndHashCode
    private static class JsonPathImpl implements JsonPath {
        private final JsonPathImpl parent;
        private final PathPart current;

        public JsonPathImpl() {
            this(null, null);
        }

        private Stream<PathPart> parts() {
            if(parent == null) {
                return Stream.ofNullable(current);
            }
            return Stream.concat(
                    parent.parts(),
                    Stream.of(current)
            );
        }

        @Override
        public boolean eq(PathPart... path) {
            return Objects.equals(Arrays.asList(path), parts().toList());
        }

        @Override
        public PathPart last() {
            return current;
        }

        public String toString() {
            return parts().map(Objects::toString)
                    .collect(Collectors.joining("", "[JsonPath: ", "]"));
        }

        JsonPathImpl push(PathPart part) {
            return new JsonPathImpl(this, part);
        }

        JsonPathImpl pop(Class<? extends PathPart> type) {
            if(!type.isInstance(current)) {
                throw new IllegalArgumentException("Current part is expected to be '%s' but was '%s'".formatted(type, current.getClass()));
            }
            return parent;
        }

        JsonPathImpl replaceObjectFieldName(String currentName) {
            return pop(ObjectPathPart.class)
                    .push(new ObjectPathPart(currentName));
        }
    }

    interface ParserState<T> {
        ParserState<T> consume(JsonParser parser, Consumer<ParserState<T>> finished) throws IOException;
        Flux<T> materialize();
    }

    @RequiredArgsConstructor
    @ToString
    private class ActiveAssemblerParserState implements ParserState<T> {

        @NonNull
        private final JsonPathImpl basePath;

        @NonNull
        private final ParserState<T> parentState;

        @NonNull
        @ToString.Exclude
        private final Assembler.TokenAssembler<T> tokenAssembler;

        @ToString.Exclude
        private Flux<T> resultFlux = Flux.empty();

        int depth = 0;

        public ParserState<T> consume(JsonParser parser, Consumer<ParserState<T>> finished) throws IOException {
            var currentToken = parser.currentToken();
            if(currentToken.isStructStart()) {
                depth++;
            } else if(currentToken.isStructEnd()) {
                depth--;
                if(depth < 0) {
                    // We got a closing struct too many, this belongs in the parent state.
                    // Our current buffer is empty, so we can safely discard it
                    return parentState.consume(parser, finished);
                }
            }

            resultFlux = resultFlux.concatWith(tokenAssembler.consume(parser));

            if(depth == 0) {
                finished.accept(this);
                if(basePath.last() instanceof ArrayPathPart) {
                    // If we are going through an array, we need to create a new parser on the same level
                    return createParser(basePath, parentState).orElse(parentState);
                }
                // If we were going through a single object, the we need to go to the next key on the parent level
                return parentState;
            }
            return this;
        }

        public Flux<T> materialize() {
            return resultFlux;
        }
    }

    @RequiredArgsConstructor
    @ToString
    private class PathUpdatingParserState implements ParserState<T> {
        private JsonPathImpl currentPath = new JsonPathImpl();

        @Override
        public ParserState<T> consume(JsonParser parser, Consumer<ParserState<T>> finished) throws IOException {
            currentPath = switch (parser.currentToken()) {
                case START_ARRAY -> currentPath.push(PathPart.array());
                case END_ARRAY -> currentPath.pop(ArrayPathPart.class);
                case START_OBJECT -> currentPath.push(new ObjectPathPart(null));
                case FIELD_NAME -> currentPath.replaceObjectFieldName(parser.getCurrentName());
                case END_OBJECT -> currentPath.pop(ObjectPathPart.class);
                default -> currentPath;
            };
            return createParser(currentPath, this).orElse(this);
        }

        @Override
        public Flux<T> materialize() {
            throw new UnsupportedOperationException("PathUpdatingParserState can not be materialized");
        }
    }

}
