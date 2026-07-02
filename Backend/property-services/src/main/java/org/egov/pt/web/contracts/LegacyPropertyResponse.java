package org.egov.pt.web.contracts;

import java.util.List;

import org.egov.common.contract.response.ResponseInfo;
import org.egov.pt.models.Property;
import org.egov.pt.web.contracts.PropertyResponse.PropertyResponseBuilder;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@ToString
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LegacyPropertyResponse {
	
	@JsonProperty("ResponseInfo")
	  private ResponseInfo responseInfo;

	@JsonProperty("LegacyProperties")
	  private List<LegacyProperty> legacyProperty;

}
