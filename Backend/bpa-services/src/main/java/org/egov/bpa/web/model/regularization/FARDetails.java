package org.egov.bpa.web.model.regularization;

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
public class FARDetails {

	@JsonProperty("baseFar")
	@Default
	private String baseFar = null;

	@JsonProperty("maxPermissibleFar")
	@Default
	private String maxPermissibleFar = null;

	@JsonProperty("approvedFar")
	@Default
	private String approvedFar = null;

	@JsonProperty("asBuiltFar")
	@Default
	private String asBuiltFar = null;

	@JsonProperty("farStatus")
	@Default
	private String farStatus = null;
	
}
