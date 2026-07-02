package org.egov.bpa.web.model;

import java.util.ArrayList;
import java.util.List;
import javax.validation.Valid;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class PlinthApproval {

	@JsonProperty("id")
	private String id = null;

	@JsonProperty("applicationNo")
	private String applicationNo = null;

	@JsonProperty("tenantId")
	private String tenantId = null;
	
	@JsonProperty("bpaApplicationNo")
	private String bpaApplicationNo = null;

	@JsonProperty("declarationDetails")
	private Object declarationDetails = null;

	@JsonProperty("accreditedPersonDetails")
	private Object accreditedPersonDetails = null;

	@JsonProperty("pmoDetails")
	private Object pmoDetails = null;

	@JsonProperty("additionalDetails")
	private Object additionalDetails = null;

	@JsonProperty("status")
	private String status = null;

	@JsonProperty("bpaApprover")
	private String bpaApprover = null;
	
	@JsonProperty("approvalNo")
	private String approvalNo = null;

	@JsonProperty("documents")
	@Valid
	private List<Document> documents = null;

	@JsonProperty("auditDetails")
	private AuditDetails auditDetails = null;
	
	public PlinthApproval documents(List<Document> documents) {
		this.documents = documents;
		return this;
	}

	public PlinthApproval addDocumentsItem(Document documentsItem) {
		if (this.documents == null) {
			this.documents = new ArrayList<Document>();
		}
		this.documents.add(documentsItem);
		return this;
	}

	/**
	 * The documents attached by owner for exemption.
	 * 
	 * @return documents
	 **/
	@ApiModelProperty(value = "The documents attached by owner for exemption.")
	@Valid
	public List<Document> getDocuments() {
		return documents;
	}

	public void setDocuments(List<Document> documents) {
		this.documents = documents;
	}


}
