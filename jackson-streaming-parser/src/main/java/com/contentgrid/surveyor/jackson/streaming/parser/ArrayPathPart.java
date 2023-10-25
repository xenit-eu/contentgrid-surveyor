package com.contentgrid.surveyor.jackson.streaming.parser;

import com.contentgrid.surveyor.jackson.streaming.parser.JsonPath.PathPart;
import lombok.Value;

@Value
class ArrayPathPart implements PathPart {

    public String toString() {
        return "[]";
    }

}
