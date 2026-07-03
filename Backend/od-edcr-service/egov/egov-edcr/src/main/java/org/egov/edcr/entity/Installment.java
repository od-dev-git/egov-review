package org.egov.edcr.entity;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

public class Installment {
	
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getTenantId() {
		return tenantId;
	}

	public void setTenantId(String tenantId) {
		this.tenantId = tenantId;
	}

	public int getInstallmentNo() {
		return installmentNo;
	}

	public void setInstallmentNo(int installmentNo) {
		this.installmentNo = installmentNo;
	}

	public StatusEnum getStatus() {
		return status;
	}

	public void setStatus(StatusEnum status) {
		this.status = status;
	}

	public String getConsumerCode() {
		return consumerCode;
	}

	public void setConsumerCode(String consumerCode) {
		this.consumerCode = consumerCode;
	}

	public String getTaxHeadCode() {
		return taxHeadCode;
	}

	public void setTaxHeadCode(String taxHeadCode) {
		this.taxHeadCode = taxHeadCode;
	}

	public BigDecimal getTaxAmount() {
		return taxAmount;
	}

	public void setTaxAmount(BigDecimal taxAmount) {
		this.taxAmount = taxAmount;
	}

	public String getDemandId() {
		return demandId;
	}

	public void setDemandId(String demandId) {
		this.demandId = demandId;
	}

	public boolean isPaymentCompletedInDemand() {
		return isPaymentCompletedInDemand;
	}

	public void setPaymentCompletedInDemand(boolean isPaymentCompletedInDemand) {
		this.isPaymentCompletedInDemand = isPaymentCompletedInDemand;
	}

	public AuditDetails getAuditDetails() {
		return auditDetails;
	}

	public void setAuditDetails(AuditDetails auditDetails) {
		this.auditDetails = auditDetails;
	}

	public Object getAdditionalDetails() {
		return additionalDetails;
	}

	public void setAdditionalDetails(Object additionalDetails) {
		this.additionalDetails = additionalDetails;
	}

	@JsonProperty("id")
	private String id;

	@JsonProperty("tenantId")
	private String tenantId;
	
	@JsonProperty("installmentNo")
	private int installmentNo;

	public Installment(String id, String tenantId, int installmentNo, StatusEnum status, String consumerCode,
			String taxHeadCode, BigDecimal taxAmount, String demandId, boolean isPaymentCompletedInDemand,
			AuditDetails auditDetails, Object additionalDetails) {
		this.id = id;
		this.tenantId = tenantId;
		this.installmentNo = installmentNo;
		this.status = status;
		this.consumerCode = consumerCode;
		this.taxHeadCode = taxHeadCode;
		this.taxAmount = taxAmount;
		this.demandId = demandId;
		this.isPaymentCompletedInDemand = isPaymentCompletedInDemand;
		this.auditDetails = auditDetails;
		this.additionalDetails = additionalDetails;
	}

	public Installment() {
		// TODO Auto-generated constructor stub
	}

	/**
	 * Gets or Sets status
	 */
	public enum StatusEnum {

		ACTIVE("ACTIVE"),

		CANCELLED("CANCELLED"),

		ADJUSTED("ADJUSTED");

		private String value;

		StatusEnum(String value) {
			this.value = value;
		}

		@Override
		@JsonValue
		public String toString() {
			return String.valueOf(value);
		}

		@JsonCreator
		public static StatusEnum fromValue(String text) {
			for (StatusEnum b : StatusEnum.values()) {
				if (String.valueOf(b.value).equalsIgnoreCase(text)) {
					return b;
				}
			}
			return null;
		}
	}

	@JsonProperty("status")
	private StatusEnum status;

	@JsonProperty("consumerCode")
	private String consumerCode;

	@JsonProperty("taxHeadCode")
	private String taxHeadCode;

	@JsonProperty("taxAmount")
	private BigDecimal taxAmount;
	
	@JsonProperty("demandId")
	private String demandId;
	
	@JsonProperty("isPaymentCompletedInDemand")
	private boolean isPaymentCompletedInDemand;

	@JsonProperty("auditDetails")
	private AuditDetails auditDetails;

	@JsonProperty("additionalDetails")
	private Object additionalDetails;

}
