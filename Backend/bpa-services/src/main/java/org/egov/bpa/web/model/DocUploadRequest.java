package org.egov.bpa.web.model;

import java.util.List;

import javax.validation.Valid;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Builder
public class DocUploadRequest {

	@JsonProperty("tenantId")
	private String tenantId;

	@JsonProperty("applicationNo")
	private String applicationNo;
	
	@JsonProperty("docUploadType")
	private String docUploadType;

	@JsonProperty("documents")
	private List<DocumentList> documents;

	@JsonProperty("auditDetails")
	private AuditDetails auditDetails = null;

}
