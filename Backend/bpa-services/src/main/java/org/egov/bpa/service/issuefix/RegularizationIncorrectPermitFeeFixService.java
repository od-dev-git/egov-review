package org.egov.bpa.service.issuefix;

import static java.util.Objects.isNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.egov.bpa.repository.IssueFixRepository;
import org.egov.bpa.repository.RegularizationRepository;
import org.egov.bpa.util.IssueFixConstants;
import org.egov.bpa.validator.IssueFixValidator;
import org.egov.bpa.web.model.collection.Demand;
import org.egov.bpa.web.model.collection.DemandDetail;
import org.egov.bpa.web.model.collection.DemandSearchCriteria;
import org.egov.bpa.web.model.collection.Payment;
import org.egov.bpa.web.model.collection.PaymentSearchCriteria;
import org.egov.bpa.web.model.issuefix.IssueFix;
import org.egov.bpa.web.model.issuefix.IssueFixRequest;
import org.egov.bpa.web.model.regularization.Regularization;
import org.egov.bpa.web.model.regularization.RegularizationSearchCriteria;
import org.egov.tracer.model.CustomException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import lombok.extern.slf4j.Slf4j;

@Service("regIncorrectPermitFeeFixService")
@Slf4j
public class RegularizationIncorrectPermitFeeFixService implements RegularizationIIssueFixService {

	@Autowired
	private IssueFixValidator validator;

	@Autowired
	private IssueFixRepository repository;

	@Autowired
	private RegularizationRepository regularizationRepository;

	@Override
	public IssueFix issueFix(IssueFixRequest issueFixRequest) {

		validator.validateIssueFix(issueFixRequest);
		Map<String, Boolean> isDataUpdateNeeded = new HashMap<>();

		RegularizationSearchCriteria searchCriteria = createRegularizationSearchCriteria(issueFixRequest);

		List<Regularization> regularizations = regularizationRepository.searchRegularization(searchCriteria, issueFixRequest.getRequestInfo());
		validator.validateRegularizationApplication(regularizations);
		validator.validateApprovedApplication(regularizations);
		
		Boolean isPaymentReceived = checkIfPaymentReceived(issueFixRequest);

		Demand demandToBeUpdated = checkIfDemandUpdatedProperly(issueFixRequest, isDataUpdateNeeded);			
		
		if (!isDataUpdateNeeded.containsKey(IssueFixConstants.IS_DEMAND_DELETE_NEEDED)) {
			throw new CustomException("NO_DUPLICATES_FOUND", "No duplicate found for sanc fee demand in the mentioned application number !!");
		}

		updateDataIfNeeded(demandToBeUpdated, isPaymentReceived, isDataUpdateNeeded, issueFixRequest.getIssueFix());
		return issueFixRequest.getIssueFix();
	}


	/**
	 * Update execution 
	 * 
	 * @param demandToBeUpdated
	 * @param isPaymentReceived
	 * @param isDataUpdateNeeded
	 * @param issueFix
	 */
	@Transactional
	private void updateDataIfNeeded(Demand demandToBeUpdated,
		 Boolean isPaymentReceived, Map<String, Boolean> isDataUpdateNeeded, IssueFix issueFix) {

		if (isDataUpdateNeeded.containsKey(IssueFixConstants.IS_DEMAND_DELETE_NEEDED)
				&& isDataUpdateNeeded.get(IssueFixConstants.IS_DEMAND_DELETE_NEEDED)) {
			repository.deleteDemandDetail(demandToBeUpdated);
			repository.deleteDemand(demandToBeUpdated);
		}

		if ((isDataUpdateNeeded.containsKey(IssueFixConstants.IS_DEMAND_DELETE_NEEDED)
				&& isDataUpdateNeeded.get(IssueFixConstants.IS_DEMAND_DELETE_NEEDED))) {
			repository.expireBill(demandToBeUpdated.getConsumerCode());
		}
	}

	/**
	 * check weather is payment completed flag is updated or not
	 * 
	 * @param issueFixRequest
	 * @param isDataUpdateNeeded
	 */
	private Demand checkIfDemandUpdatedProperly(IssueFixRequest issueFixRequest, Map<String, Boolean> isDataUpdateNeeded) {
		Demand demand = new Demand();

		String tenantId = issueFixRequest.getIssueFix().getTenantId();
		String applicationNumber = issueFixRequest.getIssueFix().getApplicationNo();

		DemandSearchCriteria demandSearchCriteria = DemandSearchCriteria.builder().consumerCode(applicationNumber)
				.businessService(IssueFixConstants.REG_SAN_FEE).tenantId(tenantId).build();

		List<Demand> demands = repository.getDemands(demandSearchCriteria);

		if (demands.size() > 1) {
			log.info("Nos. of sanc fee demand found for application number : " + applicationNumber + " - " + demands.size());
			if (demands.get(0).getIsPaymentCompleted().equals(Boolean.TRUE)) {
				throw new CustomException("PAYMENT_DONE_FOR_DEMAND",
						"Payment has been done for sanc fee demand application number :" + applicationNumber);
			} else {
				demand = demands.get(0);
				isDataUpdateNeeded.put(IssueFixConstants.IS_DEMAND_DELETE_NEEDED, true);
			}
		} else if (demands.size() == 1) {
			demand = demands.get(0);
			List<DemandDetail> demandDetailsWithDuplicateTaxhead = checkIfDemandDetailsWithDuplicateTaxhead(demand.getDemandDetails());
			
			if(demandDetailsWithDuplicateTaxhead.isEmpty()) {
				log.info("No duplicates sanc fee demand found for application number :" + applicationNumber);
			} else {
				repository.deleteDemandDetails(demandDetailsWithDuplicateTaxhead);
				repository.expireBill(applicationNumber);
			}
			
		} else {
			log.info("No sanc fee demand found for application number :" + applicationNumber);
		}

		return demand;
	}
	
	
	private List<DemandDetail> checkIfDemandDetailsWithDuplicateTaxhead(List<DemandDetail> allDemandDetails) {
		if(!CollectionUtils.isEmpty(allDemandDetails)) {
			List<DemandDetail> uniqueDemandDetails = allDemandDetails
					.stream()
					.filter(duplicateByKey(DemandDetail::getTaxHeadMasterCode))
					.collect(Collectors.toList());
			log.info("Unique Demand Details " + uniqueDemandDetails.toString());
			
			if(allDemandDetails.size() != uniqueDemandDetails.size()) {
				Set<DemandDetail> duplicateDemandDetails = allDemandDetails.stream()
						.filter(all -> uniqueDemandDetails.stream()
								.noneMatch(unique -> unique.getId().equalsIgnoreCase(all.getId())))
						.collect(Collectors.toCollection(() -> new TreeSet<>(Comparator.comparing(DemandDetail::getId))));

				log.info("Duplicate Demand Details " + duplicateDemandDetails.toString());
				return new ArrayList<>(duplicateDemandDetails);
			}
		}
		return new ArrayList<>();
	}


	private static <T> Predicate<T> duplicateByKey(final Function<? super T, Object> keyExtractor) {
	    Map<Object,Boolean> seen = new ConcurrentHashMap<>();
	    return t -> isNull(seen.putIfAbsent(keyExtractor.apply(t), Boolean.TRUE));
	}
	

	/**
	 * check weather payment is done for mentioned application
	 * 
	 * @param issueFixRequest
	 */
	private Boolean checkIfPaymentReceived(IssueFixRequest issueFixRequest) {
		Boolean isPaymentReceived = Boolean.FALSE;

		String tenantId = issueFixRequest.getIssueFix().getTenantId();

		String applicationNumber = issueFixRequest.getIssueFix().getApplicationNo();

		PaymentSearchCriteria paymentSearchCriteria = PaymentSearchCriteria.builder().consumerCode(applicationNumber)
				.businessService(IssueFixConstants.REG_SAN_FEE).tenantId(tenantId).build();

		List<Payment> payments = repository.getPayments(paymentSearchCriteria);

		if (payments.size() >= 1) {

			isPaymentReceived = Boolean.TRUE;
			throw new CustomException("PAYMENT_ISSUE", "Sanc fee payment has been done for application no :"
					+ applicationNumber + " Demand can't be modified !");
		}

		log.info("Sanc Fee payment has not been done.. Moving to next step !!");

		return isPaymentReceived;

	}

}
