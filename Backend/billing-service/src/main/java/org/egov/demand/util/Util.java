package org.egov.demand.util;

import static java.util.Objects.isNull;
import static org.egov.demand.util.Constants.ADVANCE_BUSINESSSERVICE_JSONPATH_CODE;
import static org.egov.demand.util.Constants.INVALID_TENANT_ID_MDMS_KEY;
import static org.egov.demand.util.Constants.INVALID_TENANT_ID_MDMS_MSG;
import static org.egov.demand.util.Constants.MDMS_CODE_FILTER;
import static org.egov.demand.util.Constants.MDMS_MASTER_NAMES;
import static org.egov.demand.util.Constants.MODULE_NAME;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import javax.validation.constraints.NotNull;

import org.apache.commons.lang3.StringUtils;
import org.egov.common.contract.request.RequestInfo;
import org.egov.demand.amendment.model.ProcessInstance;
import org.egov.demand.amendment.model.ProcessInstanceRequest;
import org.egov.demand.amendment.model.ProcessInstanceResponse;
import org.egov.demand.amendment.model.State;
import org.egov.demand.config.ApplicationProperties;
import org.egov.demand.consumer.notification.NotificationConsumer;
import org.egov.demand.model.AuditDetails;
import org.egov.demand.model.BillDetail;
import org.egov.demand.model.Demand;
import org.egov.demand.model.DemandDetail;
import org.egov.demand.producer.Producer;
import org.egov.demand.repository.ServiceRequestRepository;
import org.egov.mdms.model.MasterDetail;
import org.egov.mdms.model.MdmsCriteria;
import org.egov.mdms.model.MdmsCriteriaReq;
import org.egov.mdms.model.ModuleDetail;
import org.egov.tracer.model.CustomException;
import org.json.JSONObject;
import org.egov.demand.web.contract.DemandRequest;
import org.egov.demand.web.contract.User;
import org.egov.demand.web.models.Email;
import org.egov.demand.web.models.EmailRequest;
import org.egov.demand.web.models.ShortenRequest;
import org.postgresql.util.PGobject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class Util {
	

    private static final String BILLING_LOCALIZATION_MODULE = "billing-services";
	public static final String DEMAND_GEN_EMAIL_LOCALIZATION_CODE = "BILLINGSERVICE_BUSINESSSERVICE_DEMAND_GEN_NOTIF_MAIL";
	public static final String DEMAND_GEN_SMS_LOCALIZATION_CODE = "BILLINGSERVICE_BUSINESSSERVICE_DEMAND_GEN_NOTIF_MSG";
	public static final String DEMAND_GEN_SMS_LOCALIZATION_CODE_PT = "BILLINGSERVICE_BUSINESSSERVICE_DEMAND_GEN_NOTIF_MSG_PT";
	public static final String DEMAND_GEN_SMS_LOCALIZATION_CODE_WS = "BILLINGSERVICE_BUSINESSSERVICE_DEMAND_GEN_NOTIF_MSG_WS";
	public static final String BUSINESSSERVICELOCALIZATION_CODE_PREFIX = "BILLINGSERVICE_BUSINESSSERVICE_";
	
	public static final String LOCALIZATION_CODES_JSONPATH = "$.messages.*.code";
	public static final String LOCALIZATION_MSGS_JSONPATH = "$.messages.*.message";
	
	public static final String BUSINESSSERVICE_MDMS_MASTER = "BusinessService";
	public static final String BUSINESSSERVICE_CODES_JSONPATH = "$.MdmsRes.BillingService.BusinessService";

	public static final String USERNAME_REPLACE_STRING = "{username}";
	public static final String PERIOD_REPLACE_STRING = "{period}";
	public static final String TAX_REPLACE_STRING = "{amt}";
	
	public static final String MODULE_REPLACE_STRING = "{module}";
	public static final String MODULE_REPLACE_STRING_VALUE = "Property Tax";
	public static final String MODULE_REPLACE_WSModule_STRING_VALUE = "Water And Sewerage Tax";
	
	public static final String MODULE_PRIMARYKEY_REPLACE_STRING = "{primarykeystring}";
	public static final String MODULE_PRIMARYKEY_REPLACE_STRING_VALUE_PT = "property-id";
	public static final String MODULE_PRIMARYKEY_REPLACE_STRING_VALUE_WS = "connection-no";
	
	public static final String SERVICENUMBER_OF_MODULE_REPLACE_STRING = "{servicenumber}";
	public static final String DEMAND_AMOUNT_REPLACE_STRING = "{demandamount}";
	public static final String EXPIRY_DATE_REPLACE_STRING = "{expirydate}";
	public static final String PAYMENT_LINK_REPLACE_STRING = "{paymentlink}";
	
    public static final String EMAIL_SUBJECT_KEY = "application_no";

	@Autowired
	private ApplicationProperties appProps;
	
	@Autowired
	private ObjectMapper mapper;

	@Autowired
	private ServiceRequestRepository serviceRequestRepository;
	
	@Autowired
	private ApplicationProperties config;
	
	@Autowired
	private NotificationConsumer notificationConsumer;

	/**
	 * prepares mdms request
	 * 
	 * @param tenantId
	 * @param moduleName
	 * @param names
	 * @param filter
	 * @param requestInfo
	 * @return
	 */
	public MdmsCriteriaReq prepareMdMsRequest(String tenantId, String moduleName, List<String> names, String filter,
			RequestInfo requestInfo) {

		List<MasterDetail> masterDetails = new ArrayList<>();
		names.forEach(name -> {
				masterDetails.add(MasterDetail.builder().name(name).build());
		});

		ModuleDetail moduleDetail = ModuleDetail.builder().moduleName(moduleName).masterDetails(masterDetails).build();
		List<ModuleDetail> moduleDetails = new ArrayList<>();
		moduleDetails.add(moduleDetail);
		MdmsCriteria mdmsCriteria = MdmsCriteria.builder().tenantId(tenantId).moduleDetails(moduleDetails).build();
		return MdmsCriteriaReq.builder().requestInfo(requestInfo).mdmsCriteria(mdmsCriteria).build();
	}

	/**
	 * Fetches all the values of particular attribute as documentContext
	 *
	 * @param tenantId    tenantId of properties in PropertyRequest
	 * @param names       List of String containing the names of all master-data
	 *                    whose code has to be extracted
	 * @param requestInfo RequestInfo of the received PropertyRequest
	 * @return Map of MasterData name to the list of code in the MasterData
	 *
	 */
	public DocumentContext getAttributeValues(MdmsCriteriaReq mdmsReq) {
		StringBuilder uri = new StringBuilder(appProps.getMdmsHost()).append(appProps.getMdmsEndpoint());

		try {
			return JsonPath.parse(serviceRequestRepository.fetchResult(uri.toString(), mdmsReq));
		} catch (Exception e) {
			log.error("Error while fetvhing MDMS data", e);
			throw new CustomException(INVALID_TENANT_ID_MDMS_KEY, INVALID_TENANT_ID_MDMS_MSG);
		}
	}

	/**
	 * Generates the Audit details object for the requested user and current time
	 * 
	 * @param requestInfo
	 * @return
	 */
	public AuditDetails getAuditDetail(RequestInfo requestInfo) {

		String userId = requestInfo.getUserInfo().getUuid();
		Long currEpochDate = System.currentTimeMillis();

		return AuditDetails.builder().createdBy(userId).createdTime(currEpochDate).lastModifiedBy(userId)
				.lastModifiedTime(currEpochDate).build();
	}

	public String getStringVal(Set<String> set) {
		StringBuilder builder = new StringBuilder();
		int i = 0;
		for (String val : set) {
			builder.append(val);
			i++;
			if (i != set.size())
				builder.append(",");
		}
		return builder.toString();
	}
	
	/**
	 * converts the object to a pgObject for persistence
	 * 
	 * @param additionalDetails
	 * @return
	 */
	public PGobject getPGObject(Object additionalDetails) {

		String value = null;
		try {
			value = mapper.writeValueAsString(additionalDetails);
		} catch (JsonProcessingException e) {
			throw new CustomException(Constants.EG_BS_JSON_EXCEPTION_KEY, Constants.EG_BS_JSON_EXCEPTION_MSG);
		}

		PGobject json = new PGobject();
		json.setType(Constants.DB_TYPE_JSONB);
		try {
			json.setValue(value);
		} catch (SQLException e) {
			throw new CustomException(Constants.EG_BS_JSON_EXCEPTION_KEY, Constants.EG_BS_JSON_EXCEPTION_MSG);
		}
		return json;
	}
	
    public JsonNode getJsonValue(PGobject pGobject){
        try {
            if(Objects.isNull(pGobject) || Objects.isNull(pGobject.getValue()))
                return null;
            else
                return mapper.readTree( pGobject.getValue());
        } catch (Exception e) {
        	throw new CustomException(Constants.EG_BS_JSON_EXCEPTION_KEY, Constants.EG_BS_JSON_EXCEPTION_MSG);
        }
    }


    public String getApportionURL(){
		StringBuilder builder = new StringBuilder(appProps.getApportionHost());
		builder.append(appProps.getApportionEndpoint());
		return builder.toString();
	}

	/**
	 * Fetches the isAdvanceAllowed flag for the given businessService
	 * @param businessService
	 * @param mdmsData
	 * @return
	 */
	public Boolean getIsAdvanceAllowed(String businessService, DocumentContext mdmsData){
		String jsonpath = ADVANCE_BUSINESSSERVICE_JSONPATH_CODE;
		jsonpath = jsonpath.replace("{}",businessService);

		List<Boolean> isAdvanceAllowed = mdmsData.read(jsonpath);

		if(CollectionUtils.isEmpty(isAdvanceAllowed))
			throw new CustomException("BUSINESSSERVICE_ERROR","Failed to fetch isAdvanceAllowed for businessService: "+businessService);

		return isAdvanceAllowed.get(0);
	}
	
	public String getValueFromAdditionalDetailsForKey (Object additionalDetails, String key) {
		
		@SuppressWarnings("unchecked")
		Map<String, Object> additionalDetailMap = mapper.convertValue(additionalDetails, Map.class);
		if(null == additionalDetails) 
			return "";
		
		return (String) additionalDetailMap.get(key);
	}
	
	/**
	 * Setting the receiptnumber from payment to bill
	 * @param request
	 * @param uuid
	 * @return
	 */
	public ObjectNode setValuesAndGetAdditionalDetails(JsonNode additionalDetails, String key, String value) {

		ObjectNode objectNodeDetail;

		if (null == additionalDetails || (null != additionalDetails && additionalDetails.isNull())) {
			objectNodeDetail = mapper.createObjectNode();

		} else {

			objectNodeDetail = (ObjectNode) additionalDetails;
		}
		objectNodeDetail.put(key, value);
		
		return objectNodeDetail;
	}

	/**
	 * to Check and update whether a demand has been completely paid or not
	 * 
	 * demand payment will be complete when tax and collection are equal and the method is called with payment true
	 * 
	 * if the call happens with payment false and the demand is already tallied even then the demands won't be set to paid-completely to allow zero payment
	 */
	public void updateDemandPaymentStatus(Demand demand, Boolean isUpdateFromPayment) {
		BigDecimal totoalTax = demand.getDemandDetails().stream().map(DemandDetail::getTaxAmount)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		
		BigDecimal totalCollection = demand.getDemandDetails().stream().map(DemandDetail::getCollectionAmount)
				.reduce(BigDecimal.ZERO, BigDecimal::add);

		if (totoalTax.compareTo(totalCollection) == 0 && isUpdateFromPayment)
			demand.setIsPaymentCompleted(true);
		else if (totoalTax.compareTo(totalCollection) != 0)
			demand.setIsPaymentCompleted(false);
	}
	
	/**
	 * validates state level tenant-id for citizens and employees
	 * 
	 */
	public void validateTenantIdForUserType(String tenantId, RequestInfo requestInfo) {

		String userType = null;
		if(requestInfo.getUserInfo() != null)
		{
			userType = requestInfo.getUserInfo().getType();
		}
		if(Constants.EMPLOYEE_TYPE_CODE.equalsIgnoreCase(userType) && tenantId.split("\\.").length == 1) {
			throw new CustomException("EG_BS_INVALID_TENANTID","Employees cannot search based on state level tenantid");
		}
	}
	
	/**
	 * Fetches the required master data from MDMS service
	 * @param demandRequest The request for which master data has to be fetched
	 * @return
	 */
	public DocumentContext getMDMSData(RequestInfo requestInfo, String tenantId){
		
		/*
		 * Preparing the mdms request with billing service master and calling the mdms search API
		 */
		MdmsCriteriaReq mdmsReq = prepareMdMsRequest(tenantId, MODULE_NAME, MDMS_MASTER_NAMES, MDMS_CODE_FILTER,
				requestInfo);
		DocumentContext mdmsData = getAttributeValues(mdmsReq);

		return mdmsData;
	}
	

	/**
	 * Method to integrate with workflow
	 *
	 * takes the trade-license request as parameter constructs the work-flow request
	 *
	 * and sets the resultant status from wf-response back to trade-license object
	 *
	 */
	public State callWorkFlow(ProcessInstance workflow, RequestInfo requestInfo) {

		ProcessInstanceRequest workflowReq = ProcessInstanceRequest.builder()
				.processInstances(Arrays.asList(workflow))
				.requestInfo(requestInfo)
				.build();
				
		ProcessInstanceResponse response = null;
		StringBuilder url = new StringBuilder(appProps.getWfHost().concat(appProps.getWfTransitionPath()));
		Object objectResponse = serviceRequestRepository.fetchResult(url.toString(), workflowReq);
		response = mapper.convertValue(objectResponse, ProcessInstanceResponse.class);
		return response.getProcessInstances().get(0).getState();
	}
	
	/*
	 * 
	 * Json merge utils
	 */
	
	/**
	 * Method to merge additional details during update 
	 * 
	 * @param mainNode
	 * @param updateNode
	 * @return
	 */
	public JsonNode jsonMerge(JsonNode mainNode, JsonNode updateNode) {

		if (isNull(mainNode) || mainNode.isNull())
			return updateNode;
		if (isNull(updateNode) || updateNode.isNull())
			return mainNode;

		Iterator<String> fieldNames = updateNode.fieldNames();
		while (fieldNames.hasNext()) {

			String fieldName = fieldNames.next();
			JsonNode jsonNode = mainNode.get(fieldName);
			// if field exists and is an embedded object
			if (jsonNode != null && jsonNode.isObject()) {
				jsonMerge(jsonNode, updateNode.get(fieldName));
			} else {
				if (mainNode instanceof ObjectNode) {
					// Overwrite field
					JsonNode value = updateNode.get(fieldName);
					((ObjectNode) mainNode).set(fieldName, value);
				}
			}

		}
		return mainNode;
	}
	
	public String getIdsQueryForList(Set<String> ownerIds, List<Object> preparedStmtList) {

		StringBuilder query = new StringBuilder("(");
		query.append(createPlaceHolderForList(ownerIds));
		addToPreparedStatement(preparedStmtList, ownerIds);
		query.append(")");
		
		return query.toString();
	}

	private String createPlaceHolderForList(Set<String> ids) {
		
		StringBuilder builder = new StringBuilder();
		int length = ids.size();
		for (int i = 0; i < length; i++) {
			builder.append(" ?");
			if (i != length - 1)
				builder.append(",");
		}
		return builder.toString();
	}

	private void addToPreparedStatement(List<Object> preparedStmtList, Set<String> ids) {
		ids.forEach(id -> {
			preparedStmtList.add(id);
		});
	}

	public String buildEmailMessageBodyForDemandGenerationNotification(Demand demand, @NotNull RequestInfo requestInfo) {
		User user = demand.getPayer();
		log.info("Building Email Message Body For Demand Generation Notification ");
		log.info("Demand is " + String.valueOf(demand));

		String tenantId = demand.getTenantId();
		String content = notificationConsumer.fetchContentFromLocalization(requestInfo, tenantId, BILLING_LOCALIZATION_MODULE,
				DEMAND_GEN_EMAIL_LOCALIZATION_CODE);
		
		log.info("Localization Code :" + DEMAND_GEN_EMAIL_LOCALIZATION_CODE);

		if (!StringUtils.isEmpty(content)) {

			Calendar cal = Calendar.getInstance();
			cal.setTimeInMillis(demand.getTaxPeriodTo());
			
			content = content.replace(USERNAME_REPLACE_STRING, user.getName());
			content = content.replace(EXPIRY_DATE_REPLACE_STRING,
					" "+ cal.get(Calendar.DATE) + "/" + cal.get(Calendar.MONTH) + "/" + cal.get(Calendar.YEAR)+ " ".toUpperCase());
			content = content.replace(PERIOD_REPLACE_STRING, notificationConsumer.getPeriod(demand.getTaxPeriodFrom(), demand.getTaxPeriodTo()));
			content = content.replace(SERVICENUMBER_OF_MODULE_REPLACE_STRING, demand.getConsumerCode().split(":")[0]);
			if (demand.getBusinessService().equalsIgnoreCase("PT")) {
				content = content.replace(MODULE_REPLACE_STRING, MODULE_REPLACE_STRING_VALUE);
				content = content.replace(MODULE_PRIMARYKEY_REPLACE_STRING, MODULE_PRIMARYKEY_REPLACE_STRING_VALUE_PT);
			} else {
				content = content.replace(MODULE_REPLACE_STRING, MODULE_REPLACE_WSModule_STRING_VALUE);
				content = content.replace(MODULE_PRIMARYKEY_REPLACE_STRING,MODULE_PRIMARYKEY_REPLACE_STRING_VALUE_WS);
			}
		}
		return content;
	}
	
	public List<EmailRequest> createEmailRequest(String completeMsgs, String emailId, DemandRequest demandRequest,
			Object object) {
		log.info("Creating EmailRequest ");
		log.info("Demand Request is " + String.valueOf(demandRequest));
		List<EmailRequest> emailRequests = new ArrayList<>();
			
			if(demandRequest != null ) {
				
				String subject = config.getEmailSubject().replaceAll(EMAIL_SUBJECT_KEY, demandRequest.getDemands().get(0).getConsumerCode());
				String emailTo = emailId;
				String customizedMsg = completeMsgs;
				Email email = Email.builder().body(customizedMsg).emailTo(new HashSet<>(Arrays.asList(emailTo))).subject(subject).build();
				emailRequests.add(EmailRequest.builder().email(email).requestInfo(demandRequest.getRequestInfo()).build());
			}

			log.info("Email Request Created ");
			log.info(emailRequests.toString());
		return emailRequests;
	}

	public String buildSmsBody(Demand demand, @NotNull RequestInfo requestInfo) {
		User user = demand.getPayer();
		log.info("Building SMS Message Body For Demand Generation Notification ");
		log.info("Demand is " + String.valueOf(demand));
		String tenantId = demand.getTenantId();
		BigDecimal demandAmount = demand.getDemandDetails().stream()
	            .filter(Objects::nonNull)
	            .filter(demandDetail -> demandDetail.getTaxAmount() != null)
	            .map(DemandDetail::getTaxAmount)
	            .reduce(BigDecimal.ZERO, BigDecimal::add);

		String content = "";
		if (demand.getBusinessService().equalsIgnoreCase("PT")) {
			 content = notificationConsumer.fetchContentFromLocalization(requestInfo, tenantId, BILLING_LOCALIZATION_MODULE,
					DEMAND_GEN_SMS_LOCALIZATION_CODE_PT);
				log.info("Localization Code :" + DEMAND_GEN_SMS_LOCALIZATION_CODE_PT);

		} else {
			 content = notificationConsumer.fetchContentFromLocalization(requestInfo, tenantId, BILLING_LOCALIZATION_MODULE,
					DEMAND_GEN_SMS_LOCALIZATION_CODE_WS);
				log.info("Localization Code :" + DEMAND_GEN_SMS_LOCALIZATION_CODE_WS);

		}

		if (!StringUtils.isEmpty(content)) {
			content = content.replace(DEMAND_AMOUNT_REPLACE_STRING, demandAmount.toString());
			content = content.replace(SERVICENUMBER_OF_MODULE_REPLACE_STRING, demand.getConsumerCode().split(":")[0]);
			content = content.replace(PAYMENT_LINK_REPLACE_STRING,callUrlShortenerForSMS(tenantId,demand.getConsumerCode(),demand.getBusinessService()));
		}
		return content;
	}
	
	public String callUrlShortenerForSMS(String tenantId, String consumerCode, String module) {
		ShortenRequest shortenRequest = getShortenRequestForSMS(tenantId, consumerCode, module);
		StringBuilder url = new StringBuilder(
				appProps.getUrlShorteningHost().concat(appProps.getUrlShorteningNoAuthEndpoint()));
		Object objectResponse = serviceRequestRepository.fetchObject(url.toString(), shortenRequest);
		log.info("Shortener response : " + objectResponse.toString());
		return Optional.ofNullable(objectResponse.toString()).filter(s -> s.contains("noAuth?"))
				.map(s -> s.substring(s.indexOf("noAuth?") + "noAuth?".length())).orElse("");
	}
	
	private ShortenRequest getShortenRequestForSMS(String tenantId, String consumerCode, String module) {
		StringBuilder shorteningUrl= new StringBuilder(appProps.getCommonHost().concat(appProps.getCommonNoAuthPayEndpoint()));
		shorteningUrl.append("?consumerCode=").append(consumerCode).append("&tenantId=").append(tenantId)
		.append("&businessService=").append(module);
		Long currentTime = System.currentTimeMillis();
		ShortenRequest shortenRequest= ShortenRequest.builder().url(shorteningUrl.toString())
				.id(UUID.randomUUID().toString()).validFrom(currentTime).validTill(currentTime+1800000).build();
		return shortenRequest;
	}
	
	public String callUrlShortener(String tenantId,String consumerCode,String module,String whatsAppNumber) {
		ShortenRequest shortenRequest= getShortenRequest(tenantId,consumerCode,module,whatsAppNumber);
		StringBuilder url = new StringBuilder(appProps.getUrlShorteningHost().concat(appProps.getUrlShorteningNoAuthEndpoint()));
		Object objectResponse = serviceRequestRepository.fetchObject(url.toString(), shortenRequest);
		return objectResponse.toString();
	}
	
	public String callUrlShortenerv2(String tenantId,String consumerCode,String module,String whatsAppNumberv2) {
		ShortenRequest shortenRequest= getShortenRequestv2(tenantId,consumerCode,module,whatsAppNumberv2);
		StringBuilder url = new StringBuilder(appProps.getUrlShorteningHost().concat(appProps.getUrlShorteningNoAuthEndpoint()));
		Object objectResponse = serviceRequestRepository.fetchObject(url.toString(), shortenRequest);
		return objectResponse.toString();
	}

	private ShortenRequest getShortenRequest(String tenantId, String consumerCode, String module,
			String whatsAppNumber) {
		StringBuilder shorteningUrl= new StringBuilder(appProps.getCommonHost().concat(appProps.getCommonNoAuthPayEndpoint()));
		shorteningUrl.append("?consumerCode=").append(consumerCode).append("&tenantId=").append(tenantId)
		.append("&businessService=").append(module).append("&redirectNumber=").append(whatsAppNumber)
		.append("&channel=whatsapp&locale=en_IN");
		Long currentTime = System.currentTimeMillis();
		ShortenRequest shortenRequest= ShortenRequest.builder().url(shorteningUrl.toString())
				.id(UUID.randomUUID().toString()).validFrom(currentTime).validTill(currentTime+1800000).build();
		return shortenRequest;
	}
	
	private ShortenRequest getShortenRequestv2(String tenantId, String consumerCode, String module,
			String whatsAppNumber) {
		StringBuilder shorteningUrl= new StringBuilder(appProps.getCommonHost().concat(appProps.getCommonNoAuthPayEndpoint()));
		shorteningUrl.append("?consumerCode=").append(consumerCode).append("&tenantId=").append(tenantId)
		.append("&businessService=").append(module).append("&redirectNumber=").append(whatsAppNumber)
		.append("&channel=whatsappv2&locale=en_IN");
		Long currentTime = System.currentTimeMillis();
		ShortenRequest shortenRequest= ShortenRequest.builder().url(shorteningUrl.toString())
				.id(UUID.randomUUID().toString()).validFrom(currentTime).validTill(currentTime+1800000).build();
		return shortenRequest;
	}
	
}
