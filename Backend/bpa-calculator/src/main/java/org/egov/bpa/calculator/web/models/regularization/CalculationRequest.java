package org.egov.bpa.calculator.web.models.regularization;

import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.egov.common.contract.request.RequestInfo;

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
public class CalculationRequest {
	
	@JsonProperty("RequestInfo")
	@NotNull
	@Valid
	@Default
	private RequestInfo requestInfo = null;

	@JsonProperty("CalculationCriteria")
	@Valid
	@Default
	private List<RegularizationCalculationCriteria> calculationCriteria = null;

}
