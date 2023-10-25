package com.contentgrid.surveyor.jackson.streaming.parser;

public interface JsonPath {

    boolean eq(PathPart... tokens);

    PathPart last();

    sealed interface PathPart permits ArrayPathPart, ObjectPathPart {

        static PathPart array() {
            return new ArrayPathPart();
        }

        static PathPart object(String fieldName) {
            return new ObjectPathPart(fieldName);
        }

    }

}
