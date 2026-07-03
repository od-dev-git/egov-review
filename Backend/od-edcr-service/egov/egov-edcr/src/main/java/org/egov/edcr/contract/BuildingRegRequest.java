package org.egov.edcr.contract;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.egov.infra.microservice.models.RequestInfo;

import java.util.ArrayList;
import java.util.LinkedHashMap;

public class BuildingRegRequest {

    @JsonProperty("Regularizations")
    private ArrayList<LinkedHashMap<String, Object>> regularizations;

    @JsonProperty("RequestInfo")
    private RequestInfo requestInfo;

    public ArrayList<LinkedHashMap<String, Object>> getRegularizations() {
        return regularizations;
    }

    public RequestInfo getRequestInfo() {
        return requestInfo;
    }

    public void setRegularizations(ArrayList<LinkedHashMap<String, Object>> regularizations) {
        this.regularizations = regularizations;
    }

    public void setRequestInfo(RequestInfo requestInfo) {
        this.requestInfo = requestInfo;
    }
}
