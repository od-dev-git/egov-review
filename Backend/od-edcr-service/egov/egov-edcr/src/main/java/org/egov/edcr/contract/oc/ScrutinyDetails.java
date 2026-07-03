package org.egov.edcr.contract.oc;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.List;

public class ScrutinyDetails {

    @JsonProperty("id")
    String id;

    @JsonProperty("scrutinyType")
    String scrutinyType;

    @JsonProperty("outsidePermitNumber")
    String outsidePermitNumber;

    @JsonProperty("plotDetails")
    PlotDetails plotDetails;

    @JsonProperty("buildingBlockDetails")
    List<BuildingBlockDetails> buildingBlockDetails;

    @JsonProperty("totalFloorArea")
    BigDecimal totalFloorArea;

    @JsonProperty("totalBuiltUpArea")
    BigDecimal totalBuiltUpArea;

    @JsonProperty("totalCarpetArea")
    BigDecimal totalCarpetArea;

    @JsonProperty("totalEWSFeeEffectiveArea")
    BigDecimal totalEWSFeeEffectiveArea;

    @JsonProperty("benchmarkValuePerAcre")
    BigDecimal benchmarkValuePerAcre;

    @JsonProperty("baseFar")
    Double baseFar;

    @JsonProperty("providedFar")
    Double providedFar;

    @JsonProperty("maxPermissibleFar")
    Double maxPermissibleFar;

    @JsonProperty("approvedFar")
    Double approvedFar;

    @JsonProperty("totalNoOfDwellingUnits")
    long totalNoOfDwellingUnits;

    @JsonProperty("isShelterFeeRequired")
    boolean isShelterFeeRequired;

    @JsonProperty("isSecurityDepositRequired")
    boolean isSecurityDepositRequired;

    @JsonProperty("isRetentionFeeApplicable")
    boolean isRetentionFeeApplicable;

    @JsonProperty("projectValueForEIDP")
    BigDecimal projectValueForEIDP;

    @JsonProperty("isProjectUndertakingByGovt")
    String isProjectUndertakingByGovt;

    @JsonProperty("numberOfTemporaryStructures")
    BigDecimal numberOfTemporaryStructures;

    @JsonProperty("tdrFarRelaxation")
    Double tdrFarRelaxation;

    @JsonProperty("occupancyTypeHelperCode")
    String occupancyTypeHelperCode;

    @JsonProperty("occupancySubTypeHelperCode")
    String occupancySubTypeHelperCode;

    @JsonProperty("permitFee")
    List<Fee> permitFee;

    @JsonProperty("additionalDetails")
    Object additionalDetails;

	public ScrutinyDetails(String id, String scrutinyType, String outsidePermitNumber, PlotDetails plotDetails,
			List<BuildingBlockDetails> buildingBlockDetails, BigDecimal totalFloorArea, BigDecimal totalBuiltUpArea,
			BigDecimal totalCarpetArea, BigDecimal totalEWSFeeEffectiveArea, BigDecimal benchmarkValuePerAcre,
			Double baseFar, Double providedFar, Double maxPermissibleFar, Double approvedFar,
			long totalNoOfDwellingUnits, boolean isShelterFeeRequired, boolean isSecurityDepositRequired,
			boolean isRetentionFeeApplicable, BigDecimal projectValueForEIDP, String isProjectUndertakingByGovt,
			BigDecimal numberOfTemporaryStructures, Double tdrFarRelaxation, String occupancyTypeHelperCode,
			String occupancySubTypeHelperCode, List<Fee> permitFee, Object additionalDetails) {
		super();
		this.id = id;
		this.scrutinyType = scrutinyType;
		this.outsidePermitNumber = outsidePermitNumber;
		this.plotDetails = plotDetails;
		this.buildingBlockDetails = buildingBlockDetails;
		this.totalFloorArea = totalFloorArea;
		this.totalBuiltUpArea = totalBuiltUpArea;
		this.totalCarpetArea = totalCarpetArea;
		this.totalEWSFeeEffectiveArea = totalEWSFeeEffectiveArea;
		this.benchmarkValuePerAcre = benchmarkValuePerAcre;
		this.baseFar = baseFar;
		this.providedFar = providedFar;
		this.maxPermissibleFar = maxPermissibleFar;
		this.approvedFar = approvedFar;
		this.totalNoOfDwellingUnits = totalNoOfDwellingUnits;
		this.isShelterFeeRequired = isShelterFeeRequired;
		this.isSecurityDepositRequired = isSecurityDepositRequired;
		this.isRetentionFeeApplicable = isRetentionFeeApplicable;
		this.projectValueForEIDP = projectValueForEIDP;
		this.isProjectUndertakingByGovt = isProjectUndertakingByGovt;
		this.numberOfTemporaryStructures = numberOfTemporaryStructures;
		this.tdrFarRelaxation = tdrFarRelaxation;
		this.occupancyTypeHelperCode = occupancyTypeHelperCode;
		this.occupancySubTypeHelperCode = occupancySubTypeHelperCode;
		this.permitFee = permitFee;
		this.additionalDetails = additionalDetails;
	}

	public ScrutinyDetails() {
		super();
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getScrutinyType() {
		return scrutinyType;
	}

	public void setScrutinyType(String scrutinyType) {
		this.scrutinyType = scrutinyType;
	}

	public String getOutsidePermitNumber() {
		return outsidePermitNumber;
	}

	public void setOutsidePermitNumber(String outsidePermitNumber) {
		this.outsidePermitNumber = outsidePermitNumber;
	}

	public PlotDetails getPlotDetails() {
		return plotDetails;
	}

	public void setPlotDetails(PlotDetails plotDetails) {
		this.plotDetails = plotDetails;
	}

	public List<BuildingBlockDetails> getBuildingBlockDetails() {
		return buildingBlockDetails;
	}

	public void setBuildingBlockDetails(List<BuildingBlockDetails> buildingBlockDetails) {
		this.buildingBlockDetails = buildingBlockDetails;
	}

	public BigDecimal getTotalFloorArea() {
		return totalFloorArea;
	}

	public void setTotalFloorArea(BigDecimal totalFloorArea) {
		this.totalFloorArea = totalFloorArea;
	}

	public BigDecimal getTotalBuiltUpArea() {
		return totalBuiltUpArea;
	}

	public void setTotalBuiltUpArea(BigDecimal totalBuiltUpArea) {
		this.totalBuiltUpArea = totalBuiltUpArea;
	}

	public BigDecimal getTotalCarpetArea() {
		return totalCarpetArea;
	}

	public void setTotalCarpetArea(BigDecimal totalCarpetArea) {
		this.totalCarpetArea = totalCarpetArea;
	}

	public BigDecimal getTotalEWSFeeEffectiveArea() {
		return totalEWSFeeEffectiveArea;
	}

	public void setTotalEWSFeeEffectiveArea(BigDecimal totalEWSFeeEffectiveArea) {
		this.totalEWSFeeEffectiveArea = totalEWSFeeEffectiveArea;
	}

	public BigDecimal getBenchmarkValuePerAcre() {
		return benchmarkValuePerAcre;
	}

	public void setBenchmarkValuePerAcre(BigDecimal benchmarkValuePerAcre) {
		this.benchmarkValuePerAcre = benchmarkValuePerAcre;
	}

	public Double getBaseFar() {
		return baseFar;
	}

	public void setBaseFar(Double baseFar) {
		this.baseFar = baseFar;
	}

	public Double getProvidedFar() {
		return providedFar;
	}

	public void setProvidedFar(Double providedFar) {
		this.providedFar = providedFar;
	}

	public Double getMaxPermissibleFar() {
		return maxPermissibleFar;
	}

	public void setMaxPermissibleFar(Double maxPermissibleFar) {
		this.maxPermissibleFar = maxPermissibleFar;
	}

	public Double getApprovedFar() {
		return approvedFar;
	}

	public void setApprovedFar(Double approvedFar) {
		this.approvedFar = approvedFar;
	}

	public long getTotalNoOfDwellingUnits() {
		return totalNoOfDwellingUnits;
	}

	public void setTotalNoOfDwellingUnits(long totalNoOfDwellingUnits) {
		this.totalNoOfDwellingUnits = totalNoOfDwellingUnits;
	}

	public boolean isShelterFeeRequired() {
		return isShelterFeeRequired;
	}

	public void setShelterFeeRequired(boolean isShelterFeeRequired) {
		this.isShelterFeeRequired = isShelterFeeRequired;
	}

	public boolean isSecurityDepositRequired() {
		return isSecurityDepositRequired;
	}

	public void setSecurityDepositRequired(boolean isSecurityDepositRequired) {
		this.isSecurityDepositRequired = isSecurityDepositRequired;
	}

	public boolean isRetentionFeeApplicable() {
		return isRetentionFeeApplicable;
	}

	public void setRetentionFeeApplicable(boolean isRetentionFeeApplicable) {
		this.isRetentionFeeApplicable = isRetentionFeeApplicable;
	}

	public BigDecimal getProjectValueForEIDP() {
		return projectValueForEIDP;
	}

	public void setProjectValueForEIDP(BigDecimal projectValueForEIDP) {
		this.projectValueForEIDP = projectValueForEIDP;
	}

	public String getIsProjectUndertakingByGovt() {
		return isProjectUndertakingByGovt;
	}

	public void setIsProjectUndertakingByGovt(String isProjectUndertakingByGovt) {
		this.isProjectUndertakingByGovt = isProjectUndertakingByGovt;
	}

	public BigDecimal getNumberOfTemporaryStructures() {
		return numberOfTemporaryStructures;
	}

	public void setNumberOfTemporaryStructures(BigDecimal numberOfTemporaryStructures) {
		this.numberOfTemporaryStructures = numberOfTemporaryStructures;
	}

	public Double getTdrFarRelaxation() {
		return tdrFarRelaxation;
	}

	public void setTdrFarRelaxation(Double tdrFarRelaxation) {
		this.tdrFarRelaxation = tdrFarRelaxation;
	}

	public String getOccupancyTypeHelperCode() {
		return occupancyTypeHelperCode;
	}

	public void setOccupancyTypeHelperCode(String occupancyTypeHelperCode) {
		this.occupancyTypeHelperCode = occupancyTypeHelperCode;
	}

	public String getOccupancySubTypeHelperCode() {
		return occupancySubTypeHelperCode;
	}

	public void setOccupancySubTypeHelperCode(String occupancySubTypeHelperCode) {
		this.occupancySubTypeHelperCode = occupancySubTypeHelperCode;
	}

	public List<Fee> getPermitFee() {
		return permitFee;
	}

	public void setPermitFee(List<Fee> permitFee) {
		this.permitFee = permitFee;
	}

	public Object getAdditionalDetails() {
		return additionalDetails;
	}

	public void setAdditionalDetails(Object additionalDetails) {
		this.additionalDetails = additionalDetails;
	}
    
    


}
