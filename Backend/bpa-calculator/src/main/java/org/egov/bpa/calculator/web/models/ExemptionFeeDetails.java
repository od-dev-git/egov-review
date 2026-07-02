package org.egov.bpa.calculator.web.models;

import java.math.BigDecimal;

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
public class ExemptionFeeDetails {

	@JsonProperty("feeType")
	private String feeType;

	@JsonProperty("calculationAmount")
	private BigDecimal calculationAmount;

	@JsonProperty("excemptionAmount")
	private BigDecimal excemptionAmount;

	@JsonProperty("totalAmount")
	private BigDecimal totalAmount;

	@JsonProperty("reason")
	private String reason;

}
