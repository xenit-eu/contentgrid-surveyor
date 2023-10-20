package com.contentgrid.surveyor.jackson.streaming.parser;

import com.contentgrid.surveyor.jackson.streaming.parser.Assembler.TokenAssembler;
import com.contentgrid.surveyor.jackson.streaming.parser.Assembler.TokenBufferParser;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.util.TokenBuffer;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
class TokenBufferParserAssembler<T> implements TokenAssembler<T> {
    private final TokenBufferParser<T> bufferParser;
    private TokenBuffer buffer;
    private int depth = 0;

    @Override
    public Flux<T> consume(JsonParser parser) throws IOException {
        if(buffer == null) {
            buffer = new TokenBuffer(parser);
        }
        var token = parser.currentToken();

        if(token.isStructStart()) {
            this.depth++;
        } else if(token.isStructEnd()) {
            this.depth --;
        }

        buffer.copyCurrentEvent(parser);

        if(this.depth == 0) {
            return Flux.from(Mono.just(bufferParser.parse(buffer)));
        } else {
            return Flux.empty();
        }
    }
}
