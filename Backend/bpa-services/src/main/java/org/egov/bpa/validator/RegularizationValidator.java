package org.egov.bpa.validator;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.validation.Valid;

import org.egov.bpa.config.BPAConfiguration;
import org.egov.bpa.repository.RegularizationIssueFixRepository;
import org.egov.bpa.repository.ScnRepository;
import org.egov.bpa.service.RegularizationWorkflow;
import org.egov.bpa.util.BPAConstants;
import org.egov.bpa.util.BPAErrorConstants;
import org.egov.bpa.util.IssueFixConstants;
import org.egov.bpa.util.RegularizationConstants;
import org.egov.bpa.util.RegularizationUtil;
import org.egov.bpa.web.model.BPASearchCriteria;
import org.egov.bpa.web.model.Document;
import org.egov.bpa.web.model.Notice;
import org.egov.bpa.web.model.NoticeSearchCriteria;
import org.egov.bpa.web.model.RegularizationDraft;
import org.egov.bpa.web.model.RegularizationDraftRequest;
import org.egov.bpa.web.model.RegularizationDraftSearchCriteria;
import org.egov.bpa.web.model.VillageSearchCriteria;
import org.egov.bpa.web.model.collection.Demand;
import org.egov.bpa.web.model.collection.DemandSearchCriteria;
import org.egov.bpa.web.model.collection.Payment;
import org.egov.bpa.web.model.collection.PaymentSearchCriteria;
import org.egov.bpa.web.model.landInfo.OwnerInfo;
import org.egov.bpa.web.model.regularization.AppType;
import org.egov.bpa.web.model.regularization.Regularization;
import org.egov.bpa.web.model.regularization.RegularizationDocUpload;
import org.egov.bpa.web.model.regularization.RegularizationDscDetails;
import org.egov.bpa.web.model.regularization.RegularizationFieldInspection;
import org.egov.bpa.web.model.regularization.RegularizationRequest;
import org.egov.bpa.web.model.regularization.RegularizationSearchCriteria;
import org.egov.bpa.web.model.workflow.ProcessInstance;
import org.egov.common.contract.request.RequestInfo;
import org.egov.tracer.model.CustomException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import com.jayway.jsonpath.JsonPath;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class RegularizationValidator {
	
	@Autowired
	private RegularizationUtil util;
	
	@Autowired
	private BPAConfiguration config;
	
	@Autowired
	private RegularizationIssueFixRepository repository;
	
	@Autowired
	private ScnRepository scnRepository;

	@Autowired
	private RegularizationWorkflow workflowService;
	
	/**
	 * Validate the data before creating Regularization Request
	 * 
	 * @param requestInfo
	 * @param regularization
	 */
	public void validateLandCreate(RequestInfo requestInfo, Regularization regularization) {
		
		// Call MDMS once to get OwnerShip Master data and ODA master data: FileName for ODA -> OdaUlbs.json
		// FileName for OwnerShip -> OwnerShipCategory.json
		Object mdmsData = util.mDMSCall(requestInfo, RegularizationConstants.STATE_TENANTID);
		
		// Validate if the Ulb is under ODA ulbs, Land Regulariation is only allowed for ODA ulbs
		validateIfUlbUnderODA(regularization, mdmsData);
		
		// No Validation needed for sale deed date as per ODA OTPIT amendments
		//validateSaleDeedDate(regularization);

		// Validate the mandatory fileds if the plot is for gift
		vaildateIsPlotForGift(regularization);
		
		// Validate the access road width, it should be 4.5 metres for Type A applications and 6 Meters for Type B
		validateAccessRoadWidth(regularization);
		
		// Validate Owner details, all the validations for owners at creation time are under this method only
		validateOwnerDetails(regularization, mdmsData);
			
	}

	/**
	 * Validate the owner details for create request
	 * 
	 * @param regularization
	 * @param mdmsData
	 */
	private void validateOwnerDetails(Regularization regularization, Object mdmsData) {
		validateMdmsData(regularization, mdmsData);
		validateDuplicateUser(regularization);
		validateOwnerDetails(regularization);
	}
	
	
	/**
	 * Validate if all the mandatory info is present, in regularization request
	 * 
	 * @param regularization
	 */
	private void validateOwnerDetails(Regularization regularization) {
		
		if (regularization.getOwnershipCategory().toLowerCase().contains("individual.singleowner")
				&& Objects.isNull(regularization.getOwners().get(0).getMobileNumber())) {
			throw new CustomException("OWNER_MOBILE_NUMBER_MISSING",
					"Mobile number is mandatory for Individual Single Owner");
		}

		if (regularization.getOwnershipCategory().toLowerCase().contains("individual.singleowner")
				&& Objects.isNull(regularization.getOwners().get(0).getName())) {
			throw new CustomException("OWNER_NAME_MISSING",
					"Applicant name is mandatory for Individual Single Owner");
		}

		if (regularization.getOwnershipCategory().toLowerCase().contains("individual.singleowner")
				&& Objects.isNull(regularization.getOwners().get(0).getCorrespondenceAddress())) {
			throw new CustomException("OWNER_ADDRESS_MISSING",
					"Applicant Correspondence Address is mandatory for Individual Single Owner");
		}
		
	}

	/**
	 * Validate if Same user is sent twice in multiple owners request, if yes don't
	 * allow application creation
	 * 
	 * @param regularization
	 */
	private void validateDuplicateUser(Regularization regularization) {
		List<OwnerInfo> owners = regularization.getOwners();
		if (owners.size() > 1) {
			List<String> mobileNos = new ArrayList<String>();
			for (OwnerInfo owner : owners) {
				if (mobileNos.contains(owner.getMobileNumber())) {
					throw new CustomException("DUPLICATE_MOBILENUMBER_EXCEPTION",
							"Duplicate mobile numbers found for owners");
				} else {
					mobileNos.add(owner.getMobileNumber());
				}
			}
		}
	}

	/**
	 * Validate the Ownership category from MDMS data 
	 * 
	 * @param regularization
	 * @param mdmsData
	 */
	private void validateMdmsData(Regularization regularization, Object mdmsData) {

		Map<String, String> errorMap = new HashMap<>();

		Map<String, List<String>> masterData = getAttributeValues(mdmsData);
		String[] masterArray = { RegularizationConstants.OWNERSHIP_CATEGORY };

		validateIfMasterPresent(masterArray, masterData);

		regularization.getOwners().forEach(owner -> {
			if (owner.getOwnerType() == null) {
				owner.setOwnerType("NONE");
			}
		});
		if (!masterData.get(RegularizationConstants.OWNERSHIP_CATEGORY).contains(regularization.getOwnershipCategory()))
			errorMap.put("INVALID OWNERSHIPCATEGORY",
					"The OwnerShipCategory '" + regularization.getOwnershipCategory() + "' does not exists");

		if (!CollectionUtils.isEmpty(errorMap))
			throw new CustomException(errorMap);

	}

	private void validateIfMasterPresent(String[] masterNames, Map<String, List<String>> codes) {
		Map<String, String> errorMap = new HashMap<>();
		for (String masterName : masterNames) {
			if (CollectionUtils.isEmpty(codes.get(masterName))) {
				errorMap.put("MDMS DATA ERROR ", "Unable to fetch " + masterName + " codes from MDMS");
			}
		}
		if (!errorMap.isEmpty())
			throw new CustomException(errorMap);
	}

	public Map<String, List<String>> getAttributeValues(Object mdmsData) {

		List<String> modulepaths = Arrays.asList(RegularizationConstants.COMMON_MASTER_JSONPATH_CODE);
		final Map<String, List<String>> mdmsResMap = new HashMap<>();
		modulepaths.forEach(modulepath -> {
			try {
				mdmsResMap.putAll(JsonPath.read(mdmsData, modulepath));
			} catch (Exception e) {
				throw new CustomException("MDMS_ERROR", "Data not fetched from MDMS for request...");
			}
		});
		return mdmsResMap;
	}
	
	/**
	 * Vaildation check for Access Road Width for both type of Land Regularization,
	 * For Type A applications width should be minimum 4.5 meters and for Type B it
	 * should be 6 meters minimum.. For Building regularization it should be more
	 * than 4.5 meters
	 * 
	 * @param regularization
	 */
	private void validateAccessRoadWidth(Regularization regularization) {
		
		Boolean isValidA18 = Boolean.FALSE;
		
		if (RegularizationConstants.OBPS_ALL_DA_LIST.contains(regularization.getTenantId().toLowerCase()) && regularization.getAppType().equals(AppType.BUILDING)) {
			List<Map<String, Object>> buildingBlocks = (List<Map<String, Object>>) regularization
					.getBuildingRegularizationInfo().getBuildingBlocks();
			List<String> subOccupancyValues = new ArrayList<>();
			for (Map<String, Object> buildingBlock : buildingBlocks) {

				List<Map<String, Object>> floors = (List<Map<String, Object>>) buildingBlock.get("floors");

				if (floors != null) {
					for (Map<String, Object> floor : floors) {

						Map<String, Object> subOcuupancy = (Map<String, Object>) floor.get("subOcuupancy");

						if (subOcuupancy != null && subOcuupancy.get("value") != null) {
							subOccupancyValues.add(subOcuupancy.get("value").toString());
						}
					}
				}
			}			
			
			if (!CollectionUtils.isEmpty(subOccupancyValues)
					&& subOccupancyValues.contains(RegularizationConstants.INDUSTRIAL_BUILDING_CODE)) {
				isValidA18 = Boolean.TRUE;
			}

		} 

		if (regularization.getAppType().equals(AppType.LAND)
				|| regularization.getAppType().equals(AppType.LAND_AND_BUILDING)) {		
							
			if (isValidA18 && regularization.getLandRegularizationInfo().getAccessRoadWidth()
					.compareTo(new BigDecimal("6.0")) < 0) {
				log.info(" Access Road width should be more than 6.0 meters !!!");
				throw new CustomException("CREATE_ERROR", "Access Road width should be more than 6.0 meters !!!");
			}
			
 
			if (regularization.getLandRegularizationInfo().getLandRegularizationType()
					.equalsIgnoreCase(RegularizationConstants.PLOTS_SUBDIVIDED_BEFORE_30th_MAY_2017)
					&& regularization.getLandRegularizationInfo().getAccessRoadWidth()
							.compareTo(new BigDecimal("4.5")) < 0) {
				log.info(" Access Road width should be more than 4.5 meters !!!");
				throw new CustomException("CREATE_ERROR", "Access Road width should be more than 4.5 meters !!!");
			}

			if (regularization.getLandRegularizationInfo().getLandRegularizationType()
					.equalsIgnoreCase(RegularizationConstants.PLOTS_SUBDIVIDED_AFTER_30th_MAY_2017_TILL_29th_SEP_2022)
					&& regularization.getLandRegularizationInfo().getAccessRoadWidth()
							.compareTo(new BigDecimal("6.0")) < 0) {
				log.info(" Access Road width should be more than 6 meters !!!");
				throw new CustomException("CREATE_ERROR", "Access Road width should be more than 6 meters !!!");
			}
		} else if (regularization.getAppType().equals(AppType.BUILDING)) {

			
			if (isValidA18 && regularization.getLandRegularizationInfo().getAccessRoadWidth()
					.compareTo(new BigDecimal("6.0")) < 0) {
				log.info(" Access Road width should be more than 6.0 meters !!!");
				throw new CustomException("CREATE_ERROR", "Access Road width should be more than 6.0 meters !!!");
			} else if (regularization.getLandRegularizationInfo().getAccessRoadWidth()
					.compareTo(new BigDecimal("4.5")) < 0) {
				log.info(" Access Road width should be more than 4.5 meters !!!");
				throw new CustomException("CREATE_ERROR", "Access Road width should be more than 4.5 meters !!!");
			}
		}

	}

	/**
	 * Area to be Gifted and Reason for Gift are Mandatory if PlotToBeGifted checkbox is selected from FrontEnd
	 * 
	 * @param regularization
	 */
	private void vaildateIsPlotForGift(Regularization regularization) {
		
		regularization.getLandRegularizationInfo().getPlotInfo().forEach(plot -> {
			if (!StringUtils.isEmpty(plot.getIsPlotToBeGifted()) 
					&& plot.getIsPlotToBeGifted().equalsIgnoreCase(RegularizationConstants.ANSWER_YES)) {
				if (StringUtils.isEmpty(plot.getAreaToBeGifted()) || StringUtils.isEmpty(plot.getReasonForGift())) {
					log.info(" Area to be Gifted or Reason for Gift is Empty for plot: " + plot.getPlotNo());
					throw new CustomException("CREATE_ERROR",
							"Kindly Provide Area to be Gifted and Reason for Gift for plot " + plot.getPlotNo());
				}
			}
		});

	}

	/**
	 * Allow the Application to Proceed if Sale Deed Date is Before 30th Dec, 2022 (Business Requirement)
	 * 
	 * @param regularization
	 */
	private void validateSaleDeedDate(Regularization regularization) {

		regularization.getLandRegularizationInfo().getPlotInfo().forEach(plot -> {
			if (plot.getSaleDeedDate() > RegularizationConstants.THIRTY_DECEMBER_2022) {
				log.info(" Sale Deed Date is After 30th Dec, 2023 for Plot " + plot.getPlotNo());
				throw new CustomException("CREATE_ERROR", "Sale Deed Date is After 30th Dec, 2023 for Plot " + plot.getPlotNo());
			}
		});

	}

	/**
	 * Validate if the Ulb is under ODA or not, if not application can't be created, ODA Ulbs list is defined in MDMS data
	 * 
	 * @param regularization
	 * @param mdmsData
	 */
	private void validateIfUlbUnderODA(Regularization regularization, Object mdmsData) {

		Set<String> odaUlbs = new HashSet<>();

		List<LinkedHashMap<String, Object>> odaulbsFromMdms = JsonPath.read(mdmsData,
				RegularizationConstants.ODA_ULBS_JSONPATH_CODE);

		for (HashMap<String, Object> odaulb : odaulbsFromMdms) {
			if (((String) odaulb.get("oda")).equalsIgnoreCase("true")) {
				odaUlbs.add((String) odaulb.get("ulb"));
			}
		}
		if (!odaUlbs.contains(regularization.getTenantId())) {
			log.info(regularization.getTenantId() + " does not fall under ODA !!");
			throw new CustomException("CREATE_ERROR", "Ulb is not under ODA area !!");
		}
	}
	
	/**
	 * Validate the update request here
	 * 
	 * @param regularizationRequest
	 * @param searchResult
	 * @param mdmsData
	 * @param currentState
	 */
	public void validateUpdate(@Valid RegularizationRequest regularizationRequest, List<Regularization> searchResult,
			Object mdmsData, String currentState) {

		Regularization regularization = regularizationRequest.getRegularization();

		validateApplicationDocuments(regularizationRequest, mdmsData, currentState);
		validateAllIds(searchResult, regularization);
		validateDuplicateDocuments(regularizationRequest);
		setFieldsFromSearch(regularizationRequest, searchResult);

	}

	/**
	 * Set the required fields here
	 * 
	 * @param regularizationRequest
	 * @param searchResult
	 */
	private void setFieldsFromSearch(@Valid RegularizationRequest regularizationRequest,
			List<Regularization> searchResult) {

		Map<String, Regularization> idToRegularizationFromSearch = new HashMap<>();

		searchResult.forEach(item -> {
			idToRegularizationFromSearch.put(item.getId(), item);
		});

		regularizationRequest.getRegularization().getAuditDetails().setCreatedBy(idToRegularizationFromSearch
				.get(regularizationRequest.getRegularization().getId()).getAuditDetails().getCreatedBy());
		regularizationRequest.getRegularization().getAuditDetails().setCreatedTime(idToRegularizationFromSearch
				.get(regularizationRequest.getRegularization().getId()).getAuditDetails().getCreatedTime());
		regularizationRequest.getRegularization().setStatus(
				idToRegularizationFromSearch.get(regularizationRequest.getRegularization().getId()).getStatus());

	}

	/**
	 * Validate if same document is sent more than one time in request
	 * 
	 * @param regularizationRequest
	 */
	private void validateDuplicateDocuments(@Valid RegularizationRequest regularizationRequest) {

		if (regularizationRequest.getRegularization().getDocuments() != null) {
			List<String> documentFileStoreIds = new LinkedList<String>();
			regularizationRequest.getRegularization().getDocuments().forEach(document -> {
				if (documentFileStoreIds.contains(document.getFileStoreId()))
					throw new CustomException(RegularizationConstants.DUPLICATE_DOCUMENT,
							"Same document cannot be used multiple times");
				else
					documentFileStoreIds.add(document.getFileStoreId());
			});
		}

	}

	/**
	 * Validate the applicaton documents here. Configured in MDMS
	 * 
	 * @param regularizationRequest
	 * @param mdmsData
	 * @param currentState
	 */
	private void validateApplicationDocuments(@Valid RegularizationRequest regularizationRequest, Object mdmsData,
			String currentState) {

		Map<String, List<String>> masterData = util.getAttributeValues(mdmsData);

		Regularization regularization = regularizationRequest.getRegularization();

		if (!regularization.getWorkflow().getAction().equalsIgnoreCase(BPAConstants.ACTION_REJECT)
				&& !regularization.getWorkflow().getAction().equalsIgnoreCase(BPAConstants.ACTION_ADHOC)
				&& !regularization.getWorkflow().getAction().equalsIgnoreCase(BPAConstants.ACTION_PAY)) {

			String filterExp = "$.[?(@.WFState=='" + currentState + "' && @.applicationType == '"+ regularization.getAppType().toString() +"')].docTypes";

			List<Object> docTypeMappings = JsonPath.read(masterData.get(RegularizationConstants.DOCUMENT_TYPE_MAPPING),
					filterExp);

			List<Document> allDocuments = new ArrayList<Document>();
			if (regularization.getDocuments() != null) {
				allDocuments.addAll(regularization.getDocuments());
			}

			if (CollectionUtils.isEmpty(docTypeMappings)) {
				return;
			}

			filterExp = "$.[?(@.required==true)].code";
			List<String> requiredDocTypes = JsonPath.read(docTypeMappings.get(0), filterExp);

			List<String> validDocumentTypes = masterData.get(RegularizationConstants.DOCUMENT_TYPE);

			
			if (!CollectionUtils.isEmpty(allDocuments)) {

				allDocuments.forEach(document -> {

					if (!validDocumentTypes.contains(document.getDocumentType())) {
						throw new CustomException(RegularizationConstants.UNKNOWN_DOCUMENTTYPE,
								document.getDocumentType() + " is Unkown");
					}
				});

				if (requiredDocTypes.size() > 0) {

					List<String> addedDocTypes = new ArrayList<String>();
					allDocuments.forEach(document -> {

						String docType = document.getDocumentType();
						int lastIndex = docType.lastIndexOf(".");
						String documentNs = "";
						if (lastIndex > 1) {
							documentNs = docType.substring(0, lastIndex);
							if(requiredDocTypes.contains(documentNs)) {
								requiredDocTypes.remove(documentNs);
							}
						} else if (lastIndex == 1) {
							throw new CustomException(RegularizationConstants.INVALID_DOCUMENTTYPE,
									document.getDocumentType() + " is Invalid");
						} else {
							documentNs = docType;
						}

						addedDocTypes.add(documentNs);
					});
				}
			} else if (requiredDocTypes.size() > 0) {
				throw new CustomException(RegularizationConstants.MANADATORY_DOCUMENTPYE_MISSING,
						"Atleast " + requiredDocTypes.size() + " Documents are requied ");
			}
			if(requiredDocTypes.size() > 0) {
				throw new CustomException(RegularizationConstants.MANADATORY_DOCUMENTPYE_MISSING,
						"Either one or more mandatory documents are missing. Kindly upload them to proceed !!");
			}
			regularization.setDocuments(allDocuments);
		}

	}

	/**
	 * Validation for application id
	 * 
	 * @param searchResult
	 * @param regularization
	 */
	private void validateAllIds(List<Regularization> searchResult, Regularization regularization) {

		Map<String, Regularization> idToRegularizationFromSearch = new HashMap<>();
		searchResult.forEach(item -> {
			idToRegularizationFromSearch.put(item.getId(), item);
		});

		Map<String, String> errorMap = new HashMap<>();
		Regularization searchedRegularization = idToRegularizationFromSearch.get(regularization.getId());

		if (!searchedRegularization.getApplicationNo().equalsIgnoreCase(regularization.getApplicationNo()))
			errorMap.put("INVALID UPDATE",
					"The application number from search: " + searchedRegularization.getApplicationNo()
							+ " and from update: " + regularization.getApplicationNo() + " does not match");

		if (!searchedRegularization.getId().equalsIgnoreCase(regularization.getId()))
			errorMap.put("INVALID UPDATE", "The id " + regularization.getId() + " does not exist");

		if (!CollectionUtils.isEmpty(errorMap))
			throw new CustomException(errorMap);
	}
	
	
	
	/**
	 * Validate Regularization search 
	 * 
	 * @param requestInfo
	 * @param criteria
	 */
	public void validateSearch(RequestInfo requestInfo, RegularizationSearchCriteria criteria) {
		if (!requestInfo.getUserInfo().getType().equalsIgnoreCase(BPAConstants.CITIZEN) && criteria.isEmpty())
			throw new CustomException(BPAErrorConstants.INVALID_SEARCH, "Search without any paramters is not allowed");

		if (!requestInfo.getUserInfo().getType().equalsIgnoreCase(BPAConstants.CITIZEN) && !criteria.tenantIdOnly()
				&& criteria.getTenantId() == null)
			throw new CustomException(BPAErrorConstants.INVALID_SEARCH, "TenantId is mandatory in search");

		if (requestInfo.getUserInfo().getType().equalsIgnoreCase(BPAConstants.CITIZEN) && !criteria.isEmpty()
				&& !criteria.tenantIdOnly() && criteria.getTenantId() == null)
			throw new CustomException(BPAErrorConstants.INVALID_SEARCH, "TenantId is mandatory in search");
		
		if ((criteria.isEscalatedToMe() || criteria.isAboutToEscalate()) && requestInfo.getUserInfo().getUuid()==null)
            throw new CustomException(BPAErrorConstants.INVALID_SEARCH, "uuid is mandatory in search");

		String allowedParamStr = null;

		if (requestInfo.getUserInfo().getType().equalsIgnoreCase(BPAConstants.CITIZEN))
			allowedParamStr = config.getAllowedCitizenSearchParameters();
		else if (requestInfo.getUserInfo().getType().equalsIgnoreCase(BPAConstants.EMPLOYEE))
			allowedParamStr = config.getAllowedEmployeeSearchParameters();
		// handling for edcr data pull scheduler search- 
		else if (requestInfo.getUserInfo().getType().equalsIgnoreCase(BPAConstants.SYSTEM))
			allowedParamStr = config.getAllowedEmployeeSearchParameters();
		else
			throw new CustomException(BPAErrorConstants.INVALID_SEARCH,
					"The userType: " + requestInfo.getUserInfo().getType() + " does not have any search config");

		if (StringUtils.isEmpty(allowedParamStr) && !criteria.isEmpty())
			throw new CustomException(BPAErrorConstants.INVALID_SEARCH, "No search parameters are expected");
		else {
			List<String> allowedParams = Arrays.asList(allowedParamStr.split(","));
			validateSearchParams(criteria, allowedParams);
		}
	}

	/**
	 * Validates if the paramters coming in search are allowed
	 * 
	 * @param criteria
	 *            BPA search criteria
	 * @param allowedParams
	 *            Allowed Params for search
	 */
	private void validateSearchParams(RegularizationSearchCriteria criteria, List<String> allowedParams) {

		if (criteria.getApplicationNo() != null && !allowedParams.contains("applicationNo"))
			throw new CustomException(BPAErrorConstants.INVALID_SEARCH, "Search on applicationNo is not allowed");

		if (criteria.getStatus() != null && !allowedParams.contains("status"))
			throw new CustomException(BPAErrorConstants.INVALID_SEARCH, "Search on Status is not allowed");

		if (criteria.getIds() != null && !allowedParams.contains("ids"))
			throw new CustomException(BPAErrorConstants.INVALID_SEARCH, "Search on ids is not allowed");

		if (criteria.getMobileNumber() != null && !allowedParams.contains("mobileNumber"))
			throw new CustomException(BPAErrorConstants.INVALID_SEARCH, "Search on mobileNumber is not allowed");

		if (criteria.getOffset() != null && !allowedParams.contains("offset"))
			throw new CustomException(BPAErrorConstants.INVALID_SEARCH, "Search on offset is not allowed");

		if (criteria.getLimit() != null && !allowedParams.contains("limit"))
			throw new CustomException(BPAErrorConstants.INVALID_SEARCH, "Search on limit is not allowed");
		
		if (criteria.getApprovalDate() != null && (criteria.getApprovalDate() > new Date().getTime()))
			throw new CustomException(BPAErrorConstants.INVALID_SEARCH, "Permit Order Genarated date cannot be a future date");
		
		if (criteria.getFromDate() != null && (criteria.getFromDate() > new Date().getTime()))
			throw new CustomException(BPAErrorConstants.INVALID_SEARCH, "From date cannot be a future date");

		if (criteria.getToDate() != null && criteria.getFromDate() != null
				&& (criteria.getFromDate() > criteria.getToDate()))
			throw new CustomException(BPAErrorConstants.INVALID_SEARCH, "To date cannot be prior to from date");
	}
	
	/**
	 * Validates if the search parameters are valid
	 * 
	 * @param requestInfo
	 *            The requestInfo of the incoming request
	 * @param criteria
	 *            The BPASearch Criteria
	 */
	//TODO need to make the changes in the data
	public void validateReportSearch(RequestInfo requestInfo, RegularizationSearchCriteria criteria) {

		String allowedParamStr = null;

		if (requestInfo.getUserInfo().getType().equalsIgnoreCase(BPAConstants.CITIZEN))
			allowedParamStr = config.getAllowedCitizenSearchParameters();
		else if (requestInfo.getUserInfo().getType().equalsIgnoreCase(BPAConstants.EMPLOYEE))
			allowedParamStr = config.getAllowedEmployeeSearchParameters();
		else
			throw new CustomException(BPAErrorConstants.INVALID_SEARCH,
					"The userType: " + requestInfo.getUserInfo().getType() + " does not have any search config");

		if (StringUtils.isEmpty(allowedParamStr) && !criteria.isEmpty())
			throw new CustomException(BPAErrorConstants.INVALID_SEARCH, "No search parameters are expected");
		else {
			List<String> allowedParams = Arrays.asList(allowedParamStr.split(","));
			validateSearchParams(criteria, allowedParams);
		}
	}


	

	/**
	 * Validation for update regularization dsc details
	 * 
	 * @param regularizationRequest
	 * @param searchResult
	 */
	public void validateRegularizationDscDetails(@Valid RegularizationRequest regularizationRequest,
			List<Regularization> searchResult) {
		Map<String, Regularization> idToRegularizationMapFromSearch = new HashMap<>();
		searchResult.forEach(regularization -> {
			idToRegularizationMapFromSearch.put(regularization.getId(), regularization);
		});

		Regularization regularizationFromRequest = regularizationRequest.getRegularization();
		Regularization regularizationFromSearch = idToRegularizationMapFromSearch.get(regularizationFromRequest.getId());

		if (ObjectUtils.isEmpty(regularizationFromRequest)) {
			throw new CustomException("APPLICATION  ERROR", "The Application does not exist .");
		}
		
		if (!(regularizationFromSearch.getStatus().equalsIgnoreCase("APPROVED")
				|| regularizationFromSearch.getStatus().equalsIgnoreCase("REJECTED"))) {
			throw new CustomException("APPLICATION STATUS ERROR",
					"The Application should be in approved or rejected state for document signing .");
		}

		List<RegularizationDscDetails> dscDetailsFromRequest = regularizationFromRequest.getDscDetails();
		List<RegularizationDscDetails> dscDetailsFromSearch = regularizationFromSearch.getDscDetails();
		

		if (!CollectionUtils.isEmpty(dscDetailsFromSearch)) {
			if (dscDetailsFromSearch.size() != 1) 
				throw new CustomException("DSC DETAILS ERROR", "There should be only one Digitally signed documemts for application .");

			RegularizationDscDetails searchedDscDetail = dscDetailsFromSearch.get(0);

			if (!StringUtils.isEmpty(searchedDscDetail.getDocumentId())) 
				throw new CustomException("DSC DETAILS ERROR", "This application License is already digitally signed.");
			
		} else {
			throw new CustomException("DSC DETAILS ERROR", "There are no Digitally signed documemts for this application.");
		}
		
		if (!CollectionUtils.isEmpty(dscDetailsFromRequest)) {
			if (dscDetailsFromRequest.size() != 1)
				throw new CustomException("DSC DETAILS ERROR", "There should be only one Digitally signed documemts for application .");

			RegularizationDscDetails requestDscDetails = dscDetailsFromRequest.get(0);
			RegularizationDscDetails searchedDscDetails = dscDetailsFromSearch.get(0);

			if (StringUtils.isEmpty(requestDscDetails.getApprovedBy()))
				throw new CustomException("DSC DETAILS ERROR", "Approved by is mandatory in Digitally signed documemts .");

			if (StringUtils.isEmpty(requestDscDetails.getApplicationNo()))
				throw new CustomException("DSC DETAILS ERROR", "Application Number is mandatory in Digitally signed documemts .");

			if (StringUtils.isEmpty(requestDscDetails.getDocumentType()))
				throw new CustomException("DSC DETAILS ERROR", "Document Type is mandatory in Digitally signed documemts .");

			if (StringUtils.isEmpty(requestDscDetails.getId()))
				throw new CustomException("DSC DETAILS ERROR", "Id is mandatory in Digitally signed documemts .");

			if (StringUtils.isEmpty(requestDscDetails.getTenantId()))
				throw new CustomException("DSC DETAILS ERROR", "TenantId is mandatory in Digitally signed documemts .");

			if (StringUtils.isEmpty(requestDscDetails.getDocumentId()))
				throw new CustomException("DSC DETAILS ERROR", "DocumentId is mandatory in Digitally signed documemts .");

			if (!searchedDscDetails.getApplicationNo().equalsIgnoreCase(requestDscDetails.getApplicationNo()))
				throw new CustomException("DSC DETAILS ERROR", "DSC Document Application Number does not match .");

			if (!searchedDscDetails.getId().equalsIgnoreCase(requestDscDetails.getId()))
				throw new CustomException("DSC DETAILS ERROR", "DSC Document Id does not match .");

			if (!searchedDscDetails.getApprovedBy().equalsIgnoreCase(requestDscDetails.getApprovedBy()))
				throw new CustomException("DSC DETAILS ERROR", "DSC Document Approved by does not match .");

			if (!searchedDscDetails.getApprovedBy().equalsIgnoreCase(regularizationRequest.getRequestInfo().getUserInfo().getUuid()))
				throw new CustomException("DSC DETAILS ERROR", "DSC Document Can only be signed by the person who approved it .");

		} else {
			throw new CustomException("DSC DETAILS ERROR", "There are no Digitally signed documemts for this application .");
		}
	}

	
	
	
	/**
	 * Validation for Search regularization dsc details
	 *
	 * @param requestInfo
	 * @param searchCriteria
	 */
	public void validateDscSearch(RequestInfo requestInfo, RegularizationSearchCriteria searchCriteria) {
		if (StringUtils.isEmpty(searchCriteria.getTenantId()))
			throw new CustomException(BPAErrorConstants.INVALID_SEARCH, "TenantId is mandatory in search");
		
	}

	/**
	 * Validate building create request here
	 * 
	 * @param requestInfo
	 * @param regularization
	 */
	public void validateBuildingCreate(RequestInfo requestInfo, Regularization regularization) {

		// Validate Access road width, if less than 4.5 don't allow the application creation
		validateAccessRoadWidth(regularization);

	}


	
	/**
	 * Validate field inspection request
	 * @param fieldInspection
	 */
	public void validatefieldInspectionRequest(RegularizationFieldInspection fieldInspection) {
		validateDocs(fieldInspection);
		Map<Integer,List<String>> docTypes = getAlldocTypeforphotos(fieldInspection);
		log.info("docs list:"+docTypes);
		validateAllImagesUpload(fieldInspection,docTypes);
		log.info("docs list:"+docTypes);
	}
	
	
	
	/**
	 * Validate Documents
	 * @param fieldInspection
	 */
	private void validateDocs(RegularizationFieldInspection fieldInspection) {
		Object reportDetails = fieldInspection.getReportDetails();
		if (ObjectUtils.isEmpty(reportDetails))
			throw new CustomException("create error", "Failed to create feild inspection report, reportdetails can't be empty !");
		Object details = ((List<Object>) reportDetails).get(0);
		Object docs = (((Map) details).get(BPAConstants.DOCS));
		if (ObjectUtils.isEmpty(docs)) {
			throw new CustomException("create error", "Failed to create feild inspection report, docs, questions is mandatory!");
		}
	}
	
	
	
	/**
	 * Get all docType for photos
	 * @param fieldInspection
	 * @return
	 */
	private Map<Integer, List<String>> getAlldocTypeforphotos(RegularizationFieldInspection fieldInspection) {
		Object ReportDetails = fieldInspection.getReportDetails();
		Map<Integer, List<String>> doctype = new HashMap<>();
		List report = (List) ReportDetails;
		int count = 1;
		for (Object o : report) {
			// photos will be there
			List photos = (List) (((Map) o).get(BPAConstants.PHOTOS));
			if (Objects.nonNull(photos)) {
				List<String> docList = new ArrayList<String>();
				for (Object obj : photos) {
					Object docType = ((Map) obj).get(BPAConstants.DOCUMENTTYPE);
					if(Objects.nonNull(docType))
					docList.add(docType.toString());

				}
				doctype.put(count++, docList);
			}

		}

		return doctype;
	}

	
	
	/**
	 * validate all images upload
	 * @param fieldInspection
	 * @param docTypes
	 */
	private void validateAllImagesUpload(RegularizationFieldInspection fieldInspection, Map<Integer, List<String>> docTypes) {
		Boolean check = Boolean.FALSE;
		Object approachRoad = fieldInspection.getApproachRoad();
		log.info("approach road" + approachRoad);
		Object roadSideDrain = ((Map) approachRoad).get(BPAConstants.IS_ROAD_SIDE);
		if (Objects.isNull(roadSideDrain) || roadSideDrain.toString().isEmpty())
			throw new CustomException("create error", "Failed to create feild inspection report, roadSideDrain can't be empty !");
		String value = roadSideDrain.toString();
		if (value.equalsIgnoreCase(BPAConstants.YES)) {
			docTypes.forEach((k, v) -> {
				if (!v.contains(BPAConstants.ROAD_SIDE_DRAIN)) {
					throw new CustomException("create error", "Failed to create feild inspection report, pls upload road side drain image for all reports !");
				}
			});
		}
		
		Object siteSituation = fieldInspection.getSiteSituation();
		Object waterSupply = ((Map) siteSituation).get(BPAConstants.IS_WATER_SUPPLY);
		if (Objects.isNull(waterSupply) || waterSupply.toString().isEmpty())
			throw new CustomException("create error", "Failed to create feild inspection report, waterSupply can't be empty !");

		if (waterSupply.toString().equalsIgnoreCase(BPAConstants.YES)) {
			docTypes.forEach((k, v) -> {
				if (!v.contains(BPAConstants.WATER_SUPPLY_DOCTYPE)) {
					throw new CustomException("create error", "Failed to create feild inspection report, pls upload water supply image for all reports !");
				}
			});
		}

		Object electricSupply = ((Map) siteSituation).get(BPAConstants.IS_ELECTRIC_SUPPLY);
		if (Objects.isNull(electricSupply) || electricSupply.toString().isEmpty())
			throw new CustomException("create error", "Failed to create feild inspection report, electricitySupply can't be empty !");

		if (electricSupply.toString().equalsIgnoreCase(BPAConstants.YES)) {
			docTypes.forEach((k, v) -> {
				if (!v.contains(BPAConstants.ELECTRIC_SUPPLY_DOCTYPE)) {
					throw new CustomException("create error",
							"Failed to create feild inspection report, pls upload electricity supply image for all reports !");
				}
			});
		}
		

		Object drainage = ((Map) siteSituation).get(BPAConstants.IS_DRAINAGE_REQUIRED);
		if (Objects.isNull(drainage) || drainage.toString().isEmpty())
			throw new CustomException("create error", "Failed to create feild inspection report, drainage can't be empty !");

		if (drainage.toString().equalsIgnoreCase(BPAConstants.YES)) {
			docTypes.forEach((k, v) -> {
				if (!v.contains(BPAConstants.DRAINAGE_DOCTYPE)) {
					throw new CustomException("create error", "Failed to create feild inspection report, pls upload drainage image for all reports !");
				}
			});
		}
		

		Object sewregeDisposal = ((Map) siteSituation).get(BPAConstants.IS_SEWARGE_REQUIRED);
		if (Objects.isNull(sewregeDisposal) || sewregeDisposal.toString().isEmpty())
			throw new CustomException("create error", "Failed to create feild inspection report, sewrage can't be empty !");

		if (sewregeDisposal.toString().equalsIgnoreCase(BPAConstants.YES)) {
			docTypes.forEach((k, v) -> {
				if (!v.contains(BPAConstants.SEWARGE_DOCTYPE)) {
					throw new CustomException("create error",
							"Failed to create feild inspection report, pls upload sewrage image for all reports !");
				}
			});
		}

		Object rainWaterHarvesting = ((Map) siteSituation).get(BPAConstants.IS_RAINWATER_HARVESTING_REQUIRED);
		if (!Objects.isNull(rainWaterHarvesting) && !String.valueOf(rainWaterHarvesting).isEmpty()) {
			if (rainWaterHarvesting.toString().equalsIgnoreCase(BPAConstants.YES)) {
				docTypes.forEach((k, v) -> {
					if (!v.contains(BPAConstants.RAINWATER_HARVESTING_DOCTYPE)) {
						throw new CustomException("create error", "Failed to create feild inspection report, pls upload rainWaterHarvesting image for all reports !");
					}
				});
			}
		}

		Object noOfTrees = ((Map) siteSituation).get(BPAConstants.NO_OF_TREES);
		if (!Objects.isNull(noOfTrees) && !String.valueOf(noOfTrees).isEmpty()) {
			if (Integer.valueOf(noOfTrees.toString()) > 0) {
				log.info("Total No of trees:" + Integer.valueOf(noOfTrees.toString()));
				docTypes.forEach((k, v) -> {
					if (!v.contains(BPAConstants.NO_OF_TREES_DOCTYPE)) {
						throw new CustomException("create error", "Failed to create feild inspection report, pls upload noOfTrees image for all reports !");
					}
				});
			}
		}

		
		Object noOfRechargingPits = ((Map) siteSituation).get(BPAConstants.NO_OF_RECHARGING_PITS);
		if (!Objects.isNull(noOfRechargingPits) && !String.valueOf(noOfRechargingPits).isEmpty()) {
			if (Integer.valueOf(noOfRechargingPits.toString()) > 0) {
				log.info("Total No of Recharging Pits:" + Integer.valueOf(noOfRechargingPits.toString()));
				docTypes.forEach((k, v) -> {
					if (!v.contains(BPAConstants.NO_OF_RECHARGING_PITS_DOCTYPE)) {
						throw new CustomException("create error", "Failed to create feild inspection report, pls upload noOfRechargingPits image for all reports !");
					}
				});
			}
		}

		Object buildingSituation = fieldInspection.getBuildingSituation();
		if(!Objects.isNull(buildingSituation) && !String.valueOf(buildingSituation).isEmpty()) {
			Object noOfLifts = ((Map) (((List) buildingSituation).get(0))).get(BPAConstants.NO_OF_LIFTS);
			if (!Objects.isNull(noOfLifts) && !String.valueOf(noOfLifts).isEmpty()) {
				if (Integer.valueOf(noOfLifts.toString()) > 0) {
					log.info("Total No of noOfLifts:" + Integer.valueOf(noOfLifts.toString()));
					docTypes.forEach((k, v) -> {
						if (!v.contains(BPAConstants.NO_OF_LIFTS_DOCTYPE)) {
							throw new CustomException("create error", "Failed to create feild inspection report, pls upload noOfLifts image for all reports !");
						}
					});
				}
			}
	
			Object noOfStairCases = ((Map) (((List) buildingSituation).get(0))).get(BPAConstants.NO_OF_STAIR_CASE);
			if (!Objects.isNull(noOfStairCases) && !String.valueOf(noOfStairCases).isEmpty()) {
				if (Integer.valueOf(noOfStairCases.toString()) > 0) {
					log.info("Total No of noOfStairCases:" + Integer.valueOf(noOfStairCases.toString()));
					docTypes.forEach((k, v) -> {
						if (!v.contains(BPAConstants.NO_OF_STAIR_CASE_DOCTYPE)) {
							throw new CustomException("create error", "Failed to create feild inspection report, pls upload noOfstairCase image for all reports !");
						}
					});
				}
			}
		}
		
		Object buildingRegularizationSetback = fieldInspection.getBuildingRegularizationSetback();
		if (!Objects.isNull(buildingRegularizationSetback)
				&& !String.valueOf(buildingRegularizationSetback).isEmpty()) {
			Object setBackFront = ((Map) (((List) buildingRegularizationSetback).get(0)))
					.get(BPAConstants.SET_BACK_FRONT);
			if (Objects.isNull(setBackFront) && String.valueOf(setBackFront).isEmpty()) {

				throw new CustomException("create error", "Set Back Front is mandatory field !");

			}
		}
	   
		if (!Objects.isNull(buildingRegularizationSetback)
				&& !String.valueOf(buildingRegularizationSetback).isEmpty()) {
			Object setBackRear = ((Map) (((List) buildingRegularizationSetback).get(0)))
					.get(BPAConstants.SET_BACK_REAR);
			if (Objects.isNull(setBackRear) && String.valueOf(setBackRear).isEmpty()) {

				throw new CustomException("create error", "Set Back Rear is mandatory field !");

			}
		}
		
		if (!Objects.isNull(buildingRegularizationSetback)
				&& !String.valueOf(buildingRegularizationSetback).isEmpty()) {
			Object setBackLeft = ((Map) (((List) buildingRegularizationSetback).get(0)))
					.get(BPAConstants.SET_BACK_LEFT);
			if (Objects.isNull(setBackLeft) && String.valueOf(setBackLeft).isEmpty()) {

				throw new CustomException("create error", "Set Back Left is mandatory field !");

			}
		}
		
		if (!Objects.isNull(buildingRegularizationSetback)
				&& !String.valueOf(buildingRegularizationSetback).isEmpty()) {
			Object setBackRight = ((Map) (((List) buildingRegularizationSetback).get(0)))
					.get(BPAConstants.SET_BACK_RIGHT);
			if (Objects.isNull(setBackRight) && String.valueOf(setBackRight).isEmpty()) {

				throw new CustomException("create error", "Set Back Right is mandatory field !");

			}
		}

	}
	
	public void validateRegularizationDeletion(Regularization regularization) {

		if (!(regularization.getStatus().equalsIgnoreCase(RegularizationConstants.INITIATED)
				|| regularization.getStatus()
						.equalsIgnoreCase(RegularizationConstants.STATUS_CITIZEN_APPROVAL_INPROCESS)
				|| regularization.getStatus().equalsIgnoreCase(RegularizationConstants.INPROGRESS_STATUS)
				|| regularization.getStatus().equalsIgnoreCase(RegularizationConstants.APPL_FEE_STATE))) {
			throw new CustomException("Delete Regularization Error",
					"Application is not allowed to delete after Application Fee Payment");
		}

	}

	/**
	 * Validate all the components of FI Report here
	 * 
	 * @param fieldInspection
	 */
	public void validatefieldInspectionRequestV2(RegularizationFieldInspection fieldInspection) {
		// Validate Report Details Json
		validateReportDetails(fieldInspection);
		// Validate Site Situation Json
		validateSiteSituation(fieldInspection);
		// Validate Building Situation Json
		validateBuildingSituation(fieldInspection);
		// Validate Approch Raod Json
		validateApprochRoad(fieldInspection);
	}

	/**
	 * Validate Approch road json and all the applicable photos
	 * 
	 * @param fieldInspection
	 */
	private void validateApprochRoad(RegularizationFieldInspection fieldInspection) {

		Object approachRoad = fieldInspection.getApproachRoad();

		if (Objects.isNull(approachRoad) || approachRoad.toString().isEmpty()) {
			throw new CustomException("create error",
					"Failed to create feild inspection report, approachRoad can't be empty !");
		}

		// Validations for Road Side Drain
		Object roadSideDrain = ((Map) approachRoad).get(BPAConstants.IS_ROAD_SIDE);
		if (Objects.isNull(roadSideDrain) || roadSideDrain.toString().isEmpty())
			throw new CustomException("create error",
					"Failed to create feild inspection report, roadSideDrain can't be empty !");

		Object valueForRoadSideDrain = ((Map) roadSideDrain).get(BPAConstants.VALUE);
		if (valueForRoadSideDrain.toString().equalsIgnoreCase(BPAConstants.YES)) {
			List photos = (List) (((Map) roadSideDrain).get(BPAConstants.PHOTOS));
			List<String> docList = new ArrayList<String>();
			for (Object obj : photos) {
				Object docType = ((Map) obj).get(BPAConstants.DOCUMENTTYPE);
				if (Objects.nonNull(docType))
					docList.add(docType.toString());
			}

			if (!docList.contains(BPAConstants.ROAD_SIDE_DRAIN)) {
				throw new CustomException("create_error",
						"Failed to create feild inspection report, pls upload roadSideDrain image for all reports !");
			}
		}

	}

	/**
	 * Validate Building Situation json and all the applicable photos
	 * 
	 * @param fieldInspection
	 */
	private void validateBuildingSituation(RegularizationFieldInspection fieldInspection) {

		Object buildingSituation = fieldInspection.getBuildingSituation();

		if (Objects.isNull(buildingSituation) || buildingSituation.toString().isEmpty()) {

			throw new CustomException("create error",
					"Failed to create feild inspection report, buildingSituation can't be empty !");
		}

		Object blocks = ((Map) buildingSituation).get(BPAConstants.BLOCK);

		List block = (List) blocks;

		// Validation for No of Staircase
		for (Object o : block) {

			Object noOfStairCase = ((Map) o).get(BPAConstants.NO_OF_STAIR_CASE);
			if (Objects.isNull(noOfStairCase) || noOfStairCase.toString().isEmpty())
				throw new CustomException("create error",
						"Failed to create feild inspection report, noOfStairCase can't be empty !");

			Object valueFornoOfStairCase = ((Map) noOfStairCase).get(BPAConstants.VALUE);
			if (!valueFornoOfStairCase.toString().equalsIgnoreCase(BPAConstants.ZERO)) {
				List photos = (List) (((Map) noOfStairCase).get(BPAConstants.PHOTOS));
				List<String> docList = new ArrayList<String>();
				for (Object obj : photos) {
					Object docType = ((Map) obj).get(BPAConstants.DOCUMENTTYPE);
					if (Objects.nonNull(docType))
						docList.add(docType.toString());
				}

				if (!docList.contains(BPAConstants.NO_OF_STAIR_CASE_DOCTYPE)) {
					throw new CustomException("create_error",
							"Failed to create feild inspection report, pls upload noOfStairCase image for all reports !");
				}
			}
		}

		// Validation for noOfLifts
		for (Object o : block) {

			Object noOfLifts = ((Map) o).get(BPAConstants.NO_OF_LIFTS);
			if (Objects.isNull(noOfLifts) || noOfLifts.toString().isEmpty())
				throw new CustomException("create error",
						"Failed to create feild inspection report, noOfLifts can't be empty !");

			Object valueForNoOfLifts = ((Map) noOfLifts).get(BPAConstants.VALUE);
			if (!valueForNoOfLifts.toString().equalsIgnoreCase(BPAConstants.ZERO)) {
				List photos = (List) (((Map) noOfLifts).get(BPAConstants.PHOTOS));
				List<String> docList = new ArrayList<String>();
				for (Object obj : photos) {
					Object docType = ((Map) obj).get(BPAConstants.DOCUMENTTYPE);
					if (Objects.nonNull(docType))
						docList.add(docType.toString());
				}

				if (!docList.contains(BPAConstants.NO_OF_LIFTS_DOCTYPE)) {
					throw new CustomException("create_error",
							"Failed to create feild inspection report, pls upload noOfLifts image for all reports !");
				}
			}
		}

		// Validation for blockWisePhoto
//		for (Object o : block) {
//
//			Object blockWisePhoto = ((Map) o).get(BPAConstants.IS_BLOCK_WISE_PHOTO);
//			if (Objects.isNull(blockWisePhoto) || blockWisePhoto.toString().isEmpty())
//				throw new CustomException("create error",
//						"Failed to create feild inspection report, blockWisePhoto can't be empty !");
//
//			List blockWisePhotos = (List) blockWisePhoto;
//
//			List<String> docList = new ArrayList<String>();
//			for (Object obj : blockWisePhotos) {
//				Object docType = ((Map) obj).get(BPAConstants.DOCUMENTTYPE);
//				if (Objects.nonNull(docType))
//					docList.add(docType.toString());
//			}
//
//			if (!docList.contains(BPAConstants.BLOCK_WISE_PHOTO_DOCTYPE)) {
//				throw new CustomException("create_error",
//						"Failed to create feild inspection report, pls upload BlockWise image for all reports !");
//			}
//
//		}
	}

	/**
	 * Validate Site Situation json and all the applicable photos
	 * 
	 * @param fieldInspection
	 */
	private void validateSiteSituation(RegularizationFieldInspection fieldInspection) {
		
		Object siteSituation = fieldInspection.getSiteSituation();

		if (Objects.isNull(siteSituation) || siteSituation.toString().isEmpty()) {

			throw new CustomException("create error",
					"Failed to create feild inspection report, siteSituation can't be empty !");
		}
		
		// Validations for Water Supply
		Object waterSupply = ((Map) siteSituation).get(BPAConstants.IS_WATER_SUPPLY);
		if (Objects.isNull(waterSupply) || waterSupply.toString().isEmpty())
			throw new CustomException("create error",
					"Failed to create feild inspection report, waterSupply can't be empty !");

		Object valueForWaterSupply = ((Map) waterSupply).get(BPAConstants.VALUE);
		if (valueForWaterSupply.toString().equalsIgnoreCase(BPAConstants.YES)) {
			List photos = (List) (((Map) waterSupply).get(BPAConstants.PHOTOS));
			List<String> docList = new ArrayList<String>();
			for (Object obj : photos) {
				Object docType = ((Map) obj).get(BPAConstants.DOCUMENTTYPE);
				if (Objects.nonNull(docType))
					docList.add(docType.toString());
			}

			if (!docList.contains(BPAConstants.WATER_SUPPLY_DOCTYPE)) {
				throw new CustomException("create_error",
						"Failed to create feild inspection report, pls upload water supply image for all reports !");
			}
		}
		
		// Validations for Electricity
		Object electricity = ((Map) siteSituation).get(BPAConstants.IS_ELECTRIC_SUPPLY);
		if (Objects.isNull(electricity) || electricity.toString().isEmpty())
			throw new CustomException("create error",
					"Failed to create feild inspection report, electricity can't be empty !");

		Object valueForElectricity = ((Map) electricity).get(BPAConstants.VALUE);
		if (valueForElectricity.toString().equalsIgnoreCase(BPAConstants.YES)) {
			List photos = (List) (((Map) electricity).get(BPAConstants.PHOTOS));
			List<String> docList = new ArrayList<String>();
			for (Object obj : photos) {
				Object docType = ((Map) obj).get(BPAConstants.DOCUMENTTYPE);
				if (Objects.nonNull(docType))
					docList.add(docType.toString());
			}

			if (!docList.contains(BPAConstants.ELECTRIC_SUPPLY_DOCTYPE)) {
				throw new CustomException("create_error",
						"Failed to create feild inspection report, pls upload electricity image for all reports !");
			}
		}
		
		// Validations for drainage
		Object drainage = ((Map) siteSituation).get(BPAConstants.IS_DRAINAGE_REQUIRED);
		if (Objects.isNull(drainage) || drainage.toString().isEmpty())
			throw new CustomException("create error",
					"Failed to create feild inspection report, drainage can't be empty !");

		Object valueForDrainage = ((Map) drainage).get(BPAConstants.VALUE);
		if (valueForDrainage.toString().equalsIgnoreCase(BPAConstants.YES)) {
			List photos = (List) (((Map) drainage).get(BPAConstants.PHOTOS));
			List<String> docList = new ArrayList<String>();
			for (Object obj : photos) {
				Object docType = ((Map) obj).get(BPAConstants.DOCUMENTTYPE);
				if (Objects.nonNull(docType))
					docList.add(docType.toString());
			}

			if (!docList.contains(BPAConstants.DRAINAGE_DOCTYPE)) {
				throw new CustomException("create_error",
						"Failed to create feild inspection report, pls upload drainage image for all reports !");
			}
		}
		
		// Validations for sewrage
		Object sewrage = ((Map) siteSituation).get(BPAConstants.IS_SEWARGE_REQUIRED);
		if (Objects.isNull(sewrage) || sewrage.toString().isEmpty())
			throw new CustomException("create error",
					"Failed to create feild inspection report, sewrage can't be empty !");

		Object valueForSewrage = ((Map) sewrage).get(BPAConstants.VALUE);
		if (valueForSewrage.toString().equalsIgnoreCase(BPAConstants.YES)) {
			List photos = (List) (((Map) sewrage).get(BPAConstants.PHOTOS));
			List<String> docList = new ArrayList<String>();
			for (Object obj : photos) {
				Object docType = ((Map) obj).get(BPAConstants.DOCUMENTTYPE);
				if (Objects.nonNull(docType))
					docList.add(docType.toString());
			}

			if (!docList.contains(BPAConstants.SEWARGE_DOCTYPE)) {
				throw new CustomException("create_error",
						"Failed to create feild inspection report, pls upload sewrage image for all reports !");
			}
		}
		
		// Validations for rainwaterharvesting
		Object rainwaterharvesting = ((Map) siteSituation).get(BPAConstants.IS_RAINWATER_HARVESTING_REQUIRED);
		if (Objects.isNull(rainwaterharvesting) || rainwaterharvesting.toString().isEmpty())
			throw new CustomException("create error",
					"Failed to create feild inspection report, rainwaterharvesting can't be empty !");

		Object valueForRainWaterHarvesting = ((Map) rainwaterharvesting).get(BPAConstants.VALUE);
		if (valueForRainWaterHarvesting.toString().equalsIgnoreCase(BPAConstants.YES)) {
			List photos = (List) (((Map) rainwaterharvesting).get(BPAConstants.PHOTOS));
			List<String> docList = new ArrayList<String>();
			for (Object obj : photos) {
				Object docType = ((Map) obj).get(BPAConstants.DOCUMENTTYPE);
				if (Objects.nonNull(docType))
					docList.add(docType.toString());
			}

			if (!docList.contains(BPAConstants.RAINWATER_HARVESTING_DOCTYPE)) {
				throw new CustomException("create_error",
						"Failed to create feild inspection report, pls upload rainwaterharvesting image for all reports !");
			}
		}
		
		// Validations for noOfTrees
		Object noOfTrees = ((Map) siteSituation).get(BPAConstants.NO_OF_TREES);
		if (Objects.isNull(noOfTrees) || noOfTrees.toString().isEmpty())
			throw new CustomException("create error",
					"Failed to create feild inspection report, noOfTrees can't be empty !");

		Object valueForNoOfTrees = ((Map) noOfTrees).get(BPAConstants.VALUE);
		if (valueForNoOfTrees.toString().equalsIgnoreCase(BPAConstants.YES)) {
			List photos = (List) (((Map) noOfTrees).get(BPAConstants.PHOTOS));
			List<String> docList = new ArrayList<String>();
			for (Object obj : photos) {
				Object docType = ((Map) obj).get(BPAConstants.DOCUMENTTYPE);
				if (Objects.nonNull(docType))
					docList.add(docType.toString());
			}

			if (!docList.contains(BPAConstants.NO_OF_TREES_DOCTYPE)) {
				throw new CustomException("create_error",
						"Failed to create feild inspection report, pls upload noOfTrees image for all reports !");
			}
		}
		
		// Validations for numberofRechargingPits
		Object numberofRechargingPits = ((Map) siteSituation).get(BPAConstants.NO_OF_RECHARGING_PITS);
		if (Objects.isNull(numberofRechargingPits) || numberofRechargingPits.toString().isEmpty())
			throw new CustomException("create error",
					"Failed to create feild inspection report, numberofRechargingPits can't be empty !");

		Object valueForNumberofRechargingPits = ((Map) numberofRechargingPits).get(BPAConstants.VALUE);
		if (valueForNumberofRechargingPits.toString().equalsIgnoreCase(BPAConstants.YES)) {
			List photos = (List) (((Map) numberofRechargingPits).get(BPAConstants.PHOTOS));
			List<String> docList = new ArrayList<String>();
			for (Object obj : photos) {
				Object docType = ((Map) obj).get(BPAConstants.DOCUMENTTYPE);
				if (Objects.nonNull(docType))
					docList.add(docType.toString());
			}

			if (!docList.contains(BPAConstants.NO_OF_RECHARGING_PITS)) {
				throw new CustomException("create_error",
						"Failed to create feild inspection report, pls upload valueForNumberofRechargingPits image for all reports !");
			}
		}	
	}

	/**
	 * Validate Report Details json and all the applicable photos
	 * 
	 * @param fieldInspection
	 */
	private void validateReportDetails(RegularizationFieldInspection fieldInspection) {

		Object reportDetails = fieldInspection.getReportDetails();

		if (Objects.isNull(reportDetails) || reportDetails.toString().isEmpty()) {

			throw new CustomException("create error",
					"Failed to create feild inspection report, reportdetails can't be empty !");
		}

		List report = (List) reportDetails;

		for (Object o : report) {
			// photos will be there
			List photos = (List) (((Map) o).get(BPAConstants.PHOTOS));
			List docs = (List) (((Map) o).get(BPAConstants.DOCS));
			if (Objects.nonNull(photos)) {
				List<String> docList = new ArrayList<String>();
				for (Object obj : photos) {
					Object docType = ((Map) obj).get(BPAConstants.DOCUMENTTYPE);
					if (Objects.nonNull(docType))
						docList.add(docType.toString());
				}

				if (!docList.contains("FI.SIPU.SIPU")) {
					throw new CustomException("create_error", "Site Inspection Report Missing...");
				}
			}

			if (Objects.nonNull(docs)) {
				List<String> docList = new ArrayList<String>();
				for (Object obj : docs) {
					Object docType = ((Map) obj).get(BPAConstants.DOCUMENTTYPE);
					if (Objects.nonNull(docType))
						docList.add(docType.toString());
				}

				if (!docList.contains("FI.FIR.FIR")) {
					throw new CustomException("create_error", "Field Inspection Report Missing...");
				}
			}
		}
	}

	/**
	 * Validator of Village Search Request
	 * 
	 * @param criteria
	 */
	public void validateVillageSearchRequest(@Valid VillageSearchCriteria criteria) {

		// Don't Allow the request to process, if Application Number is missing
		if (CollectionUtils.isEmpty(criteria.getApplicationNos())) {

			throw new CustomException("INVALID_SEARCH_PARAM",
					"Kindly provide Application Number to Search the Villages !!");
		}

	}

	/**
	 * Validate Search Request for Applications Pending at Sanc Fee Status
	 * 
	 * @param criteria
	 */
	public void validateSearchFeePendingRequest(@Valid BPASearchCriteria criteria) {

		if (criteria.isEmpty()) {
			throw new CustomException("search_error", "Search Criteria cannot be empty !!!");
		}

		if (criteria.getTenantId().isEmpty() || criteria.getApprovedBy().isEmpty()) {

			throw new CustomException("search_error", "TenantId or Approved By cannot be empty !!!");
		}

	}

	/**
	 * Validate if payment Received for Pull Back Reuquest
	 * 
	 * @param request
	 * @return
	 */
	public Boolean validateIfPaymentReceived(RegularizationRequest request) {
		
		Boolean isPaymentReceived = Boolean.FALSE;

		String tenantId = request.getRegularization().getTenantId();

		String applicationNumber = request.getRegularization().getApplicationNo();

		PaymentSearchCriteria paymentSearchCriteria = PaymentSearchCriteria.builder().consumerCode(applicationNumber)
				.businessService(IssueFixConstants.REG_SAN_FEE).tenantId(tenantId).build();
		
		List<Payment> payments = repository.getPayments(paymentSearchCriteria);

		if (payments.size() >= 1) {
			isPaymentReceived = Boolean.TRUE;
			throw new CustomException("PAYMENT_ISSUE", "Sanc fee payment has been done for application no :" + applicationNumber + " Can't step back !");
		}

		log.info("Sanc Fee payment has not been done.. Checking other details !!");

		return isPaymentReceived;
	}

	/**
	 * Validate the demand to be deleted for Pull Back Request
	 * 
	 * @param request
	 * @param isDataUpdateNeeded
	 * @return
	 */
	public Demand validateDemandToBeDeleted(RegularizationRequest request, Map<String, Boolean> isDataUpdateNeeded) {

		Demand demand = new Demand();

		String tenantId = request.getRegularization().getTenantId();

		String applicationNumber = request.getRegularization().getApplicationNo();

		DemandSearchCriteria demandSearchCriteria = DemandSearchCriteria.builder().consumerCode(applicationNumber)
				.businessService(IssueFixConstants.REG_SAN_FEE).tenantId(tenantId).build();

		// Search demand here
		List<Demand> demands = repository.getDemands(demandSearchCriteria);
		if (demands.size() > 1) {
			throw new CustomException("MULTIPLE_DEMNADS_FOUND",
					"Multiple Sanction Fee demand found for application no :" + applicationNumber);
		} else if (!CollectionUtils.isEmpty(demands)) {
			if (demands.get(0).getIsPaymentCompleted().equals(Boolean.TRUE)) {
				throw new CustomException("PAYMENT_DONE_FOR_DEMAND",
						"Payment has been done for Sanction Fee demand application no :" + applicationNumber);
			} else {
				demand = demands.get(0);
				isDataUpdateNeeded.put(IssueFixConstants.IS_DEMAND_DELETE_NEEDED, true);
			}
		}
		return demand;
	}

	/**
	 * Validate the DSC details to be deleted for Pull Back Request
	 * 
	 * @param request
	 * @return
	 */
	public RegularizationDscDetails validateIfDscToBeDeleted(RegularizationRequest request,
			Map<String, Boolean> isDataUpdateNeeded) {

		RegularizationSearchCriteria searchCriteria = RegularizationSearchCriteria.builder()
				.tenantId(request.getRegularization().getTenantId())
				.applicationNo(request.getRegularization().getApplicationNo()).build();

		RegularizationDscDetails dscDetails = new RegularizationDscDetails();
		List<RegularizationDscDetails> regularizationDscDetails = repository
				.searchRegularizationDscDetails(searchCriteria, request.getRequestInfo());

		if (!CollectionUtils.isEmpty(regularizationDscDetails)) {
			dscDetails = regularizationDscDetails.get(0);
			isDataUpdateNeeded.put(IssueFixConstants.IS_DSC_DELETE_NEEDED, true);

		}
		return dscDetails;
	}

	public void validateDocUploadRequest(RegularizationDocUpload docUploadRequest) {

		log.info("Validating Document Upload Request");

		if (!org.springframework.util.StringUtils.hasText(docUploadRequest.getTenantId())) {
			throw new CustomException(BPAErrorConstants.INVALID_TENANT_ID_MDMS_KEY, "TenantID is Mandatory ");
		}

		if (!org.springframework.util.StringUtils.hasText(docUploadRequest.getApplicationNo())) {
			throw new CustomException(BPAErrorConstants.INVALID_APPLICATION_NO_KEY,
					"Regularization Application No is Mandatory ");
		}

		if (CollectionUtils.isEmpty(docUploadRequest.getDocuments())) {
			throw new CustomException(BPAErrorConstants.INVALID_DOCUMENTS_KEY, "Documents are Mandatory ");
		}

		if (!org.springframework.util.StringUtils.hasText(docUploadRequest.getDocUploadType())) {
			throw new CustomException(BPAErrorConstants.INVALID_DOCUMENTTYPE_KEY,
					"Regularization Document Upload Type is Mandatory ");
		}

	}

	public void validateDocUpload(RegularizationDocUpload docUploadRequest, List<Regularization> applications,
			Object mdmsData, String currentState) {
		Regularization application = applications.get(0);
		validateIfDocsAllowed(docUploadRequest, application,mdmsData);
		validateDuplicateDocs(docUploadRequest);
	}

	private void validateDuplicateDocs(RegularizationDocUpload docUploadRequest) {
		if (docUploadRequest.getDocuments() != null) {
			List<String> documentFileStoreIds = new LinkedList<String>();
			List<String> documentTypes = new LinkedList<String>();
			docUploadRequest.getDocuments().forEach(document -> {
				if (documentFileStoreIds.contains(document.getFileStoreId())
						|| documentTypes.contains(document.getDocumentType())
								&& !document.getDocumentType().equals("ADDL.ADDLDOCUMENTS"))
					throw new CustomException(BPAErrorConstants.BPA_DUPLICATE_DOCUMENT,
							"Same document cannot be used multiple times");
				else {
					documentFileStoreIds.add(document.getFileStoreId());
					documentTypes.add(document.getDocumentType());
				}
			});
		}
	}

	private void validateIfDocsAllowed(RegularizationDocUpload docUploadRequest, Regularization application,
			Object mdmsData) {
		
		List<Document> existingDocuments = application.getDocuments();
		Set<String> existingDocTypes = existingDocuments.stream().map(Document::getDocumentType)
				.collect(Collectors.toSet());
		Set<String> newDocTypes = docUploadRequest.getDocuments().stream().map(document -> document.getDocumentType())
				.collect(Collectors.toSet());
		Map<String, List<String>> masterData = util.getAttributeValues(mdmsData);
		Set<String> allowedDocumentTypes = masterData.get("DocumentType").stream()
				//.filter(docType -> docType.startsWith("NOC"))
				.collect(Collectors.toSet());

		List<String> invalidDocuments = newDocTypes.stream().filter(Doc -> !allowedDocumentTypes.contains(Doc))
				.collect(Collectors.toList());

		if (!CollectionUtils.isEmpty(invalidDocuments)) {
			throw new CustomException(BPAErrorConstants.BPA_INVALID_DOCUMENTTYPE,
					"Invalid Document Type for " + invalidDocuments);
		}
		
		Set<String> newDocTypesForUpload = docUploadRequest.getDocuments().stream().filter(document -> !org.springframework.util.StringUtils.hasText(document.getId())).map(document -> document.getDocumentType())
				.collect(Collectors.toSet());

		boolean hasCommonDocs = newDocTypesForUpload.stream().filter(Doc -> !Doc.equalsIgnoreCase("ADDL.ADDLDOCUMENTS")).anyMatch(existingDocTypes::contains);
		if (hasCommonDocs) {
			throw new CustomException(BPAErrorConstants.BPA_DUPLICATE_DOCUMENT,
					"One or All of the documents already uploaded");
		}
		
	}

	public void validatePullBackAction(@Valid RegularizationRequest regularizationRequest) {

		Regularization application = regularizationRequest.getRegularization();
		if (application.getWorkflow().getAction().equalsIgnoreCase(BPAConstants.ACTION_PULL_BACK)) {
			String uuid = regularizationRequest.getRequestInfo().getUserInfo().getUuid();
			if (!application.getDscDetails().get(0).getApprovedBy().equalsIgnoreCase(uuid)) {
				throw new CustomException("UNAUTHORISED_ACTION", "User is unauthorised to perform this action.");
			}
			


			List<ProcessInstance> processInstances =
			        workflowService.getProcessInstances(application, regularizationRequest.getRequestInfo(), true);
			
			long createdTime = processInstances.get(0).getAuditDetails().getCreatedTime();
			log.info("last process instance createdTime is {}",createdTime);
				
			long count = processInstances.stream()
			        .filter(p -> BPAConstants.ACTION_PULL_BACK.equals(p.getAction()))
			        .count();
			log.info("total pull back count for the application is {}",count);
			// Current time in millis
			
		/*	if(!BPAConstants.ACTION_APPROVE.equalsIgnoreCase(processInstances.get(0).getAction())) {
				throw new CustomException("INVALID_STATE","Application is not in approve state.");
			} */
			
			long currentTime = System.currentTimeMillis();

			// 30 days in milliseconds
			long thirtyDaysInMillis = 30L * 24 * 60 * 60 * 1000;

			// Check if older than 30 days
			if(count>=config.getMaxCountPullBackActionLimit()) {
				if(currentTime - createdTime <= thirtyDaysInMillis ) {
				throw new CustomException( "BPA_INVALID_PULL_BACK_ACTION",
					    "Cannot withdraw the application. Pull-back action has already been performed " +count + " times. Withdrawal is allowed only once in 30 days.");
				}
			}
		}
	}

	public void validateShowCauseRefusal(@Valid RegularizationRequest regularizationRequest) {
		Regularization regularization = regularizationRequest.getRegularization();
		if (config.getBpaRefusalShowCauseNoticeEnable()) {
			if (regularization.getWorkflow().getAction().equalsIgnoreCase(BPAConstants.ACTION_APPROVE)
					&& regularization.getStatus().equalsIgnoreCase(BPAConstants.APPROVAL_INPROGRESS)) {

				String applicationNo = regularization.getApplicationNo();
				NoticeSearchCriteria noticeSearchCriteria = NoticeSearchCriteria.builder().businessid(applicationNo)
						.letterType(BPAConstants.REFUSAL_SHOWCAUSE_LETTER_TYPE).build();

				List<Notice> notices = scnRepository.getNoticeDataForRegularization(noticeSearchCriteria);

				if (!CollectionUtils.isEmpty(notices)) {
					throw new CustomException("REFUSAL_SCN_EXIST", String.format(
							"A refusal SCN already exists for Business ID: %s and Letter Number: %s. Approval of the application is not possible.",
							applicationNo, notices.get(0).getLetterNo()));

				}
			}

			if (BPAConstants.ACTION_REJECT.equalsIgnoreCase(regularization.getWorkflow().getAction())) {
				String applicationNo = regularization.getApplicationNo();
				NoticeSearchCriteria noticeSearchCriteria = NoticeSearchCriteria.builder().businessid(applicationNo)
						.letterType(BPAConstants.REFUSAL_SHOWCAUSE_LETTER_TYPE).build();

				List<Notice> notices = scnRepository.getNoticeDataForRegularization(noticeSearchCriteria);

				if (CollectionUtils.isEmpty(notices)) {
					throw new CustomException("NOTICE_NOT_EXIST", String.format(
							"Refusal SCN not exists for business ID: %s. Create Refusal Show Cause Notice before Rejecting the Application",
							applicationNo));
				}

			}
		}
	}

	public void validateSaveDraft(@Valid RegularizationDraftRequest request) {
		RequestInfo requestInfo = request.getRequestInfo();
		String tenantId = request.getRegularizationDraft().getTenantId();
		RegularizationDraft draft = request.getRegularizationDraft();

		if (tenantId.split("\\.").length == 1) {
			throw new CustomException(BPAErrorConstants.INVALID_TENANT, "Draft cannot be saved at StateLevel");
		}

		if (ObjectUtils.isEmpty(draft)) {
			throw new CustomException(BPAErrorConstants.INVALID_REQUEST, "Regularization Draft object can't be null or empty.");
		}

	}

	public void validateDraftSearch(RequestInfo requestInfo, @Valid RegularizationDraftSearchCriteria criteria) {
		if (StringUtils.isEmpty(criteria.getDraftNo()) && StringUtils.isEmpty(criteria.getTenantId())) {
			throw new CustomException(BPAErrorConstants.INVALID_SEARCH,
					"Either Draft Number or tenant id is mandatory for BPA Draft Search.");
		}
		
	}

	public void validateRegularizationUpdateRequest(@Valid RegularizationRequest regularizationRequest) {

		// Validate Regularization FAR Status
		Regularization regularization = regularizationRequest.getRegularization();
		String farStatus = null;
		if (regularization.getAppType().equals(AppType.BUILDING)
				|| regularization.getAppType().equals(AppType.LAND_AND_BUILDING)) {
			farStatus = regularization.getBuildingRegularizationInfo().getFarDetails().getFarStatus();
			if (!StringUtils.isEmpty(farStatus) && farStatus.equalsIgnoreCase("Rejected")) {
				throw new CustomException("INVALID_FAR_STATUS",
						"FAR Status is Rejected for Building and Land & Building Regularization Application. Cannot forward the application.");
			}
		}

	}
	
	public void validateDuplicateDocs(RegularizationRequest regularizationRequest) {
		Regularization regularization = regularizationRequest.getRegularization();
		if (regularization.getDocuments() != null) {
			List<String> documentFileStoreIds = new LinkedList<String>();
			// List<String> documentTypes = new LinkedList<String>();
			regularization.getDocuments().forEach(document -> {
				if (documentFileStoreIds.contains(document.getFileStoreId())
						&& !document.getDocumentType().equals("ADDL.ADDLDOCUMENTS"))
					throw new CustomException(BPAErrorConstants.BPA_DUPLICATE_DOCUMENT,
							"Same Filestore ID cannot be used multiple times");
				else {
					documentFileStoreIds.add(document.getFileStoreId());
					// documentTypes.add(document.getDocumentType());
				}
			});
		}
	}
	
	
}
