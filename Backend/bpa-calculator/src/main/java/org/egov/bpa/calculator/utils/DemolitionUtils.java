package org.egov.bpa.calculator.utils;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.egov.bpa.calculator.config.BPACalculatorConfig;
import org.egov.bpa.calculator.web.models.demand.Category;
import org.egov.bpa.calculator.web.models.demand.TaxHeadEstimate;
import org.egov.bpa.calculator.web.models.demolition.BlockInfo;
import org.egov.bpa.calculator.web.models.demolition.DemolitionCalculationCriteria;
import org.egov.bpa.calculator.web.models.demolition.DemolitionCalculationRequest;
import org.egov.tracer.model.CustomException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

@Component
public class DemolitionUtils {
	
	@Autowired
	private BPACalculatorConfig config;
	
	/**
	 * Validate the owners before givings estimates
	 * 
	 * @param calculationReq
	 */
	public void validateOwnerDetails(DemolitionCalculationRequest calculationReq) {
		Map<String, String> errorMap = new HashMap<>();
		for (DemolitionCalculationCriteria calculationCriteria : calculationReq.getCalculationCriteria()) {

			if (calculationCriteria.getDemolition().getOwnershipCategory().toLowerCase()
					.contains("individual.singleowner")
					&& Objects.isNull(calculationCriteria.getDemolition().getOwners().get(0).getMobileNumber())) {
				errorMap.put("MOBILE_NUMBER_MISSING", "Mobile number is mandatory for Individual Single Owner");
			}

			if (calculationCriteria.getDemolition().getOwnershipCategory().toLowerCase()
					.contains("individual.singleowner")
					&& Objects.isNull(calculationCriteria.getDemolition().getOwners().get(0).getName())) {
				errorMap.put("APPLICANT_NAME_MISSING", "Applicant name is mandatory for Individual Single Owner");
			}

			if (calculationCriteria.getDemolition().getOwnershipCategory().toLowerCase()
					.contains("individual.singleowner")
					&& Objects.isNull(
							calculationCriteria.getDemolition().getOwners().get(0).getCorrespondenceAddress())) {
				errorMap.put("CORRESPONDENCE_ADDRESS_MISSING",
						"Applicant Correspondence Address is mandatory for Individual Single Owner");
			}

		}

		if (!CollectionUtils.isEmpty(errorMap))
			throw new CustomException(errorMap);

	}

	public Boolean anyApprovedAreaExists(DemolitionCalculationCriteria criteria) {

		Boolean anyApprovedArea = Boolean.FALSE;

		List<BlockInfo> blockInfos = criteria.getDemolition().getLandInfo().getBlockInfo();

		for (BlockInfo blockInfo : blockInfos) {
			if (blockInfo.getAnyApprovedArea().equalsIgnoreCase("Yes")) {
				anyApprovedArea = true;
				break;
			}
		}
		return anyApprovedArea;
	}

	public void generateTaxHeadEstimate(List<TaxHeadEstimate> estimates, BigDecimal feeAmount, String taxHeadCode,
			Category category) {

		TaxHeadEstimate estimate = TaxHeadEstimate.builder().estimateAmount(roundUpValue(feeAmount)).category(category)
				.taxHeadCode(taxHeadCode).build();
		estimates.add(estimate);

	}

	public BigDecimal roundUpValue(BigDecimal value) {
		return value.setScale(0, BigDecimal.ROUND_UP);
	}

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

	public String getBillingBusinessService() {
		return config.getDemolitionBusinessServie();
	}
}
