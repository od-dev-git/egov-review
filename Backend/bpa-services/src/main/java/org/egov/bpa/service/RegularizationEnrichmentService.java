package org.egov.bpa.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.validation.Valid;

import org.egov.bpa.config.BPAConfiguration;
import org.egov.bpa.repository.IdGenRepository;
import org.egov.bpa.util.BPAConstants;
import org.egov.bpa.util.RegularizationConstants;
import org.egov.bpa.util.RegularizationUtil;
import org.egov.bpa.web.model.AuditDetails;
import org.egov.bpa.web.model.NoticeRequest;
import org.egov.bpa.web.model.RegularizationDraft;
import org.egov.bpa.web.model.RegularizationDraftRequest;
import org.egov.bpa.web.model.Workflow;
import org.egov.bpa.web.model.idgen.IdResponse;
import org.egov.bpa.web.model.regularization.PlotInfo;
import org.egov.bpa.web.model.regularization.Regularization;
import org.egov.bpa.web.model.regularization.RegularizationDocRemarkRequest;
import org.egov.bpa.web.model.regularization.RegularizationDscDetails;
import org.egov.bpa.web.model.regularization.RegularizationFieldInspectionRequest;
import org.egov.bpa.web.model.regularization.RegularizationRequest;
import org.egov.bpa.web.model.workflow.BusinessService;
import org.egov.common.contract.request.RequestInfo;
import org.egov.tracer.model.CustomException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class RegularizationEnrichmentService {

	@Autowired
	private RegularizationUtil util;

	@Autowired
	private IdGenRepository idGenRepository;

	@Autowired
	private BPAConfiguration config;

	@Autowired
	private RegularizationUserService userService;

	@Autowired
	private RegularizationWorkflow workflowService;

	/**
	 * Enriches the required data for a Land Regularization create request.
	 * 
	 * <p>
	 * This method orchestrates the enrichment process by performing the following
	 * steps:
	 * <ol>
	 * <li>Sets audit details (created by, created time) from the request user</li>
	 * <li>Generates unique identifiers for regularization and land info
	 * entities</li>
	 * <li>Enriches land info and plot info with required metadata</li>
	 * <li>Generates application number using ID generation service</li>
	 * <li>Determines the applicable business service based on plot area</li>
	 * <li>Enriches owner details by creating/updating user records</li>
	 * </ol>
	 * </p>
	 * 
	 * @param requestInfo    The request information containing user context and
	 *                       auth details
	 * @param regularization The regularization entity to be enriched
	 */
	public void enrichLandCreateRequest(RequestInfo requestInfo, Regularization regularization) {

		// Step 1: Create audit details from the authenticated user
		AuditDetails auditDetails = util.getAuditDetails(requestInfo.getUserInfo().getUuid(), true);

		// Step 2: Enrich regularization with audit details and account information
		enrichRegularizationAuditDetails(regularization, auditDetails, requestInfo);

		// Step 3: Generate and set unique identifiers for regularization and land info
		String regularizationUniqueId = UUID.randomUUID().toString();
		String landInfoUniqueId = UUID.randomUUID().toString();
		setUniqueIdentifiers(regularization, regularizationUniqueId, landInfoUniqueId);

		// Step 4: Enrich plot info with required metadata
		enrichPlotInfoForCreate(regularization, landInfoUniqueId, auditDetails);

		// Step 5: Generate application number from ID generation service
		String applicationNo = generateApplicationNumber(requestInfo, regularization.getTenantId());
		regularization.setApplicationNo(applicationNo);

		// Step 6: Determine and set the applicable business service based on plot area
		populateBusinessServiceForLand(regularization);

		// Step 7: Enrich owner details (create/update user records)
		enrichOwnerDetails(requestInfo, regularization);
	}

	/**
	 * Enriches the regularization entity with audit details and account
	 * information.
	 * 
	 * @param regularization The regularization entity to enrich
	 * @param auditDetails   The audit details containing created by and created
	 *                       time
	 * @param requestInfo    The request info containing user context
	 */
	private void enrichRegularizationAuditDetails(Regularization regularization, AuditDetails auditDetails,
			RequestInfo requestInfo) {

		// Set audit details on regularization entity
		regularization.setAuditDetails(auditDetails);

		// Set audit details on land info entity
		regularization.getLandRegularizationInfo().setAuditDetails(auditDetails);

		// Set account ID to track the creator of this regularization
		regularization.setAccountId(requestInfo.getUserInfo().getUuid());
	}

	/**
	 * Sets unique identifiers for regularization and land info entities.
	 * 
	 * <p>
	 * Establishes the parent-child relationship between regularization and land
	 * info by setting the regularization ID reference.
	 * </p>
	 * 
	 * @param regularization         The regularization entity
	 * @param regularizationUniqueId The unique ID for regularization
	 * @param landInfoUniqueId       The unique ID for land info
	 */
	private void setUniqueIdentifiers(Regularization regularization, String regularizationUniqueId,
			String landInfoUniqueId) {

		// Set primary key for regularization
		regularization.setId(regularizationUniqueId);

		// Set primary key for land info
		regularization.getLandRegularizationInfo().setId(landInfoUniqueId);

		// Establish foreign key relationship to parent regularization
		regularization.getLandRegularizationInfo().setRegularizationId(regularizationUniqueId);
	}

	/**
	 * Enriches plot info entities with required metadata for create operation.
	 * 
	 * <p>
	 * Each plot info is enriched with:
	 * <ul>
	 * <li>Unique identifier</li>
	 * <li>Tenant ID from parent regularization</li>
	 * <li>Foreign key reference to land info</li>
	 * <li>Initial deletion flag</li>
	 * <li>Audit details</li>
	 * </ul>
	 * </p>
	 * 
	 * @param regularization   The regularization entity containing plot info
	 * @param landInfoUniqueId The unique ID of the parent land info
	 * @param auditDetails     The audit details to set on each plot
	 */
	private void enrichPlotInfoForCreate(Regularization regularization, String landInfoUniqueId,
			AuditDetails auditDetails) {

		regularization.getLandRegularizationInfo().getPlotInfo().forEach(plot -> {
			// Generate unique ID for each plot
			plot.setId(UUID.randomUUID().toString());

			// Set tenant ID from parent regularization
			plot.setTenantId(regularization.getTenantId());

			// Establish foreign key relationship to parent land info
			plot.setLandInfoId(landInfoUniqueId);

			// Set initial deletion flag (TRUE indicates active/not deleted)
			plot.setIsDeleted(Boolean.TRUE);

			// Set audit details for tracking
			plot.setAuditDetails(auditDetails);
		});
	}

	/**
	 * Generates a unique application number using the ID generation service.
	 * 
	 * @param requestInfo The request info for ID generation API call
	 * @param tenantId    The tenant ID for which to generate the application number
	 * @return The generated application number
	 */
	private String generateApplicationNumber(RequestInfo requestInfo, String tenantId) {

		// Call ID generation service with configured name and format
		List<IdResponse> idResponses = idGenRepository
				.getId(requestInfo, tenantId, config.getBpaRegularizationName(), config.getBpaRegularizationFormat(), 1)
				.getIdResponses();

		// Extract and trim the generated application number
		return idResponses.get(0).getId().trim();
	}

	/**
	 * Method to Enrich Owner details
	 * 
	 * @param requestInfo
	 * @param regularization
	 */
	private void enrichOwnerDetails(RequestInfo requestInfo, Regularization regularization) {
		userService.manageUser(requestInfo, regularization);
		enrichRequiredDataForUser(requestInfo, regularization, true);
	}

	/**
	 * Enrich the required Institution Data and User Data in Request
	 * 
	 * @param requestInfo
	 * @param regularization
	 * @param isUpdate
	 */
	private void enrichRequiredDataForUser(RequestInfo requestInfo, Regularization regularization, Boolean isUpdate) {

		AuditDetails auditDetails = util.getAuditDetails(requestInfo.getUserInfo().getUuid(), true);

		// Institution data
		if (regularization.getInstitution() != null) {
			if (StringUtils.isEmpty(regularization.getInstitution().getId()))
				regularization.getInstitution().setId(UUID.randomUUID().toString());
			if (StringUtils.isEmpty(regularization.getInstitution().getTenantId()))
				regularization.getInstitution().setTenantId(regularization.getTenantId());
		}

		// Owners data
		if (!CollectionUtils.isEmpty(regularization.getOwners())) {
			regularization.getOwners().forEach(owner -> {
				if (StringUtils.isEmpty(owner.getOwnerId()))
					owner.setOwnerId(UUID.randomUUID().toString());
				owner.setAuditDetails(auditDetails);
			});
		}

	}

	/**
	 * This method is responsible for deciding the business Service flow for Land
	 * Request. For Land request basis is plot area
	 * 
	 * @param regularization
	 */
	private void populateBusinessServiceForLand(Regularization regularization) {

		BigDecimal totalPlotArea = util.getNetPlotArea(regularization.getLandRegularizationInfo().getPlotInfo());

		if (totalPlotArea.compareTo(RegularizationConstants.FIVE_HUNDRED_SQMT) < 0) {
			regularization.setBusinessService(RegularizationConstants.LAND_BUSSINESS_SERVICE_1);
		} else if (totalPlotArea.compareTo(RegularizationConstants.FIVE_HUNDRED_SQMT) >= 0
				&& totalPlotArea.compareTo(RegularizationConstants.ONE_ACRE) < 0) {
			regularization.setBusinessService(RegularizationConstants.LAND_BUSSINESS_SERVICE_2);
		} else if (totalPlotArea.compareTo(RegularizationConstants.ONE_ACRE) >= 0
				&& totalPlotArea.compareTo(RegularizationConstants.ONE_HECTARE) < 0) {
			regularization.setBusinessService(RegularizationConstants.LAND_BUSSINESS_SERVICE_3);
		} else if (totalPlotArea.compareTo(RegularizationConstants.ONE_HECTARE) >= 0) {
			regularization.setBusinessService(RegularizationConstants.LAND_BUSSINESS_SERVICE_4);
		}
	}

	/**
	 * Enriches the required data for a Regularization update request.
	 * 
	 * <p>
	 * This method orchestrates the update enrichment process by performing the
	 * following steps:
	 * <ol>
	 * <li>Updates audit details with last modified information while preserving
	 * created details</li>
	 * <li>Enriches workflow assignees based on the action being performed</li>
	 * <li>Generates unique IDs for newly added documents</li>
	 * <li>Generates unique IDs for newly added workflow verification documents</li>
	 * <li>Creates DSC (Digital Signature Certificate) details for
	 * approval/rejection actions</li>
	 * <li>Enriches plot info details for both updates and new additions</li>
	 * </ol>
	 * </p>
	 * 
	 * @param request The regularization request containing application details and
	 *                request info
	 */
	public void enrichRegularizationUpdateRequest(RegularizationRequest request) {

		RequestInfo requestInfo = request.getRequestInfo();
		Regularization regularization = request.getRegularization();

		// Step 1: Create and set audit details for the update operation
		AuditDetails auditDetails = enrichUpdateAuditDetails(requestInfo, regularization);

		// Step 2: Enrich workflow assignees based on the current action
		enrichAssignes(regularization);

		// Step 3: Generate unique IDs for newly added regularization documents
		enrichDocumentsForUpdate(regularization);

		// Step 4: Generate unique IDs for newly added workflow verification documents
		enrichWorkflowDocumentsForUpdate(regularization);

		// Step 5: Create DSC details for approval/rejection actions by employees
		enrichDscDetailsForUpdate(request, requestInfo);

		// Step 6: Enrich plot info details (both existing and newly added plots)
		enrichPlotInfoDetails(request, auditDetails);
	}

	/**
	 * Creates and sets audit details for the update operation.
	 * 
	 * <p>
	 * Preserves the original created by and created time while updating the last
	 * modified time to the current timestamp.
	 * </p>
	 * 
	 * @param requestInfo    The request info containing user context
	 * @param regularization The regularization entity to update
	 * @return The updated audit details object
	 */
	private AuditDetails enrichUpdateAuditDetails(RequestInfo requestInfo, Regularization regularization) {

		// Create audit details for update (isCreate = false)
		AuditDetails auditDetails = util.getAuditDetails(requestInfo.getUserInfo().getUuid(), false);

		// Preserve original creation metadata
		auditDetails.setCreatedBy(regularization.getAuditDetails().getCreatedBy());
		auditDetails.setCreatedTime(regularization.getAuditDetails().getCreatedTime());

		// Update last modified time on the regularization entity
		regularization.getAuditDetails().setLastModifiedTime(auditDetails.getLastModifiedTime());

		return auditDetails;
	}

	/**
	 * Enriches regularization documents by generating unique IDs for newly added
	 * documents.
	 * 
	 * <p>
	 * Only documents without an existing ID will be assigned a new UUID.
	 * </p>
	 * 
	 * @param regularization The regularization entity containing documents
	 */
	private void enrichDocumentsForUpdate(Regularization regularization) {

		if (!CollectionUtils.isEmpty(regularization.getDocuments())) {
			regularization.getDocuments().forEach(document -> {
				// Generate UUID only for new documents (documents without ID)
				if (document.getId() == null) {
					document.setId(UUID.randomUUID().toString());
				}
			});
		}
	}

	/**
	 * Enriches workflow verification documents by generating unique IDs for newly
	 * added documents.
	 * 
	 * <p>
	 * These documents are typically attached during workflow transitions such as
	 * verification, approval, or rejection actions.
	 * </p>
	 * 
	 * @param regularization The regularization entity containing workflow documents
	 */
	private void enrichWorkflowDocumentsForUpdate(Regularization regularization) {

		if (!CollectionUtils.isEmpty(regularization.getWorkflow().getVarificationDocuments())) {
			regularization.getWorkflow().getVarificationDocuments().forEach(document -> {
				// Generate UUID only for new workflow documents
				if (document.getId() == null) {
					document.setId(UUID.randomUUID().toString());
				}
			});
		}
	}

	/**
	 * Enriches DSC (Digital Signature Certificate) details for approval and
	 * rejection actions.
	 * 
	 * <p>
	 * DSC details are created when:
	 * <ul>
	 * <li>An EMPLOYEE or BPA_ARC_APPROVER performs an APPROVE action</li>
	 * <li>An EMPLOYEE or BPA_ARC_APPROVER performs a REJECT action</li>
	 * </ul>
	 * For all other actions, DSC details are set to null.
	 * </p>
	 * 
	 * @param request     The regularization request
	 * @param requestInfo The request info containing user roles
	 */
	private void enrichDscDetailsForUpdate(RegularizationRequest request, RequestInfo requestInfo) {

		Regularization regularization = request.getRegularization();

		// Extract user roles for authorization check
		List<String> roles = requestInfo.getUserInfo().getRoles().stream().map(role -> role.getCode())
				.collect(Collectors.toList());

		String workflowAction = regularization.getWorkflow().getAction();
		boolean isEmployeeOrApprover = roles.contains("EMPLOYEE") || roles.contains("BPA_ARC_APPROVER");
		boolean hasValidStatus = regularization.getStatus() != null;

		// Check if DSC details should be created for approval action
		if (hasValidStatus && isEmployeeOrApprover && workflowAction.equalsIgnoreCase("APPROVE")) {
			createDscDetails(request, requestInfo);
		}
		// Check if DSC details should be created for rejection action
		else if (hasValidStatus && isEmployeeOrApprover && workflowAction.equalsIgnoreCase("REJECT")) {
			createDscDetails(request, requestInfo);
		}
		// Clear DSC details for all other actions
		else {
			regularization.setDscDetails(null);
		}
	}

	/**
	 * Creates and sets DSC (Digital Signature Certificate) details on the
	 * regularization.
	 * 
	 * <p>
	 * DSC details capture the approver information for digital signature
	 * integration.
	 * </p>
	 * 
	 * @param request     The regularization request to update
	 * @param requestInfo The request info containing approver details
	 */
	private void createDscDetails(RegularizationRequest request, RequestInfo requestInfo) {

		List<RegularizationDscDetails> dscDetailsList = new ArrayList<>();

		RegularizationDscDetails dscDetails = new RegularizationDscDetails();
		dscDetails.setTenantId(request.getRegularization().getTenantId());
		dscDetails.setId(UUID.randomUUID().toString());
		dscDetails.setApprovedBy(requestInfo.getUserInfo().getUuid());

		dscDetailsList.add(dscDetails);
		request.getRegularization().setDscDetails(dscDetailsList);
	}

	/**
	 * Enrich updated plot and newly added plot
	 * 
	 * @param request
	 * @param auditDetails
	 */
	private void enrichPlotInfoDetails(RegularizationRequest request, AuditDetails auditDetails) {

		List<PlotInfo> plotInfo = request.getRegularization().getLandRegularizationInfo().getPlotInfo();

		// Existing plot to be update
		List<PlotInfo> plotToBeUpdate = plotInfo.stream().filter(plot -> StringUtils.hasText(plot.getId()))
				.collect(Collectors.toList());

		// enrich update plot info object
		plotToBeUpdate.forEach(plot -> {
			plot.setAuditDetails(auditDetails);
		});

		request.getRegularization().getLandRegularizationInfo().setPlotInfo(plotToBeUpdate);

		// Newly added plot to be insert
		List<PlotInfo> plotToBeInsert = plotInfo.stream().filter(plot -> StringUtils.isEmpty(plot.getId()))
				.collect(Collectors.toList());

		// enrich data newly added plot info object
		plotToBeInsert.forEach(plot -> {
			plot.setId(UUID.randomUUID().toString());
			plot.setTenantId(request.getRegularization().getTenantId());
			plot.setLandInfoId(request.getRegularization().getLandRegularizationInfo().getId());
			plot.setAuditDetails(auditDetails);
		});

		request.getRegularization().getLandRegularizationInfo().setNewPlotInfo(plotToBeInsert);

	}

	/**
	 * Enriches workflow assignees based on the current workflow action being
	 * performed.
	 * 
	 * <p>
	 * This method determines who should be assigned to handle the application next
	 * based on the workflow action. The assignment logic varies by action:
	 * <ul>
	 * <li><b>SENDBACKTOCITIZEN:</b> Assigns to all owners, account creator, and
	 * registered users</li>
	 * <li><b>SEND_TO_CITIZEN:</b> Assigns to all owners and registered users</li>
	 * <li><b>SEND_TO_ARCHITECT / CITIZEN_APPROVAL_INPROCESS + APPROVE:</b> Assigns
	 * to the application creator</li>
	 * <li><b>Other actions:</b> Uses assignees provided in the request</li>
	 * </ul>
	 * </p>
	 * 
	 * @param regularization The regularization entity containing workflow and owner
	 *                       information
	 */
	public void enrichAssignes(Regularization regularization) {

		Workflow wf = regularization.getWorkflow();

		// Step 1: Initialize assignees set with any assignees already in the request
		Set<String> assignees = initializeAssigneesFromRequest(regularization);

		// Step 2: Enrich assignees based on the workflow action
		if (wf != null) {
			String action = wf.getAction();

			if (action.equalsIgnoreCase(BPAConstants.ACTION_SENDBACKTOCITIZEN)) {
				// Send back to citizen - include owners, creator, and registered users
				enrichAssigneesForSendBackToCitizen(regularization, assignees);

			} else if (action.equalsIgnoreCase(BPAConstants.ACTION_SEND_TO_CITIZEN)) {
				// Send to citizen - include owners and registered users
				enrichAssigneesForSendToCitizen(regularization, assignees);

			} else if (isArchitectOrCitizenApprovalAction(regularization, action)) {
				// Send to architect or citizen approval - assign to application creator
				enrichAssigneesForSendToArchitect(regularization, assignees);
			}
		}

		// Step 3: Set the final assignees on the workflow
		setAssigneesOnWorkflow(regularization, assignees);
	}

	/**
	 * Initializes the assignees set with any assignees already present in the
	 * workflow request.
	 * 
	 * @param regularization The regularization entity
	 * @return A mutable set containing initial assignees from the request
	 */
	private Set<String> initializeAssigneesFromRequest(Regularization regularization) {

		Set<String> assignees = new HashSet<>();

		// Add any assignees already specified in the workflow request
		if (regularization.getWorkflow() != null && regularization.getWorkflow().getAssignes() != null) {
			assignees.addAll(regularization.getWorkflow().getAssignes());
		}

		return assignees;
	}

	/**
	 * Enriches assignees for the SENDBACKTOCITIZEN action.
	 * 
	 * <p>
	 * Includes the following in the assignees list:
	 * <ul>
	 * <li>All application owners</li>
	 * <li>The account creator (if available)</li>
	 * <li>All registered users associated with the application</li>
	 * </ul>
	 * </p>
	 * 
	 * @param regularization The regularization entity
	 * @param assignees      The set to add assignees to
	 */
	private void enrichAssigneesForSendBackToCitizen(Regularization regularization, Set<String> assignees) {

		// Add all application owners
		addOwnerUuidsToAssignees(regularization, assignees);

		// Add the account creator/application submitter
		if (regularization.getAccountId() != null) {
			assignees.add(regularization.getAccountId());
		}

		// Add all registered users (e.g., architects, technical persons)
		addRegisteredUsersToAssignees(regularization, assignees);
	}

	/**
	 * Enriches assignees for the SEND_TO_CITIZEN action.
	 * 
	 * <p>
	 * Includes the following in the assignees list:
	 * <ul>
	 * <li>All application owners</li>
	 * <li>All registered users associated with the application</li>
	 * </ul>
	 * </p>
	 * 
	 * @param regularization The regularization entity
	 * @param assignees      The set to add assignees to
	 */
	private void enrichAssigneesForSendToCitizen(Regularization regularization, Set<String> assignees) {

		// Add all application owners
		addOwnerUuidsToAssignees(regularization, assignees);

		// Add all registered users (e.g., architects, technical persons)
		addRegisteredUsersToAssignees(regularization, assignees);
	}

	/**
	 * Enriches assignees for the SEND_TO_ARCHITECT action or citizen approval
	 * scenarios.
	 * 
	 * <p>
	 * Assigns the application to its original creator (account ID).
	 * </p>
	 * 
	 * @param regularization The regularization entity
	 * @param assignees      The set to add assignees to
	 */
	private void enrichAssigneesForSendToArchitect(Regularization regularization, Set<String> assignees) {

		// Assign to the application creator
		if (regularization.getAccountId() != null) {
			assignees.add(regularization.getAccountId());
		}
	}

	/**
	 * Checks if the action is SEND_TO_ARCHITECT or a citizen approval in-process
	 * approval action.
	 * 
	 * @param regularization The regularization entity
	 * @param action         The workflow action being performed
	 * @return true if the action requires assignment to architect/creator
	 */
	private boolean isArchitectOrCitizenApprovalAction(Regularization regularization, String action) {

		// Check for SEND_TO_ARCHITECT action
		boolean isSendToArchitect = action.equalsIgnoreCase(BPAConstants.ACTION_SEND_TO_ARCHITECT);

		// Check for citizen approval in-process with APPROVE action
		boolean isCitizenApprovalApprove = regularization.getStatus() != null
				&& regularization.getStatus().equalsIgnoreCase(BPAConstants.STATUS_CITIZEN_APPROVAL_INPROCESS)
				&& action.equalsIgnoreCase(BPAConstants.ACTION_APPROVE);

		return isSendToArchitect || isCitizenApprovalApprove;
	}

	/**
	 * Adds all owner UUIDs from the regularization to the assignees set.
	 * 
	 * @param regularization The regularization entity containing owners
	 * @param assignees      The set to add owner UUIDs to
	 */
	private void addOwnerUuidsToAssignees(Regularization regularization, Set<String> assignees) {

		if (!CollectionUtils.isEmpty(regularization.getOwners())) {
			regularization.getOwners().forEach(ownerInfo -> {
				assignees.add(ownerInfo.getUuid());
			});
		}
	}

	/**
	 * Adds registered user UUIDs (architects, technical persons) to the assignees
	 * set.
	 * 
	 * @param regularization The regularization entity
	 * @param assignees      The set to add registered user UUIDs to
	 */
	private void addRegisteredUsersToAssignees(Regularization regularization, Set<String> assignees) {

		Set<String> registeredUuids = userService.getUUidFromUserName(regularization);

		if (!CollectionUtils.isEmpty(registeredUuids)) {
			assignees.addAll(registeredUuids);
		}
	}

	/**
	 * Sets the final assignees on the workflow object.
	 * 
	 * <p>
	 * Creates a new workflow if one doesn't exist, otherwise updates the existing
	 * workflow's assignees list.
	 * </p>
	 * 
	 * @param regularization The regularization entity
	 * @param assignees      The final set of assignees to set
	 */
	private void setAssigneesOnWorkflow(Regularization regularization, Set<String> assignees) {

		if (regularization.getWorkflow() == null) {
			// Create new workflow with assignees
			Workflow wfNew = new Workflow();
			wfNew.setAssignes(new LinkedList<>(assignees));
			regularization.setWorkflow(wfNew);
		} else {
			// Update existing workflow's assignees
			regularization.getWorkflow().setAssignes(new LinkedList<>(assignees));
		}
	}

	/**
	 * Generate approval No here and set application date if needed
	 * 
	 * @param regularizationRequest
	 * @param mdmsData
	 */
	public void postStatusEnrichment(@Valid RegularizationRequest regularizationRequest, Object mdmsData) {

		Regularization regularization = regularizationRequest.getRegularization();

		BusinessService businessService = workflowService.getBusinessService(regularization,
				regularizationRequest.getRequestInfo(), regularization.getApplicationNo());

		log.info("Application status is : " + regularization.getStatus());
		String state = workflowService.getCurrentState(regularization.getStatus(), businessService);

		if ((state.equalsIgnoreCase(BPAConstants.DOCVERIFICATION_STATE)
				&& (regularization.getApplicationDate() == null || regularization.getApplicationDate() == 0))) {
			regularization.setApplicationDate(Calendar.getInstance().getTimeInMillis());
		}

		log.info("Application state is : " + state);
		generateApprovalNo(regularizationRequest, state);
	}

	private void generateApprovalNo(@Valid RegularizationRequest regularizationRequest, String state) {

		Regularization regularization = regularizationRequest.getRegularization();

		if (regularization.getStatus().equalsIgnoreCase(RegularizationConstants.APPROVED)) {
			List<IdResponse> idResponses = idGenRepository
					.getId(regularizationRequest.getRequestInfo(), regularization.getTenantId(),
							config.getRegularizationPermitName(), config.getRegularizationPermitFormat(), 1)
					.getIdResponses();
			regularization.setApprovalNo(idResponses.get(0).getId());
		}

	}

	/**
	 * Enriches DSC (Digital Signature Certificate) details for a regularization
	 * update request.
	 * 
	 * <p>
	 * This method is called when an application is digitally signed during the
	 * approval process. It performs the following enrichment operations:
	 * <ol>
	 * <li>Updates audit details with last modified information while preserving
	 * created details</li>
	 * <li>Sets the approval date to the current timestamp (when digital signature
	 * is applied)</li>
	 * <li>Calculates and sets the validity date based on configured validity
	 * period</li>
	 * </ol>
	 * </p>
	 * 
	 * @param regularizationRequest The regularization request containing the
	 *                              application to update
	 * @param regularization        The existing regularization entity with original
	 *                              audit details
	 */
	@SuppressWarnings("unchecked")
	public void enrichRegularizationDscDetailsUpdateRequest(RegularizationRequest regularizationRequest,
			Regularization regularization) {

		// Step 1: Update audit details preserving original creation metadata
		AuditDetails auditDetails = updateDscAuditDetails(regularizationRequest, regularization);

		// Step 2: Set approval date to the digital signature timestamp
		setApprovalDate(regularizationRequest, auditDetails);

		// Step 3: Calculate and set validity date in additional details
		calculateAndSetValidityDate(regularizationRequest);
	}

	/**
	 * Updates audit details for DSC update request while preserving original
	 * creation metadata.
	 * 
	 * <p>
	 * Creates new audit details with current timestamp for last modified, but
	 * retains the original created by and created time from the existing
	 * regularization.
	 * </p>
	 * 
	 * @param regularizationRequest The regularization request to update
	 * @param regularization        The existing regularization with original audit
	 *                              details
	 * @return The updated audit details object
	 */
	private AuditDetails updateDscAuditDetails(RegularizationRequest regularizationRequest,
			Regularization regularization) {

		RequestInfo requestInfo = regularizationRequest.getRequestInfo();

		// Create audit details for update operation (isCreate = false)
		AuditDetails auditDetails = util.getAuditDetails(requestInfo.getUserInfo().getUuid(), false);

		// Preserve original creation metadata from existing regularization
		auditDetails.setCreatedBy(regularization.getAuditDetails().getCreatedBy());
		auditDetails.setCreatedTime(regularization.getAuditDetails().getCreatedTime());

		// Set the updated audit details on the request
		regularizationRequest.getRegularization().setAuditDetails(auditDetails);

		return auditDetails;
	}

	/**
	 * Sets the approval date on the regularization when digital signature is
	 * applied.
	 * 
	 * <p>
	 * The approval date is set to the last modified time from audit details, which
	 * represents the timestamp when the digital signature was applied. This ensures
	 * the approval date reflects the actual signing moment rather than payment
	 * time.
	 * </p>
	 * 
	 * @param regularizationRequest The regularization request to update
	 * @param auditDetails          The audit details containing the last modified
	 *                              timestamp
	 */
	private void setApprovalDate(RegularizationRequest regularizationRequest, AuditDetails auditDetails) {

		// Set approval date to the digital signature timestamp
		regularizationRequest.getRegularization().setApprovalDate(auditDetails.getLastModifiedTime());
	}

	/**
	 * Calculates the validity date and stores it in the regularization's additional
	 * details.
	 * 
	 * <p>
	 * The validity date is calculated by adding the configured validity period (in
	 * months) to the current date. This determines how long the approved
	 * regularization remains valid.
	 * </p>
	 * 
	 * @param regularizationRequest The regularization request to update with
	 *                              validity date
	 */
	@SuppressWarnings("unchecked")
	private void calculateAndSetValidityDate(RegularizationRequest regularizationRequest) {

		// Calculate validity date by adding configured months to current date
		Calendar calendar = util.getCalendarInstaceAfter(config.getValidityInMonths());

		// Get or create additional details map
		Map<String, Object> additionalDetail = getOrCreateAdditionalDetails(regularizationRequest);

		// Store validity date as epoch milliseconds
		additionalDetail.put("validityDate", calendar.getTimeInMillis());
	}

	/**
	 * Gets existing additional details map or creates a new one if not present.
	 * 
	 * @param regularizationRequest The regularization request
	 * @return The additional details map (existing or newly created)
	 */
	@SuppressWarnings("unchecked")
	private Map<String, Object> getOrCreateAdditionalDetails(RegularizationRequest regularizationRequest) {

		Regularization regularization = regularizationRequest.getRegularization();
		Map<String, Object> additionalDetail;

		if (regularization.getAdditionalDetails() != null) {
			// Use existing additional details map
			additionalDetail = (Map<String, Object>) regularization.getAdditionalDetails();
		} else {
			// Create new additional details map and set it on regularization
			additionalDetail = new HashMap<String, Object>();
			regularization.setAdditionalDetails(additionalDetail);
		}

		return additionalDetail;
	}

	/**
	 * Enrich Regularization create request
	 * 
	 * @param request
	 */
	public void enrichScnCreateRequest(@Valid NoticeRequest request) {

		RequestInfo requestInfo = request.getRequestInfo();
		AuditDetails auditDetails = util.getAuditDetails(requestInfo.getUserInfo().getUuid(), true);
		Map<String, String> errorMap = new HashMap<>();

		if (request.getnotice().getLetterNo() != null && request.getnotice().getBusinessid() != null
				&& request.getnotice().getLetterType() != null && request.getnotice().getFilestoreid() != null
				&& request.getnotice().getTenantid() != null) {
			request.getnotice().setAuditDetails(auditDetails);
			request.getnotice().setId(UUID.randomUUID().toString());
			request.getnotice().setReminderCount(0);

		} else {
			errorMap.put("NOTICE_CREATE_ERROR", "Please provide valid details to create a notice.");
		}
		if (!errorMap.isEmpty())
			throw new CustomException(errorMap);

	}

	/**
	 * Enrich Regularization Update request
	 * 
	 * @param request
	 */
	public void enrichScnUpdateRequest(@Valid NoticeRequest request) {

		RequestInfo requestInfo = request.getRequestInfo();
		Map<String, String> errorMap = new HashMap<>();
		AuditDetails auditDetails = util.getAuditDetails(requestInfo.getUserInfo().getUuid(), true);
		if (request.getnotice().getLetterNo() != null && request.getnotice().getBusinessid() != null
				&& request.getnotice().getLetterType() != null && request.getnotice().getFilestoreid() != null
				&& request.getnotice().getTenantid() != null) {
			request.getnotice().setAuditDetails(auditDetails);
			// request.getnotice().setId(UUID.randomUUID().toString());
		} else {
			errorMap.put("NoticeUpdateError", "please provide valid details to update a  notice.");
		}
		if (!errorMap.isEmpty())
			throw new CustomException(errorMap);

	}

	/**
	 * Enrich building create request here
	 * 
	 * @param requestInfo
	 * @param regularization
	 */
	/**
	 * Enriches the required data for a Building Regularization create request.
	 * 
	 * <p>
	 * This method orchestrates the enrichment process for building regularization
	 * by performing:
	 * <ol>
	 * <li>Sets audit details (created by, created time) from the request user</li>
	 * <li>Generates unique identifiers for regularization and land info
	 * entities</li>
	 * <li>Enriches land info and plot info with required metadata</li>
	 * <li>Generates application number using ID generation service</li>
	 * <li>Determines the applicable business service based on plot area and
	 * building height</li>
	 * <li>Enriches owner details by creating/updating user records</li>
	 * <li>Enriches building-specific data (building info, blocks, etc.)</li>
	 * </ol>
	 * </p>
	 * 
	 * @param requestInfo    The request information containing user context and
	 *                       auth details
	 * @param regularization The regularization entity to be enriched
	 */
	public void enrichBuildingCreateRequest(RequestInfo requestInfo, Regularization regularization) {

		// Step 1: Create audit details from the authenticated user
		AuditDetails auditDetails = util.getAuditDetails(requestInfo.getUserInfo().getUuid(), true);

		// Step 2: Enrich regularization with audit details and account information
		enrichRegularizationAuditDetails(regularization, auditDetails, requestInfo);

		// Step 3: Generate and set unique identifiers for regularization and land info
		String regularizationUniqueId = UUID.randomUUID().toString();
		String landInfoUniqueId = UUID.randomUUID().toString();
		setUniqueIdentifiers(regularization, regularizationUniqueId, landInfoUniqueId);

		// Step 4: Enrich plot info with required metadata
		enrichPlotInfoForCreate(regularization, landInfoUniqueId, auditDetails);

		// Step 5: Generate application number from ID generation service
		String applicationNo = generateApplicationNumber(requestInfo, regularization.getTenantId());
		regularization.setApplicationNo(applicationNo);

		// Step 6: Determine and set the applicable business service based on plot area
		// and building height
		populateBusinessServiceForBuilding(regularization);

		// Step 7: Enrich owner details (create/update user records)
		enrichOwnerDetails(requestInfo, regularization);

		// Step 8: Enrich building-specific data (building info, building blocks)
		enrichBuildingData(regularization, auditDetails);
	}

	/**
	 * Enriches building-specific data required for the persister.
	 * 
	 * <p>
	 * This method sets up the building regularization info entity with:
	 * <ul>
	 * <li>Unique identifier for the building info record</li>
	 * <li>Foreign key reference to the parent regularization</li>
	 * <li>Audit details for tracking creation metadata</li>
	 * </ul>
	 * </p>
	 * 
	 * @param regularization The regularization entity containing building info
	 * @param auditDetails   The audit details to set on building info
	 */
	private void enrichBuildingData(Regularization regularization, AuditDetails auditDetails) {

		// Generate unique identifier for building info
		String buildingInfoId = UUID.randomUUID().toString();
		regularization.getBuildingRegularizationInfo().setId(buildingInfoId);

		// Establish foreign key relationship to parent regularization
		String regularizationId = regularization.getId();
		regularization.getBuildingRegularizationInfo().setRegularizationId(regularizationId);

		// Set audit details for tracking
		regularization.getBuildingRegularizationInfo().setAuditDetails(auditDetails);
	}

	/**
	 * Determines and sets the appropriate business service (workflow path) for
	 * Building Regularization.
	 * 
	 * <p>
	 * The business service is determined based on the following criteria:
	 * <ul>
	 * <li><b>Plot Area:</b> Total plot area determines the base workflow tier</li>
	 * <li><b>Building Height:</b> Maximum building height may escalate to a higher
	 * tier</li>
	 * <li><b>Special Building:</b> Presence of special buildings may require
	 * elevated approval</li>
	 * </ul>
	 * </p>
	 * 
	 * <p>
	 * Business Service Tiers:
	 * <ul>
	 * <li><b>SERVICE_1:</b> Basic approval (small plot, low height, no special
	 * buildings)</li>
	 * <li><b>SERVICE_2:</b> Standard approval (medium complexity)</li>
	 * <li><b>SERVICE_3:</b> Enhanced approval (larger area or taller
	 * buildings)</li>
	 * <li><b>SERVICE_4:</b> Full approval (largest area or tallest buildings)</li>
	 * </ul>
	 * </p>
	 * 
	 * @param regularization The regularization entity to set business service on
	 */
	@SuppressWarnings("unchecked")
	private void populateBusinessServiceForBuilding(Regularization regularization) {

		// Extract building characteristics from building blocks
		BuildingCharacteristics characteristics = extractBuildingCharacteristics(regularization);

		// Calculate total plot area
		BigDecimal totalPlotArea = util.getNetPlotArea(regularization.getLandRegularizationInfo().getPlotInfo());

		// Determine and set the appropriate business service based on area and height
		String businessService = determineBusinessService(totalPlotArea, characteristics);
		regularization.setBusinessService(businessService);
	}

	/**
	 * Extracts building characteristics (max height, special building presence)
	 * from building blocks.
	 * 
	 * @param regularization The regularization entity containing building blocks
	 * @return BuildingCharacteristics object with extracted values
	 */
	@SuppressWarnings("unchecked")
	private BuildingCharacteristics extractBuildingCharacteristics(Regularization regularization) {

		List<Map<String, Object>> buildingBlocks = (List<Map<String, Object>>) regularization
				.getBuildingRegularizationInfo().getBuildingBlocks();

		BigDecimal maxBuildingHeight = BigDecimal.ZERO;
		boolean isSpecialBuildingPresent = false;

		// Iterate through all building blocks to find max height and special buildings
		for (Map<String, Object> buildingBlock : buildingBlocks) {

			// Check if this block is a special building
			if ((boolean) buildingBlock.get(RegularizationConstants.IS_SPECIAL_BUILDING)) {
				isSpecialBuildingPresent = true;
			}

			// Track the maximum building height across all blocks
			BigDecimal height = new BigDecimal((String) buildingBlock.get(RegularizationConstants.BUILDING_HEIGHT));
			if (height.compareTo(maxBuildingHeight) > 0) {
				maxBuildingHeight = height;
			}
		}

		return new BuildingCharacteristics(maxBuildingHeight, isSpecialBuildingPresent);
	}

	/**
	 * Determines the appropriate business service based on plot area and building
	 * characteristics.
	 * 
	 * <p>
	 * Decision matrix:
	 * <table border="1">
	 * <tr>
	 * <th>Plot Area</th>
	 * <th>Height/Special</th>
	 * <th>Service</th>
	 * </tr>
	 * <tr>
	 * <td>≤500 sqm</td>
	 * <td>No special, ≤10m</td>
	 * <td>SERVICE_1</td>
	 * </tr>
	 * <tr>
	 * <td>≤500 sqm</td>
	 * <td>Special or 10-15m</td>
	 * <td>SERVICE_2</td>
	 * </tr>
	 * <tr>
	 * <td>≤500 sqm</td>
	 * <td>15-30m</td>
	 * <td>SERVICE_3</td>
	 * </tr>
	 * <tr>
	 * <td>≤500 sqm</td>
	 * <td>&gt;30m</td>
	 * <td>SERVICE_4</td>
	 * </tr>
	 * <tr>
	 * <td>500 sqm - 1 acre</td>
	 * <td>≤15m</td>
	 * <td>SERVICE_2</td>
	 * </tr>
	 * <tr>
	 * <td>500 sqm - 1 acre</td>
	 * <td>15-30m</td>
	 * <td>SERVICE_3</td>
	 * </tr>
	 * <tr>
	 * <td>500 sqm - 1 acre</td>
	 * <td>&gt;30m</td>
	 * <td>SERVICE_4</td>
	 * </tr>
	 * <tr>
	 * <td>1 acre - 1 hectare</td>
	 * <td>≤30m</td>
	 * <td>SERVICE_3</td>
	 * </tr>
	 * <tr>
	 * <td>1 acre - 1 hectare</td>
	 * <td>&gt;30m</td>
	 * <td>SERVICE_4</td>
	 * </tr>
	 * <tr>
	 * <td>&gt;1 hectare</td>
	 * <td>Any</td>
	 * <td>SERVICE_4</td>
	 * </tr>
	 * </table>
	 * </p>
	 * 
	 * @param totalPlotArea   The total plot area
	 * @param characteristics The building characteristics (height, special
	 *                        building)
	 * @return The appropriate business service code
	 */
	private String determineBusinessService(BigDecimal totalPlotArea, BuildingCharacteristics characteristics) {

		BigDecimal maxHeight = characteristics.getMaxBuildingHeight();
		boolean isSpecialBuilding = characteristics.isSpecialBuildingPresent();

		// Category 1: Small plots (≤500 sqm)
		if (totalPlotArea.compareTo(RegularizationConstants.FIVE_HUNDRED_SQMT) <= 0) {
			return determineServiceForSmallPlot(maxHeight, isSpecialBuilding);
		}

		// Category 2: Medium plots (500 sqm - 1 acre)
		if (totalPlotArea.compareTo(RegularizationConstants.ONE_ACRE) <= 0) {
			return determineServiceForMediumPlot(maxHeight);
		}

		// Category 3: Large plots (1 acre - 1 hectare)
		if (totalPlotArea.compareTo(RegularizationConstants.ONE_HECTARE) <= 0) {
			return determineServiceForLargePlot(maxHeight);
		}

		// Category 4: Very large plots (>1 hectare) - always requires highest tier
		return RegularizationConstants.BUILDING_BUSSINESS_SERVICE_4;
	}

	/**
	 * Determines business service for small plots (≤500 sqm).
	 * 
	 * @param maxHeight         Maximum building height
	 * @param isSpecialBuilding Whether special building is present
	 * @return The appropriate business service code
	 */
	private String determineServiceForSmallPlot(BigDecimal maxHeight, boolean isSpecialBuilding) {

		// Height > 30m always requires SERVICE_4
		if (maxHeight.compareTo(RegularizationConstants.THIRTY_METERS) > 0) {
			return RegularizationConstants.BUILDING_BUSSINESS_SERVICE_4;
		}

		// Height 15-30m requires SERVICE_3
		if (maxHeight.compareTo(RegularizationConstants.FIFTEEN_METERS) > 0) {
			return RegularizationConstants.BUILDING_BUSSINESS_SERVICE_3;
		}

		// Height 10-15m requires SERVICE_2
		if (maxHeight.compareTo(RegularizationConstants.TEN_METERS) > 0) {
			return RegularizationConstants.BUILDING_BUSSINESS_SERVICE_2;
		}

		// Special building requires SERVICE_2, otherwise SERVICE_1
		return isSpecialBuilding ? RegularizationConstants.BUILDING_BUSSINESS_SERVICE_2
				: RegularizationConstants.BUILDING_BUSSINESS_SERVICE_1;
	}

	/**
	 * Determines business service for medium plots (500 sqm - 1 acre).
	 * 
	 * @param maxHeight Maximum building height
	 * @return The appropriate business service code
	 */
	private String determineServiceForMediumPlot(BigDecimal maxHeight) {

		// Height > 30m requires SERVICE_4
		if (maxHeight.compareTo(RegularizationConstants.THIRTY_METERS) > 0) {
			return RegularizationConstants.BUILDING_BUSSINESS_SERVICE_4;
		}

		// Height 15-30m requires SERVICE_3
		if (maxHeight.compareTo(RegularizationConstants.FIFTEEN_METERS) > 0) {
			return RegularizationConstants.BUILDING_BUSSINESS_SERVICE_3;
		}

		// Default for medium plots is SERVICE_2
		return RegularizationConstants.BUILDING_BUSSINESS_SERVICE_2;
	}

	/**
	 * Determines business service for large plots (1 acre - 1 hectare).
	 * 
	 * @param maxHeight Maximum building height
	 * @return The appropriate business service code
	 */
	private String determineServiceForLargePlot(BigDecimal maxHeight) {

		// Height > 30m requires SERVICE_4
		if (maxHeight.compareTo(RegularizationConstants.THIRTY_METERS) > 0) {
			return RegularizationConstants.BUILDING_BUSSINESS_SERVICE_4;
		}

		// Default for large plots is SERVICE_3
		return RegularizationConstants.BUILDING_BUSSINESS_SERVICE_3;
	}

	/**
	 * Inner class to hold building characteristics extracted from building blocks.
	 */
	private static class BuildingCharacteristics {
		private final BigDecimal maxBuildingHeight;
		private final boolean specialBuildingPresent;

		public BuildingCharacteristics(BigDecimal maxBuildingHeight, boolean specialBuildingPresent) {
			this.maxBuildingHeight = maxBuildingHeight;
			this.specialBuildingPresent = specialBuildingPresent;
		}

		public BigDecimal getMaxBuildingHeight() {
			return maxBuildingHeight;
		}

		public boolean isSpecialBuildingPresent() {
			return specialBuildingPresent;
		}
	}

	/**
	 * Enrich create field inspection report
	 * 
	 * @param request
	 */
	public void enrichCreateFieldInspectionReport(@Valid RegularizationFieldInspectionRequest request) {
		request.getFieldInspection().setId(UUID.randomUUID().toString());
		RequestInfo requestInfo = request.getRequestInfo();
		AuditDetails auditDetails = util.getAuditDetails(requestInfo.getUserInfo().getUuid(), true);
		request.getFieldInspection().setAuditDetails(auditDetails);
	}

	/**
	 * Enrich create for Regularization Document Remark
	 * 
	 * @param request
	 */
	public void enrichDocRemarkCreateRequest(@Valid RegularizationDocRemarkRequest request) {
		log.info(" Inside enrich DocRemark CreateRequest ");
		RequestInfo requestInfo = request.getRequestInfo();
		AuditDetails auditDetails = util.getAuditDetails(requestInfo.getUserInfo().getUuid(), true);
		request.getDocRemark().setAuditDetails(auditDetails);
		request.getDocRemark().setId(UUID.randomUUID().toString());
	}

	/**
	 * Enrich update for Regularization Document Remark
	 * 
	 * @param request
	 */
	public void enrichDocRemarkUpdateRequest(@Valid RegularizationDocRemarkRequest request) {
		RequestInfo requestInfo = request.getRequestInfo();
		AuditDetails auditDetails = util.getAuditDetails(requestInfo.getUserInfo().getUuid(), false);
		request.getDocRemark().getAuditDetails().setLastModifiedTime(auditDetails.getLastModifiedTime());
		request.getDocRemark().getAuditDetails().setLastModifiedBy(auditDetails.getLastModifiedBy());
	}

	/**
	 * Enrich update request for Field Inspection
	 * 
	 * @param request
	 */
	public void enrichUpdateFieldInspectionReport(@Valid RegularizationFieldInspectionRequest request) {

		RequestInfo requestInfo = request.getRequestInfo();
		AuditDetails auditDetails = util.getAuditDetails(requestInfo.getUserInfo().getUuid(), false);
		request.getFieldInspection().setAuditDetails(auditDetails);

	}

	/**
	 * Enriches a Regularization Draft request with required metadata for saving.
	 * 
	 * <p>
	 * This method orchestrates the draft enrichment process by performing:
	 * <ol>
	 * <li>Generates a unique draft number if not already present</li>
	 * <li>Creates and sets audit details for tracking creation metadata</li>
	 * <li>Generates unique identifier and sets initial status</li>
	 * </ol>
	 * </p>
	 * 
	 * @param request The regularization draft request to be enriched
	 */
	public void enrichRegularizationSaveDraft(@Valid RegularizationDraftRequest request) {

		RequestInfo requestInfo = request.getRequestInfo();
		RegularizationDraft draft = request.getRegularizationDraft();

		// Step 1: Generate draft number if not already assigned
		generateDraftNumberIfRequired(requestInfo, draft);

		// Step 2: Create and set audit details for the draft
		enrichDraftAuditDetails(requestInfo, draft);

		// Step 3: Set unique identifier and initial status
		setDraftIdentifiers(draft);
	}

	/**
	 * Generates a unique draft number using ID generation service if not already
	 * present.
	 * 
	 * <p>
	 * Draft numbers are only generated for new drafts. If the draft already has a
	 * draft number assigned, this method does nothing.
	 * </p>
	 * 
	 * @param requestInfo The request info for ID generation API call
	 * @param draft       The regularization draft to set the draft number on
	 */
	private void generateDraftNumberIfRequired(RequestInfo requestInfo, RegularizationDraft draft) {

		// Check if draft number is missing or empty
		boolean isDraftNumberMissing = draft.getDraftNo() == null || StringUtils.isEmpty(draft.getDraftNo());

		if (isDraftNumberMissing) {
			// Call ID generation service to get a unique draft number
			List<IdResponse> idResponses = idGenRepository.getId(requestInfo, draft.getTenantId(),
					config.getRegularizationDraftFormat(), config.getRegularizationDraftFormat(), 1).getIdResponses();

			// Set the generated draft number (trimmed to remove any whitespace)
			draft.setDraftNo(idResponses.get(0).getId().trim());
		}
	}

	/**
	 * Creates and sets audit details on the regularization draft.
	 * 
	 * @param requestInfo The request info containing user context
	 * @param draft       The regularization draft to set audit details on
	 */
	private void enrichDraftAuditDetails(RequestInfo requestInfo, RegularizationDraft draft) {

		// Create audit details for new draft (isCreate = true)
		AuditDetails auditDetails = util.getAuditDetails(requestInfo.getUserInfo().getUuid(), true);
		draft.setAuditDetails(auditDetails);
	}

	/**
	 * Sets unique identifier and initial status on the regularization draft.
	 * 
	 * <p>
	 * Assigns a new UUID as the draft's primary key and sets the initial status to
	 * "CREATED" indicating a newly saved draft.
	 * </p>
	 * 
	 * @param draft The regularization draft to set identifiers on
	 */
	private void setDraftIdentifiers(RegularizationDraft draft) {

		// Generate and set unique identifier for the draft
		draft.setId(UUID.randomUUID().toString());

		// Set initial status to indicate a newly created draft
		draft.setStatus("CREATED");
	}

}
