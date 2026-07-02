package org.egov.bpa.util;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.egov.bpa.config.BPAConfiguration;
import org.egov.bpa.producer.Producer;
import org.egov.bpa.repository.ServiceRequestRepository;
import org.egov.bpa.web.model.BPA;
import org.egov.bpa.web.model.BPARequest;
import org.egov.bpa.web.model.Email;
import org.egov.bpa.web.model.EmailRequest;
import org.egov.bpa.web.model.SMSRequest;
import org.egov.bpa.web.model.demolition.Demolition;
import org.egov.bpa.web.model.demolition.DemolitionRequest;
import org.egov.common.contract.request.RequestInfo;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class DemolitionNotificationUtil {

	private BPAConfiguration config;

	private Producer producer;

	private ObjectMapper mapper;

	private BPAUtil bpaUtil;

	private ServiceRequestRepository serviceRequestRepository;
	
	public static final Map<String, String> statusMap = createStatusMap();

	@Autowired
	public DemolitionNotificationUtil(BPAConfiguration config, ServiceRequestRepository serviceRequestRepository,
			Producer producer, BPAUtil bpaUtil, ObjectMapper mapper) {
		this.config = config;
		this.serviceRequestRepository = serviceRequestRepository;
		this.producer = producer;
		this.bpaUtil = bpaUtil;
		this.mapper = mapper;
	}

	@SuppressWarnings("rawtypes")
	public String getLocalizationMessages(String tenantId, RequestInfo requestInfo) {

		LinkedHashMap responseMap = (LinkedHashMap) serviceRequestRepository.fetchResult(getUri(tenantId, requestInfo),
				requestInfo);
		String jsonString = new JSONObject(responseMap).toString();
		return jsonString;
	}

	public StringBuilder getUri(String tenantId, RequestInfo requestInfo) {

		if (config.getIsLocalizationStateLevel())
			tenantId = tenantId.split("\\.")[0];

		String locale = "en_IN";
		if (!StringUtils.isEmpty(requestInfo.getMsgId()) && requestInfo.getMsgId().split("|").length >= 2)
			locale = requestInfo.getMsgId().split("\\|")[1];

		StringBuilder uri = new StringBuilder();
		uri.append(config.getLocalizationHost()).append(config.getLocalizationContextPath())
				.append(config.getLocalizationSearchEndpoint()).append("?").append("locale=").append(locale)
				.append("&tenantId=").append(tenantId).append("&module=").append(BPAConstants.SEARCH_MODULE);
		return uri;
	}

	public List<SMSRequest> createSMSRequestForEmployee(Map<String, String> employeeNumberWithMsg) {
		List<SMSRequest> smsRequest = new LinkedList<>();

		for (Map.Entry<String, String> entryset : employeeNumberWithMsg.entrySet()) {
			smsRequest.add(new SMSRequest(entryset.getKey(), entryset.getValue()));
		}
		return smsRequest;
	}

	public List<SMSRequest> createSMSRequest(String message, Map<String, String> mobileNumberToOwner) {
		List<SMSRequest> smsRequest = new LinkedList<>();

		for (Map.Entry<String, String> entryset : mobileNumberToOwner.entrySet()) {
			String customizedMsg = message.replace("<1>", entryset.getValue());
			smsRequest.add(new SMSRequest(entryset.getKey(), customizedMsg));
		}
		return smsRequest;
	}

	public void sendSMS(List<org.egov.bpa.web.model.SMSRequest> smsRequestList, boolean isSMSEnabled) {
		if (isSMSEnabled) {
			if (CollectionUtils.isEmpty(smsRequestList))
				log.debug("Messages from localization couldn't be fetched!");
			for (SMSRequest smsRequest : smsRequestList) {
				producer.push(config.getSmsNotifTopic(), smsRequest);
				log.debug("MobileNumber: " + smsRequest.getMobileNumber() + " Messages: " + smsRequest.getMessage());
			}
		}
	}

	@SuppressWarnings("unchecked")
	public Map<String, String> getCustomizedMsgForEmployee(RequestInfo requestInfo, Demolition demolition,
			String localizationMessage) {
		Map<String, String> messageMap = new HashMap<String, String>();
		String message = null, messageTemplate;
		String messageTo = null;
		String messageFrom = null;
		String messageTemplateTo = null;
		String messageTemplateFrom = null;

		String applicationStatus = demolition.getStatus().toString().toUpperCase();
		String action = demolition.getWorkflow().getAction().toString().toUpperCase();
		log.info(demolition.getApplicationNo() + " - " + applicationStatus + "_" + action);
		switch (applicationStatus) {
		case BPAConstants.STATUS_DOC_VERIFICATION_INPROGRESS:
			messageTemplateTo = getMessageTemplate(applicationStatus + "_" + action + "_TO", localizationMessage);
			messageTo = getLocalizedMsg(demolition, messageTemplateTo, applicationStatus);
			messageTemplateFrom = getMessageTemplate(applicationStatus + "_" + action + "_FROM", localizationMessage);
			messageFrom = getLocalizedMsg(demolition, messageTemplateFrom, applicationStatus);
			messageMap.put("messageTo", messageTo);
			messageMap.put("messageFrom", messageFrom);
			break;

		case BPAConstants.STATUS_FIELDINSPECTION_INPROGRESS:
			messageTemplateTo = getMessageTemplate(applicationStatus + "_" + action + "_TO", localizationMessage);
			messageTo = getLocalizedMsg(demolition, messageTemplateTo, applicationStatus);
			messageTemplateFrom = getMessageTemplate(applicationStatus + "_" + action + "_FROM", localizationMessage);
			messageFrom = getLocalizedMsg(demolition, messageTemplateFrom, applicationStatus);
			messageMap.put("messageTo", messageTo);
			messageMap.put("messageFrom", messageFrom);
			break;

		case BPAConstants.STATUS_REJECTED_SCN:
			messageTemplateTo = getMessageTemplate(applicationStatus + "_" + action + "_TO", localizationMessage);
			messageTo = getLocalizedMsg(demolition, messageTemplateTo, applicationStatus);
			messageTemplateFrom = getMessageTemplate(applicationStatus + "_" + action + "_FROM", localizationMessage);
			messageFrom = getLocalizedMsg(demolition, messageTemplateFrom, applicationStatus);
			messageMap.put("messageTo", messageTo);
			messageMap.put("messageFrom", messageFrom);
			break;

		case BPAConstants.STATUS_PENDING_SANC_FEE_PAYMENT:
			messageTemplateTo = getMessageTemplate(applicationStatus + "_" + action + "_TO", localizationMessage);
			messageTo = getLocalizedMsg(demolition, messageTemplateTo, applicationStatus);
			messageTemplateFrom = getMessageTemplate(applicationStatus + "_" + action + "_FROM", localizationMessage);
			messageFrom = getLocalizedMsg(demolition, messageTemplateFrom, applicationStatus);
			messageMap.put("messageTo", messageTo);
			messageMap.put("messageFrom", messageFrom);
			break;

		case BPAConstants.STATUS_CITIZEN_ACTION_PENDING_AT_DOC_VERIF:
			messageTemplateTo = getMessageTemplate(applicationStatus + "_" + action + "_TO", localizationMessage);
			messageTo = getLocalizedMsg(demolition, messageTemplateTo, applicationStatus);
			messageTemplateFrom = getMessageTemplate(applicationStatus + "_" + action + "_FROM", localizationMessage);
			messageFrom = getLocalizedMsg(demolition, messageTemplateFrom, applicationStatus);
			messageMap.put("messageTo", messageTo);
			messageMap.put("messageFrom", messageFrom);
			break;

		case BPAConstants.STATUS_CITIZEN_ACTION_PENDING_AT_APPROVAL:
			messageTemplateTo = getMessageTemplate(applicationStatus + "_" + action + "_TO", localizationMessage);
			messageTo = getLocalizedMsg(demolition, messageTemplateTo, applicationStatus);
			messageTemplateFrom = getMessageTemplate(applicationStatus + "_" + action + "_FROM", localizationMessage);
			messageFrom = getLocalizedMsg(demolition, messageTemplateFrom, applicationStatus);
			messageMap.put("messageTo", messageTo);
			messageMap.put("messageFrom", messageFrom);
			break;

		case BPAConstants.STATUS_PENDING_ARCHITECT_ACTION_FOR_REWORK:
			messageTemplateTo = getMessageTemplate(applicationStatus + "_" + action + "_TO", localizationMessage);
			messageTo = getLocalizedMsg(demolition, messageTemplateTo, applicationStatus);
			messageTemplateFrom = getMessageTemplate(applicationStatus + "_" + action + "_FROM", localizationMessage);
			messageFrom = getLocalizedMsg(demolition, messageTemplateFrom, applicationStatus);
			messageMap.put("messageTo", messageTo);
			messageMap.put("messageFrom", messageFrom);
			break;

		case BPAConstants.STATUS_APP_L2_VERIFICATION_INPROGRESS:
			messageTemplateTo = getMessageTemplate(applicationStatus + "_" + action + "_TO", localizationMessage);
			messageTo = getLocalizedMsg(demolition, messageTemplateTo, applicationStatus);
			messageTemplateFrom = getMessageTemplate(applicationStatus + "_" + action + "_FROM", localizationMessage);
			messageFrom = getLocalizedMsg(demolition, messageTemplateFrom, applicationStatus);
			messageMap.put("messageTo", messageTo);
			messageMap.put("messageFrom", messageFrom);
			break;

		case BPAConstants.STATUS_APP_L3_VERIFICATION_INPROGRESS:
			messageTemplateTo = getMessageTemplate(applicationStatus + "_" + action + "_TO", localizationMessage);
			messageTo = getLocalizedMsg(demolition, messageTemplateTo, applicationStatus);
			messageTemplateFrom = getMessageTemplate(applicationStatus + "_" + action + "_FROM", localizationMessage);
			messageFrom = getLocalizedMsg(demolition, messageTemplateFrom, applicationStatus);
			messageMap.put("messageTo", messageTo);
			messageMap.put("messageFrom", messageFrom);
			break;

		case BPAConstants.STATUS_APP_L4_VERIFICATION_INPROGRESS:
			messageTemplateTo = getMessageTemplate(applicationStatus + "_" + action + "_TO", localizationMessage);
			messageTo = getLocalizedMsg(demolition, messageTemplateTo, applicationStatus);
			messageTemplateFrom = getMessageTemplate(applicationStatus + "_" + action + "_FROM", localizationMessage);
			messageFrom = getLocalizedMsg(demolition, messageTemplateFrom, applicationStatus);
			messageMap.put("messageTo", messageTo);
			messageMap.put("messageFrom", messageFrom);
			break;

		case BPAConstants.STATUS_APP_L2_PENDING_DEPT_VERIFICATION_INPROGRESS:
			messageTemplateTo = getMessageTemplate(applicationStatus + "_" + action + "_TO", localizationMessage);
			messageTo = getLocalizedMsg(demolition, messageTemplateTo, applicationStatus);
			messageTemplateFrom = getMessageTemplate(applicationStatus + "_" + action + "_FROM", localizationMessage);
			messageFrom = getLocalizedMsg(demolition, messageTemplateFrom, applicationStatus);
			messageMap.put("messageTo", messageTo);
			messageMap.put("messageFrom", messageFrom);
			break;

		case BPAConstants.STATUS_APP_L3_PENDING_DEPT_VERIFICATION_INPROGRESS:
			messageTemplateTo = getMessageTemplate(applicationStatus + "_" + action + "_TO", localizationMessage);
			messageTo = getLocalizedMsg(demolition, messageTemplateTo, applicationStatus);
			messageTemplateFrom = getMessageTemplate(applicationStatus + "_" + action + "_FROM", localizationMessage);
			messageFrom = getLocalizedMsg(demolition, messageTemplateFrom, applicationStatus);
			messageMap.put("messageTo", messageTo);
			messageMap.put("messageFrom", messageFrom);
			break;

		case BPAConstants.STATUS_APP_L4_PENDING_DEPT_VERIFICATION_INPROGRESS:
			messageTemplateTo = getMessageTemplate(applicationStatus + "_" + action + "_TO", localizationMessage);
			messageTo = getLocalizedMsg(demolition, messageTemplateTo, applicationStatus);
			messageTemplateFrom = getMessageTemplate(applicationStatus + "_" + action + "_FROM", localizationMessage);
			messageFrom = getLocalizedMsg(demolition, messageTemplateFrom, applicationStatus);
			messageMap.put("messageTo", messageTo);
			messageMap.put("messageFrom", messageFrom);
			break;

		case BPAConstants.STATUS_SHOW_CAUSE_ISSUED:
			messageTemplateTo = getMessageTemplate(applicationStatus + "_" + action + "_TO", localizationMessage);
			messageTo = getLocalizedMsg(demolition, messageTemplateTo, applicationStatus);
			messageTemplateFrom = getMessageTemplate(applicationStatus + "_" + action + "_FROM", localizationMessage);
			messageFrom = getLocalizedMsg(demolition, messageTemplateFrom, applicationStatus);
			messageMap.put("messageTo", messageTo);
			messageMap.put("messageFrom", messageFrom);
			break;

		case BPAConstants.STATUS_SHOW_CAUSE_REPLY_VERIFICATION_PENDING:
			messageTemplateTo = getMessageTemplate(applicationStatus + "_" + action + "_TO", localizationMessage);
			messageTo = getLocalizedMsg(demolition, messageTemplateTo, applicationStatus);
			messageTemplateFrom = getMessageTemplate(applicationStatus + "_" + action + "_FROM", localizationMessage);
			messageFrom = getLocalizedMsg(demolition, messageTemplateFrom, applicationStatus);
			messageMap.put("messageTo", messageTo);
			messageMap.put("messageFrom", messageFrom);
			break;

		case BPAConstants.STATUS_APPROVAL_INPROGRESS:
			messageTemplateTo = getMessageTemplate(applicationStatus + "_" + action + "_TO", localizationMessage);
			messageTo = getLocalizedMsg(demolition, messageTemplateTo, applicationStatus);
			messageTemplateFrom = getMessageTemplate(applicationStatus + "_" + action + "_FROM", localizationMessage);
			messageFrom = getLocalizedMsg(demolition, messageTemplateFrom, applicationStatus);
			messageMap.put("messageTo", messageTo);
			messageMap.put("messageFrom", messageFrom);
			break;

		}

		return messageMap;
	}
	
	/**
	 * Extracts message for the specific code
	 * 
	 * @param notificationCode    The code for which message is required
	 * @param localizationMessage The localization messages
	 * @return message for the specific code
	 */
	@SuppressWarnings("rawtypes")
	public String getMessageTemplate(String notificationCode, String localizationMessage) {
		String path = "$..messages[?(@.code==\"{}\")].message";
		path = path.replace("{}", notificationCode);
		String message = null;
		try {
			List data = JsonPath.parse(localizationMessage).read(path);
			if (!CollectionUtils.isEmpty(data))
				message = data.get(0).toString();
			else
				log.error("Fetching from localization failed with code " + notificationCode);
		} catch (Exception e) {
			log.warn("Fetching from localization failed", e);
		}
		return message;
	}
	
	@SuppressWarnings("unchecked")
	private String getLocalizedMsg(Demolition demolition, String message, String status) {
		message = message.replace("<applicationno>", demolition.getApplicationNo());
		message = message.replace("<status>", statusMap.get(status));
		return message;
	}
	
	private static final Map<String, String> createStatusMap() {
        Map<String, String> map = new HashMap<>();
        map.put("PENDING_APPL_FEE", "Pending Application Fee");
        map.put("DOC_VERIFICATION_INPROGRESS", "Document Verification");
        map.put("FIELDINSPECTION_INPROGRESS", "Field Inspection");
        map.put("APPROVAL_INPROGRESS", "Approval");
        map.put("REJECTED_SCN", "Refusal Show Cause");
        map.put("PENDING_SANC_FEE_PAYMENT", "Field Inspection");
        map.put("APPROVED", "Approved");
        map.put("REJECTED", "Rejected");
        map.put("CITIZEN_ACTION_PENDING_AT_DOC_VERIF", "Doc Verifier");
        map.put("CITIZEN_ACTION_PENDING_AT_APPROVAL", "Approval");
        map.put("PENDING_ARCHITECT_ACTION_FOR_REWORK", "Rework");
        map.put("APP_L2_VERIFICATION_INPROGRESS", "L2 verification");
        map.put("APP_L3_VERIFICATION_INPROGRESS", "L3 verification");
        map.put("APP_L4_VERIFICATION_INPROGRESS", "L4 verification");
        map.put("SHOW_CAUSE_ISSUED", "Show Cause Issued");
        map.put("SHOW_CAUSE_REPLY_VERIFICATION_PENDING", "Show Cause Reply Verification");
        map.put("APP_L2_PENDING_DEPT_VERIFICATION_INPROGRESS", "L2 Department Verification");
        map.put("APP_L3_PENDING_DEPT_VERIFICATION_INPROGRESS", "L3 Department Verification");
        map.put("APP_L4_PENDING_DEPT_VERIFICATION_INPROGRESS", "L4 Department Verification");
        return Collections.unmodifiableMap(map);
    }
	
	public List<EmailRequest> createNewEmailRequest(DemolitionRequest demolitionRequest, String message, Map<String, String> emailToOwner,
			String subject) {
		log.info("inside method createNewEmailRequest");
		List<EmailRequest> emailRequest = new LinkedList<>();
		Set<String> strings = new LinkedHashSet<>();
		for (Map.Entry<String, String> entryset : emailToOwner.entrySet()) {
			String customizedMsg = message.replace("<1>", entryset.getValue());

			strings.add(entryset.getKey());

			emailRequest.add(new EmailRequest(demolitionRequest.getRequestInfo(), new Email(strings, subject, customizedMsg, true)));
		}

		log.info("method createNewEmailRequest execution ends");
		return emailRequest;
	}
	
	public List<EmailRequest> createNewEmailRequestV2(DemolitionRequest demolitionRequest, String message,
			String emailId, String subject) {
		log.info("inside method createNewEmailRequest");
		List<EmailRequest> emailRequest = new LinkedList<>();
		Set<String> emailIds = new LinkedHashSet<>();
		emailIds.add(emailId);
		emailRequest
				.add(new EmailRequest(demolitionRequest.getRequestInfo(), new Email(emailIds, subject, message, true)));
		log.info("method createNewEmailRequest execution ends");
		return emailRequest;
	}
	
	public void sendNewEmail(List<org.egov.bpa.web.model.EmailRequest> emailRequestList, boolean isEmailEnabled) {
		if (isEmailEnabled) {
			if (CollectionUtils.isEmpty(emailRequestList))
				log.info("Messages from localization couldn't be fetched!");
			for (EmailRequest emailRequest : emailRequestList) {
				log.info("pushing to email notif topic: "+config.getEmailNotifTopic());
				try {
					log.info("data pushed as json:"+mapper.writeValueAsString(emailRequest));
				} catch (JsonProcessingException e) {
					log.error("error while parsing emailRequest as json",e);
				}
				producer.push(config.getEmailNotifTopic(), emailRequest);
				log.info("EMail Ids: " + emailRequest.getEmail() + " Messages: " + emailRequest.getEmail().getBody());
			}
		}
	}

}
