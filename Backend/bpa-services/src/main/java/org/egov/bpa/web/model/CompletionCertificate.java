package org.egov.bpa.web.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter	
public class CompletionCertificate {

	@JsonProperty("id")
	private String id = null;

	@JsonProperty("certificateNo")
	private String certificateNo = null;

	@JsonProperty("tenantId")
	private String tenantId = null;

	@JsonProperty("applicantName")
	private String applicantName = null;

	@JsonProperty("applicantAddress")
	private String applicantAddress = null;

	@JsonProperty("bpaPermitNumber")
	private String bpaPermitNumber = null;

	@JsonProperty("bpaPermitDate")
	private Long bpaPermitDate = null;

	@JsonProperty("plotNo")
	private String plotNo = null;

	@JsonProperty("khataNo")
	private String khataNo = null;

	@JsonProperty("mouza")
	private String mouza = null;
	
	@JsonProperty("mouzaName")
	private String mouzaName = null;

	@JsonProperty("architectName")
	private String architectName = null;

	@JsonProperty("pmoName")
	private String pmoName = null;

	@JsonProperty("architectAddress")
	private String architectAddress = null;

	@JsonProperty("phaseWiseCompletion")
	private String phaseWiseCompletion = null;

	@JsonProperty("completionDate")
	private Long completionDate = null;
	
	@JsonProperty("status")
	private String status = null;	
	
	@JsonProperty("completionFilestoreId")
	private String completionFilestoreId = null;	
	
	@JsonProperty("additionalDetails")
	private Object additionalDetails = null;

	@JsonProperty("auditDetails")
	private AuditDetails auditDetails = null;

}
