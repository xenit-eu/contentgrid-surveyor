package com.contentgrid.surveyor.jackson.streaming.parser;

import com.contentgrid.surveyor.jackson.streaming.parser.JsonPath.PathPart;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.With;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

class JsonStreamParserTest {

    public final static ObjectMapper om = new ObjectMapper();


    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    @With
    private static class TestObject {

        private String x;
    }

    private static class TestObjectAssembler implements Assembler<TestObject> {

        @Override
        public Optional<TokenAssembler<TestObject>> forPath(JsonPath path) {
            if (path.eq(PathPart.object("key1"), PathPart.array()) || path.eq(PathPart.object("key2"))) {
                return Optional.of(TokenAssembler.fromParser(TokenBufferParser.readValue(om, TestObject.class)));
            }
            return Optional.empty();
        }
    }

    @Test
    void parseSingleBufferSimpleObject() {
        var buf = DefaultDataBufferFactory.sharedInstance.wrap("""
                {
                    "key1": [
                        {"x": "a"},
                        {"x": "b"}
                    ],
                    "key2": {
                        "x": "c"
                    }
                }
                """.getBytes(StandardCharsets.UTF_8));

        var flux = Flux.<DataBuffer>fromStream(Stream.of(buf));

        var data = JsonStreamParser.parse(flux, new TestObjectAssembler(), om);

        StepVerifier.create(data)
                .expectNext(new TestObject("a"))
                .expectNext(new TestObject("b"))
                .expectNext(new TestObject("c"))
                .verifyComplete();
    }

    @Test
    void parseMultipleBuffersSimpleObject() {

        var flux = Flux.<DataBuffer>fromStream(Stream.of(
                DefaultDataBufferFactory.sharedInstance.wrap("""
                        {
                            "key1": [
                        """.getBytes(StandardCharsets.UTF_8)),
                DefaultDataBufferFactory.sharedInstance.wrap("""
                        {
                            "x": "abc"
                        },
                        {
                        """.getBytes(StandardCharsets.UTF_8)),
                DefaultDataBufferFactory.sharedInstance.wrap("""
                            "x": "def"
                        }
                        ],
                        "key4": {"x": "mmm"},
                        """.getBytes(StandardCharsets.UTF_8)),
                DefaultDataBufferFactory.sharedInstance.wrap("""
                        "key2": {
                            "x": "ghi"
                        },
                        "key3": {}
                        }
                        """.getBytes(StandardCharsets.UTF_8))
        ));

        var data = JsonStreamParser.parse(flux, new TestObjectAssembler(), om);

        StepVerifier.create(data)
                .expectNext(new TestObject("abc"))
                .expectNext(new TestObject("def"))
                .expectNext(new TestObject("ghi"))
                .verifyComplete();

    }

    @Test
    void parseSingleBufferComplexObject() {
        var flux = Flux.<DataBuffer>fromStream(Stream.of(
                DefaultDataBufferFactory.sharedInstance.wrap("""
                        {
                            "key1": [
                                {
                                    "y": {
                                        "z": "abc"
                                    },
                                    "x": "abc",
                                    "z": []
                                },
                                {
                                    "y": {
                                        "z": "abc"
                                    },
                                    "x": "def",
                                    "z": []
                                }
                            ],
                            "key2": {
                                "y": {
                                    "z": "abc"
                                },
                                "x": "ghi",
                                "z": []
                            }
                        }
                        """.getBytes(StandardCharsets.UTF_8))
        ));

        var data = JsonStreamParser.parse(flux, new TestObjectAssembler(), om);

        StepVerifier.create(data)
                .expectNext(new TestObject("abc"))
                .expectNext(new TestObject("def"))
                .expectNext(new TestObject("ghi"))
                .verifyComplete();
    }

    private static class TestObjectAssembler2 implements Assembler<TestObject> {

        private String currentValue;

        @Override
        public Optional<TokenAssembler<TestObject>> forPath(JsonPath tokenList) {
            if (tokenList.eq(PathPart.object("key1"), PathPart.array(), PathPart.object("x"))) {
                return Optional.of(jsonParser -> {
                    currentValue = jsonParser.getValueAsString();
                    return Flux.empty();
                });
            }
            if (tokenList.eq(PathPart.object("key1"), PathPart.array(), PathPart.object("z"), PathPart.array())) {
                return Optional.of(parse -> {
                    return TokenAssembler.fromParser(TokenBufferParser.readValue(om, TestObject.class))
                            .consume(parse)
                            .map(o -> o.withX(currentValue + ":" + o.getX()));
                });
            }
            return Optional.empty();
        }
    }

    @Test
    void parseSingleBufferNestedObject() {
        var flux = Flux.<DataBuffer>fromStream(Stream.of(
                DefaultDataBufferFactory.sharedInstance.wrap("""
                        {
                            "key1": [
                                {
                                    "x": "abc",
                                    "z": [
                                        {
                                            "x": "zzz"
                                        },
                                        {
                                            "x": "zzy"
                                        }
                                    ]
                                },
                                {
                                    "x": "def",
                                    "z": [
                                        {
                                            "x": "zzz"
                                        },
                                        {
                                            "x": "zzy"
                                        },
                                        {
                                            "x": "zzx"
                                        }
                                    ]
                                }
                            ],
                            "key2": {
                                "x": "ghi",
                                "z": []
                            }
                        }
                        """.getBytes(StandardCharsets.UTF_8))
        ));

        var data = JsonStreamParser.parse(flux, new TestObjectAssembler2(), om);

    }
}