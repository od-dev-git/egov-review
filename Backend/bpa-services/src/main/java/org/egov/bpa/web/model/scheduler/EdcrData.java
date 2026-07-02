package org.egov.bpa.web.model.scheduler;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class EdcrData {
	@JsonProperty("id")
	private String id;
	
	@JsonProperty("applicationId")
	private String applicationId;
	
	@JsonProperty("applicationNo")
	private String applicationNo;
	
	@JsonProperty("mauza")
	private String mauza;
	
	@JsonProperty("riskType")
	private String riskType;
	
	@JsonProperty("serviceType")
	private String serviceType;
	
	@JsonProperty("plotNumber")
	private String plotNumber;
	
	@JsonProperty("isBUAAbove500")
	private Boolean isBUAAbove500;
	
	@JsonProperty("workflowName")
	private String workflowName;
	
	@JsonProperty("totalBuiltupArea")
	private String totalBuiltupArea;
	
	@JsonProperty("totalPlotArea")
	private String totalPlotArea;
	
	@JsonProperty("maxBuildingHeight")
	private String maxBuildingHeight;
	
	@JsonProperty("alterationSubService")
	private String alterationSubService;
	
	@JsonProperty("liftCountRelaxation")
	private String liftCountRelaxation;
	
	@JsonProperty("occupancyType")
	private String occupancyType;
	
	@JsonProperty("basementPresent")
	private Boolean basementPresent;
	
	@JsonProperty("affordableUnitsPresent")
	private String affordableUnitsPresent;
	
	@JsonProperty("noOfDwellingUnits")
	private String noOfDwellingUnits;
	
	@JsonProperty("publicWashRoomProvided")
	private Boolean publicWashRoomProvided;
	
	@JsonProperty("noOfFloors")
	private String noOfFloors;
	
	@JsonProperty("approachRoadWidth")
	private String approachRoadWidth;
	
	@JsonProperty("proposedFar")
	private String proposedFar;
	
	@JsonProperty("correlationId")
	private String correlationId;
	
	@JsonProperty("noOfStorey")
	private String noOfStorey;
	
	@JsonProperty("subOccupancy")
	private String subOccupancy;
}
