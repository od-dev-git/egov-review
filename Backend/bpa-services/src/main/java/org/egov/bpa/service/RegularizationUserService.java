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
import org.egov.bpa.util.RegularizationConstants;
import org.egov.bpa.web.model.landInfo.OwnerInfo;
import org.egov.bpa.web.model.regularization.Regularization;
import org.egov.bpa.web.model.regularization.RegularizationSearchCriteria;
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
public class RegularizationUserService {

	@Autowired
	private ServiceRequestRepository serviceRequestRepository;

	@Autowired
	private ObjectMapper mapper;

	@Autowired
	private BPAConfiguration config;

	/**
	 * Manages user creation/update for all owners in a regularization application.
	 * 
	 * <p>
	 * This method orchestrates user management for each owner by:
	 * <ol>
	 * <li>Validating that mobile number exists for each owner</li>
	 * <li>Checking if the user already exists in the system</li>
	 * <li>Creating a new user if not found or if details have changed</li>
	 * <li>Setting owner fields from the user response</li>
	 * </ol>
	 * </p>
	 * 
	 * @param requestInfo    The request info containing user context
	 * @param regularization The regularization containing owners to process
	 * @throws CustomException if mobile number is missing for any owner
	 */
	public void manageUser(RequestInfo requestInfo, Regularization regularization) {

		regularization.getOwners().forEach(owner -> {
			processOwner(requestInfo, regularization, owner);
		});
	}

	/**
	 * Processes a single owner - validates, checks existence, creates if needed,
	 * and sets fields.
	 * 
	 * @param requestInfo    The request info
	 * @param regularization The regularization entity
	 * @param owner          The owner to process
	 * @throws CustomException if mobile number is missing
	 */
	private void processOwner(RequestInfo requestInfo, Regularization regularization, OwnerInfo owner) {

		// Validate mobile number is present
		validateOwnerMobileNumber(owner);

		// Set tenant ID if not present
		setOwnerTenantIdIfMissing(owner, regularization);

		// Check if user exists and get response
		UserDetailResponse userDetailResponse = userExists(owner, requestInfo);

		// Create new user if needed (not found or details changed)
		if (isUserCreationRequired(owner, userDetailResponse)) {
			userDetailResponse = createNewUser(requestInfo, owner);
		}

		// Set owner fields from user response
		if (userDetailResponse != null) {
			setOwnerFields(owner, userDetailResponse, requestInfo);
		}
	}

	/**
	 * Validates that the owner has a mobile number.
	 * 
	 * @param owner The owner to validate
	 * @throws CustomException if mobile number is null
	 */
	private void validateOwnerMobileNumber(OwnerInfo owner) {

		if (owner.getMobileNumber() == null) {
			log.debug("MobileNo does not exist in ownerInfo.");
			throw new CustomException(RegularizationConstants.INVAILD_OWNER, "MobileNo is mandatory for ownerInfo");
		}
	}

	/**
	 * Sets the tenant ID on owner if not already present.
	 * 
	 * <p>
	 * Extracts the root tenant ID from the regularization's tenant ID.
	 * </p>
	 * 
	 * @param owner          The owner to update
	 * @param regularization The regularization containing tenant ID
	 */
	private void setOwnerTenantIdIfMissing(OwnerInfo owner, Regularization regularization) {

		if (owner.getTenantId() == null) {
			// Extract root tenant ID (before first dot)
			owner.setTenantId(regularization.getTenantId().split("\\.")[0]);
		}
	}

	/**
	 * Determines if a new user needs to be created.
	 * 
	 * <p>
	 * User creation is required when:
	 * <ul>
	 * <li>No user found with the mobile number</li>
	 * <li>User found but details have changed</li>
	 * </ul>
	 * </p>
	 * 
	 * @param owner              The owner to check
	 * @param userDetailResponse The user search response
	 * @return true if user creation is required
	 */
	private boolean isUserCreationRequired(OwnerInfo owner, UserDetailResponse userDetailResponse) {

		// No user found
		if (userDetailResponse == null || CollectionUtils.isEmpty(userDetailResponse.getUser())) {
			return true;
		}

		// User found but details changed
		return !owner.compareWithExistingUser(userDetailResponse.getUser().get(0));
	}

	/**
	 * Creates a new user in the system.
	 * 
	 * <p>
	 * Sets up default fields, generates username, and calls user create endpoint.
	 * </p>
	 * 
	 * @param requestInfo The request info
	 * @param owner       The owner to create as a user
	 * @return The user detail response from creation
	 */
	private UserDetailResponse createNewUser(RequestInfo requestInfo, OwnerInfo owner) {

		// Set up role and default fields
		Role role = getCitizenRole();
		addUserDefaultFields(owner.getTenantId(), role, owner);

		// Generate unique username
		setUserName(owner);

		// Set owner type
		owner.setOwnerType(RegularizationConstants.CITIZEN);

		// Build create endpoint URI
		StringBuilder uri = new StringBuilder(config.getUserHost()).append(config.getUserContextPath())
				.append(config.getUserCreateEndpoint());

		// Call user service to create user
		UserDetailResponse userDetailResponse = userCall(new CreateUserRequest(requestInfo, owner), uri);

		log.debug("Owner created with UUID: {}", userDetailResponse.getUser().get(0).getUuid());

		return userDetailResponse;
	}

	/**
	 * set required details for owner
	 * 
	 * @param owner
	 * @param userDetailResponse
	 * @param requestInfo
	 */
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

	private Role getCitizenRole() {
		Role role = new Role();
		role.setCode(RegularizationConstants.CITIZEN);
		role.setName("Citizen");
		return role;
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

	/**
	 * @param ownersUuid
	 * @param requestInfo
	 * @return UserDetailResponse
	 */
	public UserDetailResponse getUsersInfo(List<String> ownersUuids, @Valid RequestInfo requestInfo) {

		if (!CollectionUtils.isEmpty(ownersUuids)) {
			UserSearchRequest userSearchRequest = UserSearchRequest.builder().requestInfo(requestInfo).uuid(ownersUuids)
					.build();
			StringBuilder url = new StringBuilder(config.getUserHost()).append(config.getUserSearchEndpoint());
			return userCall(userSearchRequest, url);
		}
		return null;
	}

	/**
	 * Retrieves user UUIDs for all owners in a regularization by searching with
	 * their mobile numbers.
	 * 
	 * <p>
	 * This method is used to find registered users associated with owners by:
	 * <ol>
	 * <li>Extracting all unique mobile numbers from the regularization owners</li>
	 * <li>Searching for users with each mobile number as username</li>
	 * <li>Collecting the UUIDs of all found users</li>
	 * </ol>
	 * </p>
	 * 
	 * <p>
	 * This is typically used to identify architects, technical persons, or other
	 * registered users who may be associated with the application owners.
	 * </p>
	 * 
	 * @param regularization The regularization containing owners
	 * @return Set of user UUIDs found for the owner mobile numbers
	 */
	public Set<String> getUUidFromUserName(Regularization regularization) {

		// Step 1: Extract all unique mobile numbers from owners
		Set<String> mobileNumbers = extractUniqueMobileNumbers(regularization.getOwners());

		// Step 2: Search for users by mobile number and collect UUIDs
		return fetchUuidsForMobileNumbers(mobileNumbers);
	}

	/**
	 * Extracts unique mobile numbers from a list of owners.
	 * 
	 * @param ownerInfos The list of owners
	 * @return Set of unique mobile numbers
	 */
	private Set<String> extractUniqueMobileNumbers(List<OwnerInfo> ownerInfos) {

		Set<String> mobileNumbers = new HashSet<>();

		ownerInfos.forEach(owner -> {
			if (owner.getMobileNumber() != null) {
				mobileNumbers.add(owner.getMobileNumber());
			}
		});

		return mobileNumbers;
	}

	/**
	 * Fetches user UUIDs for each mobile number by searching with mobile number as
	 * username.
	 * 
	 * @param mobileNumbers The set of mobile numbers to search
	 * @return Set of UUIDs for found users
	 */
	private Set<String> fetchUuidsForMobileNumbers(Set<String> mobileNumbers) {

		Set<String> uuids = new HashSet<>();

		mobileNumbers.forEach(mobileNumber -> {
			// Search for user with mobile number as username
			UserDetailResponse userDetailResponse = searchByUserName(mobileNumber,
					RegularizationConstants.STATE_TENANTID);

			// Add UUID if user found
			if (!CollectionUtils.isEmpty(userDetailResponse.getUser())) {
				uuids.add(userDetailResponse.getUser().get(0).getUuid());
			}
		});

		return uuids;
	}

	private UserDetailResponse searchByUserName(String userName, String tenantId) {
		UserSearchRequest userSearchRequest = new UserSearchRequest();
		userSearchRequest.setUserType(RegularizationConstants.CITIZEN);
		userSearchRequest.setUserName(userName);
		userSearchRequest.setTenantId(tenantId);
		StringBuilder uri = new StringBuilder(config.getUserHost()).append(config.getUserSearchEndpoint());
		return userCall(userSearchRequest, uri);

	}

	public UserDetailResponse getUser(RegularizationSearchCriteria searchCriteria, RequestInfo requestInfo) {
		UserSearchRequest userSearchRequest = getUserSearchRequest(searchCriteria, requestInfo);
		StringBuilder uri = new StringBuilder(config.getUserHost()).append(config.getUserSearchEndpoint());
		UserDetailResponse userDetailResponse = userCall(userSearchRequest, uri);
		return userDetailResponse;
	}

	private UserSearchRequest getUserSearchRequest(RegularizationSearchCriteria searchCriteria,
			RequestInfo requestInfo) {
		UserSearchRequest userSearchRequest = new UserSearchRequest();
		userSearchRequest.setRequestInfo(requestInfo);
		userSearchRequest.setTenantId(searchCriteria.getTenantId().split("\\.")[0]);
		userSearchRequest.setMobileNumber(searchCriteria.getMobileNumber());
		userSearchRequest.setActive(true);
		userSearchRequest.setUserType(BPAConstants.CITIZEN);
		if (!CollectionUtils.isEmpty(searchCriteria.getOwnerIds()))
			userSearchRequest.setUuid(searchCriteria.getOwnerIds());
		return userSearchRequest;
	}

}
