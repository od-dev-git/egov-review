package org.egov.bpa.service;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.validation.Valid;

import org.apache.commons.lang3.StringUtils;
import org.egov.bpa.config.BPAConfiguration;
import org.egov.bpa.repository.ServiceRequestRepository;
import org.egov.bpa.util.BPAConstants;
import org.egov.bpa.util.DemolitionConstants;
import org.egov.bpa.util.RegularizationConstants;
import org.egov.bpa.web.model.demolition.Demolition;
import org.egov.bpa.web.model.demolition.DemolitionRequest;
import org.egov.bpa.web.model.demolition.DemolitionSearchCriteria;
import org.egov.bpa.web.model.landInfo.OwnerInfo;
import org.egov.bpa.web.model.user.CreateUserRequest;
import org.egov.bpa.web.model.user.UserDetailResponse;
import org.egov.bpa.web.model.user.UserSearchRequest;
import org.egov.common.contract.request.RequestInfo;
import org.egov.common.contract.request.Role;
import org.egov.tracer.model.CustomException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class DemolitionUserService {

	@Autowired
	private ServiceRequestRepository serviceRequestRepository;

	@Autowired
	private ObjectMapper mapper;

	@Autowired
	private BPAConfiguration config;

	/**
	 * Manages user lifecycle for all owners in a demolition application.
	 * 
	 * <p>
	 * This method orchestrates the complete user management process:
	 * <ul>
	 * <li>Validates that all owners have required mobile numbers</li>
	 * <li>Ensures owners have proper tenant ID set</li>
	 * <li>Checks if users already exist in the system</li>
	 * <li>Creates new users if they don't exist or if details have changed</li>
	 * <li>Updates owner objects with user system IDs (UUID, userId, username)</li>
	 * </ul>
	 * 
	 * <p>
	 * <strong>Business Logic:</strong>
	 * <ul>
	 * <li>Mobile number is mandatory for all owners</li>
	 * <li>If user exists with same mobile but different details, creates new
	 * user</li>
	 * <li>All owners are assigned CITIZEN role by default</li>
	 * <li>Tenant ID defaults to root tenant if not specified</li>
	 * </ul>
	 * 
	 * @param request the demolition request containing owner information
	 * @throws CustomException if any owner is missing mobile number
	 */
	public void manageUser(DemolitionRequest request) {

		Demolition demolition = request.getDemolition();
		RequestInfo requestInfo = request.getRequestInfo();

		// Process each owner in the demolition application
		demolition.getOwners().forEach(owner -> {
			processOwner(owner, demolition, requestInfo);
		});
	}

	/**
	 * Processes a single owner to ensure they have a valid user account in the
	 * system.
	 * 
	 * <p>
	 * This method handles the complete lifecycle for a single owner:
	 * <ul>
	 * <li>Validates mobile number is present</li>
	 * <li>Sets default tenant ID if missing</li>
	 * <li>Checks for existing user or creates new one</li>
	 * <li>Links owner with user system information</li>
	 * </ul>
	 * 
	 * @param owner       the owner information to process
	 * @param demolition  the parent demolition application (for tenant ID)
	 * @param requestInfo the request information for authentication
	 * @throws CustomException if mobile number is missing
	 */
	private void processOwner(OwnerInfo owner, Demolition demolition, RequestInfo requestInfo) {

		// Step 1: Validate mobile number is present
		validateOwnerMobileNumber(owner);

		// Step 2: Ensure owner has tenant ID set
		ensureOwnerTenantId(owner, demolition);

		// Step 3: Get or create user account
		UserDetailResponse userDetailResponse = getOrCreateUser(owner, requestInfo);

		// Step 4: Link owner with user system information
		if (userDetailResponse != null) {
			setOwnerFields(owner, userDetailResponse, requestInfo);
		}
	}

	/**
	 * Validates that the owner has a mobile number.
	 * 
	 * <p>
	 * <strong>Business Rule:</strong> Mobile number is mandatory for all owners as
	 * it serves as the username for citizen login and is the primary identifier for
	 * user accounts in the system.
	 * 
	 * @param owner the owner to validate
	 * @throws CustomException if mobile number is null or empty
	 */
	private void validateOwnerMobileNumber(OwnerInfo owner) {
		if (owner.getMobileNumber() == null) {
			log.error("Mobile number does not exist in ownerInfo");
			throw new CustomException(RegularizationConstants.INVAILD_OWNER, "MobileNo is mandatory for ownerInfo");
		}
	}

	/**
	 * Ensures the owner has a tenant ID set, defaulting to root tenant if not
	 * present.
	 * 
	 * <p>
	 * Tenant ID is extracted from the demolition's tenant ID by taking only the
	 * root level (before the first dot). For example, "pb.amritsar" becomes "pb".
	 * 
	 * @param owner      the owner to update
	 * @param demolition the demolition application containing tenant information
	 */
	private void ensureOwnerTenantId(OwnerInfo owner, Demolition demolition) {
		if (owner.getTenantId() == null) {
			// Extract root tenant ID (e.g., "pb" from "pb.amritsar")
			owner.setTenantId(demolition.getTenantId().split("\\.")[0]);
		}
	}

	/**
	 * Gets an existing user or creates a new one if needed.
	 * 
	 * <p>
	 * <strong>User Creation Logic:</strong>
	 * <ul>
	 * <li>If no user exists with the mobile number, creates new user</li>
	 * <li>If user exists but details don't match (name, DOB, etc.), creates new
	 * user</li>
	 * <li>If user exists and details match, returns existing user</li>
	 * </ul>
	 * 
	 * <p>
	 * <strong>Note:</strong> Creating a new user when details change ensures that
	 * owners can update their personal information without conflicts with existing
	 * user accounts.
	 * 
	 * @param owner       the owner information
	 * @param requestInfo the request information for user service calls
	 * @return UserDetailResponse containing user details, or null if creation fails
	 */
	private UserDetailResponse getOrCreateUser(OwnerInfo owner, RequestInfo requestInfo) {

		// Check if user already exists in the system
		UserDetailResponse userDetailResponse = userExists(owner, requestInfo);

		// Determine if user needs to be created
		boolean shouldCreateUser = shouldCreateNewUser(userDetailResponse, owner);

		if (shouldCreateUser) {
			userDetailResponse = createNewCitizenUser(owner, requestInfo);
		}

		return userDetailResponse;
	}

	/**
	 * Determines if a new user should be created based on existing user data.
	 * 
	 * <p>
	 * A new user should be created when:
	 * <ul>
	 * <li>No user exists with the given mobile number</li>
	 * <li>User service returned null or empty response</li>
	 * <li>Existing user's details don't match the owner's details</li>
	 * </ul>
	 * 
	 * @param userDetailResponse the existing user details from user service
	 * @param owner              the owner information to compare
	 * @return true if a new user should be created, false otherwise
	 */
	private boolean shouldCreateNewUser(UserDetailResponse userDetailResponse, OwnerInfo owner) {
		return userDetailResponse == null || CollectionUtils.isEmpty(userDetailResponse.getUser())
				|| !owner.compareWithExistingUser(userDetailResponse.getUser().get(0));
	}

	/**
	 * Creates a new citizen user in the user management system.
	 * 
	 * <p>
	 * This method performs the following operations:
	 * <ul>
	 * <li>Assigns CITIZEN role to the user</li>
	 * <li>Sets default fields (active status, tenant ID, user type)</li>
	 * <li>Generates random username (UUID-based)</li>
	 * <li>Sets owner type to CITIZEN</li>
	 * <li>Calls user service to create the user</li>
	 * </ul>
	 * 
	 * @param owner       the owner information to create user from
	 * @param requestInfo the request information for user service authentication
	 * @return UserDetailResponse containing the newly created user details
	 */
	private UserDetailResponse createNewCitizenUser(OwnerInfo owner, RequestInfo requestInfo) {

		// Prepare user with citizen role and default fields
		Role citizenRole = getCitizenRole();
		addUserDefaultFields(owner.getTenantId(), citizenRole, owner);

		// Generate unique username
		setUserName(owner);

		// Set owner type
		owner.setOwnerType(RegularizationConstants.CITIZEN);

		// Build user creation endpoint
		StringBuilder uri = new StringBuilder(config.getUserHost()).append(config.getUserContextPath())
				.append(config.getUserCreateEndpoint());

		// Call user service to create user
		UserDetailResponse userDetailResponse = userCall(new CreateUserRequest(requestInfo, owner), uri);

		log.debug("New citizen user created with UUID: {}", userDetailResponse.getUser().get(0).getUuid());

		return userDetailResponse;
	}

	private UserDetailResponse userExists(OwnerInfo owner, @Valid RequestInfo requestInfo) {

		UserSearchRequest userSearchRequest = new UserSearchRequest();
		userSearchRequest.setTenantId(owner.getTenantId().split("\\.")[0]);
		userSearchRequest.setMobileNumber(owner.getMobileNumber());
		if (!StringUtils.isEmpty(owner.getUuid())) {
			List<String> uuids = new ArrayList<String>();
			uuids.add(owner.getUuid());
			userSearchRequest.setUuid(uuids);
		}

		StringBuilder uri = new StringBuilder(config.getUserHost()).append(config.getUserSearchEndpoint());
		return userCall(userSearchRequest, uri);
	}

	UserDetailResponse userCall(Object userRequest, StringBuilder uri) {
		String dobFormat = null;
		if (uri.toString().contains(config.getUserSearchEndpoint())
				|| uri.toString().contains(config.getUserUpdateEndpoint()))
			dobFormat = "yyyy-MM-dd";
		else if (uri.toString().contains(config.getUserCreateEndpoint()))
			dobFormat = "dd/MM/yyyy";
		try {
			LinkedHashMap responseMap = (LinkedHashMap) serviceRequestRepository.fetchResult(uri, userRequest);
			parseResponse(responseMap, dobFormat);
			UserDetailResponse userDetailResponse = mapper.convertValue(responseMap, UserDetailResponse.class);
			return userDetailResponse;
		} catch (IllegalArgumentException e) {
			throw new CustomException("ILLEGAL_ARGUMENT", "ObjectMapper not able to convertValue in userCall");
		}
	}

	private void parseResponse(LinkedHashMap responeMap, String dobFormat) {
		List<LinkedHashMap> users = (List<LinkedHashMap>) responeMap.get("user");
		String format1 = "dd-MM-yyyy HH:mm:ss";
		if (users != null) {
			users.forEach(map -> {
				map.put("createdDate", dateTolong((String) map.get("createdDate"), format1));
				if ((String) map.get("lastModifiedDate") != null)
					map.put("lastModifiedDate", dateTolong((String) map.get("lastModifiedDate"), format1));
				if ((String) map.get("dob") != null)
					map.put("dob", dateTolong((String) map.get("dob"), dobFormat));
				if ((String) map.get("pwdExpiryDate") != null)
					map.put("pwdExpiryDate", dateTolong((String) map.get("pwdExpiryDate"), format1));
			});
		}
	}

	private Long dateTolong(String date, String format) {
		SimpleDateFormat f = new SimpleDateFormat(format);
		Date d = null;
		try {
			d = f.parse(date);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return d.getTime();
	}

	private Role getCitizenRole() {
		Role role = new Role();
		role.setCode(RegularizationConstants.CITIZEN);
		role.setName("Citizen");
		return role;
	}

	private void setOwnerFields(OwnerInfo owner, UserDetailResponse userDetailResponse, RequestInfo requestInfo) {
		owner.setId(userDetailResponse.getUser().get(0).getId());
		owner.setUuid(userDetailResponse.getUser().get(0).getUuid());
		owner.setUserName((userDetailResponse.getUser().get(0).getUserName()));
	}

	private void setUserName(OwnerInfo owner) {
		owner.setUserName(UUID.randomUUID().toString());
	}

	private void addUserDefaultFields(String tenantId, Role role, OwnerInfo owner) {
		owner.setActive(true);
		owner.setTenantId(tenantId);
		owner.setRoles(Collections.singletonList(role));
		owner.setType(RegularizationConstants.CITIZEN);
	}

	/**
	 * Retrieves UUIDs for all owners in a demolition application by searching users
	 * with mobile numbers.
	 * 
	 * <p>
	 * This method performs the following operations:
	 * <ul>
	 * <li>Extracts unique mobile numbers from all owners in the demolition</li>
	 * <li>Searches the user system for each unique mobile number (used as
	 * username)</li>
	 * <li>Collects and returns the UUIDs of found users</li>
	 * </ul>
	 * 
	 * <p>
	 * <strong>Business Logic:</strong>
	 * <ul>
	 * <li>Mobile numbers are used as usernames in the citizen user system</li>
	 * <li>Multiple owners may share the same mobile number (duplicate
	 * prevention)</li>
	 * <li>Only UUIDs of existing users are returned (new users are ignored)</li>
	 * <li>Search is performed at state level tenant for cross-ULB user lookup</li>
	 * </ul>
	 * 
	 * <p>
	 * <strong>Use Case:</strong> This method is typically used for notification
	 * purposes to identify all unique users who should receive updates about the
	 * demolition application.
	 * 
	 * @param demolition the demolition application containing owner information
	 * @return set of unique user UUIDs for all owners found in the system
	 */
	public Set<String> getUUidFromUserName(Demolition demolition) {

		// Step 1: Extract unique mobile numbers from all owners
		Set<String> uniqueMobileNumbers = extractUniqueMobileNumbers(demolition.getOwners());

		// Step 2: Retrieve UUIDs for each mobile number
		Set<String> userUuids = retrieveUserUuidsFromMobileNumbers(uniqueMobileNumbers);

		return userUuids;
	}

	/**
	 * Extracts unique mobile numbers from a list of owners.
	 * 
	 * <p>
	 * This method ensures that each mobile number is processed only once, even if
	 * multiple owners share the same mobile number. This prevents redundant user
	 * service calls and duplicate UUIDs in the result.
	 * 
	 * @param owners the list of owners to extract mobile numbers from
	 * @return set of unique mobile numbers (empty set if owners is null or empty)
	 */
	private Set<String> extractUniqueMobileNumbers(List<OwnerInfo> owners) {
		Set<String> mobileNumbers = new HashSet<>();

		if (CollectionUtils.isEmpty(owners)) {
			return mobileNumbers;
		}

		// Collect all unique mobile numbers
		owners.forEach(owner -> {
			if (owner.getMobileNumber() != null) {
				mobileNumbers.add(owner.getMobileNumber());
			}
		});

		return mobileNumbers;
	}

	/**
	 * Retrieves user UUIDs by searching the user system with mobile numbers.
	 * 
	 * <p>
	 * For each mobile number:
	 * <ul>
	 * <li>Searches the user system using mobile number as username</li>
	 * <li>Extracts UUID if user is found</li>
	 * <li>Skips if no user exists with that mobile number</li>
	 * </ul>
	 * 
	 * <p>
	 * <strong>Note:</strong> Search is performed at STATE_TENANTID level to find
	 * users across all ULBs (Urban Local Bodies) within the state.
	 * 
	 * @param mobileNumbers the set of unique mobile numbers to search for
	 * @return set of user UUIDs found in the system
	 */
	private Set<String> retrieveUserUuidsFromMobileNumbers(Set<String> mobileNumbers) {
		Set<String> uuids = new HashSet<>();

		// Search for each mobile number and collect UUIDs
		mobileNumbers.forEach(mobileNumber -> {
			UserDetailResponse userResponse = searchByUserName(mobileNumber, DemolitionConstants.STATE_TENANTID);

			// Add UUID if user found
			if (isUserFound(userResponse)) {
				String uuid = userResponse.getUser().get(0).getUuid();
				uuids.add(uuid);
			}
		});

		return uuids;
	}

	/**
	 * Checks if a user was found in the user service response.
	 * 
	 * @param userResponse the user service response to check
	 * @return true if response contains at least one user, false otherwise
	 */
	private boolean isUserFound(UserDetailResponse userResponse) {
		return userResponse != null && !CollectionUtils.isEmpty(userResponse.getUser());
	}

	private UserDetailResponse searchByUserName(String userName, String tenantId) {
		UserSearchRequest userSearchRequest = new UserSearchRequest();
		userSearchRequest.setUserType(RegularizationConstants.CITIZEN);
		userSearchRequest.setUserName(userName);
		userSearchRequest.setTenantId(tenantId);
		StringBuilder uri = new StringBuilder(config.getUserHost()).append(config.getUserSearchEndpoint());
		return userCall(userSearchRequest, uri);
	}

	public UserDetailResponse getUser(@Valid DemolitionSearchCriteria criteria, RequestInfo requestInfo) {

		UserSearchRequest userSearchRequest = getUserSearchRequest(criteria, requestInfo);
		StringBuilder uri = new StringBuilder(config.getUserHost()).append(config.getUserSearchEndpoint());
		UserDetailResponse userDetailResponse = userCall(userSearchRequest, uri);
		return userDetailResponse;
	}

	private UserSearchRequest getUserSearchRequest(@Valid DemolitionSearchCriteria criteria, RequestInfo requestInfo) {

		UserSearchRequest userSearchRequest = new UserSearchRequest();
		userSearchRequest.setRequestInfo(requestInfo);
		userSearchRequest.setTenantId(criteria.getTenantId().split("\\.")[0]);
		userSearchRequest.setMobileNumber(criteria.getMobileNumber());
		userSearchRequest.setActive(true);
		userSearchRequest.setUserType(BPAConstants.CITIZEN);
		if (!CollectionUtils.isEmpty(criteria.getOwnerIds()))
			userSearchRequest.setUuid(criteria.getOwnerIds());
		return userSearchRequest;
	}

	public UserDetailResponse getUsersInfo(List<String> ownersUuids, RequestInfo requestInfo) {

		if (!CollectionUtils.isEmpty(ownersUuids)) {
			UserSearchRequest userSearchRequest = UserSearchRequest.builder().requestInfo(requestInfo).uuid(ownersUuids)
					.build();
			StringBuilder url = new StringBuilder(config.getUserHost()).append(config.getUserSearchEndpoint());
			return userCall(userSearchRequest, url);
		}
		return null;
	}
}
