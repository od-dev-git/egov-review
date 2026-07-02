package org.egov.bpa.web.model.regularization;

import java.util.ArrayList;
import java.util.List;

import javax.validation.Valid;

import org.egov.bpa.web.model.AuditDetails;
import org.egov.bpa.web.model.Document;
import org.egov.bpa.web.model.Workflow;
import org.egov.bpa.web.model.landInfo.Institution;
import org.egov.bpa.web.model.landInfo.OwnerInfo;

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
public class Regularization {

	@JsonProperty("id")
	@Default
	private String id = null;

	@JsonProperty("tenantId")
	@Default
	private String tenantId = null;
	
	@JsonProperty("appType")
	@Default
	private AppType appType = null;

	@JsonProperty("applicationNo")
	@Default
	private String applicationNo = null;

	@JsonProperty("applicationDate")
	@Default
	private Long applicationDate = 0L;

	@JsonProperty("applicationType")
	@Default
	private String applicationType = null;

	@JsonProperty("approvalDate")
	@Default
	private Long approvalDate = 0L;

	@JsonProperty("status")
	@Default
	private String status = null;
	
	@JsonProperty("accountId")
	@Default
	private String accountId=null;

	@JsonProperty("documents")
	@Default
	@Valid
	private List<Document> documents = null;

	@JsonProperty("workflow")
	@Default
	private Workflow workflow = null;

	@JsonProperty("landRegularizationInfo")
	@Default
	private LandRegularizationInfo landRegularizationInfo = null;
	
	@JsonProperty("buildingRegularizationInfo")
	@Default
	private BuildingRegularizationInfo buildingRegularizationInfo = null;

	@JsonProperty("ownershipCategory")
	@Default
	private String ownershipCategory = null;

	@JsonProperty("owners")
	@Valid
	@Default
	private List<OwnerInfo> owners = new ArrayList<OwnerInfo>();

	@JsonProperty("institution")
	@Default
	private Institution institution = null;
	
	@JsonProperty("dscDetails")
	@Valid
	@Default
	private List<RegularizationDscDetails> dscDetails = null;

	@JsonProperty("serviceType")
	@Default
	private String serviceType = null;

	@JsonProperty("serviceSubType")
	@Default
	private String serviceSubType = null;

	@JsonProperty("businessService")
	@Default
	private String businessService = null;

	@JsonProperty("approvalNo")
	@Default
	private String approvalNo = null;

	@JsonProperty("permitExpiryDate")
	@Default
	private String permitExpiryDate = null;

	@JsonProperty("additionalDetails")
	@Default
	private Object additionalDetails = new JSONObject();

	@JsonProperty("auditDetails")
	@Default
	private AuditDetails auditDetails = null;
	
	@JsonProperty("plotNumbers")
	@Default
	private String plotNumbers=null;
	
	@JsonProperty("khataNumbers")
	@Default
	private String khataNumbers=null;
	
	@JsonProperty("villages")
	@Default
	private String villages=null;
	

	public Regularization addDocuments(Document document) {
        if (this.documents == null) {
            this.documents = new ArrayList<>();
        }

        if (null != document)
            this.documents.add(document);
        return this;
    }
	
	
	public Regularization addDscDetails(RegularizationDscDetails dscDetail) {
        if (this.dscDetails == null) {
            this.dscDetails = new ArrayList<>();
        }

        if (null != dscDetail)
            this.dscDetails.add(dscDetail);
        return this;
    }
	
	
	public Regularization addOwners(OwnerInfo ownerInfo) {
        if (this.owners == null) {
            this.owners = new ArrayList<>();
        }

        if (null != ownerInfo)
            this.owners.add(ownerInfo);
        return this;
    }
}
