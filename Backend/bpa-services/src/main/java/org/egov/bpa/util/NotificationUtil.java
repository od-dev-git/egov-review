package org.egov.bpa.util;

import static org.egov.bpa.util.BPAConstants.BILL_AMOUNT;

import java.math.BigDecimal;
import java.util.Collection;
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
import org.egov.bpa.service.EDCRService;
import org.egov.bpa.web.model.BPA;
import org.egov.bpa.web.model.BPARequest;
import org.egov.bpa.web.model.Email;
import org.egov.bpa.web.model.EmailRequest;
import org.egov.bpa.web.model.EventRequest;
import org.egov.bpa.web.model.RequestInfoWrapper;
import org.egov.bpa.web.model.SMSRequest;
import org.egov.bpa.web.model.regularization.Regularization;
import org.egov.bpa.web.model.regularization.RegularizationRequest;
import org.egov.common.contract.request.RequestInfo;
import org.egov.tracer.model.CustomException;
import org.json.JSONArray;
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
public class NotificationUtil {

	private BPAConfiguration config;

	private ServiceRequestRepository serviceRequestRepository;

	private Producer producer;

	private EDCRService edcrService;

	private BPAUtil bpaUtil;
	
	private ObjectMapper mapper;

	private RegularizationUtil regularizationUtil;
	
	public static final Map<String, String> statusMap = createStatusMap();

	@Autowired
	public NotificationUtil(BPAConfiguration config, ServiceRequestRepository serviceRequestRepository,
			Producer producer, BPAUtil bpaUtil, ObjectMapper mapper, RegularizationUtil regularizationUtil) {
		this.config = config;
		this.serviceRequestRepository = serviceRequestRepository;
		this.producer = producer;
		this.bpaUtil = bpaUtil;
		this.mapper = mapper;
		this.regularizationUtil = regularizationUtil;
	}

	final String receiptNumberKey = "receiptNumber";
	final String amountPaidKey = "amountPaid";
	
	public static final String MESSAGE_CODE = "MESSAGE_CODE";
	public static final String AMOUNT_TO_BE_PAID = "<AMOUNT_TO_BE_PAID>";
	public static final String _DELIMITER = "_";
	public static final String REGULARIZATION = "Regularization";
	

	/**
	 * Creates customized message based on bpa
	 * 
	 * @param bpa                 The bpa for which message is to be sent
	 * @param localizationMessage The messages from localization
	 * @return customized message based on bpa
	 */
	@SuppressWarnings("unchecked")
	public String getCustomizedMsg(RequestInfo requestInfo, BPA bpa, String localizationMessage, Map<String, String> edcrResponse) {
		String message = null, messageTemplate;
		BPARequest bpaRequest = new BPARequest();
		bpaRequest.setBPA(bpa);
		bpaRequest.setRequestInfo(requestInfo);
		String applicationType = edcrResponse.get(BPAConstants.APPLICATIONTYPE);
		String serviceType = edcrResponse.get(BPAConstants.SERVICETYPE);

		if (bpa.getStatus().toString().toUpperCase().equals(BPAConstants.STATUS_REJECTED)) {
			messageTemplate = getMessageTemplate(
					applicationType + "_" + serviceType + "_" + BPAConstants.STATUS_REJECTED, localizationMessage);
			log.info("SMS Message code, if rejected : " + applicationType + "_" + serviceType + "_" + BPAConstants.STATUS_REJECTED);
			message = getInitiatedMsg(bpa, messageTemplate, serviceType);
		} else {

			String messageCode = applicationType + "_" + serviceType + "_" + bpa.getWorkflow().getAction() + "_"
					+ bpa.getStatus();
			log.info("SMS notification message code : " + messageCode);

			messageTemplate = getMessageTemplate(messageCode, localizationMessage);
			if (!StringUtils.isEmpty(messageTemplate)) {
				message = getInitiatedMsg(bpa, messageTemplate, serviceType);

				if (message.contains("<AMOUNT_TO_BE_PAID>")) {
					BigDecimal amount = getAmountToBePaid(requestInfo, bpa);
					message = message.replace("<AMOUNT_TO_BE_PAID>", amount.toString());
				}
			}
		}
		return message;
	}

	@SuppressWarnings("unchecked")
	public String getCustomizedEmailMsg(RequestInfo requestInfo, BPA bpa, String localizationMessage,Map<String, String> edcrResponse) {
		log.info("inside method getCustomizedEmailMsg");
		String message = null, messageTemplate;
		BPARequest bpaRequest = new BPARequest();
		bpaRequest.setBPA(bpa);
		bpaRequest.setRequestInfo(requestInfo);
		String applicationType = edcrResponse.get(BPAConstants.APPLICATIONTYPE);
		String serviceType = edcrResponse.get(BPAConstants.SERVICETYPE);
		// String applicationType = "BUILDING_PLAN_SCRUTINY";
		// String serviceType ="NEW_CONSTRUCTION";
		// bpa.setStatus("INITIATED");

		if (bpa.getStatus().toString().toUpperCase().equals(BPAConstants.STATUS_REJECTED)) {
			messageTemplate = getMessageTemplate(
					applicationType + "_" + serviceType + "_" + BPAConstants.STATUS_REJECTED,
					localizationMessage + "_EMAIL");
			log.info("messageTemplate for rejected status: "+messageTemplate);
			message = getInitiatedMsg(bpa, messageTemplate, serviceType);
			log.info("message for rejected status: "+message);
		} else {

			String messageCode = applicationType + "_" + serviceType + "_" + bpa.getWorkflow().getAction() + "_"
					+ bpa.getStatus() + "_EMAIL";
			log.info("messageCode for else block: "+messageCode);

			messageTemplate = getMessageTemplate(messageCode, localizationMessage);
			log.info("messageTemplate for else block: "+messageTemplate);
			//System.out.println("localizationMessage::: " + localizationMessage);
			if (!StringUtils.isEmpty(messageTemplate)) {
				message = getInitiatedMsg(bpa, messageTemplate, serviceType);
				log.info("message: "+message);

				if (message.contains("<AMOUNT_TO_BE_PAID>")) {
					BigDecimal amount = getAmountToBePaid(requestInfo, bpa);
					message = message.replace("<AMOUNT_TO_BE_PAID>", amount.toString());
					log.info("message after replaceing amount to be paid:"+message);
				}
			}
		}
		return message;
	}

	@SuppressWarnings("unchecked")
	// As per OAP-304, keeping the same messages for Events and SMS, so removed
	// "M_" prefix for the localization codes.
	// so it will be same as the getCustomizedMsg
	public String getEventsCustomizedMsg(RequestInfo requestInfo, BPA bpa, String localizationMessage, Map<String, String> edcrResponse) {
		String message = null, messageTemplate;
		BPARequest bpaRequest = new BPARequest();
		bpaRequest.setBPA(bpa);
		bpaRequest.setRequestInfo(requestInfo);
		String applicationType = edcrResponse.get(BPAConstants.APPLICATIONTYPE);
		String serviceType = edcrResponse.get(BPAConstants.SERVICETYPE);

		if (bpa.getStatus().toString().toUpperCase().equals(BPAConstants.STATUS_REJECTED)) {
			messageTemplate = getMessageTemplate(BPAConstants.M_APP_REJECTED, localizationMessage);
			message = getInitiatedMsg(bpa, messageTemplate, serviceType);
		} else {
			String messageCode = applicationType + "_" + serviceType + "_" + bpa.getWorkflow().getAction() + "_"
					+ bpa.getStatus();
			messageTemplate = getMessageTemplate(messageCode, localizationMessage);
			if (!StringUtils.isEmpty(messageTemplate)) {
				message = getInitiatedMsg(bpa, messageTemplate, serviceType);
				if (message.contains("<AMOUNT_TO_BE_PAID>")) {
					BigDecimal amount = getAmountToBePaid(requestInfo, bpa);
					message = message.replace("<AMOUNT_TO_BE_PAID>", amount.toString());
				}
			}
		}
		return message;

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

	/**
	 * Fetches the amount to be paid from getBill API
	 * 
	 * @param requestInfo The RequestInfo of the request
	 * @param license     The TradeLicense object for which
	 * @return
	 */
	public BigDecimal getAmountToBePaid(RequestInfo requestInfo, BPA bpa) {

		LinkedHashMap responseMap = (LinkedHashMap) serviceRequestRepository.fetchResult(bpaUtil.getBillUri(bpa),
				new RequestInfoWrapper(requestInfo));
		JSONObject jsonObject = new JSONObject(responseMap);
		BigDecimal amountToBePaid;
		double amount = 0.0;
		try {
			JSONArray demandArray = (JSONArray) jsonObject.get("Demands");
			if (demandArray != null) {
				JSONObject firstElement = (JSONObject) demandArray.get(0);
				if (firstElement != null) {
					JSONArray demandDetails = (JSONArray) firstElement.get("demandDetails");
					if (demandDetails != null) {
						for (int i = 0; i < demandDetails.length(); i++) {
							JSONObject object = (JSONObject) demandDetails.get(i);
							Double taxAmt = Double.valueOf((object.get("taxAmount").toString()));
							amount = amount + taxAmt;
						}
					}
				}
			}
			amountToBePaid = BigDecimal.valueOf(amount);
		} catch (Exception e) {
			throw new CustomException("PARSING ERROR", "Failed to parse the response using jsonPath: " + BILL_AMOUNT);
		}
		return amountToBePaid;
	}

	/**
	 * Returns the uri for the localization call
	 * 
	 * @param tenantId TenantId of the propertyRequest
	 * @return The uri for localization search call
	 */
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

	/**
	 * Fetches messages from localization service
	 * 
	 * @param tenantId    tenantId of the BPA
	 * @param requestInfo The requestInfo of the request
	 * @return Localization messages for the module
	 */
	@SuppressWarnings("rawtypes")
	public String getLocalizationMessages(String tenantId, RequestInfo requestInfo) {

		LinkedHashMap responseMap = (LinkedHashMap) serviceRequestRepository.fetchResult(getUri(tenantId, requestInfo),
				requestInfo);
		String jsonString = new JSONObject(responseMap).toString();
		return jsonString;
	}

	/**
	 * Creates customized message for initiate
	 * 
	 * @param bpa     tenantId of the bpa
	 * @param message Message from localization for initiate
	 * @return customized message for initiate
	 */
	@SuppressWarnings("unchecked")
	private String getInitiatedMsg(BPA bpa, String message, String serviceType) {
		message = message.replace("<2>", serviceType);
		message = message.replace("<3>", bpa.getApplicationNo());
		return message;
	}

	/**
	 * Send the SMSRequest on the SMSNotification kafka topic
	 * 
	 * @param smsRequestList The list of SMSRequest to be sent
	 */
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

	/**
	 * Creates sms request for the each owners
	 * 
	 * @param message                 The message for the specific bpa
	 * @param mobileNumberToOwnerName Map of mobileNumber to OwnerName
	 * @return List of SMSRequest
	 */
	public List<SMSRequest> createSMSRequest(String message, Map<String, String> mobileNumberToOwner) {
		List<SMSRequest> smsRequest = new LinkedList<>();

		for (Map.Entry<String, String> entryset : mobileNumberToOwner.entrySet()) {
			String customizedMsg = message.replace("<1>", entryset.getValue());
			smsRequest.add(new SMSRequest(entryset.getKey(), customizedMsg));
		}
		return smsRequest;
	}

	public List<EmailRequest> createNewEmailRequest(BPARequest bpa, String message, Map<String, String> emailToOwner,
			String subject) {
		log.info("inside method createNewEmailRequest");
		List<EmailRequest> emailRequest = new LinkedList<>();
		Set<String> strings = new LinkedHashSet<>();
		for (Map.Entry<String, String> entryset : emailToOwner.entrySet()) {
			String customizedMsg = message.replace("<1>", entryset.getValue());

			strings.add(entryset.getKey());

			emailRequest.add(new EmailRequest(bpa.getRequestInfo(), new Email(strings, subject, customizedMsg, true)));
		}

		log.info("method createNewEmailRequest execution ends");
		return emailRequest;
	}

	/**
	 * Pushes the event request to Kafka Queue.
	 * 
	 * @param request
	 */
	public void sendEventNotification(EventRequest request) {
		producer.push(config.getSaveUserEventsTopic(), request);

		log.debug("STAKEHOLDER:: " + request.getEvents().get(0).getDescription());
	}

	

	/**
	 * @param requestInfo
	 * @param regularization
	 * @param localizationMessages
	 * @return CustomizedMsg
	 */
	public String getRegularizationCustomizedMsg(RequestInfo requestInfo, Regularization regularization,
			String localizationMessage) {
		String message = null, messageTemplate;
		String messageCode = BPAConstants.REGULARIZATION_MESSAGE_CODE_PREFIX + _DELIMITER
				+ MESSAGE_CODE + _DELIMITER + BPAConstants.REGULARIZATION_MESSAGE_CODE_SUFFIX;
		
		if (regularization.getStatus().toString().toUpperCase().equals(BPAConstants.STATUS_REJECTED)) {
			messageCode = messageCode.replace(MESSAGE_CODE, BPAConstants.STATUS_REJECTED);
			log.info("SMS Message code, if rejected : " + messageCode);
		} else {
			messageCode = messageCode.replace(MESSAGE_CODE, regularization.getWorkflow().getAction() + _DELIMITER + regularization.getStatus());
			log.info("SMS notification message code : " + messageCode);
		}
		
		messageTemplate = getMessageTemplate(messageCode, localizationMessage);
		if (!StringUtils.isEmpty(messageTemplate)) {
			message = getRegularizationInitiatedMsg(regularization, messageTemplate);

			if (message.contains(AMOUNT_TO_BE_PAID)) {
				BigDecimal amount = regularizationUtil.getAmountToBePaid(requestInfo, regularization);
				message = message.replace(AMOUNT_TO_BE_PAID, amount.toString());
			}
		}
		return message;
	}


	private String getRegularizationInitiatedMsg(Regularization regularization, String message) {
		String serviceType = regularization.getServiceType().replace(REGULARIZATION, StringUtils.EMPTY);
		message = message.replace("<2>", serviceType);
		message = message.replace("<3>", regularization.getApplicationNo());
		return message;
	}

	
	/**
	 * 
	 * @param requestInfo
	 * @param regularization
	 * @param localizationMessages
	 * @return Regularization Customized Email Msg
	 */
	public String getRegularizationCustomizedEmailMsg(RequestInfo requestInfo, Regularization regularization,
			String localizationMessages) {
		String message = null, messageTemplate;
        String messageCode = BPAConstants.REGULARIZATION_MESSAGE_CODE_PREFIX + _DELIMITER
                +  MESSAGE_CODE + _DELIMITER + BPAConstants.REGULARIZATION_MESSAGE_CODE_SUFFIX;

        if (regularization.getStatus().toString().toUpperCase().equals(BPAConstants.STATUS_REJECTED)) {
            messageCode = messageCode.replace(MESSAGE_CODE, BPAConstants.STATUS_REJECTED);
            log.info("SMS Message code, if rejected : " + messageCode);
        } else {
            messageCode = messageCode.replace(MESSAGE_CODE, regularization.getWorkflow().getAction() + _DELIMITER + regularization.getStatus());
            log.info("SMS notification message code : " + messageCode);
        }

        messageTemplate = getMessageTemplate(messageCode, localizationMessages);
        if (!StringUtils.isEmpty(messageTemplate)) {
            message = getRegularizationInitiatedMsg(regularization, messageTemplate);

            if (message.contains(AMOUNT_TO_BE_PAID)) {
                BigDecimal amount = regularizationUtil.getAmountToBePaid(requestInfo, regularization);
                message = message.replace(AMOUNT_TO_BE_PAID, amount.toString());
            }
        }
        
        return message;
	}

	
	/**
	 * @param regularizationRequest
	 * @param message
	 * @param emailToOwner
	 * @param customisedSubject
	 * @return
	 */
	public List<EmailRequest> createNewRegularizationEmailRequest( RegularizationRequest regularizationRequest, 
			String message, Map<String, String> emailToOwner, String subject) {
		log.info("inside method createNewEmailRequest");
        List<EmailRequest> emailRequest = new LinkedList<>();
        Set<String> strings = new LinkedHashSet<>();
        for (Map.Entry<String, String> entryset : emailToOwner.entrySet()) {
            String customizedMsg = message.replace("<1>", entryset.getValue());

            strings.add(entryset.getKey());

            emailRequest.add(new EmailRequest(regularizationRequest.getRequestInfo(), new Email(strings, subject, customizedMsg, true)));
        }

        log.info("method createNewEmailRequest execution ends");
        return emailRequest;
	}
	
	@SuppressWarnings("unchecked")
	public String getCustomizedEmailMsgEmployee(RequestInfo requestInfo, BPA bpa, String localizationMessage,Map<String, String> edcrResponse) {
		log.info("inside method getCustomizedEmailMsg");
		String message = null, messageTemplate;
		BPARequest bpaRequest = new BPARequest();
		bpaRequest.setBPA(bpa);
		bpaRequest.setRequestInfo(requestInfo);
		String applicationType = edcrResponse.get(BPAConstants.APPLICATIONTYPE);
		String serviceType = edcrResponse.get(BPAConstants.SERVICETYPE);

		if (bpa.getStatus().toString().toUpperCase().equals(BPAConstants.STATUS_APPROVED)) {
			messageTemplate = getMessageTemplate(
					applicationType + "_" + serviceType + "_" + BPAConstants.STATUS_APPROVED,
					localizationMessage + "_EMAIL");
			log.info("messageTemplate for approved status: "+messageTemplate);
			message = getInitiatedMsg(bpa, messageTemplate, serviceType);
			log.info("message for appro status: "+message);
		}
		if (bpa.getStatus().toString().toUpperCase().equals(BPAConstants.STATUS_APPLIED)) {
			messageTemplate = getMessageTemplate(
					applicationType + "_" + serviceType + "_" + BPAConstants.STATUS_APPLIED,
					localizationMessage + "_EMAIL");
			log.info("messageTemplate for applied status: "+messageTemplate);
			message = getInitiatedMsg(bpa, messageTemplate, serviceType);
			log.info("message for applied status: "+message);
		}
		if (bpa.getStatus().toString().toUpperCase().equals(BPAConstants.STATUS_DOCUMENTVERIFICATION)) {
			messageTemplate = getMessageTemplate(
					applicationType + "_" + serviceType + "_" + BPAConstants.STATUS_DOCUMENTVERIFICATION,
					localizationMessage + "_EMAIL");
			log.info("messageTemplate for document verification status: "+messageTemplate);
			message = getInitiatedMsg(bpa, messageTemplate, serviceType);
			log.info("message for document verification status: "+message);
		}
		if (bpa.getStatus().toString().toUpperCase().equals(BPAConstants.STATUS_FIELDINSPECTION)) {
			messageTemplate = getMessageTemplate(
					applicationType + "_" + serviceType + "_" + BPAConstants.STATUS_FIELDINSPECTION,
					localizationMessage + "_EMAIL");
			log.info("messageTemplate for field inspection status: "+messageTemplate);
			message = getInitiatedMsg(bpa, messageTemplate, serviceType);
			log.info("message for field inspection status: "+message);
		}else {

			String messageCode = applicationType + "_" + serviceType + "_" + bpa.getWorkflow().getAction() + "_"
					+ bpa.getStatus() + "_EMAIL";
			log.info("messageCode for else block: "+messageCode);

			messageTemplate = getMessageTemplate(messageCode, localizationMessage);
			log.info("messageTemplate for else block: "+messageTemplate);
			//System.out.println("localizationMessage::: " + localizationMessage);
			if (!StringUtils.isEmpty(messageTemplate)) {
				message = getInitiatedMsg(bpa, messageTemplate, serviceType);
				log.info("message: "+message);

				if (message.contains("<AMOUNT_TO_BE_PAID>")) {
					BigDecimal amount = getAmountToBePaid(requestInfo, bpa);
					message = message.replace("<AMOUNT_TO_BE_PAID>", amount.toString());
					log.info("message after replaceing amount to be paid:"+message);
				}
			}
		}
		return message;
	}
	
	@SuppressWarnings("unchecked")
	public Map<String, String> getCustomizedMsgForEmployee(RequestInfo requestInfo, BPA bpa,
			String localizationMessage) {
		Map<String, String> messageMap = new HashMap<String, String>();
		String message = null, messageTemplate;
		String messageTo = null;
		String messageFrom = null;
		String messageTemplateTo = null;
		String messageTemplateFrom = null;

		String applicationStatus = bpa.getStatus().toString().toUpperCase();
		String action = bpa.getWorkflow().getAction().toString().toUpperCase();
		log.info(bpa.getApplicationNo() + " - " + applicationStatus + "_" + action);
		switch (applicationStatus) {
		case BPAConstants.STATUS_DOC_VERIFICATION_INPROGRESS:
			messageTemplateTo = getMessageTemplate(applicationStatus + "_" + action + "_TO", localizationMessage);
			messageTo = getLocalizedMsg(bpa, messageTemplateTo, applicationStatus);
			messageTemplateFrom = getMessageTemplate(applicationStatus + "_" + action + "_FROM", localizationMessage);
			messageFrom = getLocalizedMsg(bpa, messageTemplateFrom, applicationStatus);
			messageMap.put("messageTo", messageTo);
			messageMap.put("messageFrom", messageFrom);
			break;

		case BPAConstants.STATUS_FIELDINSPECTION_INPROGRESS:
			messageTemplateTo = getMessageTemplate(applicationStatus + "_" + action + "_TO", localizationMessage);
			messageTo = getLocalizedMsg(bpa, messageTemplateTo, applicationStatus);
			messageTemplateFrom = getMessageTemplate(applicationStatus + "_" + action + "_FROM", localizationMessage);
			messageFrom = getLocalizedMsg(bpa, messageTemplateFrom, applicationStatus);
			messageMap.put("messageTo", messageTo);
			messageMap.put("messageFrom", messageFrom);
			break;

		case BPAConstants.STATUS_REJECTED_SCN:
			messageTemplateTo = getMessageTemplate(applicationStatus + "_" + action + "_TO", localizationMessage);
			messageTo = getLocalizedMsg(bpa, messageTemplateTo, applicationStatus);
			messageTemplateFrom = getMessageTemplate(applicationStatus + "_" + action + "_FROM", localizationMessage);
			messageFrom = getLocalizedMsg(bpa, messageTemplateFrom, applicationStatus);
			messageMap.put("messageTo", messageTo);
			messageMap.put("messageFrom", messageFrom);
			break;

		case BPAConstants.STATUS_PENDING_SANC_FEE_PAYMENT:
			messageTemplateTo = getMessageTemplate(applicationStatus + "_" + action + "_TO", localizationMessage);
			messageTo = getLocalizedMsg(bpa, messageTemplateTo, applicationStatus);
			messageTemplateFrom = getMessageTemplate(applicationStatus + "_" + action + "_FROM", localizationMessage);
			messageFrom = getLocalizedMsg(bpa, messageTemplateFrom, applicationStatus);
			messageMap.put("messageTo", messageTo);
			messageMap.put("messageFrom", messageFrom);
			break;

		case BPAConstants.STATUS_CITIZEN_ACTION_PENDING_AT_DOC_VERIF:
			messageTemplateTo = getMessageTemplate(applicationStatus + "_" + action + "_TO", localizationMessage);
			messageTo = getLocalizedMsg(bpa, messageTemplateTo, applicationStatus);
			messageTemplateFrom = getMessageTemplate(applicationStatus + "_" + action + "_FROM", localizationMessage);
			messageFrom = getLocalizedMsg(bpa, messageTemplateFrom, applicationStatus);
			messageMap.put("messageTo", messageTo);
			messageMap.put("messageFrom", messageFrom);
			break;

		case BPAConstants.STATUS_CITIZEN_ACTION_PENDING_AT_APPROVAL:
			messageTemplateTo = getMessageTemplate(applicationStatus + "_" + action + "_TO", localizationMessage);
			messageTo = getLocalizedMsg(bpa, messageTemplateTo, applicationStatus);
			messageTemplateFrom = getMessageTemplate(applicationStatus + "_" + action + "_FROM", localizationMessage);
			messageFrom = getLocalizedMsg(bpa, messageTemplateFrom, applicationStatus);
			messageMap.put("messageTo", messageTo);
			messageMap.put("messageFrom", messageFrom);
			break;

		case BPAConstants.STATUS_PENDING_ARCHITECT_ACTION_FOR_REWORK:
			messageTemplateTo = getMessageTemplate(applicationStatus + "_" + action + "_TO", localizationMessage);
			messageTo = getLocalizedMsg(bpa, messageTemplateTo, applicationStatus);
			messageTemplateFrom = getMessageTemplate(applicationStatus + "_" + action + "_FROM", localizationMessage);
			messageFrom = getLocalizedMsg(bpa, messageTemplateFrom, applicationStatus);
			messageMap.put("messageTo", messageTo);
			messageMap.put("messageFrom", messageFrom);
			break;

		case BPAConstants.STATUS_APP_L2_VERIFICATION_INPROGRESS:
			messageTemplateTo = getMessageTemplate(applicationStatus + "_" + action + "_TO", localizationMessage);
			messageTo = getLocalizedMsg(bpa, messageTemplateTo, applicationStatus);
			messageTemplateFrom = getMessageTemplate(applicationStatus + "_" + action + "_FROM", localizationMessage);
			messageFrom = getLocalizedMsg(bpa, messageTemplateFrom, applicationStatus);
			messageMap.put("messageTo", messageTo);
			messageMap.put("messageFrom", messageFrom);
			break;

		case BPAConstants.STATUS_APP_L3_VERIFICATION_INPROGRESS:
			messageTemplateTo = getMessageTemplate(applicationStatus + "_" + action + "_TO", localizationMessage);
			messageTo = getLocalizedMsg(bpa, messageTemplateTo, applicationStatus);
			messageTemplateFrom = getMessageTemplate(applicationStatus + "_" + action + "_FROM", localizationMessage);
			messageFrom = getLocalizedMsg(bpa, messageTemplateFrom, applicationStatus);
			messageMap.put("messageTo", messageTo);
			messageMap.put("messageFrom", messageFrom);
			break;

		case BPAConstants.STATUS_APP_L4_VERIFICATION_INPROGRESS:
			messageTemplateTo = getMessageTemplate(applicationStatus + "_" + action + "_TO", localizationMessage);
			messageTo = getLocalizedMsg(bpa, messageTemplateTo, applicationStatus);
			messageTemplateFrom = getMessageTemplate(applicationStatus + "_" + action + "_FROM", localizationMessage);
			messageFrom = getLocalizedMsg(bpa, messageTemplateFrom, applicationStatus);
			messageMap.put("messageTo", messageTo);
			messageMap.put("messageFrom", messageFrom);
			break;

		case BPAConstants.STATUS_APP_L2_PENDING_DEPT_VERIFICATION_INPROGRESS:
			messageTemplateTo = getMessageTemplate(applicationStatus + "_" + action + "_TO", localizationMessage);
			messageTo = getLocalizedMsg(bpa, messageTemplateTo, applicationStatus);
			messageTemplateFrom = getMessageTemplate(applicationStatus + "_" + action + "_FROM", localizationMessage);
			messageFrom = getLocalizedMsg(bpa, messageTemplateFrom, applicationStatus);
			messageMap.put("messageTo", messageTo);
			messageMap.put("messageFrom", messageFrom);
			break;

		case BPAConstants.STATUS_APP_L3_PENDING_DEPT_VERIFICATION_INPROGRESS:
			messageTemplateTo = getMessageTemplate(applicationStatus + "_" + action + "_TO", localizationMessage);
			messageTo = getLocalizedMsg(bpa, messageTemplateTo, applicationStatus);
			messageTemplateFrom = getMessageTemplate(applicationStatus + "_" + action + "_FROM", localizationMessage);
			messageFrom = getLocalizedMsg(bpa, messageTemplateFrom, applicationStatus);
			messageMap.put("messageTo", messageTo);
			messageMap.put("messageFrom", messageFrom);
			break;

		case BPAConstants.STATUS_APP_L4_PENDING_DEPT_VERIFICATION_INPROGRESS:
			messageTemplateTo = getMessageTemplate(applicationStatus + "_" + action + "_TO", localizationMessage);
			messageTo = getLocalizedMsg(bpa, messageTemplateTo, applicationStatus);
			messageTemplateFrom = getMessageTemplate(applicationStatus + "_" + action + "_FROM", localizationMessage);
			messageFrom = getLocalizedMsg(bpa, messageTemplateFrom, applicationStatus);
			messageMap.put("messageTo", messageTo);
			messageMap.put("messageFrom", messageFrom);
			break;

		case BPAConstants.STATUS_SHOW_CAUSE_ISSUED:
			messageTemplateTo = getMessageTemplate(applicationStatus + "_" + action + "_TO", localizationMessage);
			messageTo = getLocalizedMsg(bpa, messageTemplateTo, applicationStatus);
			messageTemplateFrom = getMessageTemplate(applicationStatus + "_" + action + "_FROM", localizationMessage);
			messageFrom = getLocalizedMsg(bpa, messageTemplateFrom, applicationStatus);
			messageMap.put("messageTo", messageTo);
			messageMap.put("messageFrom", messageFrom);
			break;

		case BPAConstants.STATUS_SHOW_CAUSE_REPLY_VERIFICATION_PENDING:
			messageTemplateTo = getMessageTemplate(applicationStatus + "_" + action + "_TO", localizationMessage);
			messageTo = getLocalizedMsg(bpa, messageTemplateTo, applicationStatus);
			messageTemplateFrom = getMessageTemplate(applicationStatus + "_" + action + "_FROM", localizationMessage);
			messageFrom = getLocalizedMsg(bpa, messageTemplateFrom, applicationStatus);
			messageMap.put("messageTo", messageTo);
			messageMap.put("messageFrom", messageFrom);
			break;

		case BPAConstants.STATUS_APPROVAL_INPROGRESS:
			messageTemplateTo = getMessageTemplate(applicationStatus + "_" + action + "_TO", localizationMessage);
			messageTo = getLocalizedMsg(bpa, messageTemplateTo, applicationStatus);
			messageTemplateFrom = getMessageTemplate(applicationStatus + "_" + action + "_FROM", localizationMessage);
			messageFrom = getLocalizedMsg(bpa, messageTemplateFrom, applicationStatus);
			messageMap.put("messageTo", messageTo);
			messageMap.put("messageFrom", messageFrom);
			break;

		}

		return messageMap;
	}
		
	@SuppressWarnings("unchecked")
	private String getLocalizedMsg(BPA bpa, String message, String status) {
		message = message.replace("<applicationno>", bpa.getApplicationNo());
		message = message.replace("<status>", statusMap.get(status));
		return message;
	}
	
	@SuppressWarnings("unchecked")
	private String getLocalizedMsg(Regularization regularization, String message, String status) {
		message = message.replace("<applicationno>", regularization.getApplicationNo());
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

	public List<SMSRequest> createSMSRequestForEmployee(Map<String, String> employeeNumberWithMsg) {
		List<SMSRequest> smsRequest = new LinkedList<>();

		for (Map.Entry<String, String> entryset : employeeNumberWithMsg.entrySet()) {
			smsRequest.add(new SMSRequest(entryset.getKey(), entryset.getValue()));
		}
		return smsRequest;
	}
	
	@SuppressWarnings("unchecked")
	public Map<String, String> getCustomizedMsgForEmployee(RequestInfo requestInfo, Regularization regularization,
			String localizationMessage) {
		Map<String, String> messageMap = new HashMap<String, String>();
		String message = null, messageTemplate;
		String messageTo = null;
		String messageFrom = null;
		String messageTemplateTo = null;
		String messageTemplateFrom = null;

		String applicationStatus = regularization.getStatus().toString().toUpperCase();
		String action = regularization.getWorkflow().getAction().toString().toUpperCase();
		log.info(regularization.getApplicationNo() + " - " + applicationStatus + "_" + action);
		switch (applicationStatus) {
		case BPAConstants.STATUS_DOC_VERIFICATION_INPROGRESS:
			messageTemplateTo = getMessageTemplate(applicationStatus + "_" + action + "_TO", localizationMessage);
			messageTo = getLocalizedMsg(regularization, messageTemplateTo, applicationStatus);
			messageTemplateFrom = getMessageTemplate(applicationStatus + "_" + action + "_FROM", localizationMessage);
			messageFrom = getLocalizedMsg(regularization, messageTemplateFrom, applicationStatus);
			messageMap.put("messageTo", messageTo);
			messageMap.put("messageFrom", messageFrom);
			break;

		case BPAConstants.STATUS_FIELDINSPECTION_INPROGRESS:
			messageTemplateTo = getMessageTemplate(applicationStatus + "_" + action + "_TO", localizationMessage);
			messageTo = getLocalizedMsg(regularization, messageTemplateTo, applicationStatus);
			messageTemplateFrom = getMessageTemplate(applicationStatus + "_" + action + "_FROM", localizationMessage);
			messageFrom = getLocalizedMsg(regularization, messageTemplateFrom, applicationStatus);
			messageMap.put("messageTo", messageTo);
			messageMap.put("messageFrom", messageFrom);
			break;

		case BPAConstants.STATUS_REJECTED_SCN:
			messageTemplateTo = getMessageTemplate(applicationStatus + "_" + action + "_TO", localizationMessage);
			messageTo = getLocalizedMsg(regularization, messageTemplateTo, applicationStatus);
			messageTemplateFrom = getMessageTemplate(applicationStatus + "_" + action + "_FROM", localizationMessage);
			messageFrom = getLocalizedMsg(regularization, messageTemplateFrom, applicationStatus);
			messageMap.put("messageTo", messageTo);
			messageMap.put("messageFrom", messageFrom);
			break;

		case BPAConstants.STATUS_PENDING_SANC_FEE_PAYMENT:
			messageTemplateTo = getMessageTemplate(applicationStatus + "_" + action + "_TO", localizationMessage);
			messageTo = getLocalizedMsg(regularization, messageTemplateTo, applicationStatus);
			messageTemplateFrom = getMessageTemplate(applicationStatus + "_" + action + "_FROM", localizationMessage);
			messageFrom = getLocalizedMsg(regularization, messageTemplateFrom, applicationStatus);
			messageMap.put("messageTo", messageTo);
			messageMap.put("messageFrom", messageFrom);
			break;

		case BPAConstants.STATUS_CITIZEN_ACTION_PENDING_AT_DOC_VERIF:
			messageTemplateTo = getMessageTemplate(applicationStatus + "_" + action + "_TO", localizationMessage);
			messageTo = getLocalizedMsg(regularization, messageTemplateTo, applicationStatus);
			messageTemplateFrom = getMessageTemplate(applicationStatus + "_" + action + "_FROM", localizationMessage);
			messageFrom = getLocalizedMsg(regularization, messageTemplateFrom, applicationStatus);
			messageMap.put("messageTo", messageTo);
			messageMap.put("messageFrom", messageFrom);
			break;

		case BPAConstants.STATUS_CITIZEN_ACTION_PENDING_AT_APPROVAL:
			messageTemplateTo = getMessageTemplate(applicationStatus + "_" + action + "_TO", localizationMessage);
			messageTo = getLocalizedMsg(regularization, messageTemplateTo, applicationStatus);
			messageTemplateFrom = getMessageTemplate(applicationStatus + "_" + action + "_FROM", localizationMessage);
			messageFrom = getLocalizedMsg(regularization, messageTemplateFrom, applicationStatus);
			messageMap.put("messageTo", messageTo);
			messageMap.put("messageFrom", messageFrom);
			break;

		case BPAConstants.STATUS_PENDING_ARCHITECT_ACTION_FOR_REWORK:
			messageTemplateTo = getMessageTemplate(applicationStatus + "_" + action + "_TO", localizationMessage);
			messageTo = getLocalizedMsg(regularization, messageTemplateTo, applicationStatus);
			messageTemplateFrom = getMessageTemplate(applicationStatus + "_" + action + "_FROM", localizationMessage);
			messageFrom = getLocalizedMsg(regularization, messageTemplateFrom, applicationStatus);
			messageMap.put("messageTo", messageTo);
			messageMap.put("messageFrom", messageFrom);
			break;

		case BPAConstants.STATUS_APP_L2_VERIFICATION_INPROGRESS:
			messageTemplateTo = getMessageTemplate(applicationStatus + "_" + action + "_TO", localizationMessage);
			messageTo = getLocalizedMsg(regularization, messageTemplateTo, applicationStatus);
			messageTemplateFrom = getMessageTemplate(applicationStatus + "_" + action + "_FROM", localizationMessage);
			messageFrom = getLocalizedMsg(regularization, messageTemplateFrom, applicationStatus);
			messageMap.put("messageTo", messageTo);
			messageMap.put("messageFrom", messageFrom);
			break;

		case BPAConstants.STATUS_APP_L3_VERIFICATION_INPROGRESS:
			messageTemplateTo = getMessageTemplate(applicationStatus + "_" + action + "_TO", localizationMessage);
			messageTo = getLocalizedMsg(regularization, messageTemplateTo, applicationStatus);
			messageTemplateFrom = getMessageTemplate(applicationStatus + "_" + action + "_FROM", localizationMessage);
			messageFrom = getLocalizedMsg(regularization, messageTemplateFrom, applicationStatus);
			messageMap.put("messageTo", messageTo);
			messageMap.put("messageFrom", messageFrom);
			break;

		case BPAConstants.STATUS_APP_L4_VERIFICATION_INPROGRESS:
			messageTemplateTo = getMessageTemplate(applicationStatus + "_" + action + "_TO", localizationMessage);
			messageTo = getLocalizedMsg(regularization, messageTemplateTo, applicationStatus);
			messageTemplateFrom = getMessageTemplate(applicationStatus + "_" + action + "_FROM", localizationMessage);
			messageFrom = getLocalizedMsg(regularization, messageTemplateFrom, applicationStatus);
			messageMap.put("messageTo", messageTo);
			messageMap.put("messageFrom", messageFrom);
			break;

		case BPAConstants.STATUS_APP_L2_PENDING_DEPT_VERIFICATION_INPROGRESS:
			messageTemplateTo = getMessageTemplate(applicationStatus + "_" + action + "_TO", localizationMessage);
			messageTo = getLocalizedMsg(regularization, messageTemplateTo, applicationStatus);
			messageTemplateFrom = getMessageTemplate(applicationStatus + "_" + action + "_FROM", localizationMessage);
			messageFrom = getLocalizedMsg(regularization, messageTemplateFrom, applicationStatus);
			messageMap.put("messageTo", messageTo);
			messageMap.put("messageFrom", messageFrom);
			break;

		case BPAConstants.STATUS_APP_L3_PENDING_DEPT_VERIFICATION_INPROGRESS:
			messageTemplateTo = getMessageTemplate(applicationStatus + "_" + action + "_TO", localizationMessage);
			messageTo = getLocalizedMsg(regularization, messageTemplateTo, applicationStatus);
			messageTemplateFrom = getMessageTemplate(applicationStatus + "_" + action + "_FROM", localizationMessage);
			messageFrom = getLocalizedMsg(regularization, messageTemplateFrom, applicationStatus);
			messageMap.put("messageTo", messageTo);
			messageMap.put("messageFrom", messageFrom);
			break;

		case BPAConstants.STATUS_APP_L4_PENDING_DEPT_VERIFICATION_INPROGRESS:
			messageTemplateTo = getMessageTemplate(applicationStatus + "_" + action + "_TO", localizationMessage);
			messageTo = getLocalizedMsg(regularization, messageTemplateTo, applicationStatus);
			messageTemplateFrom = getMessageTemplate(applicationStatus + "_" + action + "_FROM", localizationMessage);
			messageFrom = getLocalizedMsg(regularization, messageTemplateFrom, applicationStatus);
			messageMap.put("messageTo", messageTo);
			messageMap.put("messageFrom", messageFrom);
			break;

		case BPAConstants.STATUS_SHOW_CAUSE_ISSUED:
			messageTemplateTo = getMessageTemplate(applicationStatus + "_" + action + "_TO", localizationMessage);
			messageTo = getLocalizedMsg(regularization, messageTemplateTo, applicationStatus);
			messageTemplateFrom = getMessageTemplate(applicationStatus + "_" + action + "_FROM", localizationMessage);
			messageFrom = getLocalizedMsg(regularization, messageTemplateFrom, applicationStatus);
			messageMap.put("messageTo", messageTo);
			messageMap.put("messageFrom", messageFrom);
			break;

		case BPAConstants.STATUS_SHOW_CAUSE_REPLY_VERIFICATION_PENDING:
			messageTemplateTo = getMessageTemplate(applicationStatus + "_" + action + "_TO", localizationMessage);
			messageTo = getLocalizedMsg(regularization, messageTemplateTo, applicationStatus);
			messageTemplateFrom = getMessageTemplate(applicationStatus + "_" + action + "_FROM", localizationMessage);
			messageFrom = getLocalizedMsg(regularization, messageTemplateFrom, applicationStatus);
			messageMap.put("messageTo", messageTo);
			messageMap.put("messageFrom", messageFrom);
			break;

		case BPAConstants.STATUS_APPROVAL_INPROGRESS:
			messageTemplateTo = getMessageTemplate(applicationStatus + "_" + action + "_TO", localizationMessage);
			messageTo = getLocalizedMsg(regularization, messageTemplateTo, applicationStatus);
			messageTemplateFrom = getMessageTemplate(applicationStatus + "_" + action + "_FROM", localizationMessage);
			messageFrom = getLocalizedMsg(regularization, messageTemplateFrom, applicationStatus);
			messageMap.put("messageTo", messageTo);
			messageMap.put("messageFrom", messageFrom);
			break;

		}

		return messageMap;
	}
	
}
