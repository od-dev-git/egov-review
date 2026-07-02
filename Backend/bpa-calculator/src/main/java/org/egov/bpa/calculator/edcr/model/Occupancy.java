package org.egov.bpa.calculator.edcr.model;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.validation.annotation.Validated;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Validated
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class Occupancy {
	

	@JsonProperty("type")
	private String type = null;
	
	
	
	@JsonProperty("typeHelper")
	private OccupancytypeHelper typeHelper = null;

	
	
	
	@JsonProperty("deduction")
	private Double deduction = null;
	
	@JsonProperty("builtUpArea")
	private Double builtUpArea = null;
	
	
	@JsonProperty("floorArea")
	private Double floorArea = null;
	
	@JsonProperty("carpetArea")
	private Double carpetArea = null;
	
	@JsonProperty("carpetAreaDeduction")
	private Double carpetAreaDeduction = null;
	
	@JsonProperty("existingBuiltUpArea")
	private Double existingBuiltUpArea = null;
	
	@JsonProperty("existingFloorArea")
	private Double existingFloorArea = null;
	
	@JsonProperty("proposedFloorArea")
	private Double proposedFloorArea = null;
	
	
	@JsonProperty("existingCarpetArea")
	private Double existingCarpetArea = null;
	
	
	@JsonProperty("existingCarpetAreaDeduction")
	private Double existingCarpetAreaDeduction = null;
	
	
	@JsonProperty("existingDeduction")
	private Double existingDeduction = null;
	
	
	@JsonProperty("subOccupancyCode")
	private String subOccupancyCode = null;
	
	
	@JsonProperty("OccupancyCode")
	private String OccupancyCode = null;
	
	

	
	
	


}
