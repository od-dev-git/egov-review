package org.egov.pt.service.issuefix;

import lombok.extern.slf4j.Slf4j;
import org.egov.pt.models.Property;
import org.egov.pt.models.PropertyCriteria;
import org.egov.pt.models.collection.DemandSearchCriteria;
import org.egov.pt.models.collection.Payment;
import org.egov.pt.models.collection.PaymentSearchCriteria;
import org.egov.pt.models.workflow.ProcessInstance;
import org.egov.pt.models.workflow.WorkFlowSearchCriteria;
import org.egov.pt.repository.IssueFixRepository;
import org.egov.pt.service.PropertyService;
import org.egov.pt.util.IssueFixConstants;
import org.egov.pt.validator.IssueFixValidator;
import org.egov.pt.web.contracts.Demand;
import org.egov.pt.models.issuefix.IssueFix;
import org.egov.pt.models.issuefix.IssueFixRequest;
import org.egov.pt.web.contracts.DemandDetail;
import org.egov.tracer.model.CustomException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import io.micrometer.core.instrument.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service("paymentIssueFixService")
public class PaymentIssueFixService implements IIssueFixService{

    @Autowired
    private IssueFixValidator issueFixValidator;

    @Autowired
    private IssueFixRepository issueFixRepository;

    @Autowired
    private PropertyService propertyService;

    @Override
    public IssueFix issueFix(IssueFixRequest issueFixRequest) {

        Map<String, Boolean> isDataUpdateNeeded = new HashMap<>();
        issueFixValidator.validatePaymentIssueRequest(issueFixRequest);
        PropertyCriteria propertyCriteria = getSearchCriteria(issueFixRequest);
        if(StringUtils.isEmpty(issueFixRequest.getIssueFix().getPropertyId())) {
        	String propertyId=issueFixRepository.getPropertyId(issueFixRequest.getIssueFix().getApplicationNo());
        	issueFixRequest.getIssueFix().setPropertyId(propertyId);
        	
        }
        log.info("@Class: ApplicationStatusMismatchIssueFixService @method:issueFix @message: Property Search Completed");
        List<Property> propertyList = propertyService.searchProperty(propertyCriteria, issueFixRequest.getRequestInfo());
        issueFixValidator.validatePropertySearch(propertyList);
        Property property=propertyList.get(0);
        Payment payment = checkIfPaymentReceived(issueFixRequest);
        Demand demandToBeUpdated = checkIfDemandUpdatedProperly(issueFixRequest,isDataUpdateNeeded);
        ProcessInstance processInstance = checkIfWorkFlowUpdatedProperly(issueFixRequest, isDataUpdateNeeded);
        Property propertyToBeUpdated = checkIfApplicationUpdatedProperly(issueFixRequest,
                property, isDataUpdateNeeded);

        updateDataIfNeeded(demandToBeUpdated, processInstance, propertyToBeUpdated, payment, isDataUpdateNeeded);
        return issueFixRequest.getIssueFix();
    }

    /**
     * check weather payment is done for mentioned application
     *
     * @param issueFixRequest
     */
    private Payment checkIfPaymentReceived(IssueFixRequest issueFixRequest) {

        String tenantId = issueFixRequest.getIssueFix().getTenantId();

        String applicationNumber = issueFixRequest.getIssueFix().getApplicationNo();

        PaymentSearchCriteria paymentSearchCriteria = PaymentSearchCriteria.builder().consumerCode(applicationNumber).tenantId(tenantId).build();

        List<Payment> payments = issueFixRepository.getPayments(paymentSearchCriteria);

        if (CollectionUtils.isEmpty(payments) || payments.size() > 1) {

            throw new CustomException("PAYMENT_ISSUE",
                    "Found No or Multiple Payments for mentioned Application Number. Kindly revalidate the payments !!");
        }

        log.info("Payment received.. Moving to next step !!");

        return payments.get(0);

    }

    /**
     * check weather is payment completed flag is updated or not
     *
     * @param issueFixRequest
     * @param isDataUpdateNeeded
     */
    private Demand checkIfDemandUpdatedProperly(IssueFixRequest issueFixRequest, Map<String, Boolean> isDataUpdateNeeded) {

        String tenantId = issueFixRequest.getIssueFix().getTenantId();

        String propertyid = issueFixRequest.getIssueFix().getPropertyId();
        
        String acknowldgementNo = issueFixRequest.getIssueFix().getApplicationNo();

        DemandSearchCriteria demandSearchCriteria = DemandSearchCriteria.builder()
        		.consumerCode(acknowldgementNo)
        		.businessService("PT.MUTATION")
        		.tenantId(tenantId).build();

        // Search demand here
        List<Demand> demands = issueFixRepository.getDemands(demandSearchCriteria);

        if (CollectionUtils.isEmpty(demands)) {

            throw new CustomException("DEMAND_ISSUE",
                    "Found no demands for mentioned Application Number. Kindly revalidate the Application State !!");
        }

        // Start Checking if the demand is updated properly, if not update the demand
        Demand demand = demands.get(0);

        List<DemandDetail> demandDetails = demand.getDemandDetails();

        for (DemandDetail demandDetail : demandDetails) {
            if (demandDetail.getTaxAmount().compareTo(demandDetail.getCollectionAmount()) == 0) {
                log.info("Demand Details are already updated for .. "+demandDetail.getTaxHeadMasterCode() + " Moving to next step...");
            } else {
                isDataUpdateNeeded.put(IssueFixConstants.IS_DEMAND_DETAILS_UPDATE_NEEDED, true);
            }
        }

        if (!demand.getIsPaymentCompleted()) {
            isDataUpdateNeeded.put(IssueFixConstants.IS_DEMAND_UPDATE_NEEDED, true);
        } else {
            log.info("Is PaymentCompleted Flag already updated.. Moving to next step...");
        }
        return demand;
    }

    /**
     * Check if Pay action inserted in workflow, if not insert pay from this method
     *
     * @param issueFixRequest
     * @param isDataUpdateNeeded
     */
    private ProcessInstance checkIfWorkFlowUpdatedProperly(IssueFixRequest issueFixRequest, Map<String, Boolean> isDataUpdateNeeded) {

        String tenantId = issueFixRequest.getIssueFix().getTenantId();

        String applicationNumber = issueFixRequest.getIssueFix().getApplicationNo();

        WorkFlowSearchCriteria workFlowSearchCriteria = WorkFlowSearchCriteria.builder().businessId(applicationNumber)
                .tenantId(tenantId).build();

        List<ProcessInstance> processInstances = issueFixRepository.getProcessInstances(workFlowSearchCriteria);


        if(CollectionUtils.isEmpty(processInstances)) {
            throw new CustomException("WORKFLOW_ISSUE",
                    "Found No Workflow records for mentioned Application Number !!");
        }

        ProcessInstance lastestProcessInstance = processInstances.get(0);

        if(lastestProcessInstance.getAction().equalsIgnoreCase(IssueFixConstants.PAY)) {
            log.info("Workflow is already updated.. Pay Action is already there... Moving to next step...");
        } else {
            isDataUpdateNeeded.put(IssueFixConstants.IS_WORKFLOW_UPDATE_NEEDED, true);
        }

        List<ProcessInstance> processInstanceForApply = processInstances.stream()
                .filter(process -> process.getAction().equalsIgnoreCase(IssueFixConstants.APPROVE))
                .map(process -> process).collect(Collectors.toList());

        return processInstanceForApply.get(0);
    }


    @Transactional
    private void updateDataIfNeeded(Demand demandToBeUpdated, ProcessInstance processInstance, Property propertyToBeUpdated,
                                   Payment payment, Map<String, Boolean> isDataUpdateNeeded) {

        if (isDataUpdateNeeded.containsKey(IssueFixConstants.IS_DEMAND_DETAILS_UPDATE_NEEDED)
                && isDataUpdateNeeded.get(IssueFixConstants.IS_DEMAND_DETAILS_UPDATE_NEEDED)) {
            demandToBeUpdated.getDemandDetails().forEach(demandDetail -> {
                issueFixRepository.updateDemandDetail(demandDetail);
            });
        }
        if (isDataUpdateNeeded.containsKey(IssueFixConstants.IS_DEMAND_UPDATE_NEEDED)
                && isDataUpdateNeeded.get(IssueFixConstants.IS_DEMAND_UPDATE_NEEDED)) {
            issueFixRepository.updateDemand(demandToBeUpdated);
        }

        if ((isDataUpdateNeeded.containsKey(IssueFixConstants.IS_DEMAND_DETAILS_UPDATE_NEEDED)
                && isDataUpdateNeeded.get(IssueFixConstants.IS_DEMAND_DETAILS_UPDATE_NEEDED))
                || (isDataUpdateNeeded.containsKey(IssueFixConstants.IS_DEMAND_UPDATE_NEEDED)
                && isDataUpdateNeeded.get(IssueFixConstants.IS_DEMAND_UPDATE_NEEDED))) {

            issueFixRepository.expireBill(demandToBeUpdated.getConsumerCode());

        }

        if (isDataUpdateNeeded.containsKey(IssueFixConstants.IS_APPLICATION_UPDATE_NEEDED)
                && isDataUpdateNeeded.get(IssueFixConstants.IS_APPLICATION_UPDATE_NEEDED)) {
            issueFixRepository.updateApplication(propertyToBeUpdated, payment);
        }
        if (isDataUpdateNeeded.containsKey(IssueFixConstants.IS_WORKFLOW_UPDATE_NEEDED)
                && isDataUpdateNeeded.get(IssueFixConstants.IS_WORKFLOW_UPDATE_NEEDED)) {
            issueFixRepository.updateWorkflow(processInstance, payment);
        }

    }

    private Property checkIfApplicationUpdatedProperly(IssueFixRequest issueFixRequest,
                                                              Property property, Map<String, Boolean> isDataUpdateNeeded) {

        if (!property.getStatus().toString().equalsIgnoreCase(IssueFixConstants.STATUS_INWORKFLOW)) {
            log.info(" Application state already updated.. No Changes done in Application status...");
        } else {
//			repository.updateApplication(waterConnection);
            isDataUpdateNeeded.put(IssueFixConstants.IS_APPLICATION_UPDATE_NEEDED, true);
        }
        return property;
    }

}
