package org.egov.bpa.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.validation.Valid;

import org.egov.bpa.repository.DemolitionRepository;
import org.egov.bpa.util.BPAConstants;
import org.egov.bpa.util.DemolitionConstants;
import org.egov.bpa.util.DemolitionUtil;
import org.egov.bpa.util.RegularizationConstants;
import org.egov.bpa.validator.DemolitionValidator;
import org.egov.bpa.web.model.AuditDetails;
import org.egov.bpa.web.model.BPAVillage;
import org.egov.bpa.web.model.VillageSearchCriteria;
import org.egov.bpa.web.model.demolition.Demolition;
import org.egov.bpa.web.model.demolition.DemolitionActionValidator;
import org.egov.bpa.web.model.demolition.DemolitionApprovedByApplicationSearch;
import org.egov.bpa.web.model.demolition.DemolitionCalculationService;
import org.egov.bpa.web.model.demolition.DemolitionDocumentList;
import org.egov.bpa.web.model.demolition.DemolitionFISearchCriteria;
import org.egov.bpa.web.model.demolition.DemolitionFieldInspection;
import org.egov.bpa.web.model.demolition.DemolitionFieldInspectionRequest;
import org.egov.bpa.web.model.demolition.DemolitionRequest;
import org.egov.bpa.web.model.demolition.DemolitionSearchCriteria;
import org.egov.bpa.web.model.user.UserDetailResponse;
import org.egov.bpa.web.model.workflow.Action;
import org.egov.bpa.web.model.workflow.BusinessService;
import org.egov.bpa.web.model.workflow.ProcessInstance;
import org.egov.common.contract.request.RequestInfo;
import org.egov.common.contract.request.Role;
import org.egov.tracer.model.CustomException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class DemolitionService {

	@Autowired
	private DemolitionValidator validator;

	@Autowired
	private DemolitionEnrichmentService enrichmentService;

	@Autowired
	private DemolitionWorkflowService workflowService;

	@Autowired
	private DemolitionRepository repository;

	@Autowired
	private DemolitionUtil util;

	@Autowired
	private DemolitionActionValidator actionValidator;

	@Autowired
	private DemolitionCalculationService calculationService;

	@Autowired
	private DemolitionUserService userService;

	/**
	 * Service layer to create Demolition Application
	 * 
	 * @param request
	 * @return
	 */
	public Demolition createDemolition(@Valid DemolitionRequest request) {

		// Validate create Request
		validator.validateDemolitionRequest(request);

		// Enrich the required data for create request
		enrichmentService.enrichDemolitionCreateRequest(request);

		// Call workflow Service here
		workflowService.callWorkFlow(request);

		// persist the request through Kafka
		repository.save(request);

		return request.getDemolition();

	}

	/**
	 * Searches for demolition applications based on the provided criteria.
	 * 
	 * <p>
	 * This method performs the following operations:
	 * <ul>
	 * <li>Validates the search request parameters</li>
	 * <li>Determines search strategy based on user role (CITIZEN vs others)</li>
	 * <li>Retrieves demolition applications from the repository</li>
	 * <li>Enriches results with owner details</li>
	 * </ul>
	 * 
	 * <p>
	 * <strong>Business Logic:</strong>
	 * <ul>
	 * <li>Citizens with minimal search criteria only see applications they
	 * created</li>
	 * <li>Other users (officials) can search all applications matching
	 * criteria</li>
	 * <li>Owner details are enriched from user service for all results</li>
	 * </ul>
	 * 
	 * @param criteria    the search criteria containing filters like tenant ID,
	 *                    application number, etc.
	 * @param requestInfo the request information containing user context and
	 *                    authentication details
	 * @return list of demolition applications matching the search criteria with
	 *         enriched owner details
	 */
	public List<Demolition> searchDemolition(@Valid DemolitionSearchCriteria criteria, RequestInfo requestInfo) {

		// Step 1: Validate search request
		validator.validateSearch(requestInfo, criteria);

		// Step 2: Extract user roles from request
		List<String> userRoles = extractUserRoles(requestInfo);

		// Step 3: Determine search strategy and fetch results
		List<Demolition> demolitions = fetchDemolitions(criteria, requestInfo, userRoles);

		// Step 4: Enrich results with owner details
		enrichWithOwnerDetails(demolitions, requestInfo);

		return demolitions;
	}

	/**
	 * Extracts role codes from the user information in the request.
	 * 
	 * @param requestInfo the request information containing user roles
	 * @return list of role codes assigned to the user
	 */
	private List<String> extractUserRoles(RequestInfo requestInfo) {
		List<String> roles = new ArrayList<>();

		if (requestInfo.getUserInfo() != null && !CollectionUtils.isEmpty(requestInfo.getUserInfo().getRoles())) {
			for (Role role : requestInfo.getUserInfo().getRoles()) {
				roles.add(role.getCode());
			}
		}

		return roles;
	}

	/**
	 * Fetches demolition applications based on user role and search criteria.
	 * 
	 * <p>
	 * <strong>Business Logic:</strong>
	 * <ul>
	 * <li>If the user is a CITIZEN and provides minimal criteria (tenant ID only or
	 * empty), return only applications created by that citizen</li>
	 * <li>For all other cases, perform a standard repository search</li>
	 * </ul>
	 * 
	 * @param criteria    the search criteria
	 * @param requestInfo the request information
	 * @param userRoles   the roles assigned to the user
	 * @return list of demolition applications
	 */
	private List<Demolition> fetchDemolitions(DemolitionSearchCriteria criteria, RequestInfo requestInfo,
			List<String> userRoles) {

		// Check if this is a citizen with minimal search criteria
		boolean isCitizenMinimalSearch = (criteria.tenantIdOnly() || criteria.isEmpty())
				&& userRoles.contains(BPAConstants.CITIZEN);

		if (isCitizenMinimalSearch) {
			log.info("Fetching demolitions created by citizen user");
			List<Demolition> result = getDemolitionsCreatedByMe(criteria, requestInfo);
			log.info("Number of demolitions returned: {}", result.size());
			return result;
		} else {
			// Standard search for officials or citizens with specific criteria
			return repository.getDemolitions(criteria);
		}
	}

	/**
	 * Enriches demolition applications with detailed owner information.
	 * 
	 * <p>
	 * This method performs the following operations:
	 * <ul>
	 * <li>Extracts all unique owner UUIDs from the demolition applications</li>
	 * <li>Fetches complete user details from the user service</li>
	 * <li>Maps and enriches owner objects with their full user information</li>
	 * </ul>
	 * 
	 * <p>
	 * <strong>Note:</strong> This method modifies the demolition objects in place,
	 * enriching the owner objects with additional user details without modifying
	 * audit information.
	 * 
	 * @param demolitions the list of demolition applications to enrich
	 * @param requestInfo the request information for user service authentication
	 */
	private void enrichWithOwnerDetails(List<Demolition> demolitions, RequestInfo requestInfo) {
		// Step 1: Extract all owner UUIDs from demolitions
		List<String> ownerUuids = extractOwnerUuids(demolitions);

		// Step 2: Return early if no owners found
		if (CollectionUtils.isEmpty(ownerUuids)) {
			return;
		}

		// Step 3: Fetch complete user details from user service
		UserDetailResponse userResponse = userService.getUsersInfo(ownerUuids, requestInfo);

		// Step 4: Enrich owners with user details
		if (!ObjectUtils.isEmpty(userResponse) && !CollectionUtils.isEmpty(userResponse.getUser())) {
			mapUserDetailsToOwners(demolitions, userResponse);
		}
	}

	/**
	 * Extracts unique owner UUIDs from all demolition applications.
	 * 
	 * @param demolitions the list of demolition applications
	 * @return list of unique owner UUIDs
	 */
	private List<String> extractOwnerUuids(List<Demolition> demolitions) {
		return demolitions.stream().filter(demolition -> !CollectionUtils.isEmpty(demolition.getOwners()))
				.flatMap(demolition -> demolition.getOwners().stream()).map(owner -> owner.getUuid()).distinct()
				.collect(Collectors.toList());
	}

	/**
	 * Maps user details from the user service response to owner objects in
	 * demolitions.
	 * 
	 * <p>
	 * This method iterates through users and matches them with owners by UUID, then
	 * enriches the owner object with the complete user information.
	 * 
	 * @param demolitions  the list of demolition applications with owners
	 * @param userResponse the user service response containing user details
	 */
	private void mapUserDetailsToOwners(List<Demolition> demolitions, UserDetailResponse userResponse) {
		userResponse.getUser().forEach(user -> {
			demolitions.forEach(demolition -> {
				if (!CollectionUtils.isEmpty(demolition.getOwners())) {
					demolition.getOwners().stream().filter(owner -> user.getUuid().equals(owner.getUuid()))
							.forEach(owner -> owner.addUserWithoutAuditDetail(user));
				}
			});
		});
	}

	/**
	 * Retrieves demolition applications created by the current citizen user.
	 * 
	 * <p>
	 * <strong>Business Logic:</strong> This method filters demolition applications
	 * to return only those created by the logged-in citizen user. This ensures
	 * citizens can only view their own applications when performing a basic search.
	 * 
	 * <p>
	 * The method extracts the user UUID from the request information and adds it as
	 * a "createdBy" filter to the search criteria before querying the repository.
	 * 
	 * @param criteria    the search criteria to be modified with creator UUID
	 *                    filter
	 * @param requestInfo the request information containing the current user's UUID
	 * @return list of demolition applications created by the current user
	 */
	private List<Demolition> getDemolitionsCreatedByMe(@Valid DemolitionSearchCriteria criteria,
			RequestInfo requestInfo) {

		// Extract current user's UUID
		List<String> creatorUuids = new ArrayList<>();
		if (requestInfo.getUserInfo() != null && !StringUtils.isEmpty(requestInfo.getUserInfo().getUuid())) {
			creatorUuids.add(requestInfo.getUserInfo().getUuid());
		}

		// Add creator filter to search criteria
		criteria.setCreatedBy(creatorUuids);

		log.info("Fetching demolitions created by citizen user with UUID: {}", creatorUuids);

		// Retrieve demolitions from repository
		List<Demolition> demolitions = repository.getDemolitions(criteria);

		return demolitions;
	}

	/**
	 * Updates an existing demolition application with workflow progression and
	 * validations.
	 * 
	 * <p>
	 * This method orchestrates the complete update lifecycle:
	 * <ul>
	 * <li>Validates application existence in the database</li>
	 * <li>Handles special cases: digital signature and deletion requests</li>
	 * <li>Performs workflow validations and enrichment</li>
	 * <li>Assigns to approver if applicable</li>
	 * <li>Progresses workflow and enriches post-status data</li>
	 * <li>Generates fee demands when required</li>
	 * <li>Persists updates to the database</li>
	 * </ul>
	 * 
	 * <p>
	 * <strong>Business Logic:</strong>
	 * <ul>
	 * <li>Digital signature requests bypass normal workflow and only update
	 * signature status</li>
	 * <li>Deletion requests mark application as deleted without workflow
	 * progression</li>
	 * <li>APPLY action triggers random approver assignment</li>
	 * <li>Fee calculation is triggered when status reaches PENDING_FEE_PAYMENT</li>
	 * </ul>
	 * 
	 * @param request the demolition update request containing application data and
	 *                workflow action
	 * @return the updated demolition application
	 * @throws CustomException if application doesn't exist or multiple applications
	 *                         found
	 */
	public Demolition updateDemolition(@Valid DemolitionRequest request) {

		Demolition demolition = request.getDemolition();
		RequestInfo requestInfo = request.getRequestInfo();

		// Step 1: Fetch MDMS master data for validations
		Object mdmsData = fetchMasterData(requestInfo);

		// Step 2: Validate application exists in database
		List<Demolition> existingApplications = validateAndFetchExistingApplication(demolition);

		// Step 3: Handle special update cases (digital signature or deletion)
		Demolition specialCaseResult = handleSpecialUpdateCases(request, existingApplications);
		if (specialCaseResult != null) {
			return specialCaseResult;
		}

		// Step 4: Retrieve business service for workflow processing
		BusinessService businessService = workflowService.getBusinessService(demolition, requestInfo,
				demolition.getApplicationNo());

		// Step 5: Enrich application data for update
		enrichmentService.enrichDemolitionUpdateRequest(request);

		// Step 6: Validate and handle reject/send-back actions
		handleRejectSendBackActions(request, businessService, existingApplications, mdmsData);

		// Step 7: Assign to random approver if action is APPLY
		assignApproverIfRequired(request, businessService);

		// Step 8: Progress workflow
		workflowService.callWorkFlow(request);
		log.info("Workflow processed successfully. New status: {}", demolition.getStatus());

		// Step 9: Enrich post-workflow data (application date, approval number)
		enrichmentService.postStatusEnrichment(request);

		// Step 10: Clean up preview permit details from additional details
		cleanupAdditionalDetails(demolition);

		// Step 11: Generate fee demand if status requires payment
		generateFeeDemandIfRequired(request, demolition);

		// Step 12: Persist updates to database
		repository.update(request);

		return demolition;
	}

	/**
	 * Fetches master data from MDMS for validation purposes.
	 * 
	 * @param requestInfo the request information for MDMS authentication
	 * @return MDMS data object containing master configurations
	 */
	private Object fetchMasterData(RequestInfo requestInfo) {
		return util.mDMSCall(requestInfo, RegularizationConstants.STATE_TENANTID);
	}

	/**
	 * Validates that exactly one demolition application exists in the database.
	 * 
	 * <p>
	 * This method ensures data integrity by checking that:
	 * <ul>
	 * <li>At least one application exists (not empty result)</li>
	 * <li>Exactly one application exists (not multiple duplicates)</li>
	 * </ul>
	 * 
	 * @param demolition the demolition application with ID and tenant ID
	 * @return list containing the single existing application
	 * @throws CustomException if no application found or multiple applications
	 *                         found
	 */
	private List<Demolition> validateAndFetchExistingApplication(Demolition demolition) {
		DemolitionSearchCriteria searchCriteria = DemolitionSearchCriteria.builder()
				.ids(Arrays.asList(demolition.getId())).tenantId(demolition.getTenantId()).build();

		List<Demolition> existingApplications = repository.getDemolitions(searchCriteria);

		if (CollectionUtils.isEmpty(existingApplications) || existingApplications.size() > 1) {
			throw new CustomException("update_error",
					"Failed to Update the Application, Found None or multiple applications!");
		}

		return existingApplications;
	}

	/**
	 * Handles special update cases that bypass the normal workflow.
	 * 
	 * <p>
	 * <strong>Special Cases:</strong>
	 * <ul>
	 * <li><strong>Digital Signature:</strong> Updates signature status for approved
	 * demolitions</li>
	 * <li><strong>Deletion:</strong> Marks demolition as deleted</li>
	 * </ul>
	 * 
	 * @param request              the demolition update request
	 * @param existingApplications the existing applications from database
	 * @return the updated demolition if special case handled, null otherwise
	 */
	private Demolition handleSpecialUpdateCases(DemolitionRequest request, List<Demolition> existingApplications) {
		// Handle digital signature request
		if (isRequestForDigialSignature(request)) {
			return processDigitalSignature(request, existingApplications);
		}

		// Handle deletion request
		if (isRequestForDemolitionDeletion(request)) {
			return processDemolitionDeletion(request);
		}

		return null;
	}

	/**
	 * Assigns the application to a random approver if the action is APPLY.
	 * 
	 * <p>
	 * This method checks if the workflow action is APPLY and delegates to the
	 * approver assignment logic if needed.
	 * 
	 * @param request         the demolition request
	 * @param businessService the business service configuration for workflow
	 */
	private void assignApproverIfRequired(DemolitionRequest request, BusinessService businessService) {
		Demolition demolition = request.getDemolition();

		if (demolition.getWorkflow().getAction().equalsIgnoreCase(DemolitionConstants.APPLY)) {
			assignToRandomApprover(request, businessService);
		}
	}

	/**
	 * Cleans up preview permit details from additional details.
	 * 
	 * <p>
	 * Removes the preview permit key from additional details if present, as it's
	 * only needed during preview and shouldn't be persisted.
	 * 
	 * @param demolition the demolition application
	 */
	private void cleanupAdditionalDetails(Demolition demolition) {
		Map<String, String> additionalDetails = demolition.getAdditionalDetails() != null
				? (Map) demolition.getAdditionalDetails()
				: new HashMap<String, String>();

		if (additionalDetails.get(BPAConstants.PREVIEW_PERMIT_ADDITIONAL_DETAILS) != null) {
			additionalDetails.remove(BPAConstants.PREVIEW_PERMIT_ADDITIONAL_DETAILS);
		}
	}

	/**
	 * Generates fee demand calculation if the demolition status requires payment.
	 * 
	 * <p>
	 * <strong>Business Logic:</strong> When a demolition application reaches the
	 * PENDING_FEE_PAYMENT status, the system must calculate and generate the fee
	 * demand so citizens can proceed with payment.
	 * 
	 * @param request    the demolition request
	 * @param demolition the demolition application
	 */
	private void generateFeeDemandIfRequired(DemolitionRequest request, Demolition demolition) {
		if (demolition.getStatus().equalsIgnoreCase(DemolitionConstants.PENDING_FEE_PAYMENT)) {
			calculationService.addCalculation(request);
		}
	}

	/**
	 * Assigns the demolition application to a random approver from the eligible
	 * approver pool.
	 * 
	 * <p>
	 * This method performs the following operations:
	 * <ul>
	 * <li>Checks if assignees are already set (skip if already assigned)</li>
	 * <li>Retrieves the current workflow state and available actions</li>
	 * <li>Determines the next workflow state based on APPLY action</li>
	 * <li>Fetches eligible approver roles for the next state</li>
	 * <li>Retrieves list of users with those roles in the tenant</li>
	 * <li>Randomly selects one user as the assignee</li>
	 * </ul>
	 * 
	 * <p>
	 * <strong>Business Logic:</strong> Random assignment ensures fair distribution
	 * of workload among approvers and prevents manual cherry-picking of
	 * applications.
	 * 
	 * @param request         the demolition request containing application details
	 * @param businessService the business service configuration defining workflow
	 *                        states and roles
	 */
	private void assignToRandomApprover(@Valid DemolitionRequest request, BusinessService businessService) {

		Demolition demolition = request.getDemolition();
		RequestInfo requestInfo = request.getRequestInfo();

		log.info("Starting random approver assignment for application: {}", demolition.getApplicationNo());

		// Check if action is APPLY
		if (!demolition.getWorkflow().getAction().equalsIgnoreCase(DemolitionConstants.APPLY)) {
			return;
		}

		// Skip if assignees already set
		if (!CollectionUtils.isEmpty(demolition.getWorkflow().getAssignes())) {
			log.info("Assignees already set, skipping random assignment");
			return;
		}

		// Step 1: Retrieve current workflow state and available actions
		List<ProcessInstance> processInstances = workflowService.getProcessInstances(demolition, requestInfo, false);
		List<Action> availableActions = processInstances.get(0).getState().getActions();

		// Step 2: Find the action matching the request
		Action matchingAction = findMatchingAction(availableActions, demolition.getWorkflow().getAction());

		// Step 3: Determine roles for the next workflow state
		String eligibleRoles = util.getNextValidUserUUIDByNextState(matchingAction.getNextState(), businessService,
				requestInfo);
		log.info("Eligible roles for next state: {}", eligibleRoles);

		// Step 4: Fetch users with eligible roles in the tenant
		List<String> eligibleApprovers = util.getNextValidUserUUID(eligibleRoles, demolition.getTenantId(), true,
				requestInfo);
		log.info("Eligible approvers found: {}", eligibleApprovers.size());

		// Step 5: Randomly select an approver
		String selectedApprover = util.getRandomValue(eligibleApprovers);
		log.info("Randomly selected approver: {}", selectedApprover);

		// Step 6: Assign the selected approver
		demolition.getWorkflow().setAssignes(Arrays.asList(selectedApprover));
	}

	/**
	 * Finds the action in the available actions list that matches the requested
	 * action.
	 * 
	 * @param availableActions the list of available workflow actions
	 * @param requestedAction  the action being requested
	 * @return the matching action object
	 * @throws CustomException if no matching action found
	 */
	private Action findMatchingAction(List<Action> availableActions, String requestedAction) {
		return availableActions.stream().filter(action -> action.getAction().equalsIgnoreCase(requestedAction))
				.findFirst().orElseThrow(() -> new CustomException("workflow_error",
						"Action '" + requestedAction + "' not found in available actions"));
	}

	/**
	 * Processes a digital signature request for an approved demolition application.
	 * 
	 * <p>
	 * This method performs the following operations:
	 * <ul>
	 * <li>Removes the temporary "applicationType" marker from additional
	 * details</li>
	 * <li>Marks the demolition as digitally signed by setting "demolitionIsSigned"
	 * to true</li>
	 * <li>Preserves the original audit details from the existing application</li>
	 * <li>Persists the updated signature status to the database</li>
	 * </ul>
	 * 
	 * <p>
	 * <strong>Business Logic:</strong> This is used when an official digitally
	 * signs an approved demolition permit offline. The signature status is recorded
	 * in the system to track which permits have been officially signed.
	 * 
	 * @param request      the demolition request containing signature update
	 * @param searchResult the existing demolition application from database
	 * @return the updated demolition application with signature status
	 */
	private Demolition processDigitalSignature(@Valid DemolitionRequest request, List<Demolition> searchResult) {

		log.info("Processing digital signature for demolition application");

		Map<String, Object> additionalDetails = (Map) request.getDemolition().getAdditionalDetails();

		// Remove temporary application type marker
		additionalDetails.remove("applicationType");

		// Mark demolition as digitally signed
		additionalDetails.put("demolitionIsSigned", true);

		// Preserve original audit details
		request.getDemolition().setAuditDetails(searchResult.get(0).getAuditDetails());

		// Persist signature status update
		repository.update(request);

		return request.getDemolition();
	}

	/**
	 * Checks if the request is for updating digital signature status.
	 * 
	 * <p>
	 * A request is considered a digital signature request when ALL of the following
	 * conditions are met:
	 * <ul>
	 * <li>Demolition status is "APPROVED"</li>
	 * <li>Additional details are present and is a Map</li>
	 * <li>Application type in additional details is "demolitionSignature"</li>
	 * <li>Demolition is not already marked as digitally signed</li>
	 * </ul>
	 * 
	 * @param request the demolition request to check
	 * @return true if this is a digital signature request, false otherwise
	 */
	private boolean isRequestForDigialSignature(@Valid DemolitionRequest request) {

		return (request.getDemolition().getStatus().equalsIgnoreCase("APPROVED")
				&& Objects.nonNull(request.getDemolition().getAdditionalDetails())
				&& request.getDemolition().getAdditionalDetails() instanceof Map
				&& "demolitionSignature"
						.equals(((Map) request.getDemolition().getAdditionalDetails()).get("applicationType"))
				&& !(Objects.nonNull(((Map) request.getDemolition().getAdditionalDetails()).get("demolitionIsSigned"))
						&& ((boolean) ((Map) request.getDemolition().getAdditionalDetails())
								.get("demolitionIsSigned"))));
	}

	/**
	 * Handles validation and enrichment for REJECT and SEND_BACK_TO_CITIZEN
	 * workflow actions.
	 * 
	 * <p>
	 * <strong>Business Logic:</strong>
	 * <ul>
	 * <li><strong>REJECT Action:</strong> Requires mandatory comments explaining
	 * rejection reason</li>
	 * <li><strong>Other Actions (except SEND_BACK_TO_CITIZEN):</strong> Performs
	 * workflow and business validations</li>
	 * <li><strong>SEND_BACK_TO_CITIZEN Action:</strong> Bypasses standard
	 * validations</li>
	 * </ul>
	 * 
	 * @param request         the demolition request containing workflow action
	 * @param businessService the business service configuration
	 * @param searchResult    the existing demolition application from database
	 * @param mdmsData        the master data for validation rules
	 * @throws CustomException if reject action lacks comments or validation fails
	 */
	private void handleRejectSendBackActions(@Valid DemolitionRequest request, BusinessService businessService,
			List<Demolition> searchResult, Object mdmsData) {

		Demolition demolition = request.getDemolition();

		// Handle REJECT action - comments are mandatory
		if (demolition.getWorkflow().getAction() != null
				&& (demolition.getWorkflow().getAction().equalsIgnoreCase(BPAConstants.ACTION_REJECT))) {

			if (demolition.getWorkflow().getComments() == null || demolition.getWorkflow().getComments().isEmpty()) {
				throw new CustomException("update_error", "Comment is mandatory, please provide the comments");
			}

		} else {
			// For other actions (except SEND_BACK_TO_CITIZEN), perform full validations
			if (!demolition.getWorkflow().getAction().equalsIgnoreCase(BPAConstants.ACTION_SENDBACKTOCITIZEN)) {

				// Validate workflow-related rules
				actionValidator.validateUpdateRequest(request, businessService);

				// Validate business rules and application data
				validator.validateUpdateRequest(request, searchResult, mdmsData,
						workflowService.getCurrentState(request.getDemolition().getStatus(), businessService));
			}
		}
	}

	/**
	 * Retrieves fee estimate from the BPA calculator service.
	 * 
	 * @param demolitionRequest the demolition request for fee calculation
	 * @return fee estimate object from calculator service
	 */
	public Object getFeeEstimateFromBpaCalculator(Object demolitionRequest) {
		return calculationService.callBpaCalculatorEstimate(demolitionRequest);
	}

	/**
	 * Checks if the request is for deleting a demolition application.
	 * 
	 * <p>
	 * This method verifies if the workflow action is DELETE and validates that the
	 * demolition is in a state that allows deletion.
	 * 
	 * @param request the demolition request to check
	 * @return true if this is a deletion request and validation passes, false
	 *         otherwise
	 */
	private boolean isRequestForDemolitionDeletion(DemolitionRequest request) {
		Demolition demolition = request.getDemolition();
		String action = demolition.getWorkflow().getAction();

		if (action.equalsIgnoreCase(BPAConstants.ACTION_BPA_DELETE)) {
			// Validate that demolition can be deleted
			actionValidator.validateDemolitionDeletion(demolition);
			return true;
		}

		return false;
	}

	/**
	 * Processes the deletion of a demolition application.
	 * 
	 * <p>
	 * This method performs a soft delete by:
	 * <ul>
	 * <li>Verifying the application exists in the database</li>
	 * <li>Setting the status to DELETED</li>
	 * <li>Removing workflow information</li>
	 * <li>Persisting the updated status</li>
	 * </ul>
	 * 
	 * <p>
	 * <strong>Business Logic:</strong> Applications are not physically deleted but
	 * marked with DELETED status to maintain an audit trail and for reporting
	 * purposes.
	 * 
	 * @param request the demolition deletion request
	 * @return the deleted demolition application
	 * @throws CustomException if application doesn't exist in database
	 */
	private Demolition processDemolitionDeletion(DemolitionRequest request) {
		Demolition demolition = request.getDemolition();

		// Verify application exists
		DemolitionSearchCriteria criteria = new DemolitionSearchCriteria();
		criteria.setApplicationNo(demolition.getApplicationNo());
		List<Demolition> searchResult = repository.getDemolitions(criteria);

		if (CollectionUtils.isEmpty(searchResult)) {
			throw new CustomException("Deletion Error",
					"Failed to Delete Demolition, No application exists with mentioned Demolition ID");
		}

		// Mark as deleted (soft delete)
		demolition.setStatus(DemolitionConstants.DELETED);
		demolition.setWorkflow(null);

		// Persist deletion status
		repository.update(request);

		return request.getDemolition();
	}

	public List<BPAVillage> searchVillage(@Valid VillageSearchCriteria criteria, RequestInfo requestInfo) {

		List<BPAVillage> villageData = new LinkedList<>();

		validator.validateVillageSearchRequest(criteria);

		villageData = repository.getDemolitionVillageData(criteria);

		return villageData;
	}

	public List<DemolitionApprovedByApplicationSearch> searchApplicationApprovedBy(
			@Valid DemolitionSearchCriteria criteria, String uuid) {

		List<DemolitionApprovedByApplicationSearch> demolitionsApprovedByApplicationSearchs = repository
				.getApprovedbyData(uuid, criteria);

		if (demolitionsApprovedByApplicationSearchs.isEmpty()) {
			return Collections.emptyList();
		} else {
			this.populatedocumentdetailstoSearch(demolitionsApprovedByApplicationSearchs);
			return demolitionsApprovedByApplicationSearchs;
		}
	}

	private void populatedocumentdetailstoSearch(
			List<DemolitionApprovedByApplicationSearch> demolitionsApprovedByApplicationSearchs) {

		List<String> demolitionIds = demolitionsApprovedByApplicationSearchs.stream()
				.filter(item -> item.getDemolitionId() != null)
				.map(DemolitionApprovedByApplicationSearch::getDemolitionId).collect(Collectors.toList());
		List<DemolitionDocumentList> documentList = repository.getdocumentDataForApproveBy(demolitionIds);

		for (DemolitionApprovedByApplicationSearch application : demolitionsApprovedByApplicationSearchs) {
			List<DemolitionDocumentList> docList = documentList.stream()
					.filter(doc -> doc.getDemolitionId().equals(application.getDemolitionId()))
					.collect(Collectors.toList());
			application.setDocuments(docList);
		}

	}

	/**
	 * Updates the permit letter details for a demolition application.
	 * 
	 * <p>
	 * This method handles the specific use case of updating permit letter
	 * information after a demolition has been approved. It performs minimal
	 * validation and delegates to the repository for persistence.
	 * 
	 * <p>
	 * The method performs the following operations:
	 * <ul>
	 * <li>Validates that exactly one demolition application exists in the
	 * database</li>
	 * <li>Persists the updated permit letter details through Kafka messaging</li>
	 * </ul>
	 * 
	 * <p>
	 * <strong>Business Logic:</strong> This method is typically called after
	 * approval to update permit-specific details like permit number, issuance date,
	 * or other administrative information that gets finalized post-approval.
	 * 
	 * <p>
	 * <strong>Note:</strong> This method does NOT trigger workflow progression or
	 * validations - it's a simple administrative update for permit documentation.
	 * 
	 * @param demolitionRequest the demolition request containing updated permit
	 *                          letter details
	 * @return the demolition application with updated permit letter information
	 * @throws CustomException if application doesn't exist or multiple applications
	 *                         found
	 */
	public Demolition permitLetterUpdate(@Valid DemolitionRequest demolitionRequest) {

		Demolition demolition = demolitionRequest.getDemolition();

		// Step 1: Validate application exists in database (reuse existing validation)
		validateAndFetchExistingApplication(demolition);

		// Step 2: Persist permit letter updates via Kafka
		repository.updatePermitLetterRequest(demolitionRequest);

		return demolition;
	}
	public DemolitionFieldInspection createFieldInspectionReport(
			@Valid DemolitionFieldInspectionRequest request) {

		DemolitionFieldInspection fieldInspection = request.getFieldInspection();

		// Step 1: Validate mandatory application number
		validateApplicationNumberPresent(fieldInspection);

		// Step 2: Validate field inspection request data
		validator.validatefieldInspectionRequest(fieldInspection);

		// Step 3: Verify demolition application exists
		validateDemolitionExists(fieldInspection, request.getRequestInfo());

		// Step 4: Enrich report with metadata (ID, audit details)
		enrichmentService.enrichCreateFieldInspectionReport(request);

		// Step 5: Persist field inspection report via Kafka
		repository.savefieldInspectionReport(request);

		return request.getFieldInspection();
	}
	/**
	 * Validates that application number is present in the field inspection request.
	 * 
	 * @param fieldInspection The field inspection object to validate
	 * @throws CustomException if application number is null or empty
	 */
	private void validateApplicationNumberPresent(DemolitionFieldInspection fieldInspection) {

		if (ObjectUtils.isEmpty(fieldInspection.getApplicationno())) {
			throw new CustomException("create error",
					"Failed to create feild inspection report, application no is mandatory");
		}
	}
	
	private void validateDemolitionExists(DemolitionFieldInspection fieldInspection, RequestInfo requestInfo) {

		// Build search criteria
		DemolitionSearchCriteria searchCriteria = DemolitionSearchCriteria.builder()
				.applicationNo(fieldInspection.getApplicationno()).tenantId(fieldInspection.getTenantId()).build();

		// Search for the demolition application
		List<Demolition> demolitionList = repository.getDemolitions(searchCriteria);
		log.info("Found {} demolition(s) for application: {}", demolitionList.size(),
				fieldInspection.getApplicationno());

		// Validate exactly one application exists
		if (CollectionUtils.isEmpty(demolitionList) || demolitionList.size() > 1) {
			throw new CustomException("create error",
					"Failed to create feild inspection report, Found None or multiple bpa applications!");
		}
	}
	
	
	public List<DemolitionFieldInspection> searchFieldInspectionReport(
			@Valid DemolitionFISearchCriteria criteria, RequestInfo requestInfo) {
		List<DemolitionFieldInspection> result = repository.getfieldInspectionReport(criteria);
		return result;
	}

	public DemolitionFieldInspection updateFieldInspectionReport(@Valid DemolitionFieldInspectionRequest request) {
		DemolitionFieldInspection fieldInspection = request.getFieldInspection();

		if (ObjectUtils.isEmpty(fieldInspection.getApplicationno()))
			throw new CustomException("update error",
					"Failed to update feild inspection report, application no is mandatory");
		validator.validatefieldInspectionRequest(fieldInspection);

		DemolitionSearchCriteria searchCriteria = DemolitionSearchCriteria.builder()
				.applicationNo(fieldInspection.getApplicationno()).tenantId(fieldInspection.getTenantId()).build();
		List<Demolition> demolitionList = repository.getDemolitions(searchCriteria);
		log.info(demolitionList.toString());

		if (CollectionUtils.isEmpty(demolitionList) || demolitionList.size() > 1) {
			throw new CustomException("update error",
					"Failed to update feild inspection report, Found None or multiple bpa applications!");
		}

		enrichmentService.enrichUpdateFieldInspectionReport(request);
		repository.updateFieldInspectionReport(request);
		// return the object
		return request.getFieldInspection();

	}


	
}
