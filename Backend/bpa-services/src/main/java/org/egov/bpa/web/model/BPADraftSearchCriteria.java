package org.egov.bpa.web.model;

import java.util.List;

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
public class BPADraftSearchCriteria {

	@JsonProperty("ids")
	private List<String> ids;

	@JsonProperty("edcrNo")
	private String edcrNo;

	@JsonProperty("tenantId")
	private String tenantId;
	
	@JsonProperty("status")
	private String status;
	
	@JsonProperty("createdBy")
	private String createdBy;
	
	@JsonProperty("offset")
    private Integer offset;

    @JsonProperty("limit")
    private Integer limit; 

}
