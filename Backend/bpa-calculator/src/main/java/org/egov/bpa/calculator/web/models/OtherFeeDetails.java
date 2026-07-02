package org.egov.bpa.calculator.web.models;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OtherFeeDetails {
	
	@JsonProperty("order")
	private int order;
	
	@JsonProperty("amount")
	private Object amount;
	
	@JsonProperty("reason")
	private String reason;
	
	@JsonProperty("comment")
	private String comment;
	
}
