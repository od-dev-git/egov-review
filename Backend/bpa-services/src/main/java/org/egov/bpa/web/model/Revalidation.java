package org.egov.bpa.web.model;

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
public class Revalidation {

	@JsonProperty("id")
	private String id = null;

	@JsonProperty("tenantId")
	private String tenantId = null;

	@JsonProperty("isSujogExistingApplication")
	private boolean isSujogExistingApplication = Boolean.TRUE;

	@JsonProperty("bpaApplicationNo")
	private String bpaApplicationNo = null;

	@JsonProperty("bpaApplicationId")
	private String bpaApplicationId = null;

	@JsonProperty("refBpaApplicationNo")
	private String refBpaApplicationNo = null;

	@JsonProperty("permitNo")
	private String permitNo = null;

	@JsonProperty("permitDate")
	private Long permitDate = null;

	@JsonProperty("permitExpiryDate")
	private Long permitExpiryDate = null;

	@JsonProperty("refApplicationDetails")
	private Object refApplicationDetails = null;

	@JsonProperty("auditDetails")
	private AuditDetails auditDetails = null;

	@JsonProperty("isConstructionPresent")
	private boolean isConstructionPresent = Boolean.FALSE;

	@JsonProperty("isConstructionAsPerApprovedPlan")
	private boolean isConstructionAsPerApprovedPlan = Boolean.TRUE;

	@JsonProperty("documents")
	@Valid
	private List<Document> documents = null;

	public Revalidation addDocumentsItem(Document documentsItem) {
		if (this.documents == null) {
			this.documents = new ArrayList<Document>();
		}
		this.documents.add(documentsItem);
		return this;
	}
}
