package org.egov.bpa.web.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FeePendingApplication {
	
	@JsonProperty("tenantId")
	private String tenantId;
	
	@JsonProperty("applicatioNo")
	private String applicatioNo;
	
	@JsonProperty("businessService")
	private String businessService;
	
	@JsonProperty("status")
	private String status;
	
	@JsonProperty("approvedDate")
	private Long approvedDate;
	
	@JsonProperty("daysSinceApproved")
	private Long daysSinceApproved;
	
	@JsonIgnore
	private String action;

}
