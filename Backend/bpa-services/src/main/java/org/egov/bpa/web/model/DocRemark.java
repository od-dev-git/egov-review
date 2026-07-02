package org.egov.bpa.web.model;

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
public class DocRemark {

	@JsonProperty("id")
	private String id = null;

	@JsonProperty("documentCode")
	private String documentCode = null;

	@JsonProperty("businessId")
	private String businessId = null;

	@JsonProperty("additionalDetails")
	private Object additionalDetails = null;

	@JsonProperty("auditDetails")
	private AuditDetails auditDetails = null;

	@JsonProperty("isUpdatable")
	private Boolean isUpdatable = Boolean.FALSE;
	
	@JsonProperty("docCommentCount")
	private int docCommentCount = 0;

}
