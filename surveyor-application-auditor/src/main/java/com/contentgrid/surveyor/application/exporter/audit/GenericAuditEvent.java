package com.contentgrid.surveyor.application.exporter.audit;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.util.List;
import java.util.Map;
import lombok.Data;

@Data
public class GenericAuditEvent {

    @JsonProperty("operation")
    public String operation;

    @JsonProperty("request.method")
    public String requestMethod;

    @JsonProperty("request.uri")
    public String requestUri;

    @JsonProperty("response.status")
    public int responseStatus;

    @JsonIgnore
    public String getResponseCategory() {
        if (responseStatus >= 200 && responseStatus <= 299) {
            return "2xx";
        } else if (responseStatus >= 400 && responseStatus <= 499) {
            return "4xx";
        } else if (responseStatus >= 500 && responseStatus <= 599) {
            return "5xx";
        }
        return null;
    }

    @JsonProperty("response.location")
    @JsonInclude(Include.NON_NULL)
    public String responseLocation;

    @JsonProperty("subject.type")
    @JsonInclude(Include.NON_NULL)
    public String domainType;

    @JsonProperty("subject.id")
    @JsonInclude(Include.NON_NULL)
    public String id;

    @JsonProperty("subject.relation")
    @JsonInclude(Include.NON_NULL)
    public String relationName;

    @JsonProperty("subject.relation.id")
    @JsonInclude(Include.NON_NULL)
    public String relationId;

    @JsonProperty("subject.content")
    @JsonInclude(Include.NON_NULL)
    public String contentName;

    @JsonProperty("search")
    @JsonInclude(Include.NON_NULL)
    Map<String, List<String>> queryParameters;

}
