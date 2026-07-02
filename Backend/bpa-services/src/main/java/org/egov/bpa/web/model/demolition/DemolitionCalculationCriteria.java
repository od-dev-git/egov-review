package org.egov.bpa.web.model.demolition;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

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
public class DemolitionCalculationCriteria {

	@JsonProperty("demolition")
	@Default
	private Demolition demolition = null;

	@JsonProperty("applicationNo")
	@Default
	@NotNull
	private String applicationNo = null;

	@JsonProperty("tenantId")
	@Default
	@NotNull
	@Size(min = 2, max = 256)
	private String tenantId = null;

	@JsonProperty("feeType")
	@Default
	private String feeType = null;

	@JsonProperty("applicationType")
	@Default
	private String applicationType = null;

}
