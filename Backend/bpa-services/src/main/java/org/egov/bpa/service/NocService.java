package org.egov.bpa.service;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.egov.bpa.config.BPAConfiguration;
import org.egov.bpa.repository.ServiceRequestRepository;
import org.egov.bpa.util.BPAConstants;
import org.egov.bpa.util.BPAErrorConstants;
import org.egov.bpa.web.model.BPA;
import org.egov.bpa.web.model.BPARequest;
import org.egov.bpa.web.model.RequestInfoWrapper;
import org.egov.bpa.web.model.NOC.Noc;
import org.egov.bpa.web.model.NOC.NocRequest;
import org.egov.bpa.web.model.NOC.NocResponse;
import org.egov.bpa.web.model.NOC.Workflow;
import org.egov.bpa.web.model.NOC.enums.ApplicationType;
import org.egov.tracer.model.CustomException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;

import lombok.extern.slf4j.Slf4j;
import net.logstash.logback.encoder.org.apache.commons.lang.StringUtils;

@Service
@Slf4j
public class NocService {

	@Autowired
	private EDCRService edcrService;

	@Autowired
	private BPAConfiguration config;

	@Autowired
	private ServiceRequestRepository serviceRequestRepository;

	@Autowired
	private ObjectMapper mapper;

	/**
	 * Creates NOC (No Objection Certificate) requests for a BPA application.
	 * 
	 * <p>
	 * This method orchestrates the complete NOC creation lifecycle:
	 * <ul>
	 * <li>Determines required NOCs based on application type, service type, and
	 * risk type</li>
	 * <li>Handles bypass NOCs (NOCs that citizen has chosen to bypass/opt-out)</li>
	 * <li>Creates NOC applications for all required NOCs not bypassed</li>
	 * <li>Matches EDCR suggested NOCs with MDMS configured NOCs</li>
	 * </ul>
	 * 
	 * <p>
	 * <strong>Business Logic:</strong>
	 * <ul>
	 * <li><strong>NOC Types:</strong> Different building plans require different
	 * NOCs (Fire NOC, Airport NOC, Environment NOC, etc.)</li>
	 * <li><strong>Bypass Mechanism:</strong> Citizens can bypass certain NOCs by
	 * providing offline approvals or exemption certificates</li>
	 * <li><strong>Risk-Based:</strong> LOW risk applications may have fewer NOC
	 * requirements</li>
	 * <li><strong>EDCR Integration:</strong> EDCR plan suggests required NOCs based
	 * on building design</li>
	 * </ul>
	 * 
	 * @param bpaRequest        the BPA request containing application details
	 * @param mdmsData          the master data from MDMS containing NOC
	 *                          configuration
	 * @param edcrSuggestedNocs list of NOCs suggested by EDCR based on plan
	 *                          scrutiny
	 * @param applicationType   the application type (e.g., BUILDING_PLAN_SCRUTINY)
	 * @param serviceType       the service type (e.g., NEW_CONSTRUCTION,
	 *                          ALTERATION)
	 */
	@SuppressWarnings("unchecked")
	public void createNocRequest(BPARequest bpaRequest, Object mdmsData, List<String> edcrSuggestedNocs,
			String applicationType, String serviceType) {

		BPA bpa = bpaRequest.getBPA();

		log.info("Creating NOC requests for application: {}, applicationType: {}, serviceType: {}",
				bpa.getApplicationNo(), applicationType, serviceType);

		// Step 1: Determine risk type for NOC mapping
		String riskType = determineRiskType(bpa);

		// Step 2: Fetch required NOC types from MDMS
		List<String> requiredNocTypes = fetchRequiredNocTypes(mdmsData, applicationType, serviceType, riskType);

		// Step 3: Extract bypass NOCs from BPA additional details
		Map<String, String> bypassNocs = extractBypassNocs(bpa);

		// Step 4: Get NOC source configuration
		Map<String, String> nocSourceConfig = config.getNocSourceConfig();

		// Step 5: Create NOC applications based on bypass status
		if (!CollectionUtils.isEmpty(bypassNocs)) {
			// Process bypass NOCs (create only those marked as "No")
			createNocFromBypassList(bpaRequest, bypassNocs, nocSourceConfig, applicationType);
		} else {
			// Process standard NOCs (create those matching EDCR suggestions)
			createNocFromRequiredList(bpaRequest, requiredNocTypes, edcrSuggestedNocs, nocSourceConfig,
					applicationType);
		}
	}

	/**
	 * Determines the risk type for NOC mapping.
	 * 
	 * <p>
	 * <strong>Risk Type Logic:</strong>
	 * <ul>
	 * <li><strong>LOW:</strong> Low-risk buildings with fewer NOC requirements</li>
	 * <li><strong>ALL:</strong> Default for medium/high-risk buildings requiring
	 * all NOCs</li>
	 * </ul>
	 * 
	 * @param bpa the BPA application
	 * @return the risk type ("LOW" or "ALL")
	 */
	private String determineRiskType(BPA bpa) {
		String riskType = "ALL";

		if (StringUtils.isEmpty(bpa.getRiskType()) || bpa.getRiskType().equalsIgnoreCase("LOW")) {
			riskType = bpa.getRiskType();
		}

		log.debug("Determined risk type: {} for application: {}", riskType, bpa.getApplicationNo());
		return riskType;
	}

	/**
	 * Fetches required NOC types from MDMS based on application context.
	 * 
	 * <p>
	 * The MDMS NOC mapping configuration determines which NOCs are required based
	 * on:
	 * <ul>
	 * <li>Application type (permit, occupancy certificate)</li>
	 * <li>Service type (new construction, alteration, demolition)</li>
	 * <li>Risk type (LOW vs. ALL)</li>
	 * </ul>
	 * 
	 * @param mdmsData        the master data from MDMS
	 * @param applicationType the application type
	 * @param serviceType     the service type
	 * @param riskType        the risk type
	 * @return list of required NOC types
	 */
	@SuppressWarnings("unchecked")
	private List<String> fetchRequiredNocTypes(Object mdmsData, String applicationType, String serviceType,
			String riskType) {

		String nocPath = BPAConstants.NOCTYPE_REQUIRED_MAP.replace("{1}", applicationType).replace("{2}", serviceType)
				.replace("{3}", riskType);

		List<Object> nocMappingResponse = (List<Object>) JsonPath.read(mdmsData, nocPath);
		List<String> nocTypes = JsonPath.read(nocMappingResponse, "$..type");

		log.debug("Found {} required NOC types from MDMS for applicationType: {}, serviceType: {}, riskType: {}",
				nocTypes != null ? nocTypes.size() : 0, applicationType, serviceType, riskType);

		return nocTypes;
	}

	/**
	 * Extracts bypass NOC configuration from BPA additional details.
	 * 
	 * <p>
	 * <strong>Bypass NOCs:</strong> Citizens can bypass certain NOCs by:
	 * <ul>
	 * <li>Providing offline approval documents</li>
	 * <li>Having exemption certificates</li>
	 * <li>Each NOC type mapped to "Yes" (bypass) or "No" (create)</li>
	 * </ul>
	 * 
	 * @param bpa the BPA application
	 * @return map of NOC types to bypass status (empty if no bypasses)
	 */
	@SuppressWarnings("unchecked")
	private Map<String, String> extractBypassNocs(BPA bpa) {
		Map<String, String> bypassNocs = new HashMap<>();

		Map<String, Object> additionalDetails = (Map<String, Object>) bpa.getAdditionalDetails();

		if (!CollectionUtils.isEmpty(additionalDetails)) {
			bypassNocs = (Map<String, String>) additionalDetails.get(BPAConstants.NOC_BYPASS_DETAILS);
		}

		if (!CollectionUtils.isEmpty(bypassNocs)) {
			log.debug("Found {} bypass NOCs in additional details", bypassNocs.size());
		}

		return bypassNocs;
	}

	/**
	 * Creates NOC applications from bypass list.
	 * 
	 * <p>
	 * Processes each bypass NOC and creates NOC application only if the bypass
	 * value is "No" (meaning citizen chose NOT to bypass and needs NOC).
	 * 
	 * @param bpaRequest      the BPA request
	 * @param bypassNocs      the bypass NOC configuration
	 * @param nocSourceConfig the NOC source configuration
	 * @param applicationType the application type
	 */
	private void createNocFromBypassList(BPARequest bpaRequest, Map<String, String> bypassNocs,
			Map<String, String> nocSourceConfig, String applicationType) {

		BPA bpa = bpaRequest.getBPA();

		for (Map.Entry<String, String> bypassNoc : bypassNocs.entrySet()) {
			if ("No".equalsIgnoreCase(bypassNoc.getValue())) {
				// Citizen chose NOT to bypass - create NOC application
				NocRequest nocRequest = buildNocRequest(bpa, bpaRequest.getRequestInfo(), bypassNoc.getKey(),
						nocSourceConfig.get(applicationType));

				createNocWithErrorHandling(nocRequest);
			}
		}
	}

	/**
	 * Creates NOC applications from required list matched with EDCR suggestions.
	 * 
	 * <p>
	 * Creates NOC applications only for NOC types that are:
	 * <ul>
	 * <li>Required by MDMS configuration</li>
	 * <li>Suggested by EDCR plan scrutiny</li>
	 * <li>Not for BPA_AC (After Construction) applications</li>
	 * </ul>
	 * 
	 * @param bpaRequest        the BPA request
	 * @param requiredNocTypes  the NOC types required by MDMS
	 * @param edcrSuggestedNocs the NOC types suggested by EDCR
	 * @param nocSourceConfig   the NOC source configuration
	 * @param applicationType   the application type
	 */
	private void createNocFromRequiredList(BPARequest bpaRequest, List<String> requiredNocTypes,
			List<String> edcrSuggestedNocs, Map<String, String> nocSourceConfig, String applicationType) {

		BPA bpa = bpaRequest.getBPA();

		if (!CollectionUtils.isEmpty(requiredNocTypes)
				&& !BPAConstants.BPA_AC_MODULE_CODE.equals(bpa.getBusinessService())) {

			for (String nocType : requiredNocTypes) {
				// Create NOC only if suggested by EDCR
				if (edcrSuggestedNocs.contains(nocType)) {
					NocRequest nocRequest = buildNocRequest(bpa, bpaRequest.getRequestInfo(), nocType,
							nocSourceConfig.get(applicationType));

					createNocWithErrorHandling(nocRequest);
				}
			}
		} else {
			log.debug("NOC mapping not found or BPA_AC application - skipping NOC creation");
		}
	}

	/**
	 * Builds a NOC request object.
	 * 
	 * @param bpa         the BPA application
	 * @param requestInfo the request information
	 * @param nocType     the NOC type to create
	 * @param source      the NOC source system
	 * @return the built NOC request
	 */
	private NocRequest buildNocRequest(BPA bpa, org.egov.common.contract.request.RequestInfo requestInfo,
			String nocType, String source) {

		return NocRequest.builder()
				.noc(Noc.builder().tenantId(bpa.getTenantId())
						.applicationType(ApplicationType.valueOf(BPAConstants.NOC_APPLICATIONTYPE))
						.sourceRefId(bpa.getApplicationNo()).nocType(nocType).edcrNumber(bpa.getEdcrNumber())
						.source(source).build())
				.requestInfo(requestInfo).build();
	}

	/**
	 * Creates a NOC application with error handling.
	 * 
	 * <p>
	 * Catches and logs exceptions to ensure that failure to create one NOC doesn't
	 * prevent creation of other NOCs.
	 * 
	 * @param nocRequest the NOC request to create
	 */
	private void createNocWithErrorHandling(NocRequest nocRequest) {
		try {
			createNoc(nocRequest);
		} catch (Exception e) {
			log.error("Failed to create NOC of type {}: {}", nocRequest.getNoc().getNocType(), e.getMessage(), e);
		}
	}

	@SuppressWarnings("unchecked")
	private void createNoc(NocRequest nocRequest) {
		StringBuilder uri = new StringBuilder(config.getNocServiceHost());
		uri.append(config.getNocCreateEndpoint());

		LinkedHashMap<String, Object> responseMap = null;
		try {
			log.debug("Creating NOC application with nocType : " + nocRequest.getNoc().getNocType());
			responseMap = (LinkedHashMap<String, Object>) serviceRequestRepository.fetchResult(uri, nocRequest);
			NocResponse nocResponse = mapper.convertValue(responseMap, NocResponse.class);
			log.debug("NOC created with applicationNo : " + nocResponse.getNoc().get(0).getApplicationNo());
		} catch (Exception se) {
			throw new CustomException(BPAErrorConstants.NOC_SERVICE_EXCEPTION,
					" Failed to create NOC of Type " + nocRequest.getNoc().getNocType());
		}
	}

	@SuppressWarnings("unchecked")
	private void updateNoc(NocRequest nocRequest) {
		StringBuilder uri = new StringBuilder(config.getNocServiceHost());
		uri.append(config.getNocUpdateEndpoint());

		LinkedHashMap<String, Object> responseMap = null;
		try {
			responseMap = (LinkedHashMap<String, Object>) serviceRequestRepository.fetchResult(uri, nocRequest);
			NocResponse nocResponse = mapper.convertValue(responseMap, NocResponse.class);
			log.debug("NOC updated with applicationNo : " + nocResponse.getNoc().get(0).getApplicationNo());
		} catch (Exception se) {
			throw new CustomException(BPAErrorConstants.NOC_SERVICE_EXCEPTION,
					" Failed to update NOC of Type " + nocRequest.getNoc().getNocType());
		}
	}

	@SuppressWarnings("unchecked")
	public List<Noc> fetchNocRecords(BPARequest bpaRequest) {

		StringBuilder url = getNOCWithSourceRef(bpaRequest);

		RequestInfoWrapper requestInfoWrapper = RequestInfoWrapper.builder().requestInfo(bpaRequest.getRequestInfo())
				.build();
		LinkedHashMap<String, Object> responseMap = null;
		try {
			responseMap = (LinkedHashMap<String, Object>) serviceRequestRepository.fetchResult(url, requestInfoWrapper);
			NocResponse nocResponse = mapper.convertValue(responseMap, NocResponse.class);
			return nocResponse.getNoc();
		} catch (Exception e) {
			throw new CustomException(BPAErrorConstants.NOC_SERVICE_EXCEPTION, " Unable to fetch the NOC records");
		}
	}

	/**
	 * fetch the noc records with sourceRefId
	 * 
	 * @param bpaRequest
	 * @return
	 */
	private StringBuilder getNOCWithSourceRef(BPARequest bpaRequest) {
		StringBuilder uri = new StringBuilder(config.getNocServiceHost());
		uri.append(config.getNocSearchEndpoint());
		uri.append("?tenantId=");
		uri.append(bpaRequest.getBPA().getTenantId());
		NocRequest nocRequest = new NocRequest();
		nocRequest.setRequestInfo(bpaRequest.getRequestInfo());
		uri.append("&sourceRefId=");
		uri.append(bpaRequest.getBPA().getApplicationNo());
		return uri;
	}

	/**
	 * Calls the iniate workflow for the applicable noc records
	 * 
	 * @param bpaRequest
	 * @param mdmsData
	 */
	public void initiateNocWorkflow(BPARequest bpaRequest, Object mdmsData) {
		log.debug("====> initiateNocWorkflow");
		List<Noc> nocs = fetchNocRecords(bpaRequest);
		log.debug("====> initiateNocWorkflow = no of noc " + nocs.size());
		initiateNocWorkflow(bpaRequest, mdmsData, nocs);
	}

	/**
	 * Calls the approve offline workflow for the applicable noc records
	 * 
	 * @param bpaRequest
	 * @param mdmsData
	 */
	public void manageOfflineNocs(BPARequest bpaRequest, Object mdmsData) {
		List<Noc> nocs = fetchNocRecords(bpaRequest);
		approveOfflineNoc(bpaRequest, mdmsData, nocs);
	}

	/**
	 * fetches the applicable offline noc's and mark them as approved
	 * 
	 * @param bpaRequest
	 * @param mdmsData
	 * @param nocs
	 */
	@SuppressWarnings("unchecked")
	private void approveOfflineNoc(BPARequest bpaRequest, Object mdmsData, List<Noc> nocs) {
		BPA bpa = bpaRequest.getBPA();
		log.debug(" auto approval of offline noc with bpa status " + bpa.getStatus() + " and "
				+ bpa.getWorkflow().getAction());
		if ((bpa.getStatus().equalsIgnoreCase(BPAConstants.NOCVERIFICATION_STATUS)
				&& bpa.getWorkflow().getAction().equalsIgnoreCase(BPAConstants.ACTION_FORWORD))
				|| (bpa.getStatus().equalsIgnoreCase(BPAConstants.APPROVAL_INPROGRESS)
						&& bpa.getWorkflow().getAction().equalsIgnoreCase(BPAConstants.ACTION_APPROVE))) {
			List<String> statuses = Arrays.asList(config.getNocValidationCheckStatuses().split(","));
			List<String> offlneNocs = (List<String>) JsonPath.read(mdmsData, BPAConstants.NOCTYPE_OFFLINE_MAP);
			log.debug(" auto approval of offline noc with bpa status and no of nocs " + offlneNocs.size()
					+ " noc statuses" + statuses.toString());
			if (!CollectionUtils.isEmpty(nocs)) {
				nocs.forEach(noc -> {
					log.debug(" auto approval of offline noc " + noc.getApplicationNo() + " _"
							+ noc.getApplicationStatus());
					if (offlneNocs.contains(noc.getNocType()) && !statuses.contains(noc.getApplicationStatus())) {
						Workflow workflow = Workflow.builder().action(config.getNocAutoApproveAction()).build();
						noc.setWorkflow(workflow);
						NocRequest nocRequest = NocRequest.builder().noc(noc).requestInfo(bpaRequest.getRequestInfo())
								.build();
						updateNoc(nocRequest);
						log.debug("Offline NOC is Auto-Approved " + noc.getApplicationNo());
					}

				});
			}
		}
	}

	/**
	 * Initiates workflow for NOC applications based on BPA status.
	 * 
	 * <p>
	 * This method orchestrates the NOC workflow initiation lifecycle:
	 * <ul>
	 * <li>Retrieves EDCR details (service type, application type) for NOC trigger
	 * mapping</li>
	 * <li>Determines trigger states from MDMS based on application context</li>
	 * <li>Checks if current BPA status matches trigger state</li>
	 * <li>Initiates workflow for NOCs not already in progress</li>
	 * </ul>
	 * 
	 * <p>
	 * <strong>Business Logic:</strong>
	 * <ul>
	 * <li><strong>NOC Workflow Trigger:</strong> NOCs are initiated at specific BPA
	 * workflow states (e.g., when BPA reaches NOC_VERIFICATION state)</li>
	 * <li><strong>State-Based:</strong> Different application types trigger NOCs at
	 * different states</li>
	 * <li><strong>Pre-Approved Plans (BPA6):</strong> Uses pre-approved plan
	 * details instead of EDCR</li>
	 * <li><strong>Already In Progress:</strong> Skips NOCs that are already
	 * initiated</li>
	 * </ul>
	 * 
	 * @param bpaRequest the BPA request containing application details
	 * @param mdmsData   the master data from MDMS containing NOC trigger
	 *                   configuration
	 * @param nocs       the list of NOC applications to potentially initiate
	 */
	@SuppressWarnings("unchecked")
	private void initiateNocWorkflow(BPARequest bpaRequest, Object mdmsData, List<Noc> nocs) {

		BPA bpa = bpaRequest.getBPA();

		log.info("Initiating NOC workflow for application: {}, status: {}", bpa.getApplicationNo(), bpa.getStatus());

		// Step 1: Retrieve or build EDCR response
		Map<String, String> edcrResponse = retrieveEdcrResponse(bpaRequest);

		// Step 2: Determine trigger states from MDMS
		List<Object> triggerActionStates = fetchTriggerStates(mdmsData, edcrResponse, bpa);

		// Step 3: Check if current status matches trigger state
		if (!shouldTriggerNocWorkflow(triggerActionStates, bpa.getStatus())) {
			log.debug("BPA status {} does not match trigger states - skipping NOC workflow initiation",
					bpa.getStatus());
			return;
		}

		// Step 4: Initiate workflow for applicable NOCs
		initiateNocWorkflowForList(nocs, bpaRequest);
	}

	/**
	 * Retrieves or builds EDCR response containing service type and application
	 * type.
	 * 
	 * <p>
	 * <strong>EDCR Response Sources (priority order):</strong>
	 * <ol>
	 * <li><strong>Pre-Approved Plan (BPA6):</strong> Retrieves from pre-approved
	 * plan master</li>
	 * <li><strong>Cached Response:</strong> Uses already fetched EDCR response from
	 * request</li>
	 * <li><strong>EDCR Service Call:</strong> Fetches from EDCR service</li>
	 * </ol>
	 * 
	 * @param bpaRequest the BPA request
	 * @return map containing serviceType and applicationType
	 */
	private Map<String, String> retrieveEdcrResponse(BPARequest bpaRequest) {
		String businessServices = bpaRequest.getBPA().getBusinessService();
		Map<String, String> edcrResponse = new HashMap<>();

		log.debug("EDCR response in request: {}", bpaRequest.getEdcrResponse());

		// Priority 1: Pre-approved plan for BPA6 applications
		if (StringUtils.isNotEmpty(businessServices) && "BPA6".equals(businessServices)) {
			log.info("Using pre-approved plan details for BPA6 application");
			edcrResponse = edcrService.getEdcrDetailsForPreapprovedPlan(edcrResponse, bpaRequest);
		}
		// Priority 2: Use cached EDCR response if available
		else if (bpaRequest.getEdcrResponse() != null && !CollectionUtils.isEmpty(bpaRequest.getEdcrResponse())) {
			log.info("Using cached EDCR response");
			edcrResponse = bpaRequest.getEdcrResponse();
		}
		// Priority 3: Fetch from EDCR service
		else {
			log.info("Fetching EDCR details from service");
			edcrResponse = edcrService.getEDCRDetails(bpaRequest.getRequestInfo(), bpaRequest.getBPA());
		}

		// Cache the response for subsequent use
		bpaRequest.setEdcrResponse(edcrResponse);

		return edcrResponse;
	}

	/**
	 * Fetches NOC trigger states from MDMS based on application context.
	 * 
	 * <p>
	 * The MDMS NOC trigger state configuration determines at which BPA workflow
	 * state the NOCs should be initiated, based on:
	 * <ul>
	 * <li>Application type (permit, occupancy certificate)</li>
	 * <li>Service type (new construction, alteration, demolition)</li>
	 * <li>Risk type (LOW vs. ALL)</li>
	 * </ul>
	 * 
	 * @param mdmsData     the master data from MDMS
	 * @param edcrResponse the EDCR response containing application context
	 * @param bpa          the BPA application
	 * @return list of trigger states for NOC workflow initiation
	 */
	@SuppressWarnings("unchecked")
	private List<Object> fetchTriggerStates(Object mdmsData, Map<String, String> edcrResponse, BPA bpa) {

		// Determine risk type for mapping
		String riskType = (StringUtils.isEmpty(bpa.getRiskType()) || !bpa.getRiskType().equalsIgnoreCase("LOW")) ? "ALL"
				: bpa.getRiskType();

		// Build JSONPath for MDMS trigger state mapping
		String nocPath = BPAConstants.NOC_TRIGGER_STATE_MAP
				.replace("{1}", edcrResponse.get(BPAConstants.APPLICATIONTYPE))
				.replace("{2}", edcrResponse.get(BPAConstants.SERVICETYPE)).replace("{3}", riskType);

		List<Object> triggerActionStates = (List<Object>) JsonPath.read(mdmsData, nocPath);

		log.debug("Found {} trigger states from MDMS: {}", triggerActionStates != null ? triggerActionStates.size() : 0,
				triggerActionStates);

		return triggerActionStates;
	}

	/**
	 * Determines if NOC workflow should be triggered based on current BPA status.
	 * 
	 * <p>
	 * NOC workflow is triggered only when the BPA application reaches a specific
	 * state configured in MDMS as a trigger state.
	 * 
	 * @param triggerActionStates the list of trigger states from MDMS
	 * @param currentStatus       the current BPA status
	 * @return true if workflow should be triggered, false otherwise
	 */
	private boolean shouldTriggerNocWorkflow(List<Object> triggerActionStates, String currentStatus) {
		if (CollectionUtils.isEmpty(triggerActionStates)) {
			return false;
		}

		return triggerActionStates.get(0).toString().equalsIgnoreCase(currentStatus);
	}

	/**
	 * Initiates workflow for list of NOC applications.
	 * 
	 * <p>
	 * This method processes each NOC and initiates workflow only if:
	 * <ul>
	 * <li>NOC is not already in INPROGRESS status</li>
	 * <li>NOC workflow is in a state that allows initiation</li>
	 * </ul>
	 * 
	 * <p>
	 * <strong>Business Logic:</strong> Once a NOC is initiated, it moves to
	 * INPROGRESS status and the concerned department can start processing it. This
	 * method prevents duplicate initiation by checking current status.
	 * 
	 * @param nocs       the list of NOC applications
	 * @param bpaRequest the BPA request containing request info
	 */
	private void initiateNocWorkflowForList(List<Noc> nocs, BPARequest bpaRequest) {
		if (CollectionUtils.isEmpty(nocs)) {
			log.debug("No NOCs found for workflow initiation");
			return;
		}

		nocs.forEach(noc -> {
			log.debug("Processing NOC: {}, current status: {}", noc.getApplicationNo(), noc.getApplicationStatus());

			// Initiate only if not already in progress
			if (!noc.getApplicationStatus().equalsIgnoreCase(BPAConstants.INPROGRESS_STATUS)) {
				initiateNocWorkflowForSingle(noc, bpaRequest);
			} else {
				log.debug("NOC {} already in INPROGRESS status - skipping initiation", noc.getApplicationNo());
			}
		});
	}

	/**
	 * Initiates workflow for a single NOC application.
	 * 
	 * @param noc        the NOC application to initiate
	 * @param bpaRequest the BPA request containing request info
	 */
	private void initiateNocWorkflowForSingle(Noc noc, BPARequest bpaRequest) {
		// Set workflow action to initiate
		noc.setWorkflow(Workflow.builder().action(config.getNocInitiateAction()).build());

		// Build NOC request
		NocRequest nocRequest = NocRequest.builder().noc(noc).requestInfo(bpaRequest.getRequestInfo()).build();

		// Update NOC to initiate workflow
		updateNoc(nocRequest);

		log.info("NOC workflow initiated successfully for application: {}", noc.getApplicationNo());
	}

	/**
	 * handles the BPA reject and revocate state by voiding the NOC applicable to
	 * BPA
	 * 
	 * @param bpaRequest
	 */
	public void handleBPARejectedStateForNoc(BPARequest bpaRequest) {
		List<Noc> nocs = fetchNocRecords(bpaRequest);
		BPA bpa = bpaRequest.getBPA();

		nocs.forEach(noc -> {
			noc.setWorkflow(Workflow.builder().action(config.getNocVoidAction())
					.comment(bpa.getWorkflow().getComments()).build());
			NocRequest nocRequest = NocRequest.builder().noc(noc).requestInfo(bpaRequest.getRequestInfo()).build();
			updateNoc(nocRequest);
			log.debug("Noc Voided having applicationNo : " + noc.getApplicationNo());

		});
	}

//	private List<String> getStringToList(String data) {
//		List<String> list = new ArrayList<String>();
//		if (data != null) {
//			data = data.replace("[", "").replace("]", "");
//			list = Arrays.asList(data.split(",", -1));
//		}
//		return list;
//	}
}
