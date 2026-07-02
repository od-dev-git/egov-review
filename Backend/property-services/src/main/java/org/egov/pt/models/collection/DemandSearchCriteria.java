package org.egov.pt.models.collection;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DemandSearchCriteria {
	
	@JsonProperty("tenantId")
	private String tenantId=null;
	
	@JsonProperty("id")
	private String id=null;
	
	@JsonProperty("consumerCode")
	private String consumerCode=null;
	
	@JsonProperty("businessService")
	private String businessService=null;
	
	@JsonProperty("status")
	private String status=null;

}
