package org.egov.pt.service;

import static org.egov.pt.util.PTConstants.ASSESSMENT_BUSINESSSERVICE;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import javax.validation.Valid;

import org.apache.commons.lang3.ObjectUtils;
import org.egov.common.contract.request.RequestInfo;
import org.egov.pt.config.PropertyConfiguration;
import org.egov.pt.models.Assessment;
import org.egov.pt.models.Assessment.Source;
import org.egov.pt.models.AssessmentSearchCriteria;
import org.egov.pt.models.BulkAssesmentCreationCriteria;
import org.egov.pt.models.BulkAssesmentCreationCriteriaWrapper;
import org.egov.pt.models.BulkAssessmentAudit;
import org.egov.pt.models.Property;
import org.egov.pt.models.PropertyCriteria;
import org.egov.pt.models.enums.CreationReason;
import org.egov.pt.models.enums.Status;
import org.egov.pt.models.workflow.BusinessService;
import org.egov.pt.models.workflow.ProcessInstanceRequest;
import org.egov.pt.models.workflow.State;
import org.egov.pt.producer.Producer;
import org.egov.pt.repository.AssessmentRepository;
import org.egov.pt.repository.PropertyRepository;
import org.egov.pt.util.AssessmentUtils;
import org.egov.pt.util.PTConstants;
import org.egov.pt.util.CommonUtils;
import org.egov.pt.util.DemandUtils;
import org.egov.pt.validator.AssessmentValidator;
import org.egov.pt.web.contracts.AssessmentRequest;
import org.egov.pt.web.contracts.Demand;
import org.egov.pt.web.contracts.DemandDetail;
import org.egov.pt.web.contracts.MigrateAssessmentRequest;
import org.egov.pt.web.contracts.PropertyRequest;
import org.egov.tracer.model.CustomException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class AssessmentService {

	private AssessmentValidator validator;

	private Producer producer;

	private PropertyConfiguration props;

	private AssessmentRepository repository;

	private AssessmentEnrichmentService assessmentEnrichmentService;

	private PropertyConfiguration config;

	private DiffService diffService;

	private AssessmentUtils utils;
	
	private DemandUtils demandUtils;

	private WorkflowService workflowService;

	private CalculationService calculationService;
	
	private PropertyService propertyService;
	
	private PropertyRepository propertyRepository;
	
	private DemandService demandService;


	@Autowired
	public AssessmentService(AssessmentValidator validator, Producer producer, PropertyConfiguration props, AssessmentRepository repository,
							 AssessmentEnrichmentService assessmentEnrichmentService, PropertyConfiguration config, DiffService diffService,
							 AssessmentUtils utils, WorkflowService workflowService, CalculationService calculationService, 
							 PropertyService propertyService, PropertyRepository propertyRepository, DemandService demandService,
							 DemandUtils demandUtils) {
		this.validator = validator;
		this.producer = producer;
		this.props = props;
		this.repository = repository;
		this.assessmentEnrichmentService = assessmentEnrichmentService;
		this.config = config;
		this.diffService = diffService;
		this.utils = utils;
		this.workflowService = workflowService;
		this.calculationService = calculationService;
		this.propertyService = propertyService;
		this.propertyRepository = propertyRepository;
		this.demandService = demandService;
		this.demandUtils = demandUtils;
	}

	/**
	 * Method to create an assessment asynchronously.
	 *
	 * @param request
	 * @return
	 */
	public Assessment createAssessment(AssessmentRequest request, boolean autoTriggered) {
		log.info("Receive assessment Request: "+request);
		Property property = utils.getPropertyForAssessment(request);
		validator.validateAssessmentCreate(request, property);
		assessmentEnrichmentService.enrichAssessmentCreate(request, autoTriggered);

		if(config.getIsAssessmentWorkflowEnabled() && !autoTriggered){
			assessmentEnrichmentService.enrichWorkflowForInitiation(request);
			ProcessInstanceRequest workflowRequest = new ProcessInstanceRequest(request.getRequestInfo(),
					Collections.singletonList(request.getAssessment().getWorkflow()));
			State state = workflowService.callWorkFlow(workflowRequest);
			request.getAssessment().getWorkflow().setState(state);
		}
		else {
			assessmentEnrichmentService.enrichDemand(request, property);
			calculationService.calculateTax(request, property);
			updatePropertyAfterAssessmentApproved(request.getRequestInfo(), request.getAssessment(), property);
		}
		producer.push(props.getCreateAssessmentTopic(), request);

		return request.getAssessment();
	}


	/**
	 * Method to update an assessment asynchronously.
	 *
	 * @param request
	 * @return
	 */
	public Assessment updateAssessment(AssessmentRequest request) {
		
		validateAssessment(request.getAssessment().getAdditionalDetails());
		Assessment assessment = request.getAssessment();
		RequestInfo requestInfo = request.getRequestInfo();
		Property property = utils.getPropertyForAssessment(request);
		assessmentEnrichmentService.enrichAssessmentUpdate(request, property);
		Assessment assessmentFromSearch = repository.getAssessmentFromDB(request.getAssessment());
		Boolean isWorkflowTriggered = isWorkflowTriggered(request.getAssessment(),assessmentFromSearch);
		validator.validateAssessmentUpdate(request, assessmentFromSearch, property, isWorkflowTriggered);

		if ((request.getAssessment().getStatus().equals(Status.INWORKFLOW) || isWorkflowTriggered)
				&& config.getIsAssessmentWorkflowEnabled()){

			BusinessService businessService = workflowService.getBusinessService(request.getAssessment().getTenantId(),
												ASSESSMENT_BUSINESSSERVICE,request.getRequestInfo());

			assessmentEnrichmentService.enrichAssessmentProcessInstance(request, property);

			Boolean isStateUpdatable = workflowService.isStateUpdatable(request.getAssessment().getWorkflow().getState().getState(),businessService);

			if(isStateUpdatable){

				assessmentEnrichmentService.enrichAssessmentUpdate(request, property);
				/*
				calculationService.getMutationFee();
				producer.push(topic1,request);*/
			}
			ProcessInstanceRequest workflowRequest = new ProcessInstanceRequest(requestInfo, Collections.singletonList(assessment.getWorkflow()));
			State state = workflowService.callWorkFlow(workflowRequest);
			String status = state.getApplicationStatus();
			request.getAssessment().getWorkflow().setState(state);
			//assessmentEnrichmentService.enrichStatus(status, assessment, businessService);
			assessment.setStatus(Status.fromValue(status));
			if(assessment.getWorkflow().getState().getState().equalsIgnoreCase(config.getDemandTriggerState())) {
				calculationService.calculateTax(request, property);
				//update property with latest demands edited by field inspector only on approval-
				updatePropertyAfterAssessmentApproved(requestInfo, assessment, property);
			}

			producer.push(props.getUpdateAssessmentTopic(), request);


			/*
				*
				* if(stateIsUpdatable){
				*
				*
				*  }
				*
				*  else {
				*  	producer.push(stateUpdateTopic, request);
				*
				*  }
				*
				*
				* */


		} else if(config.getIsAssessmentWorkflowEnabled() && request.getAssessment().getStatus().equals(Status.ACTIVE)){
			assessmentEnrichmentService.enrichWorkflowForInitiation(request);
			ProcessInstanceRequest workflowRequest = new ProcessInstanceRequest(request.getRequestInfo(),
					Collections.singletonList(request.getAssessment().getWorkflow()));
			State state = workflowService.callWorkFlow(workflowRequest);
			request.getAssessment().getWorkflow().setState(state);
			// To initiate the workflow change the status
			request.getAssessment().setStatus(Status.INWORKFLOW);
			producer.push(props.getUpdateAssessmentTopic(), request);
		} else if(!config.getIsAssessmentWorkflowEnabled()) {
			calculationService.calculateTax(request, property);
			producer.push(props.getUpdateAssessmentTopic(), request);
		}
		return request.getAssessment();
	}

	public void validateAssessment(JsonNode additionalDetails) {
		validator.validateAssessmentAndMutationAmount(additionalDetails);
	}

	public List<Assessment> searchAssessments(AssessmentSearchCriteria criteria){
		return repository.getAssessments(criteria);
	}

	public List<Assessment> getAssessmenPlainSearch(AssessmentSearchCriteria criteria) {
		if (criteria.getLimit() != null && criteria.getLimit() > config.getMaxSearchLimit())
			criteria.setLimit(config.getMaxSearchLimit());
		if(criteria.getLimit()==null)
			criteria.setLimit(config.getDefaultLimit());
		if(criteria.getOffset()==null)
			criteria.setOffset(config.getDefaultOffset());
		AssessmentSearchCriteria assessmentSearchCriteria = new AssessmentSearchCriteria();
		if (criteria.getIds() != null || criteria.getPropertyIds() != null || criteria.getAssessmentNumbers() != null) {
			if (criteria.getIds() != null)
				assessmentSearchCriteria.setIds(criteria.getIds());
			if (criteria.getPropertyIds() != null)
				assessmentSearchCriteria.setPropertyIds(criteria.getPropertyIds());
			if (criteria.getAssessmentNumbers() != null)
				assessmentSearchCriteria.setAssessmentNumbers(criteria.getAssessmentNumbers());

		} else {
			List<String> assessmentNumbers = repository.fetchAssessmentNumbers(criteria);
			if (assessmentNumbers.isEmpty())
				return Collections.emptyList();
			assessmentSearchCriteria.setAssessmentNumbers(new HashSet<>(assessmentNumbers));
		}
		assessmentSearchCriteria.setLimit(criteria.getLimit());
		return repository.getAssessmentPlainSearch(assessmentSearchCriteria);
	}

	/**
	 * Checks if the fields modified can trigger a workflow
	 * @return true if workflow is triggered else false
	 */
	private Boolean isWorkflowTriggered(Assessment assessment, Assessment assessmentFromSearch){

		Boolean isWorkflowTriggeredByFieldChange = false;
		List<String> fieldsUpdated = diffService.getUpdatedFields(assessment, assessmentFromSearch, "");

		if(!CollectionUtils.isEmpty(fieldsUpdated))
			isWorkflowTriggeredByFieldChange = intersection(new LinkedList<>(Arrays.asList(config.getAssessmentWorkflowTriggerParams().split(","))), fieldsUpdated);

		// third variable is needed only for mutation
		List<String> objectsAdded = diffService.getObjectsAdded(assessment, assessmentFromSearch, "");

		Boolean isWorkflowTriggeredByObjectAddition = false;
		if(!CollectionUtils.isEmpty(objectsAdded))
			isWorkflowTriggeredByObjectAddition = intersection(new LinkedList<>(Arrays.asList(config.getAssessmentWorkflowObjectTriggers().split(","))), objectsAdded);

		return (isWorkflowTriggeredByFieldChange || isWorkflowTriggeredByObjectAddition);
	}

	/**
	 * Checks if list2 has any element in list1
	 * @param list1
	 * @param list2
	 * @return true if list2 have any element in list1 else false
	 */
	private Boolean intersection(List<String> list1, List<String> list2){
		list1.retainAll(list2);
		return !CollectionUtils.isEmpty(list1);

	}

	public void validateAssessment(PropertyRequest request, String assessmentYear) {
		Map<String, Object> financialYearMaster =utils.getFinancialYear(request.getRequestInfo(), assessmentYear, request.getProperty().getTenantId());
	}

	public Assessment migrateAssessment(MigrateAssessmentRequest request) {
		AssessmentRequest assessmentRequest = createAssessmentRequest(request);
		Property property = utils.getPropertyForAssessment(assessmentRequest);
		
		//Added newly for populating additional details
		property.setAdditionalDetails(demandUtils.prepareAdditionalDetailsFromDemand(request.getDemands()));
		assessmentRequest.getAssessment().setAdditionalDetails(demandUtils.prepareAdditionalDetailsFromDemand(request.getDemands()));
		
		assessmentEnrichmentService.enrichAssessmentMigrate(assessmentRequest);

		calculationService.calculateMigrationFee(assessmentRequest, property, request.getDemands());
		producer.push(props.getCreateAssessmentTopic(), request);
		
		//Added newly for updating additional details in property table
		updatePropertyAfterAssessmentApproved(request.getRequestInfo(), request.getAssessment(), property);

		return request.getAssessment();
	}
	
	private AssessmentRequest createAssessmentRequest(@Valid MigrateAssessmentRequest request) {
		AssessmentRequest assessmentRequest = AssessmentRequest.builder().requestInfo(request.getRequestInfo())
				.assessment(request.getAssessment()).build();
		return assessmentRequest;
	}

	private void updatePropertyAfterAssessmentApproved(RequestInfo requestInfo, Assessment assessment,
			Property property) {
		PropertyRequest propertyRequest = new PropertyRequest();
		propertyRequest.setRequestInfo(requestInfo);
		property.setAdditionalDetails(
				utils.jsonMerge(property.getAdditionalDetails(), assessment.getAdditionalDetails()));
		propertyRequest.setProperty(property);
		propertyRequest.getProperty().setCreationReason(CreationReason.UPDATE);
		propertyService.updateProperty(propertyRequest, true);
	}


	public void createNewAssesmentFromPropertyForNewFinYear(BulkAssesmentCreationCriteriaWrapper bulkAssesmentCreationCriteriaWrapper) {
		RequestInfo requestInfo = bulkAssesmentCreationCriteriaWrapper.getRequestInfo();
		BulkAssesmentCreationCriteria bulkBillCriteria = bulkAssesmentCreationCriteriaWrapper.getBulkAssesmentCreationCriteria();
		
		DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
		LocalDateTime date = LocalDateTime.now();
		log.info("Time schedule start for property new assesment generation on : " + date.format(dateTimeFormatter));
		List<String> tenantIds = new ArrayList<>();
		
		if(Objects.isNull(bulkBillCriteria)) {
			throw new CustomException("ASSESSSMENT_CREATION_ERROR","BulkAssesmentCreationCriteria cannot be blank");
		} else if(Objects.isNull(bulkBillCriteria.getFinancialYear())){
			throw new CustomException("ASSESSSMENT_CREATION_ERROR","financialYear cannot be blank");
		}
		
		if(!Objects.isNull(bulkBillCriteria.getTenantIds()) && !bulkBillCriteria.getTenantIds().isEmpty()){
			tenantIds = bulkBillCriteria.getTenantIds();
		}else {
			if(StringUtils.hasText(config.getSchedulerTenants()) && config.getSchedulerTenants().trim().equalsIgnoreCase("ALL")) {
				log.info("Processing for all tenants");
				tenantIds = propertyRepository.getDistinctTenantIds();
			} else {
				String tenants = config.getSchedulerTenants();
				log.info("Processing for specific tenants: " + tenants);
				if(StringUtils.hasText(tenants)) {
					tenantIds = Arrays.asList(tenants.trim().split(","));
				}
			}
		}
		

		if(StringUtils.hasText(config.getSkipSchedulerTenants()) && !config.getSkipSchedulerTenants().trim().equalsIgnoreCase("NONE")) {
			log.info("Skip tenants: " + config.getSkipSchedulerTenants());
			List<String> skipTenants = Arrays.asList(config.getSkipSchedulerTenants().trim().split(","));
			tenantIds = tenantIds.stream().filter(tenant -> !skipTenants.contains(tenant)).collect(Collectors.toList());
		}
		if (tenantIds.isEmpty())
			return;
		
		log.info("Effective processing tenant Ids : " + tenantIds.toString());
		
		long count = 0;
		
		long batchsize = config.getDefaultLimit();
		long batchOffset = config.getDefaultOffset();

		if(bulkBillCriteria.getLimit() != null)
			batchsize = Math.toIntExact(bulkBillCriteria.getLimit());

		if(bulkBillCriteria.getOffset() != null)
			batchOffset = Math.toIntExact(bulkBillCriteria.getOffset());
		
		
		for(String tenantId : tenantIds) {
			tenantId = tenantId.trim();
			count = getCountOfActivePropertyByTenantId(tenantId);
			if(count>0) {
				while (count>0) {
					//Get all active property for tenants
					log.info("count [ "+count+" ], batchsize [ "+batchsize+" ], batchOffset [ "+batchOffset+" ]");
					List<Property> properties = getActivePropertiesWithActiveAssesment(tenantId,batchsize, batchOffset, bulkBillCriteria.getFinancialYear());
					
					if(Objects.isNull(properties) || properties.isEmpty() ) {
						count = 0;
					}
					log.info(properties.stream().map(Property::getPropertyId).collect(Collectors.toList()).toString());
					if (properties.size() > 0) {
						properties.stream()
						.forEach(property -> {
							try {
								Thread.sleep(5000);
								AssessmentRequest assessmentRequest = prepareAssessmentRequest(PropertyRequest
										.builder().requestInfo(requestInfo).property(property).build(),
										bulkBillCriteria.getFinancialYear());
								createAssessmentForNewFinYear(assessmentRequest, true);
							} catch (InterruptedException e) { 
								log.error("Failed to sleep thread");
							} catch (Exception e) {
								log.error("Failed to migrate assessment for property id ["+property.getPropertyId()+"]");
							}
							
							
							
						});
						count = count - properties.size();
					}
					batchOffset = batchOffset + batchsize;
					
					log.info("Pending connection count "+ count +" for tenant: "+ tenantId);
					
					BulkAssessmentAudit audit = BulkAssessmentAudit.builder().tenantid(tenantId)
							.businessService("PT")
							.limit(batchsize)
							.id(UUID.randomUUID().toString())
							.offset(batchOffset)
							.createdTime(System.currentTimeMillis())
							.auditTime(System.currentTimeMillis())
							.recordCount(count)
							.message("prcoess succeded in assesment creation for fin year "+bulkBillCriteria.getFinancialYear())
							.build();
					
					producer.push(props.getBulkAssessmentGenAutidTopic(), audit);
				}
			}
			
		};
	}
	
	/**
	 * 
	 * @param tenantId
	 * @param limit
	 * @param offset
	 * @return
	 */
	public List<Property> getActivePropertiesWithActiveAssesment(String tenantId,long limit,long offset, String financialYear) {
		log.info("getActivePropertiesWithActiveAssesment >>");
		String previuosFinancialYear = CommonUtils.getPreviousFinancialYear(financialYear);
		log.info("Get all properties for tenant Ids : " + tenantId + " , financialYear : "+previuosFinancialYear);
		PropertyCriteria criteria = PropertyCriteria.builder().tenantId(tenantId).limit(limit).offset(offset).build();
		return propertyRepository.getActivePropertiesWithActiveAssesmentForCurentFinYear(criteria, previuosFinancialYear);
	}
	
	public int getCountOfActivePropertyByTenantId(String tenantId) {
		log.info("getCountOfActivePropertyByTenantId >>");
		log.info("Get no of property ids for tenant id : " + tenantId);
		PropertyCriteria criteria = PropertyCriteria.builder().tenantId(tenantId).build();
		return propertyRepository.getCountOfActivePropertyByTenantId(criteria);
	}
	
	
	
	/**
	 * Prepare {@link AssessmentRequest} from {@link PropertyRequest} for a financialYear
	 * @param request
	 * @param financialYear
	 * @return
	 */
	private AssessmentRequest prepareAssessmentRequest(PropertyRequest request, String financialYear) {
		return AssessmentRequest.builder()
						.assessment(Assessment.builder()
								.tenantId(request.getProperty().getTenantId())
								.propertyId(request.getProperty().getPropertyId())
								.source(Source.MUNICIPAL_RECORDS)
								.channel(request.getProperty().getChannel())
								.assessmentDate((new Date()).getTime())
								.financialYear(financialYear).build())
						.property(request.getProperty())
						.requestInfo(request.getRequestInfo()).build();
	}
	
	/**
	 * Method to create an assessment for new financial year.
	 *
	 * @param request
	 * @return
	 */
	private Assessment createAssessmentForNewFinYear(AssessmentRequest request, boolean autoTriggered) {
		Property property = utils.getPropertyForAssessment(request);
		validator.validateAssessmentCreate(request, property);
		assessmentEnrichmentService.enrichAssessmentCreate(request, autoTriggered);
		
		if(Objects.isNull(property.getAdditionalDetails())) {
			List<Demand> demands = demandService.searchDemand(property.getTenantId(),
					Collections.singleton(property.getPropertyId()), null, null, PTConstants.ASMT_MODULENAME,
					request.getRequestInfo());
			//Added newly for populating additional details
			JsonNode additionalDetails = demandUtils.prepareAdditionalDetailsFromDemand(demands);
			property.setAdditionalDetails(additionalDetails);
			request.getAssessment().setAdditionalDetails(additionalDetails);
		}

		//Remove OTHER_DUES for new demand creation for new financial year
		JsonNode additionalDetails = property.getAdditionalDetails();
		if(additionalDetails.has(PTConstants.OTHER_DUES)  && additionalDetails.get(PTConstants.OTHER_DUES) != null) {
			((ObjectNode)additionalDetails).put(PTConstants.OTHER_DUES, "0");
		}
		property.setAdditionalDetails(additionalDetails);
		
		assessmentEnrichmentService.enrichDemand(request, property);
		calculationService.calculateTax(request, property);
		
		producer.push(props.getCreateAssessmentTopic(), request);
		
		//Update additional details after assesment creation
		updatePropertyAfterAssessmentApproved(request.getRequestInfo(), request.getAssessment(), property);

		return request.getAssessment();
	}
	
	public void createbulkAssessment(BulkAssesmentCreationCriteriaWrapper bulkAssesmentCreationCriteriaWrapper) {
		RequestInfo requestInfo = bulkAssesmentCreationCriteriaWrapper.getRequestInfo();
		BulkAssesmentCreationCriteria bulkBillCriteria = bulkAssesmentCreationCriteriaWrapper.getBulkAssesmentCreationCriteria();
		
		DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
		LocalDateTime date = LocalDateTime.now();
		log.info("Time schedule start for property new assesment generation on : " + date.format(dateTimeFormatter));
		List<String> tenantIds = new ArrayList<>();
		
		if(Objects.isNull(bulkBillCriteria)) {
			throw new CustomException("ASSESSSMENT_CREATION_ERROR","BulkAssesmentCreationCriteria cannot be blank");
		} else if(Objects.isNull(bulkBillCriteria.getFinancialYear())){
			throw new CustomException("ASSESSSMENT_CREATION_ERROR","financialYear cannot be blank");
		}
		
		if(!Objects.isNull(bulkBillCriteria.getTenantIds()) && !bulkBillCriteria.getTenantIds().isEmpty()){
			tenantIds = bulkBillCriteria.getTenantIds();
		}else {
			if(StringUtils.hasText(config.getSchedulerTenants()) && config.getSchedulerTenants().trim().equalsIgnoreCase("ALL")) {
				log.info("Processing for all tenants");
				tenantIds = propertyRepository.getDistinctTenantIds();
			} else {
				String tenants = config.getSchedulerTenants();
				log.info("Processing for specific tenants: " + tenants);
				if(StringUtils.hasText(tenants)) {
					tenantIds = Arrays.asList(tenants.trim().split(","));
				}
			}
		}
		

		if(StringUtils.hasText(config.getSkipSchedulerTenants()) && !config.getSkipSchedulerTenants().trim().equalsIgnoreCase("NONE")) {
			log.info("Skip tenants: " + config.getSkipSchedulerTenants());
			List<String> skipTenants = Arrays.asList(config.getSkipSchedulerTenants().trim().split(","));
			tenantIds = tenantIds.stream().filter(tenant -> !skipTenants.contains(tenant)).collect(Collectors.toList());
		}
		if (tenantIds.isEmpty())
			return;
		
		log.info("Effective processing tenant Ids : " + tenantIds.toString());
		
		for(String tenantId : tenantIds) {
			tenantId = tenantId.trim();
			long count = 0;
			
			long batchsize = config.getDefaultLimit();
			long batchOffset = config.getDefaultOffset();

			if(bulkBillCriteria.getLimit() != null)
				batchsize = Math.toIntExact(bulkBillCriteria.getLimit());

			if(bulkBillCriteria.getOffset() != null)
				batchOffset = Math.toIntExact(bulkBillCriteria.getOffset());
			
			// TODO:  Count only those that dont have assessments in pending for current year
			//count = getCountOfActivePropertyByTenantId(tenantId);//--check this query
		    count = getCountOfActivePropertyByTenantIdNotHavingAnyAssessmentForCurrentFY(tenantId,bulkBillCriteria.getFinancialYear());


			if(count>0) {
				while (count>0) {
					//Get all active property for tenants
					log.info("count [ "+count+" ], batchsize [ "+batchsize+" ], batchOffset [ "+batchOffset+" ]");
					
					// TODO:  Only include those active properties that dont have assessments in pending for current year also
					List<Property> tempProperties = getActivePropertiesWithActiveAssesment(tenantId,batchsize, batchOffset, bulkBillCriteria.getFinancialYear());
					List<String> propertiesHavingCurrentFYAssessment = getPropertiesHavingCurrentFYAssessment(tenantId, bulkBillCriteria.getFinancialYear());
					Set<String> propertiesSet = new HashSet<>(propertiesHavingCurrentFYAssessment);
					List<Property> properties = tempProperties.stream().filter(property -> !propertiesSet.contains(property.getPropertyId())).collect(Collectors.toList());

					if(Objects.isNull(properties) || properties.isEmpty() ) {
						count = 0;
					}					
					Set<String> propertyIds = properties.stream().map(Property::getPropertyId).collect(Collectors.toSet());
					log.info("Property Ids :" + propertyIds.toString());
					List<Property> listOfProperties = new ArrayList<>();
					if(count>0) {
						listOfProperties = utils.getPropertyForBulkAssessment(propertyIds, tenantId, requestInfo);
					}
					if (listOfProperties.size() > 0) {
						List<AssessmentRequest> assessmentRequests = listOfProperties.stream().map(property -> prepareAssessmentRequest(PropertyRequest
										.builder().requestInfo(requestInfo).property(property).build(),
										bulkBillCriteria.getFinancialYear())).collect(Collectors.toList());
						
						BulkAssessmentAudit assessmentAudit = BulkAssessmentAudit.builder().tenantid(tenantId)
								.businessService("PT")
								.limit(batchsize)
								.id(UUID.randomUUID().toString())
								.offset(batchOffset)
								.createdTime(System.currentTimeMillis())
								.auditTime(System.currentTimeMillis())
								.recordCount(count)
								.assessmentRequests(assessmentRequests)
								.build();
						
						producer.push(props.getBulkAssessmentCreateTopic(), assessmentAudit);
					}
					
					
					log.info("Pending connection count "+ count +" for tenant: "+ tenantId);
					
					
					batchOffset = batchOffset + batchsize;
					count = count - listOfProperties.size();
				}
			}
		}
	}
	
	
	private List<String> getPropertiesHavingCurrentFYAssessment(String tenantId, String financialYear) {
		log.info("getPropertiesHavingCurrentFYAssessment >>");
		log.info("Get property ids for tenant id : " + tenantId);
		PropertyCriteria criteria = PropertyCriteria.builder().tenantId(tenantId).build();
		return propertyRepository.getPropertiesHavingCurrentFYAssessment(criteria, financialYear); //--check this query
	}

	private long getCountOfActivePropertyByTenantIdNotHavingAnyAssessmentForCurrentFY(String tenantId,
			String financialYear) {
			log.info("Get all properties for tenant Ids : " + tenantId + " , financialYear : "+financialYear);
			PropertyCriteria criteria = PropertyCriteria.builder().tenantId(tenantId).build();
			return propertyRepository.getCountOfActivePropertyByTenantIdNotHavingAnyAssessmentForCurrentFY(criteria, financialYear);
	}

	public void processBulkAssessment(List<AssessmentRequest> request, boolean autoTriggered) {
//		Property property = utils.getPropertyForAssessment(request);
//		validator.validateAssessmentCreate(request, property);
//		enrichProperties(request);
		RequestInfo requestInfo = request.get(0).getRequestInfo();
		request.parallelStream().forEach(asmtRequest -> assessmentEnrichmentService.enrichAssessmentCreate(asmtRequest, autoTriggered));
		
		Set<String> propertyIds = request.parallelStream().map(asmt -> asmt.getAssessment().getPropertyId()).collect(Collectors.toSet());
		List<Demand> demands = demandService.searchDemand(request.get(0).getAssessment().getTenantId(),
				propertyIds, null, null, PTConstants.ASMT_MODULENAME,
				requestInfo);
		
		AtomicBoolean removeWaterTaxAllowed = new AtomicBoolean(false);
		if (config.getIsULBSExceptionAllowed()) {
			List<String> exemptedULBS = Arrays.asList(config.getExemptedULBS().split(","));
			if (!ObjectUtils.isEmpty(request.get(0).getProperty())
					&& StringUtils.hasText(request.get(0).getProperty().getTenantId())) {
				String tenantid = request.get(0).getProperty().getTenantId();
				if (!CollectionUtils.isEmpty(exemptedULBS) && exemptedULBS.contains(tenantid)) {
					removeWaterTaxAllowed.set(true);
				}
			}

		}
		
		request.stream().forEach(asmt -> {
			if(Objects.isNull(asmt.getProperty().getAdditionalDetails())) {
				//Added newly for populating additional details
				JsonNode additionalDetails = demandUtils.prepareAdditionalDetailsFromDemand(demands);
				asmt.getProperty().setAdditionalDetails(additionalDetails);
				asmt.getAssessment().setAdditionalDetails(additionalDetails);
			}
		
			//Remove OTHER_DUES for new demand creation for new financial year
			JsonNode additionalDetails = asmt.getProperty().getAdditionalDetails();
			if(additionalDetails.has(PTConstants.OTHER_DUES)  && additionalDetails.get(PTConstants.OTHER_DUES) != null) {
				((ObjectNode)additionalDetails).put(PTConstants.OTHER_DUES, "0");
			}
			
			if (removeWaterTaxAllowed.get()) {
				if (additionalDetails.has(PTConstants.WATER_TAX)
						&& additionalDetails.get(PTConstants.WATER_TAX) != null) {
					((ObjectNode) additionalDetails).put(PTConstants.WATER_TAX, "0");
				}

			}

			asmt.getProperty().setAdditionalDetails(additionalDetails);
			
			assessmentEnrichmentService.enrichDemand(asmt, asmt.getProperty());
		});
		calculationService.calculateTaxForbulkAssessment(request);
		
		
		request.stream().forEach(asmtreq -> {
			producer.push(props.getCreateAssessmentTopic(), asmtreq);
		});
		
		//Update additional details after assesment creation
		request.parallelStream().forEach(asmtReq ->
				updatePropertyAfterAssessmentApproved(requestInfo, asmtReq.getAssessment(), asmtReq.getProperty()));

	}

}
