package org.egov.bpa.util;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.egov.bpa.config.BPAConfiguration;
import org.egov.bpa.repository.ServiceRequestRepository;
import org.egov.bpa.web.model.AuditDetails;
import org.egov.bpa.web.model.RequestInfoWrapper;
import org.egov.bpa.web.model.regularization.PlotInfo;
import org.egov.bpa.web.model.regularization.Regularization;
import org.egov.common.contract.request.RequestInfo;
import org.egov.mdms.model.MasterDetail;
import org.egov.mdms.model.MdmsCriteria;
import org.egov.mdms.model.MdmsCriteriaReq;
import org.egov.mdms.model.ModuleDetail;
import org.egov.tracer.model.CustomException;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.spi.json.JacksonJsonProvider;
import com.jayway.jsonpath.spi.json.JsonProvider;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;
import com.jayway.jsonpath.spi.mapper.MappingProvider;

import io.micrometer.core.instrument.util.StringUtils;

@Component
public class RegularizationUtil {
	
	@Autowired
	private BPAConfiguration config;

	@Autowired
	private ServiceRequestRepository serviceRequestRepository;
	
	/**
	 * Generic Method to make MDMS call
	 * 
	 * @param requestInfo
	 * @param tenantId
	 * @return mdms data
	 */
	public Object mDMSCall(RequestInfo requestInfo, String tenantId) {
		MdmsCriteriaReq mdmsCriteriaReq = getMDMSRequest(requestInfo, tenantId);
		Object result = serviceRequestRepository.fetchResult(getMdmsSearchUrl(), mdmsCriteriaReq);
		return result;
	}
	
	/**
	 * Get MDMS url with this method
	 * 
	 * @return string url
	 */
	public StringBuilder getMdmsSearchUrl() {
		return new StringBuilder().append(config.getMdmsHost()).append(config.getMdmsEndPoint());
	}
	
	/**
	 * Method to prepare the MDMS request
	 * 
	 * @param requestInfo
	 * @param tenantId
	 * @return
	 */
	public MdmsCriteriaReq getMDMSRequest(RequestInfo requestInfo, String tenantId) {
		List<ModuleDetail> moduleRequest = getBPAModuleRequest();

		List<ModuleDetail> moduleDetails = new LinkedList<>();
		moduleDetails.addAll(moduleRequest);

		MdmsCriteria mdmsCriteria = MdmsCriteria.builder().moduleDetails(moduleDetails).tenantId(tenantId).build();

		MdmsCriteriaReq mdmsCriteriaReq = MdmsCriteriaReq.builder().mdmsCriteria(mdmsCriteria).requestInfo(requestInfo)
				.build();
		return mdmsCriteriaReq;
	}
	
	/**
	 * Method where we provide the path and fileName for MDMS request
	 * 
	 * @return list of Module Details to be used for MDMS Request
	 */
	public List<ModuleDetail> getBPAModuleRequest() {

		List<MasterDetail> bpaMasterDtls = new ArrayList<>();

		// filter to only get code field from master data
		final String filterCode = "$.[?(@.active==true)].code";

		// master details for BPA module
		bpaMasterDtls.add(MasterDetail.builder().name(RegularizationConstants.ODA_ULBS).build());
		bpaMasterDtls.add(MasterDetail.builder().name(RegularizationConstants.DOCUMENT_TYPE_MAPPING).build());

		List<MasterDetail> commonMasterDetails = new ArrayList<>();
		commonMasterDetails.add(
				MasterDetail.builder().name(RegularizationConstants.OWNERSHIP_CATEGORY).filter(filterCode).build());
		commonMasterDetails
				.add(MasterDetail.builder().name(RegularizationConstants.DOCUMENT_TYPE).filter(filterCode).build());

		ModuleDetail bpaModuleDtls = ModuleDetail.builder().masterDetails(bpaMasterDtls)
				.moduleName(BPAConstants.BPA_MODULE).build();

		ModuleDetail commonMasterMDtl = ModuleDetail.builder().masterDetails(commonMasterDetails)
				.moduleName(BPAConstants.COMMON_MASTERS_MODULE).build();

		return Arrays.asList(bpaModuleDtls, commonMasterMDtl);
	}
	
	public AuditDetails getAuditDetails(String by, Boolean isCreate) {
		Long time = System.currentTimeMillis();
		if (isCreate)
			return AuditDetails.builder().createdBy(by).lastModifiedBy(by).createdTime(time).lastModifiedTime(time)
					.build();
		else
			return AuditDetails.builder().lastModifiedBy(by).lastModifiedTime(time).build();
	}
	
	public Map<String, List<String>> getAttributeValues(Object mdmsData) {

		List<String> modulepaths = Arrays.asList(BPAConstants.BPA_JSONPATH_CODE,
				BPAConstants.COMMON_MASTER_JSONPATH_CODE);
		final Map<String, List<String>> mdmsResMap = new HashMap<>();
		modulepaths.forEach(modulepath -> {
			try {
				mdmsResMap.putAll(JsonPath.read(mdmsData, modulepath));
			} catch (Exception e) {
				throw new CustomException(BPAErrorConstants.INVALID_TENANT_ID_MDMS_KEY,
						BPAErrorConstants.INVALID_TENANT_ID_MDMS_MSG);
			}
		});
		return mdmsResMap;
	}

	
	public Calendar getCalendarInstaceAfter(Integer validityInMonths) {
		Calendar calendar = Calendar.getInstance();
		calendar.add(Calendar.MONTH, validityInMonths);
		return calendar;
	}
	
	/**
	 * Set Default json path for regularization notice request
	 * 
	 */
	public void defaultJsonPathConfig() {
		Configuration.setDefaults(new Configuration.Defaults() {

			private final JsonProvider jsonProvider = new JacksonJsonProvider();
			private final MappingProvider mappingProvider = new JacksonMappingProvider();

			@Override
			public JsonProvider jsonProvider() {
				return jsonProvider;
			}

			@Override
			public MappingProvider mappingProvider() {
				return mappingProvider;
			}

			@Override
			public Set<Option> options() {
				return EnumSet.noneOf(Option.class);
			}
		});

	}
	
	
	
	/**
	 * Fetch the demand amount of the Regularization
	 * 
	 * @param requestInfo
	 * @param regularization
	 * @return paid amount
	 */
	@SuppressWarnings("rawtypes")
	public BigDecimal getAmountToBePaid(RequestInfo requestInfo, Regularization regularization) {
		LinkedHashMap responseMap = (LinkedHashMap) serviceRequestRepository.fetchResult(getBillUri(regularization), new RequestInfoWrapper(requestInfo));
		JSONObject jsonObject = new JSONObject(responseMap);
		BigDecimal amountToBePaid;
		double amount = 0.0;
		try {
			JSONArray demandArray = (JSONArray) jsonObject.get("Demands");
			if (demandArray != null) {
				JSONObject firstElement = (JSONObject) demandArray.get(0);
				if (firstElement != null) {
					JSONArray demandDetails = (JSONArray) firstElement.get("demandDetails");
					if (demandDetails != null) {
						for (int i = 0; i < demandDetails.length(); i++) {
							JSONObject object = (JSONObject) demandDetails.get(i);
							Double taxAmt = Double.valueOf((object.get("taxAmount").toString()));
							amount = amount + taxAmt;
						}
					}
				}
			}
			amountToBePaid = BigDecimal.valueOf(amount);
		} catch (Exception e) {
			throw new CustomException("PARSING ERROR", "Failed to parse the response using jsonPath: " + BPAConstants.BILL_AMOUNT);
		}
		return amountToBePaid;
	}

	
	/**
	 * @param regularization
	 * @return BillUri
	 */
	public StringBuilder getBillUri(Regularization regularization) {
		String code = getBusinessServiceCode(regularization);

		StringBuilder builder = new StringBuilder(config.getBillingHost());
		builder.append(config.getDemandSearchEndpoint());
		builder.append("?tenantId=");
		builder.append(regularization.getTenantId());
		builder.append("&consumerCode=");
		builder.append(regularization.getApplicationNo());
		builder.append("&businessService=");
		builder.append(code);
		return builder;
	}

	
	
	/**
	 * return the FeeBusiness Service Code based on the Regularization Status , Regularization Business Service
	 * 
	 * @param regularization
	 * @return Business Service Code
	 */
	private String getBusinessServiceCode(Regularization regularization) {
		Map<String, String> statusBusinessServiceMap = config.getRegularizationStatusBusinessServiceMap();
		String businessServiceCode = null;
		if (!CollectionUtils.isEmpty(statusBusinessServiceMap)) {
			if (!ObjectUtils.isEmpty(regularization.getStatus())) {
				businessServiceCode = statusBusinessServiceMap.get(regularization.getStatus());
			} 
		}
		return businessServiceCode;
	}

	
	/**
	 * @param List<PlotInfo>
	 * @return NetPlotArea excluding gift area
	 */
	public BigDecimal getNetPlotArea(List<PlotInfo> plotInfo) {
		BigDecimal netPlotArea = BigDecimal.ZERO;
		for(PlotInfo plot : plotInfo) {
			netPlotArea = netPlotArea.add(getNetPlotArea(plot));
		}
		return netPlotArea;
	}

	
	/**
	 * @param plot
	 * @return NetPlotArea excluding gift area
	 */
	public BigDecimal getNetPlotArea(PlotInfo plot) {
		return convertToBigDecimal(plot.getPlotArea()).subtract(convertToBigDecimal(plot.getAreaToBeGifted())).setScale(2, BigDecimal.ROUND_HALF_UP);
	}
	
	
	/**
	 * @param stringValue
	 * @return converted value
	 */
	public BigDecimal convertToBigDecimal(String stringValue) {
		BigDecimal value = BigDecimal.ZERO;
		if(StringUtils.isNotEmpty(stringValue)) {
			try {
				value = new BigDecimal(stringValue);
			} catch (Exception e) {}
		}
		return value;
	}
	
	public Boolean checkIfOlderThanThirtyDays(Long approvedDate) {
		// Get the current time
		Instant now = Instant.now();

		// Convert the epoch timestamp to Instant
		Instant epochInstant = Instant.ofEpochMilli(approvedDate);

		Boolean isOlderThan30Days = Duration.between(epochInstant, now).toDays() > 30;

		return isOlderThan30Days;
	}

	public Long calculateDaysSinceApproved(Long approvedDate) {
		// Get the current time
		Instant now = Instant.now();

		// Convert the epoch timestamp to Instant
		Instant epochInstant = Instant.ofEpochMilli(approvedDate);
		
		Long daysSinceApproval = Duration.between(epochInstant, now).toDays();
		
		return daysSinceApproval;
	}
}
