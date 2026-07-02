package org.egov.bpa.service.notification;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.egov.bpa.config.BPAConfiguration;
import org.egov.bpa.repository.ServiceRequestRepository;
import org.egov.bpa.service.BPALandService;
import org.egov.bpa.service.DemolitionService;
import org.egov.bpa.service.RegularizationService;
import org.egov.bpa.service.UserService;
import org.egov.bpa.util.BPAConstants;
import org.egov.bpa.util.DemolitionNotificationUtil;
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
import org.egov.bpa.web.model.demolition.Demolition;
import org.egov.bpa.web.model.demolition.DemolitionRequest;
import org.egov.bpa.web.model.demolition.DemolitionSearchCriteria;
import org.egov.bpa.web.model.landInfo.LandInfo;
import org.egov.bpa.web.model.landInfo.LandSearchCriteria;
import org.egov.bpa.web.model.landInfo.Source;
import org.egov.bpa.web.model.user.UserDetailResponse;
import org.egov.common.contract.request.RequestInfo;
import org.egov.tracer.model.CustomException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class DemolitionNotificationService {

	@Autowired
	private BPAConfiguration config;

	@Autowired
	private DemolitionNotificationUtil util;

	@Autowired
	private DemolitionService demolitionService;

	private ServiceRequestRepository serviceRequestRepository;

	@Autowired
	private UserService userService;
	
	@Autowired
	private ObjectMapper mapper;

	@Autowired
	private BPALandService bpalandService;

	public void process(DemolitionRequest demolationRequest) {

		List<SMSRequest> smsRequests = new LinkedList<>();

		List<EmailRequest> emailnewRequests = new LinkedList<>();

		// enrichEdcrData(bpaRequest);

		if (null != config.getIsSMSEnabled()) {

			if (config.getIsSMSEnabled()) {

				enrichSMSRequest(demolationRequest, smsRequests);
				if (!CollectionUtils.isEmpty(smsRequests))
					util.sendSMS(smsRequests, config.getIsSMSEnabled());

			}
			
			log.info("config.getIsEmailNotificationEnabled() value:"+config.getIsEmailNotificationEnabled());
			if (null != config.getIsEmailNotificationEnabled()) {
				if (config.getIsEmailNotificationEnabled()) {

					enrichEmailRequest(demolationRequest, emailnewRequests);
					if (!CollectionUtils.isEmpty(emailnewRequests))
						util.sendNewEmail(emailnewRequests, config.getIsEmailNotificationEnabled());
					log.info(" Sending mail : " + emailnewRequests);

				}
			}

		}

	}

	private void enrichSMSRequest(DemolitionRequest demolationRequest, List<SMSRequest> smsRequests) {
		String message = null;
		String tenantId = demolationRequest.getDemolition().getTenantId();
		String localizationMessages = util.getLocalizationMessages(tenantId, demolationRequest.getRequestInfo());
		// String message = util.getCustomizedMsg(demolationRequest.getRequestInfo(),
		// demolationRequest.getDemolition(), localizationMessages);
		log.info("localizationMessages ##################### " + localizationMessages );
		// log.info("SMS notification message : " + message);
		Map<String, String> mobileNumberToOwner = getUserList(demolationRequest);
		log.info("Demolition Mobile Number To: ",String.valueOf(mobileNumberToOwner));
		if (config.getEmployeeSmsEnable()) {
			log.info("SMS Notification Send to Demolition Employee Enable : " + config.getEmployeeSmsEnable());
			Map<String, String> employeeNumberWithMsg = getEmployeeList(demolationRequest, localizationMessages);
			smsRequests.addAll(util.createSMSRequestForEmployee(employeeNumberWithMsg));
		}
		smsRequests.addAll(util.createSMSRequest(message, mobileNumberToOwner));
	}

	private Map<String, String> getEmployeeList(DemolitionRequest demolitionRequest, String localizationMessages) {

		Map<String, String> numberMsgMap = new HashMap<String, String>();

		RequestInfo requestInfo = demolitionRequest.getRequestInfo();

		Demolition demolition = demolitionRequest.getDemolition();

		String tenantId = demolition.getTenantId();

		String appSendByEmployee = requestInfo.getUserInfo().getUuid();
		log.info("Sent By UUId : " + appSendByEmployee);

		List<String> appSentToEmployee = demolition.getWorkflow().getAssignes();
		log.info("Sent To UUId : " + appSentToEmployee);

		List<String> sentToMobile = callHRMSToGetMobileNumbers(appSentToEmployee, requestInfo, tenantId);
		log.info("Sent To Mobile : " + sentToMobile);

		List<String> sentFromMobile = callHRMSToGetMobileNumbers(Arrays.asList(appSendByEmployee), requestInfo,
				tenantId);
		log.info("Sent By Mobile : " + sentFromMobile);

		Map<String, String> messageMap = util.getCustomizedMsgForEmployee(requestInfo, demolition, localizationMessages);
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

	private Map<String, String> getUserList(DemolitionRequest demolitionRequest) {
		Map<String, String> mobileNumberToOwner = new HashMap<>();
		String tenantId = demolitionRequest.getDemolition().getTenantId();
		String stakeUUID = demolitionRequest.getDemolition().getAuditDetails().getCreatedBy();
		List<String> ownerId = new ArrayList<String>();
		ownerId.add(stakeUUID);
		BPASearchCriteria bpaSearchCriteria = new BPASearchCriteria();
		bpaSearchCriteria.setOwnerIds(ownerId);
		bpaSearchCriteria.setTenantId(tenantId);
		UserDetailResponse userDetailResponse = userService.getUser(bpaSearchCriteria,
				demolitionRequest.getRequestInfo());

		LandSearchCriteria landcriteria = new LandSearchCriteria();
		landcriteria.setTenantId(bpaSearchCriteria.getTenantId());
		landcriteria.setIds(Arrays.asList(demolitionRequest.getDemolition().getLandInfo().getId()));
		List<LandInfo> landInfo = bpalandService.searchLandInfoToBPA(demolitionRequest.getRequestInfo(), landcriteria);

		mobileNumberToOwner.put(userDetailResponse.getUser().get(0).getUserName(),
				userDetailResponse.getUser().get(0).getName());

//		if (demolitionRequest.getDemolition().getLandInfo() == null) {
//			for (int j = 0; j < landInfo.size(); j++)
//				demolitionRequest.getDemolition().setLandInfo(landInfo.get(j));
//		}

		if (!(demolitionRequest.getDemolition().getWorkflow().getAction().equals(config.getActionsendtocitizen())
				&& demolitionRequest.getDemolition().getStatus().equals("INITIATED"))
				&& !(demolitionRequest.getDemolition().getWorkflow().getAction().equals(config.getActionapprove())
						&& demolitionRequest.getDemolition().getStatus().equals("INPROGRESS"))) {

			demolitionRequest.getDemolition().getOwners().forEach(owner -> {
				if (owner.getMobileNumber() != null) {
					mobileNumberToOwner.put(owner.getMobileNumber(), owner.getName());
				}
			});

		}
		return mobileNumberToOwner;
	}
	
	/**
	 * Enriches the emailRequest with the customized messages
	 * 
	 * @param bpaRequest
	 * @param emailRequests
	 */
	public void enrichEmailRequest(DemolitionRequest demolitionRequest, List<EmailRequest> emailRequests) {

		String subject = config.getDemolitionEmailSubject();
		log.info("subject inside method enrichEmailRequest:"+subject);
		String tenantId = demolitionRequest.getDemolition().getTenantId();
		String localizationMessages = util.getLocalizationMessages(tenantId, demolitionRequest.getRequestInfo());
		log.info("fetched all bpa localizations");
//		String message = util.getCustomizedEmailMsg(demolitionRequest.getRequestInfo(), demolitionRequest.getDemolition(),
//				localizationMessages);
		Map<String, String> employeeNumberWithEmail = getEmployeeListForEmail(demolitionRequest, localizationMessages);
//		Map<String, String> emailToOwner = getUserEmailList(demolitionRequest);
		String customisedSubject = subject + demolitionRequest.getDemolition().getApplicationNo();
		log.info("customisedSubject: "+customisedSubject);
		// emailRequests.addAll(util.createNewEmailRequest(demolitionRequest, message, emailToOwner, customisedSubject));
		employeeNumberWithEmail.entrySet().stream().forEach(emailCtx -> emailRequests.addAll(util.createNewEmailRequestV2(demolitionRequest,
						emailCtx.getValue(), emailCtx.getKey(), customisedSubject)));
	}
	
	private Map<String, String> getEmployeeListForEmail(DemolitionRequest demolitionRequest,
			String localizationMessages) {
		Map<String, String> emailMsgMap = new HashMap<String, String>();

		RequestInfo requestInfo = demolitionRequest.getRequestInfo();

		Demolition demolition = demolitionRequest.getDemolition();

		String tenantId = demolition.getTenantId();

		String appSendByEmployee = requestInfo.getUserInfo().getUuid();
		log.info("Sent By UUId : " + appSendByEmployee);

		List<String> appSentToEmployee = demolition.getWorkflow().getAssignes();
		log.info("Sent To UUId : " + appSentToEmployee);

		List<String> sentToEmail = callHRMSToGetEmailId(appSentToEmployee, requestInfo, tenantId);
		log.info("Sent To Mobile : " + sentToEmail);

		List<String> sentFromEmail = callHRMSToGetEmailId(Arrays.asList(appSendByEmployee), requestInfo,
				tenantId);
		log.info("Sent By Mobile : " + sentFromEmail);

		Map<String, String> messageMap = util.getCustomizedMsgForEmployee(requestInfo, demolition,
				localizationMessages);
		log.info("messageMap : " + messageMap);

		if (!CollectionUtils.isEmpty(sentToEmail)) {
			emailMsgMap.put(sentToEmail.get(0), messageMap.get("messageTo"));
		}
		;

		if (!CollectionUtils.isEmpty(sentFromEmail)) {
			emailMsgMap.put(sentFromEmail.get(0), messageMap.get("messageFrom"));
		}
		;
		log.info("numberMsgMap : " + emailMsgMap);

		return emailMsgMap;
	}

	private Map<String, String> getUserEmailList(DemolitionRequest demolitionRequest) {
		log.info("inside method getUserEmailList");
		Map<String, String> emailToOwner = new HashMap<>();

		if (!(demolitionRequest.getDemolition().getWorkflow().getAction().equals(config.getActionsendtocitizen())
				&& demolitionRequest.getDemolition().getStatus().equals("INITIATED"))
				&& !(demolitionRequest.getDemolition().getWorkflow().getAction().equals(config.getActionapprove())
						&& demolitionRequest.getDemolition().getStatus().equals("INPROGRESS"))) {

			demolitionRequest.getDemolition().getOwners().forEach(owner -> {
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
	
	private List<String> callHRMSToGetEmailId(List<String> uuids, RequestInfo requestInfo, String tenantId) {

		List<String> uuidsForRequest = new ArrayList<>();
		List<String> emailIds = new ArrayList<>();
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
			emailIds = context.read("Employees.*.user.emailId");
		}

		return emailIds;
	}


}
