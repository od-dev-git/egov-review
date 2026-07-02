package org.egov.bpa.calculator.web.models.regularization;

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
public class CalculateFeeInfo {
	
	@JsonProperty("totalBuiltUpArea")
	private Double totalBuiltUpArea;
	
	@JsonProperty("minCalculationPoint")
	private Integer minCalculationPoint;
	
	@JsonProperty("maxCalculationPoint")
	private Integer maxCalculationPoint;
	
	@JsonProperty("minimumFee")
	private BigDecimal minimumFee;
	
	@JsonProperty("inBetweenFeePerUnit")
	private BigDecimal inBetweenFeePerUnit;
	
	@JsonProperty("maximumFeePerUnit")
	private BigDecimal maximumFeePerUnit;
	
}
