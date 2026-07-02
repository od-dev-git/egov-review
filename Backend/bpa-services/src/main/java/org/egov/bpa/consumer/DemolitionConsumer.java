package org.egov.bpa.consumer;

import java.util.HashMap;

import org.egov.bpa.service.notification.BPANotificationService;
import org.egov.bpa.service.notification.DemolitionNotificationService;
import org.egov.bpa.web.model.BPARequest;
import org.egov.bpa.web.model.demolition.DemolitionRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class DemolitionConsumer {

	
	@Autowired
	private DemolitionNotificationService notificationService;
	
	@KafkaListener(topics = { "${persister.update.demolition.topic}", "${persister.save.demolition.topic}" })
	public void listen(final HashMap<String, Object> record, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
		ObjectMapper mapper = new ObjectMapper();
		DemolitionRequest demolationRequest = new DemolitionRequest();
		try {
			log.debug("Consuming record: " + record);
			demolationRequest = mapper.convertValue(record, DemolitionRequest.class);
		} catch (final Exception e) {
			log.error("Error while listening to value: " + record + " on topic: " + topic + ": " + e);
		}
		log.info("Demolition Received: " + demolationRequest.getDemolition().getApplicationNo());
		notificationService.process(demolationRequest);
	}
}
