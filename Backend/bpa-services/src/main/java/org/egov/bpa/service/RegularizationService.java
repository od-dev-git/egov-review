package org.egov.bpa.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.validation.Valid;

import org.egov.bpa.config.BPAConfiguration;
import org.egov.bpa.repository.RegularizationIssueFixRepository;
import org.egov.bpa.repository.RegularizationRepository;
import org.egov.bpa.repository.ScnRepository;
import org.egov.bpa.util.BPAConstants;
import org.egov.bpa.util.BPAErrorConstants;
import org.egov.bpa.util.IssueFixConstants;
import org.egov.bpa.util.RegularizationConstants;
import org.egov.bpa.util.RegularizationUtil;
import org.egov.bpa.validator.RegularizationValidator;
import org.egov.bpa.web.model.BPASearchCriteria;
import org.egov.bpa.web.model.Document;
import org.egov.bpa.web.model.FeePendingApplication;
import org.egov.bpa.web.model.Installment;
import org.egov.bpa.web.model.Notice;
import org.egov.bpa.web.model.NoticeSearchCriteria;
import org.egov.bpa.web.model.RegularizationDraft;
import org.egov.bpa.web.model.RegularizationDraftRequest;
import org.egov.bpa.web.model.RegularizationDraftSearchCriteria;
import org.egov.bpa.web.model.VillageSearchCriteria;
import org.egov.bpa.web.model.collection.Demand;
import org.egov.bpa.web.model.landInfo.OwnerInfo;
import org.egov.bpa.web.model.regularization.AppType;
import org.egov.bpa.web.model.regularization.Regularization;
import org.egov.bpa.web.model.regularization.RegularizationActionValidator;
import org.egov.bpa.web.model.regularization.RegularizationApprovedByApplicationSearch;
import org.egov.bpa.web.model.regularization.RegularizationDocUpload;
import org.egov.bpa.web.model.regularization.RegularizationDocUploadRequest;
import org.egov.bpa.web.model.regularization.RegularizationDocumentList;
import org.egov.bpa.web.model.regularization.RegularizationDscDetails;
import org.egov.bpa.web.model.regularization.RegularizationFISearchCriteria;
import org.egov.bpa.web.model.regularization.RegularizationFieldInspection;
import org.egov.bpa.web.model.regularization.RegularizationFieldInspectionRequest;
import org.egov.bpa.web.model.regularization.RegularizationRequest;
import org.egov.bpa.web.model.regularization.RegularizationSearchCriteria;
import org.egov.bpa.web.model.regularization.RegularizationVillage;
import org.egov.bpa.web.model.user.UserDetailResponse;
import org.egov.bpa.web.model.workflow.BusinessService;
import org.egov.common.contract.request.RequestInfo;
import org.egov.common.contract.request.Role;
import org.egov.tracer.model.CustomException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import lombok.extern.slf4j.Slf4j;
import net.logstash.logback.encoder.org.apache.commons.lang.StringUtils;

@Service
@Slf4j
public class RegularizationService {

	@Autowired
	private RegularizationValidator regularizationValidator;

	@Autowired
	private RegularizationEnrichmentService enrichmentService;

	@Autowired
	private RegularizationWorkflow workflowService;

	@Autowired
	private RegularizationUserService userService;

	@Autowired
	private RegularizationRepository repository;

	@Autowired
	private RegularizationCalculationService calculationService;

	@Autowired
	private RegularizationActionValidator actionValidator;

	@Autowired
	private RegularizationUtil util;

	@Autowired
	private BPAConfiguration config;

	@Autowired
	private RegularizationIssueFixRepository issueFixRepository;

	@Autowired
	private ScnRepository scnRepository;

	/**
	 * Create Regularization Request from the request Payload
	 * 
	 * @param regularizationRequest
	 * @return
	 */
	/**
	 * Creates a new Regularization application based on the application type.
	 * 
	 * <p>
	 * This method orchestrates the regularization creation process by:
	 * <ol>
	 * <li>Validates that the application type is specified</li>
	 * <li>Routes to the appropriate creation handler based on application type:
	 * <ul>
	 * <li><b>LAND:</b> Creates land regularization with land-specific
	 * validations</li>
	 * <li><b>BUILDING:</b> Creates building regularization with building-specific
	 * validations</li>
	 * <li><b>LAND_AND_BUILDING:</b> Creates combined regularization with both
	 * validations</li>
	 * </ul>
	 * </li>
	 * <li>Cleans up any associated draft records after successful creation</li>
	 * </ol>
	 * </p>
	 * 
	 * @param regularizationRequest The regularization request containing
	 *                              application details
	 * @return The created regularization entity with generated application number
	 * @throws CustomException if application type is null or invalid
	 */
	public Regularization create(@Valid RegularizationRequest regularizationRequest) {

		RequestInfo requestInfo = regularizationRequest.getRequestInfo();
		Regularization regularization = regularizationRequest.getRegularization();

		// Step 1: Validate application type is specified
		validateApplicationType(regularization);

		// Step 2: Route to appropriate creation handler based on application type
		processCreationByApplicationType(regularizationRequest, requestInfo, regularization);

		// Step 3: Clean up associated draft record if exists
		enrichRegularizationDraft(requestInfo, regularization);

		return regularization;
	}

	/**
	 * Validates that the application type is specified and valid.
	 * 
	 * @param regularization The regularization entity to validate
	 * @throws CustomException if application type is null
	 */
	private void validateApplicationType(Regularization regularization) {
		if (Objects.isNull(regularization.getAppType())) {
			throw new CustomException("INVALID_REGULARIZATION_CREATE",
					"Application Type does not fall in either Land or Building or Land & Building");
		}
	}

	/**
	 * Routes the creation request to the appropriate handler based on application
	 * type.
	 * 
	 * <p>
	 * Application types and their handlers:
	 * <ul>
	 * <li><b>LAND:</b> Handled by {@link #createLandRegularization}</li>
	 * <li><b>BUILDING:</b> Handled by {@link #createBuildingRegularization}</li>
	 * <li><b>LAND_AND_BUILDING:</b> Handled by
	 * {@link #createLandAndBuildingRegularization}</li>
	 * </ul>
	 * </p>
	 * 
	 * @param regularizationRequest The regularization request
	 * @param requestInfo           The request info containing user context
	 * @param regularization        The regularization entity
	 */
	private void processCreationByApplicationType(RegularizationRequest regularizationRequest, RequestInfo requestInfo,
			Regularization regularization) {

		AppType appType = regularization.getAppType();

		switch (appType) {
		case LAND:
			// Create land-only regularization
			createLandRegularization(regularizationRequest, requestInfo, regularization);
			break;

		case BUILDING:
			// Create building-only regularization
			createBuildingRegularization(regularizationRequest, requestInfo, regularization);
			break;

		case LAND_AND_BUILDING:
			// Create combined land and building regularization
			createLandAndBuildingRegularization(regularizationRequest, requestInfo, regularization);
			break;

		default:
			throw new CustomException("INVALID_APPLICATION_TYPE", "Unsupported application type: " + appType);
		}
	}

	/**
	 * Creates a Land Regularization application.
	 * 
	 * <p>
	 * This method handles the complete creation flow for land regularization:
	 * <ol>
	 * <li>Validates the land-specific create request</li>
	 * <li>Enriches the request with required fields (IDs, audit details, etc.)</li>
	 * <li>Initiates the workflow for the application</li>
	 * <li>Persists the application via Kafka</li>
	 * </ol>
	 * </p>
	 * 
	 * @param regularizationRequest The regularization request
	 * @param requestInfo           The request info containing user context
	 * @param regularization        The regularization entity to create
	 */
	private void createLandRegularization(RegularizationRequest regularizationRequest, RequestInfo requestInfo,
			Regularization regularization) {

		// Validate land-specific create request
		regularizationValidator.validateLandCreate(requestInfo, regularization);

		// Enrich the required fields for Land Create Request
		enrichmentService.enrichLandCreateRequest(requestInfo, regularization);

		// Initiate workflow for the application
		workflowService.callWorkFlow(requestInfo, regularization);

		// Persist the request through Kafka
		repository.save(regularizationRequest);
	}

	/**
	 * Service layer for searching Regularization
	 * 
	 * @param criteria
	 * @param requestInfo
	 * @return List<Regularization>
	 */
	/**
	 * Searches for regularization applications based on the provided search
	 * criteria.
	 * 
	 * <p>
	 * This method orchestrates the search process by:
	 * <ol>
	 * <li>Validates the search request and criteria</li>
	 * <li>Extracts user roles for authorization-based routing</li>
	 * <li>Routes to appropriate search handler based on search type:
	 * <ul>
	 * <li><b>Escalated:</b> Applications that have been auto-escalated</li>
	 * <li><b>Escalated To Me:</b> Applications escalated to the current user</li>
	 * <li><b>About To Escalate:</b> Applications approaching escalation
	 * threshold</li>
	 * <li><b>Citizen Search:</b> Applications created by/for the citizen</li>
	 * <li><b>General Search:</b> Standard criteria-based search</li>
	 * </ul>
	 * </li>
	 * <li>Enriches results with owner details from user service</li>
	 * </ol>
	 * </p>
	 * 
	 * @param searchCriteria The search criteria containing filters and flags
	 * @param requestInfo    The request info containing user context and auth
	 *                       details
	 * @return List of regularization applications matching the search criteria
	 */
	public List<Regularization> searchRegularization(RegularizationSearchCriteria searchCriteria,
			RequestInfo requestInfo) {

		log.info("Searching regularizations with criteria: {}", searchCriteria);

		// Step 1: Validate search request
		regularizationValidator.validateSearch(requestInfo, searchCriteria);

		// Step 2: Extract user roles for authorization-based routing
		List<String> userRoles = extractUserRoles(requestInfo);

		// Step 3: Route to appropriate search handler based on search type
		List<Regularization> regularizationList = routeToSearchHandler(searchCriteria, requestInfo, userRoles);

		// Step 4: Enrich results with owner details from user service
		getOwnersDetails(regularizationList, requestInfo);

		log.info("Search returned {} regularization(s)", regularizationList.size());
		return regularizationList;
	}

	/**
	 * Extracts role codes from the authenticated user's role list.
	 * 
	 * @param requestInfo The request info containing user details
	 * @return List of role codes assigned to the user
	 */
	private List<String> extractUserRoles(RequestInfo requestInfo) {

		List<String> roles = new ArrayList<>();

		if (requestInfo.getUserInfo() != null && requestInfo.getUserInfo().getRoles() != null) {
			for (Role role : requestInfo.getUserInfo().getRoles()) {
				roles.add(role.getCode());
			}
		}

		return roles;
	}

	/**
	 * Routes the search request to the appropriate handler based on search criteria
	 * flags.
	 * 
	 * <p>
	 * Search routing priority:
	 * <ol>
	 * <li>Escalation-related searches (isEscalated, isEscalatedToMe,
	 * isAboutToEscalate)</li>
	 * <li>Citizen-specific search (tenant-only or empty criteria with CITIZEN
	 * role)</li>
	 * <li>General repository search (all other cases)</li>
	 * </ol>
	 * </p>
	 * 
	 * @param searchCriteria The search criteria with flags
	 * @param requestInfo    The request info for context
	 * @param userRoles      List of user's role codes
	 * @return List of matching regularization applications
	 */
	private List<Regularization> routeToSearchHandler(RegularizationSearchCriteria searchCriteria,
			RequestInfo requestInfo, List<String> userRoles) {

		// Priority 1: Escalation-related searches
		if (searchCriteria.isEscalated()) {
			log.debug("Routing to: Auto-escalated applications search");
			return getRegularizationAutoEscalated(searchCriteria, requestInfo);
		}

		if (searchCriteria.isEscalatedToMe()) {
			log.debug("Routing to: Applications escalated to me");
			return getRegularizationAutoEscalatedToMe(searchCriteria, requestInfo);
		}

		if (searchCriteria.isAboutToEscalate()) {
			log.debug("Routing to: Applications about to escalate");
			return getRegularizationAboutToAutoEscalate(searchCriteria, requestInfo);
		}

		// Priority 2: Citizen-specific search
		if (isCitizenSelfSearch(searchCriteria, userRoles)) {
			log.debug("Routing to: Citizen's own applications search");
			return getRegularizationsCreatedByMe(searchCriteria, requestInfo, userRoles);
		}

		// Priority 3: General repository search
		log.debug("Routing to: General repository search");
		return repository.searchRegularization(searchCriteria, requestInfo);
	}

	/**
	 * Checks if the search is a citizen searching for their own applications.
	 * 
	 * <p>
	 * A citizen self-search is identified when:
	 * <ul>
	 * <li>Search criteria contains only tenant ID or is empty, AND</li>
	 * <li>User has the CITIZEN role</li>
	 * </ul>
	 * </p>
	 * 
	 * @param searchCriteria The search criteria
	 * @param userRoles      The user's role codes
	 * @return true if this is a citizen self-search
	 */
	private boolean isCitizenSelfSearch(RegularizationSearchCriteria searchCriteria, List<String> userRoles) {

		boolean isMinimalCriteria = searchCriteria.tenantIdOnly() || searchCriteria.isEmpty();
		boolean isCitizen = userRoles.contains(BPAConstants.CITIZEN);

		return isMinimalCriteria && isCitizen;
	}

	/**
	 * Retrieves regularization applications that have been auto-escalated to the
	 * current user.
	 * 
	 * <p>
	 * These are applications where the workflow was automatically forwarded to this
	 * user due to inaction by the previous assignee within the configured time
	 * threshold.
	 * </p>
	 * 
	 * @param searchCriteria The search criteria containing tenant and filter
	 *                       information
	 * @param requestInfo    The request info containing the current user's UUID
	 * @return List of regularizations escalated to the current user
	 */
	private List<Regularization> getRegularizationAutoEscalatedToMe(RegularizationSearchCriteria searchCriteria,
			RequestInfo requestInfo) {

		String userUuid = requestInfo.getUserInfo().getUuid();
		return repository.getRegularizationAutoEscalatedToMe(searchCriteria, userUuid);
	}

	/**
	 * Retrieves regularization applications that have been auto-escalated by the
	 * current user.
	 * 
	 * <p>
	 * These are applications that were previously assigned to the current user but
	 * were automatically escalated due to inaction within the configured time
	 * threshold.
	 * </p>
	 * 
	 * @param searchCriteria The search criteria containing tenant and filter
	 *                       information
	 * @param requestInfo    The request info containing the current user's UUID
	 * @return List of regularizations that were escalated from the current user
	 */
	private List<Regularization> getRegularizationAutoEscalated(RegularizationSearchCriteria searchCriteria,
			RequestInfo requestInfo) {

		String userUuid = requestInfo.getUserInfo().getUuid();
		return repository.getRegularizationAutoEscalated(searchCriteria, userUuid);
	}

	/**
	 * Retrieves regularization applications that are about to be auto-escalated.
	 * 
	 * <p>
	 * These are applications approaching the escalation threshold, giving the
	 * current assignee a warning to take action before automatic escalation occurs.
	 * </p>
	 * 
	 * @param searchCriteria The search criteria containing tenant and filter
	 *                       information
	 * @param requestInfo    The request info containing the current user's UUID
	 * @return List of regularizations about to be escalated
	 */
	private List<Regularization> getRegularizationAboutToAutoEscalate(RegularizationSearchCriteria searchCriteria,
			RequestInfo requestInfo) {

		String userUuid = requestInfo.getUserInfo().getUuid();
		return repository.getRegularizationAboutToAutoEscalate(searchCriteria, userUuid);
	}

	/**
	 * Retrieves regularization applications created by or associated with the
	 * current user.
	 * 
	 * <p>
	 * This method handles two scenarios based on user role:
	 * <ul>
	 * <li><b>Architect/Technical Person:</b> Searches by createdBy field
	 * (applications they submitted)</li>
	 * <li><b>Citizen:</b> Searches by owner mobile number (applications where they
	 * are an owner)</li>
	 * </ul>
	 * </p>
	 * 
	 * @param searchCriteria The search criteria to augment with user-specific
	 *                       filters
	 * @param requestInfo    The request info containing user context
	 * @param roles          The user's role codes
	 * @return List of regularizations associated with the current user
	 */
	private List<Regularization> getRegularizationsCreatedByMe(RegularizationSearchCriteria searchCriteria,
			RequestInfo requestInfo, List<String> roles) {

		// Determine search strategy based on user role
		if (isArchitectOrTechnicalPerson(roles)) {
			// Architects and technical persons search by creator UUID
			return searchByCreator(searchCriteria, requestInfo);
		} else {
			// Citizens search by owner mobile number
			return searchByOwnerMobile(searchCriteria, requestInfo);
		}
	}

	/**
	 * Checks if the user is an architect or technical person.
	 * 
	 * @param roles The user's role codes
	 * @return true if user has architect or technical person role
	 */
	private boolean isArchitectOrTechnicalPerson(List<String> roles) {
		return roles.contains(BPAConstants.BPA_ARCHITECT)
				|| roles.contains(BPAConstants.BPA_TECHNICALPERSON_CIVIL_ENGINEER);
	}

	/**
	 * Searches regularizations by the creator's UUID.
	 * 
	 * <p>
	 * Used for architects and technical persons who submit applications on behalf
	 * of citizens.
	 * </p>
	 * 
	 * @param searchCriteria The search criteria to augment
	 * @param requestInfo    The request info containing user UUID
	 * @return List of regularizations created by the current user
	 */
	private List<Regularization> searchByCreator(RegularizationSearchCriteria searchCriteria, RequestInfo requestInfo) {

		List<String> creatorUuids = new ArrayList<>();
		creatorUuids.add(requestInfo.getUserInfo().getUuid());

		searchCriteria.setCreatedBy(creatorUuids);
		log.info("Searching applications created by architect/technical person: {}", creatorUuids);

		return repository.searchRegularization(searchCriteria, requestInfo);
	}

	/**
	 * Searches regularizations by the owner's mobile number.
	 * 
	 * <p>
	 * This method performs a two-step search for citizens:
	 * <ol>
	 * <li>Fetches the citizen's user record to get their mobile number</li>
	 * <li>Searches for all users with the same mobile number (handles multiple
	 * accounts)</li>
	 * <li>Searches regularizations where any of these users are owners</li>
	 * </ol>
	 * </p>
	 * 
	 * @param searchCriteria The search criteria to augment
	 * @param requestInfo    The request info containing user context
	 * @return List of regularizations where the current user is an owner
	 */
	private List<Regularization> searchByOwnerMobile(RegularizationSearchCriteria searchCriteria,
			RequestInfo requestInfo) {

		log.info("Searching applications for citizen: {}", requestInfo.getUserInfo().getUuid());

		// Step 1: Get current user's mobile number
		String mobileNumber = getCurrentUserMobileNumber(searchCriteria, requestInfo);

		if (mobileNumber == null) {
			log.warn("Unable to fetch mobile number for user: {}", requestInfo.getUserInfo().getUuid());
			return new ArrayList<>();
		}

		// Step 2: Find all user UUIDs with this mobile number
		List<String> ownerIds = findUsersByMobileNumber(searchCriteria, requestInfo, mobileNumber);

		// Step 3: Search regularizations by owner IDs
		searchCriteria.setOwnerIds(ownerIds);
		log.info("Searching applications for citizen with {} associated owner ID(s)", ownerIds.size());

		return repository.searchRegularization(searchCriteria, requestInfo);
	}

	/**
	 * Fetches the current user's mobile number from user service.
	 * 
	 * @param searchCriteria The search criteria containing tenant info
	 * @param requestInfo    The request info containing user UUID
	 * @return The user's mobile number, or null if not found
	 */
	private String getCurrentUserMobileNumber(RegularizationSearchCriteria searchCriteria, RequestInfo requestInfo) {

		// Set owner ID to current user for initial lookup
		List<String> uuids = new ArrayList<>();
		uuids.add(requestInfo.getUserInfo().getUuid());
		searchCriteria.setOwnerIds(uuids);

		// Fetch user details
		UserDetailResponse userInfo = userService.getUser(searchCriteria, requestInfo);

		if (userInfo != null && !CollectionUtils.isEmpty(userInfo.getUser())) {
			return userInfo.getUser().get(0).getMobileNumber();
		}

		return null;
	}

	/**
	 * Finds all user UUIDs associated with a given mobile number.
	 * 
	 * <p>
	 * A single mobile number may be associated with multiple user accounts (e.g.,
	 * same person registered in different tenants).
	 * </p>
	 * 
	 * @param searchCriteria The search criteria to use
	 * @param requestInfo    The request info for API calls
	 * @param mobileNumber   The mobile number to search for
	 * @return List of user UUIDs with this mobile number
	 */
	private List<String> findUsersByMobileNumber(RegularizationSearchCriteria searchCriteria, RequestInfo requestInfo,
			String mobileNumber) {

		// Clear owner IDs and set mobile number for search
		searchCriteria.setOwnerIds(null);
		searchCriteria.setMobileNumber(mobileNumber);

		// Search users by mobile number
		UserDetailResponse userInfoWithMobile = userService.getUser(searchCriteria, requestInfo);

		// Extract all user UUIDs
		List<String> ownerIds = new ArrayList<>();
		if (userInfoWithMobile != null && !CollectionUtils.isEmpty(userInfoWithMobile.getUser())) {
			userInfoWithMobile.getUser().forEach(user -> ownerIds.add(user.getUuid()));
		}

		return ownerIds;
	}

	/**
	 * Enriches regularization applications with owner details from the user
	 * service.
	 * 
	 * <p>
	 * This method performs the following operations:
	 * <ol>
	 * <li>Extracts all unique owner UUIDs from the regularization list</li>
	 * <li>Fetches user details for all owners in a single API call</li>
	 * <li>Maps user details back to corresponding owners in each
	 * regularization</li>
	 * </ol>
	 * </p>
	 * 
	 * @param regularizationList The list of regularizations to enrich with owner
	 *                           details
	 * @param requestInfo        The request info for user service API calls
	 */
	public void getOwnersDetails(List<Regularization> regularizationList, RequestInfo requestInfo) {

		// Step 1: Extract all unique owner UUIDs from regularizations
		List<String> ownersUuids = extractOwnerUuids(regularizationList);

		if (CollectionUtils.isEmpty(ownersUuids)) {
			log.debug("No owner UUIDs found to enrich");
			return;
		}

		// Step 2: Fetch user details for all owners
		UserDetailResponse response = userService.getUsersInfo(ownersUuids, requestInfo);

		// Step 3: Map user details back to owners
		if (!ObjectUtils.isEmpty(response) && !CollectionUtils.isEmpty(response.getUser())) {
			mapUserDetailsToOwners(regularizationList, response);
		}
	}

	/**
	 * Extracts all unique owner UUIDs from a list of regularizations.
	 * 
	 * @param regularizationList The list of regularizations
	 * @return List of owner UUIDs
	 */
	private List<String> extractOwnerUuids(List<Regularization> regularizationList) {

		return regularizationList.stream()
				.filter(regularization -> !CollectionUtils.isEmpty(regularization.getOwners()))
				.flatMap(regularization -> regularization.getOwners().stream()).map(owner -> owner.getUuid()).distinct()
				.collect(Collectors.toList());
	}

	/**
	 * Maps user details from user service response to corresponding owners in
	 * regularizations.
	 * 
	 * @param regularizationList The list of regularizations containing owners
	 * @param response           The user service response containing user details
	 */
	private void mapUserDetailsToOwners(List<Regularization> regularizationList, UserDetailResponse response) {

		response.getUser().forEach(user -> {
			regularizationList.forEach(regularization -> {
				regularization.getOwners().forEach(owner -> {
					// Match owner by UUID and enrich with user details
					if (owner.getUuid().equals(user.getUuid())) {
						owner.addUserWithoutAuditDetail(user);
					}
				});
			});
		});
	}

	/**
	 * Service layer for updating a Regularization application.
	 * 
	 * <p>
	 * This method orchestrates the update process by:
	 * <ol>
	 * <li>Validates application type</li>
	 * <li>Routes to specialized handlers for specific update scenarios:
	 * <ul>
	 * <li>Data-only updates (no workflow transition)</li>
	 * <li>Site plan layout signing (Land)</li>
	 * <li>Building plan layout signing (Building)</li>
	 * <li>Land and building plan layout signing</li>
	 * <li>Regularization deletion</li>
	 * </ul>
	 * </li>
	 * <li>For standard updates: validates, enriches, processes workflow, and
	 * persists</li>
	 * </ol>
	 * </p>
	 * 
	 * @param regularizationRequest The regularization update request
	 * @return The updated regularization entity
	 * @throws CustomException if application type is invalid or application not
	 *                         found
	 */
	public Regularization update(@Valid RegularizationRequest regularizationRequest) {

		RequestInfo requestInfo = regularizationRequest.getRequestInfo();
		Regularization regularization = regularizationRequest.getRegularization();

		// Step 1: Validate application type
		validateApplicationTypeForUpdate(regularization);

		// Step 2: Route to specialized handlers for specific update scenarios
		Regularization specializedResult = routeToSpecializedUpdateHandler(regularizationRequest);
		if (specializedResult != null) {
			return specializedResult;
		}

		// Step 3: Process standard workflow update
		return processStandardWorkflowUpdate(regularizationRequest, requestInfo, regularization);
	}

	/**
	 * Validates that the application type is specified for update operation.
	 * 
	 * @param regularization The regularization entity to validate
	 * @throws CustomException if application type is null
	 */
	private void validateApplicationTypeForUpdate(Regularization regularization) {
		if (Objects.isNull(regularization.getAppType())) {
			throw new CustomException("INVALID_REGULARIZATION_UPDATE",
					"Application Type does not fall in either Land or Building or Land & Building");
		}
	}

	/**
	 * Routes to specialized update handlers based on the request type.
	 * 
	 * <p>
	 * Handles the following specialized scenarios:
	 * <ul>
	 * <li>Data-only update (no workflow transition)</li>
	 * <li>Site plan layout document signing</li>
	 * <li>Building plan layout document signing</li>
	 * <li>Land and building plan layout document signing</li>
	 * <li>Regularization deletion</li>
	 * </ul>
	 * </p>
	 * 
	 * @param regularizationRequest The update request
	 * @return The updated regularization if handled by a specialized handler, null
	 *         otherwise
	 */
	private Regularization routeToSpecializedUpdateHandler(RegularizationRequest regularizationRequest) {

		Regularization regularization = regularizationRequest.getRegularization();

		// Handle data-only update (no workflow transition)
		if (regularization.getWorkflow().getAction().equalsIgnoreCase(RegularizationConstants.ACTION_UPDATE_DATA)) {
			return updateDataOnly(regularizationRequest);
		}

		// Handle site plan layout signing for Land regularization
		if (isRequestForSitePlanLayoutSign(regularizationRequest)) {
			return processSitePlanLayoutSignRequest(regularizationRequest);
		}

		// Handle building plan layout signing for Building regularization
		if (isRequestForBuildingPlanLayoutSign(regularizationRequest)) {
			return processBuildingPlanLayoutSignRequest(regularizationRequest);
		}

		// Handle plan layout signing for Land and Building regularization
		if (isRequestForLandAndBuildingPlanLayoutSign(regularizationRequest)) {
			return processLandAndBuildingPlanLayoutSignRequest(regularizationRequest);
		}

		// Handle regularization deletion
		if (isRequestForRegularizationDeletion(regularizationRequest)) {
			return processRegularizationDeletion(regularizationRequest);
		}

		// No specialized handler matched - proceed with standard update
		return null;
	}

	/**
	 * Processes standard workflow update for regularization applications.
	 * 
	 * <p>
	 * This method handles the complete update flow:
	 * <ol>
	 * <li>Fetches MDMS data for validation</li>
	 * <li>Searches and validates existing application</li>
	 * <li>Performs all validations (update, duplicate docs, pull back, show
	 * cause)</li>
	 * <li>Enriches request data</li>
	 * <li>Handles reject/send back actions</li>
	 * <li>Processes workflow transition</li>
	 * <li>Generates fee demands if applicable</li>
	 * <li>Persists the update</li>
	 * </ol>
	 * </p>
	 * 
	 * @param regularizationRequest The update request
	 * @param requestInfo           The request info
	 * @param regularization        The regularization entity
	 * @return The updated regularization
	 */
	private Regularization processStandardWorkflowUpdate(RegularizationRequest regularizationRequest,
			RequestInfo requestInfo, Regularization regularization) {

		// Fetch MDMS data for validation
		Object mdmsData = util.mDMSCall(requestInfo, RegularizationConstants.STATE_TENANTID);

		// Search and validate existing application
		List<Regularization> searchResult = fetchAndValidateExistingApplication(regularization, requestInfo);

		// Get business service for workflow operations
		BusinessService businessService = workflowService.getBusinessService(regularization, requestInfo,
				regularization.getApplicationNo());

		// Perform all validations
		performUpdateValidations(regularizationRequest);

		// Enrich request data (documents, DSC details, etc.)
		enrichmentService.enrichRegularizationUpdateRequest(regularizationRequest);

		// Handle reject and send back workflow actions
		handleRejectSendBackActions(regularizationRequest, businessService, searchResult, mdmsData);

		// Handle refusal show cause notice if applicable
		handleRefusalShowCauseNotice(regularizationRequest);

		// Execute workflow transition
		workflowService.callWorkFlow(requestInfo, regularization);
		log.info("Workflow transition completed. New status: {}", regularization.getStatus());

		// Post-workflow enrichment (approval number, application date, etc.)
		enrichmentService.postStatusEnrichment(regularizationRequest, mdmsData);

		// Handle pull back from pending sanction fee status
		handleIfPullBackRequest(regularizationRequest);

		// Clean up preview permit from additional details
		cleanupPreviewPermitDetails(regularization);

		// Generate fee demands if applicable
		generateFeeDemandIfApplicable(regularizationRequest, regularization);

		// Persist update to database via Kafka
		repository.update(regularizationRequest,
				workflowService.isStateUpdatable(regularization.getStatus(), businessService));

		return regularization;
	}

	/**
	 * Fetches and validates existing application for update.
	 * 
	 * @param regularization The regularization with ID to search
	 * @param requestInfo    The request info
	 * @return List containing exactly one matching regularization
	 * @throws CustomException if no application found or multiple applications
	 *                         found
	 */
	private List<Regularization> fetchAndValidateExistingApplication(Regularization regularization,
			RequestInfo requestInfo) {

		RegularizationSearchCriteria searchCriteria = RegularizationSearchCriteria.builder()
				.ids(Arrays.asList(regularization.getId())).tenantId(regularization.getTenantId()).build();

		List<Regularization> searchResult = searchRegularization(searchCriteria, requestInfo);

		if (CollectionUtils.isEmpty(searchResult) || searchResult.size() > 1) {
			throw new CustomException(RegularizationConstants.UPDATE_ERROR,
					"Failed to Update the Application, Found None or multiple applications !");
		}

		return searchResult;
	}

	/**
	 * Performs all validation checks required for update operation.
	 * 
	 * @param regularizationRequest The update request to validate
	 */
	private void performUpdateValidations(RegularizationRequest regularizationRequest) {

		// Validate basic update request
		regularizationValidator.validateRegularizationUpdateRequest(regularizationRequest);

		// Validate no duplicate documents
		regularizationValidator.validateDuplicateDocs(regularizationRequest);

		// Validate pull back action is allowed
		regularizationValidator.validatePullBackAction(regularizationRequest);

		// Validate refusal show cause conditions
		regularizationValidator.validateShowCauseRefusal(regularizationRequest);
	}

	/**
	 * Cleans up preview permit details from additional details map.
	 * 
	 * @param regularization The regularization entity
	 */
	@SuppressWarnings("unchecked")
	private void cleanupPreviewPermitDetails(Regularization regularization) {

		Map<String, String> additionalDetails = regularization.getAdditionalDetails() != null
				? (Map<String, String>) regularization.getAdditionalDetails()
				: new HashMap<>();

		if (additionalDetails.get(BPAConstants.PREVIEW_PERMIT_ADDITIONAL_DETAILS) != null) {
			additionalDetails.remove(BPAConstants.PREVIEW_PERMIT_ADDITIONAL_DETAILS);
		}
	}

	/**
	 * Generates fee demand based on the current application status.
	 * 
	 * <p>
	 * Fee demands are generated at specific workflow states:
	 * <ul>
	 * <li><b>SANC_FEE_STATE:</b> Generates sanction fee demand</li>
	 * <li><b>APPL_FEE_STATE:</b> Generates application fee demand</li>
	 * </ul>
	 * </p>
	 * 
	 * @param regularizationRequest The update request
	 * @param regularization        The regularization entity
	 */
	private void generateFeeDemandIfApplicable(RegularizationRequest regularizationRequest,
			Regularization regularization) {

		String status = regularization.getStatus();

		if (status.equalsIgnoreCase(RegularizationConstants.SANC_FEE_STATE)) {
			log.info("Generating sanction fee demand for application: {}", regularization.getApplicationNo());
			calculationService.addCalculation(regularizationRequest, RegularizationConstants.SANCTION_FEE_KEY);

		} else if (status.equalsIgnoreCase(RegularizationConstants.APPL_FEE_STATE)) {
			log.info("Generating application fee demand for application: {}", regularization.getApplicationNo());
			calculationService.addCalculation(regularizationRequest, RegularizationConstants.APPLICATION_FEE_KEY);
		}
	}

	/**
	 * process update data request only, no workflow transition will happen here
	 * 
	 * @param regularizationRequest
	 * @return
	 */
	private Regularization updateDataOnly(@Valid RegularizationRequest regularizationRequest) {

		Regularization regularization = regularizationRequest.getRegularization();

		log.info("Processing data update only for appl number: " + regularization.getApplicationNo());

		// Create Search Criteria for Regularization Search
		RegularizationSearchCriteria searchCriteria = RegularizationSearchCriteria.builder()
				.ids(Arrays.asList(regularization.getId())).tenantId(regularization.getTenantId()).build();

		// Search Regularization Application in DB for update
		List<Regularization> searchResult = searchRegularization(searchCriteria,
				regularizationRequest.getRequestInfo());

		// Throw exception if no application found in DB
		if (CollectionUtils.isEmpty(searchResult) || searchResult.size() > 1) {
			throw new CustomException(RegularizationConstants.UPDATE_ERROR,
					"Failed to Update the Application, Found None or multiple applications !");
		}

		// Throw exception if appl current status is not INITIATED
		if (!searchResult.get(0).getStatus().equalsIgnoreCase(RegularizationConstants.INITIATED)) {
			throw new CustomException(RegularizationConstants.UPDATE_ERROR,
					"Application Not at INITIATED stage.. Mentioned action is only allowed at INITIATED !");
		}

		// Enrich the required data for Update here like Documents, Plot Details
		enrichmentService.enrichRegularizationUpdateRequest(regularizationRequest);

		repository.update(regularizationRequest, Boolean.FALSE);

		return regularizationRequest.getRegularization();
	}

	/**
	 * Process Building Plan Layout Sign request
	 * 
	 * @param regularizationRequest
	 * @return
	 */
	private Regularization processBuildingPlanLayoutSignRequest(@Valid RegularizationRequest regularizationRequest) {

		log.info("inside method BuildingPlanLayoutSignature");

		Regularization regularization = regularizationRequest.getRegularization();

		// Create Search Criteria for Regularization Search
		RegularizationSearchCriteria searchCriteria = RegularizationSearchCriteria.builder()
				.ids(Arrays.asList(regularization.getId())).tenantId(regularization.getTenantId()).build();

		// Search Regularization Application in DB for update
		List<Regularization> searchResult = searchRegularization(searchCriteria,
				regularizationRequest.getRequestInfo());

		// Throw exception if no application found in DB
		if (CollectionUtils.isEmpty(searchResult) || searchResult.size() > 1) {
			throw new CustomException(RegularizationConstants.UPDATE_ERROR,
					"Failed to Update the Application, Found None or multiple applications !");
		}

		Map<String, Object> additionalDetails = (Map) regularization.getAdditionalDetails();
		// additionalDetails will always be a Map and will surely contain
		// applicationType then only this method invoked-
		additionalDetails.remove(RegularizationConstants.APPLICATION_TYPE);
		additionalDetails.put(RegularizationConstants.BUILDING_PLAN_LAYOUT_IS_SIGNED, true);
		regularization.setAuditDetails(searchResult.get(0).getAuditDetails());
		Optional<Document> sitePlanLayoutDocument = searchResult.get(0).getDocuments().stream()
				.filter(document -> document.getDocumentType().equals(RegularizationConstants.DOC_TYPE_BUILDING_PLAN))
				.findFirst();
		if (sitePlanLayoutDocument.isPresent()) {
			Map<String, Object> unsignedSitePlanLayoutDetails = new HashMap<>();
			unsignedSitePlanLayoutDetails.put(RegularizationConstants.CODE,
					sitePlanLayoutDocument.get().getDocumentType());
			unsignedSitePlanLayoutDetails.put(RegularizationConstants.FILESTOREID,
					sitePlanLayoutDocument.get().getFileStoreId());
			additionalDetails.put(RegularizationConstants.UNSIGNED_BUILDING_PLAN_LAYOUT_DETAILS,
					unsignedSitePlanLayoutDetails);
		}

		// setting IDs for newly added Regularization Documents
		if (!CollectionUtils.isEmpty(regularization.getDocuments()))
			regularization.getDocuments().forEach(document -> {
				if (document.getId() == null) {
					document.setId(UUID.randomUUID().toString());
				}
			});

		repository.update(regularizationRequest, true);
		return regularizationRequest.getRegularization();

	}

	/**
	 * Check if the request is for Building Plan Layout Sign
	 * 
	 * @param regularizationRequest
	 * @return
	 */
	private boolean isRequestForBuildingPlanLayoutSign(@Valid RegularizationRequest regularizationRequest) {
		return ((regularizationRequest.getRegularization().getStatus()
				.equalsIgnoreCase(RegularizationConstants.APPROVED)
				|| regularizationRequest.getRegularization().getStatus()
						.equalsIgnoreCase(RegularizationConstants.SANC_FEE_STATE))
				&& Objects.nonNull(regularizationRequest.getRegularization().getAdditionalDetails())
				&& regularizationRequest.getRegularization().getAdditionalDetails() instanceof Map
				&& (RegularizationConstants.BUILDING_PLAN_LAYOUT_SIGN)
						.equals(((Map) regularizationRequest.getRegularization().getAdditionalDetails())
								.get(RegularizationConstants.APPLICATION_TYPE))
				&& !(Objects
						.nonNull(((Map) regularizationRequest.getRegularization().getAdditionalDetails())
								.get(RegularizationConstants.BUILDING_PLAN_LAYOUT_IS_SIGNED))
						&& ((boolean) ((Map) regularizationRequest.getRegularization().getAdditionalDetails())
								.get(RegularizationConstants.BUILDING_PLAN_LAYOUT_IS_SIGNED))));
	}

	/**
	 * Enrich the Regularization Request, if the action sent in Request is Reject or
	 * Send Back
	 * 
	 * @param regularizationRequest
	 * @param businessService
	 * @param searchResult
	 * @param mdmsData
	 */
	private void handleRejectSendBackActions(@Valid RegularizationRequest regularizationRequest,
			BusinessService businessService, List<Regularization> searchResult, Object mdmsData) {

		Regularization regularization = regularizationRequest.getRegularization();
		if (regularization.getWorkflow().getAction() != null
				&& (regularization.getWorkflow().getAction().equalsIgnoreCase(BPAConstants.ACTION_REJECT)
						|| regularization.getWorkflow().getAction().equalsIgnoreCase(BPAConstants.ACTION_REVOCATE))) {

			if (regularization.getWorkflow().getComments() == null
					|| regularization.getWorkflow().getComments().isEmpty()) {
				throw new CustomException(RegularizationConstants.UPDATE_ERROR_COMMENT_REQUIRED,
						"Comment is mandaotory, please provide the comments ");
			}

			// Check If Refusal SCN has been Created or not for the application
			// validateRefusalScn(regularization);

		} else {

			if (!regularization.getWorkflow().getAction().equalsIgnoreCase(BPAConstants.ACTION_SENDBACKTOCITIZEN)) {

				// Validate the workflow related validations here
				actionValidator.validateUpdateRequest(regularizationRequest, businessService);

				regularizationValidator.validateUpdate(regularizationRequest, searchResult, mdmsData,
						workflowService.getCurrentState(regularization.getStatus(), businessService));

			}
		}

	}

	/**
	 * Service Layer for Fee Estimate for Estimate API, to be called from Front End
	 * 
	 * @param regularizationRequest
	 * @return
	 */
	public Object getFeeEstimateFromBpaCalculator(Object regularizationRequest) {
		return calculationService.callBpaCalculatorEstimate(regularizationRequest);
	}

	/**
	 * Service layer for update Regularization DSC Details
	 * 
	 * @param regularizationRequest
	 * @return Regularization Response
	 */
	/**
	 * Updates DSC (Digital Signature Certificate) details for a regularization
	 * application.
	 * 
	 * <p>
	 * This method handles the digital signature update workflow:
	 * <ol>
	 * <li>Searches and validates the existing regularization application</li>
	 * <li>Validates the DSC details in the request</li>
	 * <li>Enriches the request with approval date and validity date</li>
	 * <li>Persists the DSC details update via Kafka</li>
	 * </ol>
	 * </p>
	 * 
	 * <p>
	 * This method is called when an approver digitally signs the regularization
	 * permit document, capturing the signature details and setting validity.
	 * </p>
	 * 
	 * @param regularizationRequest The request containing DSC details to update
	 * @return The updated regularization entity
	 * @throws CustomException if application not found or multiple applications
	 *                         found
	 */
	public Regularization updateRegularizationDscDetails(@Valid RegularizationRequest regularizationRequest) {

		RequestInfo requestInfo = regularizationRequest.getRequestInfo();
		Regularization regularization = regularizationRequest.getRegularization();

		// Step 1: Search and validate existing application
		List<Regularization> searchResult = fetchAndValidateApplicationForDscUpdate(regularization, requestInfo);

		// Step 2: Validate DSC details in the request
		regularizationValidator.validateRegularizationDscDetails(regularizationRequest, searchResult);

		// Step 3: Enrich request with approval date and validity date
		enrichmentService.enrichRegularizationDscDetailsUpdateRequest(regularizationRequest, searchResult.get(0));

		// Step 4: Persist DSC details update via Kafka
		repository.updateRegularizationDscDetails(regularizationRequest);

		return regularization;
	}

	/**
	 * Fetches and validates existing regularization application for DSC update.
	 * 
	 * <p>
	 * Ensures exactly one application exists with the given ID and tenant ID.
	 * </p>
	 * 
	 * @param regularization The regularization containing ID and tenant ID
	 * @param requestInfo    The request info for search operation
	 * @return List containing exactly one matching regularization
	 * @throws CustomException if no application found or multiple applications
	 *                         found
	 */
	private List<Regularization> fetchAndValidateApplicationForDscUpdate(Regularization regularization,
			RequestInfo requestInfo) {

		// Build search criteria
		RegularizationSearchCriteria searchCriteria = RegularizationSearchCriteria.builder()
				.ids(Arrays.asList(regularization.getId())).tenantId(regularization.getTenantId()).build();

		// Execute search
		List<Regularization> searchResult = searchRegularization(searchCriteria, requestInfo);

		// Validate exactly one result
		if (CollectionUtils.isEmpty(searchResult) || searchResult.size() > 1) {
			throw new CustomException(RegularizationConstants.UPDATE_ERROR,
					"Failed to Update the Application, Found None or multiple applications !");
		}

		return searchResult;
	}

	/**
	 * Service layer for searching Regularization DSC Details
	 * 
	 * @param searchCriteria
	 * @param requestInfo
	 * @return List<RegularizationDscDetails>
	 */
	public List<RegularizationDscDetails> searchRegularizationDscDetails(RegularizationSearchCriteria searchCriteria,
			RequestInfo requestInfo) {

		// create a new Criteria
		regularizationValidator.validateDscSearch(requestInfo, searchCriteria);
		List<RegularizationDscDetails> regularizationDscDetailsList = repository
				.searchRegularizationDscDetails(searchCriteria, requestInfo);

		return regularizationDscDetailsList;
	}

	/**
	 * Create Building Regularization Request From the provided Request Payload
	 * 
	 * @param regularizationRequest
	 * @param requestInfo
	 * @param regularization
	 */
	/**
	 * Creates a Building Regularization application.
	 * 
	 * <p>
	 * This method handles the complete creation flow for building regularization:
	 * <ol>
	 * <li>Validates the building-specific create request</li>
	 * <li>Enriches the request with required fields (IDs, audit details, building
	 * info, etc.)</li>
	 * <li>Initiates the workflow for the application</li>
	 * <li>Persists the application via Kafka</li>
	 * </ol>
	 * </p>
	 * 
	 * @param regularizationRequest The regularization request
	 * @param requestInfo           The request info containing user context
	 * @param regularization        The regularization entity to create
	 */
	private void createBuildingRegularization(@Valid RegularizationRequest regularizationRequest,
			RequestInfo requestInfo, Regularization regularization) {

		// Validate building-specific create request
		regularizationValidator.validateBuildingCreate(requestInfo, regularization);

		// Enrich the required fields for Building Create Request
		enrichmentService.enrichBuildingCreateRequest(requestInfo, regularization);

		// Initiate workflow for the application
		workflowService.callWorkFlow(requestInfo, regularization);

		// Persist the request through Kafka
		repository.save(regularizationRequest);
	}

	/**
	 * @param regularizationRequest
	 * @return Map of all owners name and mobile number
	 */
	/**
	 * Retrieves a map of owner mobile numbers to owner names for notification
	 * purposes.
	 * 
	 * <p>
	 * This method is used to fetch owner contact details for sending SMS
	 * notifications during workflow transitions. Notifications are skipped for
	 * specific workflow states to avoid duplicate or inappropriate notifications.
	 * </p>
	 * 
	 * <p>
	 * Notifications are skipped when:
	 * <ul>
	 * <li>Action is SEND_TO_CITIZEN and status is INITIATED (initial
	 * submission)</li>
	 * <li>Action is APPROVE and status is INPROGRESS (approval in progress)</li>
	 * </ul>
	 * </p>
	 * 
	 * @param regularizationRequest The regularization request containing
	 *                              application details
	 * @return Map of mobile number to owner name, or null if notifications should
	 *         be skipped
	 */
	public Map<String, String> getOwnersList(RegularizationRequest regularizationRequest) {

		Regularization regularization = regularizationRequest.getRegularization();

		// Check if notification should be skipped for this workflow state
		if (shouldSkipOwnerNotification(regularization)) {
			return null;
		}

		// Fetch regularization with owner details
		List<Regularization> regularizationList = fetchRegularizationForNotification(regularization,
				regularizationRequest.getRequestInfo());

		// Extract mobile number to owner name mapping
		return extractMobileNumberToOwnerMap(regularizationList);
	}

	/**
	 * Determines if owner notification should be skipped based on workflow state.
	 * 
	 * <p>
	 * Notifications are skipped in specific workflow states to prevent duplicate or
	 * premature notifications to owners.
	 * </p>
	 * 
	 * @param regularization The regularization entity with workflow details
	 * @return true if notification should be skipped, false otherwise
	 */
	private boolean shouldSkipOwnerNotification(Regularization regularization) {

		String action = regularization.getWorkflow().getAction();
		String status = regularization.getStatus();

		// Skip notification for initial submission to citizen
		boolean isInitialSendToCitizen = action.equals(config.getActionsendtocitizen()) && status.equals("INITIATED");

		// Skip notification for approval in progress
		boolean isApprovalInProgress = action.equals(config.getActionapprove()) && status.equals("INPROGRESS");

		return isInitialSendToCitizen || isApprovalInProgress;
	}

	/**
	 * Fetches regularization application with owner details for notification.
	 * 
	 * @param regularization The regularization containing application number and
	 *                       tenant ID
	 * @param requestInfo    The request info for search operation
	 * @return List of regularization applications with owner details
	 */
	private List<Regularization> fetchRegularizationForNotification(Regularization regularization,
			RequestInfo requestInfo) {

		RegularizationSearchCriteria searchCriteria = RegularizationSearchCriteria.builder()
				.tenantId(regularization.getTenantId()).applicationNo(regularization.getApplicationNo()).build();

		return searchRegularization(searchCriteria, requestInfo);
	}

	/**
	 * Extracts a map of mobile numbers to owner names from regularization list.
	 * 
	 * <p>
	 * Filters out owners without mobile numbers and creates a map for sending SMS
	 * notifications.
	 * </p>
	 * 
	 * @param regularizationList List of regularizations containing owner details
	 * @return Map of mobile number to owner name
	 */
	private Map<String, String> extractMobileNumberToOwnerMap(List<Regularization> regularizationList) {

		return regularizationList.stream()
				// Filter regularizations that have owners
				.filter(reg -> !reg.getOwners().isEmpty())
				// Flatten to stream of owners
				.flatMap(reg -> reg.getOwners().stream())
				// Filter owners with valid mobile numbers
				.filter(owner -> !ObjectUtils.isEmpty(owner.getMobileNumber()))
				// Create map of mobile number to owner name
				.collect(Collectors.toMap(OwnerInfo::getMobileNumber, OwnerInfo::getName));
	}

	/**
	 * Retrieves a map of owner email addresses to owner names for notification
	 * purposes.
	 * 
	 * <p>
	 * This method is used to fetch owner contact details for sending email
	 * notifications during workflow transitions. Notifications are skipped for
	 * specific workflow states to avoid duplicate or inappropriate notifications.
	 * </p>
	 * 
	 * <p>
	 * Notifications are skipped when:
	 * <ul>
	 * <li>Action is SEND_TO_CITIZEN and status is INITIATED (initial
	 * submission)</li>
	 * <li>Action is APPROVE and status is INPROGRESS (approval in progress)</li>
	 * </ul>
	 * </p>
	 * 
	 * @param regularizationRequest The regularization request containing
	 *                              application details
	 * @return Map of email address to owner name, or null if notifications should
	 *         be skipped
	 */
	public Map<String, String> getUserEmailList(RegularizationRequest regularizationRequest) {

		Regularization regularization = regularizationRequest.getRegularization();

		// Check if notification should be skipped for this workflow state
		if (shouldSkipOwnerNotification(regularization)) {
			return null;
		}

		// Fetch regularization with owner details
		List<Regularization> regularizationList = fetchRegularizationForNotification(regularization,
				regularizationRequest.getRequestInfo());

		// Extract email to owner name mapping
		return extractEmailToOwnerMap(regularizationList);
	}

	/**
	 * Extracts a map of email addresses to owner names from regularization list.
	 * 
	 * <p>
	 * Filters out owners without email addresses and creates a map for sending
	 * email notifications.
	 * </p>
	 * 
	 * @param regularizationList List of regularizations containing owner details
	 * @return Map of email address to owner name
	 */
	private Map<String, String> extractEmailToOwnerMap(List<Regularization> regularizationList) {

		return regularizationList.stream()
				// Filter regularizations that have owners
				.filter(reg -> !reg.getOwners().isEmpty())
				// Flatten to stream of owners
				.flatMap(reg -> reg.getOwners().stream())
				// Filter owners with valid email addresses
				.filter(owner -> !ObjectUtils.isEmpty(owner.getEmailId()))
				// Create map of email to owner name
				.collect(Collectors.toMap(OwnerInfo::getEmailId, OwnerInfo::getName));
	}

	/**
	 * Search Application with Regularization ID
	 * 
	 * @param request
	 * @return
	 */
	public List<Regularization> getRegularizationWithRegularizationId(RegularizationRequest request) {
		RegularizationSearchCriteria criteria = new RegularizationSearchCriteria();
		List<String> ids = new LinkedList<>();
		ids.add(request.getRegularization().getId());
		criteria.setTenantId(request.getRegularization().getTenantId());
		criteria.setIds(ids);
		List<Regularization> regularization = repository.searchRegularization(criteria, request.getRequestInfo());
		return regularization;
	}

	/**
	 * Update the Permit letter filestore in DB
	 * 
	 * 
	 * @param regularizationRequest
	 * @return
	 */
	public Regularization permitLetterUpdate(@Valid RegularizationRequest regularizationRequest) {

		List<Regularization> searchResult = getRegularizationWithRegularizationId(regularizationRequest);
		if (CollectionUtils.isEmpty(searchResult)) {
			throw new CustomException("Update error",
					"Failed to update Regularization , No application exists with mentioned Regularization ID");
		}

		repository.updatePermitLetterPreview(regularizationRequest);

		return regularizationRequest.getRegularization();
	}

	/**
	 * Search Application approved by logged in user
	 * 
	 * @param criteria
	 * @param uuid
	 * @return
	 */
	public List<RegularizationApprovedByApplicationSearch> searchApplicationApprovedBy(
			@Valid RegularizationSearchCriteria criteria, String uuid) {

		List<RegularizationApprovedByApplicationSearch> regularizationApprovedByApplicationSearchs = repository
				.getApprovedbyData(uuid, criteria);

		if (regularizationApprovedByApplicationSearchs.isEmpty()) {
			return Collections.emptyList();
		} else {
			this.populatedocumentdetailstoSearch(regularizationApprovedByApplicationSearchs);
			return regularizationApprovedByApplicationSearchs;
		}
	}

	/**
	 * Map Documents with searched data here
	 * 
	 * @param regularizationApprovedByApplicationSearchs
	 */
	private void populatedocumentdetailstoSearch(
			List<RegularizationApprovedByApplicationSearch> regularizationApprovedByApplicationSearchs) {

		List<String> regularizationIds = regularizationApprovedByApplicationSearchs.stream()
				.filter(bpa -> bpa.getRegularizationId() != null)
				.map(RegularizationApprovedByApplicationSearch::getRegularizationId).collect(Collectors.toList());
		List<RegularizationDocumentList> documentList = repository.getdocumentDataForApproveBy(regularizationIds);

		for (RegularizationApprovedByApplicationSearch application : regularizationApprovedByApplicationSearchs) {
			List<RegularizationDocumentList> docList = documentList.stream()
					.filter(doc -> doc.getRegularizationId().equals(application.getRegularizationId()))
					.collect(Collectors.toList());
			application.setDocuments(docList);
		}

	}

	/**
	 * Creates a new Field Inspection Report for a regularization application.
	 * 
	 * <p>
	 * This method orchestrates the field inspection report creation process:
	 * <ol>
	 * <li>Validates the mandatory application number in the request</li>
	 * <li>Validates the field inspection request data</li>
	 * <li>Verifies that exactly one regularization application exists</li>
	 * <li>Enriches the report with required metadata (ID, audit details)</li>
	 * <li>Persists the field inspection report via Kafka</li>
	 * </ol>
	 * </p>
	 * 
	 * <p>
	 * Field inspection reports capture on-site verification details conducted by
	 * officials during the regularization approval process.
	 * </p>
	 * 
	 * @param request The field inspection request containing report details
	 * @return The created field inspection report
	 * @throws CustomException if application number is missing or application not
	 *                         found
	 */
	public RegularizationFieldInspection createFieldInspectionReport(
			@Valid RegularizationFieldInspectionRequest request) {

		RegularizationFieldInspection fieldInspection = request.getFieldInspection();

		// Step 1: Validate mandatory application number
		validateApplicationNumberPresent(fieldInspection);

		// Step 2: Validate field inspection request data
		regularizationValidator.validatefieldInspectionRequest(fieldInspection);

		// Step 3: Verify regularization application exists
		validateRegularizationExists(fieldInspection, request.getRequestInfo());

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
	private void validateApplicationNumberPresent(RegularizationFieldInspection fieldInspection) {

		if (ObjectUtils.isEmpty(fieldInspection.getApplicationno())) {
			throw new CustomException("create error",
					"Failed to create feild inspection report, application no is mandatory");
		}
	}

	/**
	 * Validates that exactly one regularization application exists for the field
	 * inspection.
	 * 
	 * <p>
	 * This validation ensures:
	 * <ul>
	 * <li>At least one regularization application exists with the given application
	 * number</li>
	 * <li>No more than one application exists (prevents ambiguous
	 * associations)</li>
	 * </ul>
	 * </p>
	 * 
	 * @param fieldInspection The field inspection containing application number and
	 *                        tenant ID
	 * @param requestInfo     The request info for search operation
	 * @throws CustomException if no application found or multiple applications
	 *                         found
	 */
	private void validateRegularizationExists(RegularizationFieldInspection fieldInspection, RequestInfo requestInfo) {

		// Build search criteria
		RegularizationSearchCriteria searchCriteria = RegularizationSearchCriteria.builder()
				.applicationNo(fieldInspection.getApplicationno()).tenantId(fieldInspection.getTenantId()).build();

		// Search for the regularization application
		List<Regularization> regularizationList = repository.searchRegularization(searchCriteria, requestInfo);
		log.info("Found {} regularization(s) for application: {}", regularizationList.size(),
				fieldInspection.getApplicationno());

		// Validate exactly one application exists
		if (CollectionUtils.isEmpty(regularizationList) || regularizationList.size() > 1) {
			throw new CustomException("create error",
					"Failed to create feild inspection report, Found None or multiple bpa applications!");
		}
	}

	/**
	 * Service Layer to search Field Inspections from DB
	 * 
	 * @param criteria
	 * @param requestInfo
	 * @return
	 */
	public List<RegularizationFieldInspection> searchFieldInspectionReport(
			@Valid RegularizationFISearchCriteria criteria, RequestInfo requestInfo) {
		List<RegularizationFieldInspection> result = repository.getfieldInspectionReport(criteria);
		return result;
	}

	/**
	 * Create Land And Building Application Together with this method.
	 * 
	 * @param regularizationRequest
	 * @param requestInfo
	 * @param regularization
	 */
	/**
	 * Creates a combined Land and Building Regularization application.
	 * 
	 * <p>
	 * This method handles the complete creation flow for combined land and building
	 * regularization:
	 * <ol>
	 * <li>Validates the request with land-specific validations (includes all land
	 * checks)</li>
	 * <li>Enriches the request with building create fields (handles both land and
	 * building data)</li>
	 * <li>Initiates the workflow for the application</li>
	 * <li>Persists the application via Kafka</li>
	 * </ol>
	 * </p>
	 * 
	 * <p>
	 * <b>Note:</b> This type uses land validation but building enrichment as
	 * building enrichment handles the superset of both land and building data.
	 * </p>
	 * 
	 * @param regularizationRequest The regularization request
	 * @param requestInfo           The request info containing user context
	 * @param regularization        The regularization entity to create
	 */
	private void createLandAndBuildingRegularization(@Valid RegularizationRequest regularizationRequest,
			RequestInfo requestInfo, Regularization regularization) {

		// Validate create request with land regularization validations
		// (covers combined land and building requirements)
		regularizationValidator.validateLandCreate(requestInfo, regularization);

		// Enrich the required fields using building create request enrichment
		// (handles both land and building data structures)
		enrichmentService.enrichBuildingCreateRequest(requestInfo, regularization);

		// Initiate workflow for the application
		workflowService.callWorkFlow(requestInfo, regularization);

		// Persist the request through Kafka
		repository.save(regularizationRequest);
	}

	/**
	 * update the eg_bpa_regularization_document table to unlink the old filestoreid
	 * and link the new filestoreid for the document APPL.SITEPLAN.SITEPLANLAYOUT
	 * 
	 * @param regularizationRequest
	 */
	private Regularization processSitePlanLayoutSignRequest(@Valid RegularizationRequest regularizationRequest) {

		log.info("inside method siteBuildingPlanLayoutSignature");

		Regularization regularization = regularizationRequest.getRegularization();

		// Create Search Criteria for Regularization Search
		RegularizationSearchCriteria searchCriteria = RegularizationSearchCriteria.builder()
				.ids(Arrays.asList(regularization.getId())).tenantId(regularization.getTenantId()).build();

		// Search Regularization Application in DB for update
		List<Regularization> searchResult = searchRegularization(searchCriteria,
				regularizationRequest.getRequestInfo());

		// Throw exception if no application found in DB
		if (CollectionUtils.isEmpty(searchResult) || searchResult.size() > 1) {
			throw new CustomException(RegularizationConstants.UPDATE_ERROR,
					"Failed to Update the Application, Found None or multiple applications !");
		}

		Map<String, Object> additionalDetails = (Map) regularization.getAdditionalDetails();
		// additionalDetails will always be a Map and will surely contain
		// applicationType then only this method invoked-
		additionalDetails.remove(RegularizationConstants.APPLICATION_TYPE);
		additionalDetails.put(RegularizationConstants.SITE_PLAN_LAYOUT_IS_SIGNED, true);
		regularization.setAuditDetails(searchResult.get(0).getAuditDetails());
		Optional<Document> sitePlanLayoutDocument = searchResult.get(0).getDocuments().stream()
				.filter(document -> document.getDocumentType().equals(RegularizationConstants.DOC_TYPE_SITE_PLAN))
				.findFirst();
		if (sitePlanLayoutDocument.isPresent()) {
			Map<String, Object> unsignedSitePlanLayoutDetails = new HashMap<>();
			unsignedSitePlanLayoutDetails.put(RegularizationConstants.CODE,
					sitePlanLayoutDocument.get().getDocumentType());
			unsignedSitePlanLayoutDetails.put(RegularizationConstants.FILESTOREID,
					sitePlanLayoutDocument.get().getFileStoreId());
			additionalDetails.put(RegularizationConstants.UNSIGNED_SITE_PLAN_LAYOUT_DETAILS,
					unsignedSitePlanLayoutDetails);
		}

		// setting IDs for newly added Regularization Documents
		if (!CollectionUtils.isEmpty(regularization.getDocuments()))
			regularization.getDocuments().forEach(document -> {
				if (document.getId() == null) {
					document.setId(UUID.randomUUID().toString());
				}
			});

		repository.update(regularizationRequest, true);
		return regularizationRequest.getRegularization();
	}

	/**
	 * Check if the request is for Site Plan Layout Sign
	 * 
	 * @param regularizationRequest
	 * @return
	 */
	private boolean isRequestForSitePlanLayoutSign(@Valid RegularizationRequest regularizationRequest) {

		return ((regularizationRequest.getRegularization().getStatus()
				.equalsIgnoreCase(RegularizationConstants.APPROVED)
				|| regularizationRequest.getRegularization().getStatus()
						.equalsIgnoreCase(RegularizationConstants.SANC_FEE_STATE))
				&& Objects.nonNull(regularizationRequest.getRegularization().getAdditionalDetails())
				&& regularizationRequest.getRegularization().getAdditionalDetails() instanceof Map
				&& (RegularizationConstants.SITE_PLAN_LAYOUT_SIGN)
						.equals(((Map) regularizationRequest.getRegularization().getAdditionalDetails())
								.get(RegularizationConstants.APPLICATION_TYPE))
				&& !(Objects
						.nonNull(((Map) regularizationRequest.getRegularization().getAdditionalDetails())
								.get(RegularizationConstants.SITE_PLAN_LAYOUT_IS_SIGNED))
						&& ((boolean) ((Map) regularizationRequest.getRegularization().getAdditionalDetails())
								.get(RegularizationConstants.SITE_PLAN_LAYOUT_IS_SIGNED))));
	}

	/**
	 * Process Regularization request, if it is for Deletion
	 * 
	 * @param regularizationRequest
	 * @return
	 */
	private Regularization processRegularizationDeletion(@Valid RegularizationRequest regularizationRequest) {

		Regularization regularization = regularizationRequest.getRegularization();

		// Create Search Criteria for Regularization Search
		RegularizationSearchCriteria searchCriteria = RegularizationSearchCriteria.builder()
				.ids(Arrays.asList(regularization.getId())).tenantId(regularization.getTenantId()).build();

		// Search Regularization Application in DB for update
		List<Regularization> searchResult = searchRegularization(searchCriteria,
				regularizationRequest.getRequestInfo());

		// Throw exception if no application found in DB
		if (CollectionUtils.isEmpty(searchResult) || searchResult.size() > 1) {
			throw new CustomException(RegularizationConstants.UPDATE_ERROR,
					"Failed to Update the Application, Found None or multiple applications !");
		}

		regularization.setStatus(RegularizationConstants.DELETED);
		repository.update(regularizationRequest, true);
		return regularizationRequest.getRegularization();
	}

	/**
	 * Check if regularization request is for Deletion
	 * 
	 * @param regularizationRequest
	 * @return
	 */
	private boolean isRequestForRegularizationDeletion(@Valid RegularizationRequest regularizationRequest) {

		Regularization regularization = regularizationRequest.getRegularization();
		String action = regularization.getWorkflow().getAction();
		if (action.equalsIgnoreCase(RegularizationConstants.ACTION_DELETE)) {
			regularizationValidator.validateRegularizationDeletion(regularization);
			return true;
		}
		return false;
	}

	public RegularizationFieldInspection updateFieldInspectionReport(
			@Valid RegularizationFieldInspectionRequest request) {

		RegularizationFieldInspection fieldInspection = request.getFieldInspection();

		if (ObjectUtils.isEmpty(fieldInspection.getApplicationno()))
			throw new CustomException("update error",
					"Failed to update feild inspection report, application no is mandatory");
		regularizationValidator.validatefieldInspectionRequest(fieldInspection);

		RegularizationSearchCriteria searchCriteria = RegularizationSearchCriteria.builder()
				.applicationNo(fieldInspection.getApplicationno()).tenantId(fieldInspection.getTenantId()).build();
		List<Regularization> regularizationList = repository.searchRegularization(searchCriteria,
				request.getRequestInfo());
		log.info(regularizationList.toString());

		if (CollectionUtils.isEmpty(regularizationList) || regularizationList.size() > 1) {
			throw new CustomException("update error",
					"Failed to update feild inspection report, Found None or multiple bpa applications!");
		}

		enrichmentService.enrichUpdateFieldInspectionReport(request);
		repository.updateFieldInspectionReport(request);
		// return the object
		return request.getFieldInspection();
	}

	/**
	 * Check if the request is for Land And Building Plan Layout Sign
	 * 
	 * @param regularizationRequest
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	private boolean isRequestForLandAndBuildingPlanLayoutSign(@Valid RegularizationRequest regularizationRequest) {
		return ((regularizationRequest.getRegularization().getStatus()
				.equalsIgnoreCase(RegularizationConstants.APPROVED)
				|| regularizationRequest.getRegularization().getStatus()
						.equalsIgnoreCase(RegularizationConstants.SANC_FEE_STATE))
				&& Objects.nonNull(regularizationRequest.getRegularization().getAdditionalDetails())
				&& regularizationRequest.getRegularization().getAdditionalDetails() instanceof Map
				&& (RegularizationConstants.LAND_AND_BUILDING_PLAN_LAYOUT_SIGN)
						.equals(((Map) regularizationRequest.getRegularization().getAdditionalDetails())
								.get(RegularizationConstants.APPLICATION_TYPE))
				&& !(Objects
						.nonNull(((Map) regularizationRequest.getRegularization().getAdditionalDetails())
								.get(RegularizationConstants.LAND_AND_BUILDING_PLAN_LAYOUT_IS_SIGNED))
						&& ((boolean) ((Map) regularizationRequest.getRegularization().getAdditionalDetails())
								.get(RegularizationConstants.LAND_AND_BUILDING_PLAN_LAYOUT_IS_SIGNED))));
	}

	/**
	 * Process Building Plan Layout Sign request
	 * 
	 * @param regularizationRequest
	 * @return
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private Regularization processLandAndBuildingPlanLayoutSignRequest(
			@Valid RegularizationRequest regularizationRequest) {

		log.info("inside method LandAndBuildingPlanLayoutSignature");

		Regularization regularization = regularizationRequest.getRegularization();

		// Create Search Criteria for Regularization Search
		RegularizationSearchCriteria searchCriteria = RegularizationSearchCriteria.builder()
				.ids(Arrays.asList(regularization.getId())).tenantId(regularization.getTenantId()).build();

		// Search Regularization Application in DB for update
		List<Regularization> searchResult = searchRegularization(searchCriteria,
				regularizationRequest.getRequestInfo());

		// Throw exception if no application found in DB
		if (CollectionUtils.isEmpty(searchResult) || searchResult.size() > 1) {
			throw new CustomException(RegularizationConstants.UPDATE_ERROR,
					"Failed to Update the Application, Found None or multiple applications !");
		}

		Map<String, Object> additionalDetails = (Map) regularization.getAdditionalDetails();
		// additionalDetails will always be a Map and will surely contain
		// applicationType then only this method invoked-
		additionalDetails.remove(RegularizationConstants.APPLICATION_TYPE);
		additionalDetails.put(RegularizationConstants.LAND_AND_BUILDING_PLAN_LAYOUT_IS_SIGNED, true);
		regularization.setAuditDetails(searchResult.get(0).getAuditDetails());
		Optional<Document> buildingLayoutPlanDocument = searchResult.get(0).getDocuments().stream()
				.filter(document -> document.getDocumentType().equals(RegularizationConstants.DOC_TYPE_BUILDING_PLAN))
				.findFirst();
		if (buildingLayoutPlanDocument.isPresent()) {
			Map<String, Object> unsignedLandAndBuildingPlanLayoutDetails = new HashMap<>();
			unsignedLandAndBuildingPlanLayoutDetails.put(RegularizationConstants.CODE,
					buildingLayoutPlanDocument.get().getDocumentType());
			unsignedLandAndBuildingPlanLayoutDetails.put(RegularizationConstants.FILESTOREID,
					buildingLayoutPlanDocument.get().getFileStoreId());
			additionalDetails.put(RegularizationConstants.UNSIGNED_LAND_AND_BUILDING_PLAN_LAYOUT_DETAILS,
					unsignedLandAndBuildingPlanLayoutDetails);
		}

		// setting IDs for newly added Regularization Documents
		if (!CollectionUtils.isEmpty(regularization.getDocuments())) {
			regularization.getDocuments().forEach(document -> {
				if (document.getId() == null) {
					document.setId(UUID.randomUUID().toString());
				}
			});
		}
		repository.update(regularizationRequest, true);
		return regularizationRequest.getRegularization();
	}

	/**
	 * Creates a new Field Inspection Report (Version 2) for a regularization
	 * application.
	 * 
	 * <p>
	 * This enhanced version includes additional validation for the 'active'
	 * parameter in additional details. The method orchestrates the creation
	 * process:
	 * <ol>
	 * <li>Validates the mandatory application number</li>
	 * <li>Validates the 'active' parameter in additional details</li>
	 * <li>Verifies that exactly one regularization application exists</li>
	 * <li>Enriches the report with required metadata (ID, audit details)</li>
	 * <li>Persists the field inspection report via Kafka</li>
	 * </ol>
	 * </p>
	 * 
	 * @param request The field inspection request containing report details
	 * @return The created field inspection report
	 * @throws CustomException if validation fails or application not found
	 */
	public RegularizationFieldInspection createFieldInspectionReportV2(
			@Valid RegularizationFieldInspectionRequest request) {

		RegularizationFieldInspection fieldInspection = request.getFieldInspection();

		// Step 1: Validate mandatory application number
		validateApplicationNumberForFIReport(fieldInspection, "create");

		// Step 2: Validate 'active' parameter in additional details
		validateActiveParameterInAdditionalDetails(fieldInspection, "create");

		// Step 3: Verify regularization application exists
		validateRegularizationExistsForFI(fieldInspection, request.getRequestInfo(), "create");

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
	 * @param operation       The operation type ("create" or "update") for error
	 *                        messaging
	 * @throws CustomException if application number is null or empty
	 */
	private void validateApplicationNumberForFIReport(RegularizationFieldInspection fieldInspection, String operation) {

		if (ObjectUtils.isEmpty(fieldInspection.getApplicationno())) {
			throw new CustomException(operation + " error",
					"Failed to " + operation + " feild inspection report, application no is mandatory");
		}
	}

	/**
	 * Validates that the 'active' parameter is present in additional details.
	 * 
	 * <p>
	 * The 'active' parameter is required to indicate whether the field inspection
	 * report is currently active or has been superseded.
	 * </p>
	 * 
	 * <p>
	 * <b>Note:</b> Previous validation logic for inactive reports has been removed
	 * as per client request.
	 * </p>
	 * 
	 * @param fieldInspection The field inspection object containing additional
	 *                        details
	 * @param operation       The operation type ("create" or "update") for error
	 *                        messaging
	 * @throws CustomException if 'active' parameter is missing
	 */
	@SuppressWarnings("unchecked")
	private void validateActiveParameterInAdditionalDetails(RegularizationFieldInspection fieldInspection,
			String operation) {

		// Get or create additional details map
		Map<String, String> additionalDetails = fieldInspection.getAdditionalDetails() != null
				? (Map<String, String>) fieldInspection.getAdditionalDetails()
				: new HashMap<>();

		// Validate 'active' parameter is present
		if (additionalDetails.get("active") == null) {
			throw new CustomException(operation + " error", "Failed to " + operation
					+ " feild inspection report, active param is required in Additional Details !");
		}

		// Note: Validation for inactive reports removed as per client request
		// Previously: if (!isActive) {
		// regularizationValidator.validatefieldInspectionRequestV2(fieldInspection); }
	}

	/**
	 * Validates that exactly one regularization application exists for the field
	 * inspection.
	 * 
	 * @param fieldInspection The field inspection containing application number and
	 *                        tenant ID
	 * @param requestInfo     The request info for search operation
	 * @param operation       The operation type ("create" or "update") for error
	 *                        messaging
	 * @throws CustomException if no application found or multiple applications
	 *                         found
	 */
	private void validateRegularizationExistsForFI(RegularizationFieldInspection fieldInspection,
			RequestInfo requestInfo, String operation) {

		// Build search criteria
		RegularizationSearchCriteria searchCriteria = RegularizationSearchCriteria.builder()
				.applicationNo(fieldInspection.getApplicationno()).tenantId(fieldInspection.getTenantId()).build();

		// Search for regularization application
		List<Regularization> regularizationList = repository.searchRegularization(searchCriteria, requestInfo);
		log.info("Found {} regularization(s) for application: {}", regularizationList.size(),
				fieldInspection.getApplicationno());

		// Validate exactly one application exists
		if (CollectionUtils.isEmpty(regularizationList) || regularizationList.size() > 1) {
			throw new CustomException(operation + " error", "Failed to " + operation
					+ " feild inspection report, Found None or multiple Regularization applications!");
		}
	}

	/**
	 * Updates an existing Field Inspection Report (Version 2) for a regularization
	 * application.
	 * 
	 * <p>
	 * This enhanced version includes additional validation for the 'active'
	 * parameter in additional details. The method orchestrates the update process:
	 * <ol>
	 * <li>Validates the mandatory application number</li>
	 * <li>Validates the 'active' parameter in additional details</li>
	 * <li>Verifies that exactly one regularization application exists</li>
	 * <li>Enriches the report with update metadata (audit details)</li>
	 * <li>Persists the updated field inspection report via Kafka</li>
	 * </ol>
	 * </p>
	 * 
	 * @param request The field inspection request containing updated report details
	 * @return The updated field inspection report
	 * @throws CustomException if validation fails or application not found
	 */
	public RegularizationFieldInspection updateFieldInspectionReportV2(
			@Valid RegularizationFieldInspectionRequest request) {

		RegularizationFieldInspection fieldInspection = request.getFieldInspection();

		// Step 1: Validate mandatory application number
		validateApplicationNumberForFIReport(fieldInspection, "update");

		// Step 2: Validate 'active' parameter in additional details
		validateActiveParameterInAdditionalDetails(fieldInspection, "update");

		// Step 3: Verify regularization application exists
		validateRegularizationExistsForFI(fieldInspection, request.getRequestInfo(), "update");

		// Step 4: Enrich report with update metadata (audit details)
		enrichmentService.enrichUpdateFieldInspectionReport(request);

		// Step 5: Persist updated field inspection report via Kafka
		repository.updateFieldInspectionReport(request);

		return request.getFieldInspection();
	}

	/**
	 * Service Layer for providing Village for input Application Numbers
	 * 
	 * @param criteria
	 * @param requestInfo
	 * @return
	 */
	public List<RegularizationVillage> searchVillage(@Valid VillageSearchCriteria criteria, RequestInfo requestInfo) {

		List<RegularizationVillage> regularizationVillages = new LinkedList<>();

		// validate the request here
		regularizationValidator.validateVillageSearchRequest(criteria);

		// get the mapped data from the regl repository
		regularizationVillages = repository.getRegularizationVillagesData(criteria);

		return regularizationVillages;
	}

	/**
	 * Search Regularization Applications Pending at Sanc Fee Status for more than
	 * 30 days
	 * 
	 * @param criteria
	 * @param requestInfo
	 * @return
	 */
	public List<FeePendingApplication> searchRegularizationFeePending(@Valid BPASearchCriteria criteria,
			RequestInfo requestInfo) {

		List<FeePendingApplication> feePendingApplications = new ArrayList<>();

		List<FeePendingApplication> response = new ArrayList<>();

		log.info("search criteria for fee status search Regularization: " + String.valueOf(criteria));

		regularizationValidator.validateSearchFeePendingRequest(criteria);

		feePendingApplications = repository.searchRegularizationFeePendingApplications(criteria, requestInfo);

		// check if application was approved 30 days back and still pending for payment
		feePendingApplications.forEach(application -> {
			if (application.getAction().equalsIgnoreCase("APPROVE")) {
				if (util.checkIfOlderThanThirtyDays(application.getApprovedDate())) {
					application.setDaysSinceApproved(util.calculateDaysSinceApproved(application.getApprovedDate()));
					response.add(application);
				}
			}
		});

		return response;
	}

	/**
	 * Handle update request if Pull Back at Pending Sanc Fee
	 * 
	 * @param bpaRequest
	 * @param applicationType
	 */
	private void handleIfPullBackRequest(RegularizationRequest request) {

		if (request.getRegularization().getWorkflow().getAction().equalsIgnoreCase(BPAConstants.ACTION_PULL_BACK)) {

			Map<String, Boolean> isDataUpdateNeeded = new HashMap<>();

			Boolean isPaymentReceived = false;
			Demand demandToBeDeleted = null;
			Installment installmentToBeDeleted = null;
			RegularizationDscDetails dscToBeDeleted = null;

			isPaymentReceived = regularizationValidator.validateIfPaymentReceived(request);

			demandToBeDeleted = regularizationValidator.validateDemandToBeDeleted(request, isDataUpdateNeeded);

			dscToBeDeleted = regularizationValidator.validateIfDscToBeDeleted(request, isDataUpdateNeeded);

			log.info("Demand To Be Deleted " + demandToBeDeleted);
			log.info("DSC To Be Deleted " + dscToBeDeleted);

			deleteRequiredData(demandToBeDeleted, isPaymentReceived, isDataUpdateNeeded, request);

		}
	}

	/**
	 * Delete demand data, dsc details data here for Pull Back Request
	 * 
	 * @param demandToBeUpdated
	 * @param isPaymentReceived
	 * @param isDataUpdateNeeded
	 */
	private void deleteRequiredData(Demand demandToBeDeleted, Boolean isPaymentReceived,
			Map<String, Boolean> isDataUpdateNeeded, RegularizationRequest request) {

		if (isDataUpdateNeeded.containsKey(IssueFixConstants.IS_DEMAND_DELETE_NEEDED)
				&& isDataUpdateNeeded.get(IssueFixConstants.IS_DEMAND_DELETE_NEEDED)) {
			issueFixRepository.deleteDemandDetail(demandToBeDeleted);
			issueFixRepository.deleteDemand(demandToBeDeleted);
			issueFixRepository.expireBill(demandToBeDeleted.getConsumerCode());
		}

		if (isDataUpdateNeeded.containsKey(IssueFixConstants.IS_DSC_DELETE_NEEDED)
				&& isDataUpdateNeeded.get(IssueFixConstants.IS_DSC_DELETE_NEEDED)) {
			issueFixRepository.deleteDscDetails(request.getRegularization());
		}
	}

	private void handleRefusalShowCauseNotice(RegularizationRequest regularizationRequest) {
		Regularization regularization = regularizationRequest.getRegularization();

		if (BPAConstants.ACTION_REJECT_SCN.equalsIgnoreCase(regularization.getWorkflow().getAction())) {
			String applicationNo = regularization.getApplicationNo();
			log.info("Regularization Refusal SCN action triggered for applicationno : " + applicationNo);
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

	private void validateRefusalScn(Regularization regularization) {
		if (BPAConstants.ACTION_REJECT.equalsIgnoreCase(regularization.getWorkflow().getAction())) {
			String applicationNo = regularization.getApplicationNo();
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
	 * Upload offline documents from here, after approval as well
	 * 
	 * @param docUploadRequest
	 * @param requestInfo
	 * @return
	 */
	public List<RegularizationDocumentList> uploadDocument(RegularizationDocUpload docUploadRequest,
			RequestInfo requestInfo) {

		log.info("Regularization Document Upload Request Received");

		regularizationValidator.validateDocUploadRequest(docUploadRequest);
		List<Regularization> applications = new LinkedList<>();

		RegularizationSearchCriteria searchCriteria = RegularizationSearchCriteria.builder()
				.applicationNo(docUploadRequest.getApplicationNo()).tenantId(docUploadRequest.getTenantId()).build();
		applications = searchRegularization(searchCriteria, requestInfo);
		if (CollectionUtils.isEmpty(applications)) {
			throw new CustomException(BPAErrorConstants.DOC_UPLOAD_ERROR,
					"Application Not found in the System for : " + docUploadRequest.getApplicationNo());
		}

		Regularization application = applications.get(0);
		String tenantId = docUploadRequest.getTenantId().split("\\.")[0];
		Object mdmsData = util.mDMSCall(requestInfo, tenantId);

		String docUploadtype = docUploadRequest.getDocUploadType();
		List<String> uuids = new ArrayList<String>();
		List<Role> roles = requestInfo.getUserInfo().getRoles();
		List<String> roleCodes = roles.stream().map(Role::getCode).collect(Collectors.toList());

		BusinessService businessService = workflowService.getBusinessService(application, requestInfo,
				docUploadRequest.getApplicationNo());
		String currentState = workflowService.getCurrentState(applications.get(0).getStatus(), businessService);

		validateDocumentUploadRequest(docUploadRequest, applications, mdmsData, currentState);

		boolean roleFound;
		switch (docUploadtype) {

		case BPAConstants.OTHER_DOCUMENTS:

			roleFound = roleCodes.stream().anyMatch(BPAConstants.APPROVER_ROLES_ALLOWED::contains);
			if (!roleFound) {
				throw new CustomException(BPAErrorConstants.INVALID_USER_TYPE,
						"Only " + BPAConstants.APPROVER_ROLES_ALLOWED
								+ " are allowed to upload Documents for Ths Document Type ");
			}

			if (!ObjectUtils.isEmpty(application.getDscDetails()) && !org.springframework.util.StringUtils
					.hasText(application.getDscDetails().get(0).getDocumentId())) {
				throw new CustomException(BPAErrorConstants.DOC_UPLOAD_ERROR,
						"Regularization Certificate Not Signed for Application : "
								+ docUploadRequest.getApplicationNo());
			}

			break;
		}

		HashMap<String, List<RegularizationDocumentList>> documentMapping = enrichDocumentDetails(docUploadRequest,
				application);

		List<RegularizationDocumentList> documents = documentMapping.values().stream().flatMap(List::stream)
				.collect(Collectors.toList());
		RegularizationDocUploadRequest updateRequest = getRegularizationDocUploadRequest(docUploadRequest,
				documentMapping.get("DOC_UPDATE"));
		RegularizationDocUploadRequest uploadRequest = getRegularizationDocUploadRequest(docUploadRequest,
				documentMapping.get("DOC_UPLOAD"));

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

	private RegularizationDocUploadRequest getRegularizationDocUploadRequest(RegularizationDocUpload docUploadRequest,
			List<RegularizationDocumentList> list) {
		RegularizationDocUpload documents = getDocUploadRequest(docUploadRequest, list);
		return RegularizationDocUploadRequest.builder().docUploadRequest(documents).build();
	}

	private RegularizationDocUpload getDocUploadRequest(RegularizationDocUpload docUploadRequest,
			List<RegularizationDocumentList> list) {
		String tenantId = docUploadRequest.getTenantId().split("\\.")[0];
		String applicationNo = docUploadRequest.getApplicationNo();
		return RegularizationDocUpload.builder().tenantId(tenantId).applicationNo(applicationNo).documents(list)
				.auditDetails(docUploadRequest.getAuditDetails()).build();
	}

	private HashMap<String, List<RegularizationDocumentList>> enrichDocumentDetails(
			RegularizationDocUpload docUploadRequest, Regularization application) {

		List<RegularizationDocumentList> docUpdateDocuments = docUploadRequest.getDocuments().stream()
				.filter(doc -> org.springframework.util.StringUtils.hasText(doc.getId())).collect(Collectors.toList());
		List<RegularizationDocumentList> docUploadDocuments = docUploadRequest.getDocuments().stream()
				.filter(doc -> org.springframework.util.StringUtils.isEmpty(doc.getId())).collect(Collectors.toList());

		HashMap<String, List<RegularizationDocumentList>> documentMapping = new HashMap<>();
		documentMapping.put("DOC_UPDATE", docUpdateDocuments);
		documentMapping.put("DOC_UPLOAD", docUploadDocuments);

		// adding building plan id
		String regularizationId = application.getId();
		docUploadDocuments.stream().forEach(document -> {
			document.setRegularizationId(regularizationId);
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

	private void validateDocumentUploadRequest(RegularizationDocUpload docUploadRequest,
			List<Regularization> applications, Object mdmsData, String currentState) {

		log.info("Validating Regularization Offline Documents");
		validateWorkflow(applications, currentState);
		regularizationValidator.validateDocUpload(docUploadRequest, applications, mdmsData, currentState);

	}

	private void validateWorkflow(List<Regularization> applications, String currentState) {
		log.info("Current State : " + currentState);
		if (!currentState.equals("APPROVED")) {
			throw new CustomException(BPAErrorConstants.DOC_UPLOAD_ERROR, "Application Not Approved ");
		}
	}

	public RegularizationDraft save(@Valid RegularizationDraftRequest request) {
		regularizationValidator.validateSaveDraft(request);
		enrichmentService.enrichRegularizationSaveDraft(request);
		repository.save(request);
		return request.getRegularizationDraft();
	}

	public List<RegularizationDraft> search(@Valid RegularizationDraftSearchCriteria criteria,
			RequestInfo requestInfo) {
		List<RegularizationDraft> regularizationDrafts = new LinkedList<>();
		regularizationValidator.validateDraftSearch(requestInfo, criteria);
		regularizationDrafts = repository.getRegularizationDraftData(criteria);
		return regularizationDrafts;
	}

	/**
	 * Cleans up associated draft records after successful regularization creation.
	 * 
	 * <p>
	 * When a regularization application is created from a saved draft, this method
	 * marks the draft as DELETED to prevent duplicate submissions. The draft is
	 * identified by the draft number stored in the regularization's additional
	 * details.
	 * </p>
	 * 
	 * <p>
	 * Processing flow:
	 * <ol>
	 * <li>Extracts draft number from additional details (if present)</li>
	 * <li>Searches for the draft record in the database</li>
	 * <li>Marks the draft as DELETED and persists the update</li>
	 * </ol>
	 * </p>
	 * 
	 * @param requestInfo    The request info for API calls
	 * @param regularization The created regularization that may have an associated
	 *                       draft
	 */
	@SuppressWarnings("unchecked")
	private void enrichRegularizationDraft(RequestInfo requestInfo, Regularization regularization) {

		// Step 1: Extract draft number from additional details
		String draftNo = extractDraftNumber(regularization);

		// Step 2: If draft number exists, fetch and mark draft as deleted
		if (draftNo != null && !StringUtils.isEmpty(draftNo)) {
			markDraftAsDeleted(requestInfo, draftNo);
		}
	}

	/**
	 * Extracts the draft number from regularization's additional details.
	 * 
	 * @param regularization The regularization entity
	 * @return The draft number if present, null otherwise
	 */
	@SuppressWarnings("unchecked")
	private String extractDraftNumber(Regularization regularization) {

		Map<String, Object> additionalDetails = (Map<String, Object>) regularization.getAdditionalDetails();

		if (!ObjectUtils.isEmpty(additionalDetails)) {
			return (String) additionalDetails.get(RegularizationConstants.DRAFT_NUMBER);
		}

		return null;
	}

	/**
	 * Marks a draft as deleted after successful regularization creation.
	 * 
	 * @param requestInfo The request info for API calls
	 * @param draftNo     The draft number to mark as deleted
	 */
	private void markDraftAsDeleted(RequestInfo requestInfo, String draftNo) {

		// Search for the draft by draft number
		RegularizationDraftSearchCriteria criteria = RegularizationDraftSearchCriteria.builder().draftNo(draftNo)
				.build();
		List<RegularizationDraft> regularizationDrafts = repository.getRegularizationDraftData(criteria);

		// If draft found, mark it as deleted
		if (!CollectionUtils.isEmpty(regularizationDrafts)) {
			RegularizationDraft draft = regularizationDrafts.get(0);
			draft.setStatus(RegularizationConstants.DELETED);

			// Build and persist the draft update request
			RegularizationDraftRequest draftRequest = RegularizationDraftRequest.builder().regularizationDraft(draft)
					.requestInfo(requestInfo).build();
			repository.save(draftRequest);
		}
	}
}
