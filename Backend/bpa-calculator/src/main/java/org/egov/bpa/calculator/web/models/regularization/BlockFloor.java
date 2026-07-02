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
public class BlockFloor {

	@JsonProperty("floorNumber")
	private String floorNumber;
	
	@JsonProperty("floorType")
	private String floorType;
	
	@JsonProperty("subOcuupancy")
	private SubOcuupancy subOcuupancy;
	
	@JsonProperty("asBuiltBUA")
	private String asBuiltBUA;
	
	@JsonProperty("approvedBUA")
	private String approvedBUA;
	
	@JsonProperty("asBuiltFARArea")
	private String asBuiltFARArea;
	
	@JsonProperty("asBuiltCarpetArea")
	private String asBuiltCarpetArea;
	
}
