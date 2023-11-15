package com.contentgrid.surveyor.infrastructure.collector.pegman.transport;

import com.contentgrid.surveyor.jackson.streaming.parser.Assembler;
import com.contentgrid.surveyor.jackson.streaming.parser.JsonPath;
import com.contentgrid.surveyor.jackson.streaming.parser.JsonPath.PathPart;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class PegmanMetricAssembler implements Assembler<PegmanMetric> {
    private final ObjectMapper objectMapper;

    @Override
    public Optional<TokenAssembler<PegmanMetric>> forPath(JsonPath tokenList) {
        if(tokenList.eq(PathPart.object("_embedded"), PathPart.object("metrics"), PathPart.array())) {
            return Optional.of(TokenAssembler.fromParser(TokenBufferParser.readValue(objectMapper, PegmanMetric.class)));
        }
        return Optional.empty();
    }
}
