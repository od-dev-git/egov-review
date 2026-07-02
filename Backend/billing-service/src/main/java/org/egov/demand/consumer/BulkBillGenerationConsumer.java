package org.egov.demand.consumer;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.egov.demand.model.BulkBillGenerator;
import org.egov.demand.model.Demand;
import org.egov.demand.model.GenerateBillCriteria;
import org.egov.demand.model.MigrationCount;
import org.egov.demand.service.BillServicev2;
import org.egov.demand.service.DemandService;
import org.egov.demand.web.contract.DemandRequest;
import org.egov.demand.web.contract.RequestInfoWrapper;
import org.egov.tracer.kafka.CustomKafkaTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class BulkBillGenerationConsumer {

	@Autowired
	private DemandService demandService;
	
	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private BillServicev2 billService;
	
	@Autowired
	private CustomKafkaTemplate<String, Object> kafkaTemplate;
	
	@KafkaListener(topics = { "${kafka.topics.bulk.bill.generation}" })
	public void processMessage(Map<String, Object> consumerRecord, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
		//TODO: Billing service received time
		Long receiveTime = System.currentTimeMillis();
			
		log.debug("key:" + topic + ":" + "value:" + consumerRecord);
		
		BulkBillGenerator billGenerator = objectMapper.convertValue(consumerRecord, BulkBillGenerator.class);
		billGenerator.getMigrationCount().addToAdditionalDetails("billingReceivedTime", receiveTime);
		log.info(billGenerator.getMigrationCount().getBatchName()+" received in billin-service. Timestamp: "+receiveTime);
		DemandRequest request = DemandRequest.builder()
				.requestInfo(billGenerator.getRequestInfo())
				.demands(billGenerator.getCreateDemands())
				.build();
		
		log.info(" Billing-bulkbill-consumer-batch log for batch : " + billGenerator.getMigrationCount().getOffset()
				+ " with no of records " + billGenerator.getCreateDemands().size());
		
		try {
			demandService.create(request, billGenerator);
		} catch (Exception e) {
			e.printStackTrace();
			logError(" Demand creation ", e.getMessage(), billGenerator.getMigrationCount());
			return;
		}
		
		Set<String> consumerCodes = billGenerator.getCreateDemands().stream().map(Demand::getConsumerCode).collect(Collectors.toSet());
		String tenantId = billGenerator.getCreateDemands().get(0).getTenantId();
		String businessService = billGenerator.getCreateDemands().get(0).getBusinessService();
//		request.setDemands(billGenerator.getUpdateDemands());
		try {
//			if (!CollectionUtils.isEmpty(billGenerator.getUpdateDemands()))
//				demandService.updateAsync(request, null);
			if(!CollectionUtils.isEmpty(consumerCodes)) {
				RequestInfoWrapper requestInfoWrapper = new RequestInfoWrapper();
				requestInfoWrapper.setRequestInfo(billGenerator.getRequestInfo());
				
				billService.updateDemandsForexpiredBillDetails(businessService, consumerCodes, tenantId, requestInfoWrapper);
			}
		} catch (Exception e) {
			e.printStackTrace();
			logError(" Demand update ", e.getMessage(), billGenerator.getMigrationCount());
			return;
		}

//		Set<String> consumerCodes = billGenerator.getCreateDemands()
//				.stream()
//				.map(Demand::getConsumerCode)
//				.collect(Collectors.toSet());
//		String tenantId = billGenerator.getCreateDemands().get(0).getTenantId();
//		String businessService = billGenerator.getCreateDemands().get(0).getBusinessService();
		
		GenerateBillCriteria genBillCriteria = GenerateBillCriteria.builder()
				.consumerCode(consumerCodes)
				.businessService(businessService)
				.tenantId(tenantId)
				.build();
		
		try {
			billService.generateBill(genBillCriteria, billGenerator.getRequestInfo());
		} catch (Exception e) {
			logError(" Bill Gen ", e.getMessage(), billGenerator.getMigrationCount());
			return;
		}
		
		MigrationCount migrationCount = billGenerator.getMigrationCount();
		migrationCount.setAuditTime(System.currentTimeMillis());
//		migrationCount.setMessage("prcoess succeded in billing service");
		migrationCount.setMessage("prcoess succeded in billing service ["+addCapturedTimestamp(migrationCount.getAdditionalDetails())+"]");
		log.info(migrationCount.getBatchName()+" prcoess succeded in billing service. Timestamp: "+migrationCount.getAuditTime());
		kafkaTemplate.send(migrationCount.getAuditTopic(), billGenerator.getMigrationCount());
	}
	
	private void logError(String process, String message, MigrationCount bulkBillCount) {
		bulkBillCount.setAuditTime(System.currentTimeMillis());
		bulkBillCount.setMessage("prcoess failed in billing service during "+ process + " ["+addCapturedTimestamp(bulkBillCount.getAdditionalDetails())+"] with error message : " + message);
		kafkaTemplate.send(bulkBillCount.getAuditTopic(), bulkBillCount);
	}
	
	private String addCapturedTimestamp(Object additionalDetails) {
		LinkedHashMap<String, Object> ad = (LinkedHashMap<String, Object>) additionalDetails;
		StringBuilder builder = new StringBuilder();
		builder.append(" batchStartTime: ").append(ad.get("batchStartTime")).append(",");
		builder.append(" pushedForDemandGeneration: ").append(ad.get("pushedForDemandGeneration")).append(",");
		builder.append(" demandGenConsumerReceiveTime: ").append(ad.get("demandGenConsumerReceiveTime")).append(",");
		builder.append(" pushedForBilling: ").append(ad.get("pushedForBilling")).append(",");
		builder.append(" billingReceivedTime: ").append(ad.get("billingReceivedTime"));
		return builder.toString();
	}

}