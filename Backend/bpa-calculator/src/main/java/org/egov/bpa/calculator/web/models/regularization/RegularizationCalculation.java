package org.egov.bpa.calculator.web.models.regularization;

import java.util.List;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.egov.bpa.calculator.web.models.demand.TaxHeadEstimate;

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
public class RegularizationCalculation {
	
	@Default
	@JsonProperty("applicationNumber")
	private String applicationNumber = null;

	@Default
	@JsonProperty("regularization")
	private Regularization regularization = null;

	@NotNull
	@Default
	@JsonProperty("tenantId")
	@Size(min = 2, max = 256)
	private String tenantId = null;

	@JsonProperty("taxHeadEstimates")
	List<TaxHeadEstimate> taxHeadEstimates;
	
	@Default
	@JsonProperty("feeType")
	String feeType = null;

}
