package org.egov.demand.consumer.notification;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.egov.demand.config.ApplicationProperties;
import org.egov.demand.producer.Producer;
import org.egov.demand.util.Util;
import org.egov.demand.web.contract.DemandRequest;
import org.egov.demand.web.contract.User;
import org.egov.demand.web.models.EmailRequest;
import org.egov.demand.web.models.Sms;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class DemandNotification {
	
	@Autowired
	private ApplicationProperties config;
	
	@Autowired
	private Producer producer;
	
	@Autowired
	private Util util;
	
	
	@Async
	public void generateDemandNotification(DemandRequest demandRequest) {
		log.info("Sending Demand Generation Notifications");
		try {
			if (config.getIsEmalNotificationEnabled() != null && config.getIsEmalNotificationEnabled()) {
				sendDemandGenerationEmailNotification(demandRequest);
			}
		} catch (Exception e) {
			log.info("Exception occured while sending Demand Generatio Notification EMAIL : " + e.toString());
		}

		try {
			if (config.getIsSMSEnabled() != null && config.getIsSMSEnabled()) {
				sendDemandGenerationSmsNotification(demandRequest);
			}
		} catch (Exception e) {
			log.info("Exception occured while sending Demand Generatio Notification SMS : " + e.toString());
		}
	}

	private void sendDemandGenerationSmsNotification(DemandRequest demandRequest) {
		// TODO Auto-generated method stub
		log.info("Sending Demand Generation Notification for SMS");
		List<Sms> smsRequest = new ArrayList<>();
		demandRequest.getDemands().forEach(demand -> {

			log.info("Demand is " + String.valueOf(demand));
			User user = demand.getPayer();
			String phNo = user.getMobileNumber();

			if (user.getMobileNumber() == null || user.getMobileNumber().isEmpty()) {
				log.info("Owners Phone No is not present for acknowledgement no :" + demand.getConsumerCode());
			} else {
				String message = util.buildSmsBody(demand, demandRequest.getRequestInfo());
				createSMSRequest(smsRequest, phNo, message);
			}

		});

		if (!CollectionUtils.isEmpty(smsRequest)) {
			sendSMS(smsRequest);
		}

	}

	private void createSMSRequest(List<Sms> smsRequest, String phNo, String message) {
		if (!StringUtils.isEmpty(message)) {

			log.info("Sending message to user : " + message);
			Sms req = Sms.builder().mobileNumber(phNo).message(message)
					.category(org.egov.demand.web.models.Category.NOTIFICATION).build();
			smsRequest.add(req);

		} else {
			log.error("No message configured! Notification will not be sent.");
		}
	}


	/**
	 * Prepare email request for demand notification
	 * @param demandRequest
	 * @return
	 */
	private void sendDemandGenerationEmailNotification(DemandRequest demandRequest) {//here
		log.info("Sending Demand Generation Notification for EMAIL");
		demandRequest.getDemands().forEach(demand -> {
			log.info("Demand is " + String.valueOf(demand));
			User user = demand.getPayer();
			String phNo = user.getMobileNumber();
			String emailId = user.getEmailId();
			if(emailId == null || emailId.isEmpty()) {
				log.info("Owners email id is not present for acknowledgement no :"+ demand.getConsumerCode());
			} else {
				String CompleteMsgs = util.buildEmailMessageBodyForDemandGenerationNotification(demand, demandRequest.getRequestInfo());
				List<EmailRequest> emailRequestList = util.createEmailRequest(CompleteMsgs, emailId, demandRequest, null);
				sendEmail(emailRequestList);
			}
			
		});
	}
	
	public void sendEmail(List<EmailRequest> emailRequestList) {
		  
		log.info("Pusing Email Request to Email Topic  ");
		log.info("Email Request List is " + String.valueOf(emailRequestList));
		if (config.getIsEmalNotificationEnabled()) {
			if (CollectionUtils.isEmpty(emailRequestList))
				log.info("Messages for Email from localization couldn't be fetched!");
			for (EmailRequest emailRequest : emailRequestList) {
				log.info("Email sent to user : " + String.valueOf(emailRequest.getEmail().getBody()) + "  with email to: " + String.valueOf(emailRequest.getEmail().getEmailTo()) + " with subject  : " + String.valueOf(emailRequest.getEmail().getSubject()));
				producer.push(config.getEmailNotifTopic(), emailRequest);
				log.info("Email Sent to id : " + emailRequest.getEmail());
			}
		}
		log.info("Email Request pushed to Email Topic ");
	}
	
	/**
	 * Send the SMSRequest on the SMSNotification kafka topic
	 * @param smsRequestList The list of SMSRequest to be sent
	 */
	public void sendSMS(List<Sms> smsRequestList) {
		if (config.getIsSMSEnabled()) {
			if (CollectionUtils.isEmpty(smsRequestList)) {
				log.info("No SMS found!");
				return;
			}
			for (Sms smsRequest : smsRequestList) {
				producer.push(config.getSmsNotifTopic(), smsRequest);
				log.info("Messages: " + smsRequest.getMessage());
			}
		}
	}
	
}
