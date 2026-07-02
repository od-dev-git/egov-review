package org.egov.pt.models;

import java.util.ArrayList;
import java.util.List;

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
public class BulkAssesmentCreationCriteria {

	@JsonProperty("tenantIds")
	private List<String> tenantIds;

	@JsonProperty("offset")
	private Long offset;

	@JsonProperty("limit")
	private Long limit;
	
	@JsonProperty(value = "skipTenantIds")
	private List<String> skipTenantIds;
	
	@JsonProperty(value = "financialYear")
	private String financialYear;
	
	@JsonProperty(value = "propertyIds")
	private List<String> propertyIds = new ArrayList<>();
}
