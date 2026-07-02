package org.egov.pt.calculator.web.models;

import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.egov.common.contract.request.RequestInfo;
import org.egov.pt.calculator.web.models.demand.Demand;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class MigrationAssessmentReq {

	@JsonProperty("RequestInfo")
	@NotNull
    private RequestInfo requestInfo;

    @Valid
    @NotNull
    @JsonProperty("CalculationCriteria")
    private List<CalculationCriteria> calculationCriteria;
    
    private List<Demand> demands;

}
