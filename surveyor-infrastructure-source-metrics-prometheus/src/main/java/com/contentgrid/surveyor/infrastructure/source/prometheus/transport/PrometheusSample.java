package com.contentgrid.surveyor.infrastructure.source.prometheus.transport;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;

@JsonDeserialize(
        using = PrometheusSample.PrometheusSampleDeserializer.class
)
public record PrometheusSample(
        Instant timestamp,
        BigDecimal value
) {

    static class PrometheusSampleDeserializer extends JsonDeserializer {

        @Override
        public Object deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            if (p.isExpectedStartArrayToken()) {
                Object[] data = p.readValueAs(Object[].class);
                if (data.length != 2) {
                    return ctxt.handleUnexpectedToken(PrometheusSample.class, p);
                } else {
                    if (data[0] instanceof Number n) {
                        long seconds = n.longValue();
                        int nanos = (int) ((n.doubleValue() - seconds) * 1e9);
                        var timestamp = Instant.ofEpochSecond(n.longValue(), nanos);
                        if (data[1] instanceof String s) {
                            var value = new BigDecimal(s);
                            return new PrometheusSample(timestamp, value);
                        }
                        throw MismatchedInputException.from(p, String.class, "not a string metric value");
                    }
                    throw MismatchedInputException.from(p, Instant.class, "not a timestamp metric");
                }
            } else {
                return ctxt.handleUnexpectedToken(PrometheusSample.class, p);
            }
        }
    }
}
