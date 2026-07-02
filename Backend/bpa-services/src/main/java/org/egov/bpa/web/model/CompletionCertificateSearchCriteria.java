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
public class CompletionCertificateSearchCriteria {

	@JsonProperty("ids")
	private List<String> ids;

	@JsonProperty("certificateNo")
	private String certificateNo;

	@JsonProperty("tenantId")
	private String tenantId;

	@JsonProperty("applicantName")
	private String applicantName;

	@JsonProperty("bpaPermitNumber")
	private String bpaPermitNumber;

	@JsonProperty("bpaPermitDate")
	private Long bpaPermitDate;

	@JsonProperty("createdBy")
	private String createdBy;
	
	@JsonProperty("createdTime")
	private Long createdTime;
	
	@JsonProperty("offset")
	private Integer offset;

	@JsonProperty("limit")
	private Integer limit;
	
	@JsonProperty("fromDate")
    private Long fromDate;

    @JsonProperty("toDate")
    private Long toDate;

}
