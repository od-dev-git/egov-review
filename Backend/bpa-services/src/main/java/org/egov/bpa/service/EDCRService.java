package org.egov.bpa.service;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.egov.bpa.config.BPAConfiguration;
import org.egov.bpa.repository.BPARepository;
import org.egov.bpa.repository.ServiceRequestRepository;
import org.egov.bpa.util.BPAConstants;
import org.egov.bpa.util.BPAErrorConstants;
import org.egov.bpa.validator.MDMSValidator;
import org.egov.bpa.web.model.BPA;
import org.egov.bpa.web.model.BPARequest;
import org.egov.bpa.web.model.BPASearchCriteria;
import org.egov.bpa.web.model.PreapprovedPlan;
import org.egov.bpa.web.model.PreapprovedPlanSearchCriteria;
import org.egov.bpa.web.model.edcr.RequestInfo;
import org.egov.bpa.web.model.edcr.RequestInfoWrapper;
import org.egov.tracer.model.CustomException;
import org.egov.tracer.model.ServiceCallException;
import org.json.JSONObject;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class EDCRService {

	private ServiceRequestRepository serviceRequestRepository;

	private BPAConfiguration config;

	@Autowired
	private MDMSValidator mdmsValidator;

	@Autowired
	BPARepository bpaRepository;

	@Autowired
	private PreapprovedPlanService preapprovedPlanService;

	@Autowired
	public EDCRService(ServiceRequestRepository serviceRequestRepository, BPAConfiguration config) {
		this.serviceRequestRepository = serviceRequestRepository;
		this.config = config;
	}

	/**
	 * Validates the EDCR (Electronic Drawing and Computation Record) plan for a BPA
	 * application.
	 * 
	 * <p>
	 * This method orchestrates the complete EDCR validation lifecycle:
	 * <ul>
	 * <li>Validates no duplicate EDCR numbers exist in active applications</li>
	 * <li>Fetches EDCR details from the EDCR service</li>
	 * <li>Validates EDCR status is "Accepted"</li>
	 * <li>Validates risk type matches EDCR plan</li>
	 * <li>Extracts and returns additional details (service type, application type,
	 * etc.)</li>
	 * </ul>
	 * 
	 * <p>
	 * <strong>Business Logic:</strong>
	 * <ul>
	 * <li><strong>EDCR Number Uniqueness:</strong> Each EDCR can only be used once
	 * for active applications (rejected and revoked applications are excluded)</li>
	 * <li><strong>EDCR Status:</strong> Only "Accepted" EDCR plans can be used for
	 * BPA applications</li>
	 * <li><strong>Risk Type:</strong> Risk type from EDCR plan determines approval
	 * workflow</li>
	 * <li><strong>Service Type:</strong> Defaults to "NEW_CONSTRUCTION" if not
	 * specified in EDCR</li>
	 * </ul>
	 * 
	 * @param request  the BPA request containing application details
	 * @param mdmsData the master data from MDMS for validation
	 * @return map of additional details extracted from EDCR (serviceType,
	 *         applicationType, permitNumber)
	 * @throws CustomException if EDCR validation fails or duplicate EDCR found
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public Map<String, String> validateEdcrPlan(BPARequest request, Object mdmsData) {

		BPA bpa = request.getBPA();
		String edcrNo = bpa.getEdcrNumber();
		String riskType = bpa.getRiskType();

		log.info("Validating EDCR plan: {} for application", edcrNo);

		// Step 1: Validate no duplicate EDCR exists
		validateNoDuplicateEdcr(bpa);

		// Step 2: Fetch EDCR details from service
		LinkedHashMap responseMap = fetchEdcrDetails(bpa, request.getRequestInfo());

		// Step 3: Parse EDCR response
		DocumentContext edcrContext = parseEdcrResponse(responseMap);

		// Step 4: Validate EDCR status is Accepted
		validateEdcrStatus(edcrContext, edcrNo);

		// Step 5: Extract and build additional details
		Map<String, String> additionalDetails = extractAdditionalDetails(bpa, edcrContext);

		// Step 6: Determine and validate risk type
		String expectedRiskType = determineRiskType(edcrContext);

		// Note: OC EDCR validation is currently commented out
		// Map<String, List<String>> masterData =
		// mdmsValidator.getAttributeValues(mdmsData);
		// this.validateOCEdcr(OccupancyTypes, plotAreas, buildingHeights,
		// applicationType,
		// masterData, riskType, expectedRiskType);

		return additionalDetails;
	}

	/**
	 * Validates that no duplicate EDCR number exists in active applications.
	 * 
	 * <p>
	 * <strong>Business Rule:</strong> An EDCR number can only be used once across
	 * all active applications. Applications with status REJECTED or REVOCATED are
	 * excluded as they are considered inactive.
	 * 
	 * @param bpa the BPA application to validate
	 * @throws CustomException if duplicate EDCR found in active application
	 */
	private void validateNoDuplicateEdcr(BPA bpa) {
		BPASearchCriteria criteria = new BPASearchCriteria();
		criteria.setEdcrNumber(bpa.getEdcrNumber());

		List<BPA> existingBpas = bpaRepository.getBPAData(criteria, null);

		if (!CollectionUtils.isEmpty(existingBpas)) {
			for (BPA existingBpa : existingBpas) {
				boolean isActiveApplication = !existingBpa.getStatus().equalsIgnoreCase(BPAConstants.STATUS_REJECTED)
						&& !existingBpa.getStatus().equalsIgnoreCase(BPAConstants.STATUS_REVOCATED);

				if (isActiveApplication) {
					throw new CustomException(BPAErrorConstants.DUPLICATE_EDCR,
							String.format("Application already exists with EDCR Number: %s", bpa.getEdcrNumber()));
				}
			}
		}
	}

	/**
	 * Fetches EDCR details from the EDCR service.
	 * 
	 * @param bpa         the BPA application
	 * @param requestInfo the request information for authentication
	 * @return the EDCR service response map
	 * @throws CustomException if EDCR number is invalid or service call fails
	 */
	private LinkedHashMap fetchEdcrDetails(BPA bpa, org.egov.common.contract.request.RequestInfo requestInfo) {
		StringBuilder uri = new StringBuilder(config.getEdcrHost());
		uri.append(config.getGetPlanEndPoint());
		uri.append("?tenantId=").append(bpa.getTenantId());
		uri.append("&edcrNumber=").append(bpa.getEdcrNumber());

		RequestInfo edcrRequestInfo = new RequestInfo();
		BeanUtils.copyProperties(requestInfo, edcrRequestInfo);

		try {
			LinkedHashMap responseMap = (LinkedHashMap) serviceRequestRepository.fetchResult(uri,
					new RequestInfoWrapper(edcrRequestInfo));

			if (CollectionUtils.isEmpty(responseMap)) {
				throw new CustomException(BPAErrorConstants.EDCR_ERROR,
						"The response from EDCR service is empty or null");
			}

			return responseMap;

		} catch (ServiceCallException se) {
			log.error("EDCR service call failed for EDCR number: {}", bpa.getEdcrNumber(), se);
			throw new CustomException(BPAErrorConstants.EDCR_ERROR,
					String.format("EDCR Number %s is Invalid", bpa.getEdcrNumber()));
		}
	}

	/**
	 * Parses the EDCR service response to a DocumentContext for JSONPath queries.
	 * 
	 * @param responseMap the raw EDCR service response
	 * @return the parsed document context for JSONPath operations
	 */
	private DocumentContext parseEdcrResponse(LinkedHashMap responseMap) {
		String jsonString = new JSONObject(responseMap).toString();
		return JsonPath.using(Configuration.defaultConfiguration()).parse(jsonString);
	}

	/**
	 * Validates that the EDCR status is "Accepted".
	 * 
	 * <p>
	 * <strong>Business Rule:</strong> Only EDCR plans with status "Accepted" can be
	 * used for BPA applications. Draft or rejected EDCRs are not allowed.
	 * 
	 * @param context the EDCR document context
	 * @param edcrNo  the EDCR number (for error messages)
	 * @throws CustomException if EDCR status is not "Accepted"
	 */
	private void validateEdcrStatus(DocumentContext context, String edcrNo) {
		List<String> edcrStatus = context.read("edcrDetail.*.status");

		if (CollectionUtils.isEmpty(edcrStatus) || !edcrStatus.get(0).equalsIgnoreCase("Accepted")) {
			throw new CustomException(BPAErrorConstants.INVALID_EDCR_NUMBER,
					String.format("The EDCR Number %s is not Accepted", edcrNo));
		}
	}

	/**
	 * Extracts additional details from EDCR response.
	 * 
	 * <p>
	 * This method extracts:
	 * <ul>
	 * <li><strong>serviceType:</strong> Type of construction service (defaults to
	 * NEW_CONSTRUCTION)</li>
	 * <li><strong>applicationType:</strong> Type of application (defaults to
	 * permit)</li>
	 * <li><strong>permitNumber:</strong> Existing permit number if this is a
	 * revision/renewal</li>
	 * </ul>
	 * 
	 * @param bpa     the BPA application
	 * @param context the EDCR document context
	 * @return map of extracted additional details
	 */
	private Map<String, String> extractAdditionalDetails(BPA bpa, DocumentContext context) {
		Map<String, String> additionalDetails = bpa.getAdditionalDetails() != null
				? (Map<String, String>) bpa.getAdditionalDetails()
				: new HashMap<>();

		// Extract service type (defaults to NEW_CONSTRUCTION)
		LinkedList<String> serviceType = context.read("edcrDetail.*.applicationSubType");
		if (CollectionUtils.isEmpty(serviceType)) {
			serviceType = new LinkedList<>();
			serviceType.add("NEW_CONSTRUCTION");
		}
		additionalDetails.put(BPAConstants.SERVICETYPE, serviceType.get(0));

		// Extract application type (defaults to permit)
		LinkedList<String> applicationType = context.read("edcrDetail.*.appliactionType");
		if (CollectionUtils.isEmpty(applicationType)) {
			applicationType = new LinkedList<>();
			applicationType.add("permit");
		}
		additionalDetails.put(BPAConstants.APPLICATIONTYPE, applicationType.get(0));

		// Extract permit number if present
		LinkedList<String> permitNumber = context.read("edcrDetail.*.permitNumber");
		if (!CollectionUtils.isEmpty(permitNumber)) {
			additionalDetails.put(BPAConstants.PERMIT_NO, permitNumber.get(0));
		}

		return additionalDetails;
	}

	/**
	 * Determines the expected risk type from EDCR plan information.
	 * 
	 * <p>
	 * <strong>Risk Type Logic:</strong>
	 * <ul>
	 * <li><strong>LOW:</strong> Low risk type for smaller, simpler
	 * constructions</li>
	 * <li><strong>OTHER:</strong> Default risk type for all other cases</li>
	 * </ul>
	 * 
	 * <p>
	 * Risk type determines the approval workflow and scrutiny level required.
	 * 
	 * @param context the EDCR document context
	 * @return the expected risk type constant
	 */
	private String determineRiskType(DocumentContext context) {
		List<String> dcrRiskType = context.read("edcrDetail.*.planDetail.planInformation.riskType");

		String expectedRiskType = BPAConstants.OTHER_RISKTYPE;
		if (!CollectionUtils.isEmpty(dcrRiskType) && dcrRiskType.get(0).equalsIgnoreCase("LOW")) {
			expectedRiskType = BPAConstants.LOW_RISKTYPE;
		}

		return expectedRiskType;
	}

	/**
	 * Validates EDCR plan for BPA application (Version 2 with enhanced features).
	 * 
	 * <p>
	 * This enhanced version supports:
	 * <ul>
	 * <li>OC (Occupancy Certificate) applications outside Sujog system</li>
	 * <li>BPA7 (Fast-track Integrated) applications with special service types</li>
	 * <li>Pre-fetched EDCR data (passed as parameter to avoid duplicate service
	 * calls)</li>
	 * <li>Building plan PDF report validation (base layer and OBPAS reports)</li>
	 * </ul>
	 * 
	 * <p>
	 * <strong>Business Logic:</strong>
	 * <ul>
	 * <li><strong>OC Outside Sujog:</strong> Legacy OC applications bypass
	 * duplicate EDCR and status validation as they existed before the Sujog
	 * system</li>
	 * <li><strong>BPA7 Service Types:</strong> Fast-track applications use
	 * specialized service type codes for streamlined processing</li>
	 * <li><strong>PDF Reports:</strong> Building plan applications require both
	 * base layer and OBPAS (Online Building Plan Approval System) reports</li>
	 * </ul>
	 * 
	 * @param request  the BPA request containing application details
	 * @param mdmsData the master data from MDMS for validation
	 * @param edcr     the pre-fetched EDCR response to avoid duplicate service
	 *                 calls
	 * @return map of additional details extracted from EDCR (serviceType,
	 *         applicationType, permitNumber)
	 * @throws CustomException if EDCR validation fails
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public Map<String, String> validateEdcrPlanV2(BPARequest request, Object mdmsData,
			LinkedHashMap<String, Object> edcr) {

		BPA bpa = request.getBPA();
		String edcrNo = bpa.getEdcrNumber();
		boolean isOCOutsideSujog = bpa.getOCOutsideSujogApplication();

		log.info("Validating EDCR plan V2: {} for application, OC outside Sujog: {}", edcrNo, isOCOutsideSujog);

		// Step 1: Validate no duplicate EDCR exists (skip for OC outside Sujog)
		if (!isOCOutsideSujog) {
			validateNoDuplicateEdcr(bpa);
		}

		// Step 2: Parse EDCR response
		DocumentContext edcrContext = parseEdcrResponse(edcr);

		// Step 3: Validate EDCR status (skip for OC outside Sujog)
		if (!isOCOutsideSujog) {
			validateEdcrStatus(edcrContext, edcrNo);
		}

		// Step 4: Determine service type (BPA7 vs. regular)
		LinkedList<String> serviceType = determineServiceType(bpa, edcrContext);

		// Step 5: Extract application type and permit number
		LinkedList<String> applicationType = extractApplicationType(edcrContext);
		LinkedList<String> permitNumber = extractPermitNumber(edcrContext);

		// Step 6: Build additional details
		Map<String, String> additionalDetails = buildAdditionalDetailsV2(bpa, serviceType, applicationType,
				permitNumber);

		// Step 7: Validate PDF reports for building plan applications
		validatePdfReports(edcrContext, applicationType, edcrNo);

		// Step 8: Determine risk type (skip for OC outside Sujog)
		if (!isOCOutsideSujog) {
			String expectedRiskType = determineRiskType(edcrContext);
			log.debug("Expected risk type for application: {}", expectedRiskType);
		}

		// Note: GIS data enrichment is currently commented out
		// enrichGisCoordinates(edcrContext, bpa);

		return additionalDetails;
	}

	/**
	 * Determines the appropriate service type based on application context.
	 * 
	 * <p>
	 * <strong>Service Type Logic:</strong>
	 * <ul>
	 * <li><strong>BPA7 (Fast-track Integrated):</strong> Uses specialized codes:
	 * <ul>
	 * <li>NEW_CONSTRUCTION → SERVICE_TYPE_FOR_BPA7</li>
	 * <li>ALTERATION → ALTERATION_SERVICE_TYPE_FOR_BPA7</li>
	 * </ul>
	 * </li>
	 * <li><strong>Regular BPA:</strong> Uses service type from EDCR (defaults to
	 * NEW_CONSTRUCTION if not specified)</li>
	 * </ul>
	 * 
	 * @param bpa     the BPA application
	 * @param context the EDCR document context
	 * @return list containing the determined service type
	 */
	private LinkedList<String> determineServiceType(BPA bpa, DocumentContext context) {
		LinkedList<String> serviceType = new LinkedList<>();

		String businessService = bpa.getBusinessService();
		String serviceTypeFromBPA = bpa.getServiceType();

		// Check if this is BPA7 (Fast-track Integrated) application
		boolean isBPA7 = !StringUtils.isEmpty(businessService)
				&& BPAConstants.BPA_FI_ARCH_MODULE_CODE.equalsIgnoreCase(businessService);

		if (isBPA7) {
			// Use specialized BPA7 service types
			if (BPAConstants.NEW_CONSTRUCTION.equalsIgnoreCase(serviceTypeFromBPA)) {
				serviceType.add(BPAConstants.SERVICE_TYPE_FOR_BPA7);
			} else if (BPAConstants.ALTERATION.equalsIgnoreCase(serviceTypeFromBPA)) {
				serviceType.add(BPAConstants.ALTERATION_SERVICE_TYPE_FOR_BPA7);
			}
		} else {
			// Use service type from EDCR
			serviceType = context.read("edcrDetail.*.applicationSubType");
			if (CollectionUtils.isEmpty(serviceType)) {
				serviceType = new LinkedList<>();
				serviceType.add("NEW_CONSTRUCTION");
			}
		}

		return serviceType;
	}

	/**
	 * Extracts application type from EDCR response.
	 * 
	 * @param context the EDCR document context
	 * @return list containing application type (defaults to "permit")
	 */
	private LinkedList<String> extractApplicationType(DocumentContext context) {
		LinkedList<String> applicationType = context.read("edcrDetail.*.appliactionType");

		if (CollectionUtils.isEmpty(applicationType)) {
			applicationType = new LinkedList<>();
			applicationType.add("permit");
		}

		return applicationType;
	}

	/**
	 * Extracts permit number from EDCR response if present.
	 * 
	 * @param context the EDCR document context
	 * @return list containing permit number (may be empty)
	 */
	private LinkedList<String> extractPermitNumber(DocumentContext context) {
		return context.read("edcrDetail.*.permitNumber");
	}

	/**
	 * Builds additional details map with extracted EDCR information.
	 * 
	 * @param bpa             the BPA application
	 * @param serviceType     the determined service type
	 * @param applicationType the extracted application type
	 * @param permitNumber    the extracted permit number
	 * @return map of additional details
	 */
	private Map<String, String> buildAdditionalDetailsV2(BPA bpa, LinkedList<String> serviceType,
			LinkedList<String> applicationType, LinkedList<String> permitNumber) {

		Map<String, String> additionalDetails = bpa.getAdditionalDetails() != null
				? (Map<String, String>) bpa.getAdditionalDetails()
				: new HashMap<>();

		additionalDetails.put(BPAConstants.SERVICETYPE, serviceType.get(0));
		additionalDetails.put(BPAConstants.APPLICATIONTYPE, applicationType.get(0));

		if (!CollectionUtils.isEmpty(permitNumber)) {
			additionalDetails.put(BPAConstants.PERMIT_NO, permitNumber.get(0));
		}

		return additionalDetails;
	}

	/**
	 * Validates that required PDF reports are generated for building plan
	 * applications.
	 * 
	 * <p>
	 * <strong>Business Rule:</strong> Building plan applications must have:
	 * <ul>
	 * <li><strong>Base Layer Report:</strong> Basic DXF to PDF conversion</li>
	 * <li><strong>OBPAS Report:</strong> Online Building Plan Approval System
	 * overlay report</li>
	 * </ul>
	 * 
	 * <p>
	 * Both reports are mandatory for scrutiny and approval process visualization.
	 * 
	 * @param context         the EDCR document context
	 * @param applicationType the application type
	 * @param edcrNo          the EDCR number (for error messages)
	 * @throws CustomException if required reports are missing
	 */
	private void validatePdfReports(DocumentContext context, LinkedList<String> applicationType, String edcrNo) {
		if (CollectionUtils.isEmpty(applicationType)) {
			return;
		}

		// Only validate for building plan applications
		if (!BPAConstants.BUILDING_PLAN.equals(applicationType.get(0))) {
			return;
		}

		LinkedList<String> dxfToPdfBase = context.read("edcrDetail.*.dxfToPdfBase");
		LinkedList<String> dxfToPdfBasePlusObpas = context.read("edcrDetail.*.dxfToPdfBasePlusObpas");

		boolean hasBaseLayer = !CollectionUtils.isEmpty(dxfToPdfBase);
		boolean hasObpasLayer = !CollectionUtils.isEmpty(dxfToPdfBasePlusObpas);

		if (!hasBaseLayer || !hasObpasLayer) {
			throw new CustomException(BPAErrorConstants.EDCR_ERROR,
					String.format("Either the Base Layer Report or both OBPAS and Base Layer Report have not been "
							+ "generated for this EDCR Number: %s", edcrNo));
		}
	}

	public List<String> getEdcrSuggestedRequiredNocs(LinkedHashMap<String, Object> edcr) {
		String jsonString = new JSONObject(edcr).toString();
		DocumentContext context = JsonPath.using(Configuration.defaultConfiguration()).parse(jsonString);
		List<String> nocsType = context.read(BPAConstants.EDCR_SUGGESTED_REQUIRED_NOCs_PATH);

		return nocsType;
	}

	/**
	 * validate the ocEDCR values
	 * 
	 * @param OccupancyTypes
	 * @param plotAreas
	 * @param buildingHeights
	 * @param applicationType
	 * @param masterData
	 * @param riskType
	 */
	private void validateOCEdcr(List<String> OccupancyTypes, List<Double> plotAreas, List<Double> buildingHeights,
			LinkedList<String> applicationType, Map<String, List<String>> masterData, String riskType,
			String expectedRiskType) {
		if (!CollectionUtils.isEmpty(OccupancyTypes) && !CollectionUtils.isEmpty(plotAreas)
				&& !CollectionUtils.isEmpty(buildingHeights)
				&& !applicationType.get(0).equalsIgnoreCase(BPAConstants.BUILDING_PLAN_OC)) {
			Double buildingHeight = Collections.max(buildingHeights);
			String OccupancyType = OccupancyTypes.get(0); // Assuming
															// OccupancyType
															// would be same in
															// the list
			/*
			 * Double plotArea = plotAreas.get(0); List jsonOutput =
			 * JsonPath.read(masterData, BPAConstants.RISKTYPE_COMPUTATION); String
			 * filterExp = "$.[?((@.fromPlotArea < " + plotArea + " && @.toPlotArea >= " +
			 * plotArea + ") || ( @.fromBuildingHeight < " + buildingHeight +
			 * "  &&  @.toBuildingHeight >= " + buildingHeight + "  ))].riskType";
			 * 
			 * List<String> riskTypes = JsonPath.read(jsonOutput, filterExp);
			 * 
			 * if (!CollectionUtils.isEmpty(riskTypes) &&
			 * OccupancyType.equals(BPAConstants.RESIDENTIAL_OCCUPANCY)) {
			 */
			if (OccupancyType.equals(BPAConstants.RESIDENTIAL_OCCUPANCY)) {
				// String expectedRiskType = riskTypes.get(0);

				if (expectedRiskType == null || !expectedRiskType.equals(riskType)) {
					throw new CustomException(BPAErrorConstants.INVALID_RISK_TYPE,
							"The Risk Type is not valid " + riskType);
				}
			} else {
				throw new CustomException(BPAErrorConstants.INVALID_OCCUPANCY,
						"The OccupancyType " + OccupancyType + " is not supported! ");
			}
		}
	}

	/**
	 * Retrieves the EDCR PDF plan report URL for a BPA application.
	 * 
	 * <p>
	 * This method fetches the EDCR plan report URL which contains the visual
	 * representation of the building plan with scrutiny overlays and annotations.
	 * 
	 * <p>
	 * <strong>Business Logic:</strong>
	 * <ul>
	 * <li>The plan report is a PDF document generated by the EDCR service</li>
	 * <li>It contains the complete building plan with scrutiny results</li>
	 * <li>Used for official review, approval, and record-keeping</li>
	 * <li>Returns null if no plan report is available</li>
	 * </ul>
	 * 
	 * @param bpaRequest the BPA request containing application details
	 * @return the plan report URL (null if not available)
	 * @throws CustomException if EDCR number is invalid or service call fails
	 */
	@SuppressWarnings("rawtypes")
	public String getEDCRPdfUrl(BPARequest bpaRequest) {

		BPA bpa = bpaRequest.getBPA();

		log.debug("Fetching EDCR PDF URL for EDCR number: {}", bpa.getEdcrNumber());

		// Fetch EDCR details from service
		LinkedHashMap responseMap = fetchEdcrDetails(bpa, bpaRequest.getRequestInfo());

		// Parse response and extract plan report URL
		DocumentContext context = parseEdcrResponse(responseMap);
		List<String> planReports = context.read("edcrDetail.*.planReport");

		String planReportUrl = CollectionUtils.isEmpty(planReports) ? null : planReports.get(0);

		log.debug("EDCR PDF URL for EDCR number {}: {}", bpa.getEdcrNumber(), planReportUrl);

		return planReportUrl;
	}

	/**
	 * Retrieves the shortened EDCR PDF plan report URL for a BPA application.
	 * 
	 * <p>
	 * This method fetches the shortened version of the EDCR plan report URL which
	 * contains a compressed/simplified version of the building plan for quick
	 * viewing and downloads.
	 * 
	 * <p>
	 * <strong>Business Logic:</strong>
	 * <ul>
	 * <li><strong>Shortened Report:</strong> Optimized version of the plan with
	 * reduced file size</li>
	 * <li>Used for quick preview and mobile/low-bandwidth scenarios</li>
	 * <li>Contains essential plan information without detailed annotations</li>
	 * <li>Returns null if no shortened report is available</li>
	 * </ul>
	 * 
	 * <p>
	 * <strong>Use Cases:</strong>
	 * <ul>
	 * <li>Mobile app quick preview</li>
	 * <li>SMS/Email attachments with size limits</li>
	 * <li>Low-bandwidth citizen portals</li>
	 * <li>Quick reference for officials in the field</li>
	 * </ul>
	 * 
	 * @param bpaRequest the BPA request containing application details
	 * @return the shortened plan report URL (null if not available)
	 * @throws CustomException if EDCR number is invalid or service call fails
	 */
	@SuppressWarnings("rawtypes")
	public String getEDCRShortenedPdfUrl(BPARequest bpaRequest) {

		BPA bpa = bpaRequest.getBPA();

		log.debug("Fetching shortened EDCR PDF URL for EDCR number: {}", bpa.getEdcrNumber());

		// Fetch EDCR details from service (reusing helper method for consistency)
		LinkedHashMap responseMap = fetchEdcrDetails(bpa, bpaRequest.getRequestInfo());

		// Parse response and extract shortened plan report URL
		DocumentContext context = parseEdcrResponse(responseMap);
		List<String> shortenedPlanReports = context.read("edcrDetail.*.shortenedPlanReport");

		String shortenedUrl = CollectionUtils.isEmpty(shortenedPlanReports) ? null : shortenedPlanReports.get(0);

		log.info("Shortened plan report URL for EDCR number {}: {}", bpa.getEdcrNumber(), shortenedUrl);

		return shortenedUrl;
	}

	/**
	 * Retrieves EDCR details for a BPA application including service type and
	 * metadata.
	 * 
	 * <p>
	 * This method fetches comprehensive EDCR details including:
	 * <ul>
	 * <li>Service type (with BPA7 fast-track handling)</li>
	 * <li>Application type</li>
	 * <li>Lift count relaxation status</li>
	 * <li>Permit number (if applicable)</li>
	 * </ul>
	 * 
	 * <p>
	 * <strong>Business Logic:</strong>
	 * <ul>
	 * <li><strong>BPA7 Applications:</strong> Uses specialized service type codes
	 * from additional details or BPA service type field</li>
	 * <li><strong>Regular Applications:</strong> Uses service type from EDCR plan
	 * information</li>
	 * <li><strong>Lift Count Relaxation:</strong> Indicates if building has
	 * relaxation from standard lift requirements (e.g., heritage buildings)</li>
	 * </ul>
	 * 
	 * @param requestInfo the request information for authentication
	 * @param bpa         the BPA application
	 * @return map of EDCR details (serviceType, applicationType,
	 *         liftCountRelaxation, permitNumber)
	 * @throws CustomException if EDCR number is invalid or service call fails
	 */
	@SuppressWarnings("rawtypes")
	public Map<String, String> getEDCRDetails(org.egov.common.contract.request.RequestInfo requestInfo, BPA bpa) {

		log.debug("Fetching EDCR details for EDCR number: {}", bpa.getEdcrNumber());

		// Step 1: Fetch EDCR response from service (reusing helper)
		LinkedHashMap responseMap = fetchEdcrDetails(bpa, requestInfo);

		// Step 2: Parse EDCR response (reusing helper)
		DocumentContext context = parseEdcrResponse(responseMap);

		// Step 3: Determine service type (BPA7 vs. regular)
		List<String> serviceType = determineServiceTypeForDetails(bpa, context);

		// Step 4: Extract application type
		List<String> applicationType = extractApplicationType(context);

		// Step 5: Extract additional details and build response
		return buildEdcrDetailsMap(context, serviceType, applicationType);
	}

	/**
	 * Determines service type for EDCR details retrieval.
	 * 
	 * <p>
	 * <strong>Service Type Resolution Logic:</strong>
	 * <ul>
	 * <li><strong>BPA7 Applications:</strong>
	 * <ol>
	 * <li>First tries to get from additional details (highest priority)</li>
	 * <li>Falls back to BPA service type field</li>
	 * <li>Maps to specialized BPA7 codes</li>
	 * </ol>
	 * </li>
	 * <li><strong>Regular Applications:</strong> Reads from EDCR plan
	 * information</li>
	 * <li><strong>Default:</strong> NEW_CONSTRUCTION if not found</li>
	 * </ul>
	 * 
	 * @param bpa     the BPA application
	 * @param context the EDCR document context
	 * @return list containing the determined service type
	 */
	private List<String> determineServiceTypeForDetails(BPA bpa, DocumentContext context) {
		List<String> serviceType = new LinkedList<>();

		String businessService = bpa.getBusinessService();
		String serviceTypeFromBPA = bpa.getServiceType();

		// Check if this is BPA7 (Fast-track Integrated) application
		boolean isBPA7 = !StringUtils.isEmpty(businessService)
				&& BPAConstants.BPA_FI_ARCH_MODULE_CODE.equalsIgnoreCase(businessService);

		if (isBPA7) {
			// For BPA7, try to get service type from additional details first
			serviceTypeFromBPA = extractServiceTypeFromAdditionalDetails(bpa, serviceTypeFromBPA);

			// Map to specialized BPA7 service type codes
			if (serviceTypeFromBPA.equalsIgnoreCase(BPAConstants.NEW_CONSTRUCTION)
					|| serviceTypeFromBPA.equalsIgnoreCase(BPAConstants.SERVICE_TYPE_FOR_BPA7)) {
				serviceType.add(BPAConstants.SERVICE_TYPE_FOR_BPA7);
			} else if (serviceTypeFromBPA.equalsIgnoreCase(BPAConstants.ALTERATION)
					|| serviceTypeFromBPA.equalsIgnoreCase(BPAConstants.ALTERATION_SERVICE_TYPE_FOR_BPA7)) {
				serviceType.add(BPAConstants.ALTERATION_SERVICE_TYPE_FOR_BPA7);
			}
		} else {
			// For regular applications, read from EDCR plan information
			serviceType = context.read("edcrDetail.*.planDetail.planInformation.serviceType");
			if (CollectionUtils.isEmpty(serviceType)) {
				serviceType = new LinkedList<>();
				serviceType.add("NEW_CONSTRUCTION");
			}
		}

		return serviceType;
	}

	/**
	 * Extracts service type from BPA additional details.
	 * 
	 * <p>
	 * For BPA7 applications, the service type may be stored in additional details
	 * rather than the standard service type field. This method attempts to retrieve
	 * it and falls back to the original service type if not found.
	 * 
	 * @param bpa                the BPA application
	 * @param defaultServiceType the fallback service type
	 * @return the service type from additional details or default
	 */
	@SuppressWarnings("unchecked")
	private String extractServiceTypeFromAdditionalDetails(BPA bpa, String defaultServiceType) {
		try {
			Map<String, Object> additionalDetails = (Map<String, Object>) bpa.getAdditionalDetails();

			if (additionalDetails != null) {
				String serviceTypeFromAdditionalDetails = (String) additionalDetails.get(BPAConstants.SERVICETYPE);

				if (!StringUtils.isEmpty(serviceTypeFromAdditionalDetails)) {
					log.debug("Service type found in additional details: {}", serviceTypeFromAdditionalDetails);
					return serviceTypeFromAdditionalDetails;
				}
			}
		} catch (Exception e) {
			log.error("Error extracting service type from additional details: {}", e.getMessage(), e);
		}

		log.debug("Using default service type: {}", defaultServiceType);
		return defaultServiceType;
	}

	/**
	 * Builds the EDCR details map with all extracted information.
	 * 
	 * <p>
	 * This method assembles the final response map containing:
	 * <ul>
	 * <li><strong>serviceType:</strong> Type of construction service</li>
	 * <li><strong>applicationType:</strong> Type of application (permit,
	 * occupancy)</li>
	 * <li><strong>liftCountRelaxation:</strong> Whether lift requirement is relaxed
	 * (YES/NO)</li>
	 * <li><strong>permitNumber:</strong> Existing permit number (optional)</li>
	 * </ul>
	 * 
	 * @param context         the EDCR document context
	 * @param serviceType     the determined service type
	 * @param applicationType the extracted application type
	 * @return map of EDCR details
	 */
	private Map<String, String> buildEdcrDetailsMap(DocumentContext context, List<String> serviceType,
			List<String> applicationType) {

		Map<String, String> edcrDetails = new HashMap<>();

		// Add service type and application type
		edcrDetails.put(BPAConstants.SERVICETYPE, serviceType.get(0).toString());
		edcrDetails.put(BPAConstants.APPLICATIONTYPE, applicationType.get(0).toString());

		// Extract and add lift count relaxation (defaults to NO)
		List<String> liftCountRelaxation = context.read("edcrDetail.*.planDetail.planInformation.liftCountRelaxation");
		String liftRelaxationValue = CollectionUtils.isEmpty(liftCountRelaxation) ? "NO" : liftCountRelaxation.get(0);
		edcrDetails.put(BPAConstants.LIFTCOUNTRELAXATION, liftRelaxationValue);

		// Extract and add permit number if available
		List<String> approvalNo = context.read("edcrDetail.*.permitNumber");
		if (!CollectionUtils.isEmpty(approvalNo)) {
			edcrDetails.put(BPAConstants.PERMIT_NO, approvalNo.get(0).toString());
		}

		return edcrDetails;
	}

	/**
	 * Retrieves EDCR details response as a DocumentContext for JSONPath queries.
	 * 
	 * <p>
	 * This method fetches the complete EDCR response from the EDCR service and
	 * returns it as a DocumentContext object, enabling flexible JSONPath-based data
	 * extraction by callers.
	 * 
	 * <p>
	 * <strong>Business Logic:</strong>
	 * <ul>
	 * <li>Returns the complete EDCR response for flexible querying</li>
	 * <li>Callers can use JSONPath to extract any field from the EDCR plan</li>
	 * <li>Useful when specific EDCR fields are needed beyond standard
	 * extractions</li>
	 * </ul>
	 * 
	 * <p>
	 * <strong>Use Cases:</strong>
	 * <ul>
	 * <li>Custom plan detail extraction (building heights, plot areas,
	 * occupancy)</li>
	 * <li>Advanced scrutiny result querying</li>
	 * <li>Dynamic field extraction based on application type</li>
	 * </ul>
	 * 
	 * @param requestInfo the request information for authentication
	 * @param bpa         the BPA application
	 * @return DocumentContext for JSONPath-based querying of EDCR response
	 * @throws CustomException if EDCR number is invalid or service call fails
	 */
	public DocumentContext getEDCRDetailsResponse(org.egov.common.contract.request.RequestInfo requestInfo, BPA bpa) {

		log.debug("Fetching EDCR details response as DocumentContext for EDCR number: {}", bpa.getEdcrNumber());

		// Fetch EDCR details from service (reusing helper method for consistency)
		LinkedHashMap responseMap = fetchEdcrDetails(bpa, requestInfo);

		// Parse and return as DocumentContext for JSONPath queries
		return parseEdcrResponse(responseMap);
	}

	/**
	 * Retrieves EDCR details for pre-approved plan applications.
	 * 
	 * <p>
	 * This method handles applications using pre-approved standardized building
	 * plans. Instead of generating a new EDCR, it retrieves details from the
	 * pre-approved plan master database.
	 * 
	 * <p>
	 * <strong>Business Logic:</strong>
	 * <ul>
	 * <li><strong>Pre-Approved Plans:</strong> Standardized building designs that
	 * have been pre-vetted and approved by authorities (e.g., model house
	 * designs)</li>
	 * <li>Citizens can select from catalog of pre-approved plans instead of custom
	 * design</li>
	 * <li>Faster approval process as plan scrutiny is already completed</li>
	 * <li>Drawing number serves as identifier instead of EDCR number</li>
	 * </ul>
	 * 
	 * <p>
	 * <strong>Use Cases:</strong>
	 * <ul>
	 * <li>Affordable housing schemes with standardized designs</li>
	 * <li>Government housing programs (e.g., Pradhan Mantri Awas Yojana)</li>
	 * <li>Rural development with model house designs</li>
	 * <li>Fast-track approvals for standard plans</li>
	 * </ul>
	 * 
	 * @param edcrResponse the EDCR response map to populate with details
	 * @param bpaRequest   the BPA request containing drawing number
	 * @return the EDCR response map populated with service type and application
	 *         type
	 * @throws CustomException if no pre-approved plan found for the drawing number
	 */
	public Map<String, String> getEdcrDetailsForPreapprovedPlan(Map<String, String> edcrResponse,
			BPARequest bpaRequest) {

		String drawingNo = bpaRequest.getBPA().getEdcrNumber();

		log.info("Fetching EDCR details for pre-approved plan with drawing number: {}", drawingNo);

		// Step 1: Search for pre-approved plan by drawing number
		PreapprovedPlan preapprovedPlan = findPreapprovedPlan(drawingNo);

		// Step 2: Extract drawing details from pre-approved plan
		Map<String, Object> drawingDetail = extractDrawingDetails(preapprovedPlan);

		// Step 3: Populate EDCR response with service type and application type
		populateEdcrResponseFromDrawingDetails(edcrResponse, drawingDetail);

		return edcrResponse;
	}

	/**
	 * Finds pre-approved plan by drawing number.
	 * 
	 * <p>
	 * This method searches the pre-approved plan master database for the specified
	 * drawing number and validates that a plan exists.
	 * 
	 * @param drawingNo the drawing number to search for
	 * @return the found pre-approved plan
	 * @throws CustomException if no pre-approved plan found
	 */
	private PreapprovedPlan findPreapprovedPlan(String drawingNo) {
		PreapprovedPlanSearchCriteria criteria = new PreapprovedPlanSearchCriteria();
		criteria.setDrawingNo(drawingNo);

		List<PreapprovedPlan> preapprovedPlans = preapprovedPlanService.getPreapprovedPlanFromCriteria(criteria);

		if (CollectionUtils.isEmpty(preapprovedPlans)) {
			log.error("No pre-approved plan found for drawing number: {}", drawingNo);
			throw new CustomException("PREAPPROVED_PLAN_NOT_FOUND",
					String.format("No pre-approved plan found for drawing number: %s", drawingNo));
		}

		return preapprovedPlans.get(0);
	}

	/**
	 * Extracts drawing details from pre-approved plan.
	 * 
	 * <p>
	 * Drawing details contain the service type and application type that were
	 * associated with the pre-approved plan during its master data creation.
	 * 
	 * @param preapprovedPlan the pre-approved plan
	 * @return map of drawing details
	 */
	@SuppressWarnings("unchecked")
	private Map<String, Object> extractDrawingDetails(PreapprovedPlan preapprovedPlan) {
		return (Map<String, Object>) preapprovedPlan.getDrawingDetail();
	}

	/**
	 * Populates EDCR response map with service type and application type from
	 * drawing details.
	 * 
	 * <p>
	 * This method extracts the service type and application type from the
	 * pre-approved plan's drawing details and adds them to the EDCR response map.
	 * 
	 * <p>
	 * <strong>Typical Values:</strong>
	 * <ul>
	 * <li><strong>serviceType:</strong> NEW_CONSTRUCTION, ALTERATION, etc.</li>
	 * <li><strong>applicationType:</strong> BUILDING_PLAN_SCRUTINY, PERMIT,
	 * etc.</li>
	 * </ul>
	 * 
	 * @param edcrResponse  the EDCR response map to populate
	 * @param drawingDetail the drawing details containing service and application
	 *                      types
	 */
	private void populateEdcrResponseFromDrawingDetails(Map<String, String> edcrResponse,
			Map<String, Object> drawingDetail) {

		// Extract service type (e.g., NEW_CONSTRUCTION)
		String serviceType = String.valueOf(drawingDetail.get("serviceType"));
		edcrResponse.put(BPAConstants.SERVICETYPE, serviceType);

		// Extract application type (e.g., BUILDING_PLAN_SCRUTINY)
		String applicationType = String.valueOf(drawingDetail.get("applicationType"));
		edcrResponse.put(BPAConstants.APPLICATIONTYPE, applicationType);

		log.debug("Pre-approved plan details - serviceType: {}, applicationType: {}", serviceType, applicationType);
	}

	/**
	 * get edcrNumbers from the bpa search criteria
	 * 
	 * @param searchCriteria
	 * @param requestInfo
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	/**
	 * Retrieves list of EDCR numbers based on search criteria.
	 * 
	 * <p>
	 * This method searches for EDCR plans matching the provided criteria and
	 * returns a list of EDCR numbers. This is typically used for:
	 * <ul>
	 * <li>Displaying available EDCR plans to citizens during application
	 * creation</li>
	 * <li>Dropdown/autocomplete population in UI for EDCR selection</li>
	 * <li>Listing all EDCRs for a specific tenant/ULB</li>
	 * <li>EDCR plan inventory and reporting</li>
	 * </ul>
	 * 
	 * <p>
	 * <strong>Business Logic:</strong>
	 * <ul>
	 * <li>Searches EDCR service by tenant ID</li>
	 * <li>Returns all EDCR numbers that match the criteria</li>
	 * <li>Used to help citizens select correct EDCR for their BPA application</li>
	 * <li>Returns null if no EDCRs found (indicating no plans available for
	 * tenant)</li>
	 * </ul>
	 * 
	 * @param searchCriteria the search criteria containing tenant ID
	 * @param requestInfo    the request information for authentication
	 * @return list of EDCR numbers (null if none found)
	 * @throws CustomException if search criteria is invalid or service call fails
	 */
	public List<String> getEDCRNos(BPASearchCriteria searchCriteria,
			org.egov.common.contract.request.RequestInfo requestInfo) {

		log.debug("Searching EDCR numbers for tenant: {}", searchCriteria.getTenantId());

		// Fetch EDCR plans by tenant
		LinkedHashMap responseMap = fetchEdcrPlansByTenant(searchCriteria.getTenantId(), requestInfo);

		// Parse response and extract EDCR numbers
		DocumentContext context = parseEdcrResponse(responseMap);
		List<String> edcrNos = context.read("edcrDetail.*.edcrNumber");

		log.info("Found {} EDCR numbers for tenant: {}", edcrNos != null ? edcrNos.size() : 0,
				searchCriteria.getTenantId());

		return CollectionUtils.isEmpty(edcrNos) ? null : edcrNos;
	}

	/**
	 * Fetches EDCR plans by tenant ID.
	 * 
	 * <p>
	 * This method calls the EDCR service to retrieve all plans for a specific
	 * tenant.
	 * 
	 * @param tenantId    the tenant ID to search for
	 * @param requestInfo the request information for authentication
	 * @return the EDCR service response map
	 * @throws CustomException if search criteria is invalid or service call fails
	 */
	@SuppressWarnings("rawtypes")
	private LinkedHashMap fetchEdcrPlansByTenant(String tenantId,
			org.egov.common.contract.request.RequestInfo requestInfo) {

		StringBuilder uri = new StringBuilder(config.getEdcrHost());
		uri.append(config.getGetPlanEndPoint());
		uri.append("?tenantId=").append(tenantId);

		RequestInfo edcrRequestInfo = new RequestInfo();
		BeanUtils.copyProperties(requestInfo, edcrRequestInfo);

		try {
			return (LinkedHashMap) serviceRequestRepository.fetchResult(uri, new RequestInfoWrapper(edcrRequestInfo));
		} catch (ServiceCallException se) {
			log.error("Failed to fetch EDCR plans for tenant: {}", tenantId, se);
			throw new CustomException(BPAErrorConstants.EDCR_ERROR,
					String.format("Invalid search criteria for tenant: %s", tenantId));
		}
	}

	/**
	 * Retrieves complete EDCR details as a raw response map for a BPA application.
	 * 
	 * <p>
	 * This method fetches the complete EDCR response from the EDCR service and
	 * returns it as a LinkedHashMap, providing the raw response for custom
	 * processing.
	 * 
	 * <p>
	 * <strong>Business Logic:</strong>
	 * <ul>
	 * <li>Returns the complete raw EDCR response without parsing</li>
	 * <li>Useful when the caller needs to access EDCR fields not covered by
	 * standard extraction</li>
	 * <li>Provides flexibility for custom EDCR data processing</li>
	 * </ul>
	 * 
	 * <p>
	 * <strong>Use Cases:</strong>
	 * <ul>
	 * <li>Custom integrations requiring full EDCR response</li>
	 * <li>Legacy code requiring raw LinkedHashMap response</li>
	 * <li>Special processing of EDCR data not covered by standard methods</li>
	 * </ul>
	 * 
	 * @param bpaRequest the BPA request containing application details
	 * @return the raw EDCR service response as LinkedHashMap
	 * @throws CustomException if EDCR number is invalid or service call fails
	 */
	@SuppressWarnings("rawtypes")
	public LinkedHashMap getEDCRDetails(BPARequest bpaRequest) {

		log.debug("Fetching raw EDCR details for EDCR number: {}", bpaRequest.getBPA().getEdcrNumber());

		// Reuse the helper method for consistency and DRY principle
		return fetchEdcrDetails(bpaRequest.getBPA(), bpaRequest.getRequestInfo());
	}

}