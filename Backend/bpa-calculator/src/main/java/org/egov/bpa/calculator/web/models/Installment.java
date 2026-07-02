package org.egov.bpa.calculator.web.models;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Installment {

	@JsonProperty("id")
	private String id;

	@JsonProperty("tenantId")
	private String tenantId;
	
	@JsonProperty("installmentNo")
	private int installmentNo;

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
