package org.egov.bpa.calculator.web.models.regularization;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SubOcuupancy {
	
	@JsonProperty("label")
	private String label;
    
	@JsonProperty("value")
	private String value;
	
	@JsonProperty("totalBuiltUpArea")
	@Default
	private double totalBuiltUpArea = 0;
	
	@JsonProperty("totalAsBuiltBUA")
	@Default
	private double totalAsBuiltBUA = 0;
	
	@JsonProperty("totalApprovedBUA")
	@Default
	private double totalApprovedBUA = 0;
	
	@JsonProperty("totalAsBuiltCarpetArea")
	@Default
	private double totalAsBuiltCarpetArea = 0;
	
}
