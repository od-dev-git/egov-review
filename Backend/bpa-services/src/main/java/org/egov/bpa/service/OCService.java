package org.egov.bpa.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import org.egov.bpa.config.BPAConfiguration;
import org.egov.bpa.repository.ServiceRequestRepository;
import org.egov.bpa.util.BPAConstants;
import org.egov.bpa.util.BPAErrorConstants;
import org.egov.bpa.util.BPAUtil;
import org.egov.bpa.web.model.BPARequest;
import org.egov.bpa.web.model.BPASearchCriteria;
import org.egov.bpa.web.model.edcr.RequestInfo;
import org.egov.mdms.model.MdmsCriteriaReq;
import org.egov.tracer.model.CustomException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.jayway.jsonpath.JsonPath;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class OCService {

	@Autowired
	private BPAConfiguration config;

	@Autowired
	private ServiceRequestRepository serviceRequestRepository;

	@Autowired
	private BPAUtil util;

	/**
	 * Validates additional data between OC (Occupancy Certificate) and BPA EDCRs.
	 * 
	 * <p>
	 * This method orchestrates the complete EDCR validation lifecycle:
	 * <ul>
	 * <li>Fetches both OC EDCR and BPA EDCR details from EDCR service</li>
	 * <li>Extracts key parameters (risk type, khata number, plot number,
	 * occupancy)</li>
	 * <li>Validates that these parameters match between OC and BPA</li>
	 * <li>Ensures compliance and data consistency</li>
	 * </ul>
	 * 
	 * <p>
	 * <strong>Business Logic:</strong>
	 * <ul>
	 * <li><strong>OC-BPA Linkage:</strong> Occupancy Certificate must be issued for
	 * the same building/plot as the original Building Permit</li>
	 * <li><strong>Risk Type Validation:</strong> OC cannot be for higher risk than
	 * BPA (e.g., LOW BPA cannot have MEDIUM/HIGH OC)</li>
	 * <li><strong>Property Matching:</strong> Khata number, plot number must match
	 * exactly</li>
	 * <li><strong>Occupancy Consistency:</strong> Building usage type must remain
	 * consistent</li>
	 * </ul>
	 * 
	 * @param bpaRequest the BPA request containing OC EDCR number
	 * @param criteria   the search criteria containing BPA EDCR number
	 * @throws CustomException if validation fails or EDCR data doesn't match
	 */
	@SuppressWarnings({ "unchecked", "rawtypes", "unused" })
	public void validateAdditionalData(BPARequest bpaRequest, BPASearchCriteria criteria) {

		log.info("Validating additional data between OC EDCR: {} and BPA EDCR: {}", bpaRequest.getBPA().getEdcrNumber(),
				criteria.getEdcrNumber());

		// Step 1: Fetch EDCR details for both OC and BPA
		List<LinkedHashMap<String, Object>> edcrDataList = fetchBothEdcrDetails(bpaRequest, criteria);

		// Step 2: Validate we have both EDCR responses
		if (edcrDataList.size() < 2) {
			log.error("Failed to fetch both EDCR details - received {} responses", edcrDataList.size());
			throw new CustomException(BPAErrorConstants.INVALID_CREATE,
					"Unable to validate: failed to retrieve both OC and BPA EDCR details");
		}

		// Step 3: Extract key parameters from both EDCRs
		EdcrValidationData validationData = extractEdcrValidationData(edcrDataList, bpaRequest.getRequestInfo(),
				criteria);

		// Step 4: Validate all extracted parameters match
		validateExtractedData(validationData);

		log.info("Additional data validation completed successfully for OC and BPA EDCRs");
	}

	/**
	 * Fetches EDCR details for both OC and BPA applications.
	 * 
	 * @param bpaRequest the BPA request containing OC EDCR number
	 * @param criteria   the search criteria containing BPA EDCR number
	 * @return list of EDCR response maps (OC and BPA)
	 */
	private List<LinkedHashMap<String, Object>> fetchBothEdcrDetails(BPARequest bpaRequest,
			BPASearchCriteria criteria) {

		String ocEdcrNumber = bpaRequest.getBPA().getEdcrNumber();
		String bpaEdcrNumber = criteria.getEdcrNumber();

		List<String> edcrNumbers = new ArrayList<>();
		edcrNumbers.add(ocEdcrNumber);
		edcrNumbers.add(bpaEdcrNumber);

		List<LinkedHashMap<String, Object>> edcrDataList = new ArrayList<>();

		for (String edcrNumber : edcrNumbers) {
			try {
				LinkedHashMap<String, Object> edcrData = fetchSingleEdcrDetails(criteria.getTenantId(), edcrNumber);
				edcrDataList.add(edcrData);
			} catch (Exception e) {
				log.error("Failed to fetch EDCR details for EDCR number: {}", edcrNumber, e);
			}
		}

		return edcrDataList;
	}

	/**
	 * Fetches EDCR details for a single EDCR number.
	 * 
	 * @param tenantId   the tenant ID
	 * @param edcrNumber the EDCR number to fetch
	 * @return the EDCR response map
	 */
	@SuppressWarnings("rawtypes")
	private LinkedHashMap<String, Object> fetchSingleEdcrDetails(String tenantId, String edcrNumber) {
		StringBuilder uri = new StringBuilder(config.getEdcrHost());
		uri.append(config.getGetPlanEndPoint());
		uri.append("?tenantId=").append(tenantId);
		uri.append("&edcrNumber=").append(edcrNumber);

		RequestInfo edcrRequestInfo = new RequestInfo();

		return (LinkedHashMap) serviceRequestRepository.fetchResult(uri,
				new org.egov.bpa.web.model.edcr.RequestInfoWrapper(edcrRequestInfo));
	}

	/**
	 * Inner class to hold extracted EDCR validation data.
	 */
	private static class EdcrValidationData {
		private final List<String> riskTypes;
		private final List<String> khataNumbers;
		private final List<String> plotNumbers;
		private final List<String> occupancyTypes;

		public EdcrValidationData(List<String> riskTypes, List<String> khataNumbers, List<String> plotNumbers,
				List<String> occupancyTypes) {
			this.riskTypes = riskTypes;
			this.khataNumbers = khataNumbers;
			this.plotNumbers = plotNumbers;
			this.occupancyTypes = occupancyTypes;
		}

		public List<String> getRiskTypes() {
			return riskTypes;
		}

		public List<String> getKhataNumbers() {
			return khataNumbers;
		}

		public List<String> getPlotNumbers() {
			return plotNumbers;
		}

		public List<String> getOccupancyTypes() {
			return occupancyTypes;
		}
	}

	/**
	 * Extracts validation data from both EDCR responses.
	 * 
	 * @param edcrDataList the list of EDCR responses
	 * @param requestInfo  the request information
	 * @param criteria     the search criteria
	 * @return extracted validation data
	 */
	private EdcrValidationData extractEdcrValidationData(List<LinkedHashMap<String, Object>> edcrDataList,
			org.egov.common.contract.request.RequestInfo requestInfo, BPASearchCriteria criteria) {

		List<String> riskTypes = new ArrayList<>();
		List<String> khataNumbers = new ArrayList<>();
		List<String> plotNumbers = new ArrayList<>();
		List<String> occupancyTypes = new ArrayList<>();

		extractEdcrData(edcrDataList, riskTypes, khataNumbers, plotNumbers, occupancyTypes, requestInfo, criteria);

		return new EdcrValidationData(riskTypes, khataNumbers, plotNumbers, occupancyTypes);
	}

	/**
	 * Validates all extracted EDCR data for consistency.
	 * 
	 * @param validationData the extracted validation data
	 * @throws CustomException if any validation fails
	 */
	private void validateExtractedData(EdcrValidationData validationData) {
		validateRiskKhataplotOccupancyType(new ArrayList<>(validationData.getRiskTypes()),
				new ArrayList<>(validationData.getKhataNumbers()), new ArrayList<>(validationData.getPlotNumbers()),
				new ArrayList<>(validationData.getOccupancyTypes()));
	}

	/**
	 * Extracts khata number, plot number, occupancy type from EDCR data.
	 * 
	 * <p>
	 * This method processes each EDCR detail and:
	 * <ul>
	 * <li>Calculates risk type based on building height and plot area using MDMS
	 * rules</li>
	 * <li>Extracts khata number (property tax identifier)</li>
	 * <li>Extracts plot number (land parcel identifier)</li>
	 * <li>Extracts occupancy type (residential, commercial, etc.)</li>
	 * </ul>
	 * 
	 * @param edcrData    list of EDCR response maps
	 * @param riskType    output list for risk types
	 * @param khathaNos   output list for khata numbers
	 * @param plotNos     output list for plot numbers
	 * @param occupancy   output list for occupancy types
	 * @param requestInfo the request information
	 * @param criteria    the search criteria
	 */
	private void extractEdcrData(List<LinkedHashMap<String, Object>> edcrData, List<String> riskType,
			List<String> khathaNos, List<String> plotNos, List<String> occupancy,
			org.egov.common.contract.request.RequestInfo requestInfo, BPASearchCriteria criteria) {

		edcrData.forEach(edcrDetail -> {
			// Fetch MDMS risk type computation rules
			Object mdmsData = fetchMdmsRiskTypeRules(requestInfo, criteria.getTenantId());

			// Extract building parameters
			Double buildingHeight = extractBuildingHeight(edcrDetail);
			Double plotArea = extractPlotArea(edcrDetail);

			// Calculate risk type based on MDMS rules
			String calculatedRiskType = calculateRiskType(mdmsData, buildingHeight, plotArea);
			riskType.add(calculatedRiskType);

			// Extract property identifiers
			khathaNos.add(JsonPath.read(edcrDetail, BPAConstants.OC_KHATHANO));
			plotNos.add(JsonPath.read(edcrDetail, BPAConstants.OC_PLOTNO));
			occupancy.add(JsonPath.read(edcrDetail, BPAConstants.OC_OCCUPANCY));
		});
	}

	/**
	 * Fetches MDMS risk type computation rules.
	 * 
	 * @param requestInfo the request information
	 * @param tenantId    the tenant ID
	 * @return MDMS risk type computation data
	 * @throws CustomException if MDMS data doesn't exist
	 */
	private Object fetchMdmsRiskTypeRules(org.egov.common.contract.request.RequestInfo requestInfo, String tenantId) {

		MdmsCriteriaReq mdmsCriteriaReq = util.getMDMSRequest(requestInfo, tenantId);
		Object result = serviceRequestRepository.fetchResult(util.getMdmsSearchUrl(), mdmsCriteriaReq);

		ArrayList ocData = new ArrayList();
		ocData.add(JsonPath.read(result, "$.MdmsRes.BPA.RiskTypeComputation"));

		if (ocData.isEmpty()) {
			throw new CustomException(BPAErrorConstants.INVALID_CREATE,
					"RiskType Computation MDMS configuration does not exist");
		}

		return ocData.get(0);
	}

	/**
	 * Extracts building height from EDCR detail.
	 * 
	 * @param edcrDetail the EDCR detail map
	 * @return building height in meters
	 */
	@SuppressWarnings("unchecked")
	private Double extractBuildingHeight(LinkedHashMap<String, Object> edcrDetail) {
		List<Double> heights = JsonPath.read(edcrDetail, "$.edcrDetail.*.planDetail.blocks.*.building.buildingHeight");
		return heights.get(0);
	}

	/**
	 * Extracts plot area from EDCR detail.
	 * 
	 * @param edcrDetail the EDCR detail map
	 * @return plot area in square meters
	 */
	@SuppressWarnings("unchecked")
	private Double extractPlotArea(LinkedHashMap<String, Object> edcrDetail) {
		List<Double> areas = JsonPath.read(edcrDetail, "$.edcrDetail.*.planDetail.plot.area");
		return areas.get(0);
	}

	/**
	 * Calculates risk type based on building height and plot area.
	 * 
	 * <p>
	 * <strong>Risk Type Calculation:</strong> Uses MDMS rules to determine risk
	 * category based on building dimensions. Higher buildings or larger plots may
	 * fall into higher risk categories requiring more scrutiny.
	 * 
	 * @param mdmsData       the MDMS risk type computation rules
	 * @param buildingHeight the building height in meters
	 * @param plotArea       the plot area in square meters
	 * @return the calculated risk type (LOW, MEDIUM, HIGH)
	 */
	@SuppressWarnings("unchecked")
	private String calculateRiskType(Object mdmsData, Double buildingHeight, Double plotArea) {
		String filterExp = "$.[?((@.fromPlotArea < " + plotArea + " && @.toPlotArea >= " + plotArea
				+ ") || ( @.fromBuildingHeight < " + buildingHeight + "  &&  @.toBuildingHeight >= " + buildingHeight
				+ "  ))].riskType";

		List<String> riskTypes = JsonPath.read(mdmsData, filterExp);
		return riskTypes.get(0);
	}

	/**
	 * Validates risk type, khata numbers, plot numbers, and occupancy types.
	 * 
	 * <p>
	 * <strong>Validation Rules:</strong>
	 * <ul>
	 * <li><strong>Risk Type:</strong> OC risk cannot exceed BPA risk
	 * <ul>
	 * <li>LOW BPA → OC must be LOW</li>
	 * <li>MEDIUM BPA → OC can be LOW or MEDIUM (not HIGH)</li>
	 * <li>HIGH BPA → OC can be any</li>
	 * </ul>
	 * </li>
	 * <li><strong>Khata Number:</strong> Must match exactly (same property)</li>
	 * <li><strong>Plot Number:</strong> Must match exactly (same land parcel)</li>
	 * <li><strong>Occupancy:</strong> Must match exactly (same usage type)</li>
	 * </ul>
	 * 
	 * @param riskType  list of risk types [OC, BPA]
	 * @param khathaNos list of khata numbers [OC, BPA]
	 * @param plotNos   list of plot numbers [OC, BPA]
	 * @param occupancy list of occupancy types [OC, BPA]
	 * @throws CustomException if any validation fails
	 */
	private void validateRiskKhataplotOccupancyType(ArrayList<String> riskType, ArrayList<String> khathaNos,
			ArrayList<String> plotNos, ArrayList<String> occupancy) {

		// Validate risk type compatibility
		if (riskType.size() > 1) {
			validateRiskTypeCompatibility(riskType.get(0), riskType.get(1));
		}

		// Validate khata number match
		if (khathaNos.size() > 1) {
			validateExactMatch(khathaNos.get(0), khathaNos.get(1), "Khata number", "property identification");
		}

		// Validate plot number match
		if (plotNos.size() > 1) {
			validateExactMatch(plotNos.get(0), plotNos.get(1), "Plot number", "land parcel identification");
		}

		// Validate occupancy type match
		if (occupancy.size() > 1) {
			validateExactMatch(occupancy.get(0), occupancy.get(1), "Occupancy type", "building usage classification");
		}
	}

	/**
	 * Validates risk type compatibility between OC and BPA.
	 * 
	 * @param ocRiskType  the OC risk type
	 * @param bpaRiskType the BPA risk type
	 * @throws CustomException if risk types are incompatible
	 */
	private void validateRiskTypeCompatibility(String ocRiskType, String bpaRiskType) {
		boolean isIncompatible = false;
		String reason = "";

		if ("LOW".equalsIgnoreCase(bpaRiskType)) {
			if (!"LOW".equalsIgnoreCase(ocRiskType)) {
				isIncompatible = true;
				reason = "BPA is LOW risk but OC is " + ocRiskType + " risk";
			}
		} else if ("MEDIUM".equalsIgnoreCase(bpaRiskType)) {
			if ("HIGH".equalsIgnoreCase(ocRiskType)) {
				isIncompatible = true;
				reason = "BPA is MEDIUM risk but OC is HIGH risk";
			}
		}

		if (isIncompatible) {
			throw new CustomException(BPAErrorConstants.INVALID_CREATE,
					String.format("Risk type validation failed: %s. "
							+ "Risk type from BPA EDCR is not matching with the risk type from "
							+ "Occupancy Certificate EDCR. You cannot proceed with the application.", reason));
		}
	}

	/**
	 * Validates exact match between OC and BPA values.
	 * 
	 * @param ocValue      the OC value
	 * @param bpaValue     the BPA value
	 * @param fieldName    the field name for error message
	 * @param fieldPurpose the field purpose description
	 * @throws CustomException if values don't match
	 */
	private void validateExactMatch(String ocValue, String bpaValue, String fieldName, String fieldPurpose) {

		if (!ocValue.equalsIgnoreCase(bpaValue)) {
			throw new CustomException(BPAErrorConstants.INVALID_CREATE,
					String.format(
							"%s from BPA EDCR (%s) does not match with the %s from "
									+ "Occupancy Certificate EDCR (%s). This is required for %s. "
									+ "You cannot proceed with the application.",
							fieldName, bpaValue, fieldName.toLowerCase(), ocValue, fieldPurpose));
		}
	}

}
