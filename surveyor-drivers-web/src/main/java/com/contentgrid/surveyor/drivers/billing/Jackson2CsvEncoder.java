package com.contentgrid.surveyor.drivers.billing;

import com.fasterxml.jackson.core.JsonGenerator.Feature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import java.util.Map;
import org.springframework.core.ResolvableType;
import org.springframework.http.codec.json.AbstractJackson2Encoder;
import org.springframework.util.MimeType;

public class Jackson2CsvEncoder extends AbstractJackson2Encoder {

    public Jackson2CsvEncoder() {
        this(new CsvMapper(), MimeType.valueOf("text/csv"));
    }

    public Jackson2CsvEncoder(ObjectMapper mapper, MimeType... mimeTypes) {
        super(mapper, mimeTypes);
    }

    @Override
    protected byte[] getStreamingMediaTypeSeparator(MimeType mimeType) {
        // This is a bit of a hack. Without this override, AbstractJackson2Encoder skips the \n in the csv
        return new byte[] {};
    }

    @Override
    protected ObjectWriter customizeWriter(ObjectWriter writer, MimeType mimeType, ResolvableType elementType,
            Map<String, Object> hints) {
        var schema = ((CsvMapper) this.getObjectMapper()).schemaFor(elementType.resolve());
        return writer.with(schema).with(Feature.WRITE_BIGDECIMAL_AS_PLAIN);
    }
}
