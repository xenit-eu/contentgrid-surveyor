package com.contentgrid.surveyor.jackson.streaming.parser;

import com.contentgrid.surveyor.jackson.streaming.parser.JsonPath.PathPart;
import lombok.Value;

@Value
class ObjectPathPart implements PathPart {

    String fieldName;

    public String toString() {
        if (fieldName != null) {
            return "." + fieldName;
        } else {
            return ".*";
        }
    }
}
