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
public class BuildingOtherDetails {

	@JsonProperty("hasProjectProvidedMin10PercentBUAForEWSWithin5KmFromProjectSite")
	private boolean hasProjectProvidedMin10PercentBUAForEWSWithin5KmFromProjectSite;
	
	@JsonProperty("numberOfTemporaryStructures")
	@Default
	private String numberOfTemporaryStructures = null;

	@JsonProperty("projectValueIfEIDPFeeApplicableForProject")
	@Default
	private String projectValueIfEIDPFeeApplicableForProject = null;

	@JsonProperty("totalNoOfDwellingUnits")
	@Default
	private String totalNoOfDwellingUnits = null;
	
	@JsonProperty("isShelterFeeApplicable")
	private boolean isShelterFeeApplicable;
	
	@JsonProperty("effectiveEWSArea")
	@Default
	private String effectiveEWSArea = null;

	@JsonProperty("isSecurityDepositRequired")
	private boolean isSecurityDepositRequired;
	
	@JsonProperty("tdrFarRelaxation")
	@Default
	private String tdrFarRelaxation = null;
	
	@JsonProperty("farFeeRate")
	@Default
	private String farFeeRate = null;
	
	@JsonProperty("unauthorizedSBWithinNormsRate")
	@Default
	private String unauthorizedSBWithinNormsRate = null;
	
	@JsonProperty("unauthorizedSBBNUnder5Rate")
	@Default
	private String unauthorizedSBBNUnder5Rate = null;
	
	@JsonProperty("unauthorizedSBBNUnder10Rate")
	@Default
	private String unauthorizedSBBNUnder10Rate = null;
	
	@JsonProperty("isEIDPFeeApplicable")
	private boolean  isEIDPFeeApplicable;

	@JsonProperty("isCWWCFeeApplicable")
	private boolean  isCWWCFeeApplicable;
	
}
