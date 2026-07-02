package org.egov.bpa.calculator.web.models.demolition;

import java.util.ArrayList;
import java.util.List;

import javax.validation.Valid;

import org.egov.bpa.calculator.web.models.AuditDetails;
import org.egov.bpa.calculator.web.models.Document;
import org.egov.bpa.calculator.web.models.landinfo.OwnerInfo;
import org.egov.bpa.calculator.web.models.landinfo.Workflow;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.minidev.json.JSONObject;
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Demolition {

	@JsonProperty("id")
	@Default
	private String id = null;

	@JsonProperty("tenantId")
	@Default
	private String tenantId = null;

	@JsonProperty("applicationNo")
	@Default
	private String applicationNo = null;

	@JsonProperty("applicationDate")
	@Default
	private Long applicationDate = 0L;

	@JsonProperty("approvalDate")
	@Default
	private Long approvalDate = 0L;

	@JsonProperty("status")
	@Default
	private String status = null;

	@JsonProperty("accountId")
	@Default
	private String accountId = null;

	@JsonProperty("documents")
	@Default
	@Valid
	private List<Document> documents = null;

	@JsonProperty("workflow")
	@Default
	private Workflow workflow = null;

	@JsonProperty("landInfo")
	@Default
	private DemolitionLandInfo landInfo = null;

	@JsonProperty("ownershipCategory")
	@Default
	private String ownershipCategory = null;

	@JsonProperty("owners")
	@Valid
	@Default
	private List<OwnerInfo> owners = new ArrayList<OwnerInfo>();

	@JsonProperty("businessService")
	@Default
	private String businessService = null;

	@JsonProperty("approvalNo")
	@Default
	private String approvalNo = null;

	@JsonProperty("additionalDetails")
	@Default
	private Object additionalDetails = new JSONObject();

	@JsonProperty("plotDetails")
	@Default
	private Object plotDetails = new JSONObject();

	@JsonProperty("auditDetails")
	@Default
	private AuditDetails auditDetails = null;

	@JsonProperty("applicationType")
	@Default
	private String applicationType = null;

	@JsonProperty("serviceType")
	@Default
	private String serviceType = null;

	public Demolition addDocuments(Document document) {
		if (this.documents == null) {
			this.documents = new ArrayList<>();
		}

		if (null != document)
			this.documents.add(document);
		return this;
	}

	public Demolition addOwners(OwnerInfo ownerInfo) {
		if (this.owners == null) {
			this.owners = new ArrayList<>();
		}

		if (null != ownerInfo)
			this.owners.add(ownerInfo);
		return this;
	}

}
