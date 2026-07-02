package org.egov.bpa.service;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

import org.egov.bpa.config.BPAConfiguration;
import org.egov.bpa.repository.ServiceRequestRepository;
import org.egov.bpa.util.BPAConstants;
import org.egov.bpa.web.model.BPA;
import org.egov.bpa.web.model.BPASearchCriteria;
import org.egov.bpa.web.model.landInfo.OwnerInfo;
import org.egov.bpa.web.model.user.UserDetailResponse;
import org.egov.bpa.web.model.user.UserSearchRequest;
import org.egov.common.contract.request.RequestInfo;
import org.egov.tracer.model.CustomException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class UserService {

	@Autowired
	private BPAConfiguration config;

	@Autowired
	private ServiceRequestRepository serviceRequestRepository;

	@Autowired
	private ObjectMapper mapper;

	/**
	 * Retrieves user details for all owners in a list of BPA applications.
	 * 
	 * <p>This method performs the following operations:
	 * <ol>
	 *   <li>Extracts all unique owner UUIDs from the BPA applications</li>
	 *   <li>Builds a user search request with the extracted UUIDs</li>
	 *   <li>Calls the user service to fetch user details</li>
	 * </ol>
	 * </p>
	 * 
	 * @param bpas The list of BPA applications to get owner details for
	 * @return UserDetailResponse containing user details for all owners
	 */
	public UserDetailResponse getUsersForBpas(List<BPA> bpas) {
		
		// Step 1: Extract owner UUIDs from all BPA applications
		List<String> ownerUuids = extractOwnerUuidsFromBpas(bpas);
		
		// Step 2: Build user search request
		UserSearchRequest userSearchRequest = buildUserSearchRequestByUuids(ownerUuids);
		
		// Step 3: Call user service and return response
		StringBuilder uri = new StringBuilder(config.getUserHost())
				.append(config.getUserSearchEndpoint());
		
		return userCall(userSearchRequest, uri);
	}
	
	/**
	 * Extracts all owner UUIDs from a list of BPA applications.
	 * 
	 * <p>Iterates through all BPA applications and their land info owners
	 * to collect UUIDs.</p>
	 * 
	 * @param bpas The list of BPA applications
	 * @return List of owner UUIDs
	 */
	private List<String> extractOwnerUuidsFromBpas(List<BPA> bpas) {
		
		List<String> uuids = new ArrayList<>();
		
		bpas.forEach(bpa -> {
			// Only process if land info exists
			if (bpa.getLandInfo() != null) {
				bpa.getLandInfo().getOwners().forEach(owner -> {
					// Add UUID if present
					if (owner.getUuid() != null) {
						uuids.add(owner.getUuid().toString());
					}
				});
			}
		});
		
		return uuids;
	}
	
	/**
	 * Builds a UserSearchRequest for searching users by their UUIDs.
	 * 
	 * @param uuids The list of UUIDs to search for
	 * @return UserSearchRequest configured with the UUIDs
	 */
	private UserSearchRequest buildUserSearchRequestByUuids(List<String> uuids) {
		
		UserSearchRequest userSearchRequest = new UserSearchRequest();
		userSearchRequest.setId(uuids);
		userSearchRequest.setUuid(uuids);
		
		return userSearchRequest;
	}
	/**
	 * Returns UserDetailResponse by calling user service with given uri and object
	 * 
	 * @param userRequest
	 *            Request object for user service
	 * @param uri
	 *            The address of the end point
	 * @return Response from user service as parsed as userDetailResponse
	 */
	@SuppressWarnings("rawtypes")
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
			throw new CustomException("IllegalArgumentException", "ObjectMapper not able to convertValue in userCall");
		}
	}

	/**
	 * Parses date formats to long for all users in responseMap
	 * 
	 * @param responeMap
	 *            LinkedHashMap got from user api response
	 */
	@SuppressWarnings({"unchecked", "rawtypes"})
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

	/**
	 * Converts date to long
	 * 
	 * @param date
	 *            date to be parsed
	 * @param format
	 *            Format of the date
	 * @return Long value of date
	 */
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
	 * Call search in user service based on ownerids from criteria
	 * 
	 * @param criteria
	 *            The search criteria containing the ownerids
	 * @param requestInfo
	 *            The requestInfo of the request
	 * @return Search response from user service based on ownerIds
	 */
	public UserDetailResponse getUser(BPASearchCriteria criteria, RequestInfo requestInfo) {
		UserSearchRequest userSearchRequest = getUserSearchRequest(criteria, requestInfo);
		StringBuilder uri = new StringBuilder(config.getUserHost()).append(config.getUserSearchEndpoint());
		UserDetailResponse userDetailResponse = userCall(userSearchRequest, uri);
		return userDetailResponse;
	}

	/**
	 * Creates userSearchRequest from bpaSearchCriteria
	 * 
	 * @param criteria
	 *            The bpaSearch criteria
	 * @param requestInfo
	 *            The requestInfo of the request
	 * @return The UserSearchRequest based on ownerIds
	 */
	private UserSearchRequest getUserSearchRequest(BPASearchCriteria criteria, RequestInfo requestInfo) {
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
	
	private UserDetailResponse searchByUserName(String userName,String tenantId){
        UserSearchRequest userSearchRequest = new UserSearchRequest();
        userSearchRequest.setUserType("CITIZEN");
        userSearchRequest.setUserName(userName);
        userSearchRequest.setTenantId(tenantId);
        StringBuilder uri = new StringBuilder(config.getUserHost()).append(config.getUserSearchEndpoint());
        return userCall(userSearchRequest,uri);

    }
	
	private String getStateLevelTenant(String tenantId){
        return tenantId.split("\\.")[0];
    }

	/**
	 * Retrieves user UUIDs for all owners in a BPA application by searching with their mobile numbers.
	 * 
	 * <p>This method is used to find registered users associated with owners by:
	 * <ol>
	 *   <li>Extracting all unique mobile numbers from the BPA owners</li>
	 *   <li>Searching for users with each mobile number as username</li>
	 *   <li>Collecting the UUIDs of all found users</li>
	 * </ol>
	 * </p>
	 * 
	 * <p>This is typically used to identify architects, technical persons, or other
	 * registered users who may be associated with the application owners.</p>
	 * 
	 * @param bpa The BPA application containing owners
	 * @return Set of user UUIDs found for the owner mobile numbers
	 */
    public Set<String> getUUidFromUserName(BPA bpa) {

        // Step 1: Extract all unique mobile numbers from owners
        Set<String> mobileNumbers = extractUniqueMobileNumbersFromOwners(bpa.getLandInfo().getOwners());

        // Step 2: Search for users by mobile number and collect UUIDs
        String stateTenantId = getStateLevelTenant(bpa.getTenantId());
        return fetchUuidsForMobileNumbers(mobileNumbers, stateTenantId);
    }
    
    /**
     * Extracts unique mobile numbers from a list of owners.
     * 
     * @param owners The list of owners
     * @return Set of unique mobile numbers
     */
    private Set<String> extractUniqueMobileNumbersFromOwners(List<OwnerInfo> owners) {
        
        Set<String> mobileNumbers = new HashSet<>();
        
        owners.forEach(owner -> {
            if (owner.getMobileNumber() != null) {
                mobileNumbers.add(owner.getMobileNumber());
            }
        });
        
        return mobileNumbers;
    }
    
    /**
     * Fetches user UUIDs for each mobile number by searching with mobile number as username.
     * 
     * @param mobileNumbers The set of mobile numbers to search
     * @param tenantId The tenant ID for user search
     * @return Set of UUIDs for found users
     */
    private Set<String> fetchUuidsForMobileNumbers(Set<String> mobileNumbers, String tenantId) {
        
        Set<String> uuids = new HashSet<>();
        
        mobileNumbers.forEach(mobileNumber -> {
            // Search for user with mobile number as username
            UserDetailResponse userDetailResponse = searchByUserName(mobileNumber, tenantId);
            
            // Add UUID if user found
            if (!CollectionUtils.isEmpty(userDetailResponse.getUser())) {
                uuids.add(userDetailResponse.getUser().get(0).getUuid());
            }
        });
        
        return uuids;
    }
    
    
    public UserDetailResponse getUserByUUID(String uuid, RequestInfo requestInfo) {
		UserSearchRequest userSearchRequest = new UserSearchRequest();
		userSearchRequest.setRequestInfo(requestInfo);
		//userSearchRequest.setTenantId("od");
		// userSearchRequest.setMobileNumber(criteria.getMobileNumber());
		userSearchRequest.setActive(true);
		//userSearchRequest.setUserType(BPAConstants.EMPLOYEE);
		userSearchRequest.setUuid(Arrays.asList(new String[] { uuid }));
		StringBuilder uri = new StringBuilder(config.getUserHost()).append(config.getUserSearchEndpoint());
		UserDetailResponse userDetailResponse = userCall(userSearchRequest, uri);
		return userDetailResponse;
	}
    
	/**
	 * Call search in user service based on ownerids from criteria for offline doc upload
	 * 
	 * @param criteria
	 *            The search criteria containing the ownerids
	 * @param requestInfo
	 *            The requestInfo of the request
	 * @return Search response from user service based on ownerIds
	 */
	public UserDetailResponse getUserForDocUpload(BPASearchCriteria criteria, RequestInfo requestInfo) {
		UserSearchRequest userSearchRequest = getUserSearchRequestForDocUpload(criteria, requestInfo);
		StringBuilder uri = new StringBuilder(config.getUserHost()).append(config.getUserSearchEndpoint());
		UserDetailResponse userDetailResponse = userCall(userSearchRequest, uri);
		return userDetailResponse;
	}
	
	private UserSearchRequest getUserSearchRequestForDocUpload(BPASearchCriteria criteria, RequestInfo requestInfo) {
		UserSearchRequest userSearchRequest = new UserSearchRequest();
		userSearchRequest.setRequestInfo(requestInfo);
		userSearchRequest.setTenantId(criteria.getTenantId().split("\\.")[0]);
		userSearchRequest.setMobileNumber(criteria.getMobileNumber());
		userSearchRequest.setActive(true);
		if (!CollectionUtils.isEmpty(criteria.getOwnerIds()))
			userSearchRequest.setUuid(criteria.getOwnerIds());
		return userSearchRequest;
	}
	
}
