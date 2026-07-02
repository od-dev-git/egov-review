package org.egov.bpa.calculator.web.models.regularization;

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
public class BlockSetback {

	@JsonProperty("name")
	private String name;
	
	@JsonProperty("asPerRecentNorms")
	private String asPerRecentNorms;
	
	@JsonProperty("asPerApprovalLetter")
	private String asPerApprovalLetter;
	
	@JsonProperty("asBuiltMeasurement")
	private String asBuiltMeasurement;
	
	@JsonProperty("deviation")
	private String deviation;
	
	@JsonProperty("status")
	private String status;
	
}
