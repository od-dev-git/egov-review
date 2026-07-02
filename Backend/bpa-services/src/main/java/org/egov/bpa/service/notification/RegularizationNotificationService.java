package org.egov.bpa.service.notification;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.egov.bpa.config.BPAConfiguration;
import org.egov.bpa.repository.ServiceRequestRepository;
import org.egov.bpa.service.RegularizationService;
import org.egov.bpa.util.NotificationUtil;
import org.egov.bpa.web.model.BPA;
import org.egov.bpa.web.model.BPARequest;
import org.egov.bpa.web.model.EmailRequest;
import org.egov.bpa.web.model.EventRequest;
import org.egov.bpa.web.model.RequestInfoWrapper;
import org.egov.bpa.web.model.SMSRequest;
import org.egov.bpa.web.model.regularization.Regularization;
import org.egov.bpa.web.model.regularization.RegularizationRequest;
import org.egov.common.contract.request.RequestInfo;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class RegularizationNotificationService {
	
	@Autowired
	private BPAConfiguration config;

	@Autowired
	private NotificationUtil util;

	@Autowired
	private RegularizationService regularizationService;
	
	private ServiceRequestRepository serviceRequestRepository;
	

	/**
	 * Process notification for regularization
	 * 
	 * @param regularizationRequest
	 */
	public void process(RegularizationRequest regularizationRequest) {
		List<SMSRequest> smsRequests = new LinkedList<>();
		List<EmailRequest> emailnewRequests = new LinkedList<>();
		
		if (null != config.getIsSMSEnabled()) {
			if (config.getIsSMSEnabled()) {
				enrichSMSRequest(regularizationRequest, smsRequests);
				if (!CollectionUtils.isEmpty(smsRequests))
					util.sendSMS(smsRequests, config.getIsSMSEnabled());
			}
		}
		
		
		if (null != config.getIsUserEventsNotificationEnabled()) {
			if (config.getIsUserEventsNotificationEnabled()) {
				EventRequest eventRequest = getEvents(regularizationRequest);
				if (!ObjectUtils.isEmpty(eventRequest))
					util.sendEventNotification(eventRequest);
			}
		}

		
		log.info("config.getIsEmailNotificationEnabled() value:"+config.getIsEmailNotificationEnabled());
		if (null != config.getIsEmailNotificationEnabled()) {
			if (config.getIsEmailNotificationEnabled()) {
				enrichEmailRequest(regularizationRequest, emailnewRequests);
				if (!CollectionUtils.isEmpty(emailnewRequests))
					util.sendNewEmail(emailnewRequests, config.getIsEmailNotificationEnabled());
				log.info(" Sending mail : " + emailnewRequests);

			}
		}
		
	}




	/**
	 * Enrich SMS Request using sms template
	 * 
	 * @param regularizationRequest
	 * @param smsRequests
	 */
	private void enrichSMSRequest(RegularizationRequest regularizationRequest, List<SMSRequest> smsRequests) {
		String tenantId = regularizationRequest.getRegularization().getTenantId();
		String localizationMessages = util.getLocalizationMessages(tenantId, regularizationRequest.getRequestInfo());
		String message = util.getRegularizationCustomizedMsg(regularizationRequest.getRequestInfo(), regularizationRequest.getRegularization(), localizationMessages);
		log.info("SMS notification message : " + message);
		Map<String, String> mobileNumberToOwner = regularizationService.getOwnersList(regularizationRequest);
		if(config.getEmployeeSmsEnable()) {
			Map<String,String> employeeNumberWithMsg = getEmployeeList(regularizationRequest,localizationMessages);
			smsRequests.addAll(util.createSMSRequestForEmployee(employeeNumberWithMsg));
			}
		smsRequests.addAll(util.createSMSRequest(message, mobileNumberToOwner));
	}



	/**
	 * 
	 * @param regularizationRequest
	 * @return EventRequest
	 */
	private EventRequest getEvents(RegularizationRequest regularizationRequest) {
		// TODO Auto-generated method stub
		return null;
	}
	
	
	/**
	 * Enrich Mail Request using mail template
	 * 
	 * @param regularizationRequest
	 * @param emailnewRequests
	 */
	private void enrichEmailRequest(RegularizationRequest regularizationRequest, List<EmailRequest> emailnewRequests) {
		String subject = config.getRegularizationEmailSubject();
        log.info("subject inside method enrichEmailRequest:"+subject);
        String tenantId = regularizationRequest.getRegularization().getTenantId();
        String localizationMessages = util.getLocalizationMessages(tenantId, regularizationRequest.getRequestInfo());
        log.info("fetched all bpa localizations");
        String message = util.getRegularizationCustomizedEmailMsg(regularizationRequest.getRequestInfo(), regularizationRequest.getRegularization(), localizationMessages);
        Map<String, String> emailToOwner = regularizationService.getUserEmailList(regularizationRequest);
        String customisedSubject = subject + regularizationRequest.getRegularization().getApplicationNo();
        log.info("customisedSubject: "+customisedSubject);
        emailnewRequests.addAll(util.createNewRegularizationEmailRequest(regularizationRequest, message, emailToOwner, customisedSubject));
		
	}
	
	private Map<String, String> getEmployeeList(RegularizationRequest regularizationRequest, String localizationMessages) {

		Map<String, String> numberMsgMap = new HashMap<String, String>();

		RequestInfo requestInfo = regularizationRequest.getRequestInfo();

		Regularization regularization = regularizationRequest.getRegularization();

		String tenantId = regularization.getTenantId();

		String appSendByEmployee = regularization.getAuditDetails().getLastModifiedBy();

		List<String> appSentToEmployee = regularization.getWorkflow().getAssignes();

		List<String> sentToMobile = callHRMSToGetMobileNumbers(appSentToEmployee, requestInfo, tenantId);

		List<String> sentFromMobile = callHRMSToGetMobileNumbers(Arrays.asList(appSendByEmployee), requestInfo,
				tenantId);

		Map<String, String> messageMap = util.getCustomizedMsgForEmployee(requestInfo, regularization, localizationMessages);

		if (!CollectionUtils.isEmpty(sentToMobile)) {
			numberMsgMap.put(sentToMobile.get(0), messageMap.get("messageTo"));
		}
		;

		if (!CollectionUtils.isEmpty(sentFromMobile)) {
			numberMsgMap.put(sentFromMobile.get(0), messageMap.get("messageFrom"));
		}
		;

		return numberMsgMap;

	}
	
	private List<String> callHRMSToGetMobileNumbers(List<String> uuids, RequestInfo requestInfo, String tenantId) {

		List<String> uuidsForRequest = new ArrayList<>();
		uuids.addAll(uuids);

		StringBuilder uri = new StringBuilder(config.getHrmshost());
		uri.append(config.getHrmsContextPath());
		uri.append(config.getHrmsSearchEndpoint());

		uri.append("?uuids=");
		uri.append(uuidsForRequest);

		uri.append("&tenantId=");
		uri.append(tenantId);

		uri.append("&isActive=true");

		RequestInfoWrapper requestInfoWrapper = RequestInfoWrapper.builder().requestInfo(requestInfo).build();
		LinkedHashMap fetchResult = null;
		fetchResult = (LinkedHashMap) serviceRequestRepository.fetchResult(uri, requestInfoWrapper);
		String jsonString = new JSONObject(fetchResult).toString();
		DocumentContext context = JsonPath.using(Configuration.defaultConfiguration()).parse(jsonString);
		List<String> mobileNumbers = context.read("Employees.*.user.mobileNumber");

		return mobileNumbers;
	}  

}
