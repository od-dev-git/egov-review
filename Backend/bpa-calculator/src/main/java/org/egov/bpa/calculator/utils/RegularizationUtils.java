package org.egov.bpa.calculator.utils;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang3.ObjectUtils;
import org.egov.bpa.calculator.config.BPACalculatorConfig;
import org.egov.bpa.calculator.web.models.demand.Category;
import org.egov.bpa.calculator.web.models.demand.TaxHeadEstimate;
import org.egov.bpa.calculator.web.models.regularization.CalculateFeeInfo;
import org.egov.bpa.calculator.web.models.regularization.CalculationRequest;
import org.egov.bpa.calculator.web.models.regularization.PlotInfo;
import org.egov.bpa.calculator.web.models.regularization.RegularizationCalculationCriteria;
import org.egov.tracer.model.CustomException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import io.micrometer.core.instrument.util.StringUtils;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class RegularizationUtils {

	@Autowired
	private BPACalculatorConfig config;
	

	/**
	 * Validate the owners before givings estimates
	 * 
	 * @param calculationReq
	 */
	public void validateOwnerDetails(CalculationRequest calculationReq) {
		Map<String, String> errorMap = new HashMap<>();
		for (RegularizationCalculationCriteria calculationCriteria : calculationReq.getCalculationCriteria()) {

			if (calculationCriteria.getRegularization().getOwnershipCategory().toLowerCase()
					.contains("individual.singleowner")
					&& Objects.isNull(calculationCriteria.getRegularization().getOwners().get(0).getMobileNumber())) {
				errorMap.put("MOBILE_NUMBER_MISSING", "Mobile number is mandatory for Individual Single Owner");
			}

			if (calculationCriteria.getRegularization().getOwnershipCategory().toLowerCase()
					.contains("individual.singleowner")
					&& Objects.isNull(calculationCriteria.getRegularization().getOwners().get(0).getName())) {
				errorMap.put("APPLICANT_NAME_MISSING", "Applicant name is mandatory for Individual Single Owner");
			}

			if (calculationCriteria.getRegularization().getOwnershipCategory().toLowerCase()
					.contains("individual.singleowner")
					&& Objects.isNull(
							calculationCriteria.getRegularization().getOwners().get(0).getCorrespondenceAddress())) {
				errorMap.put("CORRESPONDENCE_ADDRESS_MISSING",
						"Applicant Correspondence Address is mandatory for Individual Single Owner");
			}

		}

		if (!CollectionUtils.isEmpty(errorMap))
			throw new CustomException(errorMap);

	}

	/**
	 * Method to prepare demand search url
	 * 
	 * @return
	 */
	public String getDemandSearchURL() {
		StringBuilder url = new StringBuilder(config.getBillingHost());
		url.append(config.getDemandSearchEndpoint());
		url.append("?");
		url.append("tenantId=");
		url.append("{1}");
		url.append("&");
		url.append("businessService=");
		url.append("{2}");
		url.append("&");
		url.append("consumerCode=");
		url.append("{3}");
		return url.toString();
	}

	/**
	 * Get the businessService here for demand table
	 * 
	 * @param businessService
	 * @param feeType
	 * @return
	 */
	public String getBillingBusinessService(String businessService, String feeType) {
		String billingBusinessService;
		switch (feeType) {
		case RegularizationConstants.APPLICATION_FEE:
			billingBusinessService = config.getRegularizationAppFeeBusinessService();
			break;

		case RegularizationConstants.SANCTION_FEE:
			billingBusinessService = config.getRegularizationSancFeeBusinessService();
			break;

		default:
			billingBusinessService = feeType;
			break;
		}
		return billingBusinessService;
	}

	/**
	 * Generate the estimate object here for your fee estimates
	 * 
	 * @param estimates
	 * @param feeAmount
	 * @param taxHeadCode
	 * @param category
	 */
	public void generateTaxHeadEstimate(List<TaxHeadEstimate> estimates, BigDecimal feeAmount, String taxHeadCode,
			Category category) {
	    log.info("Starting generateTaxHeadEstimate with feeAmount: {}, taxHeadCode: {}, category: {}", feeAmount, taxHeadCode, category);

	    if (feeAmount == null) {
	        log.warn("feeAmount is null, defaulting to BigDecimal.ZERO");
	        feeAmount = BigDecimal.ZERO;
	    }

	    if (feeAmount.compareTo(BigDecimal.ZERO) < 0) {
	        log.warn("feeAmount is negative: {}, setting it to BigDecimal.ZERO", feeAmount);
	        feeAmount = BigDecimal.ZERO;
	    }

		TaxHeadEstimate estimate = TaxHeadEstimate.builder().estimateAmount(roundUpValue(feeAmount)).category(category)
				.taxHeadCode(taxHeadCode).build();
		estimates.add(estimate);
		
	    log.info("Added TaxHeadEstimate to estimates: {}", estimate);
	}

	/**
	 * @param value
	 * @return roundUpValue
	 */
	public BigDecimal roundUpValue(BigDecimal value) {
		return value.setScale(0, BigDecimal.ROUND_UP);
	}

	/**
	 * divide value with divisor and round half up to 2 preceding
	 * 
	 * @param value
	 * @param divisor
	 * @return result value with ROUND_HALF_UP
	 */
	public BigDecimal divideWithRoundUp(BigDecimal value, BigDecimal divisor) {
		return value.divide(divisor, 2, BigDecimal.ROUND_HALF_UP);
	}

	/**
	 * multiply value with multiplicand and round half up to 2 preceding
	 * 
	 * @param value
	 * @param multiplicand
	 * @return result value with ROUND_HALF_UP
	 */
	public BigDecimal multiplyWithRoundUp(BigDecimal value, BigDecimal multiplicand) {
		return value.multiply(multiplicand).setScale(2, BigDecimal.ROUND_HALF_UP);
	}

	/**
	 * multiply value with multiplicand and round half up to 2 preceding
	 * 
	 * @param value
	 * @param multiplicand
	 * @return result value with ROUND_HALF_UP
	 */
	public BigDecimal add3WithRoundUp(BigDecimal value1, BigDecimal value2, BigDecimal value3) {
		return value1.add(value2).add(value3).setScale(2, BigDecimal.ROUND_HALF_UP);
	}

	/**
	 * @param feeInfo
	 * @return calculated VariableFee
	 */
	public BigDecimal calculateVariableFee(CalculateFeeInfo feeInfo) {
		BigDecimal amount = BigDecimal.ZERO;
		Double totalBuiltUpArea = feeInfo.getTotalBuiltUpArea();
		if (!ObjectUtils.isEmpty(totalBuiltUpArea)) {
			if (totalBuiltUpArea <= feeInfo.getMinCalculationPoint()) {
				amount = feeInfo.getMinimumFee();
			} else if (totalBuiltUpArea <= feeInfo.getMaxCalculationPoint()) {
				amount = (feeInfo.getMinimumFee()
						.add(feeInfo.getInBetweenFeePerUnit()
								.multiply(BigDecimal.valueOf(totalBuiltUpArea - feeInfo.getMinCalculationPoint()))))
						.setScale(2, BigDecimal.ROUND_UP);
			} else if (totalBuiltUpArea > feeInfo.getMaxCalculationPoint()) {
				amount = (feeInfo.getMinimumFee()
						.add(feeInfo.getInBetweenFeePerUnit()
								.multiply(BigDecimal
										.valueOf(feeInfo.getMaxCalculationPoint() - feeInfo.getMinCalculationPoint())))
						.add(feeInfo.getMaximumFeePerUnit()
								.multiply(BigDecimal.valueOf(totalBuiltUpArea - feeInfo.getMaxCalculationPoint()))))
						.setScale(2, BigDecimal.ROUND_UP);
			}
		}
		return amount;
	}

	
	
	/**
	 * @param totalBuiltUpArea
	 * @param commonConstantPricePerUnit
	 * @return calculated fee
	 */
	public BigDecimal calculateConstantFee(Double totalBuiltUpArea, BigDecimal commonConstantPricePerUnit) {
		BigDecimal totalAmount = BigDecimal.ZERO;
		if (!ObjectUtils.isEmpty(totalBuiltUpArea)) {
			totalAmount = (BigDecimal.valueOf(totalBuiltUpArea)
					.multiply(commonConstantPricePerUnit))
					.setScale(2, BigDecimal.ROUND_UP);
		}
		return totalAmount;
	}

	
	
	/**
	 * @param totalUnauthAreaonSBBeyondNormsButUnder5
	 * @return converted value
	 */
	public BigDecimal convertToBigDecimal(String stringValue) {
		BigDecimal value = BigDecimal.ZERO;
		if(StringUtils.isNotEmpty(stringValue)) {
			try {
				value = new BigDecimal(stringValue.trim());
			} catch (Exception e) {}
		}
		return value;
	}

	
	/**
	 * @param value
	 * @return DoubleValue
	 */
	public double getDoubleValue(String value) {
		double doubleValue = 0;
		if(ObjectUtils.isNotEmpty(value)) {
			try {
				doubleValue = Double.valueOf(value);
			} catch (Exception e) {}
		}
		return doubleValue;
	}

	
	/**
	 * @param value
	 * @return Integer Value
	 */
	public int convertToInteger(String value) {
		int intValue = 0;
		if(ObjectUtils.isNotEmpty(value)) {
			try {
				intValue = Integer.parseInt(value);
			} catch (Exception e) {}
		}
		return intValue;
	}

	
	/**
	 * @param List<PlotInfo>
	 * @return NetPlotArea excluding gift area
	 */
	public BigDecimal getNetPlotArea(List<PlotInfo> plotInfo) {
		BigDecimal netPlotArea = BigDecimal.ZERO;
		for(PlotInfo plot : plotInfo) {
			if(plot.getActive() == null || plot.getActive()) {
				netPlotArea = netPlotArea.add(getNetPlotArea(plot));
			}
		}
		return netPlotArea;
	}

	
	/**
	 * @param plot
	 * @return NetPlotArea excluding gift area
	 */
	public BigDecimal getNetPlotArea(PlotInfo plot) {
		return subtractWithRoundUp(plot.getPlotArea(), plot.getAreaToBeGifted());
	}
	
	/**
	 * @param value1
	 * @param value2
	 * @return Subtraction value in BigDecimal 
	 */
	public BigDecimal subtractWithRoundUp(String value1, String value2) {
		return convertToBigDecimal(value1).subtract(convertToBigDecimal(value2)).setScale(2, BigDecimal.ROUND_HALF_UP);
	}

	

}