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
public class BUADetails {

	@JsonProperty("totalProvidedBUA")
	@Default
	private String totalProvidedBUA = null;

	@JsonProperty("totalApprovedBUA")
	@Default
	private String totalApprovedBUA = null;

	@JsonProperty("totalUnauthorizedBUA")
	@Default
	private String totalUnauthorizedBUA = null;

	@JsonProperty("totalUnauthAreaonSBWithinNorms")
	@Default
	private String totalUnauthAreaonSBWithinNorms = null;

	@JsonProperty("totalUnauthAreaonSBBeyondNormsButUnder5")
	@Default
	private String totalUnauthAreaonSBBeyondNormsButUnder5 = null;

	@JsonProperty("totalUnauthAreaonSBBeyondNormsButUnder10")
	@Default
	private String totalUnauthAreaonSBBeyondNormsButUnder10 = null;

}
