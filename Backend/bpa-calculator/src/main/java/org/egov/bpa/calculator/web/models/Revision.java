package org.egov.bpa.calculator.web.models;

import java.util.ArrayList;
import java.util.List;

import javax.validation.Valid;

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
public class Revision {

	@JsonProperty("id")
	private String id = null;

	@JsonProperty("tenantId")
	private String tenantId = null;

	@JsonProperty("isSujogExistingApplication")
	private boolean isSujogExistingApplication;

	@JsonProperty("bpaApplicationNo")
	private String bpaApplicationNo = null;
	
	@JsonProperty("bpaApplicationId")
	private String bpaApplicationId = null;
	
	@JsonProperty("refBpaApplicationNo")
	private String refBpaApplicationNo = null;
	
	@JsonProperty("refPermitNo")
	private String refPermitNo = null;

	@JsonProperty("refPermitDate")
	private Long refPermitDate = null;

	@JsonProperty("refPermitExpiryDate")
	private Long refPermitExpiryDate = null;

	@JsonProperty("refApplicationDetails")
	private Object refApplicationDetails = null;

	@JsonProperty("auditDetails")
	private AuditDetails auditDetails = null;

	@JsonProperty("documents")
	@Valid
	private List<Document> documents = null;

	public Revision addDocumentsItem(Document documentsItem) {
		if (this.documents == null) {
			this.documents = new ArrayList<Document>();
		}
		this.documents.add(documentsItem);
		return this;
	}
}
