package org.egov.bpa.web.model.oc;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
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


}
