package org.egov.pt.util;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.egov.pt.config.PropertyConfiguration;
import org.egov.pt.web.contracts.Demand;
import org.egov.pt.web.contracts.DemandDetail;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.Getter;

@Component
@Getter
public class DemandUtils {

	@Autowired
	private PropertyConfiguration configurations;
	
	@Autowired
	private ObjectMapper mapper;
	
	private static Map<String, String> taxHeadMasterMap = new HashMap<>();
	
	static {
		taxHeadMasterMap.put("PT_PENALTY","penalty");
		taxHeadMasterMap.put("PT_INTEREST","interest");
		taxHeadMasterMap.put("PT_LIGHT_TAX","lightTax");
		taxHeadMasterMap.put("PT_WATER_TAX","waterTax");
		taxHeadMasterMap.put("PT_OTHER_DUES","otherDues");
		taxHeadMasterMap.put("PT_HOLDING_TAX","holdingTax");
		taxHeadMasterMap.put("PT_LATRINE_TAX","latrineTax");
		taxHeadMasterMap.put("PT_PARKING_TAX","parkingTax");
		taxHeadMasterMap.put("PT_SERVICE_TAX","serviceTax");
		taxHeadMasterMap.put("PT_DRAINAGE_TAX","drainageTax");
		taxHeadMasterMap.put("PT_USAGE_EXCEMPTION","usageExemption");
		taxHeadMasterMap.put("PT_OWNERSHIP_EXCEMPTION","ownershipExemption");
		taxHeadMasterMap.put("PT_SOLID_WASTE_USER_CHARGES","solidWasteUserCharges");
	}
	
	/**
	 * Creates demand Search url based on tenantId,businessService, period from, period to and
	 * ConsumerCode 
	 * 
	 * @return demand search url
	 */
    public StringBuilder getDemandSearchURL(String tenantId, Set<String> consumerCodes, Long taxPeriodFrom, Long taxPeriodTo, String businessService) {
		StringBuilder url = new StringBuilder(configurations.getEgbsHost());
		url.append(configurations.getEgbsDemandSearchEndpoint());
		url.append("?");
		url.append("tenantId=");
		url.append(tenantId);
		url.append("&");
		url.append("businessService=");
		url.append(businessService);
		url.append("&");
		url.append("consumerCode=");
		url.append(StringUtils.join(consumerCodes, ','));
		if (taxPeriodFrom != null) {
			url.append("&");
			url.append("periodFrom=");
			url.append(taxPeriodFrom.toString());
		}
		if (taxPeriodTo != null) {
			url.append("&");
			url.append("periodTo=");
			url.append(taxPeriodTo.toString());
		}
		return url;
	}
    

    public JsonNode prepareAdditionalDetailsFromDemand(List<Demand> demands) {
		Map<String,String> additionalDetail = new HashMap<>();
		Collections.sort(demands, Comparator.comparing(Demand::getTaxPeriodFrom).thenComparing(Demand::getTaxPeriodTo).reversed());
		Demand demand = demands.get(0);
		String key = null;
		BigDecimal taxAmountTaxheadWise = BigDecimal.ZERO;
		for(DemandDetail demandDetail : demand.getDemandDetails()) {
			key = taxHeadMasterMap.get(demandDetail.getTaxHeadMasterCode()) ;
			if(!Objects.isNull(key)) {
				if(demandDetail.getTaxAmount().compareTo(BigDecimal.ZERO) < 0) {
					taxAmountTaxheadWise = demandDetail.getTaxAmount().negate();
				}else {
					taxAmountTaxheadWise = demandDetail.getTaxAmount();
				}
				if(Objects.isNull(additionalDetail.get(key))) {
					additionalDetail.put(key, String.valueOf(taxAmountTaxheadWise));
				}else {
					taxAmountTaxheadWise = taxAmountTaxheadWise.add(new BigDecimal(additionalDetail.get(key)));
					additionalDetail.put(key, String.valueOf(taxAmountTaxheadWise));
				}
			}
			
		}
		return mapper.convertValue(additionalDetail,JsonNode.class);
	}
}
