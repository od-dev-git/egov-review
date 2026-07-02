package org.egov.bpa.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.validation.Valid;

import org.apache.commons.lang3.ObjectUtils;
import org.egov.bpa.config.BPAConfiguration;
import org.egov.bpa.repository.BPARepository;
import org.egov.bpa.repository.IdGenRepository;
import org.egov.bpa.util.BPAConstants;
import org.egov.bpa.util.BPAErrorConstants;
import org.egov.bpa.util.BPAUtil;
import org.egov.bpa.web.model.AuditDetails;
import org.egov.bpa.web.model.BPA;
import org.egov.bpa.web.model.BPADraftRequest;
import org.egov.bpa.web.model.BPARequest;
import org.egov.bpa.web.model.CompletionCertificate;
import org.egov.bpa.web.model.CompletionCertificateRequest;
import org.egov.bpa.web.model.DocRemarkRequest;
import org.egov.bpa.web.model.DscDetails;
import org.egov.bpa.web.model.FieldInspectionRequest;
import org.egov.bpa.web.model.NoticeRequest;
import org.egov.bpa.web.model.PlanningAssistantChecklist;
import org.egov.bpa.web.model.PlanningAssistantChecklistRequest;
import org.egov.bpa.web.model.PlinthApproval;
import org.egov.bpa.web.model.PlinthApprovalRequest;
import org.egov.bpa.web.model.PreapprovedPlanRequest;
import org.egov.bpa.web.model.Revalidation;
import org.egov.bpa.web.model.RevalidationRequest;
import org.egov.bpa.web.model.RevisionRequest;
import org.egov.bpa.web.model.StageWiseReport;
import org.egov.bpa.web.model.StageWiseReportRequest;
import org.egov.bpa.web.model.Workflow;
import org.egov.bpa.web.model.accreditedperson.AccreditedPersonRequest;
import org.egov.bpa.web.model.edcr.CustomEdcrDetail;
import org.egov.bpa.web.model.edcr.EdcrDetail;
import org.egov.bpa.web.model.edcr.FarDetails;
import org.egov.bpa.web.model.edcr.OccupancyHelperDetail;
import org.egov.bpa.web.model.edcr.OccupancyTypeHelper;
import org.egov.bpa.web.model.edcr.Plan;
import org.egov.bpa.web.model.edcr.PlanInformation;
import org.egov.bpa.web.model.edcr.Plot;
import org.egov.bpa.web.model.edcr.VirtualBuilding;
import org.egov.bpa.web.model.idgen.IdResponse;
import org.egov.bpa.web.model.oc.BuildingBlockDetails;
import org.egov.bpa.web.model.oc.Floor;
import org.egov.bpa.web.model.oc.OCConstants;
import org.egov.bpa.web.model.oc.OutsideOCDetails;
import org.egov.bpa.web.model.oc.PlotDetails;
import org.egov.bpa.web.model.oc.ScrutinyDetails;
import org.egov.bpa.web.model.user.UserDetailResponse;
import org.egov.bpa.web.model.workflow.BusinessService;
import org.egov.bpa.workflow.WorkflowIntegrator;
import org.egov.bpa.workflow.WorkflowService;
import org.egov.common.contract.request.RequestInfo;
import org.egov.tracer.model.CustomException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.TypeRef;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class EnrichmentService {

	@Autowired
	private BPAConfiguration config;

	@Autowired
	private BPAUtil bpaUtil;

	@Autowired
	private IdGenRepository idGenRepository;

	@Autowired
	private WorkflowService workflowService;

	@Autowired
	private EDCRService edcrService;

	@Autowired
	private WorkflowIntegrator wfIntegrator;

	@Autowired
	private NocService nocService;

	@Autowired
	private BPAUtil util;

	@Autowired
	private UserService userService;

	@Autowired
	private BPARepository bpaRepository;

	/**
	 * Enriches BPA create request with generated IDs, audit details, and business
	 * service configuration.
	 * 
	 * <p>
	 * This method orchestrates the complete BPA creation enrichment lifecycle:
	 * <ul>
	 * <li>Adds audit details (created by, created time) to track data lineage</li>
	 * <li>Generates unique IDs for BPA and related documents</li>
	 * <li>Determines appropriate business service based on application type and
	 * risk</li>
	 * <li>Links land information to BPA application</li>
	 * <li>Generates application number via IdGen service</li>
	 * </ul>
	 * 
	 * <p>
	 * <strong>Business Logic:</strong>
	 * <ul>
	 * <li><strong>Business Service Assignment:</strong>
	 * <ul>
	 * <li>Building Plan (Non-LOW risk) → BPA</li>
	 * <li>Building Plan (LOW risk) → BPA_LOW</li>
	 * <li>Occupancy Certificate → BPA_OC</li>
	 * </ul>
	 * </li>
	 * <li><strong>Account ID:</strong> Set to the user who created the
	 * application</li>
	 * <li><strong>Land Linkage:</strong> Associates BPA with land parcel
	 * information</li>
	 * </ul>
	 * 
	 * @param bpaRequest the BPA request to enrich
	 * @param mdmsData   the master data from MDMS (currently unused but available
	 *                   for future use)
	 * @param values     map containing extracted EDCR values (applicationType,
	 *                   landId, etc.)
	 */
	public void enrichBPACreateRequest(BPARequest bpaRequest, Object mdmsData, Map<String, String> values) {

		BPA bpa = bpaRequest.getBPA();
		RequestInfo requestInfo = bpaRequest.getRequestInfo();

		log.info("Enriching BPA create request for tenant: {}", bpa.getTenantId());

		// Step 1: Add audit details and base identifiers
		enrichAuditDetailsAndIds(bpa, requestInfo);

		// Step 2: Determine and set business service
		String applicationType = values.get(BPAConstants.APPLICATIONTYPE);
		determineAndSetBusinessService(bpa, applicationType, values);

		// Step 3: Link land information
		enrichLandInformation(bpa);

		// Step 4: Enrich document IDs
		enrichDocumentIds(bpa);

		// Step 5: Generate and set application number
		setIdgenIds(bpaRequest);

		log.debug("BPA create request enriched successfully with application type: {}, business service: {}",
				applicationType, bpa.getBusinessService());
	}

	/**
	 * Enriches BPA with audit details and base identifiers.
	 * 
	 * <p>
	 * This method:
	 * <ul>
	 * <li>Generates unique UUID for BPA entity</li>
	 * <li>Adds audit details with creator and timestamp</li>
	 * <li>Sets account ID to the creator's UUID</li>
	 * </ul>
	 * 
	 * @param bpa         the BPA application to enrich
	 * @param requestInfo the request information containing user details
	 */
	private void enrichAuditDetailsAndIds(BPA bpa, RequestInfo requestInfo) {
		// Generate audit details for creation
		AuditDetails auditDetails = bpaUtil.getAuditDetails(requestInfo.getUserInfo().getUuid(), true);

		// Set BPA identifiers and audit
		bpa.setId(UUID.randomUUID().toString());
		bpa.setAuditDetails(auditDetails);
		bpa.setAccountId(auditDetails.getCreatedBy());
	}

	/**
	 * Determines and sets the appropriate business service based on application
	 * type and risk.
	 * 
	 * <p>
	 * <strong>Business Service Mapping:</strong>
	 * <ul>
	 * <li><strong>BPA (Building Plan Approval):</strong> For non-LOW risk building
	 * plans</li>
	 * <li><strong>BPA_LOW:</strong> For LOW risk building plans (simplified
	 * workflow)</li>
	 * <li><strong>BPA_OC (Occupancy Certificate):</strong> For occupancy
	 * certificate applications</li>
	 * </ul>
	 * 
	 * <p>
	 * For OC applications, also links the land ID from EDCR values.
	 * 
	 * @param bpa             the BPA application
	 * @param applicationType the application type from EDCR
	 * @param values          map containing EDCR extracted values
	 */
	private void determineAndSetBusinessService(BPA bpa, String applicationType, Map<String, String> values) {
		if (BPAConstants.BUILDING_PLAN.equalsIgnoreCase(applicationType)) {
			// Building Plan: determine service based on risk type
			if (BPAConstants.LOW_RISKTYPE.equalsIgnoreCase(bpa.getRiskType())) {
				bpa.setBusinessService(BPAConstants.BPA_LOW_MODULE_CODE);
				log.debug("LOW risk building plan - assigned BPA_LOW business service");
			} else {
				bpa.setBusinessService(BPAConstants.BPA_MODULE_CODE);
				log.debug("Non-LOW risk building plan - assigned BPA business service");
			}
		} else {
			// Occupancy Certificate
			bpa.setBusinessService(BPAConstants.BPA_OC_MODULE_CODE);
			bpa.setLandId(values.get("landId"));
			log.debug("Occupancy Certificate - assigned BPA_OC business service");
		}
	}

	/**
	 * Enriches BPA with land information linkage.
	 * 
	 * <p>
	 * If land information is provided in the request, extracts and sets the land ID
	 * for linking BPA to the land parcel.
	 * 
	 * @param bpa the BPA application
	 */
	private void enrichLandInformation(BPA bpa) {
		if (bpa.getLandInfo() != null) {
			bpa.setLandId(bpa.getLandInfo().getId());
			log.debug("Land information linked with land ID: {}", bpa.getLandId());
		}
	}

	/**
	 * Enriches document IDs for all documents in the BPA application.
	 * 
	 * <p>
	 * Generates unique UUIDs for any documents that don't already have IDs. This
	 * ensures all documents can be uniquely identified in the database.
	 * 
	 * @param bpa the BPA application with documents
	 */
	private void enrichDocumentIds(BPA bpa) {
		if (!CollectionUtils.isEmpty(bpa.getDocuments())) {
			bpa.getDocuments().forEach(document -> {
				if (document.getId() == null) {
					document.setId(UUID.randomUUID().toString());
				}
			});
			log.debug("Enriched {} documents with IDs", bpa.getDocuments().size());
		}
	}

	/**
	 * Sets the ApplicationNumber for given bpaRequest
	 *
	 * @param request bpaRequest which is to be created
	 */
	private void setIdgenIds(BPARequest request) {
		RequestInfo requestInfo = request.getRequestInfo();
		String tenantId = request.getBPA().getTenantId();
		// String tenantId ="od.cuttack";
		BPA bpa = request.getBPA();
		List<String> applicationNumbers = new ArrayList<>();
		if (BPAConstants.BPA_AC_MODULE_CODE.equalsIgnoreCase(request.getBPA().getBusinessService())) {
			applicationNumbers = getIdList(requestInfo, tenantId, config.getApplicationNoIdgenNameforBPA5(),
					config.getApplicationNoIdgenFormatforBPA5(), 1);
			// System.out.println("idgen:"+applicationNumbers);
		} else if (BPAConstants.BPA_PAP_MODULE_CODE.equalsIgnoreCase(request.getBPA().getBusinessService())) {
			applicationNumbers = getIdList(requestInfo, tenantId, config.getApplicationNoIdgenNameforBPA6(),
					config.getApplicationNoIdgenFormatforBPA6(), 1);
		} else {
			applicationNumbers = getIdList(requestInfo, tenantId, config.getApplicationNoIdgenName(),
					config.getApplicationNoIdgenFormat(), 1);
			// System.out.println("idgen1:"+applicationNumbers);
		}
		ListIterator<String> itr = applicationNumbers.listIterator();

		Map<String, String> errorMap = new HashMap<>();

		if (!errorMap.isEmpty())
			throw new CustomException(errorMap);

		bpa.setApplicationNo(itr.next());
	}

	/**
	 * Returns a list of numbers generated from idgen
	 *
	 * @param requestInfo RequestInfo from the request
	 * @param tenantId    tenantId of the city
	 * @param idKey       code of the field defined in application properties for
	 *                    which ids are generated for
	 * @param idformat    format in which ids are to be generated
	 * @param count       Number of ids to be generated
	 * @return List of ids generated using idGen service
	 */
	private List<String> getIdList(RequestInfo requestInfo, String tenantId, String idKey, String idformat, int count) {
		List<IdResponse> idResponses = idGenRepository.getId(requestInfo, tenantId, idKey, idformat, count)
				.getIdResponses();

		if (CollectionUtils.isEmpty(idResponses))
			throw new CustomException(BPAErrorConstants.IDGEN_ERROR, "No ids returned from idgen Service");

		return idResponses.stream().map(IdResponse::getId).collect(Collectors.toList());
	}

	/**
	 * Enriches BPA update request with audit details, document IDs, assignees, and
	 * DSC details.
	 * 
	 * <p>
	 * This method orchestrates the complete BPA update enrichment lifecycle:
	 * <ul>
	 * <li>Updates audit details (modified by, modified time) while preserving
	 * creation audit</li>
	 * <li>Generates unique IDs for any new BPA documents and workflow verification
	 * documents</li>
	 * <li>Enriches workflow assignees based on workflow action</li>
	 * <li>Handles Digital Signature Certificate (DSC) details based on workflow
	 * action and user roles</li>
	 * </ul>
	 * 
	 * <p>
	 * <strong>Business Logic:</strong>
	 * <ul>
	 * <li><strong>Audit Trail:</strong> Preserves original creation details,
	 * updates modification timestamp</li>
	 * <li><strong>Document Management:</strong> Auto-generates UUIDs for new
	 * documents to ensure uniqueness</li>
	 * <li><strong>Assignee Enrichment:</strong> Determines next assignees based on
	 * workflow state transitions</li>
	 * <li><strong>DSC Integration:</strong>
	 * <ul>
	 * <li>For APPROVE action by EMPLOYEE/BPA_ARC_APPROVER → Creates DSC details for
	 * both BPA and Plan</li>
	 * <li>For REJECT action by EMPLOYEE/BPA_ARC_APPROVER → Creates DSC details for
	 * rejection record</li>
	 * <li>For other actions → Clears DSC details</li>
	 * </ul>
	 * </li>
	 * </ul>
	 * 
	 * @param bpaRequest      the BPA update request containing updated BPA details
	 *                        and workflow
	 * @param businessService the workflow business service configuration (reserved
	 *                        for future use)
	 */
	public void enrichBPAUpdateRequest(BPARequest bpaRequest, BusinessService businessService) {
		log.debug("Enriching BPA update request for application: {}", bpaRequest.getBPA().getApplicationNo());

		// Step 1: Update audit details while preserving creation information
		enrichUpdateAuditDetails(bpaRequest);

		// Step 2: Enrich workflow assignees based on action
		enrichAssignes(bpaRequest.getBPA());

		// Step 3: Generate IDs for new documents
		enrichBPADocuments(bpaRequest.getBPA());
		enrichWorkflowVerificationDocuments(bpaRequest.getBPA());

		// Step 4: Handle DSC details based on workflow action and user roles
		enrichDscDetailsForUpdate(bpaRequest);

		log.debug("BPA update request enriched successfully");
	}

	/**
	 * Updates audit details for BPA update operation.
	 * 
	 * <p>
	 * Preserves the original creation audit trail (createdBy, createdTime) while
	 * updating the last modification timestamp with current user and time.
	 * 
	 * @param bpaRequest the BPA request with request info and BPA entity
	 */
	private void enrichUpdateAuditDetails(BPARequest bpaRequest) {
		RequestInfo requestInfo = bpaRequest.getRequestInfo();
		BPA bpa = bpaRequest.getBPA();

		// Generate new audit details for update operation
		AuditDetails auditDetails = bpaUtil.getAuditDetails(requestInfo.getUserInfo().getUuid(), false);

		// Preserve original creation audit information
		auditDetails.setCreatedBy(bpa.getAuditDetails().getCreatedBy());
		auditDetails.setCreatedTime(bpa.getAuditDetails().getCreatedTime());

		// Update only the last modified timestamp
		bpa.getAuditDetails().setLastModifiedTime(auditDetails.getLastModifiedTime());

		log.debug("Audit details updated - Last modified by: {}", requestInfo.getUserInfo().getUuid());
	}

	/**
	 * Enriches BPA documents with unique IDs if not already present.
	 * 
	 * <p>
	 * Generates UUIDs for any documents that don't have IDs to ensure all documents
	 * can be uniquely identified and persisted in the database.
	 * 
	 * @param bpa the BPA entity containing documents
	 */
	private void enrichBPADocuments(BPA bpa) {
		if (!CollectionUtils.isEmpty(bpa.getDocuments())) {
			bpa.getDocuments().forEach(document -> {
				if (document.getId() == null) {
					document.setId(UUID.randomUUID().toString());
				}
			});
			log.debug("Enriched {} BPA documents with IDs", bpa.getDocuments().size());
		}
	}

	/**
	 * Enriches workflow verification documents with unique IDs if not already
	 * present.
	 * 
	 * <p>
	 * Generates UUIDs for any workflow verification documents that don't have IDs.
	 * These documents are typically attached during workflow state transitions
	 * (e.g., inspection reports, verification certificates).
	 * 
	 * @param bpa the BPA entity containing workflow with verification documents
	 */
	private void enrichWorkflowVerificationDocuments(BPA bpa) {
		if (bpa.getWorkflow() != null && !CollectionUtils.isEmpty(bpa.getWorkflow().getVarificationDocuments())) {
			bpa.getWorkflow().getVarificationDocuments().forEach(document -> {
				if (document.getId() == null) {
					document.setId(UUID.randomUUID().toString());
				}
			});
			log.debug("Enriched {} workflow verification documents with IDs",
					bpa.getWorkflow().getVarificationDocuments().size());
		}
	}

	/**
	 * Enriches Digital Signature Certificate (DSC) details based on workflow action
	 * and user roles.
	 * 
	 * <p>
	 * <strong>DSC Business Rules:</strong>
	 * <ul>
	 * <li><strong>APPROVE Action:</strong> When EMPLOYEE or BPA_ARC_APPROVER
	 * approves the application:
	 * <ul>
	 * <li>Creates DSC details for the BPA application (for permit letter
	 * signing)</li>
	 * <li>Creates DSC details for the building plan (for plan approval
	 * signing)</li>
	 * <li>Records the approver's UUID for audit trail</li>
	 * </ul>
	 * </li>
	 * <li><strong>REJECT Action:</strong> When EMPLOYEE or BPA_ARC_APPROVER rejects
	 * the application:
	 * <ul>
	 * <li>Creates DSC details for rejection record (for rejection letter
	 * signing)</li>
	 * <li>Records the rejector's UUID for audit trail</li>
	 * </ul>
	 * </li>
	 * <li><strong>Other Actions:</strong> Clears DSC details (not applicable for
	 * non-terminal actions)</li>
	 * </ul>
	 * 
	 * <p>
	 * <strong>Note:</strong> DSC integration is only applicable when the
	 * application has a valid status and the action is performed by authorized
	 * roles (EMPLOYEE or BPA_ARC_APPROVER).
	 * 
	 * @param bpaRequest the BPA request containing workflow action and user
	 *                   information
	 */
	private void enrichDscDetailsForUpdate(BPARequest bpaRequest) {
		BPA bpa = bpaRequest.getBPA();
		Workflow workflow = bpa.getWorkflow();
		RequestInfo requestInfo = bpaRequest.getRequestInfo();

		// Extract user roles for authorization check
		List<String> userRoles = extractUserRoles(requestInfo);

		// Check if user is authorized for DSC operations
		boolean isAuthorizedForDsc = isAuthorizedForDscOperation(userRoles);

		// Check if BPA has valid status
		boolean hasValidStatus = bpa.getStatus() != null;

		if (!hasValidStatus || !isAuthorizedForDsc) {
			// Clear DSC details if not authorized or status is invalid
			bpa.setDscDetails(null);
			log.debug("DSC details cleared - Not authorized or invalid status");
			return;
		}

		String workflowAction = workflow.getAction();

		if (workflowAction.equalsIgnoreCase("APPROVE")) {
			// Create DSC details for approval - both for BPA and Plan
			DscDetails dscDetails = createDscDetails(bpa.getTenantId(), requestInfo.getUserInfo().getUuid());

			bpa.setDscDetails(Arrays.asList(dscDetails));
			bpa.setPlanDscDetails(
					Arrays.asList(createDscDetails(bpa.getTenantId(), requestInfo.getUserInfo().getUuid())));

			log.info("DSC details created for APPROVE action by user: {}", requestInfo.getUserInfo().getUuid());

		} else if (workflowAction.equalsIgnoreCase("REJECT")) {
			// Create DSC details for rejection
			DscDetails dscDetails = createDscDetails(bpa.getTenantId(), requestInfo.getUserInfo().getUuid());

			bpa.setDscDetails(Arrays.asList(dscDetails));

			log.info("DSC details created for REJECT action by user: {}", requestInfo.getUserInfo().getUuid());

		} else {
			// Clear DSC details for other workflow actions
			bpa.setDscDetails(null);
			log.debug("DSC details cleared for workflow action: {}", workflowAction);
		}
	}

	/**
	 * Extracts user role codes from request info.
	 * 
	 * @param requestInfo the request information containing user details
	 * @return list of role codes assigned to the user
	 */
	private List<String> extractUserRoles(RequestInfo requestInfo) {
		return requestInfo.getUserInfo().getRoles().stream().map(role -> role.getCode()).collect(Collectors.toList());
	}

	/**
	 * Checks if user has authorized roles for DSC operations.
	 * 
	 * <p>
	 * Only EMPLOYEE and BPA_ARC_APPROVER roles are authorized to perform DSC
	 * operations (digital signing of permits, plans, and rejection letters).
	 * 
	 * @param userRoles list of user role codes
	 * @return true if user has EMPLOYEE or BPA_ARC_APPROVER role, false otherwise
	 */
	private boolean isAuthorizedForDscOperation(List<String> userRoles) {
		return userRoles.contains("EMPLOYEE") || userRoles.contains("BPA_ARC_APPROVER");
	}

	/**
	 * Creates a new DSC details entity with generated UUID.
	 * 
	 * <p>
	 * DSC (Digital Signature Certificate) details are used to track digital
	 * signatures applied to permits, plans, and rejection letters for audit and
	 * legal compliance purposes.
	 * 
	 * @param tenantId     the tenant ID for multi-tenancy support
	 * @param approverUuid the UUID of the user performing the signing action
	 * @return newly created DSC details entity
	 */
	private DscDetails createDscDetails(String tenantId, String approverUuid) {
		DscDetails dscDetails = new DscDetails();
		dscDetails.setTenantId(tenantId);
		dscDetails.setId(UUID.randomUUID().toString());
		dscDetails.setApprovedBy(approverUuid);
		return dscDetails;
	}

	/**
	 * postStatus encrichment to update the status of the workflow to the
	 * application and generating permit and oc number when applicable
	 * 
	 * @param bpaRequest
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	/**
	 * Performs post-status enrichment after workflow state transitions.
	 * 
	 * <p>
	 * This method orchestrates multiple enrichment operations that occur after a
	 * workflow action changes the BPA application status:
	 * <ul>
	 * <li>Sets application date when reaching specific workflow states</li>
	 * <li>Determines and assigns risk type if not already set</li>
	 * <li>Generates approval/permit numbers upon application approval</li>
	 * <li>Initiates NOC (No Objection Certificate) workflows when applicable</li>
	 * </ul>
	 * 
	 * <p>
	 * <strong>Workflow Integration:</strong> This method is called after workflow
	 * processing to enrich the application with state-specific data and trigger
	 * downstream processes (NOC, payments, etc.).
	 * 
	 * <p>
	 * <strong>Business Flow:</strong>
	 * <ol>
	 * <li>Retrieve workflow business service and current state</li>
	 * <li>Enrich application date based on workflow state</li>
	 * <li>Determine and set risk type if missing</li>
	 * <li>Generate approval numbers for approved applications</li>
	 * <li>Initiate NOC workflow if required</li>
	 * </ol>
	 * 
	 * @param bpaRequest the BPA request containing application details and workflow
	 *                   context
	 */
	public void postStatusEnrichment(BPARequest bpaRequest) {
		BPA bpa = bpaRequest.getBPA();

		log.info("Starting post-status enrichment for application: {}, status: {}", bpa.getApplicationNo(),
				bpa.getStatus());

		// Step 1: Retrieve MDMS data and workflow business service
		String tenantId = extractBaseTenantId(bpa.getTenantId());
		Object mdmsData = util.mDMSCall(bpaRequest.getRequestInfo(), tenantId);

		BusinessService businessService = workflowService.getBusinessService(bpa, bpaRequest.getRequestInfo(),
				bpa.getApplicationNo());
		String currentState = workflowService.getCurrentState(bpa.getStatus(), businessService);

		log.debug("Current workflow state: {}", currentState);

		// Step 2: Enrich application date based on workflow state
		enrichApplicationDate(bpa, currentState);

		// Step 3: Determine and set risk type if not already set
		enrichRiskType(bpa);

		// Step 4: Generate approval number for approved applications
		generateApprovalNo(bpaRequest, currentState);

		// Step 5: Initiate NOC workflow if applicable
		initiateNocIfRequired(bpaRequest, mdmsData);

		log.info("Post-status enrichment completed for application: {}", bpa.getApplicationNo());
	}

	/**
	 * Extracts the base tenant ID by removing any sub-tenant information.
	 * 
	 * <p>
	 * For example, "od.cuttack" becomes "od". This is required for MDMS calls which
	 * need the state-level tenant ID.
	 * 
	 * @param fullTenantId the complete tenant ID (may include city/ULB)
	 * @return base tenant ID (state level)
	 */
	private String extractBaseTenantId(String fullTenantId) {
		return fullTenantId.split("\\.")[0];
	}

	/**
	 * Enriches application date based on workflow state transitions.
	 * 
	 * <p>
	 * <strong>Business Rules for Application Date:</strong>
	 * <ul>
	 * <li><strong>Document Verification State:</strong> Set application date when
	 * entering document verification stage (if not already set)</li>
	 * <li><strong>Accredited Person (AC) Module:</strong> Set application date when
	 * status becomes APPROVAL_INPROGRESS for BPA_AC applications (if not already
	 * set)</li>
	 * </ul>
	 * 
	 * <p>
	 * The application date marks the formal submission timestamp and is used for:
	 * <ul>
	 * <li>SLA (Service Level Agreement) calculations</li>
	 * <li>Validity period computations</li>
	 * <li>Audit and reporting purposes</li>
	 * </ul>
	 * 
	 * @param bpa          the BPA application to enrich
	 * @param currentState the current workflow state
	 */
	private void enrichApplicationDate(BPA bpa, String currentState) {
		// Check if application date is already set
		boolean isApplicationDateMissing = (bpa.getApplicationDate() == null || bpa.getApplicationDate() == 0);

		if (!isApplicationDateMissing) {
			log.debug("Application date already set: {}", bpa.getApplicationDate());
			return;
		}

		// Rule 1: Set application date when entering document verification state
		if (currentState.equalsIgnoreCase(BPAConstants.DOCVERIFICATION_STATE)) {
			bpa.setApplicationDate(Calendar.getInstance().getTimeInMillis());
			log.info("Application date set for DOCVERIFICATION_STATE: application: {}", bpa.getApplicationNo());
			return;
		}

		// Rule 2: Set application date for Accredited Person (AC) module when approval
		// is in progress
		boolean isAccreditedPersonModule = bpa.getBusinessService().equalsIgnoreCase(BPAConstants.BPA_AC_MODULE_CODE);
		boolean isApprovalInProgress = bpa.getStatus().equalsIgnoreCase(BPAConstants.APPROVAL_INPROGRESS);

		if (isAccreditedPersonModule && isApprovalInProgress) {
			bpa.setApplicationDate(Calendar.getInstance().getTimeInMillis());
			log.info("Application date set for BPA_AC APPROVAL_INPROGRESS: application: {}", bpa.getApplicationNo());
		}
	}

	/**
	 * Enriches risk type for the BPA application if not already set.
	 * 
	 * <p>
	 * <strong>Risk Type Classification:</strong>
	 * <ul>
	 * <li><strong>LOW Risk:</strong> For applications using BPA_LOW business
	 * service (simplified approval process for low-risk constructions)</li>
	 * <li><strong>OTHER Risk:</strong> For all other business services (standard or
	 * high-risk constructions requiring detailed scrutiny)</li>
	 * </ul>
	 * 
	 * <p>
	 * Risk type determines:
	 * <ul>
	 * <li>Workflow complexity (number of approval stages)</li>
	 * <li>Scrutiny requirements (detailed vs. simplified)</li>
	 * <li>Fee structure and timelines</li>
	 * <li>Approval conditions and compliance checks</li>
	 * </ul>
	 * 
	 * @param bpa the BPA application to enrich
	 */
	private void enrichRiskType(BPA bpa) {
		if (StringUtils.isEmpty(bpa.getRiskType())) {
			if (bpa.getBusinessService().equals(BPAConstants.BPA_LOW_MODULE_CODE)) {
				bpa.setRiskType(BPAConstants.LOW_RISKTYPE);
				log.debug("Risk type set to LOW for business service: {}", BPAConstants.BPA_LOW_MODULE_CODE);
			} else {
				bpa.setRiskType(BPAConstants.OTHER_RISKTYPE);
				log.debug("Risk type set to OTHER for business service: {}", bpa.getBusinessService());
			}
		} else {
			log.debug("Risk type already set: {}", bpa.getRiskType());
		}
	}

	/**
	 * Initiates NOC (No Objection Certificate) workflow if required.
	 * 
	 * <p>
	 * <strong>NOC Workflow Initiation Rules:</strong>
	 * <ul>
	 * <li><strong>Skip for OC Outside Sujog:</strong> If occupancy certificate is
	 * outside Sujog application, NOC workflow is not initiated</li>
	 * <li><strong>Skip for Revalidation:</strong> Revalidation applications don't
	 * require new NOC workflow (use existing NOCs)</li>
	 * <li><strong>Initiate for New Applications:</strong> All other applications
	 * trigger NOC workflows for Fire, Airport, Health, etc. as per MDMS
	 * configuration</li>
	 * </ul>
	 * 
	 * <p>
	 * <strong>NOC Types (configured in MDMS):</strong>
	 * <ul>
	 * <li>Fire NOC - for fire safety clearance</li>
	 * <li>Airport NOC - for constructions near airports</li>
	 * <li>Health NOC - for health and sanitation clearance</li>
	 * <li>Other NOCs as per local regulations</li>
	 * </ul>
	 * 
	 * @param bpaRequest the BPA request containing application details
	 * @param mdmsData   the master data containing NOC configurations
	 */
	private void initiateNocIfRequired(BPARequest bpaRequest, Object mdmsData) {
		BPA bpa = bpaRequest.getBPA();

		// Skip NOC for OC outside Sujog application
		if (bpa.getOCOutsideSujogApplication()) {
			log.debug("Skipping NOC workflow - OC Outside Sujog Application: {}", bpa.getApplicationNo());
			return;
		}

		// Skip NOC for revalidation applications
		if (bpa.isRevalidationApplication()) {
			log.debug("Skipping NOC workflow - Revalidation Application: {}", bpa.getApplicationNo());
			return;
		}

		// Initiate NOC workflow for eligible applications
		log.info("Initiating NOC workflow for application: {}", bpa.getApplicationNo());
		nocService.initiateNocWorkflow(bpaRequest, mdmsData);
	}

	/**
	 * Generates permit/approval number for approved BPA applications.
	 * 
	 * <p>
	 * This method is invoked when a BPA application reaches the APPROVED state and
	 * orchestrates the following operations:
	 * <ul>
	 * <li>Validates if approval number generation is applicable</li>
	 * <li>Generates unique permit/OC number using IdGen service</li>
	 * <li>Retrieves or fetches EDCR (Electronic Design and Calculation Report)
	 * details</li>
	 * <li>Enriches approval conditions from MDMS based on risk type and service
	 * type</li>
	 * </ul>
	 * 
	 * <p>
	 * <strong>Business Rules:</strong>
	 * <ul>
	 * <li><strong>Revalidation Applications:</strong> Skip permit number generation
	 * (use existing permit)</li>
	 * <li><strong>OC Applications:</strong> Generate OC number when status is
	 * APPROVED</li>
	 * <li><strong>BPA Applications:</strong> Generate permit number when state is
	 * APPROVED</li>
	 * </ul>
	 * 
	 * <p>
	 * <strong>Approval Number Format:</strong> Generated using IdGen service with
	 * configured format (e.g., PB-BPA-2024-01-000123)
	 * 
	 * <p>
	 * <strong>Note:</strong> Approval date is set during digital signing of permit
	 * letter, not during this enrichment step.
	 * 
	 * @param bpaRequest the BPA request containing application details
	 * @param state      the current workflow state
	 */
	private void generateApprovalNo(BPARequest bpaRequest, String state) {
		BPA bpa = bpaRequest.getBPA();

		// Skip permit number generation for revalidation applications
		if (bpa != null && bpa.isRevalidationApplication()) {
			log.info("Skipping permit number generation for Revalidation application: {}", bpa.getApplicationNo());
			return;
		}

		// Check if application is approved
		if (!isApplicationApproved(bpa, state)) {
			log.debug("Application not in approved state - skipping approval number generation");
			return;
		}

		log.info("Generating approval number for approved application: {}", bpa.getApplicationNo());

		// Step 1: Ensure additional details map is initialized
		ensureAdditionalDetailsInitialized(bpa);

		// Step 2: Generate permit/OC number
		generatePermitNumber(bpaRequest, bpa);

		// Step 3: Retrieve or fetch EDCR response
		Map<String, String> edcrResponse = retrieveOrFetchEdcrResponse(bpaRequest);

		// Step 4: Enrich approval conditions from MDMS
		enrichApprovalConditions(bpaRequest, bpa, edcrResponse);

		log.info("Approval number generated successfully: {} for application: {}", bpa.getApprovalNo(),
				bpa.getApplicationNo());
	}

	/**
	 * Checks if the BPA application is in approved state.
	 * 
	 * <p>
	 * <strong>Approval State Rules:</strong>
	 * <ul>
	 * <li><strong>OC Applications (BPA_OC):</strong> Check if status equals
	 * APPROVED_STATE</li>
	 * <li><strong>BPA Applications:</strong> Check if workflow state equals
	 * APPROVED_STATE</li>
	 * </ul>
	 * 
	 * @param bpa   the BPA application
	 * @param state the current workflow state
	 * @return true if application is approved, false otherwise
	 */
	private boolean isApplicationApproved(BPA bpa, String state) {
		// For OC applications, check the status
		if (bpa.getBusinessService().equalsIgnoreCase(BPAConstants.BPA_OC_MODULE_CODE)) {
			return bpa.getStatus().equalsIgnoreCase(BPAConstants.APPROVED_STATE);
		}

		// For BPA applications, check the workflow state
		return state.equalsIgnoreCase(BPAConstants.APPROVED_STATE);
	}

	/**
	 * Ensures that the additional details map is initialized.
	 * 
	 * <p>
	 * Additional details store supplementary information like:
	 * <ul>
	 * <li>Validity date for the permit</li>
	 * <li>Approval conditions</li>
	 * <li>Custom attributes specific to the application</li>
	 * </ul>
	 * 
	 * @param bpa the BPA application
	 */
	private void ensureAdditionalDetailsInitialized(BPA bpa) {
		if (bpa.getAdditionalDetails() == null) {
			bpa.setAdditionalDetails(new HashMap<String, Object>());
			log.debug("Initialized additional details map for application: {}", bpa.getApplicationNo());
		}
	}

	/**
	 * Generates unique permit/OC number using IdGen service.
	 * 
	 * <p>
	 * The permit number format is configured in application properties:
	 * <ul>
	 * <li><strong>Format Example:</strong> PB-BPA-2024-01-000123</li>
	 * <li><strong>Components:</strong> State-Module-Year-Month-Sequence</li>
	 * </ul>
	 * 
	 * <p>
	 * <strong>Note:</strong> The approval date will be set during the digital
	 * signing of the permit letter, not at this stage. The validity period
	 * calculation is also deferred to signing time.
	 * 
	 * @param bpaRequest the BPA request with request info
	 * @param bpa        the BPA application to enrich with permit number
	 */
	private void generatePermitNumber(BPARequest bpaRequest, BPA bpa) {
		// Generate permit/OC number using IdGen service
		List<IdResponse> idResponses = idGenRepository.getId(bpaRequest.getRequestInfo(), bpa.getTenantId(),
				config.getPermitNoIdgenName(), config.getPermitNoIdgenFormat(), 1).getIdResponses();

		String permitNumber = idResponses.get(0).getId();
		bpa.setApprovalNo(permitNumber);

		log.info("Permit number generated: {} for application: {}", permitNumber, bpa.getApplicationNo());

		// Note: Approval date and validity date are set during digital signing of
		// permit letter
		// This is handled separately in the DSC (Digital Signature Certificate) flow
	}

	/**
	 * Retrieves existing EDCR response or fetches it from EDCR service.
	 * 
	 * <p>
	 * <strong>EDCR Retrieval Strategy:</strong>
	 * <ol>
	 * <li><strong>OC Outside Sujog:</strong> Use default values for OC plan
	 * scrutiny</li>
	 * <li><strong>Pre-approved Plans (PAP):</strong> Get details from pre-approved
	 * plan service</li>
	 * <li><strong>Cached Response:</strong> Use EDCR response if already available
	 * in request</li>
	 * <li><strong>Fresh Fetch:</strong> Call EDCR service to get plan scrutiny
	 * details</li>
	 * </ol>
	 * 
	 * <p>
	 * <strong>EDCR Response Contains:</strong>
	 * <ul>
	 * <li>Application Type (Building Plan, OC Plan, etc.)</li>
	 * <li>Service Type (New Construction, Addition/Alteration, etc.)</li>
	 * <li>Plot details, building heights, setbacks, FAR calculations</li>
	 * <li>Compliance status with building bylaws</li>
	 * </ul>
	 * 
	 * @param bpaRequest the BPA request containing application and request info
	 * @return map containing EDCR response with application type and service type
	 */
	private Map<String, String> retrieveOrFetchEdcrResponse(BPARequest bpaRequest) {
		BPA bpa = bpaRequest.getBPA();
		Map<String, String> edcrResponse = new HashMap<>();
		String businessService = bpa.getBusinessService();

		log.debug("Retrieving EDCR response for application: {}", bpa.getApplicationNo());

		// Strategy 1: OC Outside Sujog - use default values
		if (bpa.getOCOutsideSujogApplication()) {
			edcrResponse.put(BPAConstants.APPLICATIONTYPE, "BUILDING_OC_PLAN_SCRUTINY");
			edcrResponse.put(BPAConstants.SERVICETYPE, "NEW_CONSTRUCTION");
			log.debug("Using default EDCR values for OC Outside Sujog application");
			return edcrResponse;
		}

		// Strategy 2: Pre-approved Plan (PAP) - get from PAP service
		if (!StringUtils.isEmpty(businessService)
				&& businessService.equalsIgnoreCase(BPAConstants.BPA_PAP_MODULE_CODE)) {
			edcrResponse = edcrService.getEdcrDetailsForPreapprovedPlan(edcrResponse, bpaRequest);
			log.debug("Retrieved EDCR details from Pre-approved Plan service");
			return edcrResponse;
		}

		// Strategy 3: Use cached EDCR response if available
		if (bpaRequest.getEdcrResponse() != null && !CollectionUtils.isEmpty(bpaRequest.getEdcrResponse())) {
			edcrResponse = bpaRequest.getEdcrResponse();
			log.debug("Using cached EDCR response");
			return edcrResponse;
		}

		// Strategy 4: Fetch fresh EDCR response from service
		log.info("Fetching EDCR details from EDCR service for application: {}", bpa.getApplicationNo());
		edcrResponse = edcrService.getEDCRDetails(bpaRequest.getRequestInfo(), bpa);
		bpaRequest.setEdcrResponse(edcrResponse);

		log.info("EDCR response retrieved - Application Type: {}, Service Type: {}",
				edcrResponse.get(BPAConstants.APPLICATIONTYPE), edcrResponse.get(BPAConstants.SERVICETYPE));

		return edcrResponse;
	}

	/**
	 * Enriches approval conditions from MDMS based on application characteristics.
	 * 
	 * <p>
	 * <strong>Approval Conditions:</strong> Conditions are fetched from MDMS based
	 * on a combination of:
	 * <ul>
	 * <li><strong>Workflow State:</strong> PENDING_APPROVAL_STATE</li>
	 * <li><strong>Risk Type:</strong> LOW, MEDIUM, HIGH</li>
	 * <li><strong>Service Type:</strong> New Construction, Addition/Alteration,
	 * etc.</li>
	 * <li><strong>Application Type:</strong> Building Plan, OC Plan, etc.</li>
	 * </ul>
	 * 
	 * <p>
	 * <strong>Conditions Examples:</strong>
	 * <ul>
	 * <li>"Construction shall be as per approved plan"</li>
	 * <li>"Fire safety measures shall be implemented"</li>
	 * <li>"Building completion certificate to be obtained within 3 years"</li>
	 * <li>"Regular inspections by municipal authorities"</li>
	 * </ul>
	 * 
	 * <p>
	 * If no conditions are found in MDMS, logs a warning but continues processing.
	 * 
	 * @param bpaRequest   the BPA request with request info for MDMS call
	 * @param bpa          the BPA application to enrich with conditions
	 * @param edcrResponse the EDCR response containing application and service type
	 */
	private void enrichApprovalConditions(BPARequest bpaRequest, BPA bpa, Map<String, String> edcrResponse) {
		// Fetch MDMS data for approval conditions
		Object mdmsData = bpaUtil.mDMSCall(bpaRequest.getRequestInfo(), bpa.getTenantId());

		// Build JsonPath to retrieve conditions from MDMS
		String conditionsPath = BPAConstants.CONDITIONS_MAP.replace("{1}", BPAConstants.PENDING_APPROVAL_STATE)
				.replace("{2}", bpa.getRiskType().toString()).replace("{3}", edcrResponse.get(BPAConstants.SERVICETYPE))
				.replace("{4}", edcrResponse.get(BPAConstants.APPLICATIONTYPE));

		log.debug("Fetching approval conditions using path: {}", conditionsPath);

		try {
			// Retrieve conditions from MDMS
			List<String> conditions = (List<String>) JsonPath.read(mdmsData, conditionsPath);

			if (conditions != null && !conditions.isEmpty()) {
				// Ensure additional details is initialized
				if (bpa.getAdditionalDetails() == null) {
					bpa.setAdditionalDetails(new HashMap<String, Object>());
				}

				// Add conditions to additional details
				@SuppressWarnings("unchecked")
				Map<String, Object> additionalDetails = (Map<String, Object>) bpa.getAdditionalDetails();
				additionalDetails.put(BPAConstants.PENDING_APPROVAL_STATE.toLowerCase(), conditions.get(0));

				log.info("Approval conditions enriched for application: {}", bpa.getApplicationNo());
				log.debug("Conditions: {}", conditions.get(0));
			} else {
				log.warn("No approval conditions found in MDMS for application: {}", bpa.getApplicationNo());
			}

		} catch (Exception e) {
			log.warn("Failed to fetch approval conditions for application: {} - Error: {}", bpa.getApplicationNo(),
					e.getMessage());
			// Continue processing even if conditions are not found
			// Conditions are informational and not critical for approval
		}
	}

	/**
	 * handles the skippayment of the BPA when demand is zero
	 * 
	 * @param bpaRequest
	 */
	public void skipPayment(BPARequest bpaRequest) {
		BPA bpa = bpaRequest.getBPA();
		BigDecimal demandAmount = bpaUtil.getDemandAmount(bpaRequest);
		if (!(demandAmount.compareTo(BigDecimal.ZERO) > 0)) {
			Workflow workflow = Workflow.builder().action(BPAConstants.ACTION_SKIP_PAY).build();
			bpa.setWorkflow(workflow);
			wfIntegrator.callWorkFlow(bpaRequest);
		}
	}

	/**
	 * Enriches BPA create request V2 with audit details, business service, land ID,
	 * documents, and application number.
	 * 
	 * <p>
	 * This is the V2 version of BPA create enrichment that integrates with EDCR
	 * (Electronic Design and Calculation Report) to automatically determine the
	 * appropriate business service based on building characteristics extracted from
	 * plan scrutiny.
	 * 
	 * <p>
	 * <strong>Key Enhancements over V1:</strong>
	 * <ul>
	 * <li>EDCR Integration - Extracts business service recommendations from EDCR
	 * response</li>
	 * <li>Dynamic Business Service Assignment - Automatically determines BPA,
	 * BPA_LOW, BPA_OC, etc.</li>
	 * <li>Validation - Ensures user-selected business service matches EDCR
	 * suggestions</li>
	 * <li>OC Outside Sujog Support - Special handling for occupancy certificates
	 * outside Sujog platform</li>
	 * </ul>
	 * 
	 * <p>
	 * <strong>Enrichment Workflow:</strong>
	 * <ol>
	 * <li>Add audit details (created by, created time) and generate UUID</li>
	 * <li>Set account ID to the creator's UUID</li>
	 * <li>Determine and validate business service based on application type and
	 * EDCR</li>
	 * <li>Link land information (land ID) from EDCR values or land info object</li>
	 * <li>Generate document IDs for all uploaded documents</li>
	 * <li>Generate application number via IdGen service</li>
	 * </ol>
	 * 
	 * <p>
	 * <strong>Application Type Handling:</strong>
	 * <ul>
	 * <li><strong>Building Plan:</strong> Business service determined from EDCR
	 * (BPA/BPA_LOW/BPA_AC/BPA_PAP)</li>
	 * <li><strong>Occupancy Certificate:</strong>
	 * <ul>
	 * <li>If outside Sujog → Business service from EDCR, land ID from values</li>
	 * <li>If within Sujog → Skip business service population, land ID from
	 * values</li>
	 * </ul>
	 * </li>
	 * </ul>
	 * 
	 * @param bpaRequest the BPA request to enrich with all required details
	 * @param mdmsData   the master data from MDMS (reserved for future use)
	 * @param values     map containing extracted EDCR values (applicationType,
	 *                   landId, etc.)
	 * @param edcr       the EDCR response containing building plan scrutiny details
	 *                   and business service recommendations
	 */
	public void enrichBPACreateRequestV2(BPARequest bpaRequest, Object mdmsData, Map<String, String> values,
			LinkedHashMap<String, Object> edcr) {

		log.info("Starting BPA create enrichment V2 for tenant: {}", bpaRequest.getBPA().getTenantId());

		// Step 1: Enrich audit details and base identifiers
		enrichCreateAuditDetailsAndIds(bpaRequest);

		// Step 2: Determine and set business service based on application type
		String applicationType = values.get(BPAConstants.APPLICATIONTYPE);
		log.info("Application type from EDCR: {}", applicationType);
		determineAndSetBusinessServiceV2(bpaRequest, applicationType, values, edcr);

		// Step 3: Enrich land ID information
		enrichLandIdInformation(bpaRequest);

		// Step 4: Generate document IDs
		enrichBPADocumentsV2(bpaRequest);

		// Step 5: Generate application number
		setIdgenIds(bpaRequest);

		log.info("BPA create enrichment V2 completed - Business Service: {}, Application No: {}",
				bpaRequest.getBPA().getBusinessService(), bpaRequest.getBPA().getApplicationNo());
	}

	/**
	 * Enriches audit details and base identifiers for BPA create request.
	 * 
	 * <p>
	 * This method:
	 * <ul>
	 * <li>Generates unique UUID for the BPA entity</li>
	 * <li>Creates audit details with creator information and creation
	 * timestamp</li>
	 * <li>Sets account ID to the creator's UUID (for ownership and access
	 * control)</li>
	 * </ul>
	 * 
	 * <p>
	 * <strong>Account ID:</strong> Links the BPA application to the
	 * architect/consultant who created it, enabling them to view and update the
	 * application.
	 * 
	 * @param bpaRequest the BPA request containing request info and BPA entity
	 */
	private void enrichCreateAuditDetailsAndIds(BPARequest bpaRequest) {
		RequestInfo requestInfo = bpaRequest.getRequestInfo();
		BPA bpa = bpaRequest.getBPA();

		// Generate audit details for creation
		AuditDetails auditDetails = bpaUtil.getAuditDetails(requestInfo.getUserInfo().getUuid(), true);

		// Set BPA identifiers and audit
		bpa.setAuditDetails(auditDetails);
		bpa.setId(UUID.randomUUID().toString());
		bpa.setAccountId(auditDetails.getCreatedBy());

		log.debug("Audit details and IDs enriched - BPA ID: {}, Account ID: {}", bpa.getId(), bpa.getAccountId());
	}

	/**
	 * Determines and sets the appropriate business service based on application
	 * type and EDCR recommendations.
	 * 
	 * <p>
	 * <strong>Business Service Determination Strategy:</strong>
	 * <ul>
	 * <li><strong>Building Plan Applications:</strong>
	 * <ul>
	 * <li>Extract business service recommendations from EDCR (can be multiple
	 * options)</li>
	 * <li>If user provided business service → Validate it's in EDCR suggested
	 * list</li>
	 * <li>If user didn't provide → Automatically use first EDCR suggestion</li>
	 * <li>Possible values: BPA, BPA_LOW, BPA_AC, BPA_PAP</li>
	 * </ul>
	 * </li>
	 * <li><strong>Occupancy Certificate Applications:</strong>
	 * <ul>
	 * <li>If OC Outside Sujog → Populate business service from EDCR and set land
	 * ID</li>
	 * <li>If OC Within Sujog → Skip business service population, only set land
	 * ID</li>
	 * <li>Business service: BPA_OC</li>
	 * </ul>
	 * </li>
	 * </ul>
	 * 
	 * <p>
	 * <strong>EDCR Business Service Format:</strong> EDCR can suggest multiple
	 * business services separated by "|" (e.g., "BPA|BPA_LOW") allowing users to
	 * choose the most appropriate workflow for their application.
	 * 
	 * <p>
	 * <strong>Validation:</strong> If user selects a business service not suggested
	 * by EDCR, an exception is thrown to prevent workflow mismatches and ensure
	 * compliance with building regulations.
	 * 
	 * @param bpaRequest      the BPA request to enrich
	 * @param applicationType the application type from EDCR (BUILDING_PLAN or
	 *                        BUILDING_OC_PLAN_SCRUTINY)
	 * @param values          map containing EDCR extracted values including land ID
	 * @param edcr            the EDCR response containing business service
	 *                        recommendations
	 * @throws CustomException if user-selected business service is not in EDCR
	 *                         suggested list
	 */
	private void determineAndSetBusinessServiceV2(BPARequest bpaRequest, String applicationType,
			Map<String, String> values, LinkedHashMap<String, Object> edcr) {

		BPA bpa = bpaRequest.getBPA();

		if (applicationType.equalsIgnoreCase(BPAConstants.BUILDING_PLAN)) {
			// Building Plan - determine business service from EDCR
			populateBusinessService(bpaRequest, edcr);
			log.debug("Building Plan - Business service populated from EDCR: {}", bpa.getBusinessService());

		} else {
			// Occupancy Certificate
			if (bpa.getOCOutsideSujogApplication()) {
				// OC outside Sujog - skip business service population
				log.debug("OC within Sujog - Business service population skipped");
			} else {
				// OC within Sujog - populate business service from EDCR
				populateBusinessService(bpaRequest, edcr);
				log.debug("OC outside Sujog - Business service populated from EDCR: {}", bpa.getBusinessService());
			}

			// Set land ID from EDCR values for OC applications
			bpa.setLandId(values.get("landId"));
			log.debug("Land ID set from EDCR values: {}", bpa.getLandId());
		}
	}

	/**
	 * Enriches land ID information for the BPA application.
	 * 
	 * <p>
	 * If land information object is provided in the request, extracts and sets the
	 * land ID. This overrides any land ID set from EDCR values.
	 * 
	 * <p>
	 * <strong>Land ID Precedence:</strong>
	 * <ol>
	 * <li>Land ID from land info object (if provided) - takes precedence</li>
	 * <li>Land ID from EDCR values (set earlier for OC applications)</li>
	 * </ol>
	 * 
	 * <p>
	 * <strong>Land Info Object:</strong> Contains detailed land parcel information
	 * including owners, plot area, address, and other land registry details.
	 * 
	 * @param bpaRequest the BPA request with land information
	 */
	private void enrichLandIdInformation(BPARequest bpaRequest) {
		BPA bpa = bpaRequest.getBPA();

		if (bpa.getLandInfo() != null) {
			// Override with land ID from land info object
			bpa.setLandId(bpa.getLandInfo().getId());
			log.debug("Land ID set from land info object: {}", bpa.getLandId());
		} else if (bpa.getLandId() != null) {
			log.debug("Land ID already set from EDCR values: {}", bpa.getLandId());
		} else {
			log.debug("No land information available for application");
		}
	}

	/**
	 * Enriches document IDs for all documents in the BPA application (V2).
	 * 
	 * <p>
	 * Generates unique UUIDs for any documents that don't already have IDs. This
	 * ensures all uploaded documents can be uniquely identified and persisted.
	 * 
	 * <p>
	 * <strong>Document Types:</strong>
	 * <ul>
	 * <li>Building plan drawings (architectural, structural, etc.)</li>
	 * <li>Land ownership documents</li>
	 * <li>NOC documents (fire, health, airport, etc.)</li>
	 * <li>Professional certificates (architect, engineer)</li>
	 * <li>Other supporting documents as per regulations</li>
	 * </ul>
	 * 
	 * @param bpaRequest the BPA request containing documents
	 */
	private void enrichBPADocumentsV2(BPARequest bpaRequest) {
		BPA bpa = bpaRequest.getBPA();

		if (!CollectionUtils.isEmpty(bpa.getDocuments())) {
			bpa.getDocuments().forEach(document -> {
				if (document.getId() == null) {
					document.setId(UUID.randomUUID().toString());
				}
			});
			log.debug("Enriched {} documents with IDs", bpa.getDocuments().size());
		} else {
			log.debug("No documents to enrich for application");
		}
	}

	/**
	 * enrich create Preapprovedplan Request by adding auditdetails and uuids
	 * 
	 * @param request
	 */
	public void enrichPreapprovedPlanCreateRequestV2(PreapprovedPlanRequest request) {
		log.info(" Inside enrichPreapprovedPlanCreateRequestV2 ");
		RequestInfo requestInfo = request.getRequestInfo();
		AuditDetails auditDetails = bpaUtil.getAuditDetails(requestInfo.getUserInfo().getUuid(), true);
		request.getPreapprovedPlan().setAuditDetails(auditDetails);
		request.getPreapprovedPlan().setId(UUID.randomUUID().toString());

		// Documents-
		if (!CollectionUtils.isEmpty(request.getPreapprovedPlan().getDocuments()))
			request.getPreapprovedPlan().getDocuments().forEach(document -> {
				if (document.getId() == null) {
					document.setId(UUID.randomUUID().toString());
				}
			});
		setIdgenIdsForPreapprovedPlan(request);
	}

	/**
	 * enrich create Revision Request by adding auditdetails and uuids
	 * 
	 * @param request
	 */
	public void enrichRevisionCreateRequest(RevisionRequest request) {
		log.info(" Inside enrichRevisionCreateRequest ");
		RequestInfo requestInfo = request.getRequestInfo();
		AuditDetails auditDetails = bpaUtil.getAuditDetails(requestInfo.getUserInfo().getUuid(), true);
		request.getRevision().setAuditDetails(auditDetails);
		request.getRevision().setId(UUID.randomUUID().toString());

		// Documents-
		if (!CollectionUtils.isEmpty(request.getRevision().getDocuments()))
			request.getRevision().getDocuments().forEach(document -> {
				if (document.getId() == null) {
					document.setId(UUID.randomUUID().toString());
				}
			});
	}

	/**
	 * enrich create Accredited Person Request by adding auditdetails and uuids
	 * 
	 * @param request
	 */
	public void enrichAccreditedPersonCreateRequest(AccreditedPersonRequest request) {
		log.info(" Inside enrichAccreditedPersonCreateRequest ");
		UserDetailResponse userDetail = userService.getUserByUUID(request.getAccreditedPerson().getUserUUID(),
				request.getRequestInfo());

		if (ObjectUtils.isEmpty(userDetail) || CollectionUtils.isEmpty(userDetail.getUser())) {
			throw new CustomException("ID ERROR", "Employee id is not present");
		}

		RequestInfo requestInfo = request.getRequestInfo();
		AuditDetails auditDetails = bpaUtil.getAuditDetails(requestInfo.getUserInfo().getUuid(), true);
		request.getAccreditedPerson().setTenant(BPAConstants.TENANT_OD);
		request.getAccreditedPerson().setUserRole(BPAConstants.ACCREDITED_PERSON_ROLE);
		request.getAccreditedPerson().setAuditDetails(auditDetails);
		request.getAccreditedPerson().setId(UUID.randomUUID().toString());
	}

	/**
	 * enchrich the updateRequest
	 * 
	 * @param preapprovedPlanRequest
	 */
	public void enrichPreapprovedPlanUpdateRequest(PreapprovedPlanRequest preapprovedPlanRequest) {

		RequestInfo requestInfo = preapprovedPlanRequest.getRequestInfo();
		AuditDetails auditDetails = bpaUtil.getAuditDetails(requestInfo.getUserInfo().getUuid(), false);
		auditDetails.setCreatedBy(preapprovedPlanRequest.getPreapprovedPlan().getAuditDetails().getCreatedBy());
		auditDetails.setCreatedTime(preapprovedPlanRequest.getPreapprovedPlan().getAuditDetails().getCreatedTime());
		preapprovedPlanRequest.getPreapprovedPlan().getAuditDetails()
				.setLastModifiedTime(auditDetails.getLastModifiedTime());
	}

	/**
	 * enchrich the updateRequest
	 * 
	 * @param revisionRequest
	 */
	public void enrichRevisionUpdateRequest(RevisionRequest revisionRequest) {

		RequestInfo requestInfo = revisionRequest.getRequestInfo();
		AuditDetails auditDetails = bpaUtil.getAuditDetails(requestInfo.getUserInfo().getUuid(), false);
		auditDetails.setCreatedBy(revisionRequest.getRevision().getAuditDetails().getCreatedBy());
		auditDetails.setCreatedTime(revisionRequest.getRevision().getAuditDetails().getCreatedTime());
		revisionRequest.getRevision().getAuditDetails().setLastModifiedTime(auditDetails.getLastModifiedTime());
	}

	/**
	 * Sets the ApplicationNumber for given preapprovedPlanRequest
	 *
	 * @param preapprovedPlanRequest which is to be created
	 */
	private void setIdgenIdsForPreapprovedPlan(PreapprovedPlanRequest preapprovedPlanRequest) {
		RequestInfo requestInfo = preapprovedPlanRequest.getRequestInfo();
		String tenantId = preapprovedPlanRequest.getPreapprovedPlan().getTenantId();

		List<String> applicationNumbers = getIdList(requestInfo, tenantId, config.getDrawingNoIdGenName(),
				config.getDrawingNoIdGenFormat(), 1);
		ListIterator<String> itr = applicationNumbers.listIterator();

		Map<String, String> errorMap = new HashMap<>();

		if (!errorMap.isEmpty())
			throw new CustomException(errorMap);

		preapprovedPlanRequest.getPreapprovedPlan().setDrawingNo(itr.next());
	}

	/**
	 * @param bpaRequest
	 * @param edcr
	 */
	private void populateBusinessService(BPARequest bpaRequest, LinkedHashMap<String, Object> edcr) {
		log.info("populateBusinessService ");
		DocumentContext context = generateERCRContext(edcr);

		// Double plotArea = extractPlotArea(context);

		// Double buildingHeight = extractBuildingHeight(context);

		// boolean isSpecialBuilding = isSpecialBuilding(context);

		String businessServiceFromReq = bpaRequest.getBPA().getBusinessService();

		String businessServiceFromEdcr = extractBusinessService(context);
		List<String> edcrSuggestedList = Arrays.asList(businessServiceFromEdcr.split("\\|"));

		if (StringUtils.hasText(businessServiceFromReq) && !edcrSuggestedList.contains(businessServiceFromReq)) {
			throw new CustomException(BPAErrorConstants.BPA_BUSINESS_SERVICE_ISSUE,
					"Business service is not found in EDCR suggested list.");
		}

		if (!StringUtils.hasText(businessServiceFromReq)) {
			bpaRequest.getBPA().setBusinessService(edcrSuggestedList.get(0));
		}
		log.info("businessService " + bpaRequest.getBPA().getBusinessService());

		// setBusinessService(bpaRequest, buildingHeight, plotArea, isSpecialBuilding);

	}

	private String extractBusinessService(DocumentContext context) {
		if (null != context) {
			String businessService = null;
			LinkedList<String> businessServiceJSONArray = context.read(BPAConstants.BUSINESS_SERVICE_PATH);
			if (!CollectionUtils.isEmpty(businessServiceJSONArray)) {
				if (null != businessServiceJSONArray.get(0)) {
					businessService = businessServiceJSONArray.get(0).toString();
				}

			}
			return businessService;

		}
		return null;
	}

	/**
	 * @param context
	 * @return
	 */
	private boolean isSpecialBuilding(DocumentContext context) {

		/*
		 * Set<String> specialBuildings = getSpecialBuildings(); String subOccupancyType
		 * = extractSubOccupancyType(context); if (null != subOccupancyType &&
		 * specialBuildings.contains(subOccupancyType)) { return true; }
		 */
		if (null != context) {
			String specialBuilding = null;
			LinkedList<String> specialBuildingJSONArray = context.read(BPAConstants.SPECIAL_BUILDING_PATH);
			if (!CollectionUtils.isEmpty(specialBuildingJSONArray)) {
				if (null != specialBuildingJSONArray.get(0)) {
					specialBuilding = specialBuildingJSONArray.get(0).toString();
					if (null != specialBuilding && specialBuilding.equalsIgnoreCase(BPAConstants.YES)) {
						return true;
					}
				}

			}

		}

		return false;
	}

	/**
	 * @param edcr
	 * @return
	 */
	private DocumentContext generateERCRContext(LinkedHashMap<String, Object> edcr) {
		if (null != edcr) {
			String jsonString = new JSONObject(edcr).toString();
			DocumentContext context = JsonPath.using(Configuration.defaultConfiguration()).parse(jsonString);
			return context;

		}
		return null;
	}

	/**
	 * @param context
	 * @return
	 */
	private Double extractPlotArea(DocumentContext context) {
		TypeRef<List<Double>> typeRef = new TypeRef<List<Double>>() {
		};
		if (null != context) {
			Double plotArea = null;
			List<Double> plotAreas = context.read(BPAConstants.PLOT_AREA_PATH, typeRef);
			if (!CollectionUtils.isEmpty(plotAreas)) {
				if (null != plotAreas.get(0)) {
					String plotAreaString = plotAreas.get(0).toString();
					plotArea = Double.parseDouble(plotAreaString);
				}
			}
			return plotArea;

		}
		return null;
	}

	/**
	 * @param context
	 * @return
	 */
	private String extractSubOccupancyType(DocumentContext context) {
		if (null != context) {
			String subOccupancyType = null;
			LinkedList<String> subOccupancyTypeJSONArray = context.read(BPAConstants.SUB_OCCUPANCY_TYPE_PATH);
			if (!CollectionUtils.isEmpty(subOccupancyTypeJSONArray)) {
				if (null != subOccupancyTypeJSONArray.get(0)) {
					subOccupancyType = subOccupancyTypeJSONArray.get(0).toString();
				}

			}
			return subOccupancyType;

		}
		return null;
	}

	/**
	 * @param context
	 * @return
	 */
	private Double extractBuildingHeight(DocumentContext context) {
		TypeRef<List<Double>> typeRef = new TypeRef<List<Double>>() {
		};
		if (null != context) {
			Double buildingHeight = null;
			List<Double> buildingHeights = context.read(BPAConstants.BUILDING_HEIGHT_PATH, typeRef);
			if (!CollectionUtils.isEmpty(buildingHeights)) {
				if (null != buildingHeights.get(0)) {
					String buildingHeightString = buildingHeights.get(0).toString();
					buildingHeight = Double.parseDouble(buildingHeightString);
				}
			}
			return buildingHeight;

		}
		return null;
	}

	/**
	 * @param bpaRequest
	 * @param buildingHeight
	 * @param plotArea
	 * @param isSpecialBuilding
	 */
	private void setBusinessService(BPARequest bpaRequest, Double buildingHeight, Double plotArea,
			boolean isSpecialBuilding) {
		if (null != buildingHeight && null != plotArea) {
			if (!isSpecialBuilding) {
				if ((buildingHeight <= 10) || (plotArea <= 500)) {
					bpaRequest.getBPA().setBusinessService(BPAConstants.BPA_PA_MODULE_CODE);
				}
				if ((buildingHeight > 10 && buildingHeight <= 15) || (plotArea > 500 && plotArea <= 4047)) {
					bpaRequest.getBPA().setBusinessService(BPAConstants.BPA_PO_MODULE_CODE);
				}
				if ((buildingHeight > 15 && buildingHeight <= 30) || (plotArea > 4047 && plotArea <= 10000)) {
					bpaRequest.getBPA().setBusinessService(BPAConstants.BPA_PM_MODULE_CODE);
				}
				if ((buildingHeight > 30) || (plotArea > 10000)) {
					bpaRequest.getBPA().setBusinessService(BPAConstants.BPA_DP_BP_MODULE_CODE);
				}

			} else {
				if (buildingHeight <= 15) {
					bpaRequest.getBPA().setBusinessService(BPAConstants.BPA_PO_MODULE_CODE);
				} else if (buildingHeight > 15 && buildingHeight <= 30) {
					bpaRequest.getBPA().setBusinessService(BPAConstants.BPA_PM_MODULE_CODE);
				} else if (buildingHeight > 30) {
					bpaRequest.getBPA().setBusinessService(BPAConstants.BPA_DP_BP_MODULE_CODE);
				}

			}

		}
	}

	/**
	 * @return
	 */
	private Set<String> getSpecialBuildings() {
		Set<String> specialBuildings = new HashSet<>();
		specialBuildings.add(BPAConstants.E_IB);
		specialBuildings.add(BPAConstants.E_NPI);
		specialBuildings.add(BPAConstants.E_ITB);
		specialBuildings.add(BPAConstants.E_L);
		specialBuildings.add(BPAConstants.E_FF);
		specialBuildings.add(BPAConstants.E_SF);

		specialBuildings.add(BPAConstants.B_H);
		specialBuildings.add(BPAConstants.B_5S);
		specialBuildings.add(BPAConstants.B_WSt1);
		specialBuildings.add(BPAConstants.B_WSt2);
		specialBuildings.add(BPAConstants.B_WM);

		return specialBuildings;
	}

	/**
	 * Enriches workflow assignees based on the workflow action being performed.
	 * 
	 * <p>
	 * This method determines who should be assigned to work on the BPA application
	 * in the next workflow state based on the current action. It ensures that the
	 * right stakeholders (citizens, architects, land owners, registered users) are
	 * notified and can take action on the application.
	 * 
	 * <p>
	 * <strong>Assignee Determination Rules:</strong>
	 * <ul>
	 * <li><strong>SENDBACKTOCITIZEN:</strong> Application sent back to citizen for
	 * corrections
	 * <ul>
	 * <li>All land owners (from land registry)</li>
	 * <li>Application creator/architect (accountId)</li>
	 * <li>All registered users associated with application</li>
	 * </ul>
	 * </li>
	 * <li><strong>SEND_TO_CITIZEN:</strong> Application forwarded to citizen for
	 * action
	 * <ul>
	 * <li>All land owners (from land registry)</li>
	 * <li>All registered users associated with application</li>
	 * </ul>
	 * </li>
	 * <li><strong>SEND_TO_ARCHITECT:</strong> Application sent to architect for
	 * response
	 * <ul>
	 * <li>Application creator/architect (accountId/licensee)</li>
	 * </ul>
	 * </li>
	 * <li><strong>APPROVE (from CITIZEN_APPROVAL_INPROCESS):</strong> Citizen
	 * approval completed
	 * <ul>
	 * <li>Application creator/architect (accountId/licensee)</li>
	 * </ul>
	 * </li>
	 * </ul>
	 * 
	 * <p>
	 * <strong>Assignee Types:</strong>
	 * <ul>
	 * <li><strong>Land Owners:</strong> Citizens who own the land parcel (UUID from
	 * land registry)</li>
	 * <li><strong>Account ID:</strong> Architect/consultant who created the BPA
	 * application</li>
	 * <li><strong>Registered Users:</strong> Additional users registered for the
	 * application (e.g., power of attorney holders)</li>
	 * </ul>
	 * 
	 * <p>
	 * <strong>Note:</strong> Show cause notice responses can be provided by any
	 * stakeholder (citizen, architect, or accredited person), so assignee
	 * enrichment is not restricted for SHOW_CAUSE action (commented out logic
	 * retained for reference).
	 * 
	 * @param bpa the BPA application with workflow information to be enriched with
	 *            assignees
	 */
	public void enrichAssignes(BPA bpa) {
		Workflow workflow = bpa.getWorkflow();

		if (workflow == null) {
			log.debug("No workflow present for BPA: {}", bpa.getApplicationNo());
			return;
		}

		// Step 1: Initialize assignees set with any existing assignees from request
		Set<String> assignees = initializeAssignees(bpa);

		// Step 2: Enrich assignees based on workflow action
		String workflowAction = workflow.getAction();

		if (workflowAction.equalsIgnoreCase(BPAConstants.ACTION_SENDBACKTOCITIZEN)) {
			enrichAssigneesForSendBackToCitizen(bpa, assignees);

		} else if (workflowAction.equalsIgnoreCase(BPAConstants.ACTION_SEND_TO_CITIZEN)) {
			enrichAssigneesForSendToCitizen(bpa, assignees);

		} else if (workflowAction.equalsIgnoreCase(BPAConstants.ACTION_SEND_TO_ARCHITECT)
				|| isApproveFromCitizenApprovalInProcess(bpa, workflowAction)) {
			enrichAssigneesForSendToArchitect(bpa, assignees);
		}

		// Note: SHOW_CAUSE action does not restrict assignees as responses can come
		// from any stakeholder

		// Step 3: Set assignees back to workflow
		setWorkflowAssignees(bpa, assignees);

		log.debug("Enriched {} assignees for workflow action: {} on application: {}", assignees.size(), workflowAction,
				bpa.getApplicationNo());
	}

	/**
	 * Initializes the assignees set with any existing assignees from the request.
	 * 
	 * <p>
	 * This preserves any assignees that may have been specified in the request,
	 * allowing for additional assignees to be added based on business rules.
	 * 
	 * @param bpa the BPA application
	 * @return set of assignee UUIDs (empty or pre-populated from request)
	 */
	private Set<String> initializeAssignees(BPA bpa) {
		Set<String> assignees = new HashSet<>();

		if (bpa.getWorkflow() != null && bpa.getWorkflow().getAssignes() != null) {
			assignees.addAll(bpa.getWorkflow().getAssignes());
			log.debug("Initialized with {} existing assignees from request", assignees.size());
		}

		return assignees;
	}

	/**
	 * Enriches assignees for SENDBACKTOCITIZEN workflow action.
	 * 
	 * <p>
	 * When an application is sent back to citizen for corrections or additional
	 * information, all relevant stakeholders on the citizen side must be notified:
	 * <ul>
	 * <li><strong>Land Owners:</strong> All owners from land registry (primary
	 * stakeholders)</li>
	 * <li><strong>Application Creator/Architect:</strong> The licensee who created
	 * the application must be able to submit corrections on behalf of the
	 * citizen</li>
	 * <li><strong>Registered Users:</strong> Additional users who have been
	 * registered for this application (e.g., power of attorney holders, authorized
	 * representatives)</li>
	 * </ul>
	 * 
	 * <p>
	 * <strong>Use Cases:</strong>
	 * <ul>
	 * <li>Document verification failed - need document corrections</li>
	 * <li>Additional information required from citizen</li>
	 * <li>Plan modifications requested by department</li>
	 * </ul>
	 * 
	 * @param bpa       the BPA application with land and account information
	 * @param assignees the set to populate with assignee UUIDs
	 */
	private void enrichAssigneesForSendBackToCitizen(BPA bpa, Set<String> assignees) {
		// Add all land owners
		if (bpa.getLandInfo() != null && bpa.getLandInfo().getOwners() != null) {
			bpa.getLandInfo().getOwners().forEach(ownerInfo -> {
				assignees.add(ownerInfo.getUuid());
			});
			log.debug("Added {} land owners as assignees", bpa.getLandInfo().getOwners().size());
		}

		// Add architect/creator (accountId) - architect should be able to submit
		// corrections
		if (bpa.getAccountId() != null) {
			assignees.add(bpa.getAccountId());
			log.debug("Added architect/creator as assignee: {}", bpa.getAccountId());
		}

		// Add all registered users associated with the application
		Set<String> registeredUserUuids = userService.getUUidFromUserName(bpa);
		if (!CollectionUtils.isEmpty(registeredUserUuids)) {
			assignees.addAll(registeredUserUuids);
			log.debug("Added {} registered users as assignees", registeredUserUuids.size());
		}

		log.info("SENDBACKTOCITIZEN: Assigned to {} stakeholders for application: {}", assignees.size(),
				bpa.getApplicationNo());
	}

	/**
	 * Enriches assignees for SEND_TO_CITIZEN workflow action.
	 * 
	 * <p>
	 * When an application is forwarded to citizen for action (not sent back, but
	 * moving forward), the relevant citizen stakeholders are assigned:
	 * <ul>
	 * <li><strong>Land Owners:</strong> All owners from land registry (primary
	 * stakeholders)</li>
	 * <li><strong>Registered Users:</strong> Additional users registered for the
	 * application</li>
	 * </ul>
	 * 
	 * <p>
	 * <strong>Difference from SENDBACKTOCITIZEN:</strong> Does not include
	 * architect/creator as this is a forward workflow action where citizen action
	 * is required (e.g., citizen approval, acceptance, payment).
	 * 
	 * <p>
	 * <strong>Use Cases:</strong>
	 * <ul>
	 * <li>Citizen approval required before final processing</li>
	 * <li>Fee payment pending from citizen</li>
	 * <li>Citizen acknowledgment or acceptance required</li>
	 * </ul>
	 * 
	 * @param bpa       the BPA application with land information
	 * @param assignees the set to populate with assignee UUIDs
	 */
	private void enrichAssigneesForSendToCitizen(BPA bpa, Set<String> assignees) {
		// Add all land owners
		if (bpa.getLandInfo() != null && bpa.getLandInfo().getOwners() != null) {
			bpa.getLandInfo().getOwners().forEach(ownerInfo -> {
				assignees.add(ownerInfo.getUuid());
			});
			log.debug("Added {} land owners as assignees", bpa.getLandInfo().getOwners().size());
		}

		// Add all registered users associated with the application
		Set<String> registeredUserUuids = userService.getUUidFromUserName(bpa);
		if (!CollectionUtils.isEmpty(registeredUserUuids)) {
			assignees.addAll(registeredUserUuids);
			log.debug("Added {} registered users as assignees", registeredUserUuids.size());
		}

		log.info("SEND_TO_CITIZEN: Assigned to {} stakeholders for application: {}", assignees.size(),
				bpa.getApplicationNo());
	}

	/**
	 * Enriches assignees for SEND_TO_ARCHITECT workflow action.
	 * 
	 * <p>
	 * When an application is sent to architect/consultant for response or action,
	 * only the application creator (licensee) is assigned:
	 * <ul>
	 * <li><strong>Account ID:</strong> The architect/consultant who created the BPA
	 * application</li>
	 * </ul>
	 * 
	 * <p>
	 * <strong>Use Cases:</strong>
	 * <ul>
	 * <li>Department requires clarification on submitted plans</li>
	 * <li>Technical queries need architect's response</li>
	 * <li>Plan modifications required from architect</li>
	 * <li>Professional certificate or additional documents needed</li>
	 * </ul>
	 * 
	 * <p>
	 * <strong>Note:</strong> Only the architect is assigned, not land owners, as
	 * this requires professional expertise and licensee responsibility.
	 * 
	 * @param bpa       the BPA application with account information
	 * @param assignees the set to populate with assignee UUIDs
	 */
	private void enrichAssigneesForSendToArchitect(BPA bpa, Set<String> assignees) {
		// Add only the architect/creator (accountId/licensee)
		if (bpa.getAccountId() != null) {
			assignees.add(bpa.getAccountId());
			log.debug("Added architect/creator as sole assignee: {}", bpa.getAccountId());
		} else {
			log.warn("Account ID is null for SEND_TO_ARCHITECT action on application: {}", bpa.getApplicationNo());
		}

		log.info("SEND_TO_ARCHITECT: Assigned to architect for application: {}", bpa.getApplicationNo());
	}

	/**
	 * Checks if the workflow action is APPROVE from CITIZEN_APPROVAL_INPROCESS
	 * state.
	 * 
	 * <p>
	 * This is a special case where citizen has approved the application and it
	 * moves back to the architect for final submission to department. The architect
	 * needs to be assigned to proceed with the formal submission.
	 * 
	 * <p>
	 * <strong>Workflow Flow:</strong>
	 * <ol>
	 * <li>Department sends to citizen for approval →
	 * CITIZEN_APPROVAL_INPROCESS</li>
	 * <li>Citizen approves → Action: APPROVE</li>
	 * <li>Architect assigned to finalize and submit → SEND_TO_ARCHITECT
	 * equivalent</li>
	 * </ol>
	 * 
	 * @param bpa            the BPA application with status information
	 * @param workflowAction the current workflow action
	 * @return true if action is APPROVE from CITIZEN_APPROVAL_INPROCESS state
	 */
	private boolean isApproveFromCitizenApprovalInProcess(BPA bpa, String workflowAction) {
		return bpa.getStatus().equalsIgnoreCase(BPAConstants.STATUS_CITIZEN_APPROVAL_INPROCESS)
				&& workflowAction.equalsIgnoreCase(BPAConstants.ACTION_APPROVE);
	}

	/**
	 * Sets the enriched assignees back to the workflow.
	 * 
	 * <p>
	 * If workflow object doesn't exist, creates a new one. This ensures assignees
	 * are properly set even if workflow was initially null.
	 * 
	 * <p>
	 * <strong>Assignee List Conversion:</strong> Converts Set to LinkedList to
	 * maintain order and support duplicate handling (though duplicates are
	 * prevented by Set).
	 * 
	 * @param bpa       the BPA application
	 * @param assignees the set of enriched assignee UUIDs
	 */
	private void setWorkflowAssignees(BPA bpa, Set<String> assignees) {
		List<String> assigneeList = new LinkedList<>(assignees);

		if (bpa.getWorkflow() == null) {
			// Create new workflow if it doesn't exist
			Workflow newWorkflow = new Workflow();
			newWorkflow.setAssignes(assigneeList);
			bpa.setWorkflow(newWorkflow);
			log.debug("Created new workflow with {} assignees", assigneeList.size());
		} else {
			// Update existing workflow
			bpa.getWorkflow().setAssignes(assigneeList);
			log.debug("Updated existing workflow with {} assignees", assigneeList.size());
		}
	}

	public void enrichScnCreateRequestV2(@Valid NoticeRequest request) {

		RequestInfo requestInfo = request.getRequestInfo();
		// requestInfo.setUserInfo(User.builder().uuid("b111659f-7b4c-4cb8-acab-0187407d9d47").build());
		AuditDetails auditDetails = bpaUtil.getAuditDetails(requestInfo.getUserInfo().getUuid(), true);
		Map<String, String> errorMap = new HashMap<>();

		if (request.getnotice().getLetterNo() != null && request.getnotice().getBusinessid() != null
				&& request.getnotice().getLetterType() != null && request.getnotice().getFilestoreid() != null
				&& request.getnotice().getTenantid() != null) {
			request.getnotice().setAuditDetails(auditDetails);
			request.getnotice().setId(UUID.randomUUID().toString());
			request.getnotice().setReminderCount(0);

		} else {
			errorMap.put("NoticeCreateError", "please provide valid details to create a  notice.");
		}
		if (!errorMap.isEmpty())
			throw new CustomException(errorMap);
	}

	public void enrichScnUpdateRequestV2(@Valid NoticeRequest request) {
		// TODO Auto-generated method stub
		RequestInfo requestInfo = request.getRequestInfo();
		Map<String, String> errorMap = new HashMap<>();
		// requestInfo.setUserInfo(User.builder().uuid("b111659f-7b4c-4cb8-acab-0187407d9d47").build());
		AuditDetails auditDetails = bpaUtil.getAuditDetails(requestInfo.getUserInfo().getUuid(), true);
		if (request.getnotice().getLetterNo() != null && request.getnotice().getBusinessid() != null
				&& request.getnotice().getLetterType() != null && request.getnotice().getFilestoreid() != null
				&& request.getnotice().getTenantid() != null) {
			request.getnotice().setAuditDetails(auditDetails);
			// request.getnotice().setId(UUID.randomUUID().toString());
		} else {
			errorMap.put("NoticeCreateError", "please provide valid details to update a  notice.");
		}
		if (!errorMap.isEmpty())
			throw new CustomException(errorMap);
	}

	public void enrichcreatefieldinspectcreate(@Valid FieldInspectionRequest request) {
		// TODO Auto-generated method stub

		request.getFieldinspection().setId(UUID.randomUUID().toString());
		RequestInfo requestInfo = request.getRequestInfo();
		AuditDetails auditDetails = bpaUtil.getAuditDetails(requestInfo.getUserInfo().getUuid(), true);
		request.getFieldinspection().setAuditDetails(auditDetails);

	}

	public void enrichPlanningAssistantChecklistCreate(@Valid PlanningAssistantChecklistRequest request) {

		request.getPlanningAssistantChecklist().setId(UUID.randomUUID().toString());
		RequestInfo requestInfo = request.getRequestInfo();
		AuditDetails auditDetails = bpaUtil.getAuditDetails(requestInfo.getUserInfo().getUuid(), true);
		request.getPlanningAssistantChecklist().setAuditDetails(auditDetails);

	}

	public void enrichPlanningAssistantChecklistUpdate(@Valid PlanningAssistantChecklistRequest request,
			List<PlanningAssistantChecklist> pacs) {
		boolean idExists = pacs.stream()
				.anyMatch(pac -> pac.getId().equals(request.getPlanningAssistantChecklist().getId()));

		if (!idExists) {
			throw new CustomException("Invalid request", "ID not found in existing Planning Assistant Checklist.");
		}
		RequestInfo requestInfo = request.getRequestInfo();
		AuditDetails auditDetails = bpaUtil.getAuditDetails(requestInfo.getUserInfo().getUuid(), true);
		request.getPlanningAssistantChecklist().setAuditDetails(auditDetails);

	}

	public void enrichDocRemarkCreateRequest(DocRemarkRequest request) {
		log.info(" Inside enrichDocRemarkCreateRequest ");
		RequestInfo requestInfo = request.getRequestInfo();
		AuditDetails auditDetails = bpaUtil.getAuditDetails(requestInfo.getUserInfo().getUuid(), true);
		request.getDocRemark().setAuditDetails(auditDetails);
		request.getDocRemark().setId(UUID.randomUUID().toString());
	}

	public void enrichDocRemarkUpdateRequest(DocRemarkRequest request) {

		RequestInfo requestInfo = request.getRequestInfo();
		AuditDetails auditDetails = bpaUtil.getAuditDetails(requestInfo.getUserInfo().getUuid(), false);
		auditDetails.setCreatedBy(request.getDocRemark().getAuditDetails().getCreatedBy());
		auditDetails.setCreatedTime(request.getDocRemark().getAuditDetails().getCreatedTime());
		request.getDocRemark().getAuditDetails().setLastModifiedTime(auditDetails.getLastModifiedTime());
		request.getDocRemark().getAuditDetails().setLastModifiedBy(auditDetails.getLastModifiedBy());
	}

//RevalidationRelated

	public void enrichBPACreateFromRevalidation(BPARequest bpaRequest) {
		log.info(" Inside enrichBPACreateFromRevalidation ");
		RequestInfo requestInfo = bpaRequest.getRequestInfo();
		AuditDetails auditDetails = bpaUtil.getAuditDetails(requestInfo.getUserInfo().getUuid(), true);
		bpaRequest.getBPA().setAuditDetails(auditDetails);
		bpaRequest.getBPA().setId(UUID.randomUUID().toString());
		bpaRequest.getBPA().setAccountId(bpaRequest.getBPA().getAuditDetails().getCreatedBy());
		if (bpaRequest.getBPA().getLandInfo() != null) {
			bpaRequest.getBPA().setLandId(bpaRequest.getBPA().getLandInfo().getId());
		}

		// BPA Documents
		if (!CollectionUtils.isEmpty(bpaRequest.getBPA().getDocuments()))
			bpaRequest.getBPA().getDocuments().stream().filter(d -> d.getId() == null).forEach(document -> {
				document.setId(UUID.randomUUID().toString());

			});
	}

	// ...existing code...
	/**
	 * Enriches BPA update request during revalidation.
	 *
	 * <p>
	 * Business flow:
	 * <ol>
	 * <li>Update audit details (last modified fields).</li>
	 * <li>Sync landId from land info if present.</li>
	 * <li>Assign UUIDs to new BPA documents.</li>
	 * <li>Normalize workflow action for approval/payment based on fee
	 * adjustment.</li>
	 * <li>Enrich assignees for the next workflow state.</li>
	 * <li>Populate/clear DSC details based on roles, status, and action.</li>
	 * </ol>
	 *
	 * @param bpaRequest the revalidation update request payload
	 */
	public void enrichBPAUpdateFromRevalidation(BPARequest bpaRequest) {
		log.info(" Inside enrichBPAUpdateFromRevalidation ");
		RequestInfo requestInfo = bpaRequest.getRequestInfo();

		// 1) Audit fields (last modified only)
		updateRevalidationAudit(bpaRequest, requestInfo);

		// 2) Land linkage (if land info is present)
		syncLandIdFromLandInfo(bpaRequest.getBPA());

		// 3) Assign IDs to newly added documents
		enrichBpaDocumentIds(bpaRequest.getBPA());

		// 4) Normalize workflow action based on fee adjustment
		String action = normalizeRevalidationApprovalAction(bpaRequest);

		// 5) Enrich assignees for workflow routing
		enrichAssignes(bpaRequest.getBPA());

		// 6) DSC integration for approval actions
		enrichRevalidationDscDetails(bpaRequest, requestInfo, action);
	}

	/**
	 * Updates last-modified audit fields for revalidation update.
	 *
	 * @param bpaRequest  the update request
	 * @param requestInfo request metadata containing the user
	 */
	private void updateRevalidationAudit(BPARequest bpaRequest, RequestInfo requestInfo) {
		AuditDetails auditDetails = bpaUtil.getAuditDetails(requestInfo.getUserInfo().getUuid(), false);
		bpaRequest.getBPA().getAuditDetails().setLastModifiedBy(auditDetails.getLastModifiedBy());
		bpaRequest.getBPA().getAuditDetails().setLastModifiedTime(auditDetails.getLastModifiedTime());
	}

	/**
	 * Syncs landId from land info if present.
	 *
	 * @param bpa the BPA entity to update
	 */
	private void syncLandIdFromLandInfo(BPA bpa) {
		if (bpa.getLandInfo() != null) {
			bpa.setLandId(bpa.getLandInfo().getId());
		}
	}

	/**
	 * Adds UUIDs to BPA documents missing IDs.
	 *
	 * @param bpa the BPA entity with documents
	 */
	private void enrichBpaDocumentIds(BPA bpa) {
		if (!CollectionUtils.isEmpty(bpa.getDocuments())) {
			bpa.getDocuments().stream().filter(document -> document.getId() == null)
					.forEach(document -> document.setId(UUID.randomUUID().toString()));
		}
	}

	/**
	 * Normalizes approval action for revalidation based on fee adjustment data.
	 *
	 * @param bpaRequest the update request
	 * @return the normalized action after applying fee adjustment rule
	 */
	private String normalizeRevalidationApprovalAction(BPARequest bpaRequest) {
		String action = bpaRequest.getBPA().getWorkflow().getAction();

		if (action.equalsIgnoreCase(BPAConstants.ACTION_APPROVE)
				|| action.equalsIgnoreCase(BPAConstants.ACTION_APPROVE_AND_SEND_FOR_PAYMENT_RV)) {
			Map<String, String> additionalDetails = bpaRequest.getBPA().getAdditionalDetails() != null
					? (Map) bpaRequest.getBPA().getAdditionalDetails()
					: new HashMap<String, String>();

			// If adjustment exists, keep workflow in approve+payment action; otherwise
			// plain approve.
			if (additionalDetails.get(BPAConstants.BPA_ADD_DETAILS_OTHER_FEE_ADJUSTMENT_AMOUNT_KEY) != null) {
				bpaRequest.getBPA().getWorkflow().setAction(BPAConstants.ACTION_APPROVE_AND_SEND_FOR_PAYMENT_RV);
			} else {
				bpaRequest.getBPA().getWorkflow().setAction(BPAConstants.ACTION_APPROVE);
			}
		}

		return bpaRequest.getBPA().getWorkflow().getAction();
	}

	/**
	 * Enriches DSC details for approval actions in revalidation flow.
	 *
	 * @param bpaRequest  the update request
	 * @param requestInfo request metadata with user information
	 * @param action      the normalized workflow action
	 */
	private void enrichRevalidationDscDetails(BPARequest bpaRequest, RequestInfo requestInfo, String action) {
		List<String> roles = bpaRequest.getRequestInfo().getUserInfo().getRoles().stream().map(role -> role.getCode())
				.collect(Collectors.toList());

		if (bpaRequest.getBPA().getWorkflow() != null) {
			log.info("roles:" + roles + " |status: " + bpaRequest.getBPA().getStatus() + " |action:"
					+ bpaRequest.getBPA().getWorkflow().getAction());
		}

		boolean isEligibleForDsc = bpaRequest.getBPA().getStatus() != null
				&& (roles.contains("EMPLOYEE") || roles.contains("BPA_ARC_APPROVER"))
				&& (action.equalsIgnoreCase(BPAConstants.ACTION_APPROVE)
						|| action.equalsIgnoreCase(BPAConstants.ACTION_APPROVE_AND_SEND_FOR_PAYMENT_RV));

		if (isEligibleForDsc) {
			List<DscDetails> dscDetailss = new ArrayList<>();
			DscDetails dscDetails = new DscDetails();
			dscDetails.setTenantId(bpaRequest.getBPA().getTenantId());
			dscDetails.setId(UUID.randomUUID().toString());
			dscDetails.setApprovedBy(requestInfo.getUserInfo().getUuid());
			dscDetailss.add(dscDetails);
			bpaRequest.getBPA().setDscDetails(dscDetailss);
			log.info("inside enrichment dsc in case of approve . approved by:" + dscDetails.getApprovedBy());
		} else {
			bpaRequest.getBPA().setDscDetails(null);
			log.info("inside enrichment dsc in case of setting dsc as null ");
		}
		
		// Plan DSC flow for Revalidation
		if (isEligibleForDsc) {

			DscDetails planDscDetails = createDscDetails(
					bpaRequest.getBPA().getTenantId(),
					requestInfo.getUserInfo().getUuid());

			Revalidation revalidation = bpaRequest.getRevalidation();

			// Sujog Revalidation
			// BPL document exists in old BPA application
			if (revalidation != null
					&& revalidation.isSujogExistingApplication()
					&& revalidation.getRefBpaApplicationNo() != null) {

				planDscDetails.setApplicationNo(
						revalidation.getRefBpaApplicationNo());

				log.info(
						"Plan DSC mapped with reference application no : {}",
						revalidation.getRefBpaApplicationNo());

			} else {

				log.info(
						"Non-Sujog revalidation, using current BPA application for Plan DSC");
			}

			bpaRequest.getBPA().setPlanDscDetails(
					Arrays.asList(planDscDetails));

			log.info(
					"Plan DSC created successfully for BPA application : {}",
					bpaRequest.getBPA().getApplicationNo());

		} else {

			bpaRequest.getBPA().setPlanDscDetails(null);

			log.info(
					"Plan DSC cleared for BPA application : {}",
					bpaRequest.getBPA().getApplicationNo());
		}
	}
	
	// ...existing code...

	public void enrichRevalidationCreateRequest(RevalidationRequest revalidationRequest) {
		log.info(" Inside enrichRevalidationCreateRequest ");
		RequestInfo requestInfo = revalidationRequest.getRequestInfo();
		String tenantId = revalidationRequest.getRevalidation().getTenantId();
		AuditDetails auditDetails = bpaUtil.getAuditDetails(requestInfo.getUserInfo().getUuid(), true);
		revalidationRequest.getRevalidation().setAuditDetails(auditDetails);
		revalidationRequest.getRevalidation().setId(UUID.randomUUID().toString());

		String applicationNumber = (getIdList(requestInfo, tenantId, config.getApplicationNoIdgenNameforBPARV(),
				config.getApplicationNoIdgenFormatforBPARV(), 1)).get(0);
		revalidationRequest.getRevalidation().setBpaApplicationNo(applicationNumber);

		// Documents-
		if (!CollectionUtils.isEmpty(revalidationRequest.getRevalidation().getDocuments()))
			revalidationRequest.getRevalidation().getDocuments().stream().filter(d -> d.getId() == null)
					.forEach(document -> {

						document.setId(UUID.randomUUID().toString());

					});

	}

	/**
	 * enchrich the updateRequest
	 * 
	 * @param revalidationRequest
	 */
	public void enrichRevalidationUpdateRequest(RevalidationRequest revalidationRequest) {

		RequestInfo requestInfo = revalidationRequest.getRequestInfo();
		AuditDetails auditDetails = bpaUtil.getAuditDetails(requestInfo.getUserInfo().getUuid(), false);
		auditDetails.setCreatedBy(revalidationRequest.getRevalidation().getAuditDetails().getCreatedBy());
		auditDetails.setCreatedTime(revalidationRequest.getRevalidation().getAuditDetails().getCreatedTime());
		revalidationRequest.getRevalidation().getAuditDetails().setLastModifiedTime(auditDetails.getLastModifiedTime());
		// Documents-
		if (!CollectionUtils.isEmpty(revalidationRequest.getRevalidation().getDocuments()))
			revalidationRequest.getRevalidation().getDocuments().stream().filter(d -> d.getId() == null)
					.forEach(document -> {

						document.setId(UUID.randomUUID().toString());

					});
	}

	/**
	 *
	 * @param bpaRequest
	 *
	 *                   Purpose: To enrich permit and oc edcr detail from user
	 *                   input scrutiny details for OC outside Sujog
	 */
	public void enrichOCOutsideEdcrDetailsFromRequest(BPARequest bpaRequest) {
		EdcrDetail edcrDetailForPermit = new EdcrDetail();
		EdcrDetail edcrDetailForOC = new EdcrDetail();

		CustomEdcrDetail permitEdcrDetail = new CustomEdcrDetail();
		CustomEdcrDetail ocEdcrDetail = new CustomEdcrDetail();

		List<ScrutinyDetails> scrutinyDetailsList = bpaRequest.getBPA().getOutsideOCDetails().getScrutinyDetails();
		List<ScrutinyDetails> permitScrutinyDetailsList = scrutinyDetailsList.stream()
				.filter(scrutinyDetail -> scrutinyDetail.getScrutinyType().equalsIgnoreCase("PERMIT"))
				.collect(Collectors.toList());
		List<ScrutinyDetails> ocScrutinyDetailsList = scrutinyDetailsList.stream()
				.filter(scrutinyDetail -> scrutinyDetail.getScrutinyType().equalsIgnoreCase("OC"))
				.collect(Collectors.toList());

		populateEDCRDetail(permitScrutinyDetailsList.get(0), edcrDetailForPermit, bpaRequest.getBPA());
		permitEdcrDetail.setEdcrDetail(Arrays.asList(edcrDetailForPermit));

		populateEDCRDetail(ocScrutinyDetailsList.get(0), edcrDetailForOC, bpaRequest.getBPA());
		ocEdcrDetail.setEdcrDetail(Arrays.asList(edcrDetailForOC));

		bpaRequest.getBPA().setPermitEdcrDetail(permitEdcrDetail);
		bpaRequest.getBPA().setOcEdcrDetail(ocEdcrDetail);

	}

	/**
	 *
	 * @param scrutinyDetails
	 * @param edcrDetail
	 *
	 *                        Purpose: To create EdcrDetail from ScrutinyDetail
	 * @param bpa
	 */
	private void populateEDCRDetail(ScrutinyDetails scrutinyDetails, EdcrDetail edcrDetail, BPA bpa) {
		Plan plan = new Plan();

		enrichPlotDetails(plan, scrutinyDetails);
		enrichPlanInformationDetails(plan, scrutinyDetails);
		enrichFarDetails(plan, scrutinyDetails);
		enrichVirtualBuildingDetails(plan, scrutinyDetails);

		edcrDetail.setPlanDetail(plan);
		edcrDetail.setAppliactionType("BUILDING_OC_PLAN_SCRUTINY");
		edcrDetail.setApplicationSubType("NEW_CONSTRUCTION");
	}

	/**
	 * 
	 * @param plan
	 * @param scrutinyDetails
	 * 
	 *                        Purpose: To add VirtualBuildingDetails in edcrDetails
	 */
	private void enrichVirtualBuildingDetails(Plan plan, ScrutinyDetails scrutinyDetails) {

		VirtualBuilding virtualBuilding = VirtualBuilding.builder()
				.totalBuitUpArea(scrutinyDetails.getTotalBuiltUpArea())
				.totalCarpetArea(scrutinyDetails.getTotalCarpetArea())
				.totalFloorArea(scrutinyDetails.getTotalFloorArea()).build();
		enrichMostRestrictiveFarHelper(virtualBuilding, scrutinyDetails);
		plan.setVirtualBuilding(virtualBuilding);
	}

	/**
	 * 
	 * @param virtualBuilding
	 * @param scrutinyDetails
	 * 
	 *                        Purpose: To add MostRestrictiveFarHelper in
	 *                        edcrDetails
	 */
	private void enrichMostRestrictiveFarHelper(VirtualBuilding virtualBuilding, ScrutinyDetails scrutinyDetails) {

		OccupancyHelperDetail occupancyTypeHelperDetail = OccupancyHelperDetail.builder()
				.code(scrutinyDetails.getOccupancyTypeHelperCode()).build();
		OccupancyHelperDetail occpancySubTypeHelperDetail = OccupancyHelperDetail.builder()
				.code(scrutinyDetails.getOccupancySubTypeHelperCode()).build();

		OccupancyTypeHelper occupancyTypeHelper = OccupancyTypeHelper.builder().type(occupancyTypeHelperDetail)
				.subtype(occpancySubTypeHelperDetail).build();

		virtualBuilding.setMostRestrictiveFarHelper(occupancyTypeHelper);
	}

	/**
	 * 
	 * @param plan
	 * @param scrutinyDetails
	 * 
	 *                        Purpose: To add FarDetails in edcrDetails
	 */

	private void enrichFarDetails(Plan plan, ScrutinyDetails scrutinyDetails) {
		FarDetails farDetails = FarDetails.builder().baseFar(scrutinyDetails.getBaseFar())
				.providedFar(scrutinyDetails.getProvidedFar()).permissableFar(scrutinyDetails.getMaxPermissibleFar())
				.tdrFarRelaxation(scrutinyDetails.getTdrFarRelaxation()).build();
		plan.setFarDetails(farDetails);
	}

	/**
	 * 
	 * @param plan
	 * @param scrutinyDetails
	 * 
	 *                        Purpose: To add planInformation in edcrDetails
	 */

	private void enrichPlanInformationDetails(Plan plan, ScrutinyDetails scrutinyDetails) {

		PlanInformation planInformation = PlanInformation.builder()
				.totalNoOfDwellingUnits(scrutinyDetails.getTotalNoOfDwellingUnits())
				.benchmarkValuePerAcre(scrutinyDetails.getBenchmarkValuePerAcre())
				.isSecurityDepositRequired(scrutinyDetails.isSecurityDepositRequired())
				.shelterFeeRequired(scrutinyDetails.isShelterFeeRequired())
				.projectValueForEIDP(scrutinyDetails.getProjectValueForEIDP())
				.isRetentionFeeApplicable(scrutinyDetails.isRetentionFeeApplicable())
				.numberOfTemporaryStructures(scrutinyDetails.getNumberOfTemporaryStructures())
				.requiredNOCs(Arrays.asList(""))
				.isProjectUndertakingByGovt(scrutinyDetails.getIsProjectUndertakingByGovt()).build();

		plan.setPlanInformation(planInformation);

	}

	/**
	 * 
	 * @param plan
	 * @param scrutinyDetails
	 * 
	 *                        Purpose: To add Plot Details in edcrDetails
	 */

	private void enrichPlotDetails(Plan plan, ScrutinyDetails scrutinyDetails) {
		Plot plot = Plot.builder().build();
		plot.setArea(scrutinyDetails.getPlotDetails().getNetPlotArea());
		plan.setPlot(plot);
	}

	/**
	 * 
	 * @param bpas
	 * 
	 *             Purpose: To fetch edcrDetails from DB for OC Outside Applications
	 *             that are already created
	 */

	public void enrichOCSearchOutsideDetaiblsFromDB(List<BPA> bpas) {
		bpas.forEach(bpa -> {
			List<ScrutinyDetails> scrutinyDetails = bpaRepository.getScrutinyDetails(bpa);
			OutsideOCDetails outsideOCDetails = OutsideOCDetails.builder().scrutinyDetails(scrutinyDetails).build();
			bpa.setOutsideOCDetails(outsideOCDetails);
			bpa.setOCOutsideSujogApplication(true);
		});

	}

	public void enrichUUIDInScrutinyDetails(BPARequest bpaRequest) {

		List<ScrutinyDetails> scrutinyDetailsList = new ArrayList<>();
		bpaRequest.getBPA().getOutsideOCDetails().getScrutinyDetails().forEach(scrutinyDetails -> {
			scrutinyDetails.setId(UUID.randomUUID().toString());
		});

	}

	/**
	 * To determine Business Service for OC Outside SUJOG
	 * 
	 * @param bpaRequest
	 * @param isSpecialBuilding
	 */
	public void enrichOCBusinessService(BPARequest bpaRequest, Boolean isSpecialBuilding) {
		List<BuildingBlockDetails> buildingBlockDetailsList = bpaRequest.getBPA().getOutsideOCDetails()
				.getScrutinyDetails().stream()
				.filter(scrutinyDetail -> scrutinyDetail.getScrutinyType().equalsIgnoreCase("OC"))
				.collect(Collectors.toList()).get(0).getBuildingBlockDetails();

		PlotDetails plotDetails = bpaRequest.getBPA().getOutsideOCDetails().getScrutinyDetails().stream()
				.filter(scrutinyDetail -> scrutinyDetail.getScrutinyType().equalsIgnoreCase("OC"))
				.collect(Collectors.toList()).get(0).getPlotDetails();

		BigDecimal maxBuildingHeight = BigDecimal.ZERO;

		for (BuildingBlockDetails buildingBlock : buildingBlockDetailsList) {

			BigDecimal height = new BigDecimal(buildingBlock.getBuildingHeight().toString());
			if (height.compareTo(maxBuildingHeight) > 0) {
				maxBuildingHeight = height;

			}
		}

		if (bpaRequest.getBPA().getOCOutsideSujogApplication()) {
			calculateBusinessServiceForOCOutsideSujog(bpaRequest, isSpecialBuilding, plotDetails, maxBuildingHeight);
		} else {
			calculateBusinessServiceForOCInsideSujog(bpaRequest, isSpecialBuilding, plotDetails, maxBuildingHeight);
		}

	}

	private void calculateBusinessServiceForOCOutsideSujog(BPARequest bpaRequest, Boolean isSpecialBuilding,
			PlotDetails plotDetails, BigDecimal maxBuildingHeight) {
		BigDecimal totalPlotArea = plotDetails.getPlotArea();

		if (totalPlotArea.compareTo(OCConstants.FIVE_HUNDRED_SQMT) <= 0) {
			if (!isSpecialBuilding)
				bpaRequest.getBPA().setBusinessService(OCConstants.OC_OUTSIDESUJOG_BUSSINESS_SERVICE_1);
			else
				bpaRequest.getBPA().setBusinessService(OCConstants.OC_OUTSIDESUJOG_BUSSINESS_SERVICE_2);

			if (maxBuildingHeight.compareTo(OCConstants.TEN_METERS) > 0
					&& maxBuildingHeight.compareTo(OCConstants.FIFTEEN_METERS) <= 0)
				bpaRequest.getBPA().setBusinessService(OCConstants.OC_OUTSIDESUJOG_BUSSINESS_SERVICE_2);

			if (maxBuildingHeight.compareTo(OCConstants.FIFTEEN_METERS) > 0
					&& maxBuildingHeight.compareTo(OCConstants.THIRTY_METERS) <= 0)
				bpaRequest.getBPA().setBusinessService(OCConstants.OC_OUTSIDESUJOG_BUSSINESS_SERVICE_3);

			if (maxBuildingHeight.compareTo(OCConstants.THIRTY_METERS) > 0)
				bpaRequest.getBPA().setBusinessService(OCConstants.OC_OUTSIDESUJOG_BUSSINESS_SERVICE_4);

		} else if (totalPlotArea.compareTo(OCConstants.FIVE_HUNDRED_SQMT) > 0
				&& totalPlotArea.compareTo(OCConstants.ONE_ACRE) <= 0) {
			bpaRequest.getBPA().setBusinessService(OCConstants.OC_OUTSIDESUJOG_BUSSINESS_SERVICE_2);

			if (maxBuildingHeight.compareTo(OCConstants.FIFTEEN_METERS) > 0
					&& maxBuildingHeight.compareTo(OCConstants.THIRTY_METERS) <= 0)
				bpaRequest.getBPA().setBusinessService(OCConstants.OC_OUTSIDESUJOG_BUSSINESS_SERVICE_3);

			if (maxBuildingHeight.compareTo(OCConstants.THIRTY_METERS) > 0)
				bpaRequest.getBPA().setBusinessService(OCConstants.OC_OUTSIDESUJOG_BUSSINESS_SERVICE_4);

		} else if (totalPlotArea.compareTo(OCConstants.ONE_ACRE) > 0
				&& totalPlotArea.compareTo(OCConstants.ONE_HECTARE) <= 0) {
			bpaRequest.getBPA().setBusinessService(OCConstants.OC_OUTSIDESUJOG_BUSSINESS_SERVICE_3);

			if (maxBuildingHeight.compareTo(OCConstants.THIRTY_METERS) > 0)
				bpaRequest.getBPA().setBusinessService(OCConstants.OC_OUTSIDESUJOG_BUSSINESS_SERVICE_4);

		} else if (totalPlotArea.compareTo(OCConstants.ONE_HECTARE) > 0) {
			bpaRequest.getBPA().setBusinessService(OCConstants.OC_OUTSIDESUJOG_BUSSINESS_SERVICE_4);
		}
	}

	private void calculateBusinessServiceForOCInsideSujog(BPARequest bpaRequest, Boolean isSpecialBuilding,
			PlotDetails plotDetails, BigDecimal maxBuildingHeight) {
		BigDecimal totalPlotArea = plotDetails.getPlotArea();

		if (totalPlotArea.compareTo(OCConstants.FIVE_HUNDRED_SQMT) <= 0) {
			if (!isSpecialBuilding)
				bpaRequest.getBPA().setBusinessService(OCConstants.OC_BUSSINESS_SERVICE_1);
			else
				bpaRequest.getBPA().setBusinessService(OCConstants.OC_BUSSINESS_SERVICE_2);

			if (maxBuildingHeight.compareTo(OCConstants.TEN_METERS) > 0
					&& maxBuildingHeight.compareTo(OCConstants.FIFTEEN_METERS) <= 0)
				bpaRequest.getBPA().setBusinessService(OCConstants.OC_BUSSINESS_SERVICE_2);

			if (maxBuildingHeight.compareTo(OCConstants.FIFTEEN_METERS) > 0
					&& maxBuildingHeight.compareTo(OCConstants.THIRTY_METERS) <= 0)
				bpaRequest.getBPA().setBusinessService(OCConstants.OC_BUSSINESS_SERVICE_3);

			if (maxBuildingHeight.compareTo(OCConstants.THIRTY_METERS) > 0)
				bpaRequest.getBPA().setBusinessService(OCConstants.OC_BUSSINESS_SERVICE_4);

		} else if (totalPlotArea.compareTo(OCConstants.FIVE_HUNDRED_SQMT) > 0
				&& totalPlotArea.compareTo(OCConstants.ONE_ACRE) <= 0) {
			bpaRequest.getBPA().setBusinessService(OCConstants.OC_BUSSINESS_SERVICE_2);

			if (maxBuildingHeight.compareTo(OCConstants.FIFTEEN_METERS) > 0
					&& maxBuildingHeight.compareTo(OCConstants.THIRTY_METERS) <= 0)
				bpaRequest.getBPA().setBusinessService(OCConstants.OC_BUSSINESS_SERVICE_3);

			if (maxBuildingHeight.compareTo(OCConstants.THIRTY_METERS) > 0)
				bpaRequest.getBPA().setBusinessService(OCConstants.OC_BUSSINESS_SERVICE_4);

		} else if (totalPlotArea.compareTo(OCConstants.ONE_ACRE) > 0
				&& totalPlotArea.compareTo(OCConstants.ONE_HECTARE) <= 0) {
			bpaRequest.getBPA().setBusinessService(OCConstants.OC_BUSSINESS_SERVICE_3);

			if (maxBuildingHeight.compareTo(OCConstants.THIRTY_METERS) > 0)
				bpaRequest.getBPA().setBusinessService(OCConstants.OC_BUSSINESS_SERVICE_4);

		} else if (totalPlotArea.compareTo(OCConstants.ONE_HECTARE) > 0) {
			bpaRequest.getBPA().setBusinessService(OCConstants.OC_BUSSINESS_SERVICE_4);
		}
	}

	/**
	 * To determine whether the Building Blocks should fall under special building
	 * category or not for OC Outside SUJOG
	 * 
	 * @param bpaRequest
	 * @return
	 */
	public Boolean isSpecialBuilding(BPARequest bpaRequest) {

		List<BuildingBlockDetails> buildingBlockDetailsList = bpaRequest.getBPA().getOutsideOCDetails()
				.getScrutinyDetails().stream()
				.filter(scrutinyDetail -> scrutinyDetail.getScrutinyType().equalsIgnoreCase("OC"))
				.collect(Collectors.toList()).get(0).getBuildingBlockDetails();

		Boolean isSpecialBuilding = Boolean.FALSE;
		List<String> subOccupancyList = BPAConstants.SPECIAL_BUILDING_SUBOCCUPANCY_LIST;

		Map<String, BigDecimal> subOccupancyMap = new HashMap<>();

		for (BuildingBlockDetails buildingBlockDetails : buildingBlockDetailsList) {

			List<Floor> floors = buildingBlockDetails.getFloors();
			for (Floor floor : floors) {
				if (subOccupancyList.contains(floor.getSubOcuupancy())) {
					if (subOccupancyMap.containsKey(floor.getSubOcuupancy())) {
						BigDecimal asBuiltBUA = subOccupancyMap.get(floor.getSubOcuupancy());
						asBuiltBUA = asBuiltBUA.add(BigDecimal.valueOf(Double.parseDouble(floor.getAsBuiltBUA())));
						subOccupancyMap.put(floor.getSubOcuupancy().getValue(), asBuiltBUA);
					} else {
						subOccupancyMap.put(floor.getSubOcuupancy().getValue(),
								BigDecimal.valueOf(Double.parseDouble(floor.getAsBuiltBUA())));
					}
				}
			}
		}

		for (String subOccupancy : subOccupancyMap.keySet()) {
			if (subOccupancyMap.get(subOccupancy).compareTo(BigDecimal.valueOf(500d)) > 0) {
				isSpecialBuilding = true;
			}
		}
		return isSpecialBuilding;
	}

	public void enrichPlinthApprovalCreate(@Valid PlinthApprovalRequest request, List<BPA> bpa) {
		request.getPlinthApproval().setId(UUID.randomUUID().toString());
		RequestInfo requestInfo = request.getRequestInfo();
		AuditDetails auditDetails = bpaUtil.getAuditDetails(requestInfo.getUserInfo().getUuid(), true);
		request.getPlinthApproval().setAuditDetails(auditDetails);
		if (!CollectionUtils.isEmpty(bpa)) {
			request.getPlinthApproval().setBpaApprover(bpa.get(0).getDscDetails().get(0).getApprovedBy());
		}

		if (!CollectionUtils.isEmpty(request.getPlinthApproval().getDocuments()))
			request.getPlinthApproval().getDocuments().forEach(document -> {
				if (document.getId() == null) {
					document.setId(UUID.randomUUID().toString());
				}
			});
		setIdgenForPlinthApproval(request);

	}

	private void setIdgenForPlinthApproval(PlinthApprovalRequest request) {
		RequestInfo requestInfo = request.getRequestInfo();
		String tenantId = request.getPlinthApproval().getTenantId();
		PlinthApproval plinthApproval = request.getPlinthApproval();
		List<String> applicationNumbers = new ArrayList<>();
		applicationNumbers = getIdList(requestInfo, tenantId, config.getPlinthApplName(),
				config.getPlinthApplNumFormat(), 1);

		ListIterator<String> itr = applicationNumbers.listIterator();

		Map<String, String> errorMap = new HashMap<>();

		if (!errorMap.isEmpty())
			throw new CustomException(errorMap);

		plinthApproval.setApplicationNo(itr.next());
	}

	public void enrichPlinthApprovalUpdate(@Valid PlinthApprovalRequest request) {

		// enrich Uuid's for documents which are added in update request
		if (!CollectionUtils.isEmpty(request.getPlinthApproval().getDocuments()))
			request.getPlinthApproval().getDocuments().forEach(document -> {
				if (document.getId() == null) {
					document.setId(UUID.randomUUID().toString());
				}
			});

	}

	public void enrichUpdateFieldInspectionRequest(FieldInspectionRequest request) {
		RequestInfo requestInfo = request.getRequestInfo();
		AuditDetails auditDetails = bpaUtil.getAuditDetails(requestInfo.getUserInfo().getUuid(), false);
		request.getFieldinspection().setAuditDetails(auditDetails);
	}

	public void generateApprovalNoForOutsideSujogRevalidationApplication(BPARequest bpaRequest) {
		BPA bpa = bpaRequest.getBPA();
		log.info("generating permit number for Revalidation for appNo: " + bpa.getApplicationNo());
		List<IdResponse> idResponses = idGenRepository.getId(bpaRequest.getRequestInfo(), bpa.getTenantId(),
				config.getPermitNoIdgenName(), config.getPermitNoIdgenFormat(), 1).getIdResponses();
		bpa.setApprovalNo(idResponses.get(0).getId());

	}

	public void enrichBPASaveDraft(@Valid BPADraftRequest request) {
		RequestInfo requestInfo = request.getRequestInfo();
		AuditDetails auditDetails = bpaUtil.getAuditDetails(requestInfo.getUserInfo().getUuid(), true);
		request.getBpaDraft().setAuditDetails(auditDetails);
		request.getBpaDraft().setId(UUID.randomUUID().toString());
		request.getBpaDraft().setStatus("CREATED");
		Map<String, Object> additionalDetails = (Map<String, Object>) request.getBpaDraft().getAdditionalDetails();
		if (!ObjectUtils.isEmpty(additionalDetails)) {
			Map<String, Object> bpaObj = (Map<String, Object>) additionalDetails.get("BPA");
			String applicationType = (String) bpaObj.get("applicationType");
			if (applicationType != null && applicationType.equalsIgnoreCase(BPAConstants.BUILDING_PLAN_OC)
					&& StringUtils.isEmpty(request.getBpaDraft().getEdcrNo())) {
				String tenantId = request.getBpaDraft().getTenantId();
				List<IdResponse> idResponses = idGenRepository
						.getId(requestInfo, tenantId, config.getOcDraftFormat(), config.getOcDraftFormat(), 1)
						.getIdResponses();
				request.getBpaDraft().setEdcrNo(idResponses.get(0).getId().trim());
			}
		}
	}

	public void enrichCompletionCertificateCreateRequest(
			@Valid CompletionCertificateRequest completionCertificateRequest) {
		completionCertificateRequest.getCompletionCertificate().setId(UUID.randomUUID().toString());
		AuditDetails auditDetails = bpaUtil
				.getAuditDetails(completionCertificateRequest.getRequestInfo().getUserInfo().getUuid(), true);
		completionCertificateRequest.getCompletionCertificate().setAuditDetails(auditDetails);
		setIdgenForCompletionCertificate(completionCertificateRequest);

	}

	private void setIdgenForCompletionCertificate(@Valid CompletionCertificateRequest completionCertificateRequest) {
		RequestInfo requestInfo = completionCertificateRequest.getRequestInfo();
		String tenantId = completionCertificateRequest.getCompletionCertificate().getTenantId();
		CompletionCertificate completionCertificate = completionCertificateRequest.getCompletionCertificate();
		List<String> applicationNumbers = new ArrayList<>();
		applicationNumbers = getIdList(requestInfo, tenantId, config.getCompletionCertificateApplName(),
				config.getCompletionCertificateNumFormat(), 1);

		ListIterator<String> itr = applicationNumbers.listIterator();

		Map<String, String> errorMap = new HashMap<>();

		if (!errorMap.isEmpty())
			throw new CustomException(errorMap);

		completionCertificate.setCertificateNo(itr.next());

	}

	public void enrichCompletionCertificateCreateRequest(@Valid StageWiseReportRequest stageWiseReportRequest) {
		List<StageWiseReport> stageWiseReports = stageWiseReportRequest.getStageWiseReports();

		if (stageWiseReports != null && !stageWiseReports.isEmpty()) {
			String userUuid = stageWiseReportRequest.getRequestInfo().getUserInfo().getUuid();

			for (StageWiseReport report : stageWiseReports) {
				report.setId(UUID.randomUUID().toString());
				AuditDetails auditDetails = bpaUtil.getAuditDetails(userUuid, true);
				report.setAuditDetails(auditDetails);
				if (report.getStatus() == null) {
					report.setStatus(BPAConstants.STATUS_ACTIVE);
				}
			}
		}

	}

	public void enrichCompletionCertificateUpdateRequest(@Valid StageWiseReportRequest stageWiseReportRequest) {
		List<StageWiseReport> stageWiseReports = stageWiseReportRequest.getStageWiseReports();

		if (stageWiseReports != null && !stageWiseReports.isEmpty()) {
			String userUuid = stageWiseReportRequest.getRequestInfo().getUserInfo().getUuid();

			for (StageWiseReport report : stageWiseReports) {
				AuditDetails auditDetails = bpaUtil.getAuditDetails(userUuid, true);
				report.setAuditDetails(auditDetails);
			}
		}

	}

}