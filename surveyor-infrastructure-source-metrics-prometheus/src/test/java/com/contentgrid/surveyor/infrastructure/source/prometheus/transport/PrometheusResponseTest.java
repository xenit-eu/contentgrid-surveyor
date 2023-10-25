package com.contentgrid.surveyor.infrastructure.source.prometheus.transport;


import static org.assertj.core.api.Assertions.assertThat;

import com.contentgrid.surveyor.infrastructure.source.prometheus.transport.PrometheusResponse.Status;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PrometheusResponseTest {

    @Test
    void deserializesRangeVector() throws JsonProcessingException {
        var om = new ObjectMapper();

        var response = om.readValue(
                """
                        {
                            "status": "success",
                            "data": {
                                "resultType": "matrix",
                                "result": [
                                    {
                                        "metric": {
                                            "label-a": "value1",
                                            "label-b": "value2"
                                        },
                                        "values": [
                                            [12, "5"],
                                            [13, "6"],
                                            [14, "7"]
                                        ]
                                    },
                                    {
                                        "metric": {
                                            "label-a": "value2",
                                            "label-b": "value2"
                                        },
                                        "values": [
                                            [12.23, "50"],
                                            [13, "60"],
                                            [14, "70"]
                                        ]
                                    }
                                ]
                            }
                        }
                        """,
                PrometheusResponse.class);

        assertThat(response.status()).isEqualTo(Status.SUCCESS);

        assertThat(response.data().getResult()).allSatisfy(data -> {
            assertThat(data).isInstanceOf(PrometheusMatrixResult.class);
        });

        assertThat(response.data().getResult().get(0)).isInstanceOfSatisfying(PrometheusMatrixResult.class, values -> {
            assertThat(values.metric()).isEqualTo(Map.of("label-a", "value1", "label-b", "value2"));
            assertThat(values.values()).isEqualTo(List.of(
                    new PrometheusSample(Instant.ofEpochSecond(12), new BigDecimal("5")),
                    new PrometheusSample(Instant.ofEpochSecond(13), new BigDecimal("6")),
                    new PrometheusSample(Instant.ofEpochSecond(14), new BigDecimal("7"))
            ));
        });

        assertThat(response.data().getResult().get(1)).isInstanceOfSatisfying(PrometheusMatrixResult.class, values -> {
            assertThat(values.metric()).isEqualTo(Map.of("label-a", "value2", "label-b", "value2"));
            assertThat(values.values()).isEqualTo(List.of(
                    new PrometheusSample(Instant.parse("1970-01-01T00:00:12.230Z"), new BigDecimal("50")),
                    new PrometheusSample(Instant.ofEpochSecond(13), new BigDecimal("60")),
                    new PrometheusSample(Instant.ofEpochSecond(14), new BigDecimal("70"))
            ));
        });
    }

    @Test
    void deserializesInstantVector() throws JsonProcessingException {
        var om = new ObjectMapper();

        var response = om.readValue(
                """
                        {
                            "status": "success",
                            "data": {
                                "resultType": "vector",
                                "result": [
                                    {
                                        "metric": {
                                            "label-a": "value1",
                                            "label-b": "value2"
                                        },
                                        "value": [12, "5"]
                                    },
                                    {
                                        "metric": {
                                            "label-a": "value2",
                                            "label-b": "value2"
                                        },
                                        "value": [12.23, "50"]
                                    }
                                ]
                            }
                        }
                        """,
                PrometheusResponse.class);

        assertThat(response.status()).isEqualTo(Status.SUCCESS);

        assertThat(response.data().getResult()).allSatisfy(data -> {
            assertThat(data).isInstanceOf(PrometheusVectorResult.class);
        });

        assertThat(response.data().getResult().get(0)).isInstanceOfSatisfying(PrometheusVectorResult.class, values -> {
            assertThat(values.metric()).isEqualTo(Map.of("label-a", "value1", "label-b", "value2"));
            assertThat(values.value()).isEqualTo(new PrometheusSample(Instant.ofEpochSecond(12), new BigDecimal("5")));
        });

        assertThat(response.data().getResult().get(1)).isInstanceOfSatisfying(PrometheusVectorResult.class, values -> {
            assertThat(values.metric()).isEqualTo(Map.of("label-a", "value2", "label-b", "value2"));
            assertThat(values.value()).isEqualTo(
                    new PrometheusSample(Instant.parse("1970-01-01T00:00:12.230Z"), new BigDecimal("50")));
        });
    }
}