package org.egov.pt.web.contracts;

import org.egov.common.contract.response.ResponseInfo;
import org.egov.pt.web.contracts.LegacyPropertyResponse.LegacyPropertyResponseBuilder;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.util.Json;
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
public class LegacyProperty {
	
	@JsonProperty("TenantId")
	private String tenantId;
	
	@JsonProperty("OldPropertyId")
	private String oldPropertyId;
	
	@JsonProperty("PropertyId")
	private String propertyId;
	
	@JsonProperty("LegacyPropertyId")
	private String legacyPropertyId;

}
