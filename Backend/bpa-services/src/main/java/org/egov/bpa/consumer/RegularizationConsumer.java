package org.egov.bpa.consumer;

import java.util.HashMap;

import org.egov.bpa.service.notification.RegularizationNotificationService;
import org.egov.bpa.web.model.regularization.RegularizationRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class RegularizationConsumer {

	@Autowired
	private RegularizationNotificationService notificationService;

	@KafkaListener(topics = { "${persister.save.regularization.topic}", "${persister.update.regularization.topic}" })
	public void listen(final HashMap<String, Object> record, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
		ObjectMapper mapper = new ObjectMapper();
		RegularizationRequest regularizationRequest = new RegularizationRequest();
		try {
			log.debug("Consuming record: " + record);
			regularizationRequest = mapper.convertValue(record, RegularizationRequest.class);
		} catch (final Exception e) {
			log.error("Error while listening to value: " + record + " on topic: " + topic + ": " + e);
		}
		log.info("BPA Received: " + regularizationRequest.getRegularization().getApplicationNo());
		notificationService.process(regularizationRequest);
	}
}
