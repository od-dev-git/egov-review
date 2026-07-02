package org.egov.bpa.web.model;

import java.math.BigDecimal;
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
public class NoticeSearchCriteria {
	
	@JsonProperty("ids")
	private List<String> ids;
	
	
	@JsonProperty("businessid")
	private String businessid;
	
	@JsonProperty("tenantId")
	private String tenantid;
	
	@JsonProperty("LetterNo")
	private String LetterNo;
	
	@JsonProperty("filestoreid")
	private String filestoreid;
	
	@JsonProperty("offset")
	private Integer offset;

	@JsonProperty("limit")
	private Integer limit;
	
	@JsonProperty("fromDate")
	private Long fromDate;

	@JsonProperty("toDate")
	private Long toDate;
	
	@JsonProperty("letterType")
	private String letterType;
	
	@JsonProperty("isClosed")
	private Boolean isClosed;
	
	@JsonProperty("createdBy")
	private String createdBy;
	
	@JsonProperty("bpaStatus")
	private List<String> bpaStatus;
	
	public boolean isEmpty() {
		return (this.ids == null && this.businessid == null && this.LetterNo == null && this.filestoreid == null &&
				 this.fromDate == null && this.toDate == null && this.letterType == null && this.isClosed == null && this.createdBy == null);
	}


	public void setTenantId(String tenantId) {
		this.tenantid=tenantId;
	}

	public void setTenantid(String tenantid) {
		this.tenantid=tenantid;
	}
}
