package org.egov.bpa.calculator.web.models;

import java.util.List;

import org.egov.bpa.calculator.web.models.Installment.StatusEnum;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class InstallmentSearchCriteria {

	@JsonProperty("ids")
	private List<String> ids;

	@JsonProperty("tenantId")
	private String tenantId;

	@JsonProperty("installmentNos")
	private List<Integer> installmentNos;

	@JsonProperty("status")
	private StatusEnum status;

	@JsonProperty("consumerCode")
	private String consumerCode;

	@JsonProperty("taxHeadCode")
	private String taxHeadCode;

	@JsonProperty("demandId")
	private String demandId;

	@JsonProperty("isPaymentCompletedInDemand")
	private Boolean isPaymentCompletedInDemand;
	
	@JsonProperty("additionalDetails")
	private Object additionalDetails;

	@JsonProperty("offset")
	private Integer offset;

	@JsonProperty("limit")
	private Integer limit;

	@JsonProperty("fromDate")
	private Long fromDate;

	@JsonProperty("toDate")
	private Long toDate;

	@JsonIgnore
	private List<String> createdBy;

	public boolean isEmpty() {
		return (this.ids == null && this.tenantId == null && this.installmentNos == null && this.status == null
				&& this.consumerCode == null && this.taxHeadCode == null && this.demandId == null
				&& this.isPaymentCompletedInDemand == null && this.fromDate == null && this.toDate == null
				&& this.createdBy == null);
	}
}
