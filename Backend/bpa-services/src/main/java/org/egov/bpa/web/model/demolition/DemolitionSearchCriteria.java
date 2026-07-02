package org.egov.bpa.web.model.demolition;

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
public class DemolitionSearchCriteria {

	@JsonProperty("ids")
	private List<String> ids;

	@JsonProperty("tenantId")
	private String tenantId;

	@JsonProperty("applicationNo")
	private String applicationNo;

	@JsonProperty("applicationDate")
	private Long applicationDate;
	
	@JsonProperty("approvalNo")
	private String approvalNo;

	@JsonProperty("approvalDate")
	private Long approvalDate;

	@JsonProperty("status")
	private String status;

	@JsonProperty("accountId")
	private String accountId;

	@JsonProperty("businessService")
	private String businessService;
	
	@JsonProperty("mobileNumber")
	private String mobileNumber;
	
	@JsonProperty("createdBy")
	private List<String> createdBy;
	
	@JsonIgnore
    private List<String> ownerIds;
	
	private Integer limit;
	
	private Integer offset;
	
	public boolean isEmpty() {
		return (this.tenantId == null && this.status == null && this.ids == null && this.applicationNo == null
				&& this.mobileNumber == null && this.approvalNo == null && this.approvalDate == null
				&& this.ownerIds == null && this.businessService == null);
	}

	public boolean tenantIdOnly() {
		return (this.tenantId != null && this.status == null && this.ids == null && this.applicationNo == null
				&& this.mobileNumber == null && this.approvalNo == null && this.approvalDate == null
				&& this.ownerIds == null && this.businessService == null);
	}

}
