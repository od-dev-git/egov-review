package org.egov.bpa.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.egov.bpa.config.BPAConfiguration;
import org.egov.bpa.repository.ServiceRequestRepository;
import org.egov.bpa.util.BPAConstants;
import org.egov.bpa.util.BPAErrorConstants;
import org.egov.bpa.web.model.BPA;
import org.egov.bpa.web.model.BPARequest;
import org.egov.bpa.web.model.RequestInfoWrapper;
import org.egov.bpa.web.model.landInfo.LandInfo;
import org.egov.bpa.web.model.landInfo.LandInfoRequest;
import org.egov.bpa.web.model.landInfo.LandSearchCriteria;
import org.egov.bpa.web.model.landInfo.NewOwnerInfo;
import org.egov.common.contract.request.RequestInfo;
import org.egov.tracer.model.CustomException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class BPALandService {

	@Autowired
	private BPAConfiguration config;

	@Autowired
	private ServiceRequestRepository serviceRequestRepository;

	@Autowired
	private ObjectMapper mapper;

	/**
	 * Creates land information in the land service and links it to the BPA
	 * application.
	 * 
	 * <p>
	 * This method orchestrates the complete land info creation lifecycle:
	 * <ul>
	 * <li>Enriches owner information with required metadata</li>
	 * <li>Calls land-services API to create land record</li>
	 * <li>Extracts the created land info from response</li>
	 * <li>Updates BPA with the land info and land ID</li>
	 * </ul>
	 * 
	 * <p>
	 * <strong>Business Logic:</strong>
	 * <ul>
	 * <li>Land info must be created before BPA can be persisted</li>
	 * <li>Land ID serves as the foreign key linking BPA to land records</li>
	 * <li>Owner details are enriched to ensure primary owner flags are set</li>
	 * </ul>
	 * 
	 * <p>
	 * <strong>Integration:</strong> This method integrates with the land-services
	 * module which maintains the master repository of land parcels and ownership
	 * details.
	 * 
	 * @param bpaRequest the BPA request containing land information to be created
	 * @throws CustomException if land service call fails or returns invalid data
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void addLandInfoToBPA(BPARequest bpaRequest) {

		// Step 1: Enrich owner information with primary owner flags
		enrichOwnerInfoDetails(bpaRequest);

		// Step 2: Prepare and execute land service create request
		LandInfo createdLandInfo = createLandInfoViaService(bpaRequest);

		// Step 3: Update BPA with created land information
		updateBpaWithLandInfo(bpaRequest.getBPA(), createdLandInfo);
	}

	/**
	 * Creates land information by calling the land-services create API.
	 * 
	 * <p>
	 * This method:
	 * <ul>
	 * <li>Builds the land-services create endpoint URL</li>
	 * <li>Prepares the land info request payload</li>
	 * <li>Calls the external land-services API</li>
	 * <li>Parses and returns the created land info</li>
	 * </ul>
	 * 
	 * @param bpaRequest the BPA request containing land information
	 * @return the created land information returned by land-services
	 * @throws CustomException if the land service call fails
	 */
	private LandInfo createLandInfoViaService(BPARequest bpaRequest) {

		// Build land service create URL
		StringBuilder uri = new StringBuilder(config.getLandInfoHost());
		uri.append(config.getLandInfoCreate());

		// Prepare land info request
		LandInfoRequest landRequest = buildLandInfoRequest(bpaRequest.getRequestInfo(),
				bpaRequest.getBPA().getLandInfo());

		// Call land service
		LinkedHashMap responseMap = callLandService(uri, landRequest, "create");

		// Extract and return created land info
		return extractLandInfoFromResponse(responseMap);
	}

	/**
	 * Builds a land info request wrapper for land-services API calls.
	 * 
	 * @param requestInfo the request information for authentication
	 * @param landInfo    the land information to be included in request
	 * @return the land info request wrapper
	 */
	private LandInfoRequest buildLandInfoRequest(RequestInfo requestInfo, LandInfo landInfo) {
		LandInfoRequest landRequest = new LandInfoRequest();
		landRequest.setRequestInfo(requestInfo);
		landRequest.setLandInfo(landInfo);
		return landRequest;
	}

	/**
	 * Calls the land-services API and handles errors.
	 * 
	 * <p>
	 * This method wraps the service call with error handling to provide clear error
	 * messages when the land service integration fails.
	 * 
	 * @param uri         the land service endpoint URL
	 * @param landRequest the land info request payload
	 * @param operation   the operation being performed (for error messages)
	 * @return the response map from land service
	 * @throws CustomException if the service call fails
	 */
	private LinkedHashMap callLandService(StringBuilder uri, LandInfoRequest landRequest, String operation) {
		try {
			return (LinkedHashMap) serviceRequestRepository.fetchResult(uri, landRequest);
		} catch (Exception e) {
			log.error("Land service {} call failed: {}", operation, e.getMessage(), e);
			throw new CustomException(BPAErrorConstants.LANDINFO_EXCEPTION,
					"LandInfo service " + operation + " call failed: " + e.getMessage());
		}
	}

	/**
	 * Extracts land information from the land-services response.
	 * 
	 * <p>
	 * The land service response contains an array of land info objects. This method
	 * extracts the first land info from the response and converts it to the domain
	 * object.
	 * 
	 * @param responseMap the raw response from land service
	 * @return the extracted and converted land info object
	 * @throws CustomException if response is invalid or empty
	 */
	private LandInfo extractLandInfoFromResponse(LinkedHashMap responseMap) {
		if (responseMap == null || !responseMap.containsKey("LandInfo")) {
			throw new CustomException(BPAErrorConstants.LANDINFO_EXCEPTION,
					"Invalid response from land service: missing LandInfo");
		}

		ArrayList<LandInfo> landInfoList = (ArrayList<LandInfo>) responseMap.get("LandInfo");

		if (CollectionUtils.isEmpty(landInfoList)) {
			throw new CustomException(BPAErrorConstants.LANDINFO_EXCEPTION,
					"Land service returned empty LandInfo list");
		}

		return mapper.convertValue(landInfoList.get(0), LandInfo.class);
	}

	/**
	 * Updates the BPA application with created land information.
	 * 
	 * <p>
	 * This method sets both the land info object and the land ID on the BPA,
	 * establishing the link between the BPA application and the land record.
	 * 
	 * @param bpa      the BPA application to update
	 * @param landInfo the created land information
	 */
	private void updateBpaWithLandInfo(BPA bpa, LandInfo landInfo) {
		bpa.setLandInfo(landInfo);
		bpa.setLandId(landInfo.getId());
	}

	/**
	 * Updates land information in the land service for an existing BPA application.
	 * 
	 * <p>
	 * This method orchestrates the land info update lifecycle:
	 * <ul>
	 * <li>Enriches owner information with required metadata</li>
	 * <li>Calls land-services update API to modify land record</li>
	 * <li>Extracts the updated land info from response</li>
	 * <li>Updates BPA with the latest land info</li>
	 * </ul>
	 * 
	 * <p>
	 * <strong>Business Logic:</strong> Updates to land ownership, boundaries, or
	 * other land attributes during BPA amendments must be synchronized with the
	 * land-services module to maintain data consistency.
	 * 
	 * @param bpaRequest the BPA request containing updated land information
	 * @throws CustomException if land service call fails or returns invalid data
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void updateLandInfo(BPARequest bpaRequest) {

		// Step 1: Enrich owner information with primary owner flags
		enrichOwnerInfoDetails(bpaRequest);

		// Step 2: Prepare and execute land service update request
		LandInfo updatedLandInfo = updateLandInfoViaService(bpaRequest);

		// Step 3: Update BPA with latest land information
		updateBpaWithLandInfo(bpaRequest.getBPA(), updatedLandInfo);
	}

	/**
	 * Updates land information by calling the land-services update API.
	 * 
	 * @param bpaRequest the BPA request containing land information to update
	 * @return the updated land information returned by land-services
	 * @throws CustomException if the land service call fails
	 */
	private LandInfo updateLandInfoViaService(BPARequest bpaRequest) {

		// Build land service update URL
		StringBuilder uri = new StringBuilder(config.getLandInfoHost());
		uri.append(config.getLandInfoUpdate());

		// Prepare land info request
		LandInfoRequest landRequest = buildLandInfoRequest(bpaRequest.getRequestInfo(),
				bpaRequest.getBPA().getLandInfo());

		// Call land service
		LinkedHashMap responseMap = callLandService(uri, landRequest, "update");

		// Extract and return updated land info
		return extractLandInfoFromResponse(responseMap);
	}

	/**
	 * Searches for land information records based on provided criteria.
	 * 
	 * <p>
	 * This method queries the land-services module to find land parcels matching
	 * the specified search criteria. It supports multiple search modes:
	 * <ul>
	 * <li><strong>By IDs:</strong> Direct lookup using land info IDs</li>
	 * <li><strong>By Mobile Number:</strong> Search by owner's mobile number</li>
	 * <li><strong>By Name:</strong> Search by owner's name</li>
	 * <li><strong>By Land UID:</strong> Search by unique land identifier</li>
	 * </ul>
	 * 
	 * <p>
	 * <strong>Business Logic:</strong> This method is used to:
	 * <ul>
	 * <li>Retrieve existing land records for BPA applications</li>
	 * <li>Link citizens to their land parcels</li>
	 * <li>Enable search functionality in the UI</li>
	 * <li>Validate land ownership before BPA submission</li>
	 * </ul>
	 * 
	 * @param requestInfo  the request information for authentication
	 * @param landcriteria the search criteria containing filters
	 * @return list of land information records matching the criteria (empty if none
	 *         found)
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public ArrayList<LandInfo> searchLandInfoToBPA(RequestInfo requestInfo, LandSearchCriteria landcriteria) {

		// Step 1: Log search parameters for debugging
		logSearchCriteria(landcriteria);

		// Step 2: Build search URL with query parameters
		StringBuilder url = buildLandSearchUrl(landcriteria);

		// Step 3: Execute land service search
		LinkedHashMap responseMap = executeLandSearch(url, requestInfo);

		// Step 4: Extract and convert land info objects
		return extractAndConvertLandInfoList(responseMap);
	}

	/**
	 * Logs the search criteria for debugging and troubleshooting purposes.
	 * 
	 * @param landcriteria the search criteria to log
	 */
	private void logSearchCriteria(LandSearchCriteria landcriteria) {
		log.info("Searching land info with params - IDs: {}, Mobile: {}, LandUID: {}, Name: {}", landcriteria.getIds(),
				landcriteria.getMobileNumber(), landcriteria.getLandUId(), landcriteria.getName());
	}

	/**
	 * Builds the land service search URL with appropriate query parameters.
	 * 
	 * <p>
	 * This method constructs the URL based on the search criteria:
	 * <ul>
	 * <li>Always includes tenantId as base parameter</li>
	 * <li>Adds IDs parameter if IDs are provided (highest priority)</li>
	 * <li>Adds mobile number parameter if provided (second priority)</li>
	 * <li>Adds name parameter if provided (third priority)</li>
	 * </ul>
	 * 
	 * <p>
	 * <strong>Note:</strong> Parameters are mutually exclusive by priority order to
	 * ensure consistent query behavior.
	 * 
	 * @param landcriteria the search criteria
	 * @return the complete search URL with query parameters
	 */
	private StringBuilder buildLandSearchUrl(LandSearchCriteria landcriteria) {
		StringBuilder uri = new StringBuilder(config.getLandInfoHost());
		uri.append(config.getLandInfoSearch());
		uri.append("?tenantId=").append(landcriteria.getTenantId());

		// Add search parameters based on priority
		if (landcriteria.getIds() != null) {
			appendIdsParameter(uri, landcriteria.getIds());
		} else if (landcriteria.getMobileNumber() != null) {
			uri.append("&mobileNumber=").append(landcriteria.getMobileNumber());
		} else if (!StringUtils.isEmpty(landcriteria.getName())) {
			uri.append("&name=").append(landcriteria.getName());
		}

		return uri;
	}

	/**
	 * Appends comma-separated IDs parameter to the URL.
	 * 
	 * @param uri the URL builder to append to
	 * @param ids the list of land info IDs
	 */
	private void appendIdsParameter(StringBuilder uri, List<String> ids) {
		uri.append("&ids=");
		for (int i = 0; i < ids.size(); i++) {
			if (i != 0) {
				uri.append(",");
			}
			uri.append(ids.get(i));
		}
	}

	/**
	 * Executes the land service search call.
	 * 
	 * @param url         the search URL with query parameters
	 * @param requestInfo the request information for authentication
	 * @return the raw response map from land service
	 */
	private LinkedHashMap executeLandSearch(StringBuilder url, RequestInfo requestInfo) {
		RequestInfoWrapper requestInfoWrapper = RequestInfoWrapper.builder().requestInfo(requestInfo).build();

		return (LinkedHashMap) serviceRequestRepository.fetchResult(url, requestInfoWrapper);
	}

	/**
	 * Extracts and converts land info objects from the service response.
	 * 
	 * <p>
	 * This method:
	 * <ul>
	 * <li>Extracts the "LandInfo" array from response</li>
	 * <li>Converts each raw map to LandInfo domain object</li>
	 * <li>Returns empty list if no land info found</li>
	 * </ul>
	 * 
	 * @param responseMap the raw response from land service
	 * @return list of converted land info objects
	 */
	private ArrayList<LandInfo> extractAndConvertLandInfoList(LinkedHashMap responseMap) {
		ArrayList<LandInfo> landData = new ArrayList<>();

		if (responseMap == null || responseMap.get("LandInfo") == null) {
			return landData;
		}

		ArrayList<LandInfo> landInfo = (ArrayList<LandInfo>) responseMap.get("LandInfo");

		// Convert each land info map to domain object
		for (Object landInfoObj : landInfo) {
			landData.add(mapper.convertValue(landInfoObj, LandInfo.class));
		}

		return landData;
	}

	/**
	 * Searches for land information using a simplified plane search (IDs only).
	 * 
	 * <p>
	 * This is a simplified version of the land search that only supports searching
	 * by land info IDs, typically used for direct lookups when the ID is already
	 * known.
	 * 
	 * @param requestInfo  the request information for authentication
	 * @param landcriteria the search criteria containing land info IDs
	 * @return list of land information records matching the IDs
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public ArrayList<LandInfo> searchLandInfoToBPAForPlaneSearch(RequestInfo requestInfo,
			LandSearchCriteria landcriteria) {

		log.debug("Searching land info (plane search) with IDs: {}", landcriteria.getIds());

		// Build URL with IDs only
		StringBuilder url = buildPlaneSearchUrl(landcriteria);

		// Execute search
		LinkedHashMap responseMap = executeLandSearch(url, requestInfo);

		// Extract and convert results
		return extractAndConvertLandInfoList(responseMap);
	}

	/**
	 * Builds the search URL for plane search (IDs only).
	 * 
	 * @param landcriteria the search criteria containing IDs
	 * @return the complete search URL
	 */
	private StringBuilder buildPlaneSearchUrl(LandSearchCriteria landcriteria) {
		StringBuilder uri = new StringBuilder(config.getLandInfoHost());
		uri.append(config.getLandInfoSearch());
		uri.append("?tenantId=").append(landcriteria.getTenantId());

		if (landcriteria.getIds() != null) {
			appendIdsParameter(uri, landcriteria.getIds());
		}

		return uri;
	}

	private void enrichLandInfoRequest(BPA bpa) {
		Map<String, Object> additionalDetails = bpa.getAdditionalDetails() != null ? (Map) bpa.getAdditionalDetails()
				: new HashMap<String, Object>();
		String isNewOwner = "";
		String isGpaHolder = "";
		if (!CollectionUtils.isEmpty(additionalDetails)
				&& !ObjectUtils.isEmpty(additionalDetails.get(BPAConstants.APPLICATION_DATA))) {
			Map<String, Object> landInfoData = (Map<String, Object>) additionalDetails
					.get(BPAConstants.APPLICATION_DATA);
			isNewOwner = String.valueOf(landInfoData.get(BPAConstants.OPTION_1));
			isGpaHolder = String.valueOf(landInfoData.get(BPAConstants.OPTION_2));

		}

		enrichNewOwners(bpa, isNewOwner);

		enrichGpaHolders(bpa, isGpaHolder);
	}

	private void enrichGpaHolders(BPA bpa, String isGpaHolder) {
		if (!StringUtils.isEmpty(isGpaHolder) && String.valueOf(isGpaHolder).equalsIgnoreCase("YES")) {
			List<NewOwnerInfo> gpaHolders = bpa.getLandInfo().getGpaHolders();
			gpaHolders.forEach(gpaHolder -> {
				gpaHolder.setId(gpaHolder.getId() != null ? gpaHolder.getId() : UUID.randomUUID().toString());
				gpaHolder.setNewOwnerShipMajorType(bpa.getLandInfo().getGpaOwnershipMajorType());
				gpaHolder.setNewOwnershipCategory(bpa.getLandInfo().getGpaOwnershipCategory());
				/*
				 * gpaHolder.setCreatedBy(bpa.getLandInfo().getAuditDetails().getCreatedBy());
				 * gpaHolder.setCreatedDate(bpa.getLandInfo().getAuditDetails().getCreatedTime()
				 * ); gpaHolder.setLastModifiedBy(bpa.getLandInfo().getAuditDetails().
				 * getLastModifiedBy());
				 * gpaHolder.setLastModifiedDate(bpa.getLandInfo().getAuditDetails().
				 * getLastModifiedTime());
				 * 
				 */
			});
			bpa.getLandInfo().setGpaHolders(gpaHolders);
		}
	}

	private void enrichNewOwners(BPA bpa, String isNewOwner) {
		if (!StringUtils.isEmpty(isNewOwner) && String.valueOf(isNewOwner).equalsIgnoreCase("NO")) {
			List<NewOwnerInfo> newOwners = bpa.getLandInfo().getNewOwners();

			newOwners.forEach(newOwner -> {
				newOwner.setId(newOwner.getId() != null ? newOwner.getId() : UUID.randomUUID().toString());
				newOwner.setNewOwnerShipMajorType(bpa.getLandInfo().getNewOwnershipMajorType());
				newOwner.setNewOwnershipCategory(bpa.getLandInfo().getNewOwnershipCategory());
				/*
				 * newOwner.setCreatedBy(bpa.getLandInfo().getAuditDetails().getCreatedBy());
				 * newOwner.setCreatedDate(bpa.getLandInfo().getAuditDetails().getCreatedTime())
				 * ; newOwner.setLastModifiedBy(bpa.getLandInfo().getAuditDetails().
				 * getLastModifiedBy());
				 * newOwner.setLastModifiedDate(bpa.getLandInfo().getAuditDetails().
				 * getLastModifiedTime());
				 * 
				 */
			});
			bpa.getLandInfo().setNewOwners(newOwners);
		}
	}

	/**
	 * Enriches owner information with primary owner flags for multiple owners
	 * scenario.
	 * 
	 * <p>
	 * This method ensures that in cases of multiple individual owners, each owner
	 * has the {@code isPrimaryOwner} flag properly set. If not already set, it
	 * defaults to {@code false}.
	 * 
	 * <p>
	 * <strong>Business Logic:</strong>
	 * <ul>
	 * <li>Only applies to "individual.multipleowners" ownership category</li>
	 * <li>Primary owner flag indicates the main responsible party</li>
	 * <li>At least one owner should be marked as primary owner</li>
	 * <li>Defaults to false to allow explicit setting by user</li>
	 * </ul>
	 * 
	 * <p>
	 * <strong>Use Case:</strong> In joint ownership scenarios (e.g., family
	 * property, partnership), one owner is designated as primary for official
	 * communication and decision-making purposes.
	 * 
	 * @param bpaRequest the BPA request containing owner information to enrich
	 */
	private void enrichOwnerInfoDetails(BPARequest bpaRequest) {
		String ownershipCategory = bpaRequest.getBPA().getLandInfo().getOwnershipCategory();

		// Check if this is a multiple owners scenario
		if (ownershipCategory != null && ownershipCategory.toLowerCase().contains("individual.multipleowners")) {

			// Set isPrimaryOwner flag for each owner (default to false if not set)
			bpaRequest.getBPA().getLandInfo().getOwners().forEach(owner -> {
				owner.setIsPrimaryOwner(owner.isIsPrimaryOwner() != null ? owner.isIsPrimaryOwner() : Boolean.FALSE);
			});
		}
	}
}
