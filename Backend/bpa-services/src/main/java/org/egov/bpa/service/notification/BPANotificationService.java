package org.egov.bpa.service.notification;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.egov.bpa.config.BPAConfiguration;
import org.egov.bpa.repository.ServiceRequestRepository;
import org.egov.bpa.service.BPALandService;
import org.egov.bpa.service.BPAService;
import org.egov.bpa.service.EDCRService;
import org.egov.bpa.service.EnrichmentService;
import org.egov.bpa.service.UserService;
import org.egov.bpa.util.BPAConstants;
import org.egov.bpa.util.BPAUtil;
import org.egov.bpa.util.NotificationUtil;
import org.egov.bpa.web.model.Action;
import org.egov.bpa.web.model.ActionItem;
import org.egov.bpa.web.model.BPA;
import org.egov.bpa.web.model.BPARequest;
import org.egov.bpa.web.model.BPASearchCriteria;
import org.egov.bpa.web.model.EmailRequest;
import org.egov.bpa.web.model.Event;
import org.egov.bpa.web.model.EventRequest;
import org.egov.bpa.web.model.Recepient;
import org.egov.bpa.web.model.RequestInfoWrapper;
import org.egov.bpa.web.model.SMSRequest;
import org.egov.bpa.web.model.landInfo.LandInfo;
import org.egov.bpa.web.model.landInfo.LandSearchCriteria;
import org.egov.bpa.web.model.landInfo.Source;
import org.egov.bpa.web.model.user.UserDetailResponse;
import org.egov.bpa.web.model.workflow.ProcessInstance;
import org.egov.bpa.workflow.WorkflowService;
import org.egov.common.contract.request.RequestInfo;
import org.egov.tracer.model.CustomException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class BPANotificationService {

	private BPAConfiguration config;

	private ServiceRequestRepository serviceRequestRepository;

	private NotificationUtil util;

	private BPAUtil bpaUtil;
	
	@Autowired
	private EDCRService edcrService;

	@Autowired
	private UserService userService;

	@Autowired
	private BPALandService bpalandService;
	
	@Autowired
	private ObjectMapper mapper;
	
	@Autowired
	private EnrichmentService enrichmentService;
	
	@Autowired
	private BPAService bpaService;
	
	@Autowired
	WorkflowService workflowService;
	
	@Autowired
    private NotificationUtil notificationUtil;

	@Autowired
	public BPANotificationService(BPAConfiguration config, ServiceRequestRepository serviceRequestRepository,
			NotificationUtil util, BPAUtil bpaUtil) {
		this.config = config;
		this.serviceRequestRepository = serviceRequestRepository;
		this.util = util;
		this.bpaUtil = bpaUtil;
	}

	/**
	 * Creates and send the sms based on the bpaRequest
	 * 
	 * @param request The bpaRequest listenend on the kafka topic
	 */
	public void process(BPARequest bpaRequest) {

		List<SMSRequest> smsRequests = new LinkedList<>();

		List<EmailRequest> emailnewRequests = new LinkedList<>();
		
		enrichEdcrData(bpaRequest);

		if (null != config.getIsSMSEnabled()) {

			if (config.getIsSMSEnabled()) {

				enrichSMSRequest(bpaRequest, smsRequests);
				if (!CollectionUtils.isEmpty(smsRequests))
					util.sendSMS(smsRequests, config.getIsSMSEnabled());

			}

		}
		if (null != config.getIsUserEventsNotificationEnabled()) {
			if (config.getIsUserEventsNotificationEnabled()) {
				EventRequest eventRequest = getEvents(bpaRequest);
				if (null != eventRequest)
					util.sendEventNotification(eventRequest);
			}
		}

		log.info("config.getIsEmailNotificationEnabled() value:"+config.getIsEmailNotificationEnabled());
		if (null != config.getIsEmailNotificationEnabled()) {
			if (config.getIsEmailNotificationEnabled()) {

				enrichEmailRequest(bpaRequest, emailnewRequests);
				if (!CollectionUtils.isEmpty(emailnewRequests))
					util.sendNewEmail(emailnewRequests, config.getIsEmailNotificationEnabled());
				log.info(" Sending mail : " + emailnewRequests);

			}
		}
	}

	

	/**
	 * Creates and registers an event at the egov-user-event service at defined
	 * trigger points as that of sms notifs.
	 * 
	 * Assumption - The bpaRequest received will always contain only one BPA.
	 * 
	 * @param request
	 * @return
	 */
	public EventRequest getEvents(BPARequest bpaRequest) {

		List<Event> events = new ArrayList<>();
		String tenantId = bpaRequest.getBPA().getTenantId();
		String localizationMessages = util.getLocalizationMessages(tenantId, bpaRequest.getRequestInfo()); // --need
																											// localization
																											// service
																											// changes.
		String message = util.getEventsCustomizedMsg(bpaRequest.getRequestInfo(), bpaRequest.getBPA(),
				localizationMessages, bpaRequest.getEdcrResponse()); // --need localization service changes.
		BPA bpaApplication = bpaRequest.getBPA();
		Map<String, String> mobileNumberToOwner = getUserList(bpaRequest);

		List<SMSRequest> smsRequests = util.createSMSRequest(message, mobileNumberToOwner);
		Set<String> mobileNumbers = smsRequests.stream().map(SMSRequest::getMobileNumber).collect(Collectors.toSet());
		Map<String, String> mapOfPhnoAndUUIDs = fetchUserUUIDs(mobileNumbers, bpaRequest.getRequestInfo(),
				bpaRequest.getBPA().getTenantId());

		Map<String, String> mobileNumberToMsg = smsRequests.stream()
				.collect(Collectors.toMap(SMSRequest::getMobileNumber, SMSRequest::getMessage));
		for (String mobile : mobileNumbers) {
			if (null == mapOfPhnoAndUUIDs.get(mobile) || null == mobileNumberToMsg.get(mobile)) {
				log.error("No UUID/SMS for mobile {} skipping event", mobile);
				continue;
			}
			List<String> toUsers = new ArrayList<>();
			toUsers.add(mapOfPhnoAndUUIDs.get(mobile));
			Recepient recepient = Recepient.builder().toUsers(toUsers).toRoles(null).build();
			List<String> payTriggerList = Arrays.asList(config.getPayTriggers().split("[,]"));
			Action action = null;
			if (payTriggerList.contains(bpaApplication.getStatus())) {
				List<ActionItem> items = new ArrayList<>();
				String busineService = bpaUtil.getFeeBusinessSrvCode(bpaApplication);
				String actionLink = config.getPayLink().replace("$mobile", mobile)
						.replace("$applicationNo", bpaApplication.getApplicationNo())
						.replace("$tenantId", bpaApplication.getTenantId()).replace("$businessService", busineService);
				actionLink = config.getUiAppHost() + actionLink;
				ActionItem item = ActionItem.builder().actionUrl(actionLink).code(config.getPayCode()).build();
				items.add(item);
				action = Action.builder().actionUrls(items).build();
			}

			events.add(Event.builder().tenantId(bpaApplication.getTenantId()).description(mobileNumberToMsg.get(mobile))
					.eventType(BPAConstants.USREVENTS_EVENT_TYPE).name(BPAConstants.USREVENTS_EVENT_NAME)
					.postedBy(BPAConstants.USREVENTS_EVENT_POSTEDBY).source(Source.WEBAPP).recepient(recepient)
					.eventDetails(null).actions(action).build());
		}

		if (!CollectionUtils.isEmpty(events)) {
			return EventRequest.builder().requestInfo(bpaRequest.getRequestInfo()).events(events).build();
		} else {
			return null;
		}

	}

	/**
	 * Fetches UUIDs of CITIZENs based on the phone number.
	 * 
	 * @param mobileNumbers
	 * @param requestInfo
	 * @param tenantId
	 * @return
	 */
	private Map<String, String> fetchUserUUIDs(Set<String> mobileNumbers, RequestInfo requestInfo, String tenantId) {

		Map<String, String> mapOfPhnoAndUUIDs = new HashMap<>();
		StringBuilder uri = new StringBuilder();
		uri.append(config.getUserHost()).append(config.getUserSearchEndpoint());
		Map<String, Object> userSearchRequest = new HashMap<>();
		userSearchRequest.put("RequestInfo", requestInfo);
		userSearchRequest.put("tenantId", tenantId);
		userSearchRequest.put("userType", "CITIZEN");
		for (String mobileNo : mobileNumbers) {
			userSearchRequest.put("userName", mobileNo);
			try {
				Object user = serviceRequestRepository.fetchResult(uri, userSearchRequest);
				if (null != user) {
					String uuid = JsonPath.read(user, "$.user[0].uuid");
					mapOfPhnoAndUUIDs.put(mobileNo, uuid);
				} else {
					log.error("Service returned null while fetching user for username - " + mobileNo);
				}
			} catch (Exception e) {
				log.error("Exception while fetching user for username - " + mobileNo);
				log.error("Exception trace: ", e);
				continue;
			}
		}
		return mapOfPhnoAndUUIDs;
	}

	/**
	 * Enriches the smsRequest with the customized messages
	 * 
	 * @param request     The bpaRequest from kafka topic
	 * @param smsRequests List of SMSRequets
	 */
	private void enrichSMSRequest(BPARequest bpaRequest, List<SMSRequest> smsRequests) {
		String tenantId = bpaRequest.getBPA().getTenantId();
		String localizationMessages = util.getLocalizationMessages(tenantId, bpaRequest.getRequestInfo());
		String message = util.getCustomizedMsg(bpaRequest.getRequestInfo(), bpaRequest.getBPA(), localizationMessages, bpaRequest.getEdcrResponse());
//		log.info("localizationMessages ##################### " + );
		log.info("SMS notification message : " + message);
		Map<String, String> mobileNumberToOwner = getUserList(bpaRequest);
		if(config.getEmployeeSmsEnable()) {
		log.info("SMS Notification Send to Employee Enable : "+config.getEmployeeSmsEnable());
		Map<String,String> employeeNumberWithMsg = getEmployeeList(bpaRequest,localizationMessages);
		smsRequests.addAll(util.createSMSRequestForEmployee(employeeNumberWithMsg));
		}
		smsRequests.addAll(util.createSMSRequest(message, mobileNumberToOwner));
	}

	/**
	 * Enriches the emailRequest with the customized messages
	 * 
	 * @param bpaRequest
	 * @param emailRequests
	 */
	public void enrichEmailRequest(BPARequest bpaRequest, List<EmailRequest> emailRequests) {

		String subject = config.getEmailSubject();
		log.info("subject inside method enrichEmailRequest:"+subject);
		String tenantId = bpaRequest.getBPA().getTenantId();
		String localizationMessages = util.getLocalizationMessages(tenantId, bpaRequest.getRequestInfo());
		log.info("fetched all bpa localizations");
		String message = util.getCustomizedEmailMsg(bpaRequest.getRequestInfo(), bpaRequest.getBPA(),
				localizationMessages, bpaRequest.getEdcrResponse());
		Map<String, String> emailToOwner = getUserEmailList(bpaRequest);
		String customisedSubject = subject + bpaRequest.getBPA().getApplicationNo();
		log.info("customisedSubject: "+customisedSubject);
		emailRequests.addAll(util.createNewEmailRequest(bpaRequest, message, emailToOwner, customisedSubject));
	}

	/**
	 * To get the Users to whom we need to send the sms notifications or event
	 * notifications.
	 * 
	 * @param bpaRequest
	 * @return
	 */
	private Map<String, String> getUserList(BPARequest bpaRequest) {
		Map<String, String> mobileNumberToOwner = new HashMap<>();
		String tenantId = bpaRequest.getBPA().getTenantId();
		String stakeUUID = bpaRequest.getBPA().getAuditDetails().getCreatedBy();
		List<String> ownerId = new ArrayList<String>();
		ownerId.add(stakeUUID);
		BPASearchCriteria bpaSearchCriteria = new BPASearchCriteria();
		bpaSearchCriteria.setOwnerIds(ownerId);
		bpaSearchCriteria.setTenantId(tenantId);
		UserDetailResponse userDetailResponse = userService.getUser(bpaSearchCriteria, bpaRequest.getRequestInfo());

		LandSearchCriteria landcriteria = new LandSearchCriteria();
		landcriteria.setTenantId(bpaSearchCriteria.getTenantId());
		landcriteria.setIds(Arrays.asList(bpaRequest.getBPA().getLandId()));
		List<LandInfo> landInfo = bpalandService.searchLandInfoToBPA(bpaRequest.getRequestInfo(), landcriteria);

		mobileNumberToOwner.put(userDetailResponse.getUser().get(0).getUserName(),
				userDetailResponse.getUser().get(0).getName());

		if (bpaRequest.getBPA().getLandInfo() == null) {
			for (int j = 0; j < landInfo.size(); j++)
				bpaRequest.getBPA().setLandInfo(landInfo.get(j));
		}

		if (!(bpaRequest.getBPA().getWorkflow().getAction().equals(config.getActionsendtocitizen())
				&& bpaRequest.getBPA().getStatus().equals("INITIATED"))
				&& !(bpaRequest.getBPA().getWorkflow().getAction().equals(config.getActionapprove())
						&& bpaRequest.getBPA().getStatus().equals("INPROGRESS"))) {

			bpaRequest.getBPA().getLandInfo().getOwners().forEach(owner -> {
				if (owner.getMobileNumber() != null) {
					mobileNumberToOwner.put(owner.getMobileNumber(), owner.getName());
				}
			});

		}
		return mobileNumberToOwner;
	}

	private Map<String, String> getUserEmailList(BPARequest bpaRequest) {
		log.info("inside method getUserEmailList");
		Map<String, String> emailToOwner = new HashMap<>();

		if (!(bpaRequest.getBPA().getWorkflow().getAction().equals(config.getActionsendtocitizen())
				&& bpaRequest.getBPA().getStatus().equals("INITIATED"))
				&& !(bpaRequest.getBPA().getWorkflow().getAction().equals(config.getActionapprove())
						&& bpaRequest.getBPA().getStatus().equals("INPROGRESS"))) {

			bpaRequest.getBPA().getLandInfo().getOwners().forEach(owner -> {
				if (owner.getEmailId() != null) {
					emailToOwner.put(owner.getEmailId(), owner.getName());
				}
			});

		}
		try {
			log.info("***emailToOwnerJson:*****"+mapper.writeValueAsString(emailToOwner));
		} catch (JsonProcessingException e) {
			log.error("error while parsing emailToOwner into json",e);
		}
		return emailToOwner;
	}
	
	private void enrichEdcrData(BPARequest bpaRequest) {
		String businessService = bpaRequest.getBPA().getBusinessService();
		Map<String, String> edcrResponse = new HashMap<>();
		if(!(businessService.isEmpty()) && businessService.equalsIgnoreCase(BPAConstants.BPA_PAP_MODULE_CODE)) {
			edcrResponse = edcrService.getEdcrDetailsForPreapprovedPlan(edcrResponse, bpaRequest);
		}else if(bpaRequest.getBPA().getOCOutsideSujogApplication()) {
			enrichmentService.enrichOCOutsideEdcrDetailsFromRequest(bpaRequest);
			enrichmentService.enrichUUIDInScrutinyDetails(bpaRequest);
			Boolean isSpecialBuilding = enrichmentService.isSpecialBuilding(bpaRequest);
			enrichmentService.enrichOCBusinessService(bpaRequest,isSpecialBuilding);
			if(bpaRequest.getBPA().getPermitEdcrDetail().getEdcrDetail()==null) {
				throw new CustomException("EDCR_DETAIL_NOT_FOUND","Edcr Detail is not found for OC Outside Sujog");
			}
			edcrResponse= mapper.convertValue(bpaRequest.getBPA().getPermitEdcrDetail(),LinkedHashMap.class);
		}
		else {
		 edcrResponse = edcrService.getEDCRDetails(bpaRequest.getRequestInfo(), bpaRequest.getBPA());
		}
		bpaRequest.setEdcrResponse(edcrResponse);
	}
	
	private Map<String, String> getEmployeeList(BPARequest bpaRequest, String localizationMessages) {

		Map<String, String> numberMsgMap = new HashMap<String, String>();

		RequestInfo requestInfo = bpaRequest.getRequestInfo();

		BPA bpa = bpaRequest.getBPA();

		String tenantId = bpa.getTenantId();

		String appSendByEmployee = requestInfo.getUserInfo().getUuid();
		log.info("Sent By UUId : "+appSendByEmployee);

		List<String> appSentToEmployee = bpa.getWorkflow().getAssignes();
		log.info("Sent To UUId : "+appSentToEmployee);

		List<String> sentToMobile = callHRMSToGetMobileNumbers(appSentToEmployee, requestInfo, tenantId);
		log.info("Sent To Mobile : "+sentToMobile);

		List<String> sentFromMobile = callHRMSToGetMobileNumbers(Arrays.asList(appSendByEmployee), requestInfo,
				tenantId);
		log.info("Sent By Mobile : "+sentFromMobile);
		
		Map<String, String> messageMap = util.getCustomizedMsgForEmployee(requestInfo, bpa, localizationMessages);
		log.info("messageMap : " + messageMap);

		if (!CollectionUtils.isEmpty(sentToMobile)) {
			numberMsgMap.put(sentToMobile.get(0), messageMap.get("messageTo"));
		}
		;

		if (!CollectionUtils.isEmpty(sentFromMobile)) {
			numberMsgMap.put(sentFromMobile.get(0), messageMap.get("messageFrom"));
		}
		;
		log.info("numberMsgMap : " + numberMsgMap);

		return numberMsgMap;

	}
	
	private List<String> callHRMSToGetMobileNumbers(List<String> uuids, RequestInfo requestInfo, String tenantId) {

		List<String> uuidsForRequest = new ArrayList<>();
		List<String> mobileNumbers = new ArrayList<>();
		uuidsForRequest.addAll(uuids);
		
		if (!CollectionUtils.isEmpty(uuidsForRequest)) {
			
			StringBuilder uri = new StringBuilder(config.getHrmshost());
			uri.append(config.getHrmsContextPath());
			uri.append(config.getHrmsSearchEndpoint());

			uri.append("?uuids=");
			uri.append(String.join(",", uuidsForRequest));

//			uri.append("&tenantId=");
//			uri.append(tenantId);

			uri.append("&isActive=true");

			RequestInfoWrapper requestInfoWrapper = RequestInfoWrapper.builder().requestInfo(requestInfo).build();
			LinkedHashMap fetchResult = null;
			fetchResult = (LinkedHashMap) serviceRequestRepository.fetchResult(uri, requestInfoWrapper);
			String jsonString = new JSONObject(fetchResult).toString();
			DocumentContext context = JsonPath.using(Configuration.defaultConfiguration()).parse(jsonString);
			mobileNumbers = context.read("Employees.*.user.mobileNumber");
		}

		return mobileNumbers;
	}

	public String sendPaymentSMS(RequestInfo requestInfo) {

		log.info("Starting payment reminder SMS job");
		BPASearchCriteria searchCriteria = BPASearchCriteria.builder().status(BPAConstants.SANC_FEE_STATE).build();
		List<BPA> bpas = bpaService.search(searchCriteria, requestInfo);

		if (CollectionUtils.isEmpty(bpas)) {
			log.info("No BPA applications found with status PENDING_SANC_FEE_PAYMENT");
			return "No applications found";
		}

		final long currentTime = System.currentTimeMillis();
		final long MILLIS_PER_DAY = 24 * 60 * 60 * 1000L;

		List<SMSRequest> smsRequests = new ArrayList<>();

		for (BPA bpa : bpas) {
			try {
				List<ProcessInstance> processInstances = workflowService.getProcessInstances(bpa, requestInfo, false);
				if (CollectionUtils.isEmpty(processInstances)) {
					log.warn("No process instance found for application: {}", bpa.getApplicationNo());
					continue;
				}

				ProcessInstance processInstance = processInstances.get(0);
				if (!"APPROVE".equalsIgnoreCase(processInstance.getAction())) {
					continue;
				}

				if (processInstance.getAuditDetails() == null
						|| processInstance.getAuditDetails().getCreatedTime() == null) {

					log.warn("Approval date not found for application: {}", bpa.getApplicationNo());
					continue;
				}

				long approvalTime = processInstance.getAuditDetails().getCreatedTime();

				long daysSinceApproval = (currentTime - approvalTime) / MILLIS_PER_DAY;

				log.info("Application No: {}, Days Since Approval: {}", bpa.getApplicationNo(), daysSinceApproval);

				// Day 25 - Citizen Reminder
				if (daysSinceApproval == 25) {

				    String citizenMobile = null;

				    if (bpa.getLandInfo() != null && !CollectionUtils.isEmpty(bpa.getLandInfo().getOwners())) {
				        citizenMobile = bpa.getLandInfo().getOwners().get(0).getMobileNumber();
				    }

				    Date paymentDueDate = Date.from(
				            Instant.ofEpochMilli(currentTime)
				                    .plus(5, ChronoUnit.DAYS));

				    BigDecimal amountToBePaid = notificationUtil.getAmountToBePaid(requestInfo, bpa);

				    String messageToCitizen = BPAConstants.PAYMENT_REMINDER_SMS_MESSAGE_FOR_CITIZEN
				            .replace("<1>", bpa.getApplicationNo())
				            .replace("<2>", amountToBePaid.toPlainString())
				            .replace("<3>", paymentDueDate.toString());
				    
					if (!StringUtils.isEmpty(citizenMobile)) {
						smsRequests.add(SMSRequest.builder().mobileNumber(citizenMobile)
								.message(messageToCitizen).build());

						log.info("Citizen reminder SMS queued for application: {}, mobile: {}", bpa.getApplicationNo(),
								citizenMobile);
					} else {
						log.warn("Citizen mobile number not found for application: {}", bpa.getApplicationNo());
					}

				    log.info("Citizen reminder SMS queued for application: {}, mobile: {}, message: {}",
				            bpa.getApplicationNo(), citizenMobile, messageToCitizen);
				}

				// Day 30 - Employee Action Reminder
				if (daysSinceApproval == 30) {
					String employeeMobile = null;

					if (processInstance.getAssigner() != null) {
						employeeMobile = processInstance.getAssigner().getMobileNumber();
					}
					
					String messageToEmployee = BPAConstants.ACTION_REMINDER_SMS_MESSAGE_FOR_EMPLOYEE
					            .replace("<1>", bpa.getApplicationNo());       
					            
					if (!StringUtils.isEmpty(employeeMobile)) {
						smsRequests.add(SMSRequest.builder().mobileNumber(employeeMobile)
								.message(messageToEmployee).build());

						log.info("Employee reminder SMS queued for application: {}, mobile: {}", bpa.getApplicationNo(),
								employeeMobile);
					} else {
						log.warn("Employee mobile number not found for application: {}", bpa.getApplicationNo());
					}
				}

			} catch (Exception ex) {

				log.error("Error while processing application: {}", bpa.getApplicationNo(), ex);
			}
		}

		if (!smsRequests.isEmpty()) {

			log.info("Sending {} SMS notifications", smsRequests.size());

			util.sendSMS(smsRequests, config.getIsSMSEnabled());

		} else {

			log.info("No SMS notifications to send");
		}

		log.info("Payment reminder SMS job completed");

		return String.format("SMS processing completed. Total SMS queued: %d", smsRequests.size());
	}

	public void sendPostApproveNotification(BPARequest bpaRequest) {
		BPA bpa = bpaRequest.getBPA();

		String citizenMobile = getCitizenMobile(bpa);

		if (StringUtils.isEmpty(citizenMobile)) {
			log.warn("Citizen mobile number not found for application: {}", bpa.getApplicationNo());
			return;
		}

		// BigDecimal amountToBePaid = notificationUtil.getAmountToBePaid(bpaRequest.getRequestInfo(), bpa);
		
		long amountToBePaid = getTotalAmountToBePaid(bpa);

		String messageToCitizen = BPAConstants.APPLICATION_APPROVED_SMS_MESSAGE_FOR_CITIZEN
				.replace("<1>", bpa.getApplicationNo()).replace("<2>", String.valueOf(amountToBePaid));

		List<SMSRequest> smsRequests = Collections
				.singletonList(SMSRequest.builder().mobileNumber(citizenMobile).message(messageToCitizen).build());

		log.info("Application Approved SMS queued for Citizen with application: {}, mobile: {}", bpa.getApplicationNo(),
				citizenMobile);

		util.sendSMS(smsRequests, config.getIsSMSEnabled());

		log.info("Sent {} SMS notification(s)", smsRequests.size());
	}
	
	private long getTotalAmountToBePaid(BPA bpa) {

		Map<String, Object> additionalDetails = (Map<String, Object>) bpa.getAdditionalDetails();

		if (additionalDetails == null) {
			return 0L;
		}

		Object sanEstimateObj = additionalDetails.get("san_estimate");

		if (!(sanEstimateObj instanceof List<?>)) {
			return 0L;
		}

		return ((List<?>) sanEstimateObj).stream().filter(Map.class::isInstance).map(Map.class::cast)
				.map(map -> map.get("value")).filter(Objects::nonNull).mapToLong(value -> {
					if (value instanceof Number) {
						return ((Number) value).longValue();
					}
					try {
						return Long.parseLong(value.toString());
					} catch (NumberFormatException e) {
						return 0L;
					}
				}).sum();
	}
	
	private String getCitizenMobile(BPA bpa) {
		return Optional.ofNullable(bpa.getLandInfo()).map(LandInfo::getOwners)
				.filter(owners -> !CollectionUtils.isEmpty(owners)).map(owners -> owners.get(0).getMobileNumber())
				.orElse(null);
	}
	
}
