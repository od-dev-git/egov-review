package org.egov.edcr.contract.oc;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.validation.annotation.Validated;


public class Setback {

    @JsonProperty("name")
    String name;

    @JsonProperty("asPerRecentNorms")
    String asPerRecentNorms;

    @JsonProperty("asPerApprovalLetter")
    String asPerApprovalLetter;

    @JsonProperty("asBuiltMeasurement")
    String asBuiltMeasurement;

    @JsonProperty("deviation")
    String deviation;

    @JsonProperty("status")
    String status;

	public Setback(String name, String asPerRecentNorms, String asPerApprovalLetter, String asBuiltMeasurement,
			String deviation, String status) {
		super();
		this.name = name;
		this.asPerRecentNorms = asPerRecentNorms;
		this.asPerApprovalLetter = asPerApprovalLetter;
		this.asBuiltMeasurement = asBuiltMeasurement;
		this.deviation = deviation;
		this.status = status;
	}

	public Setback() {
		super();
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getAsPerRecentNorms() {
		return asPerRecentNorms;
	}

	public void setAsPerRecentNorms(String asPerRecentNorms) {
		this.asPerRecentNorms = asPerRecentNorms;
	}

	public String getAsPerApprovalLetter() {
		return asPerApprovalLetter;
	}

	public void setAsPerApprovalLetter(String asPerApprovalLetter) {
		this.asPerApprovalLetter = asPerApprovalLetter;
	}

	public String getAsBuiltMeasurement() {
		return asBuiltMeasurement;
	}

	public void setAsBuiltMeasurement(String asBuiltMeasurement) {
		this.asBuiltMeasurement = asBuiltMeasurement;
	}

	public String getDeviation() {
		return deviation;
	}

	public void setDeviation(String deviation) {
		this.deviation = deviation;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}
    
    


}
