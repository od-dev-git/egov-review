package org.egov.demand.custom;

import java.math.BigDecimal;

import org.egov.common.contract.response.ResponseInfo;
import org.egov.demand.model.AuditDetails;
import org.egov.demand.model.BillDetailV2;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomBill {
	
    @JsonProperty("id")
	private String id;

	@JsonProperty("mobileNumber")
	private String mobileNumber;

	@JsonProperty("payerName")
	private String payerName;

	@JsonProperty("payerAddress")
	private String payerAddress;

	@JsonProperty("payerEmail")
	private String payerEmail;

	@JsonProperty("status")
	private String status;

	@JsonProperty("totalAmount")
	private BigDecimal totalAmount;
	
 	@JsonProperty("arrear")
	private BigDecimal arrear;
	
	@JsonProperty("currentAmount")
	private BigDecimal currentAmount;

	@JsonProperty("businessService")
	private String businessService;

	@JsonProperty("billNumber")
	private String billNumber;
	
	@JsonProperty("billDate")
	private Long billDate;
	
	@JsonProperty("billDueDate")
	private Long billDueDate;
	
	@JsonProperty("billBeforeDueDate")
	private BigDecimal billBeforeDueDate;
	
	@JsonProperty("billAfterDueDate")
	private BigDecimal billAfterDueDate;
	
	@JsonProperty("advanceAdjusted")
	private BigDecimal advanceAdjusted;

	@JsonProperty("consumerCode")
	private String consumerCode;

	@JsonProperty("additionalDetails")
	private JsonNode additionalDetails;

	@JsonProperty("tenantId")
	private String tenantId;

	@JsonProperty("fileStoreId")
	private String fileStoreId;

	@JsonProperty("auditDetails")
	private AuditDetails auditDetails;
	
    @JsonProperty("billDetail")
	private BillDetailV2 billDetail;

	@JsonProperty("paymentLink")
	private String paymentLink;

}
