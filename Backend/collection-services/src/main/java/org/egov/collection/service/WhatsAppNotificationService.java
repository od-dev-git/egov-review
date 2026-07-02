package org.egov.collection.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.egov.collection.config.ApplicationProperties;
import org.egov.collection.model.Payment;
import org.egov.collection.model.whatsapp.*;
import org.egov.collection.repository.ServiceRequestRepository;
import org.egov.collection.web.contract.Bill;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Arrays;
import java.util.UUID;

import static org.egov.collection.config.CollectionServiceConstants.BUSINESS_SERVICE_PT;
import static org.egov.collection.config.CollectionServiceConstants.BUSINESS_SERVICE_WS;

@Service
@Slf4j
public class WhatsAppNotificationService {
	
	@Autowired
	private ApplicationProperties applicationProperties;
	
	@Autowired
	private ServiceRequestRepository serviceRequestRepository;
	
	@Autowired
    private ObjectMapper mapper;
	
	public void pushWhatsappNotification(Payment payment) {
		log.info("@Class: WhatsAppNotificationService @method: pushWhatsappNotification : @Message Inside WhatsApp Method @Payment:{}",payment);
		JsonNode additionalDetails= payment.getAdditionalDetails();
		if(additionalDetails!=null && additionalDetails.hasNonNull("isWhatsApp")) {
			Boolean isWhatsApp= additionalDetails.path("isWhatsApp").asBoolean();
			if(isWhatsApp) {
				if(additionalDetails.hasNonNull("whatsAppNo")) {
					String whatsAppNo=additionalDetails.path("whatsAppNo").asText();
					log.info("@Class: WhatsAppNotificationService @method:pushWhatsappNotification @Message: WhatsApp Payment");
					sendWhatsappNotification(payment,whatsAppNo);
				}
			}
		}
		
	}
	
	@Async
	public void pushWhatsappNotificationv2(Payment payment) {
		log.info("@Class: WhatsAppNotificationService @method: pushWhatsappNotificationV2 : @Message Inside WhatsApp Method @Payment:{}",payment);
		JsonNode additionalDetails= payment.getAdditionalDetails();
		if(additionalDetails!=null && additionalDetails.hasNonNull("isWhatsAppv2")) {
			Boolean isWhatsApp= additionalDetails.path("isWhatsAppv2").asBoolean();
			if(isWhatsApp) {
				if(additionalDetails.hasNonNull("whatsAppNo")) {
					String whatsAppNo=additionalDetails.path("whatsAppNo").asText();
					log.info("@Class: WhatsAppNotificationService @method:pushWhatsappNotificationV2 @Message: WhatsApp Payment");
					sendWhatsappNotificationv2(payment,whatsAppNo);
				}
			}
		}
		
	}



	private void sendWhatsappNotification(Payment payment, String whatsAppNo) {
		StringBuilder requestUrl= getWhatsAppRequestURL();
		WhatsAppPushRequest whatsAppPushRequest= getWhatsAppRequestBody(payment,whatsAppNo);
		String whatsAppPushRequestString="";
		try {
			whatsAppPushRequestString = mapper.writeValueAsString(whatsAppPushRequest);
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}
		log.info("@Class: WhatsAppNotificationService @method: sendWhatsappNotification @Message:WhatsApp Request Object- {}",whatsAppPushRequestString);
		Object response= serviceRequestRepository.fetchResult(requestUrl, whatsAppPushRequest);
		String whatsAppResponseString = "";
		try {
			whatsAppResponseString=mapper.writeValueAsString(response);
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}
		log.info("@Class: WhatsAppNotificationService @method: sendWhatsappNotification @Message: Notification Sent to WhatsApp @Response:{}",whatsAppResponseString);
	}
	
	private void sendWhatsappNotificationv2(Payment payment, String whatsAppNo) {
		StringBuilder requestUrl= getWhatsAppRequestURLv2();
		WhatsAppPushRequest whatsAppPushRequest= getWhatsAppRequestBodyv2(payment,whatsAppNo);
		String whatsAppPushRequestString="";
		try {
			whatsAppPushRequestString = mapper.writeValueAsString(whatsAppPushRequest);
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}
		log.info("@Class: WhatsAppNotificationService @method: sendWhatsappNotificationV2 @Message:WhatsApp Request Object- {}",whatsAppPushRequestString);
		Object response= serviceRequestRepository.fetchResult(requestUrl, whatsAppPushRequest);
		String whatsAppResponseString = "";
		try {
			whatsAppResponseString=mapper.writeValueAsString(response);
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}
		log.info("@Class: WhatsAppNotificationService @method: sendWhatsappNotificationV2 @Message: Notification Sent to WhatsApp @Response:{}",whatsAppResponseString);
	}



	private WhatsAppPushRequest getWhatsAppRequestBody(Payment payment, String whatsAppNo) {
		Bill bill = payment.getPaymentDetails().get(0).getBill();
		String uuid= UUID.randomUUID().toString();
		
		Text text =getText(bill);
		
		Message message =getMessage(text,whatsAppNo,uuid);
		
		Contact contact = getContact(payment,whatsAppNo);
		
		ExtraPayload extraPayload= getExtraPayload(payment);
		WhatsAppPushRequest whatsAppPushRequest = WhatsAppPushRequest.builder()
				.messages(Arrays.asList(message))
				.contacts(Arrays.asList(contact))
				.brand_msisdn(applicationProperties.getWhatsAppNotificationBrandMsisdn())
				.request_id(uuid)
				.extra_payload(extraPayload)
				.build();
		return whatsAppPushRequest;
	}
	
	private WhatsAppPushRequest getWhatsAppRequestBodyv2(Payment payment, String whatsAppNo) {
		Bill bill = payment.getPaymentDetails().get(0).getBill();
		String uuid= UUID.randomUUID().toString();
		
		Text text =getText(bill);
		
		Message message =getMessage(text,whatsAppNo,uuid);
		
		Contact contact = getContact(payment,whatsAppNo);
		
		ExtraPayload extraPayload= getExtraPayload(payment);
		WhatsAppPushRequest whatsAppPushRequest = WhatsAppPushRequest.builder()
				.messages(Arrays.asList(message))
				.contacts(Arrays.asList(contact))
				.brand_msisdn(applicationProperties.getWhatsAppNotificationBrandMsisdnv2())
				.request_id(uuid)
				.extra_payload(extraPayload)
				.build();
		return whatsAppPushRequest;
	}



	private ExtraPayload getExtraPayload(Payment payment) {
		ExtraPayload extraPayload = ExtraPayload.builder()
				.receiptNumbers(payment.getPaymentDetails().get(0).getReceiptNumber())
				.tenantId(payment.getTenantId())
				.consumerCode(payment.getPaymentDetails().get(0).getBill().getConsumerCode()).build();
		return extraPayload;
	}



	private Contact getContact(Payment payment, String whatsAppNo) {
		Profile profile = Profile.builder().name(payment.getPayerName()).build();
		Contact contact = Contact.builder().profile(profile).wa_id(whatsAppNo).build();
		return contact;
	}



	private Message getMessage(Text text, String whatsAppNo, String uuid) {
		String currentTimeStamp=  Long.toString(Instant.now().getEpochSecond());
		Message message = Message.builder().id(uuid).from(whatsAppNo).type("text")
				.timestamp(currentTimeStamp).text(text).build();
		return message;
	}



	private Text getText(Bill bill) {
		Text text= Text.builder().build();
		if(bill.getBusinessService().equals(BUSINESS_SERVICE_PT)) {
			text.setBody("payment_success_pt");
		}
		if(bill.getBusinessService().equals(BUSINESS_SERVICE_WS)) {
			text.setBody("payment_success_ws");
		}	
		return text;
	}



	private StringBuilder getWhatsAppRequestURL() {
		StringBuilder url = new StringBuilder();
		url.append(applicationProperties.getWhatsAppNotificationHost())
		.append(applicationProperties.getWhatsAppNotificationEndpoint());
		return url;
	}
	
	private StringBuilder getWhatsAppRequestURLv2() {
		StringBuilder url = new StringBuilder();
		url.append(applicationProperties.getWhatsAppNotificationHostv2())
		.append(applicationProperties.getWhatsAppNotificationEndpointv2());
		return url;
	}


}
