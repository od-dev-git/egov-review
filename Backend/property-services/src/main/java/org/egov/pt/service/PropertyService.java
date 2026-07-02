package org.egov.pt.service;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.validation.Valid;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.egov.common.contract.request.RequestInfo;
import org.egov.pt.config.PropertyConfiguration;
import org.egov.pt.models.*;
import org.egov.pt.models.Assessment.Source;
import org.egov.pt.models.enums.CreationReason;
import org.egov.pt.models.enums.Status;
import org.egov.pt.models.user.UserDetailResponse;
import org.egov.pt.models.user.UserSearchRequest;
import org.egov.pt.models.workflow.State;
import org.egov.pt.producer.Producer;
import org.egov.pt.repository.PropertyRepository;
import org.egov.pt.util.CommonUtils;
import org.egov.pt.util.PTConstants;
import org.egov.pt.util.PropertyUtil;
import org.egov.pt.validator.PropertyValidator;
import org.egov.pt.web.contracts.*;
import org.egov.tracer.model.CustomException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Sets;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class PropertyService {

	@Autowired
	private Producer producer;

	@Autowired
	private NotificationService notifService;
	
	@Autowired
	private PropertyConfiguration config;

	@Autowired
	private PropertyRepository repository;

	@Autowired
	private EnrichmentService enrichmentService;

	@Autowired
	private PropertyValidator propertyValidator;

	@Autowired
	private UserService userService;

	@Autowired
	private WorkflowService wfService;

	@Autowired
	private PropertyUtil util;

	@Autowired
	private ObjectMapper mapper;

	@Autowired
	private CalculationService calculatorService;
	
	@Autowired
	@Lazy
	private AssessmentService assessmentService;
	
	private static String allowedNameRegex = "^[a-zA-Z0-9 \\-'`\\.]*$"; 
	
	/**
	 * Enriches the Request and pushes to the Queue
	 *
	 * @param request PropertyRequest containing list of properties to be created
	 * @return List of properties successfully created
	 */
	public Property createProperty(PropertyRequest request) {

		propertyValidator.validateCreateRequest(request);
		enrichmentService.enrichCreateRequest(request);
		userService.createUser(request);
		if (config.getIsWorkflowEnabled()
				&& !request.getProperty().getCreationReason().equals(CreationReason.DATA_UPLOAD)) {
			wfService.updateWorkflow(request, request.getProperty().getCreationReason());

		} else {

			request.getProperty().setStatus(Status.ACTIVE);
		}

		producer.push(config.getSavePropertyTopic(), request);
		request.getProperty().setWorkflow(null);
		return request.getProperty();
	}

	/**
	 * Updates the property
	 *
	 * handles multiple processes 
	 *
	 * Update
	 *
	 * Mutation
	 *
	 * @param request PropertyRequest containing list of properties to be update
	 * @return List of updated properties
	 */
	public Property updateProperty(PropertyRequest request, boolean isUpdateWithoutWF) {
		
		boolean isRequestForUpdateDataFields = request.getProperty().getIsUpdateDataFieldRequest()==null?false:request.getProperty().getIsUpdateDataFieldRequest();
		 if(isRequestForUpdateDataFields){
				processUpdateDataFieldRequest(request);
				return request.getProperty();
			}
		assessmentService.validateAssessment(request.getProperty().getAdditionalDetails());
		Property propertyFromSearch = propertyValidator.validateCommonUpdateInformation(request);

		boolean isRequestForOwnerMutation = CreationReason.MUTATION.equals(request.getProperty().getCreationReason());
		boolean isLeacyApplicationMobileLink = CreationReason.LINK.equals(request.getProperty().getCreationReason());
//		boolean isNumberDifferent = checkIsRequestForMobileNumberUpdate(request, propertyFromSearch);
		boolean isRequestForPropertyDeactivation = CreationReason.DEACTIVATE.equals(request.getProperty().getCreationReason());
		boolean isRequestForPropertyReactivation = CreationReason.REACTIVATE.equals(request.getProperty().getCreationReason());
		
		if(isLeacyApplicationMobileLink) {
			linkMobileWithProperty(request, propertyFromSearch);
	//	}else if(isNumberDifferent) {
	//		processMobileNumberUpdate(request, propertyFromSearch);
	//		log.info(" processMobileNumberUpdate ");
		}else if (isRequestForOwnerMutation) {
			processOwnerMutation(request, propertyFromSearch);
		}else if (isRequestForPropertyDeactivation) {
			processActivationRequest(request, propertyFromSearch);
		}else if (isRequestForPropertyReactivation) {
			processActivationRequest(request, propertyFromSearch);
		} else {
			processPropertyUpdate(request, propertyFromSearch, isUpdateWithoutWF);
		}

		request.getProperty().setWorkflow(null);
		return request.getProperty();
	}

		/*
		Method to check if the update request is for updating owner mobile numbers
	*/
	
	private boolean checkIsRequestForMobileNumberUpdate(PropertyRequest request, Property propertyFromSearch) {
		Map <String, String> uuidToMobileNumber = new HashMap <String, String>();
		Map <String, String> uuidToOwnerName = new HashMap <String, String>();
		List <OwnerInfo> owners = propertyFromSearch.getOwners();
		
		if(Objects.isNull(owners) || owners.isEmpty()) {
			return false;
		}
		
		for(OwnerInfo owner : owners) {
			uuidToMobileNumber.put(owner.getUuid(), owner.getMobileNumber());
			uuidToOwnerName.put(owner.getUuid(), owner.getName());
		}
		
		List <OwnerInfo> ownersFromRequest = request.getProperty().getOwners();
		
		Boolean isNameNumberDifferent = false;
		
		for(OwnerInfo owner : ownersFromRequest) {
			if(uuidToMobileNumber.containsKey(owner.getUuid()) 
					&& ( (!Objects.isNull(owner.getMobileNumber()) && Objects.isNull(uuidToMobileNumber.get(owner.getUuid())))
							|| (!Objects.isNull(owner.getMobileNumber()) && !uuidToMobileNumber.get(owner.getUuid()).equals(owner.getMobileNumber()))
						|| (!Objects.isNull(owner.getName()) && Objects.isNull(uuidToOwnerName.get(owner.getUuid())) )
							|| (!Objects.isNull(owner.getName()) && !uuidToOwnerName.get(owner.getUuid()).equals(owner.getName()) )
						)
			) {
				isNameNumberDifferent = true;
				break;
			}
		}
		
		return isNameNumberDifferent;
	}
	
	/*
		Method to process owner mobile number update
	*/
	
	private void processMobileNumberUpdate(PropertyRequest request, Property propertyFromSearch) {
		
				if (CreationReason.CREATE.equals(request.getProperty().getCreationReason())) {
					userService.createUser(request);
				} else {			
					log.info("updating Owner Mobile Numbers");
					updateOwnerMobileNumbers(request,propertyFromSearch);
				}
				
				enrichmentService.enrichUpdateRequest(request, propertyFromSearch);
				util.mergeAdditionalDetails(request, propertyFromSearch);
				producer.push(config.getUpdatePropertyTopic(), request);		
	}
	
	private void linkMobileWithProperty(PropertyRequest request, Property propertyFromSearch) {
		userService.updateUserForPropertyLink(request);
	}
	
	/*
	Method to update owners mobile number
	*/
	private void updateOwnerMobileNumbers(PropertyRequest request, Property propertyFromSearch) {
		
		
		Map <String, String> uuidToMobileNumber = new HashMap <String, String>();
		List <OwnerInfo> owners = propertyFromSearch.getOwners();
		
		for(OwnerInfo owner : owners) {
			uuidToMobileNumber.put(owner.getUuid(), owner.getMobileNumber());
		}
		
		userService.updateUserMobileNumber(request, uuidToMobileNumber);
		notifService.sendNotificationForMobileNumberUpdate(request, propertyFromSearch,uuidToMobileNumber);						
	}

	/**
	 * Method to process Property update 
	 *
	 * @param request
	 * @param propertyFromSearch
	 */
	private void processPropertyUpdate(PropertyRequest request, Property propertyFromSearch, boolean isUpdateWithoutWF) {

		if (!isUpdateWithoutWF)
			propertyValidator.validateRequestForUpdate(request, propertyFromSearch);
		
		State currentState = wfService.getCurrentState(request.getRequestInfo(), request.getProperty().getTenantId(),
				request.getProperty().getAcknowldgementNumber());
		if (CreationReason.CREATE.equals(request.getProperty().getCreationReason()) && 	PTConstants.WF_STATUS_SENT_BACK_TO_CITIZEN.equalsIgnoreCase(currentState.getState())) {
			
	        Property property = request.getProperty();
			List<OwnerInfo> owners = property.getOwners();
			
			
			if(!CollectionUtils.isEmpty(owners)) {
				String uuid = owners.get(0).getUuid();// during direct REsubmit from CItizen during SENT BACK action, new owner info is not sent
				if ( StringUtils.isEmpty(uuid) ) {
					
					userService.createUser(request);
					
					// setting existing pt owners inactive
					log.info("@Class : PropertyService @Method : processPropertyUpdate @Message : Setting Old Users Inactive @CurrentApplicationState : " +  currentState );
			        if(!ObjectUtils.isEmpty(propertyFromSearch) && !ObjectUtils.isEmpty(propertyFromSearch.getOwners())) {
			        	List<OwnerInfo> existingOwners = propertyFromSearch.getOwners().stream().filter(Owner -> Owner.getStatus().equals(Status.ACTIVE)).collect(Collectors.toList());
			        	for(OwnerInfo existingOwner : existingOwners) {
			        		existingOwner.setStatus(Status.INACTIVE);
			        		request.getProperty().getOwners().add(existingOwner);
			        	}
			        }
					
				}
			}
   
		} else {
			//request.getProperty().setOwners(util.getCopyOfOwners(propertyFromSearch.getOwners()));
				log.info("@Class : PropertyService @Method : processPropertyUpdate @Message : Old Migrated Property for User Info Update : " +  request.getProperty().getOldPropertyId() );
			List<OwnerInfo> owners = util.getCopyOfOwners(propertyFromSearch.getOwners());
			owners.forEach(owner -> {
				Optional<OwnerInfo> ownerInfoOpt = request.getProperty().getOwners().stream().filter(reqOwner -> owner.getUuid().equals(reqOwner.getUuid())).findFirst();
				if(ownerInfoOpt.isPresent()) {
					owner.setName(removeSpecialIfPresent(owner.getName()));
					owner.setGender(ownerInfoOpt.get().getGender());
					owner.setFatherOrHusbandName(ownerInfoOpt.get().getFatherOrHusbandName());
					owner.setRelationship(ownerInfoOpt.get().getRelationship());
					owner.setOwnerType(ownerInfoOpt.get().getOwnerType());
					owner.setPermanentAddress(ownerInfoOpt.get().getPermanentAddress());
					owner.setEmailId(ownerInfoOpt.get().getEmailId());
				}
			});
			request.getProperty().setOwners(owners);
			userService.createUser(request);
		}

		if (isUpdateWithoutWF) {
			enrichmentService.enrichAuditDetails(request, propertyFromSearch);	
		} else {
			enrichmentService.enrichAssignes(request.getProperty());
			enrichmentService.enrichUpdateRequest(request, propertyFromSearch);
		}

		PropertyRequest OldPropertyRequest = PropertyRequest.builder()
				.requestInfo(request.getRequestInfo())
				.property(propertyFromSearch)
				.build();

		util.mergeAdditionalDetails(request, propertyFromSearch);

		if(config.getIsWorkflowEnabled() && !isUpdateWithoutWF) {

			// Checking for financialYear in MasterData
			String assessmentYear = CommonUtils.getFinancialYear();
			//assessmentService.validateAssessment(request, assessmentYear);
			
			State state = wfService.updateWorkflow(request, CreationReason.UPDATE);

			if (state.getIsStartState() == true
					&& state.getApplicationStatus().equalsIgnoreCase(Status.INWORKFLOW.toString())
					&& !propertyFromSearch.getStatus().equals(Status.INWORKFLOW)) {
				propertyValidator.validateExistingApplicationInWorkflow(request);

				propertyFromSearch.setStatus(Status.INACTIVE);
				producer.push(config.getUpdatePropertyTopic(), OldPropertyRequest);
				util.saveOldUuidToRequest(request, propertyFromSearch.getId());
				producer.push(config.getSavePropertyTopic(), request);

			} else if (state.getIsTerminateState()
					&& !state.getApplicationStatus().equalsIgnoreCase(Status.ACTIVE.toString())) {

				terminateWorkflowAndReInstatePreviousRecord(request, propertyFromSearch);
			}else {
				/*
				 * If property is In Workflow then continue
				 */
				producer.push(config.getUpdatePropertyTopic(), request);
				
				// If last state and property approve then trigger assessment
				if(state.getState().equalsIgnoreCase("APPROVED") && state.getApplicationStatus().equalsIgnoreCase(Status.ACTIVE.toString())) {
					try {
						Thread.sleep(5000);
					} catch (InterruptedException e) { }
					AssessmentRequest assessmentRequest = prepareAssessmentRequest(request, assessmentYear);
					assessmentService.createAssessment(assessmentRequest, true);
				}
			}

		} else {

			/*
			 * If no workflow then update property directly with mutation information
			 */
			producer.push(config.getUpdatePropertyTopic(), request);
		}
	}

	private String removeSpecialIfPresent(String name) {
		if(StringUtils.hasText(name) && !name.matches(allowedNameRegex)) {
			return name.replaceAll("[^a-zA-Z0-9 \\-'`\\.]", "");
		}
		return name;
	}

	private AssessmentRequest prepareAssessmentRequest(PropertyRequest request, String financialYear) {
		return AssessmentRequest.builder()
						.assessment(Assessment.builder()
								.tenantId(request.getProperty().getTenantId())
								.propertyId(request.getProperty().getPropertyId())
								.source(Source.MUNICIPAL_RECORDS)
								.channel(request.getProperty().getChannel())
								.assessmentDate((new Date()).getTime())
								.financialYear(financialYear).build())
						.requestInfo(request.getRequestInfo()).build();
	}

	/**
	 * method to process owner mutation
	 *
	 * @param request
	 * @param propertyFromSearch
	 */
	private void processOwnerMutation(PropertyRequest request, Property propertyFromSearch) {

		propertyValidator.validateMutation(request, propertyFromSearch);
		userService.createUserForMutation(request, !propertyFromSearch.getStatus().equals(Status.INWORKFLOW));
		enrichmentService.enrichAssignes(request.getProperty());
		enrichmentService.enrichMutationRequest(request, propertyFromSearch);
		

		// TODO FIX ME block property changes FIXME
		util.mergeAdditionalDetails(request, propertyFromSearch);
		PropertyRequest oldPropertyRequest = PropertyRequest.builder()
				.requestInfo(request.getRequestInfo())
				.property(propertyFromSearch)
				.build();

		if (config.getIsMutationWorkflowEnabled()) {
			
			/*
			 * updating property request 
			 *
			 * removing assignes during APPROVE Action to allows citizens also to Pay
			 */
			if(request.getProperty().getWorkflow().getAction().equalsIgnoreCase("APPROVE")) {
				request.getProperty().getWorkflow().setAssignes(Collections.emptyList());
			}

			State state = wfService.updateWorkflow(request, CreationReason.MUTATION);

			/*
			 * updating property from search to INACTIVE status
			 *
			 * to create new entry for new Mutation
			 */
			if (state.getIsStartState() == true
					&& state.getApplicationStatus().equalsIgnoreCase(Status.INWORKFLOW.toString())
					&& !propertyFromSearch.getStatus().equals(Status.INWORKFLOW)) {
				propertyValidator.validateExistingApplicationInWorkflow(request);

				propertyFromSearch.setStatus(Status.INACTIVE);
				producer.push(config.getUpdatePropertyTopic(), oldPropertyRequest);

				util.saveOldUuidToRequest(request, propertyFromSearch.getId());
				/* save new record */
				producer.push(config.getSavePropertyTopic(), request);

			} else if (state.getIsTerminateState()
					&& !state.getApplicationStatus().equalsIgnoreCase(Status.ACTIVE.toString())) {

				terminateWorkflowAndReInstatePreviousRecord(request, propertyFromSearch);
			} else {
				/*
				 * If property is In Workflow then continue
				 */
				if(state.getState().contains("APPROVE")) {
					calculatorService.calculateMutationFee(request.getRequestInfo(), request.getProperty());
				}
				producer.push(config.getUpdatePropertyTopic(), request);
			}

		} else {

			/*
			 * If no workflow then update property directly with mutation information
			 */
			calculatorService.calculateMutationFee(request.getRequestInfo(), request.getProperty());
			producer.push(config.getUpdatePropertyTopic(), request);
		}
	}

	private void terminateWorkflowAndReInstatePreviousRecord(PropertyRequest request, Property propertyFromSearch) {

		/* current record being rejected */
		producer.push(config.getUpdatePropertyTopic(), request);

		/* Previous record set to ACTIVE */
		@SuppressWarnings("unchecked")
		Map<String, Object> additionalDetails = mapper.convertValue(propertyFromSearch.getAdditionalDetails(), Map.class);
		if(null == additionalDetails)
			return;

		String propertyUuId = (String) additionalDetails.get(PTConstants.PREVIOUS_PROPERTY_PREVIOUD_UUID);
		if(StringUtils.isEmpty(propertyUuId))
			return;

		PropertyCriteria criteria = PropertyCriteria.builder().uuids(Sets.newHashSet(propertyUuId))
				.tenantId(propertyFromSearch.getTenantId()).build();
		Property previousPropertyToBeReInstated = searchProperty(criteria, request.getRequestInfo()).get(0);
		previousPropertyToBeReInstated.setAuditDetails(util.getAuditDetails(request.getRequestInfo().getUserInfo().getUuid().toString(), true));
		previousPropertyToBeReInstated.setStatus(Status.ACTIVE);
		request.setProperty(previousPropertyToBeReInstated);

		producer.push(config.getUpdatePropertyTopic(), request);
	}

	/**
	 * Search property with given PropertyCriteria
	 *
	 * @param criteria PropertyCriteria containing fields on which search is based
	 * @return list of properties satisfying the containing fields in criteria
	 */
	public List<Property> searchProperty(PropertyCriteria criteria, RequestInfo requestInfo) {

		List<Property> properties;

		/*
		 * throw error if audit request is with no proeprty id or multiple propertyids
		 */
		if (criteria.isAudit() && (CollectionUtils.isEmpty(criteria.getPropertyIds())
				|| (!CollectionUtils.isEmpty(criteria.getPropertyIds()) && criteria.getPropertyIds().size() > 1))) {

			throw new CustomException("EG_PT_PROPERTY_AUDIT_ERROR", "Audit can only be provided for a single propertyId");
		}

		if (criteria.getMobileNumber() != null || criteria.getName() != null || criteria.getOwnerIds() != null) {

			/* converts owner information to associated property ids */
			Boolean shouldReturnEmptyList = repository.enrichCriteriaFromUser(criteria, requestInfo);

			if (shouldReturnEmptyList)
				return Collections.emptyList();

			properties = repository.getPropertiesWithOwnerInfo(criteria, requestInfo, false);
		} else {
			properties = repository.getPropertiesWithOwnerInfo(criteria, requestInfo, false);
		}

		properties.forEach(property -> {
			enrichmentService.enrichBoundary(property, requestInfo);
			enrichmentService.enrichOwnerInfo(property);
		});

		return properties;
	}

	public List<Property> searchPropertyPlainSearch(PropertyCriteria criteria, RequestInfo requestInfo) {
		List<Property> properties = getPropertiesPlainSearch(criteria, requestInfo);
		for(Property property:properties)
			enrichmentService.enrichBoundary(property,requestInfo);
		return properties;
	}


	List<Property> getPropertiesPlainSearch(PropertyCriteria criteria, RequestInfo requestInfo) {
		if (criteria.getLimit() != null && criteria.getLimit() > config.getMaxSearchLimit())
			criteria.setLimit(config.getMaxSearchLimit());
		if(criteria.getLimit()==null)
			criteria.setLimit(config.getDefaultLimit());
		if(criteria.getOffset()==null)
			criteria.setOffset(config.getDefaultOffset());
		PropertyCriteria propertyCriteria = new PropertyCriteria();
		if (criteria.getUuids() != null || criteria.getPropertyIds() != null) {
			if (criteria.getUuids() != null)
				propertyCriteria.setUuids(criteria.getUuids());
			if (criteria.getPropertyIds() != null)
				propertyCriteria.setPropertyIds(criteria.getPropertyIds());

		} else {
			List<String> uuids = repository.fetchIds(criteria);
			if (uuids.isEmpty())
				return Collections.emptyList();
			propertyCriteria.setUuids(new HashSet<>(uuids));
		}
		propertyCriteria.setLimit(criteria.getLimit());
		List<Property> properties = repository.getPropertiesForBulkSearch(propertyCriteria);
		if(properties.isEmpty())
			return Collections.emptyList();
		Set<String> ownerIds = properties.stream().map(Property::getOwners).flatMap(List::stream)
				.map(OwnerInfo::getUuid).collect(Collectors.toSet());

		UserSearchRequest userSearchRequest = userService.getBaseUserSearchRequest(criteria.getTenantId(), requestInfo);
		userSearchRequest.setUuid(ownerIds);
		UserDetailResponse userDetailResponse = userService.getUser(userSearchRequest);
		util.enrichOwner(userDetailResponse, properties, false);
		return properties;
	}

	public Property createMigrateProperty(PropertyRequest request) {
		enrichmentService.enrichCreateRequest(request);
		enrichmentService.enrichPropertyForMigration(request);
		userService.createMigrateUser(request);
		request.getProperty().setStatus(Status.ACTIVE);
		
		producer.push(config.getSavePropertyTopic(), request);
		request.getProperty().setWorkflow(null);
		return request.getProperty();
	}

	public Property createMigratePropertyUser(PropertyRequest request) {
		userService.createMigrateUserOnly(request);
		request.getProperty().setStatus(Status.ACTIVE);
		request.getProperty().setWorkflow(null);
		return request.getProperty();
	}

	public List<LegacyProperty> searchLegacyProperty(PropertyCriteria criteria, RequestInfo requestInfo) {

		propertyValidator.validatePropertyCriteriaForLegacy(criteria, requestInfo);		

		return repository.getLegacyPropertyDetails(criteria);
	}
	
	private void processActivationRequest(PropertyRequest request, Property propertyFromSearch) {
	
		enrichmentService.enrichActivationRequest(request, propertyFromSearch);
		
		//util.mergeAdditionalDetails(request, propertyFromSearch);
		PropertyRequest oldPropertyRequest = PropertyRequest.builder()
				.requestInfo(request.getRequestInfo())
				.property(propertyFromSearch)
				.build();
		
		/*set old property as inactive*/
		propertyFromSearch.setStatus(Status.INACTIVE);
		producer.push(config.getUpdatePropertyTopic(), oldPropertyRequest);
		/* save new record */
		
		producer.push(config.getSavePropertyTopic(), request);

	}

	private void processUpdateDataFieldRequest(PropertyRequest request) {
		if(StringUtils.isEmpty(request.getProperty().getLegacyHoldingNo()))
			throw new CustomException("INVALID_DATA","Legacy Holding No. passed can't be null or empty");
		PropertyCriteria criteria = propertyValidator.getPropertyCriteriaForSearch(request);
		List<Property> propertiesFromSearchResponse = searchProperty(criteria, request.getRequestInfo());
		if(CollectionUtils.isEmpty(propertiesFromSearchResponse)){
			throw new CustomException("INVALID_DATA","No Property was found for update");
		}
		Property property=propertiesFromSearchResponse.get(0);
		if(request.getProperty().getLegacyHoldingNo().equals(property.getLegacyHoldingNo())){
			throw new CustomException("INVALID_LEGACY_HOLDING_NO","The Legacy Holding Number is already mapped to this property");
		}
		property.setLegacyHoldingNo(request.getProperty().getLegacyHoldingNo());
		request.setProperty(property);
		producer.push(config.getUpdateDataFieldTopic(),request);
	}
	/**
	 * 
	 * @param ddnNoDetailRequest
	 * @return
	 * Purpose: To update DDN No. With Given Property Id
	 */

	public DDNNoDetail updateDDNNo(DDNNoDetailRequest ddnNoDetailRequest) {
		if(StringUtils.isEmpty(ddnNoDetailRequest.getDdnNoDetail().getDdnNo()) ||
				StringUtils.isEmpty(ddnNoDetailRequest.getDdnNoDetail().getPropertyId())){
			throw new CustomException("INVALID_INPUT","DDN No. or Property Id Passed Can't be null or empty");

		}
		List<Property> propertiesFromPropertyId=getPropertiesFromPropertyIdAndDDNNo(ddnNoDetailRequest.getDdnNoDetail().getPropertyId(),null);
		if(CollectionUtils.isEmpty(propertiesFromPropertyId)){
			throw new CustomException("INVALID_PROPERTY_ID","No Property was found with the given property Id");
		}
		List<Property> propertyFromDDNNo=getPropertiesFromPropertyIdAndDDNNo(null,ddnNoDetailRequest.getDdnNoDetail().getDdnNo());
		if(!CollectionUtils.isEmpty(propertyFromDDNNo)){
			throw new CustomException("INVALID_DDN_NO","The provided DDN No. is already mapped to a property "+propertyFromDDNNo.get(0).getPropertyId()+" in our system");
		}
		Property property=propertiesFromPropertyId.get(0);
		enrichPropertyDetailsWithDDN(ddnNoDetailRequest, property);
		PropertyRequest propertyRequest=PropertyRequest.builder().property(property).requestInfo(ddnNoDetailRequest.getRequestInfo()).build();
		producer.push(config.getUpdateDataFieldTopic(),propertyRequest);
		return ddnNoDetailRequest.getDdnNoDetail();
	}

	private void enrichPropertyDetailsWithDDN(DDNNoDetailRequest ddnNoDetailRequest, Property property) {
		property.setDdnNo(ddnNoDetailRequest.getDdnNoDetail().getDdnNo());
		JsonNode additionalDetails= property.getAdditionalDetails();
		ObjectNode additionalDetailsObject = additionalDetails==null?mapper.createObjectNode() :mapper.convertValue(additionalDetails,ObjectNode.class);
		additionalDetailsObject.put("ddnNoUpdatedTime",System.currentTimeMillis());
		String ddnNoUpdatedBy= ddnNoDetailRequest.getDdnNoDetail().getDdnNoUpdatedBy()==null?"DDN_Authority":ddnNoDetailRequest.getDdnNoDetail().getDdnNoUpdatedBy();
		additionalDetailsObject.put("ddnNoUpdatedBy",ddnNoUpdatedBy);
		additionalDetails=mapper.convertValue(additionalDetailsObject, JsonNode.class);
		property.setAdditionalDetails(additionalDetails);
	}


	/**
	 * 
	 * @param propertyId
	 * @param ddnNo
	 * @return
	 * Purpose:To retrieve Properties based on Property Id and DDN No
	 */
	private List<Property> getPropertiesFromPropertyIdAndDDNNo(String propertyId, String ddnNo) {
		PropertyCriteria propertyCriteria=PropertyCriteria.builder().build();
		if(propertyId!=null){
			propertyCriteria.setPropertyIds(Stream.of(propertyId).collect(Collectors.toSet()));
		}
		if(ddnNo!=null){
			propertyCriteria.setDdnNos(Stream.of(ddnNo).collect(Collectors.toSet()));
		}
		List<Property> properties=repository.getProperties(propertyCriteria,false);
		return properties;
	}

	public PropertyCount countProperty(@Valid PropertyCriteria criteria, RequestInfo requestInfo) {

		criteria.setOwnerIds(repository.getOwnerIds(criteria, requestInfo));
		PropertyCount propertyCount = repository.getPropertyCount(criteria);
		return propertyCount;
	}
}