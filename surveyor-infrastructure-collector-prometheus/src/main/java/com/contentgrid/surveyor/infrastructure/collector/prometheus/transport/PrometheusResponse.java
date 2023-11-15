package com.contentgrid.surveyor.infrastructure.collector.prometheus.transport;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import java.util.List;
import lombok.Data;

public record PrometheusResponse(
        Status status,
        PrometheusData<?> data,
        String errorType,
        String error,
        List<String> warnings
) {


    public enum Status {
        @JsonProperty("success")
        SUCCESS,
        @JsonProperty("error")
        ERROR
    }

    @JsonTypeInfo(use = Id.NAME, property = "resultType")
    @JsonSubTypes({
            @Type(name = "matrix", value = PrometheusMatrixData.class),
            @Type(name = "vector", value = PrometheusVectorData.class),
            @Type(name = "scalar", value = PrometheusScalarData.class),
            @Type(name = "string", value = PrometheusStringData.class)
    })
    @Data
    public abstract static sealed class PrometheusData<T extends PrometheusResult> {

        String resultType;
        List<T> result;
    }

    public static final class PrometheusMatrixData extends PrometheusData<PrometheusMatrixResult> {

    }

    public static final class PrometheusVectorData extends PrometheusData<PrometheusVectorResult> {

    }

    public static final class PrometheusScalarData extends PrometheusData<PrometheusScalarResult> {

    }

    public static final class PrometheusStringData extends PrometheusData<PrometheusStringResult> {

    }

}
