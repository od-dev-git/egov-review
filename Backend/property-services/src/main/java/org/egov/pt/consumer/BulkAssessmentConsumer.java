package org.egov.pt.consumer;

import java.util.HashMap;
import org.egov.pt.config.PropertyConfiguration;
import org.egov.pt.models.BulkAssessmentAudit;
import org.egov.pt.producer.Producer;
import org.egov.pt.service.AssessmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class BulkAssessmentConsumer {
	
	@Autowired
	private ObjectMapper mapper;
	
	@Autowired
	private AssessmentService assessmentService;
	
	@Autowired
	private Producer producer;

	@Autowired
	private PropertyConfiguration props;
	
	@KafkaListener(topics = {"${kafka.topics.bulkAssessment.create}"})
    public void listenBulkAssessment(final HashMap<String, Object> record,  @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
		log.info("Batch received for processing in BulkAssessmentConsumer");
		BulkAssessmentAudit assessmentAudit = mapper.convertValue(record, BulkAssessmentAudit.class);
		try {
			assessmentService.processBulkAssessment(assessmentAudit.getAssessmentRequests(), true);
			
			assessmentAudit.setMessage("prcoess succeded in assesment creation");
			assessmentAudit.setAuditTime(System.currentTimeMillis());
			producer.push(props.getBulkAssessmentGenAutidTopic(), assessmentAudit);
			
		} catch (Exception e) {
			assessmentAudit.setMessage("Failed to create assessmnt in BulkAssessmentConsumer. Error: [ "+e.getLocalizedMessage()+" ]");
			assessmentAudit.setAuditTime(System.currentTimeMillis());
			producer.push(props.getBulkAssessmentGenAutidTopic(), assessmentAudit);
		}
	}

}
