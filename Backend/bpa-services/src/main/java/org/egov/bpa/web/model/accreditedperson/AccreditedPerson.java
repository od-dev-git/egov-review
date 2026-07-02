package org.egov.bpa.web.model.accreditedperson;

import org.egov.bpa.web.model.AuditDetails;
import org.springframework.validation.annotation.Validated;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Validated
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class AccreditedPerson {

	@JsonProperty("id")
	private String id = null;

	@JsonProperty("userUUID")
	private String userUUID = null;

	@JsonProperty("userId")
	private Long userId = null;
	
	@JsonProperty("userRole")
	private String userRole = null;
	
	@JsonProperty("tenant")
	private String tenant = null;

	@JsonProperty("personName")
	private String personName = null;

	@JsonProperty("firmName")
	private String firmName = null;

	@JsonProperty("accreditationNo")
	private String accreditationNo = null;

	@JsonProperty("certificateIssueDate")
	private Long certificateIssueDate = null;

	@JsonProperty("validTill")
	private Long validTill = null;

	@JsonProperty("auditDetails")
	private AuditDetails auditDetails = null;

}
