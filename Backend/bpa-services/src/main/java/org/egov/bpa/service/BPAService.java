package org.egov.bpa.service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.validation.Valid;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDPageTree;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.egov.bpa.config.BPAConfiguration;
import org.egov.bpa.repository.BPARepository;
import org.egov.bpa.repository.IssueFixRepository;
import org.egov.bpa.repository.RevalidationRepository;
import org.egov.bpa.repository.ScnRepository;
import org.egov.bpa.service.notification.BPANotificationService;
import org.egov.bpa.util.BPAConstants;
import org.egov.bpa.util.BPAErrorConstants;
import org.egov.bpa.util.BPAUtil;
import org.egov.bpa.util.EdcrUtil;
import org.egov.bpa.util.IssueFixConstants;
import org.egov.bpa.util.NotificationUtil;
import org.egov.bpa.util.RegularizationConstants;
import org.egov.bpa.validator.BPAValidator;
import org.egov.bpa.web.model.AuditDetails;
import org.egov.bpa.web.model.BPA;
import org.egov.bpa.web.model.BPADocUploadRequest;
import org.egov.bpa.web.model.BPADraft;
import org.egov.bpa.web.model.BPADraftRequest;
import org.egov.bpa.web.model.BPADraftSearchCriteria;
import org.egov.bpa.web.model.BPARequest;
import org.egov.bpa.web.model.BPASearchCriteria;
import org.egov.bpa.web.model.BPAVillage;
import org.egov.bpa.web.model.BpaApplicationSearch;
import org.egov.bpa.web.model.BpaApprovedByApplicationSearch;
import org.egov.bpa.web.model.CompletionCertificate;
import org.egov.bpa.web.model.CompletionCertificateRequest;
import org.egov.bpa.web.model.CompletionCertificateSearchCriteria;
import org.egov.bpa.web.model.DocUploadRequest;
import org.egov.bpa.web.model.Document;
import org.egov.bpa.web.model.DocumentList;
import org.egov.bpa.web.model.DscDetails;
import org.egov.bpa.web.model.FeePendingApplication;
import org.egov.bpa.web.model.FieldInspection;
import org.egov.bpa.web.model.FieldInspectionRequest;
import org.egov.bpa.web.model.FieldInspectionSearchCriteria;
import org.egov.bpa.web.model.Installment;
import org.egov.bpa.web.model.Notice;
import org.egov.bpa.web.model.NoticeRequest;
import org.egov.bpa.web.model.NoticeSearchCriteria;
import org.egov.bpa.web.model.PlanningAssistantChecklist;
import org.egov.bpa.web.model.PlanningAssistantChecklistRequest;
import org.egov.bpa.web.model.PlanningAssistantSearchCriteria;
import org.egov.bpa.web.model.PlinthApproval;
import org.egov.bpa.web.model.PlinthApprovalRequest;
import org.egov.bpa.web.model.PlinthApprovalSearchCriteria;
import org.egov.bpa.web.model.PreapprovedPlan;
import org.egov.bpa.web.model.PreapprovedPlanSearchCriteria;
import org.egov.bpa.web.model.Revalidation;
import org.egov.bpa.web.model.RevalidationRequest;
import org.egov.bpa.web.model.RevalidationSearchCriteria;
import org.egov.bpa.web.model.RevisionRequest;
import org.egov.bpa.web.model.StageWiseReport;
import org.egov.bpa.web.model.StageWiseReportRequest;
import org.egov.bpa.web.model.StageWiseReportSearchCriteria;
import org.egov.bpa.web.model.VillageSearchCriteria;
import org.egov.bpa.web.model.collection.Demand;
import org.egov.bpa.web.model.landInfo.LandInfo;
import org.egov.bpa.web.model.landInfo.LandSearchCriteria;
import org.egov.bpa.web.model.regularization.Regularization;
import org.egov.bpa.web.model.user.UserDetailResponse;
import org.egov.bpa.web.model.user.UserSearchRequest;
import org.egov.bpa.web.model.workflow.BusinessService;
import org.egov.bpa.workflow.ActionValidator;
import org.egov.bpa.workflow.WorkflowIntegrator;
import org.egov.bpa.workflow.WorkflowService;
import org.egov.common.contract.request.RequestInfo;
import org.egov.common.contract.request.Role;
import org.egov.tracer.model.CustomException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;

import lombok.extern.slf4j.Slf4j;
import net.logstash.logback.encoder.org.apache.commons.lang.StringUtils;

@Service
@Slf4j
public class BPAService {

	@Autowired
	private WorkflowIntegrator wfIntegrator;

	@Autowired
	private EnrichmentService enrichmentService;

	@Autowired
	private ScnRepository noticeRepository;

	@Autowired
	private EDCRService edcrService;

	@Autowired
	private BPARepository repository;

	@Autowired
	private ActionValidator actionValidator;

	@Autowired
	private BPAValidator bpaValidator;

	@Autowired
	private BPAUtil util;

	@Autowired
	private CalculationService calculationService;

	@Autowired
	private WorkflowService workflowService;

	@Autowired
	private NotificationUtil notificationUtil;

	@Autowired
	private BPALandService landService;

	@Autowired
	private OCService ocService;

	@Autowired
	private UserService userService;

	@Autowired
	private NocService nocService;

	@Autowired
	private BPAConfiguration config;

	@Autowired
	private FileStoreService fileStoreService;

	@Autowired
	private PreapprovedPlanService preapprovedPlanService;

	@Autowired
	private RevisionService revisionService;

	@Autowired
	private RevalidationService revalidationService;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private EdcrUtil edcrUtil;

	@Autowired
	private RevalidationRepository revalidationRepository;

	@Autowired
	private IssueFixRepository issueFixRepository;

	@Autowired
	private ScnRepository scnRepository;
	
	@Autowired
	private BPANotificationService notificationService;

	/**
	 * does all the validations required to create BPA Record in the system
	 * 
	 * @param bpaRequest
	 * @return
	 */
	public BPA create(BPARequest bpaRequest) {
		log.info("Inside BPA create method : " + String.valueOf(bpaRequest.getBPA().getApplicationNo()) + "---"
				+ String.valueOf(bpaRequest.getBPA().getAdditionalDetails()));

		// Step 1: Validate tenant and handle revalidation applications
		RequestInfo requestInfo = bpaRequest.getRequestInfo();
		String tenantId = extractBaseTenantId(bpaRequest.getBPA().getTenantId());
		Object mdmsData = util.mDMSCall(requestInfo, tenantId);

		BPA revalidatedBpa = validateTenantAndHandleRevalidation(bpaRequest);
		if (revalidatedBpa != null) {
			return revalidatedBpa;
		}

		// Step 2: Process EDCR details and extract application metadata
		EdcrProcessingResult edcrResult = processEdcrDetails(bpaRequest, mdmsData);

		// Step 3: Validate and enrich BPA application
		validateAndEnrichBPA(bpaRequest, mdmsData, edcrResult);

		// Step 4: Process workflow and dependencies (NOC, fees, draft)
		processWorkflowAndDependencies(bpaRequest, mdmsData, edcrResult);

		// Step 5: Persist the BPA application
		return persistBPA(bpaRequest);
	}

	/**
	 * Extracts base tenant ID from full tenant ID
	 * 
	 * @param fullTenantId
	 * @return base tenant ID
	 */
	private String extractBaseTenantId(String fullTenantId) {
		return fullTenantId.split("\\.")[0];
	}

	/**
	 * Validates tenant level and handles revalidation applications
	 * 
	 * @param bpaRequest
	 * @return BPA if revalidation application, null otherwise
	 */
	private BPA validateTenantAndHandleRevalidation(BPARequest bpaRequest) {
		// Validate tenant is not at state level
		if (bpaRequest.getBPA().getTenantId().split("\\.").length == 1) {
			throw new CustomException(BPAErrorConstants.INVALID_TENANT, " Application cannot be create at StateLevel");
		}

		// Handle revalidation applications with early return
		if (bpaRequest.getBPA().isRevalidationApplication()) {
			BPARequest request = createRevalidation(bpaRequest);
			return request.getBPA();
		}

		// Reset approval number as it should be generated at approve stage
		if (!StringUtils.isEmpty(bpaRequest.getBPA().getApprovalNo())) {
			bpaRequest.getBPA().setApprovalNo(null);
		}

		return null;
	}

	/**
	 * Processes EDCR details based on application type
	 * 
	 * @param bpaRequest
	 * @param mdmsData
	 * @return EdcrProcessingResult containing EDCR and extracted values
	 */
	private EdcrProcessingResult processEdcrDetails(BPARequest bpaRequest, Object mdmsData) {
		LinkedHashMap<String, Object> edcr = new LinkedHashMap<>();
		Map<String, String> values = new HashMap<>();
		String businessService = bpaRequest.getBPA().getBusinessService();

		if (bpaRequest.getBPA().getOCOutsideSujogApplication()) {
			// Process OC Outside Sujog applications
			processOCOutsideSujogEdcr(bpaRequest, mdmsData, edcr, values);
		} else if (StringUtils.isNotEmpty(businessService) && "BPA6".equals(businessService)) {
			// Process pre-approved plan applications
			setEdcrDetailsForPreapprovedPlan(values, edcr, bpaRequest);
		} else {
			// Process standard EDCR applications
			processStandardEdcr(bpaRequest, mdmsData, edcr, values);
		}

		String applicationType = values.get(BPAConstants.APPLICATIONTYPE);
		String serviceType = values.get(BPAConstants.SERVICETYPE);

		return new EdcrProcessingResult(edcr, values, applicationType, serviceType);
	}

	/**
	 * Processes EDCR for OC Outside Sujog applications
	 * 
	 * @param bpaRequest
	 * @param mdmsData
	 * @param edcr
	 * @param values
	 */
	private void processOCOutsideSujogEdcr(BPARequest bpaRequest, Object mdmsData, LinkedHashMap<String, Object> edcr,
			Map<String, String> values) {
		enrichmentService.enrichOCOutsideEdcrDetailsFromRequest(bpaRequest);
		enrichmentService.enrichUUIDInScrutinyDetails(bpaRequest);
		Boolean isSpecialBuilding = enrichmentService.isSpecialBuilding(bpaRequest);
		enrichmentService.enrichOCBusinessService(bpaRequest, isSpecialBuilding);

		if (bpaRequest.getBPA().getPermitEdcrDetail().getEdcrDetail() == null) {
			throw new CustomException("EDCR_DETAIL_NOT_FOUND", "Edcr Detail is not found for OC Outside Sujog");
		}

		edcr.putAll(objectMapper.convertValue(bpaRequest.getBPA().getPermitEdcrDetail(), LinkedHashMap.class));
		values.putAll(edcrService.validateEdcrPlanV2(bpaRequest, mdmsData, edcr));
	}

	/**
	 * Processes standard EDCR applications
	 * 
	 * @param bpaRequest
	 * @param mdmsData
	 * @param edcr
	 * @param values
	 */
	private void processStandardEdcr(BPARequest bpaRequest, Object mdmsData, LinkedHashMap<String, Object> edcr,
			Map<String, String> values) {
		edcr.putAll(edcrService.getEDCRDetails(bpaRequest));
		log.info("EDCR responce successfull");
		values.putAll(edcrService.validateEdcrPlanV2(bpaRequest, mdmsData, edcr));
	}

	/**
	 * Validates and enriches BPA application
	 * 
	 * @param bpaRequest
	 * @param mdmsData
	 * @param edcrResult
	 */
	private void validateAndEnrichBPA(BPARequest bpaRequest, Object mdmsData, EdcrProcessingResult edcrResult) {
		RequestInfo requestInfo = bpaRequest.getRequestInfo();

		// Validate OC creation if not OC Outside Sujog
		if (!bpaRequest.getBPA().getOCOutsideSujogApplication()) {
			this.validateCreateOC(edcrResult.applicationType, edcrResult.values, requestInfo, bpaRequest);
		}

		// Perform BPA validation
		bpaValidator.validateCreate(bpaRequest, mdmsData, edcrResult.values);

		// Add land information (skip for OC inside Sujog applications)
		if (shouldAddLandInfo(bpaRequest, edcrResult.applicationType)) {
			landService.addLandInfoToBPA(bpaRequest);
		}

		// Enrich BPA request with calculated values, IDs, and audit details
		log.info("bpaRequest before enrichBPACreateRequestV2 " + bpaRequest);
		enrichmentService.enrichBPACreateRequestV2(bpaRequest, mdmsData, edcrResult.values, edcrResult.edcr);
		edcrUtil.setEdcrDetails(edcrResult.edcr, bpaRequest);
		log.info("edcr request " + bpaRequest.getBPA().getEdcrData());
		log.info("bpaRequest after enrichBPACreateRequestV2 " + bpaRequest);
	}

	/**
	 * Determines if land information should be added
	 * 
	 * @param bpaRequest
	 * @param applicationType
	 * @return true if land info should be added
	 */
	private boolean shouldAddLandInfo(BPARequest bpaRequest, String applicationType) {
		return !(applicationType.equalsIgnoreCase(BPAConstants.BUILDING_PLAN_OC)
				&& !bpaRequest.getBPA().getOCOutsideSujogApplication());
	}

	/**
	 * Processes workflow and related dependencies (NOC, fees, draft)
	 * 
	 * @param bpaRequest
	 * @param mdmsData
	 * @param edcrResult
	 */
	private void processWorkflowAndDependencies(BPARequest bpaRequest, Object mdmsData,
			EdcrProcessingResult edcrResult) {
		// Create revision record if present
		createRevision(bpaRequest);

		// Initiate workflow
		wfIntegrator.callWorkFlow(bpaRequest);

		// Create NOC requests
		nocService.createNocRequest(bpaRequest, mdmsData, edcrService.getEdcrSuggestedRequiredNocs(edcrResult.edcr),
				edcrResult.applicationType, edcrResult.serviceType);

		// Calculate application fees
		calculationService.addCalculationV2(bpaRequest, BPAConstants.APPLICATION_FEE_KEY, edcrResult.applicationType,
				edcrResult.serviceType);

		// Handle BPA draft
		handleBPADraft(bpaRequest);
	}

	/**
	 * Persists BPA application to database
	 * 
	 * @param bpaRequest
	 * @return persisted BPA
	 */
	private BPA persistBPA(BPARequest bpaRequest) {
		log.info("bpaRequest before create : " + String.valueOf(bpaRequest.getBPA().getApplicationNo()) + "---"
				+ String.valueOf(bpaRequest.getBPA().getAdditionalDetails()));
		repository.save(bpaRequest);
		return bpaRequest.getBPA();
	}

	/**
	 * Inner class to hold EDCR processing results
	 */
	private static class EdcrProcessingResult {
		private final LinkedHashMap<String, Object> edcr;
		private final Map<String, String> values;
		private final String applicationType;
		private final String serviceType;

		public EdcrProcessingResult(LinkedHashMap<String, Object> edcr, Map<String, String> values,
				String applicationType, String serviceType) {
			this.edcr = edcr;
			this.values = values;
			this.applicationType = applicationType;
			this.serviceType = serviceType;
		}
	}

	/**
	 * Handles the lifecycle of BPA draft records when a BPA application is
	 * submitted.
	 * 
	 * <p>
	 * This method performs the following operations:
	 * <ul>
	 * <li>Determines the appropriate EDCR number based on business service type (OC
	 * vs regular BPA)</li>
	 * <li>Searches for existing draft records matching the EDCR number</li>
	 * <li>Marks the draft as deleted if found, linking it to the submitted
	 * application</li>
	 * </ul>
	 * 
	 * <p>
	 * <strong>Business Logic:</strong> When a BPA application is submitted from a
	 * draft, the original draft record should be marked as deleted to prevent
	 * duplicate submissions and maintain the link between the draft and the final
	 * application.
	 * 
	 * @param bpaRequest the BPA request containing application details and request
	 *                   info
	 */
	private void handleBPADraft(@Valid BPARequest bpaRequest) {
		BPA bpa = bpaRequest.getBPA();

		// Step 1: Build search criteria based on business service type
		BPADraftSearchCriteria searchCriteria = buildDraftSearchCriteria(bpa);

		// Step 2: Retrieve existing draft records
		List<BPADraft> existingDrafts = repository.getBPADraftData(searchCriteria);

		// Step 3: Mark draft as deleted if found
		if (!ObjectUtils.isEmpty(existingDrafts)) {
			markDraftAsDeleted(existingDrafts.get(0), bpaRequest);
		}
	}

	/**
	 * Builds the search criteria for finding the BPA draft record.
	 * 
	 * <p>
	 * The search criteria differs based on the business service type:
	 * <ul>
	 * <li><strong>OC (Occupancy Certificate) Module:</strong> Uses the draft number
	 * from additional details if available</li>
	 * <li><strong>Regular BPA Module:</strong> Uses the EDCR number directly</li>
	 * </ul>
	 * 
	 * @param bpa the BPA object containing business service and EDCR details
	 * @return BPADraftSearchCriteria with appropriate EDCR number set
	 */
	private BPADraftSearchCriteria buildDraftSearchCriteria(BPA bpa) {
		String edcrNumber;

		// Check if this is an OC (Occupancy Certificate) application
		if (bpa.getBusinessService().contains(BPAConstants.BPA_OC_MODULE_CODE)) {
			edcrNumber = extractDraftNumberFromOCApplication(bpa);
		} else {
			// For regular BPA applications, use the EDCR number directly
			edcrNumber = bpa.getEdcrNumber();
		}

		return BPADraftSearchCriteria.builder().edcrNo(edcrNumber).build();
	}

	/**
	 * Extracts the draft number from an Occupancy Certificate (OC) application.
	 * 
	 * <p>
	 * For OC applications, the draft number is stored in the additional details
	 * under the "draftNo" key. This method safely extracts the draft number,
	 * handling cases where additional details may be null or empty.
	 * 
	 * @param bpa the BPA object containing additional details
	 * @return the draft number from additional details, or null if not found
	 */
	private String extractDraftNumberFromOCApplication(BPA bpa) {
		Map<String, Object> additionalDetails = (Map<String, Object>) bpa.getAdditionalDetails();

		// Return null if additional details are not present
		if (ObjectUtils.isEmpty(additionalDetails)) {
			return null;
		}

		// Extract and return the draft number
		String draftNo = (String) additionalDetails.get("draftNo");
		return (draftNo != null && !StringUtils.isEmpty(draftNo)) ? draftNo : null;
	}

	/**
	 * Marks a BPA draft as deleted and links it to the submitted application.
	 * 
	 * <p>
	 * This method updates the draft status to "DELETED" and associates it with the
	 * application number of the submitted BPA. The updated draft is then persisted
	 * to the database.
	 * 
	 * <p>
	 * <strong>Note:</strong> The draft is not physically deleted but marked with a
	 * deleted status to maintain an audit trail and prevent reuse.
	 * 
	 * @param draft      the BPA draft to be marked as deleted
	 * @param bpaRequest the BPA request containing the application number and
	 *                   request info
	 */
	private void markDraftAsDeleted(BPADraft draft, BPARequest bpaRequest) {
		// Update draft status to deleted
		draft.setStatus(BPAConstants.BPA_DELETED);

		// Link draft to the submitted application
		draft.setBpaApplicationNo(bpaRequest.getBPA().getApplicationNo());

		// Build request and save to database
		BPADraftRequest draftRequest = BPADraftRequest.builder().bpaDraft(draft)
				.requestInfo(bpaRequest.getRequestInfo()).build();

		repository.save(draftRequest);
	}

	private void createRevision(BPARequest bpaRequest) {
		if (Objects.nonNull(bpaRequest.getRevision())) {
			try {
				log.info("inside method createRevision with revision data: "
						+ objectMapper.writeValueAsString(bpaRequest.getRevision()));
			} catch (JsonProcessingException e) {
				log.error("JsonProcessingException while logging revision data", e);
			}
			RevisionRequest revisionRequest = new RevisionRequest();
			revisionRequest.setRequestInfo(bpaRequest.getRequestInfo());
			bpaRequest.getRevision().setBpaApplicationNo(bpaRequest.getBPA().getApplicationNo());
			bpaRequest.getRevision().setBpaApplicationId(bpaRequest.getBPA().getId());
			revisionRequest.setRevision(bpaRequest.getRevision());
			revisionService.create(revisionRequest);
		}
	}

	/**
	 * Sets EDCR (E-District Common Repository) details for a preapproved plan in
	 * the BPA request.
	 * 
	 * <p>
	 * This method performs the following operations:
	 * <ul>
	 * <li>Retrieves the preapproved plan based on the drawing number from EDCR</li>
	 * <li>Populates service type, application type, and required NOCs from the
	 * preapproved plan</li>
	 * <li>Constructs the EDCR detail structure matching the scrutiny details
	 * format</li>
	 * </ul>
	 * 
	 * <p>
	 * <strong>Note:</strong> Service type and application type are mandatory and
	 * must be populated from the preapproved plan as they cannot be read from the
	 * UI. These values are used in downstream processing and should not be removed.
	 * 
	 * @param values     the map to populate with service type, application type,
	 *                   and other extracted values
	 * @param edcr       the EDCR map to be populated with plan information
	 *                   structure
	 * @param bpaRequest the BPA request containing the drawing number (EDCR number)
	 * @throws CustomException if no preapproved plan is found for the provided
	 *                         drawing number
	 */
	private void setEdcrDetailsForPreapprovedPlan(Map<String, String> values, LinkedHashMap<String, Object> edcr,
			BPARequest bpaRequest) {

		// Step 1: Retrieve preapproved plan from database
		PreapprovedPlan preapprovedPlan = retrievePreapprovedPlan(bpaRequest.getBPA().getEdcrNumber());

		// Step 2: Extract drawing details from preapproved plan
		Map<String, Object> drawingDetail = (Map<String, Object>) preapprovedPlan.getDrawingDetail();

		// Step 3: Populate values map with service type, application type, and permit
		// number
		populateValuesFromDrawingDetail(values, drawingDetail);

		// Step 4: Build EDCR structure with plan information
		List<String> requiredNOCs = extractRequiredNOCs(drawingDetail);
		buildEdcrStructure(edcr, bpaRequest.getBPA().getBusinessService(), requiredNOCs);
	}

	/**
	 * Retrieves the preapproved plan from the database based on the drawing number.
	 * 
	 * @param drawingNo the drawing number (EDCR number) to search for
	 * @return the retrieved PreapprovedPlan object
	 * @throws CustomException if no preapproved plan is found for the provided
	 *                         drawing number
	 */
	private PreapprovedPlan retrievePreapprovedPlan(String drawingNo) {
		// Build search criteria with drawing number
		PreapprovedPlanSearchCriteria searchCriteria = new PreapprovedPlanSearchCriteria();
		searchCriteria.setDrawingNo(drawingNo);

		// Fetch preapproved plans from database
		List<PreapprovedPlan> preapprovedPlans = preapprovedPlanService.getPreapprovedPlanFromCriteria(searchCriteria);

		// Validate that at least one preapproved plan exists
		if (CollectionUtils.isEmpty(preapprovedPlans)) {
			log.error("No preapproved plan found for provided drawing number: {}", drawingNo);
			throw new CustomException("no preapproved plan found for provided drawingNo",
					"no preapproved plan found for provided drawingNo");
		}

		return preapprovedPlans.get(0);
	}

	/**
	 * Populates the values map with service type, application type, and permit
	 * number from drawing details.
	 * 
	 * <p>
	 * <strong>Important:</strong> Service type and application type are mandatory
	 * fields that must be populated from the preapproved plan. They are not
	 * available from the UI and are critical for downstream processing. Do not
	 * remove these mappings.
	 * 
	 * @param values        the map to populate with extracted values
	 * @param drawingDetail the drawing detail map containing service type and
	 *                      application type
	 */
	private void populateValuesFromDrawingDetail(Map<String, String> values, Map<String, Object> drawingDetail) {
		// Populate service type (e.g., NEW_CONSTRUCTION)
		// Note: Concatenation with empty string converts null/Object to String safely
		values.put(BPAConstants.SERVICETYPE, drawingDetail.get("serviceType") + "");

		// Populate application type (e.g., BUILDING_PLAN_SCRUTINY)
		values.put(BPAConstants.APPLICATIONTYPE, drawingDetail.get("applicationType") + "");

		// TODO: Replace hardcoded permit number logic with actual condition check
		// Temporarily hardcoded - needs business rule implementation
		if ("someConditionToCheckPermitNoShouldBePopulated".equals("")) {
			values.put(BPAConstants.PERMIT_NO, "hardcodedTemporarily");
		}
	}

	/**
	 * Extracts the list of required NOCs from the drawing detail.
	 * 
	 * @param drawingDetail the drawing detail map containing required NOCs
	 * @return list of required NOC codes, empty list if none specified
	 */
	private List<String> extractRequiredNOCs(Map<String, Object> drawingDetail) {
		List<String> requiredNOCs = (List<String>) drawingDetail.get("requiredNOCs");
		return !CollectionUtils.isEmpty(requiredNOCs) ? requiredNOCs : new ArrayList<>();
	}

	/**
	 * Builds the EDCR structure with plan information matching the scrutiny details
	 * format.
	 * 
	 * <p>
	 * The structure created is: edcrDetail -> planDetail -> planInformation This
	 * matches the path structure used in scrutiny details and is essential for
	 * downstream EDCR processing.
	 * 
	 * @param edcr            the EDCR map to populate with the structured plan
	 *                        information
	 * @param businessService the business service identifier for the BPA
	 *                        application
	 * @param requiredNOCs    the list of required NOCs for the plan
	 */
	private void buildEdcrStructure(LinkedHashMap<String, Object> edcr, String businessService,
			List<String> requiredNOCs) {
		// Build plan information with business service and required NOCs
		LinkedHashMap<String, Object> planInformation = new LinkedHashMap<>();
		planInformation.put("businessService", businessService);
		planInformation.put("requiredNOCs", requiredNOCs);

		// Wrap plan information in plan detail
		LinkedHashMap<String, LinkedHashMap> planDetail = new LinkedHashMap<>();
		planDetail.put("planInformation", planInformation);

		// Wrap plan detail in plan detail object
		LinkedHashMap<String, LinkedHashMap> planDetailObject = new LinkedHashMap<>();
		planDetailObject.put("planDetail", planDetail);

		// Add to edcr detail list and populate the main edcr map
		List<LinkedHashMap> edcrDetail = new ArrayList<>();
		edcrDetail.add(planDetailObject);
		edcr.put("edcrDetail", edcrDetail);
	}

	/**
	 * applies the required vlaidation for OC on Create
	 * 
	 * @param applicationType
	 * @param values
	 * @param requestInfo
	 * @param bpaRequest
	 */
	private void validateCreateOC(String applicationType, Map<String, String> values, RequestInfo requestInfo,
			BPARequest bpaRequest) {

		if (applicationType.equalsIgnoreCase(BPAConstants.BUILDING_PLAN_OC)) {
			String approvalNo = values.get(BPAConstants.PERMIT_NO);

			BPASearchCriteria criteria = new BPASearchCriteria();
			criteria.setTenantId(bpaRequest.getBPA().getTenantId());
			criteria.setApprovalNo(approvalNo);
			criteria.setIsRevalidationApplication(Boolean.FALSE);
			List<BPA> ocBpas = search(criteria, requestInfo);

			if (ocBpas.size() <= 0 || ocBpas.size() > 1) {
				throw new CustomException(BPAErrorConstants.CREATE_ERROR,
						((ocBpas.size() <= 0) ? "BPA not found with approval Number :"
								: "Multiple BPA applications found for approval number :") + approvalNo);
			} else if (ocBpas.get(0).getStatus().equalsIgnoreCase(BPAConstants.STATUS_REVOCATED)) {
				throw new CustomException(BPAErrorConstants.CREATE_ERROR,
						"This permit number is revocated you cannot use this permit number");
			} else if (!ocBpas.get(0).getStatus().equalsIgnoreCase(BPAConstants.STATUS_APPROVED)) {
				throw new CustomException(BPAErrorConstants.CREATE_ERROR,
						"The selected permit number still in workflow approval process, Please apply occupancy after completing approval process.");
			}

			values.put("landId", ocBpas.get(0).getLandId());
			criteria.setEdcrNumber(ocBpas.get(0).getEdcrNumber());
			ocService.validateAdditionalData(bpaRequest, criteria);
			bpaRequest.getBPA().setLandInfo(ocBpas.get(0).getLandInfo());
			if (!config.isDevMode()) {
				this.validatePermitNumber(bpaRequest);
			}
		}
	}

	/**
	 * calls calculation service calculate and generte demand accordingly
	 * 
	 * @param applicationType
	 * @param bpaRequest
	 */
	private void addCalculation(String applicationType, BPARequest bpaRequest) {

		if (bpaRequest.getBPA().getRiskType().equals(BPAConstants.LOW_RISKTYPE)
				&& !applicationType.equalsIgnoreCase(BPAConstants.BUILDING_PLAN_OC)) {
			calculationService.addCalculation(bpaRequest, BPAConstants.LOW_RISK_PERMIT_FEE_KEY);
		} else {
			calculationService.addCalculation(bpaRequest, BPAConstants.APPLICATION_FEE_KEY);
		}
	}

	/**
	 * Searches for BPA applications based on provided criteria with comprehensive
	 * filtering and enrichment.
	 * <p>
	 * This method orchestrates the complete BPA search flow with the following
	 * capabilities:
	 * <ul>
	 * <li>Direct searches by mobile number, name, or escalation status</li>
	 * <li>Role-based searches (citizen vs employee)</li>
	 * <li>Automatic enrichment with land and plinth information</li>
	 * <li>Special handling for OC (Occupancy Certificate) outside Sujog
	 * applications</li>
	 * </ul>
	 * </p>
	 * 
	 * <p>
	 * <b>Search Priority Order:</b>
	 * </p>
	 * <ol>
	 * <li>Mobile number lookup</li>
	 * <li>Name-based search</li>
	 * <li>Escalation status searches (escalated, escalated to me, about to
	 * escalate)</li>
	 * <li>Role-based search (citizen's own records or general employee search)</li>
	 * </ol>
	 * 
	 * @param criteria    The search criteria containing filters like mobile number,
	 *                    name, tenant ID, status, etc.
	 * @param requestInfo The request context containing user information and
	 *                    authentication details
	 * @return List of BPA applications matching the search criteria, enriched with
	 *         land and plinth data. Returns empty list if no matches found.
	 * @throws CustomException if validation fails or required data cannot be
	 *                         retrieved
	 */
	public List<BPA> search(BPASearchCriteria criteria, RequestInfo requestInfo) {
		log.info("Initiating BPA search with criteria: {}", criteria);

		// Step 1: Validate search criteria
		bpaValidator.validateSearch(requestInfo, criteria);

		// Step 2: Initialize land search criteria
		LandSearchCriteria landCriteria = buildLandSearchCriteria(criteria);

		// Step 3: Execute search based on criteria priority
		List<BPA> bpas = executeSearchBasedOnCriteria(criteria, requestInfo, landCriteria);

		// Step 4: Enrich results with OC outside Sujog details if applicable
		enrichOCOutsideSujogDetailsIfNeeded(criteria, bpas);

		log.info("BPA search completed. Found {} records", bpas.size());
		
		try {
	        updateLandInfo(requestInfo, bpas);
	        log.info("updateLandInfo completed successfully");
	    } catch (Exception e) {
	        log.error("Error occurred while executing updateLandInfo", e);
	    }

	    log.info("Returning BPA response with {} records", bpas.size());
		return bpas;
	}
	
	private void updateLandInfo(RequestInfo requestInfo, List<BPA> bpas) {

	    if (CollectionUtils.isEmpty(bpas)) {
	        log.debug("No BPA records found, skipping land info update");
	        return;
	    }

	    log.info("Starting land info update for {} BPA records", bpas.size());

	    for (BPA bpa : bpas) {

	        try {

	            // ✅ Skip if landInfo already present with owners
	            if (bpa.getLandInfo() != null 
	                && !CollectionUtils.isEmpty(bpa.getLandInfo().getOwners())) {

	                log.debug("LandInfo already present for BPA: {}", bpa.getApplicationNo());
	                continue;
	            }

	            // ✅ Skip if landId missing
	            if (StringUtils.isEmpty(bpa.getLandId())) {
	                log.warn("Skipping BPA {} as landId is null/empty", bpa.getApplicationNo());
	                continue;
	            }

	            List<LandInfo> landInfos = null;

	            // ✅ First attempt with original tenantId
	            LandSearchCriteria landCriteria = new LandSearchCriteria();
	            landCriteria.setTenantId(bpa.getTenantId());
	            landCriteria.setIds(Collections.singletonList(bpa.getLandId()));

	            log.debug("Fetching land info for BPA: {}, landId: {}, tenant: {}", 
	                    bpa.getApplicationNo(), bpa.getLandId(), bpa.getTenantId());

	            landInfos = landService.searchLandInfoToBPA(requestInfo, landCriteria);

	            // 🔥 Fallback: try with base tenant (od instead of od.cuttack)
	            if (CollectionUtils.isEmpty(landInfos)) {

	                String fallbackTenant = bpa.getTenantId().split("\\.")[0];

	                log.warn("Retrying land fetch with fallback tenantId: {} for BPA: {}", 
	                        fallbackTenant, bpa.getApplicationNo());

	                landCriteria.setTenantId(fallbackTenant);

	                landInfos = landService.searchLandInfoToBPA(requestInfo, landCriteria);
	            }

	            // ✅ Final assignment
	            if (!CollectionUtils.isEmpty(landInfos)) {

	                LandInfo landInfo = landInfos.get(0);
	                bpa.setLandInfo(landInfo);

	                log.info("LandInfo updated successfully for BPA: {} with landId: {}", 
	                        bpa.getApplicationNo(), bpa.getLandId());

	            } else {
	                log.error("No LandInfo found for BPA: {} even after retry, landId: {}", 
	                        bpa.getApplicationNo(), bpa.getLandId());
	            }

	        } catch (Exception e) {

	            // ❗ Important: do NOT break loop
	            log.error("Exception while processing BPA: {} with landId: {}", 
	                    bpa.getApplicationNo(), bpa.getLandId(), e);

	            // continue automatically to next BPA
	        }
	    }

	    log.info("Completed land info update for all BPA records");
	}
	/**
	 * Builds a LandSearchCriteria object initialized with tenant ID from BPA search
	 * criteria.
	 * 
	 * @param criteria The BPA search criteria
	 * @return LandSearchCriteria initialized with tenant ID
	 */
	private LandSearchCriteria buildLandSearchCriteria(BPASearchCriteria criteria) {
		LandSearchCriteria landCriteria = new LandSearchCriteria();
		landCriteria.setTenantId(criteria.getTenantId());
		return landCriteria;
	}

	/**
	 * Executes the appropriate search strategy based on the provided criteria.
	 * <p>
	 * Search strategies are prioritized in the following order:
	 * <ol>
	 * <li>Mobile number search</li>
	 * <li>Name search</li>
	 * <li>Escalation-based searches</li>
	 * <li>Role-based default search</li>
	 * </ol>
	 * </p>
	 * 
	 * @param criteria     The BPA search criteria
	 * @param requestInfo  The request context
	 * @param landCriteria The land search criteria
	 * @return List of BPA applications matching the criteria
	 */
	private List<BPA> executeSearchBasedOnCriteria(BPASearchCriteria criteria, RequestInfo requestInfo,
			LandSearchCriteria landCriteria) {

		// Priority 1: Search by mobile number
		if (criteria.getMobileNumber() != null) {
			log.debug("Executing mobile number search for: {}", criteria.getMobileNumber());
			return this.getBPAFromMobileNumber(criteria, landCriteria, requestInfo);
		}

		// Priority 2: Search by name
		if (!StringUtils.isEmpty(criteria.getName())) {
			log.debug("Executing name search for: {}", criteria.getName());
			return this.getBPAFromName(criteria, landCriteria, requestInfo);
		}

		// Priority 3: Escalation-based searches
		if (criteria.isEscalated()) {
			log.debug("Executing escalated BPA search");
			return this.getBPAAutoEscalated(criteria, requestInfo);
		}

		if (criteria.isEscalatedToMe()) {
			log.debug("Executing escalated to me BPA search");
			return this.getBPAAutoEscalatedToMe(criteria, requestInfo);
		}

		if (criteria.isAboutToEscalate()) {
			log.debug("Executing about to escalate BPA search");
			return this.getBPAAboutToAutoEscalate(criteria, requestInfo);
		}

		// Priority 4: Role-based search (citizen or employee)
		return executeRoleBasedSearch(criteria, requestInfo, landCriteria);
	}

	/**
	 * Executes role-based search logic differentiating between citizen and employee
	 * searches.
	 * <p>
	 * <b>Citizen Search:</b> Returns only BPAs created by or for the logged-in
	 * citizen.<br>
	 * <b>Employee Search:</b> Returns BPAs based on criteria with land and plinth
	 * enrichment.
	 * </p>
	 * 
	 * @param criteria     The BPA search criteria
	 * @param requestInfo  The request context containing user roles
	 * @param landCriteria The land search criteria
	 * @return List of BPA applications based on user role
	 */
	private List<BPA> executeRoleBasedSearch(BPASearchCriteria criteria, RequestInfo requestInfo,
			LandSearchCriteria landCriteria) {

		List<String> roles = extractUserRoles(requestInfo);

		// Check if user is a citizen with minimal criteria
		if (isCitizenWithMinimalCriteria(criteria, roles)) {
			log.info("Executing citizen search for user's own records");
			return executeCitizenSearch(criteria, requestInfo, landCriteria);
		}

		// Execute employee/general search with land enrichment
		log.debug("Executing general criteria search with land enrichment");
		return executeGeneralSearchWithEnrichment(criteria, requestInfo, landCriteria);
	}

	/**
	 * Extracts user role codes from request info.
	 * 
	 * @param requestInfo The request context
	 * @return List of role codes
	 */
	private List<String> extractUserRoles(RequestInfo requestInfo) {
		return requestInfo.getUserInfo().getRoles().stream().map(Role::getCode).collect(Collectors.toList());
	}

	/**
	 * Checks if the user is a citizen with minimal search criteria (tenant-only or
	 * empty criteria).
	 * 
	 * @param criteria The BPA search criteria
	 * @param roles    The user's role codes
	 * @return true if user is citizen with minimal criteria, false otherwise
	 */
	private boolean isCitizenWithMinimalCriteria(BPASearchCriteria criteria, List<String> roles) {
		return (criteria.tenantIdOnly() || criteria.isEmpty()) && roles.contains(BPAConstants.CITIZEN);
	}

	/**
	 * Executes search for citizen's own BPA records (created by or for the
	 * citizen).
	 * 
	 * @param criteria     The BPA search criteria
	 * @param requestInfo  The request context
	 * @param landCriteria The land search criteria
	 * @return List of BPAs owned by or created for the citizen
	 */
	private List<BPA> executeCitizenSearch(BPASearchCriteria criteria, RequestInfo requestInfo,
			LandSearchCriteria landCriteria) {
		List<BPA> bpas = this.getBPACreatedForByMe(criteria, requestInfo, landCriteria, null);
		log.info("Citizen search returned {} records", bpas.size());
		return bpas;
	}

	/**
	 * Executes general search with automatic enrichment of land and plinth
	 * information.
	 * <p>
	 * This method:
	 * <ol>
	 * <li>Retrieves BPAs matching the criteria</li>
	 * <li>Extracts land IDs from BPA records</li>
	 * <li>Fetches corresponding land information</li>
	 * <li>Enriches BPAs with land and plinth data</li>
	 * </ol>
	 * </p>
	 * 
	 * @param criteria     The BPA search criteria
	 * @param requestInfo  The request context
	 * @param landCriteria The land search criteria
	 * @return List of BPAs enriched with land and plinth information
	 */
	private List<BPA> executeGeneralSearchWithEnrichment(BPASearchCriteria criteria, RequestInfo requestInfo,
			LandSearchCriteria landCriteria) {

		// Fetch BPAs matching criteria
		List<BPA> bpas = getBPAFromCriteria(criteria, requestInfo, null);

		if (CollectionUtils.isEmpty(bpas)) {
			log.debug("No BPAs found for given criteria");
			return bpas;
		}

		// Extract land IDs from BPAs
		List<String> landIds = extractLandIds(bpas);

		if (landIds.isEmpty()) {
			log.debug("No land IDs found in BPA records, skipping land enrichment");
			return bpas;
		}

		// Enrich with land and plinth information
		enrichBPAsWithLandAndPlinthData(bpas, landIds, landCriteria, requestInfo);

		return bpas;
	}

	/**
	 * Extracts unique land IDs from a list of BPA applications.
	 * 
	 * @param bpas List of BPA applications
	 * @return List of land IDs extracted from BPAs
	 */
	private List<String> extractLandIds(List<BPA> bpas) {
		return bpas.stream().map(BPA::getLandId).filter(Objects::nonNull).collect(Collectors.toList());
	}

	/**
	 * Enriches BPA applications with land and plinth information.
	 * <p>
	 * This method fetches land information from the land service and populates both
	 * land details and associated plinth approval records into the BPA objects.
	 * </p>
	 * 
	 * @param bpas         List of BPA applications to enrich
	 * @param landIds      List of land IDs to fetch
	 * @param landCriteria The land search criteria
	 * @param requestInfo  The request context
	 */
	private void enrichBPAsWithLandAndPlinthData(List<BPA> bpas, List<String> landIds, LandSearchCriteria landCriteria,
			RequestInfo requestInfo) {

		// Configure land search criteria
		landCriteria.setIds(landIds);
		landCriteria.setTenantId(bpas.get(0).getTenantId());

		log.info("Fetching land information for tenant: {}", landCriteria.getTenantId());

		// Fetch land information
		List<LandInfo> landInfos = landService.searchLandInfoToBPA(requestInfo, landCriteria);

		// Populate land and plinth data
		this.populateLandToBPA(bpas, landInfos, requestInfo);
		this.populatePlinthToBPA(bpas, requestInfo);

		log.debug("Enriched {} BPAs with land and plinth information", bpas.size());
	}

	/**
	 * Enriches BPA records with OC (Occupancy Certificate) outside Sujog details if
	 * applicable.
	 * <p>
	 * Enrichment is performed when:
	 * <ul>
	 * <li>Search criteria explicitly requests OC outside Sujog applications,
	 * OR</li>
	 * <li>The first result is an OC outside Sujog application</li>
	 * </ul>
	 * </p>
	 * 
	 * @param criteria The BPA search criteria
	 * @param bpas     List of BPA applications to potentially enrich
	 */
	private void enrichOCOutsideSujogDetailsIfNeeded(BPASearchCriteria criteria, List<BPA> bpas) {
		if (CollectionUtils.isEmpty(bpas)) {
			return;
		}

		boolean shouldEnrich = Boolean.TRUE.equals(criteria.getIsOCOutsideSujogApplication())
				|| bpas.get(0).getOCOutsideSujogApplication();

		if (shouldEnrich) {
			log.debug("Enriching OC outside Sujog details for {} BPAs", bpas.size());
			enrichmentService.enrichOCSearchOutsideDetaiblsFromDB(bpas);
		}
	}

	/**
	 * Populates plinth approval records into BPA applications.
	 * <p>
	 * For each BPA application, this method:
	 * <ol>
	 * <li>Searches for associated plinth approval records by application
	 * number</li>
	 * <li>Attaches the plinth approval list to the BPA object</li>
	 * </ol>
	 * </p>
	 * <p>
	 * Plinth approval represents an intermediate construction stage inspection that
	 * must be completed before proceeding with further construction.
	 * </p>
	 * 
	 * @param bpas        List of BPA applications to enrich with plinth data
	 * @param requestInfo The request context (currently not used but kept for
	 *                    consistency)
	 */
	private void populatePlinthToBPA(List<BPA> bpas, RequestInfo requestInfo) {

		if (CollectionUtils.isEmpty(bpas)) {
			return;
		}

		bpas.forEach(bpa -> {
			PlinthApprovalSearchCriteria searchCriteria = PlinthApprovalSearchCriteria.builder()
					.bpaApplicationNo(bpa.getApplicationNo()).build();

			List<PlinthApproval> plinthApprovals = repository.getPlinthApproval(searchCriteria);
			bpa.setPlinthApproval(plinthApprovals);
		});

		log.debug("Populated plinth approval data for {} BPA applications", bpas.size());
	}

	/**
	 * Retrieves BPA records created by or for the currently logged-in citizen user.
	 * <p>
	 * This method performs a multi-step search process:
	 * <ol>
	 * <li>Extracts the logged-in user's UUID</li>
	 * <li>Fetches complete user details including mobile number</li>
	 * <li>Searches land records by user's mobile number</li>
	 * <li>Filters BPAs by extracted land IDs and user UUID</li>
	 * <li>Enriches results with land and plinth information</li>
	 * </ol>
	 * </p>
	 * <p>
	 * This ensures citizens only see BPA applications that they own or that were
	 * created on their behalf.
	 * </p>
	 * 
	 * @param criteria     The BPA search criteria (will be modified with
	 *                     owner/creator filters)
	 * @param requestInfo  The request context containing logged-in user information
	 * @param landCriteria The land search criteria for fetching associated land
	 *                     records
	 * @param edcrNos      EDCR numbers for filtering (currently unused, can be
	 *                     null)
	 * @return List of BPA applications owned by or created for the logged-in
	 *         citizen
	 */
	private List<BPA> getBPACreatedForByMe(BPASearchCriteria criteria, RequestInfo requestInfo,
			LandSearchCriteria landCriteria, List<String> edcrNos) {

		// Step 1: Extract and set user UUID for filtering
		List<String> userUuids = extractUserUuid(criteria, requestInfo);
		log.info("Searching BPAs for user UUID: {}", userUuids);

		// Step 2: Fetch user details to get mobile number
		UserDetailResponse userInfo = userService.getUser(criteria, requestInfo);
		if (userInfo != null && !CollectionUtils.isEmpty(userInfo.getUser())) {
			landCriteria.setMobileNumber(userInfo.getUser().get(0).getMobileNumber());
			log.info("User mobile number: {}", userInfo.getUser().get(0).getMobileNumber());
		}

		// Step 3: Fetch land records by mobile number
		log.info("Fetching land records for tenant: {} and mobile: {}", landCriteria.getTenantId(),
				landCriteria.getMobileNumber());
		List<LandInfo> landInfos = landService.searchLandInfoToBPA(requestInfo, landCriteria);

		// Step 4: Extract land IDs and add to search criteria
		List<String> landIds = extractLandIdsFromLandInfo(landInfos);
		if (!landIds.isEmpty()) {
			criteria.setLandId(landIds);
		}

		// Step 5: Fetch BPAs matching criteria
		List<BPA> bpas = getBPAFromCriteria(criteria, requestInfo, edcrNos);
		log.info("Found {} BPA records for user", bpas.size());

		// Step 6: Enrich with land and plinth data
		this.populateLandToBPA(bpas, landInfos, requestInfo);
		this.populatePlinthToBPA(bpas, requestInfo);

		return bpas;
	}

	/**
	 * Extracts the logged-in user's UUID and configures the search criteria with
	 * owner filters.
	 * 
	 * @param criteria    The BPA search criteria to be updated
	 * @param requestInfo The request context containing user information
	 * @return List containing the user's UUID
	 */
	private List<String> extractUserUuid(BPASearchCriteria criteria, RequestInfo requestInfo) {
		List<String> uuids = new ArrayList<>();

		if (requestInfo.getUserInfo() != null && !StringUtils.isEmpty(requestInfo.getUserInfo().getUuid())) {
			String uuid = requestInfo.getUserInfo().getUuid();
			uuids.add(uuid);

			// Set both owner and creator filters to get all relevant records
			criteria.setOwnerIds(uuids);
			criteria.setCreatedBy(uuids);
		}

		return uuids;
	}

	/**
	 * Extracts land IDs from a list of LandInfo objects.
	 * 
	 * @param landInfos List of land information records
	 * @return List of land IDs
	 */
	private List<String> extractLandIdsFromLandInfo(List<LandInfo> landInfos) {
		if (CollectionUtils.isEmpty(landInfos)) {
			return Collections.emptyList();
		}

		return landInfos.stream().map(LandInfo::getId).collect(Collectors.toList());
	}

	/**
	 * Populates land information into BPA applications by matching land IDs.
	 * <p>
	 * This method performs a two-pass enrichment:
	 * <ol>
	 * <li><b>First Pass:</b> Matches BPAs with land information from the provided
	 * list</li>
	 * <li><b>Second Pass:</b> For any BPAs still missing land info, performs
	 * individual lookups</li>
	 * </ol>
	 * </p>
	 * <p>
	 * The second pass ensures complete data even when land records span multiple
	 * tenants or weren't included in the initial bulk fetch.
	 * </p>
	 * 
	 * @param bpas        List of BPA applications to enrich with land information
	 * @param landInfos   List of land information records to match against BPAs
	 * @param requestInfo The request context for additional land service calls if
	 *                    needed
	 */
	private void populateLandToBPA(List<BPA> bpas, List<LandInfo> landInfos, RequestInfo requestInfo) {
		if (CollectionUtils.isEmpty(bpas) || CollectionUtils.isEmpty(landInfos)) {
			log.debug("Skipping land population - BPAs or land infos are empty");
			return;
		}

		// First pass: Match BPAs with provided land information
		matchBPAsWithLandInfo(bpas, landInfos);

		// Second pass: Fetch missing land information for BPAs that weren't matched
		fetchMissingLandInformation(bpas, requestInfo);

		log.debug("Successfully populated land information for BPAs");
	}

	/**
	 * Matches BPAs with land information from the provided list based on land ID.
	 * 
	 * @param bpas      List of BPA applications
	 * @param landInfos List of land information records
	 */
	private void matchBPAsWithLandInfo(List<BPA> bpas, List<LandInfo> landInfos) {
		for (BPA bpa : bpas) {
			for (LandInfo landInfo : landInfos) {
				if (landInfo.getId().equalsIgnoreCase(bpa.getLandId())) {
					bpa.setLandInfo(landInfo);
					break; // Move to next BPA once match is found
				}
			}
		}
	}

	/**
	 * Fetches missing land information for BPAs that don't have land data populated
	 * yet.
	 * <p>
	 * This handles edge cases where land records weren't in the initial bulk fetch,
	 * such as cross-tenant scenarios or partial data.
	 * </p>
	 * 
	 * @param bpas        List of BPA applications
	 * @param requestInfo The request context for land service calls
	 */
	private void fetchMissingLandInformation(List<BPA> bpas, RequestInfo requestInfo) {
		for (BPA bpa : bpas) {
			// Skip if land info is already populated or land ID is null
			if (bpa.getLandId() == null || bpa.getLandInfo() != null) {
				continue;
			}

			// Fetch missing land information
			LandSearchCriteria missingLandCriteria = new LandSearchCriteria();
			missingLandCriteria.setTenantId(bpa.getTenantId());
			missingLandCriteria.setIds(Collections.singletonList(bpa.getLandId()));

			log.debug("Fetching missing land info for tenant: {}, land ID: {}", missingLandCriteria.getTenantId(),
					bpa.getLandId());

			List<LandInfo> missingLandInfo = landService.searchLandInfoToBPA(requestInfo, missingLandCriteria);

			// Populate if found
			for (LandInfo landInfo : missingLandInfo) {
				if (landInfo.getId().equalsIgnoreCase(bpa.getLandId())) {
					bpa.setLandInfo(landInfo);
					break;
				}
			}
		}
	}

	/**
	 * Searches for BPA applications by mobile number through land record linkage.
	 * <p>
	 * This method performs a two-step search process:
	 * <ol>
	 * <li>Searches land records by mobile number</li>
	 * <li>Searches BPA applications linked to the found land records</li>
	 * <li>Enriches BPAs with the land information</li>
	 * </ol>
	 * </p>
	 * <p>
	 * This approach enables finding BPA applications even when the applicant's
	 * mobile number is stored only in the land records.
	 * </p>
	 * 
	 * @param criteria     The BPA search criteria containing mobile number
	 * @param landCriteria The land search criteria to be populated with mobile
	 *                     number
	 * @param requestInfo  The request context
	 * @return List of BPA applications associated with the mobile number, enriched
	 *         with land data
	 */
	private List<BPA> getBPAFromMobileNumber(BPASearchCriteria criteria, LandSearchCriteria landCriteria,
			RequestInfo requestInfo) {

		log.debug("Searching BPAs by mobile number: {}", criteria.getMobileNumber());

		// Step 1: Search land records by mobile number
		landCriteria.setMobileNumber(criteria.getMobileNumber());
		List<LandInfo> landInfos = landService.searchLandInfoToBPA(requestInfo, landCriteria);

		if (CollectionUtils.isEmpty(landInfos)) {
			log.debug("No land records found for mobile number: {}", criteria.getMobileNumber());
			return Collections.emptyList();
		}

		// Step 2: Extract land IDs and search BPAs
		List<String> landIds = extractLandIdsFromLandInfo(landInfos);
		criteria.setLandId(landIds);
		List<BPA> bpas = getBPAFromLandId(criteria, requestInfo, null);

		// Step 3: Enrich BPAs with land information
		matchBPAsWithLandInfo(bpas, landInfos);

		log.debug("Found {} BPAs for mobile number: {}", bpas.size(), criteria.getMobileNumber());
		return bpas;
	}

	/**
	 * Retrieves BPA applications by land ID criteria.
	 * <p>
	 * This is a specialized search method that filters BPAs based on associated
	 * land IDs. It's primarily used after identifying relevant land records through
	 * other search criteria (e.g., mobile number, name).
	 * </p>
	 * 
	 * @param criteria    The BPA search criteria with land IDs populated
	 * @param requestInfo The request context
	 * @param edcrNos     Optional EDCR numbers for additional filtering (can be
	 *                    null)
	 * @return List of BPA applications linked to the specified land IDs, empty list
	 *         if none found
	 */
	private List<BPA> getBPAFromLandId(BPASearchCriteria criteria, RequestInfo requestInfo, List<String> edcrNos) {
		List<BPA> bpas = repository.getBPAData(criteria, edcrNos);

		if (CollectionUtils.isEmpty(bpas)) {
			return Collections.emptyList();
		}

		return bpas;
	}

	/**
	 * Retrieves BPA applications from database based on provided search criteria.
	 * <p>
	 * This is the core repository search method that supports a wide range of
	 * filter criteria including:
	 * <ul>
	 * <li>Application number, tenant ID, status</li>
	 * <li>Land IDs, owner IDs, creator IDs</li>
	 * <li>Date ranges, risk types, application types</li>
	 * <li>And many other BPA-specific filters</li>
	 * </ul>
	 * </p>
	 * 
	 * @param criteria    The comprehensive BPA search criteria
	 * @param requestInfo The request context
	 * @param edcrNos     Optional list of EDCR numbers for filtering (can be null)
	 * @return List of BPA applications matching the criteria, empty list if none
	 *         found
	 */
	public List<BPA> getBPAFromCriteria(BPASearchCriteria criteria, RequestInfo requestInfo, List<String> edcrNos) {
		List<BPA> bpas = repository.getBPAData(criteria, edcrNos);

		if (CollectionUtils.isEmpty(bpas)) {
			return Collections.emptyList();
		}

		return bpas;
	}

	/**
	 * Updates the BPA application with comprehensive validation, enrichment, and
	 * workflow processing.
	 * <p>
	 * This method orchestrates the complete BPA update flow including:
	 * <ul>
	 * <li>Initial validation and special case handling (deletion, revalidation,
	 * etc.)</li>
	 * <li>EDCR details retrieval and processing</li>
	 * <li>Business service and validation setup</li>
	 * <li>Additional details management</li>
	 * <li>Enrichment and workflow processing</li>
	 * <li>Post-workflow operations (demand generation, payment processing)</li>
	 * <li>Final persistence to database</li>
	 * </ul>
	 * </p>
	 * 
	 * @param bpaRequest The BPA update request containing the application details
	 *                   and request info
	 * @return Updated BPA object after successful processing
	 * @throws CustomException if validation fails or application is not found in
	 *                         the system
	 */
	@SuppressWarnings("unchecked")
	public BPA update(BPARequest bpaRequest) {
		log.info("Starting BPA update for application: {} with details: {}", bpaRequest.getBPA().getApplicationNo(),
				bpaRequest.getBPA().getAdditionalDetails());

		// Step 1: Initialize core data and perform initial validations
		UpdateContext context = initializeUpdateContext(bpaRequest);

		// Step 2: Handle special update scenarios (deletion, revalidation, etc.)
		BPA specialCaseResult = handleSpecialUpdateCases(bpaRequest);
		if (specialCaseResult != null) {
			return specialCaseResult;
		}

		// Step 3: Validate and retrieve EDCR details
		performInitialValidations(bpaRequest);
		EdcrUpdateDetails edcrDetails = retrieveAndProcessEdcrDetails(bpaRequest, context);

		// Step 4: Retrieve and validate business service
		BusinessService businessService = retrieveBusinessServiceAndValidateApplication(bpaRequest, context);

		// Step 5: Manage additional details and process OC updates
		Map<String, String> additionalDetails = manageAdditionalDetails(bpaRequest, context, edcrDetails);

		// Step 6: Perform enrichment, validation, and workflow processing
		processEnrichmentAndWorkflow(bpaRequest, context, edcrDetails, businessService, additionalDetails);

		// Step 7: Handle post-workflow operations
		processPostWorkflowOperations(bpaRequest, context, edcrDetails, businessService);

		// Step 8: Persist the updated BPA application
		log.info("Persisting BPA update for application: {} with details: {}", bpaRequest.getBPA().getApplicationNo(),
				bpaRequest.getBPA().getAdditionalDetails());
		repository.update(bpaRequest,
				workflowService.isStateUpdatable(bpaRequest.getBPA().getStatus(), businessService));

		return bpaRequest.getBPA();
	}

	/**
	 * Inner class to hold context information for the update process. This helps
	 * organize related data and reduces parameter passing between methods.
	 */
	private static class UpdateContext {
		RequestInfo requestInfo;
		String tenantId;
		Object mdmsData;
		BPA bpa;
		String businessServices;

		UpdateContext(RequestInfo requestInfo, String tenantId, Object mdmsData, BPA bpa, String businessServices) {
			this.requestInfo = requestInfo;
			this.tenantId = tenantId;
			this.mdmsData = mdmsData;
			this.bpa = bpa;
			this.businessServices = businessServices;
		}
	}

	/**
	 * Inner class to hold EDCR-related details extracted during update processing.
	 */
	private static class EdcrUpdateDetails {
		Map<String, String> edcrResponse;
		String applicationType;
		String serviceType;

		EdcrUpdateDetails(Map<String, String> edcrResponse, String applicationType, String serviceType) {
			this.edcrResponse = edcrResponse;
			this.applicationType = applicationType;
			this.serviceType = serviceType;
		}
	}

	/**
	 * Initializes the update context with core data required throughout the update
	 * process.
	 * <p>
	 * This method validates that the BPA application has an ID and retrieves MDMS
	 * data.
	 * </p>
	 * 
	 * @param bpaRequest The BPA update request
	 * @return UpdateContext containing initialized data
	 * @throws CustomException if the application ID is null (application not found)
	 */
	private UpdateContext initializeUpdateContext(BPARequest bpaRequest) {
		BPA bpa = bpaRequest.getBPA();

		if (bpa.getId() == null) {
			throw new CustomException(BPAErrorConstants.UPDATE_ERROR, "Application Not found in the System: " + bpa);
		}

		RequestInfo requestInfo = bpaRequest.getRequestInfo();
		String tenantId = bpa.getTenantId().split("\\.")[0];
		Object mdmsData = util.mDMSCall(requestInfo, tenantId);
		String businessServices = bpa.getBusinessService();

		return new UpdateContext(requestInfo, tenantId, mdmsData, bpa, businessServices);
	}

	/**
	 * Handles special update cases that require immediate processing and return.
	 * <p>
	 * Special cases include:
	 * <ul>
	 * <li>BPA deletion requests</li>
	 * <li>EDCR number update requests</li>
	 * <li>Commencement date update requests</li>
	 * <li>Revalidation applications</li>
	 * <li>Building plan/layout signature requests</li>
	 * <li>Revoke action requests</li>
	 * </ul>
	 * </p>
	 * 
	 * @param bpaRequest The BPA update request
	 * @return Updated BPA if this is a special case, null otherwise
	 */
	private BPA handleSpecialUpdateCases(BPARequest bpaRequest) {
		BPA bpa = bpaRequest.getBPA();

		log.info("Checking if the application is for revalidation.");
		log.info("Is Revalidation Application: {}", bpa.isRevalidationApplication());

		if (isRequestForBPADeletion(bpaRequest)) {
			log.info("Processing BPA deletion request");
			return processBPADeletion(bpaRequest);
		}

		if (isRequestForEdcrNoUpdation(bpaRequest)) {
			log.info("Processing EDCR number update request");
			return processEdcrNoUpdation(bpaRequest);
		}

		if (isRequestForCommencementDateUpdate(bpaRequest)) {
			log.info("Processing commencement date update request");
			return processCommencementDateUpdate(bpaRequest);
		}

		if (bpa.isRevalidationApplication()) {
			log.info("Revalidation application detected. Redirecting to processRevalidation method.");
			return processRevalidation(bpaRequest);
		}

		if (isRequestForBuildingPlanLayoutSignature(bpaRequest)) {
			log.info("Processing building plan/layout signature request");
			return processBuildingPlanLayoutSignature(bpaRequest);
		}

		if (isRequestForRevokeAction(bpaRequest)) {
			log.info("Processing revoke action request");
			return processRevokedActionForBpaApplication(bpaRequest);
		}

		return null;
	}

	/**
	 * Performs initial validations required before EDCR processing.
	 * <p>
	 * Validates:
	 * <ul>
	 * <li>Refusal Show Cause Notice requirements on approval</li>
	 * <li>Pull back action validity</li>
	 * </ul>
	 * </p>
	 * 
	 * @param bpaRequest The BPA update request
	 * @throws CustomException if validation fails
	 */
	private void performInitialValidations(BPARequest bpaRequest) {
		log.info("Performing initial validations for application: {}", bpaRequest.getBPA().getApplicationNo());
		bpaValidator.validateRefusalSCNonApprove(bpaRequest);
		bpaValidator.validatePullBackAction(bpaRequest);
	}

	/**
	 * Retrieves and processes EDCR (Electronic Data Capture and Reporting) details.
	 * <p>
	 * This method handles three scenarios:
	 * <ul>
	 * <li>OC (Occupancy Certificate) outside Sujog applications - enriches from
	 * request</li>
	 * <li>BPA6 (Pre-approved plan) applications - retrieves from pre-approved plan
	 * service</li>
	 * <li>Standard applications - retrieves from EDCR service</li>
	 * </ul>
	 * The method also extracts application type and service type from EDCR
	 * response.
	 * </p>
	 * 
	 * @param bpaRequest The BPA update request
	 * @param context    The update context containing core data
	 * @return EdcrUpdateDetails containing EDCR response and extracted metadata
	 * @throws CustomException if EDCR details are not found for OC Outside Sujog
	 *                         applications
	 */
	private EdcrUpdateDetails retrieveAndProcessEdcrDetails(BPARequest bpaRequest, UpdateContext context) {
		log.info("Retrieving EDCR details for application: {}", context.bpa.getApplicationNo());

		Map<String, String> edcrResponse = new HashMap<>();

		if (context.bpa.getOCOutsideSujogApplication()) {
			log.info("Processing OC Outside Sujog application");
			enrichmentService.enrichOCOutsideEdcrDetailsFromRequest(bpaRequest);

			if (context.bpa.getPermitEdcrDetail().getEdcrDetail() == null) {
				throw new CustomException("EDCR_DETAIL_NOT_FOUND", "EDCR Detail is not found for OC Outside Sujog");
			}

			edcrResponse = objectMapper.convertValue(context.bpa.getPermitEdcrDetail(), LinkedHashMap.class);
		} else if (StringUtils.isNotEmpty(context.businessServices) && "BPA6".equals(context.businessServices)) {
			log.info("Processing pre-approved plan (BPA6) application");
			getEdcrDetailsForPreapprovedPlan(edcrResponse, bpaRequest);
		} else {
			log.info("Processing standard EDCR application");
			edcrResponse = edcrService.getEDCRDetails(context.requestInfo, context.bpa);
		}

		bpaRequest.setEdcrResponse(edcrResponse);

		String applicationType = edcrResponse.get(BPAConstants.APPLICATIONTYPE);
		String serviceType = edcrResponse.get(BPAConstants.SERVICETYPE);

		// Override for OC Outside Sujog applications
		if (context.bpa.getOCOutsideSujogApplication()) {
			applicationType = "BUILDING_OC_PLAN_SCRUTINY";
			serviceType = "NEW_CONSTRUCTION";
		}

		log.debug("Application Type: {}, Service Type: {}", applicationType, serviceType);

		return new EdcrUpdateDetails(edcrResponse, applicationType, serviceType);
	}

	/**
	 * Retrieves the business service configuration and validates the application
	 * exists.
	 * <p>
	 * This method:
	 * <ul>
	 * <li>Retrieves the workflow business service configuration</li>
	 * <li>Searches for existing BPA application by ID</li>
	 * <li>Validates that exactly one application is found</li>
	 * </ul>
	 * </p>
	 * 
	 * @param bpaRequest The BPA update request
	 * @param context    The update context
	 * @return BusinessService configuration for workflow processing
	 * @throws CustomException if no application or multiple applications are found
	 */
	private BusinessService retrieveBusinessServiceAndValidateApplication(BPARequest bpaRequest,
			UpdateContext context) {
		log.info("Retrieving business service for application: {}", context.bpa.getApplicationNo());

		BusinessService businessService = workflowService.getBusinessService(context.bpa, context.requestInfo,
				context.bpa.getApplicationNo());

		List<BPA> searchResult = getBPAWithBPAId(bpaRequest);

		if (CollectionUtils.isEmpty(searchResult) || searchResult.size() > 1) {
			throw new CustomException(BPAErrorConstants.UPDATE_ERROR,
					"Failed to Update the Application, Found None or multiple applications!");
		}

		// Set audit details from existing application
		bpaRequest.getBPA().setAuditDetails(searchResult.get(0).getAuditDetails());

		return businessService;
	}

	/**
	 * Manages the additional details map for the BPA application.
	 * <p>
	 * This method:
	 * <ul>
	 * <li>Initializes additional details map if not present</li>
	 * <li>Sets application type if not already set</li>
	 * <li>Removes field inspection details on send back to citizen</li>
	 * <li>Removes preview permit details</li>
	 * <li>Processes OC (Occupancy Certificate) updates</li>
	 * </ul>
	 * </p>
	 * 
	 * @param bpaRequest  The BPA update request
	 * @param context     The update context
	 * @param edcrDetails The EDCR details containing application type
	 * @return Map of additional details for the application
	 */
	@SuppressWarnings("unchecked")
	private Map<String, String> manageAdditionalDetails(BPARequest bpaRequest, UpdateContext context,
			EdcrUpdateDetails edcrDetails) {
		log.info("Managing additional details for application: {}", context.bpa.getApplicationNo());

		Map<String, String> additionalDetails = context.bpa.getAdditionalDetails() != null
				? (Map<String, String>) context.bpa.getAdditionalDetails()
				: new HashMap<>();

		// Set application type if not present
		if (Objects.isNull(additionalDetails.get("applicationType"))) {
			additionalDetails.put("applicationType", edcrDetails.applicationType);
			log.debug("Set application type in additional details: {}", edcrDetails.applicationType);
		}

		// Remove field inspection details on send back to citizen from FI status
		if (context.bpa.getStatus().equalsIgnoreCase(BPAConstants.FI_STATUS)
				&& context.bpa.getWorkflow().getAction().equalsIgnoreCase(BPAConstants.ACTION_SENDBACKTOCITIZEN)) {
			if (additionalDetails.get(BPAConstants.FI_ADDITIONALDETAILS) != null) {
				additionalDetails.remove(BPAConstants.FI_ADDITIONALDETAILS);
				log.debug("Removed field inspection additional details");
			}
		}

		// Remove preview permit details
		if (additionalDetails.get(BPAConstants.PREVIEW_PERMIT_ADDITIONAL_DETAILS) != null) {
			additionalDetails.remove(BPAConstants.PREVIEW_PERMIT_ADDITIONAL_DETAILS);
			log.debug("Removed preview permit additional details");
		}

		// Process OC updates
		this.processOcUpdate(edcrDetails.applicationType, edcrDetails.edcrResponse.get(BPAConstants.PERMIT_NO),
				bpaRequest, context.requestInfo, additionalDetails);

		return additionalDetails;
	}

	/**
	 * Performs enrichment, validation, and workflow processing.
	 * <p>
	 * This comprehensive method:
	 * <ul>
	 * <li>Manages offline NOCs (No Objection Certificates) for standard
	 * applications</li>
	 * <li>Validates pre-enrichment data against MDMS</li>
	 * <li>Enriches the BPA update request with necessary data</li>
	 * <li>Handles reject and send-back workflow actions</li>
	 * <li>Processes reminder notifications</li>
	 * <li>Enriches notice information</li>
	 * <li>Handles refusal show cause notices</li>
	 * <li>Executes the workflow transition</li>
	 * <li>Performs post-status enrichment</li>
	 * </ul>
	 * </p>
	 * 
	 * @param bpaRequest        The BPA update request
	 * @param context           The update context
	 * @param edcrDetails       The EDCR details
	 * @param businessService   The business service configuration
	 * @param additionalDetails The additional details map
	 */
	private void processEnrichmentAndWorkflow(BPARequest bpaRequest, UpdateContext context,
			EdcrUpdateDetails edcrDetails, BusinessService businessService, Map<String, String> additionalDetails) {
		log.info("Processing enrichment and workflow for application: {}", context.bpa.getApplicationNo());

		// Manage offline NOCs for non-OC-Outside-Sujog applications
		if (!context.bpa.getOCOutsideSujogApplication()) {
			nocService.manageOfflineNocs(bpaRequest, context.mdmsData);
			log.debug("Offline NOCs managed successfully");
		}

		// Validate and enrich the request
		bpaValidator.validatePreEnrichData(bpaRequest, context.mdmsData);
		enrichmentService.enrichBPAUpdateRequest(bpaRequest, businessService);
		log.info("Enrichment service completed for application: {}", context.bpa.getApplicationNo());

		// Retrieve search results for reject/send-back handling
		List<BPA> searchResult = getBPAWithBPAId(bpaRequest);

		// Handle reject and send-back actions
		this.handleRejectSendBackActions(edcrDetails.applicationType, bpaRequest, businessService, searchResult,
				context.mdmsData, edcrDetails.edcrResponse);
		log.debug("Reject/send-back actions handled");

		// Handle reminder notifications
		if (context.bpa.getWorkflow().getAction() != null
				&& context.bpa.getWorkflow().getAction().equalsIgnoreCase(BPAConstants.REMINDER)) {
			log.info("Processing reminder notification for application: {}", context.bpa.getApplicationNo());
			updatenotice(bpaRequest);
		}

		// Enrich notice information
		enrichNotice(bpaRequest, context.bpa);

		// Handle refusal show cause notice
		handleRefusalShowCauseNotice(bpaRequest, edcrDetails.applicationType);

		// Execute workflow transition
		wfIntegrator.callWorkFlow(bpaRequest);
		log.info("Workflow completed for application: {} with status: {}", context.bpa.getApplicationNo(),
				context.bpa.getStatus());

		// Post-workflow enrichment
		enrichmentService.postStatusEnrichment(bpaRequest);
	}

	/**
	 * Handles post-workflow operations including demand generation and payment
	 * processing.
	 * <p>
	 * This method:
	 * <ul>
	 * <li>Handles pull-back requests from pending sanction fee status</li>
	 * <li>Generates sanction fee demand when application reaches sanction fee
	 * state</li>
	 * <li>Processes payment skip for configured statuses</li>
	 * </ul>
	 * </p>
	 * 
	 * @param bpaRequest      The BPA update request
	 * @param context         The update context
	 * @param edcrDetails     The EDCR details containing application and service
	 *                        types
	 * @param businessService The business service configuration
	 */
	private void processPostWorkflowOperations(BPARequest bpaRequest, UpdateContext context,
			EdcrUpdateDetails edcrDetails, BusinessService businessService) {
		log.info("Processing post-workflow operations for application: {} with status: {}",
				context.bpa.getApplicationNo(), context.bpa.getStatus());

		// Handle pull-back request from pending sanction fee status
		handleIfPullBackRequest(bpaRequest, edcrDetails.applicationType);

		// Generate sanction fee demand if in sanction fee state
		if (context.bpa.getStatus().equalsIgnoreCase(BPAConstants.SANC_FEE_STATE)) {
			log.info("Generating sanction fee demand for application: {}", context.bpa.getApplicationNo());
			calculationService.addCalculationV2(bpaRequest, BPAConstants.SANCTION_FEE_KEY, edcrDetails.applicationType,
					edcrDetails.serviceType);
			log.info("Calculation service completed for application: {}", context.bpa.getApplicationNo());
			notificationService.sendPostApproveNotification(bpaRequest);
		}

		// Skip payment for configured statuses
		if (Arrays.asList(config.getSkipPaymentStatuses().split(",")).contains(context.bpa.getStatus())) {
			log.info("Skipping payment for status: {} on application: {}", context.bpa.getStatus(),
					context.bpa.getApplicationNo());
			enrichmentService.skipPayment(bpaRequest);
			enrichmentService.postStatusEnrichment(bpaRequest);
		}
	}

	/**
	 * Handles refusal show cause notice processing.
	 * <p>
	 * This method is triggered when the workflow action is to reject with a show
	 * cause notice. Currently contains commented logic for future implementation.
	 * </p>
	 * 
	 * @param bpaRequest      The BPA update request
	 * @param applicationType The type of the application
	 */
	private void handleRefusalShowCauseNotice(BPARequest bpaRequest, String applicationType) {
		BPA bpa = bpaRequest.getBPA();

		if (BPAConstants.ACTION_REJECT_SCN.equalsIgnoreCase(bpa.getWorkflow().getAction())) {
			String applicationNo = bpa.getApplicationNo();
			log.info("BPA Refusal SCN action triggered for applicationno : " + applicationNo);
			;
			/*
			 * NoticeSearchCriteria noticeSearchCriteria =
			 * NoticeSearchCriteria.builder().businessid(applicationNo)
			 * .letterType(BPAConstants.REFUSAL_SHOWCAUSE_LETTER_TYPE).build();
			 * 
			 * List<Notice> notices = scnRepository.getNoticeData(noticeSearchCriteria);
			 * 
			 * if (!CollectionUtils.isEmpty(notices)) { String letterNo =
			 * notices.get(0).getLetterNo(); throw new CustomException("NOTICE_EXIST_ERROR",
			 * String.
			 * format("Refusal SCN already exists for business ID: %s. Letter Number: %s",
			 * applicationNo, StringUtils.defaultString(letterNo, "N/A"))); }
			 * 
			 * try { NoticeRequest noticeRequest =
			 * noticeService.createRefusalShowCauseNotice(bpa, bpaRequest.getRequestInfo());
			 * String letterNo = noticeRequest.getnotice().getLetterNo();
			 * log.info("Refusal SCN created for business ID: {} Letter Number: {}",
			 * applicationNo, letterNo); } catch (Exception ex) {
			 * log.error("Error while creating Refusal SCN for business ID: {}",
			 * applicationNo, ex); }
			 * 
			 */
		}

	}

	/**
	 * @param bpaRequest
	 * @param bpa
	 */
	private void enrichNotice(BPARequest bpaRequest, BPA bpa) {
		if (bpaRequest.getRequestInfo().getUserInfo().getRoles().stream().map(role -> role.getCode())
				.collect(Collectors.toList()).contains(BPAConstants.CITIZEN)) {
			if ((bpa.getWorkflow().getAction() != null
					&& bpa.getWorkflow().getAction().equalsIgnoreCase(BPAConstants.ACTION_FORWORD))
					|| (bpa.getWorkflow().getAction() != null && bpa.getWorkflow().getAction()
							.equalsIgnoreCase(BPAConstants.ACTION_FORWARD_TO_APPROVER))) {
				NoticeSearchCriteria criteria = new NoticeSearchCriteria();
				criteria.setBusinessid(bpaRequest.getBPA().getApplicationNo());
				criteria.setTenantid(bpaRequest.getBPA().getTenantId());
				List<Notice> notice = noticeRepository.getNoticeData(criteria);
				notice.stream().filter(n -> !n.isClosed()).forEach(n -> n.setClosed(true));
				notice.forEach(n -> {
					NoticeRequest request = new NoticeRequest();
					request.notice(n);
					noticeRepository.update(request);
				});

			}
		}
	}

	private void updatenotice(BPARequest bpaRequest) {
		// TODO Auto-generated method stub
		NoticeSearchCriteria criteria = new NoticeSearchCriteria();
		criteria.setBusinessid(bpaRequest.getBPA().getApplicationNo());
		criteria.setTenantid(bpaRequest.getBPA().getTenantId());
		List<Notice> notice = noticeRepository.getNoticeData(criteria);
		int reminder = notice.get(0).getReminderCount();
		notice.get(0).setReminderCount(++reminder);
		NoticeRequest request = new NoticeRequest();
		request.notice(notice.get(0));
		noticeRepository.update(request);

	}

	private boolean isRequestForRevokeAction(BPARequest bpaRequest) {
		// TODO Auto-generated method stub
		Boolean isActionRevoked = Boolean.FALSE;
		if (bpaRequest.getBPA().getStatus().equalsIgnoreCase("APPROVED")
				&& Objects.nonNull((Map) bpaRequest.getBPA().getAdditionalDetails())) {
			log.info("inside revoked action in bpa update");
			Map<String, Object> additionalDetails = (Map) bpaRequest.getBPA().getAdditionalDetails();
			Object obj = additionalDetails.get("isActionRevoked");
			if (Objects.nonNull(obj)) {
				isActionRevoked = (Boolean) obj;
			}
		}
		return isActionRevoked;
	}

	private void getEdcrDetailsForPreapprovedPlan(Map<String, String> edcrResponse, BPARequest bpaRequest) {

		log.info("edcr details for preapproved plan: ");
		PreapprovedPlanSearchCriteria preapprovedPlanSearchCriteria = new PreapprovedPlanSearchCriteria();
		preapprovedPlanSearchCriteria.setDrawingNo(bpaRequest.getBPA().getEdcrNumber());
		List<PreapprovedPlan> preapprovedPlans = preapprovedPlanService
				.getPreapprovedPlanFromCriteria(preapprovedPlanSearchCriteria);
		if (CollectionUtils.isEmpty(preapprovedPlans)) {
			log.error("no preapproved plan found for provided drawingNo:" + bpaRequest.getBPA().getEdcrNumber());
			throw new CustomException("no preapproved plan found for provided drawingNo",
					"no preapproved plan found for provided drawingNo");
		}
		PreapprovedPlan preapprovedPlanFromDb = preapprovedPlans.get(0);
		Map<String, Object> drawingDetail = (Map<String, Object>) preapprovedPlanFromDb.getDrawingDetail();

		edcrResponse.put(BPAConstants.SERVICETYPE, drawingDetail.get("serviceType") + "");// NEW_CONSTRUCTION
		edcrResponse.put(BPAConstants.APPLICATIONTYPE, drawingDetail.get("applicationType") + "");// BUILDING_PLAN_SCRUTINY

		// edcrResponse.put(BPAConstants.SERVICETYPE, "NEW_CONSTRUCTION");//
		// NEW_CONSTRUCTION
		// edcrResponse.put(BPAConstants.APPLICATIONTYPE, "BUILDING_PLAN_SCRUTINY");//
		// BUILDING_PLAN_SCRUTINY

	}

	/**
	 * Processes the building plan layout signature for a BPA application.
	 * 
	 * <p>
	 * This method handles the workflow when a building plan layout needs to be
	 * signed. It performs the following operations:
	 * <ul>
	 * <li>Validates that exactly one BPA application exists for the given
	 * request</li>
	 * <li>Updates additional details to mark the building plan layout as
	 * signed</li>
	 * <li>Stores the unsigned building plan layout details for reference</li>
	 * <li>Assigns IDs to newly added documents</li>
	 * <li>Updates the BPA application in the repository</li>
	 * </ul>
	 * 
	 * <p>
	 * <strong>Note:</strong> This method updates the eg_bpa_document table to
	 * unlink the old file store ID and link the new file store ID for the document
	 * type BPD.BPL.BPL (Building Plan Layout).
	 * 
	 * @param bpaRequest the BPA request containing the application details and new
	 *                   signed document
	 * @return the updated BPA object
	 * @throws CustomException if no BPA application or multiple BPA applications
	 *                         are found
	 */
	private BPA processBuildingPlanLayoutSignature(BPARequest bpaRequest) {
		log.info("Processing building plan layout signature for application");

		// Step 1: Validate that exactly one BPA application exists
		BPA existingBPA = validateAndRetrieveBPAForSignature(bpaRequest);

		// Step 2: Update additional details to mark building plan layout as signed
		updateAdditionalDetailsForSignature(bpaRequest, existingBPA);

		// Step 3: Store unsigned building plan layout details if present
		preserveUnsignedBuildingPlanLayoutDetails(bpaRequest, existingBPA);

		// Step 4: Assign IDs to newly added BPA documents
		assignIdsToNewDocuments(bpaRequest);

		// Step 5: Update the BPA application in repository
		repository.update(bpaRequest, true);

		return bpaRequest.getBPA();
	}

	/**
	 * Validates and retrieves the existing BPA application for signature
	 * processing.
	 * 
	 * <p>
	 * Ensures that exactly one BPA application exists for the given request to
	 * prevent ambiguous updates or operations on non-existent applications.
	 * 
	 * @param bpaRequest the BPA request containing application ID
	 * @return the existing BPA application
	 * @throws CustomException if no BPA application or multiple BPA applications
	 *                         are found
	 */
	private BPA validateAndRetrieveBPAForSignature(BPARequest bpaRequest) {
		List<BPA> searchResult = getBPAWithBPAId(bpaRequest);

		if (CollectionUtils.isEmpty(searchResult) || searchResult.size() > 1) {
			throw new CustomException(BPAErrorConstants.UPDATE_ERROR,
					"Failed to Update the Application, Found None or multiple applications!");
		}

		return searchResult.get(0);
	}

	/**
	 * Updates additional details to mark the building plan layout as signed.
	 * 
	 * <p>
	 * This method:
	 * <ul>
	 * <li>Removes the temporary 'applicationType' field from additional
	 * details</li>
	 * <li>Sets the 'buildingPlanLayoutIsSigned' flag to true</li>
	 * <li>Preserves the audit details from the existing BPA application</li>
	 * </ul>
	 * 
	 * @param bpaRequest  the BPA request to update
	 * @param existingBPA the existing BPA application containing original audit
	 *                    details
	 */
	private void updateAdditionalDetailsForSignature(BPARequest bpaRequest, BPA existingBPA) {
		Map<String, Object> additionalDetails = (Map) bpaRequest.getBPA().getAdditionalDetails();

		// Remove the temporary applicationType field (used only to route to this
		// method)
		additionalDetails.remove("applicationType");

		// Mark the building plan layout as signed
		additionalDetails.put("buildingPlanLayoutIsSigned", true);

		// Preserve original audit details from existing BPA
		bpaRequest.getBPA().setAuditDetails(existingBPA.getAuditDetails());
	}

	/**
	 * Preserves the unsigned building plan layout details in additional details for
	 * reference.
	 * 
	 * <p>
	 * If a building plan layout document exists in the existing BPA application,
	 * this method stores its document type and file store ID in the additional
	 * details map. This allows tracking the original unsigned document even after
	 * the signed version is uploaded.
	 * 
	 * @param bpaRequest  the BPA request to update
	 * @param existingBPA the existing BPA application containing the original
	 *                    documents
	 */
	private void preserveUnsignedBuildingPlanLayoutDetails(BPARequest bpaRequest, BPA existingBPA) {
		// Find the building plan layout document from existing BPA
		Optional<Document> buildingPlanLayoutDocument = existingBPA.getDocuments().stream()
				.filter(document -> document.getDocumentType().equals(BPAConstants.DOCUMENT_TYPE_BUILDING_PLAN_LAYOUT))
				.findFirst();

		// If building plan layout document exists, store its details
		if (buildingPlanLayoutDocument.isPresent()) {
			Map<String, Object> unsignedBuildingPlanLayoutDetails = new HashMap<>();
			unsignedBuildingPlanLayoutDetails.put(BPAConstants.CODE,
					buildingPlanLayoutDocument.get().getDocumentType());
			unsignedBuildingPlanLayoutDetails.put(BPAConstants.FILESTOREID,
					buildingPlanLayoutDocument.get().getFileStoreId());

			// Store unsigned document details in additional details
			Map<String, Object> additionalDetails = (Map) bpaRequest.getBPA().getAdditionalDetails();
			additionalDetails.put(BPAConstants.UNSIGNED_BUILDING_PLAN_LAYOUT_DETAILS,
					unsignedBuildingPlanLayoutDetails);
		}
	}

	/**
	 * Assigns unique IDs to newly added documents that don't have an ID yet.
	 * 
	 * <p>
	 * Documents uploaded as part of the signature process may not have IDs assigned
	 * yet. This method ensures all documents have a unique UUID before persisting
	 * to the database.
	 * 
	 * @param bpaRequest the BPA request containing documents to process
	 */
	private void assignIdsToNewDocuments(BPARequest bpaRequest) {
		if (!CollectionUtils.isEmpty(bpaRequest.getBPA().getDocuments())) {
			bpaRequest.getBPA().getDocuments().forEach(document -> {
				if (document.getId() == null) {
					document.setId(UUID.randomUUID().toString());
				}
			});
		}
	}

	private BPA processRevokedActionForBpaApplication(BPARequest bpaRequest) {

		List<BPA> searchResult = getBPAWithBPAId(bpaRequest);
		if (CollectionUtils.isEmpty(searchResult) || searchResult.size() > 1) {
			throw new CustomException(BPAErrorConstants.UPDATE_ERROR,
					"Failed to Update the Application, Found None or multiple applications!");
		}
		Map<String, Object> additionalDetails = (Map) bpaRequest.getBPA().getAdditionalDetails();
		// additionalDetails will always be a Map and will surely contain
		// applicationType then only this method invoked-
		additionalDetails.remove("isActionRevoked");
		additionalDetails.put("isApplicationStatusRevoked", true);
		bpaRequest.getBPA().setAuditDetails(searchResult.get(0).getAuditDetails());
		repository.update(bpaRequest, true);
		return bpaRequest.getBPA();
	}

	private BPA processEdcrNoUpdation(BPARequest bpaRequest) {
		log.info("inside method processEdcrNoUpdation");
		List<BPA> searchResult = getBPAWithBPAId(bpaRequest);
		if (CollectionUtils.isEmpty(searchResult) || searchResult.size() > 1) {
			throw new CustomException(BPAErrorConstants.UPDATE_ERROR,
					"Failed to Update the Application, Found None or multiple applications!");
		}
		((Map) bpaRequest.getBPA().getAdditionalDetails()).remove("applicationType");
		bpaRequest.getBPA().setAuditDetails(searchResult.get(0).getAuditDetails());
		repository.update(bpaRequest, true);
		return bpaRequest.getBPA();
	}

	private BPA processCommencementDateUpdate(BPARequest bpaRequest) {
		log.info("inside method processCommencementDateUpdate");
		List<BPA> searchResult = getBPAWithBPAId(bpaRequest);
		if (CollectionUtils.isEmpty(searchResult) || searchResult.size() > 1) {
			throw new CustomException(BPAErrorConstants.UPDATE_ERROR,
					"Failed to Update the Application, Found None or multiple applications!");
		}
		bpaRequest.getBPA().setAuditDetails(searchResult.get(0).getAuditDetails());
		repository.update(bpaRequest, true);
		return bpaRequest.getBPA();
	}

	private boolean isRequestForBuildingPlanLayoutSignature(BPARequest bpaRequest) {
		return ((bpaRequest.getBPA().getStatus().equalsIgnoreCase("APPROVED")
				|| bpaRequest.getBPA().getStatus().equalsIgnoreCase("PENDING_SANC_FEE_PAYMENT"))
				&& Objects.nonNull(bpaRequest.getBPA().getAdditionalDetails())
				&& bpaRequest.getBPA().getAdditionalDetails() instanceof Map
				&& "buildingPlanLayoutSignature"
						.equals(((Map) bpaRequest.getBPA().getAdditionalDetails()).get("applicationType"))
				&& !(Objects
						.nonNull(((Map) bpaRequest.getBPA().getAdditionalDetails()).get("buildingPlanLayoutIsSigned"))
						&& ((boolean) ((Map) bpaRequest.getBPA().getAdditionalDetails())
								.get("buildingPlanLayoutIsSigned"))));
	}

	private boolean isRequestForEdcrNoUpdation(BPARequest bpaRequest) {
		return (Objects.nonNull(bpaRequest.getBPA().getAdditionalDetails())
				&& bpaRequest.getBPA().getAdditionalDetails() instanceof Map
				&& "edcrNoUpdation".equals(((Map) bpaRequest.getBPA().getAdditionalDetails()).get("applicationType")));
	}

	private boolean isRequestForCommencementDateUpdate(BPARequest bpaRequest) {
		return (Objects.nonNull(bpaRequest.getBPA().getAdditionalDetails())
				&& bpaRequest.getBPA().getAdditionalDetails() instanceof Map
				&& !ObjectUtils.isEmpty(((Map) bpaRequest.getBPA().getAdditionalDetails()).get("CommencementDate")));
	}

	/**
	 * handle the reject and Send Back action of the update
	 * 
	 * @param applicationType
	 * @param bpaRequest
	 * @param businessService
	 * @param searchResult
	 * @param mdmsData
	 * @param edcrResponse
	 */
	private void handleRejectSendBackActions(String applicationType, BPARequest bpaRequest,
			BusinessService businessService, List<BPA> searchResult, Object mdmsData,
			Map<String, String> edcrResponse) {
		BPA bpa = bpaRequest.getBPA();
		if (bpa.getWorkflow().getAction() != null
				&& (bpa.getWorkflow().getAction().equalsIgnoreCase(BPAConstants.ACTION_REJECT)
						|| bpa.getWorkflow().getAction().equalsIgnoreCase(BPAConstants.ACTION_REVOCATE))) {

			if (bpa.getWorkflow().getComments() == null || bpa.getWorkflow().getComments().isEmpty()) {
				throw new CustomException(BPAErrorConstants.BPA_UPDATE_ERROR_COMMENT_REQUIRED,
						"Comment is mandaotory, please provide the comments ");
			}
			// below line commented because NOC update not needed for REJECTED appls
			// nocService.handleBPARejectedStateForNoc(bpaRequest);

			// Check If Refusal SCN has been Created or not for the BPA Application
			if (config.getBpaRefusalShowCauseNoticeEnable()) {
				validateRefusalScn(bpa);
			}

		} else {

			if (!bpa.getWorkflow().getAction().equalsIgnoreCase(BPAConstants.ACTION_SENDBACKTOCITIZEN)) {
				actionValidator.validateUpdateRequest(bpaRequest, businessService);
				bpaValidator.validateUpdate(bpaRequest, searchResult, mdmsData,
						workflowService.getCurrentState(bpa.getStatus(), businessService), edcrResponse);
				if (!applicationType.equalsIgnoreCase(BPAConstants.BUILDING_PLAN_OC)) {
					landService.updateLandInfo(bpaRequest);
				}
				bpaValidator.validateCheckList(mdmsData, bpaRequest,
						workflowService.getCurrentState(bpa.getStatus(), businessService));
			}
		}
	}

	private void validateRefusalScn(BPA bpa) {
		if (BPAConstants.ACTION_REJECT.equalsIgnoreCase(bpa.getWorkflow().getAction())) {
			String applicationNo = bpa.getApplicationNo();
			NoticeSearchCriteria noticeSearchCriteria = NoticeSearchCriteria.builder().businessid(applicationNo)
					.letterType(BPAConstants.REFUSAL_SHOWCAUSE_LETTER_TYPE).build();

			List<Notice> notices = scnRepository.getNoticeData(noticeSearchCriteria);

			if (CollectionUtils.isEmpty(notices)) {
				throw new CustomException("NOTICE_NOT_EXIST", String.format(
						"Refusal SCN not exists for business ID: %s. Create Refusal Show Cause Notice before Rejecting the Application",
						applicationNo));
			}

		}

	}

	/**
	 * Processes the update logic specific to Occupancy Certificate (OC)
	 * applications.
	 * 
	 * <p>
	 * This method handles the special processing required when updating an OC
	 * application. It performs the following operations:
	 * <ul>
	 * <li>Validates that the application type is an OC application</li>
	 * <li>Retrieves the associated BPA application using the approval/permit
	 * number</li>
	 * <li>Validates the status of the associated BPA (not revocated, approved)</li>
	 * <li>Enriches the OC application with land information and additional
	 * details</li>
	 * <li>Performs additional data validation for non-outside-Sujog
	 * applications</li>
	 * </ul>
	 * 
	 * <p>
	 * <strong>Note:</strong> OC Outside Sujog applications have special handling
	 * where certain validations are skipped or modified.
	 * 
	 * @param applicationType   the type of application (should be BUILDING_PLAN_OC)
	 * @param approvalNo        the approval/permit number of the associated BPA
	 *                          application
	 * @param bpaRequest        the OC BPA request to be processed
	 * @param requestInfo       the request information for service calls
	 * @param additionalDetails the map to populate with additional details like
	 *                          land ID
	 * @throws CustomException if BPA not found, multiple BPAs found, BPA is
	 *                         revocated, or BPA is not approved (for
	 *                         non-outside-Sujog applications)
	 */
	private void processOcUpdate(String applicationType, String approvalNo, BPARequest bpaRequest,
			RequestInfo requestInfo, Map<String, String> additionalDetails) {

		// Only process if this is an OC application
		if (applicationType.equalsIgnoreCase(BPAConstants.BUILDING_PLAN_OC)) {

			// Step 1: Retrieve the associated BPA application
			BPA associatedBPA = retrieveAssociatedBPAForOC(approvalNo, bpaRequest, requestInfo);

			// Step 2: Validate the status of the associated BPA
			validateAssociatedBPAStatus(associatedBPA, bpaRequest, approvalNo);

			// Step 3: Enrich the OC request with land information and additional details
			enrichOCRequestWithBPAData(bpaRequest, associatedBPA, additionalDetails);

			// Step 4: Validate additional data (skip for OC Outside Sujog applications)
			if (!bpaRequest.getBPA().getOCOutsideSujogApplication()) {
				BPASearchCriteria criteria = new BPASearchCriteria();
				criteria.setEdcrNumber(associatedBPA.getEdcrNumber());
				criteria.setTenantId(associatedBPA.getTenantId());
				ocService.validateAdditionalData(bpaRequest, criteria);
			}
		}
	}

	/**
	 * Retrieves the associated BPA application for an Occupancy Certificate using
	 * the approval number.
	 * 
	 * <p>
	 * This method builds a search criteria based on the approval number and tenant
	 * ID. For OC Outside Sujog applications, additional criteria are added to
	 * search by application number.
	 * 
	 * @param approvalNo  the approval/permit number to search for
	 * @param bpaRequest  the OC BPA request containing tenant and application
	 *                    details
	 * @param requestInfo the request information for the search service call
	 * @return the associated BPA application
	 * @throws CustomException if no BPA found or multiple BPAs found for the
	 *                         approval number
	 */
	private BPA retrieveAssociatedBPAForOC(String approvalNo, BPARequest bpaRequest, RequestInfo requestInfo) {
		// Build search criteria
		BPASearchCriteria criteria = new BPASearchCriteria();
		criteria.setTenantId(bpaRequest.getBPA().getTenantId());
		criteria.setApprovalNo(approvalNo);
		criteria.setIsRevalidationApplication(Boolean.FALSE);

		// For OC Outside Sujog applications, add additional search criteria
		if (bpaRequest.getBPA().getOCOutsideSujogApplication()) {
			criteria.setIsOCOutsideSujogApplication(true);
			criteria.setApplicationNo(bpaRequest.getBPA().getApplicationNo());
			criteria.setIsRevalidationApplication(Boolean.FALSE);
		}

		// Search for BPA applications
		List<BPA> bpas = search(criteria, requestInfo);

		// Validate exactly one BPA found
		if (bpas.size() <= 0 || bpas.size() > 1) {
			throw new CustomException(BPAErrorConstants.UPDATE_ERROR,
					((bpas.size() <= 0) ? "BPA not found with approval Number: "
							: "Multiple BPA applications found for approval number: ") + approvalNo);
		}

		return bpas.get(0);
	}

	/**
	 * Validates the status of the associated BPA application.
	 * 
	 * <p>
	 * This method ensures that:
	 * <ul>
	 * <li>The associated BPA is not revocated (applies to all OC applications)</li>
	 * <li>The associated BPA is approved (applies only to non-outside-Sujog
	 * applications)</li>
	 * </ul>
	 * 
	 * <p>
	 * <strong>Note:</strong> OC Outside Sujog applications can proceed even if the
	 * BPA is not in APPROVED status, allowing more flexibility for outside system
	 * integrations.
	 * 
	 * @param associatedBPA the associated BPA application to validate
	 * @param bpaRequest    the OC BPA request
	 * @param approvalNo    the approval number for error messages
	 * @throws CustomException if BPA is revocated or not approved (for
	 *                         non-outside-Sujog)
	 */
	private void validateAssociatedBPAStatus(BPA associatedBPA, BPARequest bpaRequest, String approvalNo) {
		// Check if the BPA is revocated (applies to all OC applications)
		if (associatedBPA.getStatus().equalsIgnoreCase(BPAConstants.STATUS_REVOCATED)) {
			throw new CustomException(BPAErrorConstants.UPDATE_ERROR,
					"This permit number is revocated you cannot use this permit number");
		}

		// Check if the BPA is approved (only for non-outside-Sujog applications)
		if (!associatedBPA.getStatus().equalsIgnoreCase(BPAConstants.STATUS_APPROVED)) {
			// OC Outside Sujog applications are allowed to proceed even if not approved
			if (!bpaRequest.getBPA().getOCOutsideSujogApplication()) {
				throw new CustomException(BPAErrorConstants.UPDATE_ERROR,
						"The selected permit number still in workflow approval process, Please apply occupancy after completing approval process.");
			}
		}
	}

	/**
	 * Enriches the OC request with data from the associated BPA application.
	 * 
	 * <p>
	 * This method populates:
	 * <ul>
	 * <li>Land ID in the additional details map</li>
	 * <li>Land information in the OC BPA request</li>
	 * </ul>
	 * 
	 * @param bpaRequest        the OC BPA request to enrich
	 * @param associatedBPA     the associated BPA application containing land
	 *                          information
	 * @param additionalDetails the map to populate with land ID
	 */
	private void enrichOCRequestWithBPAData(BPARequest bpaRequest, BPA associatedBPA,
			Map<String, String> additionalDetails) {
		// Add land ID to additional details
		additionalDetails.put("landId", associatedBPA.getLandId());

		// Set land information in the OC BPA request
		bpaRequest.getBPA().setLandInfo(associatedBPA.getLandInfo());
	}

	/**
	 * Returns bpa from db for the update request
	 * 
	 * @param request The update request
	 * @return List of bpas
	 */
	public List<BPA> getBPAWithBPAId(BPARequest request) {
		BPASearchCriteria criteria = new BPASearchCriteria();
		List<String> ids = new LinkedList<>();
		ids.add(request.getBPA().getId());
		criteria.setTenantId(request.getBPA().getTenantId());
		criteria.setIds(ids);
		List<BPA> bpa = repository.getBPAData(criteria, null);
		return bpa;
	}

	/**
	 * Downloads the EDCR (Electronic Development Control Regulations) Report from
	 * the EDCR system, stamps the permit number and generated date on the
	 * downloaded PDF, and returns the modified document.
	 * 
	 * <p>
	 * This method orchestrates the following workflow:
	 * <ol>
	 * <li>Validates that the approval number exists in the BPA request</li>
	 * <li>Downloads the EDCR report and creates a temporary file</li>
	 * <li>Retrieves localized message templates for permit number and generation
	 * date</li>
	 * <li>Stamps the permit number and date on all pages of the PDF</li>
	 * <li>Ensures proper resource cleanup in all scenarios</li>
	 * </ol>
	 * 
	 * @param bpaRequest The BPA request containing building permit application
	 *                   details
	 * @throws CustomException if approval number is missing, download fails, or PDF
	 *                         cannot be processed
	 */
	@SuppressWarnings("resource")
	public void getEdcrPdf(BPARequest bpaRequest) {
		// Validate input and prepare initial resources
		validateApprovalNumber(bpaRequest);

		String fileName = BPAConstants.EDCR_PDF;
		PDDocument document = null;

		try {
			// Step 1: Download the EDCR report from external system
			this.createTempReport(bpaRequest, fileName, document);

			// Step 2: Retrieve localized labels for stamping
			LocalizedLabels labels = retrieveLocalizedLabels(bpaRequest);

			// Step 3: Stamp permit details on the PDF document
			this.addDataToPdf(document, bpaRequest, labels.getPermitNoLabel(), labels.getGeneratedOnLabel(), fileName);

		} catch (Exception ex) {
			log.error("Exception occurred while downloading and processing EDCR PDF: {}", ex.getMessage(), ex);
			throw new CustomException(BPAErrorConstants.UNABLE_TO_DOWNLOAD, "Unable to download the file");
		} finally {
			// Ensure PDF document is properly closed to prevent resource leaks
			closePdfDocumentSafely(document);
		}
	}

	/**
	 * Validates that the BPA request contains a valid approval number.
	 * 
	 * @param bpaRequest The BPA request to validate
	 * @throws CustomException if approval number is null or empty
	 */
	private void validateApprovalNumber(BPARequest bpaRequest) {
		BPA bpa = bpaRequest.getBPA();
		if (StringUtils.isEmpty(bpa.getApprovalNo())) {
			throw new CustomException(BPAErrorConstants.INVALID_REQUEST,
					"Approval Number is required to generate EDCR PDF.");
		}
	}

	/**
	 * Retrieves localized labels for permit number and generated date from the
	 * localization service. Falls back to default constants if localization fails.
	 * 
	 * @param bpaRequest The BPA request containing tenant and request information
	 * @return LocalizedLabels object containing the permit number and generated
	 *         date labels
	 */
	private LocalizedLabels retrieveLocalizedLabels(BPARequest bpaRequest) {
		BPA bpa = bpaRequest.getBPA();

		// Fetch all localization messages for the tenant
		String localizationMessages = notificationUtil.getLocalizationMessages(bpa.getTenantId(),
				bpaRequest.getRequestInfo());

		// Extract specific message templates with fallback to default values
		String permitNoLabel = notificationUtil.getMessageTemplate(BPAConstants.PERMIT_ORDER_NO, localizationMessages);
		permitNoLabel = permitNoLabel != null ? permitNoLabel : BPAConstants.PERMIT_ORDER_NO;

		String generatedOnLabel = notificationUtil.getMessageTemplate(BPAConstants.GENERATEDON, localizationMessages);
		generatedOnLabel = generatedOnLabel != null ? generatedOnLabel : BPAConstants.GENERATEDON;

		return new LocalizedLabels(permitNoLabel, generatedOnLabel);
	}

	/**
	 * Safely closes the PDF document, handling any exceptions that may occur.
	 * 
	 * @param document The PDDocument to close (may be null)
	 * @throws CustomException if document closure fails
	 */
	private void closePdfDocumentSafely(PDDocument document) {
		try {
			if (document != null) {
				document.close();
				log.debug("PDF document closed successfully");
			}
		} catch (Exception ex) {
			log.error("Failed to close PDF document: {}", ex.getMessage(), ex);
			throw new CustomException(BPAErrorConstants.INVALID_FILE, "Unable to close the PDF file properly");
		}
	}

	/**
	 * Helper class to hold localized label values for PDF stamping. Improves
	 * readability by avoiding multiple return values.
	 */
	private static class LocalizedLabels {
		private final String permitNoLabel;
		private final String generatedOnLabel;

		public LocalizedLabels(String permitNoLabel, String generatedOnLabel) {
			this.permitNoLabel = permitNoLabel;
			this.generatedOnLabel = generatedOnLabel;
		}

		public String getPermitNoLabel() {
			return permitNoLabel;
		}

		public String getGeneratedOnLabel() {
			return generatedOnLabel;
		}
	}

	/**
	 * Retrieves the actual download URL for the EDCR report PDF from the EDCR
	 * service.
	 * 
	 * <p>
	 * This method handles URL redirection and validates that the final URL points
	 * to a valid PDF document. The EDCR service may initially return a redirect URL
	 * (HTTP 302) which needs to be followed to get the actual download URL.
	 * 
	 * @param bpaRequest The BPA request containing application details needed to
	 *                   fetch the EDCR report
	 * @return URL object pointing to the actual PDF download location
	 * @throws Exception       if the URL cannot be resolved, redirects fail, or
	 *                         content type is not PDF
	 * @throws CustomException if the download URL doesn't contain a valid PDF or
	 *                         redirect header is missing
	 */
	private URL getEdcrReportDownloaUrl(BPARequest bpaRequest) throws Exception {
		// Step 1: Get initial PDF URL from EDCR service
		String pdfUrl = edcrService.getEDCRPdfUrl(bpaRequest);
		URL downloadUrl = new URL(pdfUrl);

		log.debug("Connecting to initial EDCR URL: {} ... ", downloadUrl.toString());
		URLConnection urlConnection = downloadUrl.openConnection();

		// Step 2: Check if URL directly points to PDF or requires redirect
		if (!urlConnection.getContentType().equalsIgnoreCase("application/pdf")) {
			log.debug("Initial URL is not a direct PDF link, checking for redirect...");

			// Step 3: Follow the Location header for the actual download URL
			String downloadUrlString = urlConnection.getHeaderField("Location");

			if (!StringUtils.isEmpty(downloadUrlString)) {
				downloadUrl = new URL(downloadUrlString);
				log.debug("Following redirect to download URL: {} ... ", downloadUrl.toString());

				// Step 4: Validate that the redirected URL is a PDF
				urlConnection = downloadUrl.openConnection();
				if (!urlConnection.getContentType().equalsIgnoreCase("application/pdf")) {
					log.error("Redirected download URL content type is not application/pdf. Content-Type: {}",
							urlConnection.getContentType());
					throw new CustomException(BPAErrorConstants.INVALID_EDCR_REPORT,
							"Download url content type is not application/pdf.");
				}
			} else {
				log.error("Unable to fetch the Location header from EDCR redirect response");
				throw new CustomException(BPAErrorConstants.INVALID_EDCR_REPORT,
						"Unable to fetch the location header URL from EDCR service");
			}
		}

		log.info("Successfully resolved EDCR report download URL");
		return downloadUrl;
	}

	/**
	 * Downloads the EDCR report from the resolved URL and creates a temporary PDF
	 * file on the local filesystem.
	 * 
	 * <p>
	 * This method performs the following operations:
	 * <ol>
	 * <li>Resolves the actual download URL (handling redirects)</li>
	 * <li>Streams the PDF content from the URL to a local temporary file</li>
	 * <li>Loads the downloaded PDF into a PDDocument object for further
	 * processing</li>
	 * </ol>
	 * 
	 * <p>
	 * The method uses buffered streaming to efficiently handle large PDF files
	 * without loading the entire content into memory at once.
	 * 
	 * @param bpaRequest The BPA request containing application details
	 * @param fileName   The name of the temporary file to create
	 * @param document   The PDDocument object (unused parameter, kept for backward
	 *                   compatibility)
	 * @throws Exception if URL resolution fails, download fails, or PDF cannot be
	 *                   loaded
	 */
	private void createTempReport(BPARequest bpaRequest, String fileName, PDDocument document) throws Exception {
		// Step 1: Resolve the actual download URL (handles redirects)
		URL downloadUrl = this.getEdcrReportDownloaUrl(bpaRequest);

		// Step 2: Stream the PDF content to a local temporary file
		log.debug("Starting download of EDCR report from: {}", downloadUrl);

		FileOutputStream writeStream = null;
		InputStream readStream = null;

		try {
			writeStream = new FileOutputStream(fileName);
			readStream = downloadUrl.openStream();

			// Buffer for reading PDF content in chunks
			byte[] byteChunk = new byte[1024]; // 1KB buffer
			int bytesRead;

			// Stream the PDF content chunk by chunk
			while ((bytesRead = readStream.read(byteChunk)) != -1) {
				writeStream.write(byteChunk, 0, bytesRead);
			}

			// Ensure all buffered data is written to disk
			writeStream.flush();
			log.debug("EDCR report successfully downloaded to temporary file: {}", fileName);

		} finally {
			// Close streams to release system resources
			if (writeStream != null) {
				writeStream.close();
			}
			if (readStream != null) {
				readStream.close();
			}
		}

		// Step 3: Load the downloaded PDF for further processing
		document = PDDocument.load(new File(fileName));
		log.debug("PDF document loaded successfully from file: {}", fileName);
	}

	/**
	 * download the edcr shortened report and create in tempfile
	 * 
	 * @param bpaRequest
	 * @param fileName
	 * @param document
	 * @throws Exception
	 */
	private void createTempShortenedReport(BPARequest bpaRequest, String fileName, PDDocument document)
			throws Exception {
		log.info("inside method createTempShortenedReport");
		String downloadUrlString = edcrService.getEDCRShortenedPdfUrl(bpaRequest);
		log.info("downloadUrlString:" + downloadUrlString);
		if (downloadUrlString.contains("https")) {
			// replace https with http as getting unable to download file-
			downloadUrlString = downloadUrlString.replace("https", "http");
		}
		URL downloadUrl = new URL(downloadUrlString);
		log.info("downloadUrl: " + downloadUrl);
		// Read the PDF from the URL and save to a local file
		FileOutputStream writeStream = new FileOutputStream(fileName);
		byte[] byteChunck = new byte[1024];
		int baLength;
		InputStream readStream = downloadUrl.openStream();
		log.info("input stream opened with downloadUrl");
		while ((baLength = readStream.read(byteChunck)) != -1) {
			writeStream.write(byteChunck, 0, baLength);
		}
		log.info("before flush writestream");
		writeStream.flush();
		writeStream.close();
		log.info("write stream closed");
		readStream.close();
		log.info("read stream closed");

		document = PDDocument.load(new File(fileName));
		log.info("loaded PDDocument from fileName: " + fileName);
		document.close();
		log.info("finished execution of method createTempShortenedReport");
	}

	/**
	 * Stamps permit number and approval/generation date on all pages of the EDCR
	 * PDF document.
	 * 
	 * <p>
	 * This method adds a header to each page of the PDF containing:
	 * <ul>
	 * <li>Permit Order Number (left side of header)</li>
	 * <li>Generated/Approval Date (right side of header, offset by 436 points)</li>
	 * </ul>
	 * 
	 * <p>
	 * The stamped text is positioned at the top of each page with a standard margin
	 * and uses Times Roman font at 10pt size for consistency with official
	 * documents.
	 * 
	 * @param document         The PDDocument to stamp (must be loaded and not null)
	 * @param bpaRequest       The BPA request containing permit and approval
	 *                         details
	 * @param permitNoLabel    Localized label for permit number (e.g., "Permit
	 *                         Order No")
	 * @param generatedOnLabel Localized label for generation date (e.g., "Generated
	 *                         On")
	 * @param fileName         The file path where the modified PDF will be saved
	 * @throws IOException if PDF manipulation or file writing fails
	 */
	private void addDataToPdf(PDDocument document, BPARequest bpaRequest, String permitNoLabel, String generatedOnLabel,
			String fileName) throws IOException {

		PDPageTree allPages = document.getDocumentCatalog().getPages();
		BPA bpa = bpaRequest.getBPA();

		// Iterate through all pages in the document
		for (int i = 0; i < allPages.getCount(); i++) {
			PDPage page = (PDPage) allPages.get(i);

			// Create content stream for adding text to the page
			// Parameters: (document, page, appendContent=true, compress=true,
			// resetContext=true)
			@SuppressWarnings("deprecation")
			PDPageContentStream contentStream = new PDPageContentStream(document, page, true, true, true);

			// Configure font settings for the stamped text
			PDFont font = PDType1Font.TIMES_ROMAN;
			float fontSize = 10.0f;

			// Begin text mode for content stream
			contentStream.beginText();
			contentStream.setFont(font, fontSize);

			// Calculate position for text at top of page with standard margin
			PDRectangle mediabox = page.getMediaBox();
			float margin = 32; // Standard margin in points
			float startX = mediabox.getLowerLeftX() + margin; // Left margin
			float startY = mediabox.getUpperRightY() - (margin / 2); // Top margin (half of standard)

			// Position cursor at the calculated starting point
			contentStream.newLineAtOffset(startX, startY);

			// Add permit number on the left side
			contentStream.showText(permitNoLabel + " : " + bpa.getApprovalNo());

			// Add approval/generation date on the right side
			if (bpa.getApprovalDate() != null) {
				// Format the approval date as dd/MM/yyyy
				Date date = new Date(bpa.getApprovalDate());
				DateFormat format = new SimpleDateFormat("dd/MM/yyyy");
				String formattedDate = format.format(date);

				// Offset to right side of page (436 points from permit number)
				contentStream.newLineAtOffset(436, 0);
				contentStream.showText(generatedOnLabel + " : " + formattedDate);
			} else {
				// Handle case where approval date is not available
				contentStream.newLineAtOffset(436, 0);
				contentStream.showText(generatedOnLabel + " : " + "NA");
			}

			// End text mode and close the content stream
			contentStream.endText();
			contentStream.close();
		}

		// Save the modified document back to the file
		document.save(fileName);
		log.debug("Successfully stamped permit details on {} pages of the PDF", allPages.getCount());
	}

	/**
	 * Updates the BPA Document ID from the DSC (Digital Signature Certificate)
	 * service.
	 * 
	 * <p>
	 * This method handles the digital signing workflow for building permit
	 * applications:
	 * <ol>
	 * <li>Sets up audit details for tracking the update operation</li>
	 * <li>Retrieves and validates the existing BPA record</li>
	 * <li>Validates the DSC details against business rules</li>
	 * <li>Sets approval date and validity period for the permit</li>
	 * <li>Persists the updated DSC details to the database</li>
	 * </ol>
	 * 
	 * <p>
	 * The approval date is set during digital signing (not after payment) to ensure
	 * the permit validity period starts from the signing date.
	 * 
	 * @param bpaRequest The BPA request containing application details and DSC
	 *                   information
	 * @return Updated BPA object with DSC details and approval information
	 * @throws CustomException if validation fails or BPA record is not found
	 */
	public BPA updateDscDetails(BPARequest bpaRequest) {
		// Step 1: Set up audit details for tracking who made the update and when
		setupAuditDetails(bpaRequest);

		// Step 2: Retrieve existing BPA record from the database
		List<BPA> existingBpaRecords = retrieveExistingBpaRecord(bpaRequest);

		// Step 3: Validate DSC details against business rules
		bpaValidator.validateDscDetails(bpaRequest, existingBpaRecords);

		// Step 4: Set approval date and validity period (done during signing, not after
		// payment)
		setApprovalDateInBpa(bpaRequest);

		// Step 5: Persist the updated DSC details to the database
		repository.updateDscDetails(bpaRequest);

		log.info("Successfully updated DSC details for application: {}", bpaRequest.getBPA().getApplicationNo());

		return bpaRequest.getBPA();
	}

	/**
	 * Updates the BPA Plan Document ID from the DSC service for plan-specific
	 * digital signatures.
	 * 
	 * <p>
	 * Similar to {@link #updateDscDetails(BPARequest)}, but specifically handles
	 * digital signing of building plan documents rather than the entire permit
	 * application.
	 * 
	 * @param bpaRequest The BPA request containing plan details and DSC information
	 * @return Updated BPA object with plan DSC details
	 * @throws CustomException if validation fails or BPA record is not found
	 */
	public BPA updatePlanDscDetails(BPARequest bpaRequest) {
		// Step 1: Set up audit details for tracking the update
		setupAuditDetails(bpaRequest);

		// Step 2: Retrieve existing BPA record for validation
		List<BPA> existingBpaRecords = retrieveExistingBpaRecord(bpaRequest);

		// Step 3: Validate plan-specific DSC details
		bpaValidator.validatePlanDscDetails(bpaRequest, existingBpaRecords);

		// Step 4: Persist the updated plan DSC details
		repository.updatePlanDscDetails(bpaRequest);

		log.info("Successfully updated plan DSC details for application: {}", bpaRequest.getBPA().getApplicationNo());

		return bpaRequest.getBPA();
	}

	/**
	 * Sets up audit details for tracking who is making the update and when.
	 * Attaches audit information to the BPA object in the request.
	 * 
	 * @param bpaRequest The BPA request to populate with audit details
	 */
	private void setupAuditDetails(BPARequest bpaRequest) {
		String userUuid = bpaRequest.getRequestInfo().getUserInfo().getUuid();
		AuditDetails auditDetails = util.getAuditDetails(userUuid, false);
		bpaRequest.getBPA().setAuditDetails(auditDetails);

		log.debug("Audit details set for user: {}", userUuid);
	}

	/**
	 * Retrieves the existing BPA record from the database based on tenant ID and
	 * application number.
	 * 
	 * <p>
	 * This method constructs a search criteria and queries the database to fetch
	 * the current state of the BPA application before applying updates.
	 * 
	 * @param bpaRequest The BPA request containing tenant and application
	 *                   identifiers
	 * @return List of BPA records matching the search criteria (typically one
	 *         record)
	 */
	private List<BPA> retrieveExistingBpaRecord(BPARequest bpaRequest) {
		BPA bpa = bpaRequest.getBPA();

		// Build search criteria using tenant and application number
		BPASearchCriteria criteria = new BPASearchCriteria();
		criteria.setTenantId(bpa.getTenantId());
		criteria.setApplicationNo(bpa.getApplicationNo());

		// Query the database for existing BPA records
		List<BPA> searchResult = getBPAFromCriteria(criteria, bpaRequest.getRequestInfo(), Collections.EMPTY_LIST);

		log.debug("Retrieved {} BPA record(s) for application: {}", searchResult.size(), bpa.getApplicationNo());

		return searchResult;
	}

	/**
	 * Sets the approval date and calculates the validity date for the BPA permit.
	 * 
	 * <p>
	 * This method performs two key operations:
	 * <ol>
	 * <li>Sets the approval date to the current timestamp</li>
	 * <li>Calculates and stores the validity expiration date based on configured
	 * validity period</li>
	 * </ol>
	 * 
	 * <p>
	 * The validity period is configurable (typically 36 months / 3 years) and is
	 * added to the approval date to determine when the permit expires. The validity
	 * date is stored in the additional details map for future reference.
	 * 
	 * <p>
	 * This method is called during the digital signing process, ensuring the
	 * approval date reflects when the permit was officially signed rather than when
	 * payment was made.
	 * 
	 * @param bpaRequest The BPA request to update with approval and validity dates
	 */
	private void setApprovalDateInBpa(BPARequest bpaRequest) {
		BPA bpa = bpaRequest.getBPA();

		// Step 1: Set approval date to current timestamp
		long approvalDate = Calendar.getInstance().getTimeInMillis();
		bpa.setApprovalDate(approvalDate);

		log.debug("Approval date set to: {}", new Date(approvalDate));

		// Step 2: Calculate validity expiration date
		long validityDate = calculateValidityDate();

		// Step 3: Store validity date in additional details
		storeValidityDateInAdditionalDetails(bpa, validityDate);

		log.info("Approval date and validity date set for application: {}", bpa.getApplicationNo());
	}

	/**
	 * Calculates the permit validity expiration date based on the configured
	 * validity period.
	 * 
	 * <p>
	 * The validity period is typically configured as 36 months (3 years) but can be
	 * customized through application configuration.
	 * 
	 * @return Validity expiration date as timestamp in milliseconds
	 */
	private long calculateValidityDate() {
		Calendar calendar = Calendar.getInstance();

		// Get configured validity period (e.g., 36 months for 3 years)
		int validityInMonths = config.getValidityInMonths();

		// Add validity period to current date
		calendar.add(Calendar.MONTH, validityInMonths);

		long validityDate = calendar.getTimeInMillis();

		log.debug("Calculated validity date: {} (adding {} months)", new Date(validityDate), validityInMonths);

		return validityDate;
	}

	/**
	 * Stores the validity date in the BPA's additional details map.
	 * 
	 * <p>
	 * If the additional details map doesn't exist, it creates a new one. This
	 * ensures the validity date is persisted with the BPA record for future
	 * reference.
	 * 
	 * @param bpa          The BPA object to update
	 * @param validityDate The validity expiration date timestamp
	 */
	private void storeValidityDateInAdditionalDetails(BPA bpa, long validityDate) {
		// Get or create additional details map
		Map<String, Object> additionalDetails = getOrCreateAdditionalDetails(bpa);

		// Store validity date in the map
		additionalDetails.put("validityDate", validityDate);

		log.debug("Validity date stored in additional details");
	}

	/**
	 * Retrieves the existing additional details map or creates a new one if it
	 * doesn't exist.
	 * 
	 * <p>
	 * This helper method ensures we always have a valid map to work with when
	 * storing validity and other metadata.
	 * 
	 * @param bpa The BPA object to get or set additional details for
	 * @return The additional details map (never null)
	 */
	@SuppressWarnings("unchecked")
	private Map<String, Object> getOrCreateAdditionalDetails(BPA bpa) {
		Map<String, Object> additionalDetails;

		if (bpa.getAdditionalDetails() != null) {
			// Use existing additional details map
			additionalDetails = (Map<String, Object>) bpa.getAdditionalDetails();
		} else {
			// Create new map and attach to BPA
			additionalDetails = new HashMap<>();
			bpa.setAdditionalDetails(additionalDetails);
			log.debug("Created new additional details map for BPA");
		}

		return additionalDetails;
	}

	/**
	 * Searches the bpas that are approved but signing is pending
	 * 
	 * @param criteria
	 * @param requestInfo
	 * @return
	 */
	public List<DscDetails> searchDscDetails(BPASearchCriteria criteria, RequestInfo requestInfo) {
		List<DscDetails> pendingDigitalsignDocuments = new LinkedList<>();

		bpaValidator.validateDscSearch(criteria, requestInfo);
		pendingDigitalsignDocuments = repository.getDscDetails(criteria);

		return pendingDigitalsignDocuments;
	}

	/**
	 * Searches the bpas that are approved but plan pdf signing is pending
	 * 
	 * @param criteria
	 * @param requestInfo
	 * @return
	 */
	public List<DscDetails> searchPlanDscDetails(BPASearchCriteria criteria, RequestInfo requestInfo) {
		List<DscDetails> pendingDigitalsignDocuments = new LinkedList<>();

		bpaValidator.validateDscSearch(criteria, requestInfo);
		pendingDigitalsignDocuments = repository.getPlanDscDetails(criteria);

		return pendingDigitalsignDocuments;
	}

	/**
	 * call BPA-calculator and fetch the fee estimate
	 * 
	 * @param bpaRequest
	 * @return
	 */
	public Object getFeeEstimateFromBpaCalculator(Object bpaRequest) {
		return calculationService.callBpaCalculatorEstimate(bpaRequest);
	}

	/**
	 * call BPA-calculator and fetch all installments
	 * 
	 * @param bpaRequest
	 * @return
	 */
	public Object generateDemandFromInstallments(Object bpaRequest) {
		return calculationService.generateDemandFromInstallments(bpaRequest);
	}

	/**
	 * call BPA-calculator and fetch all installments
	 * 
	 * @param bpaRequest
	 * @return
	 */
	public Object getAllInstallmentsFromBpaCalculator(Object bpaRequest) {
		return calculationService.getAllInstallments(bpaRequest);
	}

	/**
	 * Merges the scrutiny report with the permit certificate and uploads the
	 * combined PDF to filestore.
	 * 
	 * <p>
	 * This method orchestrates the following workflow:
	 * <ol>
	 * <li>Validates that approval number exists in the BPA request</li>
	 * <li>Generates unique file names for temporary and merged PDFs</li>
	 * <li>Downloads the shortened scrutiny report from EDCR service</li>
	 * <li>Fetches the permit certificate from filestore</li>
	 * <li>Merges both PDFs into a single document (permit first, then scrutiny
	 * report)</li>
	 * <li>Uploads the merged PDF to filestore and returns the file reference</li>
	 * <li>Cleans up all temporary files regardless of success or failure</li>
	 * </ol>
	 * 
	 * <p>
	 * This operation is typically performed after permit approval to provide a
	 * comprehensive document that includes both the official permit certificate and
	 * the EDCR scrutiny analysis.
	 * 
	 * @param bpaRequest  The BPA request containing application details and
	 *                    approval information
	 * @param requestInfo The request information for authentication and
	 *                    authorization
	 * @return Filestore reference object for the uploaded merged PDF
	 * @throws CustomException if approval number is missing, file operations fail,
	 *                         or merging fails
	 */
	public Object mergeScrutinyReportToPermit(BPARequest bpaRequest, RequestInfo requestInfo) {
		// Step 1: Validate approval number exists
		validateApprovalNumberForMerge(bpaRequest);

		// Step 2: Generate unique file names for this merge operation
		PdfFileNames fileNames = generatePdfFileNames();

		// Step 3: Initialize file references and PDDocument
		PDDocument document = null;
		File permitCertificateFile = null;
		File shortenedScrutinyReportFile = null;

		try {
			// Step 4: Download scrutiny report and permit certificate
			this.createTempShortenedReport(bpaRequest, fileNames.getScrutinyReportFileName(), document);
			permitCertificateFile = downloadPermitCertificate(bpaRequest, fileNames.getTempFileName());
			shortenedScrutinyReportFile = new File(fileNames.getScrutinyReportFileName());

			// Step 5: Merge permit certificate and scrutiny report PDFs
			mergePdfDocuments(permitCertificateFile, shortenedScrutinyReportFile, fileNames.getMergedFileName());

			// Step 6: Upload merged PDF to filestore and return reference
			return uploadMergedPdfToFilestore(bpaRequest.getBPA(), fileNames.getMergedFileName());

		} catch (Exception ex) {
			log.error("Exception occurred while merging scrutiny report to permit for application: {}",
					bpaRequest.getBPA().getApplicationNo(), ex);
			throw new CustomException(BPAErrorConstants.UNABLE_TO_DOWNLOAD,
					"Unable to merge scrutiny report with permit certificate");
		} finally {
			// Step 7: Clean up all temporary files and resources
			cleanupTemporaryResources(document, permitCertificateFile, shortenedScrutinyReportFile,
					fileNames.getTempFileName(), fileNames.getMergedFileName());
		}
	}

	/**
	 * Validates that the BPA request contains a valid approval number for merge
	 * operation.
	 * 
	 * @param bpaRequest The BPA request to validate
	 * @throws CustomException if approval number is null or empty
	 */
	private void validateApprovalNumberForMerge(BPARequest bpaRequest) {
		BPA bpa = bpaRequest.getBPA();
		if (StringUtils.isEmpty(bpa.getApprovalNo())) {
			throw new CustomException(BPAErrorConstants.INVALID_REQUEST,
					"Approval Number is required to merge scrutiny report with permit.");
		}
	}

	/**
	 * Generates unique file names for PDF merge operation to avoid conflicts in
	 * multi-threaded environment.
	 * 
	 * @return PdfFileNames object containing all generated file names with unique
	 *         UUID
	 */
	private PdfFileNames generatePdfFileNames() {
		String uniqueId = UUID.randomUUID().toString();
		return new PdfFileNames(uniqueId);
	}

	/**
	 * Downloads the permit certificate from filestore using the file store ID from
	 * BPA additional details.
	 * 
	 * @param bpaRequest   The BPA request containing additional details with permit
	 *                     file store ID
	 * @param tempFileName The temporary file name to use for downloading
	 * @return File object pointing to the downloaded permit certificate
	 * @throws Exception if download fails or file store ID is not found
	 */
	@SuppressWarnings("unchecked")
	private File downloadPermitCertificate(BPARequest bpaRequest, String tempFileName) throws Exception {
		BPA bpa = bpaRequest.getBPA();

		// Extract permit file store ID from additional details
		Map<String, String> additionalDetails = (Map<String, String>) bpa.getAdditionalDetails();
		String permitFileStoreId = additionalDetails.get("permitFileStoreId");

		if (StringUtils.isEmpty(permitFileStoreId)) {
			throw new CustomException(BPAErrorConstants.INVALID_REQUEST,
					"Permit file store ID not found in additional details");
		}

		log.info("Fetching permit certificate from filestore for fileStoreId: {}", permitFileStoreId);

		// Download permit certificate from filestore
		File permitFile = fileStoreService.fetch(permitFileStoreId, "BPA", tempFileName, bpa.getTenantId());

		log.debug("Successfully downloaded permit certificate to: {}", tempFileName);
		return permitFile;
	}

	/**
	 * Merges two PDF documents (permit certificate and scrutiny report) into a
	 * single PDF file.
	 * 
	 * <p>
	 * The permit certificate is placed first, followed by the scrutiny report,
	 * providing a complete document with official permit and technical analysis
	 * details.
	 * 
	 * @param permitCertificateFile The permit certificate PDF file (will be merged
	 *                              first)
	 * @param scrutinyReportFile    The scrutiny report PDF file (will be merged
	 *                              second)
	 * @param mergedFileName        The output file name for the merged PDF
	 * @throws Exception if PDF merging fails or files cannot be read
	 */
	private void mergePdfDocuments(File permitCertificateFile, File scrutinyReportFile, String mergedFileName)
			throws Exception {

		log.info("Starting PDF merge operation: permit + scrutiny report");

		// Initialize PDF merger utility
		PDFMergerUtility pdfMerger = new PDFMergerUtility();
		pdfMerger.setDestinationFileName(mergedFileName);

		// Add source PDFs in order: permit certificate first, then scrutiny report
		pdfMerger.addSource(permitCertificateFile);
		pdfMerger.addSource(scrutinyReportFile);

		// Perform the merge operation
		pdfMerger.mergeDocuments(null);

		log.info("Successfully merged PDFs. Output file: {}", mergedFileName);
	}

	/**
	 * Uploads the merged PDF document to filestore and returns the file reference.
	 * 
	 * @param bpa            The BPA object containing tenant information
	 * @param mergedFileName The name of the merged PDF file to upload
	 * @return Filestore reference object containing upload details and file store
	 *         ID
	 * @throws Exception if upload to filestore fails
	 */
	private Object uploadMergedPdfToFilestore(BPA bpa, String mergedFileName) throws Exception {
		log.info("Uploading merged PDF to filestore for tenant: {}", bpa.getTenantId());

		File mergedFile = new File(mergedFileName);
		Object filestoreResponse = fileStoreService.upload(mergedFile, mergedFileName, MediaType.APPLICATION_PDF_VALUE,
				"BPA", bpa.getTenantId());

		log.info("Successfully uploaded merged PDF to filestore");
		return filestoreResponse;
	}

	/**
	 * Cleans up all temporary resources including PDDocument and temporary files.
	 * 
	 * <p>
	 * This method ensures proper cleanup regardless of whether the merge operation
	 * succeeded or failed. It attempts to:
	 * <ul>
	 * <li>Close the PDDocument if open</li>
	 * <li>Delete the permit certificate temporary file</li>
	 * <li>Delete the scrutiny report temporary file</li>
	 * <li>Delete any remaining temp files</li>
	 * <li>Delete the merged PDF file after upload</li>
	 * </ul>
	 * 
	 * @param document       The PDDocument to close (may be null)
	 * @param permitFile     The permit certificate file to delete (may be null)
	 * @param scrutinyFile   The scrutiny report file to delete (may be null)
	 * @param tempFileName   The temporary file name to delete if exists
	 * @param mergedFileName The merged PDF file name to delete if exists
	 */
	private void cleanupTemporaryResources(PDDocument document, File permitFile, File scrutinyFile, String tempFileName,
			String mergedFileName) {
		try {
			// Close PDDocument if open
			if (document != null) {
				document.close();
				log.debug("PDDocument closed successfully");
			}

			// Delete permit certificate file
			if (Objects.nonNull(permitFile) && permitFile.exists()) {
				FileUtils.forceDelete(permitFile);
				log.debug("Deleted permit certificate file: {}", permitFile.getName());
			}

			// Delete scrutiny report file
			if (Objects.nonNull(scrutinyFile) && scrutinyFile.exists()) {
				FileUtils.forceDelete(scrutinyFile);
				log.debug("Deleted scrutiny report file: {}", scrutinyFile.getName());
			}

			// Delete temporary file if exists
			File tempFile = new File(tempFileName);
			if (tempFile.exists()) {
				FileUtils.forceDelete(tempFile);
				log.debug("Deleted temp file: {}", tempFileName);
			}

			// Delete merged file if exists (after upload)
			File mergedFile = new File(mergedFileName);
			if (mergedFile.exists()) {
				FileUtils.forceDelete(mergedFile);
				log.debug("Deleted merged file: {}", mergedFileName);
			}

			log.info("Successfully cleaned up all temporary resources");

		} catch (Exception ex) {
			log.error("Exception occurred while cleaning up temporary files", ex);
			throw new CustomException(BPAErrorConstants.INVALID_FILE, "Unable to clean up temporary files properly");
		}
	}

	/**
	 * Helper class to encapsulate all file names used during PDF merge operation.
	 * 
	 * <p>
	 * Using a dedicated class improves code organization and makes it easier to
	 * pass multiple related file names without excessive method parameters.
	 */
	private static class PdfFileNames {
		private final String scrutinyReportFileName;
		private final String tempFileName;
		private final String mergedFileName;

		public PdfFileNames(String uniqueId) {
			this.scrutinyReportFileName = "shortenedScrutinyReport_" + uniqueId + ".pdf";
			this.tempFileName = "tempFile_" + uniqueId + ".pdf";
			this.mergedFileName = "mergedPdf_" + uniqueId + ".pdf";
		}

		public String getScrutinyReportFileName() {
			return scrutinyReportFileName;
		}

		public String getTempFileName() {
			return tempFileName;
		}

		public String getMergedFileName() {
			return mergedFileName;
		}
	}

	public List<BPA> searchApplications(RequestInfo requestInfo) {
		return repository.getBpaApplication(requestInfo);
	}

	/**
	 * Performs a plain search for BPA applications and enriches them with
	 * associated land information.
	 * 
	 * <p>
	 * This method orchestrates the following workflow:
	 * <ol>
	 * <li>Validates the search criteria and request information</li>
	 * <li>Extracts user roles for potential authorization checks (currently not
	 * used but available)</li>
	 * <li>Searches for BPA applications matching the criteria</li>
	 * <li>Collects all land IDs from the found BPA applications</li>
	 * <li>Fetches land information for all collected land IDs in bulk</li>
	 * <li>Enriches each BPA with its corresponding land information</li>
	 * </ol>
	 * 
	 * <p>
	 * The method uses a "plain search" approach which retrieves basic BPA data
	 * without complex joins or extensive enrichment, making it suitable for listing
	 * and search operations. Land information is populated separately to optimize
	 * database queries.
	 * 
	 * @param criteria    The search criteria containing filters like tenant ID,
	 *                    application number, etc.
	 * @param requestInfo The request information containing user details and
	 *                    authentication context
	 * @return List of BPA applications enriched with land information, or empty
	 *         list if no matches found
	 */
	public List<BPA> plainSearch(BPASearchCriteria criteria, RequestInfo requestInfo) {
		// Step 1: Validate search criteria and request
		bpaValidator.validateSearch(requestInfo, criteria);

		// Step 2: Extract user roles (available for future authorization checks)
		List<String> userRoles = extractUserRoles(requestInfo);

		// Step 3: Search for BPA applications matching the criteria
		List<BPA> bpas = getBPAFromCriteriaForPlainSearch(criteria, requestInfo, null);

		// Step 4: If BPAs found, enrich them with land information
		if (!bpas.isEmpty()) {
			enrichBpasWithLandInfo(bpas, criteria.getTenantId(), requestInfo);
		}

		log.debug("Plain search completed. Found {} BPA application(s)", bpas.size());
		return bpas;
	}

	/**
	 * Enriches BPA applications with their corresponding land information.
	 * 
	 * <p>
	 * This method performs the following operations:
	 * <ol>
	 * <li>Collects all land IDs from the BPA applications</li>
	 * <li>Fetches land information for all land IDs in a single bulk query</li>
	 * <li>Populates each BPA with its corresponding land information</li>
	 * </ol>
	 * 
	 * <p>
	 * Bulk fetching of land information improves performance compared to individual
	 * queries for each BPA application.
	 * 
	 * @param bpas        The list of BPA applications to enrich
	 * @param tenantId    The tenant ID for the land search
	 * @param requestInfo The request information for the land service call
	 */
	private void enrichBpasWithLandInfo(List<BPA> bpas, String tenantId, RequestInfo requestInfo) {
		// Step 1: Collect all land IDs from BPA applications
		List<String> landIds = collectLandIdsFromBpas(bpas);

		if (landIds.isEmpty()) {
			log.debug("No land IDs found in BPA applications to fetch");
			return;
		}

		// Step 2: Build search criteria for bulk land info fetch
		LandSearchCriteria landCriteria = new LandSearchCriteria();
		landCriteria.setIds(landIds);
		landCriteria.setTenantId(tenantId);

		// Step 3: Fetch land information for all land IDs in bulk
		log.debug("Fetching land information for {} land ID(s)", landIds.size());
		ArrayList<LandInfo> landInfos = landService.searchLandInfoToBPAForPlaneSearch(requestInfo, landCriteria);

		// Step 4: Populate each BPA with its corresponding land information
		this.populateLandToBPAForPlainSearch(bpas, landInfos, requestInfo);
	}

	/**
	 * Collects all unique land IDs from the given list of BPA applications.
	 * 
	 * @param bpas The list of BPA applications
	 * @return List of land IDs extracted from BPAs (may contain duplicates if not
	 *         filtered)
	 */
	private List<String> collectLandIdsFromBpas(List<BPA> bpas) {
		List<String> landIds = new ArrayList<>();
		for (BPA bpa : bpas) {
			if (bpa.getLandId() != null) {
				landIds.add(bpa.getLandId());
			}
		}
		return landIds;
	}

	/**
	 * Retrieves BPA applications from the database matching the search criteria for
	 * plain search.
	 * 
	 * <p>
	 * This is a simpler search operation that fetches basic BPA data without
	 * complex joins or enrichment. Used primarily for listing and search operations
	 * where full BPA details are not immediately needed.
	 * 
	 * @param criteria    The search criteria for filtering BPA applications
	 * @param requestInfo The request information for authentication and
	 *                    authorization
	 * @param edcrNos     List of EDCR numbers to filter by (currently null,
	 *                    reserved for future use)
	 * @return List of BPA applications matching the criteria, or empty list if none
	 *         found
	 */
	public List<BPA> getBPAFromCriteriaForPlainSearch(BPASearchCriteria criteria, RequestInfo requestInfo,
			List<String> edcrNos) {
		List<BPA> bpa = repository.getBPADataForPlainSearch(criteria, edcrNos);

		if (bpa.isEmpty()) {
			log.debug("No BPA applications found for plain search criteria");
			return Collections.emptyList();
		}

		log.debug("Found {} BPA application(s) in plain search", bpa.size());
		return bpa;
	}

	/**
	 * Populates land information into BPA applications with fallback mechanism for
	 * missing data.
	 * 
	 * <p>
	 * This method implements a two-phase approach to ensure all BPAs get their land
	 * information:
	 * <ol>
	 * <li><b>Phase 1:</b> Match BPAs with land info from bulk fetch results</li>
	 * <li><b>Phase 2:</b> For any BPA still missing land info, fetch it
	 * individually</li>
	 * </ol>
	 * 
	 * <p>
	 * The fallback mechanism (Phase 2) handles edge cases where:
	 * <ul>
	 * <li>Land info was not returned in the bulk query due to data
	 * inconsistencies</li>
	 * <li>Land records exist in different tenants or have access restrictions</li>
	 * <li>Timing issues where land was recently created but not yet indexed</li>
	 * </ul>
	 * 
	 * <p>
	 * <b>Performance Note:</b> While the fallback makes individual API calls, it
	 * only executes for BPAs that truly have missing data, keeping the performance
	 * impact minimal for the common case where bulk fetch succeeds.
	 * 
	 * @param bpas        The list of BPA applications to populate with land
	 *                    information
	 * @param landInfos   The bulk-fetched land information from the initial query
	 * @param requestInfo The request information for fallback land service calls
	 */
	private void populateLandToBPAForPlainSearch(List<BPA> bpas, List<LandInfo> landInfos, RequestInfo requestInfo) {
		// Phase 1: Match BPAs with land info from bulk fetch
		for (BPA bpa : bpas) {
			matchAndSetLandInfo(bpa, landInfos);
		}

		// Phase 2: Handle BPAs with missing land info using fallback mechanism
		for (BPA bpa : bpas) {
			if (needsLandInfoFallback(bpa)) {
				fetchAndSetMissingLandInfo(bpa, requestInfo);
			}
		}

		log.debug("Successfully populated land information for {} BPA(s)", bpas.size());
	}

	/**
	 * Matches and sets the corresponding land information for a BPA from the
	 * provided list.
	 * 
	 * <p>
	 * This method performs a case-insensitive comparison of land IDs to handle
	 * variations in ID casing that might occur across different systems or data
	 * sources.
	 * 
	 * @param bpa       The BPA application to populate with land info
	 * @param landInfos The list of land information to search through
	 */
	private void matchAndSetLandInfo(BPA bpa, List<LandInfo> landInfos) {
		if (bpa.getLandId() == null) {
			return; // No land ID to match
		}

		for (LandInfo landInfo : landInfos) {
			if (landInfo.getId().equalsIgnoreCase(bpa.getLandId())) {
				bpa.setLandInfo(landInfo);
				log.trace("Matched land info for BPA: {}, Land ID: {}", bpa.getApplicationNo(), bpa.getLandId());
				break; // Found match, no need to continue searching
			}
		}
	}

	/**
	 * Determines if a BPA needs the fallback mechanism to fetch missing land
	 * information.
	 * 
	 * <p>
	 * A BPA needs fallback if:
	 * <ul>
	 * <li>It has a land ID (expects to have land info)</li>
	 * <li>BUT its land info is still null after the bulk match attempt</li>
	 * </ul>
	 * 
	 * @param bpa The BPA to check
	 * @return true if BPA has land ID but no land info, false otherwise
	 */
	private boolean needsLandInfoFallback(BPA bpa) {
		return bpa.getLandId() != null && bpa.getLandInfo() == null;
	}

	/**
	 * Fetches and sets missing land information for a BPA using individual land
	 * service call.
	 * 
	 * <p>
	 * This is the fallback mechanism when bulk fetch didn't return the land info.
	 * It creates a specific search criteria for just this BPA's land ID and makes
	 * an individual call to the land service.
	 * 
	 * <p>
	 * <b>Why Fallback is Needed:</b> The bulk query might miss some land records
	 * due to:
	 * <ul>
	 * <li>Cross-tenant data access restrictions</li>
	 * <li>Recently created land records not yet fully indexed</li>
	 * <li>Data inconsistencies or synchronization delays</li>
	 * </ul>
	 * 
	 * @param bpa         The BPA application missing land information
	 * @param requestInfo The request information for the land service call
	 */
	private void fetchAndSetMissingLandInfo(BPA bpa, RequestInfo requestInfo) {
		// Build search criteria for this specific land ID
		LandSearchCriteria missingLandCriteria = new LandSearchCriteria();
		List<String> missingLandIds = new ArrayList<>();
		missingLandIds.add(bpa.getLandId());
		missingLandCriteria.setTenantId(bpa.getTenantId());
		missingLandCriteria.setIds(missingLandIds);

		log.debug("Fetching missing land info via fallback for BPA: {}, Land ID: {}, Tenant: {}",
				bpa.getApplicationNo(), bpa.getLandId(), bpa.getTenantId());

		// Make individual land service call
		List<LandInfo> fetchedLandInfo = landService.searchLandInfoToBPAForPlaneSearch(requestInfo,
				missingLandCriteria);

		// Match and set the fetched land info
		if (!fetchedLandInfo.isEmpty()) {
			matchAndSetLandInfo(bpa, fetchedLandInfo);

			if (bpa.getLandInfo() != null) {
				log.info("Successfully fetched missing land info via fallback for BPA: {}", bpa.getApplicationNo());
			} else {
				log.warn("Fallback fetch returned land info, but ID mismatch for BPA: {}, Land ID: {}",
						bpa.getApplicationNo(), bpa.getLandId());
			}
		} else {
			log.warn("Fallback fetch found no land info for BPA: {}, Land ID: {}", bpa.getApplicationNo(),
					bpa.getLandId());
		}
	}

	/**
	 * Searches for BPA applications based on the provided criteria with intelligent
	 * routing logic.
	 * 
	 * <p>
	 * This method implements a three-way routing strategy for optimized search
	 * execution:
	 * <ol>
	 * <li><b>Mobile Number Search:</b> When mobile number is provided, searches via
	 * user service first</li>
	 * <li><b>Citizen Minimal Search:</b> For citizens with tenant-only or empty
	 * criteria, returns applications created by them</li>
	 * <li><b>General Criteria Search:</b> For all other cases, performs standard
	 * criteria-based search with land enrichment</li>
	 * </ol>
	 * 
	 * <p>
	 * The routing strategy optimizes performance by:
	 * <ul>
	 * <li>Avoiding unnecessary database queries for citizen-specific searches</li>
	 * <li>Leveraging user service integration for mobile number lookups</li>
	 * <li>Performing bulk land info enrichment only when needed</li>
	 * </ul>
	 * 
	 * <p>
	 * <b>Use Case:</b> This method is primarily used for report generation and
	 * administrative searches where comprehensive BPA data with land information is
	 * required.
	 * 
	 * @param criteria    The search criteria containing filters like mobile number,
	 *                    tenant ID, application number, etc.
	 * @param requestInfo The request information containing user details and
	 *                    authentication context
	 * @return List of BPA applications matching the criteria, enriched with land
	 *         information where applicable
	 */
	public List<BPA> reportSearch(BPASearchCriteria criteria, RequestInfo requestInfo) {
		// Step 1: Validate search criteria and request
		bpaValidator.validateReportSearch(requestInfo, criteria);

		// Step 2: Route search based on criteria type and user role
		List<BPA> bpas = routeSearchByCriteria(criteria, requestInfo);

		log.debug("Report search completed. Found {} BPA application(s)", bpas.size());
		return bpas;
	}

	/**
	 * Routes the search request to the appropriate search method based on criteria
	 * type and user roles.
	 * 
	 * <p>
	 * This method implements the core routing logic:
	 * <ul>
	 * <li><b>Route 1:</b> Mobile number provided → Search via user service</li>
	 * <li><b>Route 2:</b> Citizen with minimal criteria → Return user's own
	 * applications</li>
	 * <li><b>Route 3:</b> General criteria → Standard search with land
	 * enrichment</li>
	 * </ul>
	 * 
	 * @param criteria    The search criteria to evaluate for routing
	 * @param requestInfo The request information containing user context
	 * @return List of BPA applications from the appropriate search route
	 */
	private List<BPA> routeSearchByCriteria(BPASearchCriteria criteria, RequestInfo requestInfo) {
		// Initialize land criteria for potential use in any route
		LandSearchCriteria landCriteria = new LandSearchCriteria();
		landCriteria.setTenantId(criteria.getTenantId());

		// Route 1: Mobile number search takes priority
		if (criteria.getMobileNumber() != null) {
			log.debug("Routing to mobile number search for mobile: {}", criteria.getMobileNumber());
			return this.getBPAFromMobileNumber(criteria, landCriteria, requestInfo);
		}

		// Extract user roles for route determination
		List<String> userRoles = extractUserRoles(requestInfo);

		// Route 2: Citizen with minimal criteria - return only their own applications
		if (isCitizenWithMinimalCriteria(criteria, userRoles)) {
			log.debug("Routing to citizen-specific search (created by me) for user: {}",
					requestInfo.getUserInfo().getUuid());
			List<BPA> bpas = this.getBPACreatedForByMe(criteria, requestInfo, landCriteria, null);
			log.debug("Citizen search returned {} application(s)", bpas.size());
			return bpas;
		}

		// Route 3: General criteria search with land enrichment
		log.debug("Routing to general criteria search with land enrichment");
		return searchWithLandEnrichment(criteria, requestInfo, landCriteria);
	}

	/**
	 * Performs general criteria-based search and enriches results with land
	 * information.
	 * 
	 * <p>
	 * This method handles the standard search flow:
	 * <ol>
	 * <li>Searches BPA applications matching the criteria</li>
	 * <li>Collects all land IDs from found applications</li>
	 * <li>Fetches land information in bulk</li>
	 * <li>Enriches each BPA with corresponding land details</li>
	 * </ol>
	 * 
	 * @param criteria     The search criteria for filtering BPA applications
	 * @param requestInfo  The request information for service calls
	 * @param landCriteria The land search criteria (pre-initialized with tenant ID)
	 * @return List of BPA applications enriched with land information
	 */
	private List<BPA> searchWithLandEnrichment(BPASearchCriteria criteria, RequestInfo requestInfo,
			LandSearchCriteria landCriteria) {
		// Step 1: Search for BPA applications matching criteria
		List<BPA> bpas = getBPAFromCriteria(criteria, requestInfo, null);

		// Step 2: If BPAs found, enrich with land information
		if (!bpas.isEmpty()) {
			enrichBpasWithLandInfoForReport(bpas, landCriteria, requestInfo);
		}

		return bpas;
	}

	/**
	 * Enriches BPA applications with land information for report search.
	 * 
	 * <p>
	 * This method performs bulk land information fetch and population:
	 * <ol>
	 * <li>Collects all land IDs from BPA applications</li>
	 * <li>Updates land criteria with collected IDs and tenant from first BPA</li>
	 * <li>Fetches all land information in a single bulk query</li>
	 * <li>Populates each BPA with its corresponding land information</li>
	 * </ol>
	 * 
	 * <p>
	 * <b>Note:</b> Uses tenant ID from first BPA to ensure correct tenant context
	 * for land fetch.
	 * 
	 * @param bpas         The list of BPA applications to enrich
	 * @param landCriteria The land search criteria to populate
	 * @param requestInfo  The request information for land service call
	 */
	private void enrichBpasWithLandInfoForReport(List<BPA> bpas, LandSearchCriteria landCriteria,
			RequestInfo requestInfo) {
		// Step 1: Collect all land IDs from BPA applications
		List<String> landIds = new ArrayList<>();
		for (BPA bpa : bpas) {
			if (bpa.getLandId() != null) {
				landIds.add(bpa.getLandId());
			}
		}

		if (landIds.isEmpty()) {
			log.debug("No land IDs found in BPA applications to enrich");
			return;
		}

		// Step 2: Configure land criteria for bulk fetch
		landCriteria.setIds(landIds);
		// Use tenant ID from first BPA to ensure correct tenant context
		landCriteria.setTenantId(bpas.get(0).getTenantId());

		log.debug("Fetching land information for {} land ID(s) with tenant: {}", landIds.size(),
				landCriteria.getTenantId());

		// Step 3: Fetch land information in bulk
		ArrayList<LandInfo> landInfos = landService.searchLandInfoToBPA(requestInfo, landCriteria);

		// Step 4: Populate BPAs with land information (includes fallback mechanism)
		this.populateLandToBPA(bpas, landInfos, requestInfo);

		log.debug("Successfully enriched {} BPA(s) with land information", bpas.size());
	}

	/**
	 * Searches for BPA applications with lightweight response objects for
	 * application listing and search operations.
	 * 
	 * <p>
	 * This method is similar to
	 * {@link #reportSearch(BPASearchCriteria, RequestInfo)} but returns
	 * {@link BpaApplicationSearch} objects which contain a subset of BPA fields
	 * optimized for:
	 * <ul>
	 * <li>Application listing pages with faster response times</li>
	 * <li>Search results where full BPA details are not needed</li>
	 * <li>API responses that need to minimize payload size</li>
	 * </ul>
	 * 
	 * <p>
	 * Implements the same three-way routing strategy:
	 * <ol>
	 * <li><b>Mobile Number Search:</b> When mobile number is provided, searches via
	 * user service first</li>
	 * <li><b>Citizen Minimal Search:</b> For citizens with tenant-only or empty
	 * criteria, returns applications created by them</li>
	 * <li><b>General Criteria Search:</b> For all other cases, performs standard
	 * criteria-based search with land enrichment</li>
	 * </ol>
	 * 
	 * @param criteria    The search criteria containing filters like mobile number,
	 *                    tenant ID, application number, etc.
	 * @param requestInfo The request information containing user details and
	 *                    authentication context
	 * @return List of BpaApplicationSearch objects matching the criteria, enriched
	 *         with land information where applicable
	 */
	public List<BpaApplicationSearch> searchBPAApplication(@Valid BPASearchCriteria criteria, RequestInfo requestInfo) {
		// Step 1: Validate search criteria and request
		bpaValidator.validateSearch(requestInfo, criteria);

		// Step 2: Route search based on criteria type and user role
		List<BpaApplicationSearch> bpas = routeApplicationSearchByCriteria(criteria, requestInfo);

		log.debug("BPA application search completed. Found {} application(s)", bpas.size());
		return bpas;
	}

	/**
	 * Routes the application search request to the appropriate search method based
	 * on criteria type and user roles.
	 * 
	 * <p>
	 * This method implements the core routing logic for BpaApplicationSearch
	 * results:
	 * <ul>
	 * <li><b>Route 1:</b> Mobile number provided → Search via user service</li>
	 * <li><b>Route 2:</b> Citizen with minimal criteria → Return user's own
	 * applications</li>
	 * <li><b>Route 3:</b> General criteria → Standard search with land
	 * enrichment</li>
	 * </ul>
	 * 
	 * @param criteria    The search criteria to evaluate for routing
	 * @param requestInfo The request information containing user context
	 * @return List of BpaApplicationSearch objects from the appropriate search
	 *         route
	 */
	private List<BpaApplicationSearch> routeApplicationSearchByCriteria(BPASearchCriteria criteria,
			RequestInfo requestInfo) {
		// Initialize land criteria for potential use in any route
		LandSearchCriteria landCriteria = new LandSearchCriteria();
		landCriteria.setTenantId(criteria.getTenantId());

		// Route 1: Mobile number search takes priority
		if (criteria.getMobileNumber() != null) {
			log.debug("Routing to mobile number search for mobile: {}", criteria.getMobileNumber());
			return this.getBPASearchApplicationFromMobileNumber(criteria, landCriteria, requestInfo);
		}

		// Extract user roles for route determination
		List<String> userRoles = extractUserRoles(requestInfo);

		// Route 2: Citizen with minimal criteria - return only their own applications
		if (isCitizenWithMinimalCriteria(criteria, userRoles)) {
			log.info("Routing to citizen-specific application search (created by me) for user: {}",
					requestInfo.getUserInfo().getUuid());
			List<BpaApplicationSearch> bpas = this.getBPASearchApplicationCreatedForByMe(criteria, requestInfo,
					landCriteria, null);
			log.info("Citizen application search returned {} result(s)", bpas.size());
			return bpas;
		}

		// Route 3: General criteria search with land enrichment
		log.debug("Routing to general application criteria search with land enrichment");
		return searchApplicationsWithLandEnrichment(criteria, requestInfo, landCriteria);
	}

	/**
	 * Performs general criteria-based application search and enriches results with
	 * land information.
	 * 
	 * <p>
	 * This method handles the standard search flow for BpaApplicationSearch
	 * objects:
	 * <ol>
	 * <li>Searches BPA applications matching the criteria</li>
	 * <li>Collects all land IDs from found applications</li>
	 * <li>Fetches land information in bulk</li>
	 * <li>Enriches each application with corresponding land details</li>
	 * </ol>
	 * 
	 * @param criteria     The search criteria for filtering BPA applications
	 * @param requestInfo  The request information for service calls
	 * @param landCriteria The land search criteria (pre-initialized with tenant ID)
	 * @return List of BpaApplicationSearch objects enriched with land information
	 */
	private List<BpaApplicationSearch> searchApplicationsWithLandEnrichment(BPASearchCriteria criteria,
			RequestInfo requestInfo, LandSearchCriteria landCriteria) {
		// Step 1: Search for BPA applications matching criteria
		List<BpaApplicationSearch> bpas = getBPASearchApplicationFromCriteria(criteria, requestInfo, null);

		// Step 2: If applications found, enrich with land information
		if (!bpas.isEmpty()) {
			enrichApplicationsWithLandInfo(bpas, landCriteria, requestInfo);
		}

		return bpas;
	}

	/**
	 * Enriches BpaApplicationSearch objects with land information.
	 * 
	 * <p>
	 * This method performs bulk land information fetch and population:
	 * <ol>
	 * <li>Collects all land IDs from BPA application search results</li>
	 * <li>Updates land criteria with collected IDs and tenant from first
	 * application</li>
	 * <li>Fetches all land information in a single bulk query</li>
	 * <li>Populates each application with its corresponding land information</li>
	 * </ol>
	 * 
	 * <p>
	 * <b>Note:</b> Uses tenant ID from first application to ensure correct tenant
	 * context for land fetch.
	 * 
	 * @param bpas         The list of BPA application search results to enrich
	 * @param landCriteria The land search criteria to populate
	 * @param requestInfo  The request information for land service call
	 */
	private void enrichApplicationsWithLandInfo(List<BpaApplicationSearch> bpas, LandSearchCriteria landCriteria,
			RequestInfo requestInfo) {
		// Step 1: Collect all land IDs from BPA applications
		List<String> landIds = new ArrayList<>();
		for (BpaApplicationSearch bpa : bpas) {
			if (bpa.getLandId() != null) {
				landIds.add(bpa.getLandId());
			}
		}

		if (landIds.isEmpty()) {
			log.debug("No land IDs found in application search results to enrich");
			return;
		}

		// Step 2: Configure land criteria for bulk fetch
		landCriteria.setIds(landIds);
		// Use tenant ID from first application to ensure correct tenant context
		landCriteria.setTenantId(bpas.get(0).getTenantId());

		log.debug("Fetching land information for {} land ID(s) with tenant: {}", landIds.size(),
				landCriteria.getTenantId());

		// Step 3: Fetch land information in bulk
		ArrayList<LandInfo> landInfos = landService.searchLandInfoToBPA(requestInfo, landCriteria);

		// Step 4: Populate applications with land information
		this.populateLandToBPAApplication(bpas, landInfos, requestInfo);

		log.debug("Successfully enriched {} BPA application(s) with land information", bpas.size());
	}

	/**
	 * Retrieves BPA applications from the database matching the search criteria.
	 * 
	 * <p>
	 * Returns {@link BpaApplicationSearch} objects which contain a lightweight
	 * subset of BPA fields, optimized for listing and search operations where full
	 * BPA details are not needed.
	 * 
	 * @param criteria    The search criteria for filtering BPA applications
	 * @param requestInfo The request information for authentication and
	 *                    authorization
	 * @param edcrNos     List of EDCR numbers to filter by (currently null,
	 *                    reserved for future use)
	 * @return List of BpaApplicationSearch objects matching the criteria, or empty
	 *         list if none found
	 */
	private List<BpaApplicationSearch> getBPASearchApplicationFromCriteria(@Valid BPASearchCriteria criteria,
			RequestInfo requestInfo, List<String> edcrNos) {

		List<BpaApplicationSearch> bpa = repository.getBPAApplicationData(criteria, edcrNos);

		if (bpa.isEmpty()) {
			log.debug("No BPA applications found for search criteria");
			return Collections.emptyList();
		}

		log.debug("Found {} BPA application(s) in search", bpa.size());
		return bpa;
	}

	private List<BpaApplicationSearch> getBPASearchApplicationCreatedForByMe(@Valid BPASearchCriteria criteria,
			RequestInfo requestInfo, LandSearchCriteria landcriteria, List<String> edcrNos) {
		List<BpaApplicationSearch> bpas = null;
		UserSearchRequest userSearchRequest = new UserSearchRequest();
		if (criteria.getTenantId() != null) {
			userSearchRequest.setTenantId(criteria.getTenantId());
		}
		List<String> uuids = new ArrayList<String>();
		if (requestInfo.getUserInfo() != null && !StringUtils.isEmpty(requestInfo.getUserInfo().getUuid())) {
			uuids.add(requestInfo.getUserInfo().getUuid());
			criteria.setOwnerIds(uuids);
			criteria.setCreatedBy(uuids);
		}
		log.debug("loading data of created and by me" + uuids.toString());
		UserDetailResponse userInfo = userService.getUser(criteria, requestInfo);
		if (userInfo != null) {
			landcriteria.setMobileNumber(userInfo.getUser().get(0).getMobileNumber());
		}
		log.debug("Call with multiple to Land::" + landcriteria.getTenantId() + landcriteria.getMobileNumber());
		ArrayList<LandInfo> landInfos = landService.searchLandInfoToBPA(requestInfo, landcriteria);
		ArrayList<String> landIds = new ArrayList<String>();
		if (landInfos.size() > 0) {
			landInfos.forEach(land -> {
				landIds.add(land.getId());
			});
			criteria.setLandId(landIds);
		}
		bpas = getBPAApplicationFromCriteria(criteria, requestInfo, edcrNos);
		log.debug("no of bpas queried" + bpas.size());
		this.populateLandToBPAApplication(bpas, landInfos, requestInfo);
		return bpas;
	}

	private void populateLandToBPAApplication(List<BpaApplicationSearch> bpas, ArrayList<LandInfo> landInfos,
			RequestInfo requestInfo) {
		for (int i = 0; i < bpas.size(); i++) {
			for (int j = 0; j < landInfos.size(); j++) {
				if (landInfos.get(j).getId().equalsIgnoreCase(bpas.get(i).getLandId())) {
					bpas.get(i).setLandInfo(landInfos.get(j));
				}
			}
			if (bpas.get(i).getLandId() != null && bpas.get(i).getLandInfo() == null) {
				LandSearchCriteria missingLandcriteria = new LandSearchCriteria();
				List<String> missingLandIds = new ArrayList<String>();
				missingLandIds.add(bpas.get(i).getLandId());
				missingLandcriteria.setTenantId(bpas.get(0).getTenantId());
				missingLandcriteria.setIds(missingLandIds);
				log.debug("Call with land ids to Land::" + missingLandcriteria.getTenantId()
						+ missingLandcriteria.getIds());
				List<LandInfo> newLandInfo = landService.searchLandInfoToBPA(requestInfo, missingLandcriteria);

				for (int j = 0; j < newLandInfo.size(); j++) {
					if (newLandInfo.get(j).getId().equalsIgnoreCase(bpas.get(i).getLandId())) {
						bpas.get(i).setLandInfo(newLandInfo.get(j));
					}
				}
			}
		}
	}

	private List<BpaApplicationSearch> getBPAApplicationFromCriteria(@Valid BPASearchCriteria criteria,
			RequestInfo requestInfo, List<String> edcrNos) {
		List<BpaApplicationSearch> bpa = repository.getBPAApplicationData(criteria, edcrNos);

		if (bpa.isEmpty())
			return Collections.emptyList();
		return bpa;
	}

	private List<BpaApplicationSearch> getBPASearchApplicationFromMobileNumber(@Valid BPASearchCriteria criteria,
			LandSearchCriteria landcriteria, RequestInfo requestInfo) {

		List<BpaApplicationSearch> bpas = null;
		log.debug("Call with mobile number to Land::" + criteria.getMobileNumber());
		landcriteria.setMobileNumber(criteria.getMobileNumber());
		ArrayList<LandInfo> landInfo = landService.searchLandInfoToBPA(requestInfo, landcriteria);
		ArrayList<String> landId = new ArrayList<String>();
		if (landInfo.size() > 0) {
			landInfo.forEach(land -> {
				landId.add(land.getId());
			});
			criteria.setLandId(landId);
		}
		bpas = getBPAApplicationFromLandId(criteria, requestInfo, null);
		if (landInfo.size() > 0) {
			for (int i = 0; i < bpas.size(); i++) {
				for (int j = 0; j < landInfo.size(); j++) {
					if (landInfo.get(j).getId().equalsIgnoreCase(bpas.get(i).getLandId())) {
						bpas.get(i).setLandInfo(landInfo.get(j));
					}
				}
			}
		}
		return bpas;
	}

	private List<BpaApplicationSearch> getBPAApplicationFromLandId(@Valid BPASearchCriteria criteria,
			RequestInfo requestInfo, List<String> edcrNos) {
		List<BpaApplicationSearch> bpa = new LinkedList<>();
		bpa = repository.getBPAApplicationData(criteria, edcrNos);
		if (bpa.size() == 0) {
			return Collections.emptyList();
		}
		return bpa;

	}

	public List<BpaApprovedByApplicationSearch> searchApplicationApprovedBy(@Valid BPASearchCriteria criteria,
			String uuid) {

		// no need to validate bussiness service as now it is being used by all the
		// bussinness service approver including bpa5

//			if(criteria.getBusinessService()==null) {
//				errorMap.put("SearchError","please provide bussiness service to search approved application.");
//			}

		List<BpaApprovedByApplicationSearch> bpaApprovedByApplicationSearch = repository.getApprovedbyData(uuid,
				criteria);

		if (bpaApprovedByApplicationSearch.isEmpty()) {
			return Collections.emptyList();
		} else {
			this.populatedocumentdetailstoSearch(bpaApprovedByApplicationSearch);
			return bpaApprovedByApplicationSearch;
		}
	}

	private void populatedocumentdetailstoSearch(List<BpaApprovedByApplicationSearch> bpaApprovedByApplicationSearch) {
		List<String> bpids = bpaApprovedByApplicationSearch.stream().filter(bpa -> bpa.getBpaid() != null)
				.map(BpaApprovedByApplicationSearch::getBpaid).collect(Collectors.toList());
		List<DocumentList> documentList = repository.getdocumentDataForApproveBy(bpids);
		// List<String>

		// System.out.println("size1:"+(documentList.stream().map(DocumentList::getBuildingPlanid).collect(Collectors.toList())).size());
		for (BpaApprovedByApplicationSearch bpas : bpaApprovedByApplicationSearch) {
			List<DocumentList> docList = documentList.stream()
					.filter(doc -> doc.getBuildingPlanid().equals(bpas.getBpaid())).collect(Collectors.toList());
			bpas.setDocuments(docList);
		}

	}

	/**
	 * Creates a new field inspection report for a BPA application.
	 * 
	 * <p>
	 * This method orchestrates the field inspection report creation workflow:
	 * <ol>
	 * <li>Validates that application number is provided and not empty</li>
	 * <li>Validates field inspection request data (business rules validation)</li>
	 * <li>Retrieves the corresponding BPA application from the database</li>
	 * <li>Validates that exactly one BPA application exists for the given
	 * application number</li>
	 * <li>Enriches the field inspection report with auto-generated fields (ID,
	 * audit details, etc.)</li>
	 * <li>Persists the field inspection report to the database</li>
	 * </ol>
	 * 
	 * <p>
	 * <b>Use Case:</b> This method is used when inspectors visit a construction
	 * site and need to record their observations, compliance status, and
	 * recommendations. The field inspection report becomes part of the BPA
	 * application's lifecycle and can influence approval decisions.
	 * 
	 * @param request The field inspection request containing inspection details and
	 *                metadata
	 * @return The created FieldInspection object with enriched fields (ID,
	 *         timestamps, etc.)
	 * @throws CustomException if application number is missing, field inspection
	 *                         validation fails, or BPA application is not found or
	 *                         multiple applications found
	 */
	public FieldInspection createFieldInspectionReport(@Valid FieldInspectionRequest request) {
		FieldInspection fieldInspection = request.getFieldinspection();

		// Step 1: Validate application number is provided
		validateApplicationNumber(fieldInspection);

		// Step 2: Validate field inspection request data
		bpaValidator.validatefieldInspectionRequest(fieldInspection);

		// Step 3: Retrieve and validate the associated BPA application
		retrieveAndValidateBpaApplication(fieldInspection);

		// Step 4: Enrich field inspection with auto-generated fields
		enrichmentService.enrichcreatefieldinspectcreate(request);

		// Step 5: Persist the field inspection report to database
		repository.savefieldInspectionReport(request);

		log.info("Successfully created field inspection report for application: {}",
				fieldInspection.getApplicationno());

		return request.getFieldinspection();
	}

	/**
	 * Validates that the field inspection contains a valid application number.
	 * 
	 * <p>
	 * The application number is mandatory as it links the field inspection report
	 * to a specific BPA application. Without it, the inspection cannot be
	 * associated with any building permit.
	 * 
	 * @param fieldInspection The field inspection to validate
	 * @throws CustomException if application number is null or empty
	 */
	private void validateApplicationNumber(FieldInspection fieldInspection) {
		String applicationNo = fieldInspection.getApplicationno();

		if (Objects.isNull(applicationNo) || applicationNo.isEmpty()) {
			throw new CustomException("CREATE_ERROR",
					"Failed to create field inspection report: Application number is mandatory");
		}

		log.debug("Application number validated: {}", applicationNo);
	}

	/**
	 * Retrieves the BPA application for the given field inspection and validates
	 * existence.
	 * 
	 * <p>
	 * This method performs the following validations:
	 * <ul>
	 * <li>Searches for BPA application by application number and tenant ID</li>
	 * <li>Validates that exactly one BPA application is found</li>
	 * <li>Throws exception if no BPA or multiple BPAs are found (data integrity
	 * issue)</li>
	 * </ul>
	 * 
	 * <p>
	 * <b>Why Single BPA Check?</b> Each application number should be unique per
	 * tenant. Finding zero or multiple applications indicates a data integrity
	 * issue that must be resolved before creating the field inspection.
	 * 
	 * @param fieldInspection The field inspection containing application number and
	 *                        tenant ID
	 * @throws CustomException if no BPA application found or multiple applications
	 *                         found
	 */
	private void retrieveAndValidateBpaApplication(FieldInspection fieldInspection) {
		String applicationNo = fieldInspection.getApplicationno();
		String tenantId = fieldInspection.getTenantId();

		// Build search criteria for BPA lookup
		BPASearchCriteria bpaCriteria = new BPASearchCriteria();
		bpaCriteria.setApplicationNo(applicationNo);
		bpaCriteria.setTenantId(tenantId);

		// Retrieve BPA applications from database
		List<String> edcrNos = new ArrayList<>();
		List<BPA> bpaList = repository.getBPAData(bpaCriteria, edcrNos);

		log.debug("BPA search for application '{}' returned {} result(s)", applicationNo, bpaList.size());

		// Validate exactly one BPA application exists
		if (CollectionUtils.isEmpty(bpaList)) {
			throw new CustomException("CREATE_ERROR",
					"Failed to create field inspection report: No BPA application found for application number '"
							+ applicationNo + "'");
		}

		if (bpaList.size() > 1) {
			throw new CustomException("CREATE_ERROR",
					"Failed to create field inspection report: Multiple BPA applications found for application number '"
							+ applicationNo + "'. Expected exactly one application.");
		}

		log.info("BPA application found and validated for field inspection: {}", applicationNo);
	}

	public List<FieldInspection> searchFieldInspectionReport(@Valid FieldInspectionSearchCriteria criteria,
			RequestInfo requestInfo) {

		List<FieldInspection> result = repository.getfieldInspectionReport(criteria);
		return result;
	}

	public List<PlanningAssistantChecklist> searchPlanningAssistanctChecklist(
			@Valid PlanningAssistantSearchCriteria criteria, RequestInfo requestInfo) {

		List<PlanningAssistantChecklist> result = repository.getPlanningAssistanctChecklist(criteria);
		return result;
	}

	private List<BPA> getBPAFromName(BPASearchCriteria criteria, LandSearchCriteria landcriteria,
			RequestInfo requestInfo) {
		List<BPA> bpas = null;
		log.debug("Call with Name to Land::" + criteria.getName());
		landcriteria.setName(criteria.getName());
		ArrayList<LandInfo> landInfo = landService.searchLandInfoToBPA(requestInfo, landcriteria);
		ArrayList<String> landId = new ArrayList<String>();
		if (landInfo.size() > 0) {
			landInfo.forEach(land -> {
				landId.add(land.getId());
			});
			criteria.setLandId(landId);
		}
		if (CollectionUtils.isEmpty(landInfo)) {
			return Collections.emptyList();
		}
		bpas = getBPAFromLandId(criteria, requestInfo, null);
		if (landInfo.size() > 0) {
			for (int i = 0; i < bpas.size(); i++) {
				for (int j = 0; j < landInfo.size(); j++) {
					if (landInfo.get(j).getId().equalsIgnoreCase(bpas.get(i).getLandId())) {
						bpas.get(i).setLandInfo(landInfo.get(j));
					}
				}
			}
		}
		return bpas;
	}

	public BPA permitLetterUpdate(@Valid BPARequest bpaRequest) {

		List<BPA> searchResult = getBPAWithBPAId(bpaRequest);
		if (CollectionUtils.isEmpty(searchResult)) {
			throw new CustomException("Update error",
					"Failed to update BPA , No application exists with mentioned BPA ID");
		}

		repository.updatePermitLetterPreview(bpaRequest);

		return bpaRequest.getBPA();
	}

	private boolean isRequestForBPADeletion(BPARequest bpaRequest) {
		BPA bpa = bpaRequest.getBPA();
		if (bpa == null || bpa.getWorkflow() == null || bpa.getWorkflow().getAction() == null) {
			log.info("Skipping BPA processing: BPA, Workflow, or Action is null.");
			return false;
		}
		String action = bpa.getWorkflow().getAction();
		if (action.equalsIgnoreCase(BPAConstants.ACTION_BPA_DELETE)) {
			bpaValidator.validateBPADeletion(bpa);
			return true;
		}
		return false;
	}

	private BPA processBPADeletion(BPARequest bpaRequest) {
		BPA bpa = bpaRequest.getBPA();
		List<BPA> searchResult = getBPAWithBPAId(bpaRequest);
		if (CollectionUtils.isEmpty(searchResult)) {
			throw new CustomException("Deletion Error",
					"Failed to Delete BPA , No application exists with mentioned BPA ID");
		}
		bpa.setStatus(BPAConstants.BPA_DELETED);
		repository.update(bpaRequest, false);
		return bpaRequest.getBPA();
	}

	private List<BPA> getBPAAutoEscalatedToMe(BPASearchCriteria criteria, RequestInfo requestInfo) {

		String uuid = requestInfo.getUserInfo().getUuid();
		List<BPA> bpa = new LinkedList<>();
		bpa = repository.getBPAAutoEscalatedToME(criteria, uuid);
		if (bpa.size() == 0) {
			return Collections.emptyList();
		}
		return bpa;
	}

	private List<BPA> getBPAAutoEscalated(BPASearchCriteria criteria, RequestInfo requestInfo) {

		String uuid = requestInfo.getUserInfo().getUuid();
		List<BPA> bpa = new LinkedList<>();
		bpa = repository.getBPAAutoEscalated(criteria, uuid);
		if (bpa.size() == 0) {
			return Collections.emptyList();
		}
		return bpa;
	}

	private List<BPA> getBPAAboutToAutoEscalate(BPASearchCriteria criteria, RequestInfo requestInfo) {

		String uuid = requestInfo.getUserInfo().getUuid();
		List<BPA> bpa = new LinkedList<>();
		bpa = repository.getBPAABoutToEscalate(criteria, uuid);
		if (bpa.size() == 0) {
			return Collections.emptyList();
		}
		return bpa;
	}

	private BPARequest createRevalidation(BPARequest bpaRequest) {

		BPA bpa = bpaRequest.getBPA();
		RequestInfo requestInfo = bpaRequest.getRequestInfo();
		Revalidation revalidation = bpaRequest.getRevalidation();
		RevalidationRequest revalidationRequest = new RevalidationRequest();
		Long approvalDate = revalidation.getPermitDate();
		;
		log.info("Provided ApprovalDate :: " + approvalDate);
		if (Objects.nonNull(revalidation)) {
			// Setting Audit Details and Id to BPA
			enrichmentService.enrichBPACreateFromRevalidation(bpaRequest);
			revalidation.setBpaApplicationId(bpa.getId());
			checkSujogApplicationSource(requestInfo, revalidation, bpa);
			if (!revalidation.isSujogExistingApplication()) {
				if (approvalDate == null) {
					throw new CustomException("CREATE ERROR",
							"Permit date is mandatory for outside SUJOG permit letter revalidation.");
				}
				revalidationService.prepareDataForNonSujog(bpa);
				landService.addLandInfoToBPA(bpaRequest);

			} else {
				// setting reference bpa
				BPASearchCriteria c = new BPASearchCriteria();
				c.setTenantId(revalidation.getTenantId());
				c.setApprovalNo(revalidation.getPermitNo());

				List<BPA> refBpas = this.getBPAFromCriteria(c, requestInfo, null);

				if (refBpas.isEmpty()) {
					throw new CustomException("Create Error",
							"No Application found for given permitNo and tenantId. Please enter correct details.");
				}
				
				BPA refBpa = refBpas.get(0);
				revalidation.setRefApplicationDetails(refBpa);
				revalidation.setRefBpaApplicationNo(refBpa.getApplicationNo());
				  
				    // setting riskType from edcr
					log.info("fetching riskType from edcr");
					LinkedHashMap edcrDetails = edcrService.getEDCRDetails(bpaRequest);
					String jsonString = new JSONObject(edcrDetails).toString();
					DocumentContext context = JsonPath.using(Configuration.defaultConfiguration()).parse(jsonString);
					List<String> edcrData = context.read("edcrDetail.*.planDetail.planInformation.riskType");
					String riskType = edcrData.get(0);
					System.out.println("riskType===== " + riskType);
					bpa.setRiskType(riskType);
				
				log.info("approval date:" + approvalDate + " ;current date:" + System.currentTimeMillis());
				System.out.println("approval date:" + approvalDate + " ;current date:" + System.currentTimeMillis());
			}
			boolean isPermitDateThreeYearsOld = revalidationService.isThreeYearsOld(approvalDate);
			if (isPermitDateThreeYearsOld) {
				throw new CustomException("Create Error",
						"Approval date is older than 3 years. Permit already expired. Please apply for fresh application.");
			}

			revalidationRequest.setRequestInfo(requestInfo);
			revalidationRequest.setRevalidation(revalidation);

			revalidation = revalidationService.create(revalidationRequest);
			bpa.setApplicationNo(revalidation.getBpaApplicationNo());
			bpa.setApprovalExpiryDate(revalidation.getPermitExpiryDate());
			revalidation.setBpaApplicationId(bpa.getId());
			bpaRequest.getBPA().setApplicationDate(System.currentTimeMillis());
			bpaRequest.setBPA(bpa);
			bpaRequest.setRevalidation(revalidation);
		}

		Map<String, String> additionalDetails = bpa.getAdditionalDetails() != null ? (Map) bpa.getAdditionalDetails()
				: new HashMap<String, String>();

		if (Objects.isNull(additionalDetails.get("riskType"))) {
			additionalDetails.put("riskType", bpa.getRiskType());
		}

		wfIntegrator.callWorkFlow(bpaRequest);
		revalidationService.revalidationFee(bpaRequest, BPAConstants.APPLICATION_FEE_KEY);
		revalidationRepository.save(revalidationRequest);
		repository.save(bpaRequest);
		return bpaRequest;

	}

	private void checkSujogApplicationSource(RequestInfo requestInfo, Revalidation revalidation, BPA bpa) {
		String permitNo = revalidation.getPermitNo();
		String tenantId = revalidation.getTenantId();

		BPASearchCriteria criteria = BPASearchCriteria.builder().approvalNo(permitNo).tenantId(tenantId)
				.isRevalidationApplication(Boolean.FALSE).build();

		List<BPA> bpas = search(criteria, requestInfo);

		if (CollectionUtils.isEmpty(bpas)) {
			revalidation.setSujogExistingApplication(false);
			BPASearchCriteria criteriaForFirstRevalidation = BPASearchCriteria.builder().approvalNo(permitNo)
					.tenantId(tenantId).isRevalidationApplication(Boolean.TRUE).build();

			List<BPA> revalidationBPAs = search(criteriaForFirstRevalidation, requestInfo);

			if (!CollectionUtils.isEmpty(revalidationBPAs)) {
			    Map<String, Object> mergedAdditionalDetails = new HashMap<>();

			    if (bpa.getAdditionalDetails() instanceof Map) {
			        mergedAdditionalDetails.putAll(
			                (Map<String, Object>) bpa.getAdditionalDetails());
			    }

			    if (revalidationBPAs.get(0).getAdditionalDetails() instanceof Map) {

			        Map<String, Object> revalidationAdditionalDetails =
			                (Map<String, Object>) revalidationBPAs.get(0).getAdditionalDetails();

			        revalidationAdditionalDetails.forEach(
			                (key, value) -> mergedAdditionalDetails.putIfAbsent(key, value));
			    }

			    bpa.setAdditionalDetails(mergedAdditionalDetails);
				bpa.setLandInfo(revalidationBPAs.get(0).getLandInfo());
			}
		}

	}

	private BPA processRevalidation(BPARequest bpaRequest) {

		log.info("Entering processRevalidation method with BPARequest: {}", bpaRequest);

		if (isRequestForBuildingPlanLayoutSignature(bpaRequest)) {
			log.info("Request is for Building Plan Layout Signature.");
			return processRevalidationBuildingPlanLayoutSignature(bpaRequest);
		}

		if (bpaRequest.getBPA().getApplicationNo() == null || bpaRequest.getBPA().getId() == null) {
			log.error("Invalid input parameters, applicationNo and Id cannot be null.");
			throw new IllegalArgumentException("Invalid input parameters, applicationNo and Id cannot be null.");
		}

		log.info("Fetching BPA details for applicationNo: {}, Id: {}", bpaRequest.getBPA().getApplicationNo(),
				bpaRequest.getBPA().getId());
		List<BPA> searchResult = getBPAWithBPAId(bpaRequest);
		log.info("Search result size: {}", searchResult.size());

		if (CollectionUtils.isEmpty(searchResult) || searchResult.size() > 1) {
			log.error("Failed to Update the Application, Found None or multiple applications!");
			throw new CustomException(BPAErrorConstants.UPDATE_ERROR,
					"Failed to Update the Application, Found None or multiple applications!");
		}

		Revalidation revalidation = bpaRequest.getRevalidation();

		if (bpaRequest.getBPA().isRevalidationApplication()) {
			log.info("BPA is a revalidation application.");
			if (Objects.isNull(revalidation)) {
				log.info("Revalidation data is null, fetching from RevalidationService.");
				RevalidationSearchCriteria revCriteria = RevalidationSearchCriteria.builder()
						.bpaApplicationNo(bpaRequest.getBPA().getApplicationNo()).build();
				List<Revalidation> revalidationFromCriteria = revalidationService
						.getRevalidationFromCriteria(revCriteria);
				if (!ObjectUtils.isEmpty(revalidationFromCriteria)) {
					revalidation = revalidationFromCriteria.get(0);
				}
				bpaRequest.setRevalidation(revalidation);
				log.info("Fetched revalidation: {}", revalidation);
			}
		}

		// validate refusal SCN while approve
		bpaValidator.validateRefusalSCNonApprove(bpaRequest);
		bpaValidator.validatePullBackAction(bpaRequest);
		bpaValidator.validateRevalidationDocRemark(bpaRequest);

		BPA bpa = bpaRequest.getBPA();

		Map<String, String> additionalDetails = bpa.getAdditionalDetails() != null ? (Map) bpa.getAdditionalDetails()
				: new HashMap<String, String>();

		if (additionalDetails.get(BPAConstants.PREVIEW_PERMIT_ADDITIONAL_DETAILS) != null)
			additionalDetails.remove(BPAConstants.PREVIEW_PERMIT_ADDITIONAL_DETAILS);

		if (Objects.nonNull(revalidation)) {
			try {
				log.info("Revalidation data before update: {}", objectMapper.writeValueAsString(revalidation));
			} catch (JsonProcessingException e) {
				log.error("JsonProcessingException while logging revalidation data", e);
			}
			RevalidationRequest revalidationRequest = new RevalidationRequest();
			revalidationRequest.setRequestInfo(bpaRequest.getRequestInfo());
			revalidationRequest.setRevalidation(revalidation);
			revalidation = revalidationService.update(revalidationRequest);
			bpaRequest.setRevalidation(revalidation);
			log.info("Revalidation data after update: {}", revalidation);

			enrichmentService.enrichBPAUpdateFromRevalidation(bpaRequest);
			String action = bpaRequest.getBPA().getWorkflow().getAction();
			List<String> roles = bpaRequest.getRequestInfo().getUserInfo().getRoles().stream()
					.map(role -> role.getCode()).collect(Collectors.toList());
			log.info("Workflow action: {}, Roles: {}", action, roles);

			if ((action.equalsIgnoreCase(BPAConstants.ACTION_APPROVE)
					|| action.equalsIgnoreCase(BPAConstants.ACTION_APPROVE_AND_SEND_FOR_PAYMENT_RV))
					&& (roles.contains("EMPLOYEE") || roles.contains("BPA_ARC_APPROVER"))) {
				
				List<BPA> bpaList = assignApprovalNo(bpaRequest, revalidation);
				
				long permitExpiryDate = revalidation.getPermitExpiryDate();	
				log.info("Default Permit Expiry Date : "+ permitExpiryDate);
				if(bpaList !=null && bpaList.size()>1 ) {
					
					log.info("processRevalidation : Inside If Condition !!!");
					bpaList = bpaList.stream()
					    .filter(obj -> BPAConstants.STATUS_APPROVED.equals(obj.getStatus()) 
					    		&& (obj.getApprovalDate()!= null && obj.getApprovalDate()!= 0L)
					    		&& (obj.getApprovalExpiryDate()!= null && obj.getApprovalExpiryDate()!= 0L))
					    .sorted(Comparator.comparingLong(BPA::getApprovalExpiryDate).reversed())
					    .collect(Collectors.toList());
					if(!bpaList.isEmpty()) {
						 permitExpiryDate = bpaList.get(0).getApprovalExpiryDate();
						 log.info("Assigned Permit Expiry Date : "+ permitExpiryDate);
					}
				}
				
				Object mdmsData = util.mDMSCall(bpaRequest.getRequestInfo(), RegularizationConstants.STATE_TENANTID);
				boolean isODA = validateIfUlbUnderODA(bpaRequest.getBPA().getTenantId(), mdmsData);
				if (isODA)
					revalidation
							.setPermitExpiryDate(revalidationService.modifyDate(permitExpiryDate, 2));
				else
					revalidation
							.setPermitExpiryDate(revalidationService.modifyDate(permitExpiryDate, 1));
				
				bpaRequest.getBPA().setApprovalExpiryDate(revalidation.getPermitExpiryDate());
				log.info("ApprovalNo set: {}, ApprovalExpiryDate set: {}", bpaRequest.getBPA().getApprovalNo(),
						bpaRequest.getBPA().getApprovalExpiryDate());
			}
		}

		// Handle update if Pull Back is Done By Approver from Pending Sanc Fee Status
		handleIfPullBackRequest(bpaRequest, "BUILDING_PLAN_SCRUTINY");

		log.info("Calling workflow integrator.");
		wfIntegrator.callWorkFlow(bpaRequest);
		BusinessService businessService = workflowService.getBusinessService(bpaRequest.getBPA(),
				bpaRequest.getRequestInfo(), bpaRequest.getBPA().getApplicationNo());
		log.info("BusinessService fetched: {}", businessService);

		if (bpaRequest.getBPA().getWorkflow().getAction()
				.equalsIgnoreCase(BPAConstants.ACTION_APPROVE_AND_SEND_FOR_PAYMENT_RV)) {
			log.info("Action is APPROVE_AND_SEND_FOR_PAYMENT_RV, calculating Revalidation Fee.");
			revalidationService.revalidationFee(bpaRequest, BPAConstants.SANCTION_FEE_KEY);
		}

		log.info("Updating BPARequest in repository.");
		repository.update(bpaRequest,
				workflowService.isStateUpdatable(bpaRequest.getBPA().getStatus(), businessService));
		log.info("Is state updatable: {}",
				workflowService.isStateUpdatable(bpaRequest.getBPA().getStatus(), businessService));

		log.info("Exiting processRevalidation method.");
		return bpaRequest.getBPA();
	}

	private List<BPA> assignApprovalNo(BPARequest bpaRequest, Revalidation revalidation) {
		BPASearchCriteria criteria = BPASearchCriteria.builder().tenantId(revalidation.getTenantId())
				.approvalNo(revalidation.getPermitNo()).build();
		List<BPA> bpas = repository.getBPAData(criteria, null);

		if (Objects.isNull(bpas) || bpas.isEmpty()) {
			// No BPA applications found, generate approval number
			enrichmentService.generateApprovalNoForOutsideSujogRevalidationApplication(bpaRequest);
		} else {
			bpaRequest.getBPA().setApprovalNo(revalidation.getPermitNo());
		}
		
		return bpas;
	}

	private BPA processRevalidationBuildingPlanLayoutSignature(BPARequest bpaRequest) {
		// update the eg_bpa_document table to unlink the old filestoreid and link the
		// new filestoreid
		// for the document RVL.BUILDING_PERMIT_LAYOUTDRAWING
		log.info("inside method processRevalidationBuildingPlanLayoutSignature");
		List<BPA> searchResult = getBPAWithBPAId(bpaRequest);
		if (CollectionUtils.isEmpty(searchResult) || searchResult.size() > 1) {
			throw new CustomException(BPAErrorConstants.UPDATE_ERROR,
					"Failed to Update the Application, Found None or multiple applications!");
		}
		Map<String, Object> additionalDetails = (Map) bpaRequest.getBPA().getAdditionalDetails();
		// additionalDetails will always be a Map and will surely contain
		// applicationType then only this method invoked-
		additionalDetails.remove("applicationType");
		additionalDetails.put("buildingPlanLayoutIsSigned", true);
		bpaRequest.getBPA().setAuditDetails(searchResult.get(0).getAuditDetails());
		Optional<Document> buildingPlanLayoutDocument = searchResult.get(0).getDocuments().stream().filter(
				document -> document.getDocumentType().equals(BPAConstants.DOCUMENT_TYPE_REVISION_BUILDING_PLAN_LAYOUT))
				.findFirst();// RVL.BUILDING_PERMIT_LAYOUTDRAWING
		if (buildingPlanLayoutDocument.isPresent()) {
			Map<String, Object> unsignedBuildingPlanLayoutDetails = new HashMap<>();
			unsignedBuildingPlanLayoutDetails.put(BPAConstants.CODE,
					buildingPlanLayoutDocument.get().getDocumentType());
			unsignedBuildingPlanLayoutDetails.put(BPAConstants.FILESTOREID,
					buildingPlanLayoutDocument.get().getFileStoreId());
			additionalDetails.put(BPAConstants.UNSIGNED_REVISION_BUILDING_PLAN_LAYOUT_DETAILS,
					unsignedBuildingPlanLayoutDetails);
		}

		// setting IDs for newly added BPA Documents
		if (!CollectionUtils.isEmpty(bpaRequest.getBPA().getDocuments()))
			bpaRequest.getBPA().getDocuments().forEach(document -> {
				if (document.getId() == null) {
					document.setId(UUID.randomUUID().toString());
				}
			});

		repository.update(bpaRequest, true);
		return bpaRequest.getBPA();
	}

	public List<DocumentList> uploadDocument(DocUploadRequest docUploadRequest, RequestInfo requestInfo) {

		log.info("Document Upload Request Received");
		// validate user
		bpaValidator.validateDocUploadRequest(docUploadRequest);
		List<BPA> bpas = new LinkedList<>();
		List<String> edcrNos = null;
		BPASearchCriteria bpaSearchCriteria = BPASearchCriteria.builder()
				.applicationNo(docUploadRequest.getApplicationNo()).tenantId(docUploadRequest.getTenantId()).build();
		bpas = getBPAFromCriteria(bpaSearchCriteria, requestInfo, edcrNos);
		if (CollectionUtils.isEmpty(bpas)) {
			throw new CustomException(BPAErrorConstants.DOC_UPLOAD_ERROR,
					"Application Not found in the System for : " + docUploadRequest.getApplicationNo());
		}

		BPA bpaApplication = bpas.get(0);
		String tenantId = docUploadRequest.getTenantId().split("\\.")[0];
		Object mdmsData = util.mDMSCall(requestInfo, tenantId);
		BPASearchCriteria bpaSearchCriteria2 = BPASearchCriteria.builder().tenantId(docUploadRequest.getTenantId())
				.build();

		String docUploadtype = docUploadRequest.getDocUploadType();
		List<String> uuids = new ArrayList<String>();
		List<Role> roles = extracted(requestInfo, bpaApplication, bpaSearchCriteria2, uuids);
		List<String> roleCodes = roles.stream().map(Role::getCode).collect(Collectors.toList());

		BusinessService businessService = workflowService.getBusinessService(bpaApplication, requestInfo,
				docUploadRequest.getApplicationNo());
		String currentState = workflowService.getCurrentState(bpas.get(0).getStatus(), businessService);

		validateDocumentUploadRequest(docUploadRequest, bpas, mdmsData, currentState);

		boolean roleFound;
		switch (docUploadtype) {
		case BPAConstants.NOC_DOCUMENTS:
			roleFound = roleCodes.stream().anyMatch(BPAConstants.CITIZEN_ROLES_ALLOWED::contains);
			if (!roleFound) {
				throw new CustomException(BPAErrorConstants.INVALID_USER_TYPE,
						"Only " + BPAConstants.CITIZEN_ROLES_ALLOWED
								+ " are allowed to upload Documents for Ths Document Type ");
			}

			break;
		case BPAConstants.OTHER_DOCUMENTS:

			roleFound = roleCodes.stream().anyMatch(BPAConstants.APPROVER_ROLES_ALLOWED::contains);
			if (!roleFound) {
				throw new CustomException(BPAErrorConstants.INVALID_USER_TYPE,
						"Only " + BPAConstants.APPROVER_ROLES_ALLOWED
								+ " are allowed to upload Documents for Ths Document Type ");
			}

			if (!ObjectUtils.isEmpty(bpaApplication.getDscDetails()) && !org.springframework.util.StringUtils
					.hasText(bpaApplication.getDscDetails().get(0).getDocumentId())) {
				throw new CustomException(BPAErrorConstants.DOC_UPLOAD_ERROR,
						"BPA Certificate Not Signed for Application : " + docUploadRequest.getApplicationNo());
			}

			break;
		}

		HashMap<String, List<DocumentList>> documentMapping = enrichDocumentDetails(docUploadRequest, bpaApplication);

		List<DocumentList> documents = documentMapping.values().stream().flatMap(List::stream)
				.collect(Collectors.toList());
		BPADocUploadRequest updateRequest = getBPADocUploadRequest(docUploadRequest, documentMapping.get("DOC_UPDATE"));
		BPADocUploadRequest uploadRequest = getBPADocUploadRequest(docUploadRequest, documentMapping.get("DOC_UPLOAD"));

		if (!CollectionUtils.isEmpty(documentMapping.get("DOC_UPDATE"))) {
			log.info("Updating Documents");
			repository.updateDocument(updateRequest);
		}

		if (!CollectionUtils.isEmpty(documentMapping.get("DOC_UPLOAD"))) {
			log.info("Saving Documents");
			repository.saveDocument(uploadRequest);
		}

		return documents;

	}

	private List<Role> extracted(RequestInfo requestInfo, BPA bpaApplication, BPASearchCriteria bpaSearchCriteria2,
			List<String> uuids) {
//		if (bpaApplication.getAuditDetails() != null && !StringUtils.isEmpty(bpaApplication.getAuditDetails().getLastModifiedBy())) {
//			uuids.add(bpaApplication.getAuditDetails().getLastModifiedBy());
//			bpaSearchCriteria2.setOwnerIds(uuids);
//			bpaSearchCriteria2.setCreatedBy(uuids);
//		}
//		UserDetailResponse userInfo = userService.getUserForDocUpload(bpaSearchCriteria2, requestInfo);
//		if (userInfo == null || CollectionUtils.isEmpty(userInfo.getUser())
//				|| ObjectUtils.isEmpty(userInfo.getUser().get(0))) {
//			throw new CustomException(BPAErrorConstants.DOC_UPLOAD_ERROR,
//					"No Such User Found for Application : " + bpaApplication.getApplicationNo());
//		}
//		List<Role> roles = userInfo.getUser().get(0).getRoles(); 
		List<Role> roles = requestInfo.getUserInfo().getRoles();
		return roles;
	}

	private void validateDocumentUploadRequest(DocUploadRequest docUploadRequest, List<BPA> bpas, Object mdmsData,
			String currentState) {
		log.info("Validating Documents");
		validateWorkflow(bpas, currentState);
		bpaValidator.validateDocUpload(docUploadRequest, bpas, mdmsData, currentState);
	}

	private BPADocUploadRequest getBPADocUploadRequest(DocUploadRequest docUploadRequest, List<DocumentList> list) {
		DocUploadRequest documents = getDocUploadRequest(docUploadRequest, list);
		return BPADocUploadRequest.builder().docUploadRequest(documents).build();
	}

	private DocUploadRequest getDocUploadRequest(DocUploadRequest docUploadRequest, List<DocumentList> list) {
		String tenantId = docUploadRequest.getTenantId().split("\\.")[0];
		String applicationNo = docUploadRequest.getApplicationNo();
		return DocUploadRequest.builder().tenantId(tenantId).applicationNo(applicationNo).documents(list)
				.auditDetails(docUploadRequest.getAuditDetails()).build();
	}

	private void validateWorkflow(List<BPA> bpas, String currentState) {
		log.info("Current State : " + currentState);
		if (!currentState.equals("APPROVED")) {
			throw new CustomException(BPAErrorConstants.DOC_UPLOAD_ERROR, "Application Not Approved ");
		}
	}

	private HashMap<String, List<DocumentList>> enrichDocumentDetails(DocUploadRequest docUploadRequest, BPA bpas) {

		List<DocumentList> docUpdateDocuments = docUploadRequest.getDocuments().stream()
				.filter(doc -> org.springframework.util.StringUtils.hasText(doc.getId())).collect(Collectors.toList());
		List<DocumentList> docUploadDocuments = docUploadRequest.getDocuments().stream()
				.filter(doc -> org.springframework.util.StringUtils.isEmpty(doc.getId())).collect(Collectors.toList());

		HashMap<String, List<DocumentList>> documentMapping = new HashMap<>();
		documentMapping.put("DOC_UPDATE", docUpdateDocuments);
		documentMapping.put("DOC_UPLOAD", docUploadDocuments);

		// adding building plan id
		String buildingPlanId = bpas.getId();
		docUploadDocuments.stream().forEach(document -> {
			document.setBuildingPlanid(buildingPlanId);
		});

		// adding id
		if (!CollectionUtils.isEmpty(docUploadRequest.getDocuments()))
			docUploadDocuments.forEach(document -> {
				if (document.getId() == null) {
					document.setId(UUID.randomUUID().toString());
				}
			});

		return documentMapping;

	}

	/**
	 * Creates a new planning assistant checklist for a BPA application.
	 * 
	 * <p>
	 * This method performs the following operations:
	 * <ul>
	 * <li>Validates that the application number is present and not empty</li>
	 * <li>Validates the planning assistant checklist request data</li>
	 * <li>Verifies that exactly one BPA application exists for the given
	 * application number</li>
	 * <li>Enriches the request with audit details and persists the planning
	 * assistant checklist</li>
	 * </ul>
	 * 
	 * @param request the planning assistant checklist request containing checklist
	 *                details to be created
	 * @return the created PlanningAssistantChecklist object with enriched data
	 * @throws CustomException if application number is missing, validation fails,
	 *                         or if no BPA application or multiple BPA applications
	 *                         are found
	 */
	public PlanningAssistantChecklist createPlanningAssistantChecklist(
			@Valid PlanningAssistantChecklistRequest request) {

		PlanningAssistantChecklist planningAssistantChecklist = request.getPlanningAssistantChecklist();

		// Step 1: Validate application number is present
		String applicationNo = validatePlanningAssistantApplicationNumber(planningAssistantChecklist);

		// Step 2: Validate planning assistant checklist request data
		bpaValidator.validatePlanningAssitantChecklistRequest(planningAssistantChecklist);

		// Step 3: Verify that exactly one BPA application exists
		validateUniqueBPAForPlanningAssistant(applicationNo, planningAssistantChecklist.getTenantId());

		// Step 4: Enrich and persist the planning assistant checklist
		enrichmentService.enrichPlanningAssistantChecklistCreate(request);
		repository.savePlanningAssistantChecklist(request);

		// Return the created planning assistant checklist object
		return request.getPlanningAssistantChecklist();
	}

	/**
	 * Validates that the application number is present and not empty for planning
	 * assistant checklist creation.
	 * 
	 * @param planningAssistantChecklist the planning assistant checklist object
	 *                                   containing application number
	 * @return the validated application number
	 * @throws CustomException if application number is null or empty
	 */
	private String validatePlanningAssistantApplicationNumber(PlanningAssistantChecklist planningAssistantChecklist) {
		String applicationNo = planningAssistantChecklist.getApplicationno();

		if (Objects.isNull(applicationNo) || applicationNo.isEmpty()) {
			throw new CustomException("create error",
					"Failed to create planning assistant checklist, application no is mandatory");
		}

		return applicationNo;
	}

	/**
	 * Validates that exactly one BPA application exists for the given application
	 * number.
	 * 
	 * <p>
	 * This ensures data integrity by preventing checklist creation for non-existent
	 * or ambiguous BPA applications.
	 * 
	 * @param applicationNo the BPA application number
	 * @param tenantId      the tenant identifier
	 * @throws CustomException if no BPA application is found or if multiple BPA
	 *                         applications are found
	 */
	private void validateUniqueBPAForPlanningAssistant(String applicationNo, String tenantId) {
		// Build search criteria
		BPASearchCriteria bpaCriteria = new BPASearchCriteria();
		bpaCriteria.setApplicationNo(applicationNo);
		bpaCriteria.setTenantId(tenantId);

		// Fetch BPA applications
		List<String> edcrNos = new ArrayList<>();
		List<BPA> bpaList = repository.getBPAData(bpaCriteria, edcrNos);

		log.info("BPA applications found for planning assistant checklist: {}", bpaList);

		// Validate exactly one BPA application exists
		if (CollectionUtils.isEmpty(bpaList) || bpaList.size() > 1) {
			throw new CustomException("create error",
					"Failed to create planning assistant checklist, Found None or multiple BPA applications!");
		}
	}

	/**
	 * Updates an existing planning assistant checklist for a BPA application.
	 * 
	 * <p>
	 * This method performs the following operations:
	 * <ul>
	 * <li>Validates that the planning assistant checklist object is present</li>
	 * <li>Retrieves and verifies that an existing checklist record exists for the
	 * application</li>
	 * <li>Validates the planning assistant checklist request data</li>
	 * <li>Enriches the request with audit details and updates the planning
	 * assistant checklist</li>
	 * </ul>
	 * 
	 * @param request the planning assistant checklist request containing checklist
	 *                details to be updated
	 * @return the updated PlanningAssistantChecklist object with enriched data
	 * @throws CustomException if checklist object is null/empty, application number
	 *                         is missing, or if no existing checklist record is
	 *                         found
	 */
	public PlanningAssistantChecklist updatePlanningAssistantChecklist(
			@Valid PlanningAssistantChecklistRequest request) {

		// Step 1: Validate that the planning assistant checklist object is present
		PlanningAssistantChecklist planningAssistantChecklist = validatePlanningAssistantChecklistObject(request);

		// Step 2: Retrieve existing checklist records to ensure record exists
		String applicationNo = planningAssistantChecklist.getApplicationno();
		List<PlanningAssistantChecklist> existingChecklists = retrieveExistingPlanningAssistantChecklists(
				applicationNo);

		// Step 3: Validate planning assistant checklist request data
		bpaValidator.validatePlanningAssitantChecklistRequest(planningAssistantChecklist);

		// Step 4: Enrich and persist the updated planning assistant checklist
		enrichmentService.enrichPlanningAssistantChecklistUpdate(request, existingChecklists);
		repository.updatePlanningAssistantChecklist(request);

		// Return the updated planning assistant checklist object
		return request.getPlanningAssistantChecklist();
	}

	/**
	 * Validates that the planning assistant checklist object is present in the
	 * request.
	 * 
	 * @param request the planning assistant checklist request
	 * @return the validated planning assistant checklist object
	 * @throws CustomException if the checklist object is null or empty
	 */
	private PlanningAssistantChecklist validatePlanningAssistantChecklistObject(
			PlanningAssistantChecklistRequest request) {
		PlanningAssistantChecklist planningAssistantChecklist = request.getPlanningAssistantChecklist();

		if (ObjectUtils.isEmpty(planningAssistantChecklist) || Objects.isNull(planningAssistantChecklist)) {
			throw new CustomException("update error",
					"Failed to update, planning assistant checklist object is mandatory");
		}

		return planningAssistantChecklist;
	}

	/**
	 * Retrieves existing planning assistant checklists for the given application
	 * number.
	 * 
	 * <p>
	 * This method ensures that at least one checklist record exists before allowing
	 * an update operation.
	 * 
	 * @param applicationNo the BPA application number
	 * @return list of existing planning assistant checklists
	 * @throws CustomException if no checklist record is found for the application
	 *                         number
	 */
	private List<PlanningAssistantChecklist> retrieveExistingPlanningAssistantChecklists(String applicationNo) {
		// Build search criteria with application number
		PlanningAssistantSearchCriteria criteria = PlanningAssistantSearchCriteria.builder()
				.applicationNo(applicationNo).build();

		// Fetch existing checklist records
		List<PlanningAssistantChecklist> existingChecklists = repository.getPlanningAssistanctChecklist(criteria);

		// Validate that at least one record exists
		if (CollectionUtils.isEmpty(existingChecklists)) {
			throw new CustomException("update error",
					"No checklist record found for the application no " + applicationNo);
		}

		return existingChecklists;
	}

	private void validatePermitNumber(BPARequest bpaRequest) {

		org.egov.common.contract.request.RequestInfo requestInfo = bpaRequest.getRequestInfo();
		BPASearchCriteria criteria = new BPASearchCriteria();
		criteria.setTenantId(bpaRequest.getBPA().getTenantId());

		Map<String, Object> additionalDetails = (Map<String, Object>) bpaRequest.getBPA().getAdditionalDetails();
		Map<String, Object> oldApplicationDetails = (Map<String, Object>) additionalDetails.get("oldApplicationDetail");

		String permitNumber = (String) oldApplicationDetails.get("permitNumber");
		criteria.setOldPermitNumber(permitNumber);
		List<BPA> ocBpas = search(criteria, requestInfo);

		if (ocBpas.size() > 1) {
			throw new CustomException(BPAErrorConstants.CREATE_ERROR,
					"OC application already exists for the given permit number!");
		}

	}

	public PlinthApproval createPlinthApproval(@Valid PlinthApprovalRequest request) {
		PlinthApproval plinthApproval = request.getPlinthApproval();
		String bpaApplicationNo = plinthApproval.getBpaApplicationNo();
		BPASearchCriteria bpacriteria = new BPASearchCriteria();
		PlinthApprovalSearchCriteria plinthCriteria = new PlinthApprovalSearchCriteria();
		List<String> edcrNos = new ArrayList<>();
		if (Objects.isNull(bpaApplicationNo) || bpaApplicationNo.isEmpty())
			throw new CustomException("create error",
					"Failed to create plinth level approval, BPA application number is mandatory");
		plinthCriteria.setBpaApplicationNo(bpaApplicationNo);
		List<PlinthApproval> plas = repository.getPlinthApproval(plinthCriteria);
		if (!CollectionUtils.isEmpty(plas)) {
			if (!plas.get(0).getStatus().equalsIgnoreCase(BPAConstants.STATUS_REJECTED)) {
				throw new CustomException("create error", "Plinth level Approval Application :"
						+ plas.get(0).getApplicationNo() + " is exist for BPA Application number :" + bpaApplicationNo);
			}
		}
		bpaValidator.validateplinthApprovalRequest(plinthApproval);
		bpacriteria.setApplicationNo(bpaApplicationNo);
		bpacriteria.setTenantId(plinthApproval.getTenantId());
		List<BPA> bpa = repository.getBPAData(bpacriteria, edcrNos);
		log.info(bpa.toString());

		bpaValidator.validateBPAForPlinthCreateRequest(bpa, bpaApplicationNo);

		enrichmentService.enrichPlinthApprovalCreate(request, bpa);
		repository.savePlinthApproval(request);
		return request.getPlinthApproval();
	}

	public List<PlinthApproval> serachPlinthApproval(@Valid PlinthApprovalSearchCriteria criteria,
			RequestInfo requestInfo) {
		List<PlinthApproval> result = repository.getPlinthApproval(criteria);
		return result;
	}

	public PlinthApproval updatePlinthApproval(@Valid PlinthApprovalRequest request) {
		PlinthApproval plinthApproval = request.getPlinthApproval();
		String applicationNo = plinthApproval.getApplicationNo();
		PlinthApprovalSearchCriteria plinthCriteria = new PlinthApprovalSearchCriteria();
		if (Objects.isNull(applicationNo) || applicationNo.isEmpty())
			throw new CustomException("update error",
					"Failed to update plinth level approval, Application number is mandatory");
		bpaValidator.validateplinthApprovalRequest(plinthApproval);
		plinthCriteria.setApplicationNo(applicationNo);
		plinthCriteria.setTenantId(plinthApproval.getTenantId());
		List<PlinthApproval> plas = repository.getPlinthApproval(plinthCriteria);
		if (CollectionUtils.isEmpty(plas) || plas.size() > 1) {
			throw new CustomException("update error",
					"Failed to update Plinth Level Approval, Found None or multiple bpa applications for application number : "
							+ applicationNo);
		}

		enrichmentService.enrichPlinthApprovalUpdate(request);
		repository.upadtePlinthApproval(request);
		return request.getPlinthApproval();
	}

	/**
	 * Service Layer for providing Village for input Application Numbers
	 * 
	 * @param criteria
	 * @param requestInfo
	 * @return
	 */
	public List<BPAVillage> searchVillage(@Valid VillageSearchCriteria criteria, RequestInfo requestInfo) {

		List<BPAVillage> bpaVillages = new LinkedList<>();

		// validate the request here
		bpaValidator.validateVillageSearchRequest(criteria);

		// get the mapped data from the bpa repository
		bpaVillages = repository.getBPAVillagesData(criteria);

		return bpaVillages;
	}

	/**
	 * Updates an existing field inspection report for a BPA application.
	 * 
	 * <p>
	 * This method orchestrates the field inspection report update workflow:
	 * <ol>
	 * <li>Validates that application number is provided and not empty</li>
	 * <li>Validates field inspection request data (business rules validation)</li>
	 * <li>Retrieves the corresponding BPA application from the database</li>
	 * <li>Validates that exactly one BPA application exists for the given
	 * application number</li>
	 * <li>Enriches the field inspection report with update metadata (modified by,
	 * modified time, etc.)</li>
	 * <li>Persists the updated field inspection report to the database</li>
	 * </ol>
	 * 
	 * <p>
	 * <b>Use Case:</b> This method is used when inspectors need to modify or add
	 * information to an existing field inspection report, such as updating
	 * compliance status, adding observations, or correcting previously entered
	 * data.
	 * 
	 * <p>
	 * <b>Note:</b> This method reuses validation helper methods from the create
	 * workflow to ensure consistency in validation logic across create and update
	 * operations.
	 * 
	 * @param request The field inspection request containing updated inspection
	 *                details and metadata
	 * @return The updated FieldInspection object with enriched fields (modified
	 *         timestamps, etc.)
	 * @throws CustomException if application number is missing, field inspection
	 *                         validation fails, or BPA application is not found or
	 *                         multiple applications found
	 */
	public FieldInspection updateFieldInspectionReport(@Valid FieldInspectionRequest request) {
		FieldInspection fieldInspection = request.getFieldinspection();

		// Step 1: Validate application number is provided (reusing create validation)
		validateApplicationNumber(fieldInspection);

		// Step 2: Validate field inspection request data
		bpaValidator.validatefieldInspectionRequest(fieldInspection);

		// Step 3: Retrieve and validate the associated BPA application (reusing create
		// validation)
		retrieveAndValidateBpaApplication(fieldInspection);

		// Step 4: Enrich field inspection with update metadata (modified by, modified
		// time)
		enrichmentService.enrichUpdateFieldInspectionRequest(request);

		// Step 5: Persist the updated field inspection report to database
		repository.updateFieldInspectionReport(request);

		log.info("Successfully updated field inspection report for application: {}",
				fieldInspection.getApplicationno());

		return request.getFieldinspection();
	}

	/**
	 * Creates a new field inspection report with V2 workflow that supports draft
	 * and final submissions.
	 * 
	 * <p>
	 * This is an enhanced version of
	 * {@link #createFieldInspectionReport(FieldInspectionRequest)} that introduces
	 * the concept of "active" status in additional details to support:
	 * <ul>
	 * <li><b>Draft Reports (active=true):</b> Inspector can save work in progress
	 * without full validation</li>
	 * <li><b>Final Reports (active=false):</b> Inspector submits final report with
	 * complete validation</li>
	 * </ul>
	 * 
	 * <p>
	 * This method orchestrates the V2 field inspection report creation workflow:
	 * <ol>
	 * <li>Validates that application number is provided and not empty</li>
	 * <li>Validates that "active" parameter exists in additional details (mandatory
	 * for V2)</li>
	 * <li>Conditionally validates field inspection data based on active status</li>
	 * <li>Retrieves and validates the corresponding BPA application</li>
	 * <li>Enriches the field inspection report with auto-generated fields</li>
	 * <li>Persists the field inspection report to the database</li>
	 * </ol>
	 * 
	 * <p>
	 * <b>Use Case:</b> This method is used when the system needs to support
	 * progressive data entry, allowing inspectors to save partial reports as drafts
	 * and later submit them as final reports with full validation.
	 * 
	 * <p>
	 * <b>Key Difference from V1:</b> V2 requires "active" parameter and applies
	 * different validation rules based on whether the report is a draft
	 * (active=true) or final (active=false).
	 * 
	 * @param request The field inspection request containing inspection details and
	 *                metadata
	 * @return The created FieldInspection object with enriched fields (ID,
	 *         timestamps, etc.)
	 * @throws CustomException if application number is missing, active parameter is
	 *                         missing, validation fails, or BPA application is not
	 *                         found/multiple found
	 */
	public FieldInspection createFieldInspectionReportV2(@Valid FieldInspectionRequest request) {
		FieldInspection fieldInspection = request.getFieldinspection();

		// Step 1: Validate application number is provided (reusing V1 validation)
		validateApplicationNumber(fieldInspection);

		// Step 2: Validate and extract "active" parameter from additional details
		boolean isActive = validateAndExtractActiveParameter(fieldInspection);

		// Step 3: Conditionally validate based on active status
		// Draft reports (active=true) skip detailed validation
		// Final reports (active=false) require full V2 validation
		if (!isActive) {
			bpaValidator.validatefieldInspectionRequestV2(fieldInspection);
		} else {
			log.debug("Skipping detailed validation for draft field inspection report (active=true)");
		}

		// Step 4: Retrieve and validate the associated BPA application (reusing V1
		// validation)
		retrieveAndValidateBpaApplication(fieldInspection);

		// Step 5: Enrich field inspection with auto-generated fields
		enrichmentService.enrichcreatefieldinspectcreate(request);

		// Step 6: Persist the field inspection report to database
		repository.savefieldInspectionReport(request);

		log.info("Successfully created V2 field inspection report (active={}) for application: {}", isActive,
				fieldInspection.getApplicationno());

		return request.getFieldinspection();
	}

	/**
	 * Validates and extracts the "active" parameter from field inspection
	 * additional details.
	 * 
	 * <p>
	 * The "active" parameter is mandatory in V2 workflow and determines the
	 * validation level:
	 * <ul>
	 * <li><b>active=true:</b> Report is a draft; detailed validation is
	 * skipped</li>
	 * <li><b>active=false:</b> Report is final; full V2 validation is applied</li>
	 * </ul>
	 * 
	 * <p>
	 * <b>Why Required?</b> This parameter enables progressive data entry where
	 * inspectors can save incomplete work (draft) and later submit complete reports
	 * (final) without losing data.
	 * 
	 * @param fieldInspection The field inspection to validate
	 * @return boolean value of the active parameter (true for draft, false for
	 *         final)
	 * @throws CustomException if "active" parameter is missing in additional
	 *                         details
	 */
	@SuppressWarnings("unchecked")
	private boolean validateAndExtractActiveParameter(FieldInspection fieldInspection) {
		// Get or create additional details map
		Map<String, Object> additionalDetails = fieldInspection.getAdditionalDetails() != null
				? (Map<String, Object>) fieldInspection.getAdditionalDetails()
				: new HashMap<>();

		// Validate "active" parameter exists
		if (additionalDetails.get("active") == null) {
			throw new CustomException("CREATE_ERROR",
					"Failed to create V2 field inspection report: 'active' parameter is required in additional details. "
							+ "Set active=true for draft or active=false for final submission.");
		}

		// Extract and cast to Boolean
		Object activeValue = additionalDetails.get("active");
		Boolean isActiveBool = (Boolean) activeValue;

		log.debug("Active parameter validated: {} ({})", isActiveBool,
				isActiveBool ? "draft mode" : "final submission mode");

		return isActiveBool;
	}

	/**
	 * Update the FI report here
	 * 
	 * @param request
	 * @return
	 */
	/**
	 * Updates the field inspection report for a BPA application (Version 2).
	 * 
	 * <p>
	 * This method performs the following operations:
	 * <ul>
	 * <li>Validates the application number is present and not empty</li>
	 * <li>Validates the 'active' parameter in additional details</li>
	 * <li>Performs field inspection validation if the record is being
	 * deactivated</li>
	 * <li>Verifies that exactly one BPA application exists for the given
	 * application number</li>
	 * <li>Enriches the request with audit details and updates the field inspection
	 * report</li>
	 * </ul>
	 * 
	 * @param request the field inspection request containing the field inspection
	 *                details to be updated
	 * @return the updated FieldInspection object
	 * @throws CustomException if application number is missing, active parameter is
	 *                         missing, or if no BPA application or multiple BPA
	 *                         applications are found
	 */
	public FieldInspection updateFieldInspectionReportV2(@Valid FieldInspectionRequest request) {

		FieldInspection fieldInspection = request.getFieldinspection();

		// Step 1: Validate application number is present
		String applicationNo = validateAndRetrieveApplicationNumber(fieldInspection);

		// Step 2: Validate and check active parameter, perform additional validation if
		// deactivating
		validateActiveParameterAndConditionalInspection(fieldInspection);

		// Step 3: Fetch and validate BPA application exists and is unique
		validateBPAApplicationExists(applicationNo, fieldInspection.getTenantId());

		// Step 4: Enrich and persist the field inspection report
		enrichmentService.enrichUpdateFieldInspectionRequest(request);
		repository.updateFieldInspectionReport(request);

		// Return the updated field inspection object
		return request.getFieldinspection();
	}

	/**
	 * Validates that the application number is present and not empty.
	 * 
	 * @param fieldInspection the field inspection object containing application
	 *                        number
	 * @return the validated application number
	 * @throws CustomException if application number is null or empty
	 */
	private String validateAndRetrieveApplicationNumber(FieldInspection fieldInspection) {
		String applicationNo = fieldInspection.getApplicationno();

		if (Objects.isNull(applicationNo) || applicationNo.isEmpty()) {
			throw new CustomException("update error",
					"Failed to update field inspection report, application no is mandatory");
		}

		return applicationNo;
	}

	/**
	 * Validates the 'active' parameter in additional details and performs
	 * conditional field inspection validation.
	 * 
	 * <p>
	 * If the 'active' parameter is set to false (i.e., the record is being
	 * deactivated), this method triggers additional validation on the field
	 * inspection request.
	 * 
	 * @param fieldInspection the field inspection object containing additional
	 *                        details
	 * @throws CustomException if the 'active' parameter is not present in
	 *                         additional details
	 */
	private void validateActiveParameterAndConditionalInspection(FieldInspection fieldInspection) {
		// Extract additional details, defaulting to empty map if null
		// Note: Additional details can contain mixed types (String, Boolean, etc.)
		Map<String, Object> additionalDetails = fieldInspection.getAdditionalDetails() != null
				? (Map) fieldInspection.getAdditionalDetails()
				: new HashMap<String, Object>();

		// Validate that 'active' parameter exists
		if (additionalDetails.get("active") == null) {
			throw new CustomException("update error",
					"Failed to update field inspection report, active param is required in Additional Details!");
		}

		// If record is being deactivated (active = false), perform additional
		// validation
		Boolean isActive = (Boolean) additionalDetails.get("active");
		if (!isActive) {
			bpaValidator.validatefieldInspectionRequestV2(fieldInspection);
		}
	}

	/**
	 * Validates that exactly one BPA application exists for the given application
	 * number and tenant ID.
	 * 
	 * @param applicationNo the BPA application number
	 * @param tenantId      the tenant identifier
	 * @throws CustomException if no BPA application is found or if multiple BPA
	 *                         applications are found
	 */
	private void validateBPAApplicationExists(String applicationNo, String tenantId) {
		// Build search criteria
		BPASearchCriteria bpaCriteria = new BPASearchCriteria();
		bpaCriteria.setApplicationNo(applicationNo);
		bpaCriteria.setTenantId(tenantId);

		// Fetch BPA applications
		List<String> edcrNos = new ArrayList<>();
		List<BPA> bpaList = repository.getBPAData(bpaCriteria, edcrNos);

		log.info("BPA applications found: {}", bpaList);

		// Validate exactly one BPA application exists
		if (CollectionUtils.isEmpty(bpaList) || bpaList.size() > 1) {
			throw new CustomException("update error",
					"Failed to update field inspection report, Found None or multiple BPA applications!");
		}
	}

	/**
	 * Handle update request if Pull Back at Pending Sanc Fee
	 * 
	 * @param bpaRequest
	 * @param applicationType
	 */
	private void handleIfPullBackRequest(BPARequest bpaRequest, String applicationType) {

		if (bpaRequest.getBPA().getWorkflow().getAction().equalsIgnoreCase(BPAConstants.ACTION_PULL_BACK)) {

			Map<String, Boolean> isDataUpdateNeeded = new HashMap<>();

			Boolean isPaymentReceived = false;
			Demand demandToBeDeleted = null;
			Installment installmentToBeDeleted = null;
			DscDetails dscToBeDeleted = null;
			DscDetails planDscToBeDeleted = null;

			isPaymentReceived = bpaValidator.validateIfPaymentReceived(bpaRequest, applicationType);

			demandToBeDeleted = bpaValidator.validateDemandToBeDeleted(bpaRequest, isDataUpdateNeeded, applicationType);

			installmentToBeDeleted = bpaValidator.validateIfInstallmentToBeDeleted(bpaRequest, isDataUpdateNeeded);

			dscToBeDeleted = bpaValidator.validateIfDscToBeDeleted(bpaRequest, isDataUpdateNeeded);

			planDscToBeDeleted = bpaValidator.validateIfPlanDscToBeDeleted(bpaRequest, isDataUpdateNeeded);

			log.info("Demand To Be Deleted " + demandToBeDeleted);
			log.info("Installment To Be Deleted " + installmentToBeDeleted);
			log.info("DSC To Be Deleted " + dscToBeDeleted);
			log.info("Plan DSC To Be Deleted " + planDscToBeDeleted);

			deleteRequiredData(demandToBeDeleted, isPaymentReceived, isDataUpdateNeeded, bpaRequest);

		}
	}

	/**
	 * Delete demand data, installment data, dsc details data here for Pull Back
	 * Request
	 * 
	 * @param demandToBeUpdated
	 * @param isPaymentReceived
	 * @param isDataUpdateNeeded
	 */
	private void deleteRequiredData(Demand demandToBeDeleted, Boolean isPaymentReceived,
			Map<String, Boolean> isDataUpdateNeeded, BPARequest bpaRequest) {

		if (isDataUpdateNeeded.containsKey(IssueFixConstants.IS_DEMAND_DELETE_NEEDED)
				&& isDataUpdateNeeded.get(IssueFixConstants.IS_DEMAND_DELETE_NEEDED)) {
			issueFixRepository.deleteDemandDetail(demandToBeDeleted);
			issueFixRepository.deleteDemand(demandToBeDeleted);
			issueFixRepository.expireBill(demandToBeDeleted.getConsumerCode());
		}

		if (isDataUpdateNeeded.containsKey(IssueFixConstants.IS_INSTALLMENT_DELETE_NEEDED)
				&& isDataUpdateNeeded.get(IssueFixConstants.IS_INSTALLMENT_DELETE_NEEDED)) {
			issueFixRepository.deleteInstallments(bpaRequest.getBPA());
		}

		if (isDataUpdateNeeded.containsKey(IssueFixConstants.IS_DSC_DELETE_NEEDED)
				&& isDataUpdateNeeded.get(IssueFixConstants.IS_DSC_DELETE_NEEDED)) {
			issueFixRepository.deleteDsc(bpaRequest.getBPA());
		}

		if (isDataUpdateNeeded.containsKey(IssueFixConstants.IS_PLAN_DSC_DELETE_NEEDED)
				&& isDataUpdateNeeded.get(IssueFixConstants.IS_PLAN_DSC_DELETE_NEEDED)) {
			issueFixRepository.deletePlanDsc(bpaRequest.getBPA());
		}

	}

	/**
	 * Search BPA Applications Pending at Sanc Fee Status for more than 30 days
	 * 
	 * @param criteria
	 * @param requestInfo
	 * @return
	 */
	public List<FeePendingApplication> searchBPAFeePending(@Valid BPASearchCriteria criteria, RequestInfo requestInfo) {

		List<FeePendingApplication> feePendingApplications = new ArrayList<>();

		List<FeePendingApplication> response = new ArrayList<>();

		log.info("search criteria for fee status search: " + String.valueOf(criteria));

		bpaValidator.validateSearchFeePendingRequest(criteria);

		feePendingApplications = repository.searchBPAFeePendingApplications(criteria, requestInfo);

		// check if application was approved 30 days back and still pending for payment
		feePendingApplications.forEach(application -> {
			if (application.getAction().equalsIgnoreCase("APPROVE")
					|| application.getAction().equalsIgnoreCase("APPROVE_AND_SEND_FOR_PAYMENT")) {
				if (util.checkIfOlderThanThirtyDays(application.getApprovedDate())) {
					application.setDaysSinceApproved(util.calculateDaysSinceApproved(application.getApprovedDate()));
					response.add(application);
				}
			}
		});

		return response;
	}

	public BPADraft save(@Valid BPADraftRequest request) {
		bpaValidator.validateSaveDraft(request);
		enrichmentService.enrichBPASaveDraft(request);
		repository.save(request);
		return request.getBpaDraft();
	}

	public List<BPADraft> search(@Valid BPADraftSearchCriteria criteria, RequestInfo requestInfo) {
		List<BPADraft> bpaDrafts = new LinkedList<>();
		bpaValidator.validateDraftSearch(requestInfo, criteria);
		bpaDrafts = repository.getBPADraftData(criteria);
		return bpaDrafts;
	}

	public Integer count(@Valid BPASearchCriteria criteria, RequestInfo requestInfo) {

		int count = 0;
		bpaValidator.validateSearch(requestInfo, criteria);

		LandSearchCriteria landcriteria = new LandSearchCriteria();
		landcriteria.setTenantId(criteria.getTenantId());
		List<String> edcrNos = null;
		if (criteria.getMobileNumber() != null) {
			count = this.getBPACountFromMobileNumber(criteria, landcriteria, requestInfo);
		} else if (!StringUtils.isEmpty(criteria.getName())) {
			count = this.getBPACountFromName(criteria, landcriteria, requestInfo);
		} else {
			List<String> roles = new ArrayList<>();
			for (Role role : requestInfo.getUserInfo().getRoles()) {
				roles.add(role.getCode());
			}
			if ((criteria.tenantIdOnly() || criteria.isEmpty()) && roles.contains(BPAConstants.CITIZEN)) {
				log.info("loading count of created and by me");
				count = this.getBPACountCreatedForByMe(criteria, requestInfo, landcriteria, edcrNos);
				log.info("count query response" + count);
			} else {
				count = getBPACountFromCriteria(criteria, requestInfo, edcrNos);
			}
		}
		return count;
	}

	/**
	 * Retrieves the count of BPA (Building Plan Approval) applications based on
	 * various search criteria. This method supports multiple search strategies
	 * including mobile number, name, escalation status, and citizen-specific
	 * searches.
	 * 
	 * @param criteria    The search criteria containing filters like mobile number,
	 *                    name, tenant ID, escalation flags, etc.
	 * @param requestInfo The request metadata including user information and
	 *                    authorization details
	 * @return The total count of BPA applications matching the specified criteria
	 * 
	 * @see #determineBPACountStrategy(BPASearchCriteria, RequestInfo,
	 *      LandSearchCriteria, List)
	 * @see #getBPACountForCitizen(BPASearchCriteria, RequestInfo,
	 *      LandSearchCriteria, List)
	 */
	public Integer countv2(@Valid BPASearchCriteria criteria, RequestInfo requestInfo) {
		// Initialize count and validate search criteria
		int count = 0;

		// Validate the incoming search criteria
		bpaValidator.validateSearch(requestInfo, criteria);

		// Initialize land search criteria for associated land information
		LandSearchCriteria landcriteria = new LandSearchCriteria();
		landcriteria.setTenantId(criteria.getTenantId());
		landcriteria.setLimit(-1);

		// Initialize EDCR numbers list (currently not used but kept for future
		// extension)
		List<String> edcrNos = null;

		// Set unlimited results for count operations
		criteria.setLimit(-1);

		// Determine and execute the appropriate count strategy based on search criteria
		List<BPA> bpas = determineBPACountStrategy(criteria, requestInfo, landcriteria, edcrNos);

		// Calculate final count from the retrieved BPA list
		if (!CollectionUtils.isEmpty(bpas)) {
			count = bpas.size();
		}

		return count;
	}

	/**
	 * Determines and executes the appropriate BPA retrieval strategy based on the
	 * provided search criteria. This method acts as a router, directing the search
	 * to specialized methods based on the criteria type.
	 * 
	 * The priority order of search strategies is: 1. Mobile number search 2. Name
	 * search 3. Escalated applications 4. Applications escalated to current user 5.
	 * Applications about to be escalated 6. Citizen-specific search (if user has
	 * CITIZEN role) 7. General criteria-based search with land information
	 * population
	 * 
	 * @param criteria     The search criteria defining filters and search
	 *                     parameters
	 * @param requestInfo  The request metadata including user information
	 * @param landcriteria The land-specific search criteria for associated land
	 *                     information
	 * @param edcrNos      List of EDCR numbers (optional, can be null)
	 * @return List of BPA applications matching the specified criteria
	 */
	private List<BPA> determineBPACountStrategy(BPASearchCriteria criteria, RequestInfo requestInfo,
			LandSearchCriteria landcriteria, List<String> edcrNos) {

		List<BPA> bpas = new LinkedList<>();

		// Priority 1: Search by mobile number
		if (criteria.getMobileNumber() != null) {
			log.debug("Executing BPA count strategy: Mobile Number Search");
			bpas = this.getBPAFromMobileNumber(criteria, landcriteria, requestInfo);
		}
		// Priority 2: Search by owner/applicant name
		else if (!StringUtils.isEmpty(criteria.getName())) {
			log.debug("Executing BPA count strategy: Name Search");
			bpas = this.getBPAFromName(criteria, landcriteria, requestInfo);
		}
		// Priority 3: Search for auto-escalated applications
		else if (criteria.isEscalated()) {
			log.debug("Executing BPA count strategy: Auto-Escalated Applications");
			bpas = this.getBPAAutoEscalated(criteria, requestInfo);
		}
		// Priority 4: Search for applications escalated to the current user
		else if (criteria.isEscalatedToMe()) {
			log.debug("Executing BPA count strategy: Applications Escalated To Me");
			bpas = this.getBPAAutoEscalatedToMe(criteria, requestInfo);
		}
		// Priority 5: Search for applications about to be auto-escalated
		else if (criteria.isAboutToEscalate()) {
			log.debug("Executing BPA count strategy: Applications About To Escalate");
			bpas = this.getBPAAboutToAutoEscalate(criteria, requestInfo);
		}
		// Priority 6 & 7: Handle default search scenarios
		else {
			bpas = handleDefaultCountStrategy(criteria, requestInfo, landcriteria, edcrNos);
		}

		return bpas;
	}

	/**
	 * Handles the default BPA count strategy when no specific search criteria is
	 * provided. This method differentiates between citizen users and other users
	 * (officers/employees).
	 * 
	 * For citizen users with minimal criteria (tenant ID only or empty criteria): -
	 * Retrieves applications created by or for the citizen
	 * 
	 * For other users or specific criteria: - Retrieves applications based on
	 * criteria - Enriches the results with associated land information - Enriches
	 * the results with plinth approval information
	 * 
	 * @param criteria     The search criteria
	 * @param requestInfo  The request metadata including user information
	 * @param landcriteria The land-specific search criteria
	 * @param edcrNos      List of EDCR numbers (optional)
	 * @return List of BPA applications with enriched information
	 */
	private List<BPA> handleDefaultCountStrategy(BPASearchCriteria criteria, RequestInfo requestInfo,
			LandSearchCriteria landcriteria, List<String> edcrNos) {

		List<BPA> bpas;

		// Extract user roles from request info
		List<String> roles = extractUserRoles(requestInfo);

		// Check if the user is a citizen with minimal search criteria
		if (isCitizenWithMinimalCriteria(criteria, roles)) {
			log.debug("Executing BPA count strategy: Citizen - Created By/For Me");
			bpas = executeCitizenSearch(criteria, requestInfo, landcriteria);
		}
		// Standard search with land and plinth information enrichment
		else {
			log.debug("Executing BPA count strategy: General Criteria-based Search");
			bpas = executeGeneralSearchWithEnrichment(criteria, requestInfo, landcriteria);
		}

		return bpas;
	}

	private Integer getBPACountFromMobileNumber(BPASearchCriteria criteria, LandSearchCriteria landcriteria,
			RequestInfo requestInfo) {
		List<BPA> bpas = null;
		log.debug("Call with mobile number to Land::" + criteria.getMobileNumber());
		landcriteria.setMobileNumber(criteria.getMobileNumber());
		ArrayList<LandInfo> landInfo = landService.searchLandInfoToBPA(requestInfo, landcriteria);
		ArrayList<String> landId = new ArrayList<String>();
		if (landInfo.size() > 0) {
			landInfo.forEach(land -> {
				landId.add(land.getId());
			});
			criteria.setLandId(landId);
		}
		return getBPACountFromLandId(criteria, requestInfo, null);

	}

	private Integer getBPACountFromName(BPASearchCriteria criteria, LandSearchCriteria landcriteria,
			RequestInfo requestInfo) {
		List<BPA> bpas = null;
		log.debug("Call with Name to Land::" + criteria.getName());
		landcriteria.setName(criteria.getName());
		ArrayList<LandInfo> landInfo = landService.searchLandInfoToBPA(requestInfo, landcriteria);
		ArrayList<String> landId = new ArrayList<String>();
		if (landInfo.size() > 0) {
			landInfo.forEach(land -> {
				landId.add(land.getId());
			});
			criteria.setLandId(landId);
		}

		return getBPACountFromLandId(criteria, requestInfo, null);

	}

	private Integer getBPACountCreatedForByMe(BPASearchCriteria criteria, RequestInfo requestInfo,
			LandSearchCriteria landcriteria, List<String> edcrNos) {
		int count = 0;
		UserSearchRequest userSearchRequest = new UserSearchRequest();
		if (criteria.getTenantId() != null) {
			userSearchRequest.setTenantId(criteria.getTenantId());
		}
		List<String> uuids = new ArrayList<String>();
		if (requestInfo.getUserInfo() != null && !StringUtils.isEmpty(requestInfo.getUserInfo().getUuid())) {
			uuids.add(requestInfo.getUserInfo().getUuid());
			criteria.setOwnerIds(uuids);
			criteria.setCreatedBy(uuids);
		}
		log.info("loading data of created and by me" + uuids.toString());
		UserDetailResponse userInfo = userService.getUser(criteria, requestInfo);
		if (userInfo != null) {
			landcriteria.setMobileNumber(userInfo.getUser().get(0).getMobileNumber());
		}
		log.info("Call with multiple to Land::" + landcriteria.getTenantId() + landcriteria.getMobileNumber());
		ArrayList<LandInfo> landInfos = landService.searchLandInfoToBPA(requestInfo, landcriteria);
		ArrayList<String> landIds = new ArrayList<String>();

		count = getBPACountFromCriteria(criteria, requestInfo, edcrNos);

		return count;
	}

	private Integer getBPACountFromLandId(BPASearchCriteria criteria, RequestInfo requestInfo, List<String> edcrNos) {
		return repository.getBPACountData(criteria, edcrNos);
	}

	public Integer getBPACountFromCriteria(BPASearchCriteria criteria, RequestInfo requestInfo, List<String> edcrNos) {
		return repository.getBPACountData(criteria, edcrNos);
	}

	public Integer draftCount(@Valid BPADraftSearchCriteria criteria, RequestInfo requestInfo) {
		return repository.getBPADraftCountData(criteria);
	}

	public Integer searchDscDetailsCount(BPASearchCriteria criteria, RequestInfo requestInfo) {
		Integer count = 0;
		bpaValidator.validateDscSearch(criteria, requestInfo);
		count = repository.getDscDetails(criteria).size();
		return count;
	}

	public Integer searchPlanDscDetailsCount(@Valid BPASearchCriteria criteria, RequestInfo requestInfo) {
		Integer count = 0;
		bpaValidator.validateDscSearch(criteria, requestInfo);
		count = repository.getPlanDscDetails(criteria).size();
		return count;
	}

	public Integer searchApplicationApprovedByCount(@Valid BPASearchCriteria criteria, String uuid) {
		Integer count = 0;
		count = repository.getApprovedbyData(uuid, criteria).size();
		return count;
	}

	public CompletionCertificate create(@Valid CompletionCertificateRequest completionCertificateRequest) {
		bpaValidator.validateCompletionCertificateCreateRequest(completionCertificateRequest);
		enrichmentService.enrichCompletionCertificateCreateRequest(completionCertificateRequest);
		repository.save(completionCertificateRequest);
		return completionCertificateRequest.getCompletionCertificate();
	}

	public List<CompletionCertificate> search(@Valid CompletionCertificateSearchCriteria criteria,
			RequestInfo requestInfo) {
		bpaValidator.validateCompletionCertificateSearchRequest(criteria);
		List<CompletionCertificate> completionCertificates = repository.getCompletionCertificateData(criteria);
		return completionCertificates;
	}

	public Integer count(@Valid CompletionCertificateSearchCriteria criteria, RequestInfo requestInfo) {
		List<CompletionCertificate> completionCertificates = repository.getCompletionCertificateData(criteria);
		return completionCertificates.size();
	}

	public List<StageWiseReport> create(@Valid StageWiseReportRequest stageWiseReportRequest) {
		bpaValidator.validateStageWiseReportRequest(stageWiseReportRequest);
		enrichmentService.enrichCompletionCertificateCreateRequest(stageWiseReportRequest);
		repository.save(stageWiseReportRequest);
		return stageWiseReportRequest.getStageWiseReports();
	}

	public List<StageWiseReport> update(@Valid StageWiseReportRequest stageWiseReportRequest) {
		bpaValidator.validateStageWiseReportRequest(stageWiseReportRequest);
		enrichmentService.enrichCompletionCertificateUpdateRequest(stageWiseReportRequest);
		repository.update(stageWiseReportRequest);
		return stageWiseReportRequest.getStageWiseReports();
	}

	public List<StageWiseReport> search(@Valid StageWiseReportSearchCriteria criteria, RequestInfo requestInfo) {
		List<StageWiseReport> stageWiseReports = repository.getStageWiseReports(criteria);
		return stageWiseReports;
	}

	public String getStageWiseReportFileStoreId() {
		return config.getStageWiseReportFilestoreId();
	}
	
	private boolean validateIfUlbUnderODA(String tenantId, Object mdmsData) {
	    try {
	        if (tenantId == null || mdmsData == null) {
	            log.warn("Validation failed: tenantId or mdmsData is null");
	            return false;
	        }

	        List<LinkedHashMap<String, Object>> odaUlbsFromMdms = JsonPath.read(
	                mdmsData, RegularizationConstants.ODA_ULBS_JSONPATH_CODE);

	        if (odaUlbsFromMdms == null || odaUlbsFromMdms.isEmpty()) {
	        	log.info("No ODA ULBs found in MDMS data");
	            return false;
	        }

	        boolean result = odaUlbsFromMdms.stream()
	                .filter(ulb -> "true".equalsIgnoreCase((String) ulb.get("oda")))
	                .map(ulb -> (String) ulb.get("ulb"))
	                .anyMatch(tenantId::equals);

	        log.debug("TenantId [{}] under ODA: {}", tenantId, result);
	        return result;

	    } catch (Exception e) {
	    	log.error("Error validating tenantId [{}] against ODA ULBs", tenantId, e);
	        return false;
	    }
}

	
public Boolean isFirstPermitForOutsideSujogApplicationTemp(RequestInfo requestInfo, String permitNo) {

	Boolean isFirstPermit = Boolean.FALSE;
	BPASearchCriteria bpaSearchCriteria = BPASearchCriteria.builder().approvalNo(permitNo).build();
	List<BPA> bpas = getBPAFromCriteria(bpaSearchCriteria, requestInfo, null);
	bpas = bpas.stream().filter(obj -> BPAConstants.STATUS_APPROVED.equals(obj.getStatus())
			&& obj.getApprovalDate() != null && obj.getApprovalDate() != 0L).collect(Collectors.toList());

	if (bpas.size() == 0) {
		isFirstPermit = Boolean.TRUE;
	}
	log.info("Print the Pemit "+isFirstPermit);
	return isFirstPermit;
}

public Boolean isScrutinyDoneOutsideSujogTemp(RequestInfo requestInfo, Revalidation revalidation) {

	Boolean isSrutinyOutsideSujog = Boolean.FALSE;
	BPASearchCriteria bpaSearchCriteria = BPASearchCriteria.builder().approvalNo(revalidation.getPermitNo()).build();
	List<BPA> bpas = getBPAFromCriteria(bpaSearchCriteria, requestInfo, null);
	List<BPA> sortedBPAs = bpas.stream()
			.filter(obj -> BPAConstants.STATUS_APPROVED.equals(obj.getStatus()) && obj.getApprovalDate() != null
					&& obj.getApprovalDate() != 0L)
			.sorted(Comparator.comparing(BPA::getApprovalDate)).collect(Collectors.toList());
	
	if(sortedBPAs.isEmpty()) {
		return Boolean.FALSE;
	}
	BPA firstRevalidationApplication = sortedBPAs.get(0);
	List<Revalidation> revalidations = revalidationRepository.getRevalidationData(RevalidationSearchCriteria.builder()
			.bpaApplicationNo(firstRevalidationApplication.getApplicationNo()).build());

	if (!revalidations.isEmpty() && !revalidations.get(0).isSujogExistingApplication()) {
		isSrutinyOutsideSujog = Boolean.TRUE;
	}
	
	log.info("Print isScrutinyDoneOutsideSujog :: " + isSrutinyOutsideSujog + " approval date :: " + sortedBPAs.get(0).getApprovalDate());
	return isSrutinyOutsideSujog;
}

}
