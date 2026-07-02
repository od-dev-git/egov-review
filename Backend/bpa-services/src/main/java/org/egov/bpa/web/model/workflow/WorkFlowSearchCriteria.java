package org.egov.bpa.web.model.workflow;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkFlowSearchCriteria {

	@JsonProperty("tenantId")
	private String tenantId=null;

	@JsonProperty("id")
	private String id=null;

	@JsonProperty("businessId")
	private String businessId=null;

}