package org.egov.edcr.contract.oc;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;


public class SubOccupancy {

	@JsonProperty("label")
	private String label;

	@JsonProperty("value")
	private String value;

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public SubOccupancy(String label, String value) {
		super();
		this.label = label;
		this.value = value;
	}

	public SubOccupancy() {
		super();
	}
	
	
	
}
