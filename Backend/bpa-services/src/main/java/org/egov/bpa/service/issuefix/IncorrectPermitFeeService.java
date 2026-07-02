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

import org.egov.bpa.repository.BPARepository;
import org.egov.bpa.repository.IssueFixRepository;
import org.egov.bpa.util.IssueFixConstants;
import org.egov.bpa.validator.IssueFixValidator;
import org.egov.bpa.web.model.BPA;
import org.egov.bpa.web.model.BPASearchCriteria;
import org.egov.bpa.web.model.collection.Demand;
import org.egov.bpa.web.model.collection.DemandDetail;
import org.egov.bpa.web.model.collection.DemandSearchCriteria;
import org.egov.bpa.web.model.collection.Payment;
import org.egov.bpa.web.model.collection.PaymentSearchCriteria;
import org.egov.bpa.web.model.issuefix.IssueFix;
import org.egov.bpa.web.model.issuefix.IssueFixRequest;
import org.egov.tracer.model.CustomException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import lombok.extern.slf4j.Slf4j;

@Service("incorrectPermitFeeService")
@Slf4j
public class IncorrectPermitFeeService implements IIssueFixService {

	@Autowired
	private IssueFixValidator validator;

	@Autowired
	private IssueFixRepository repository;

	@Autowired
	private BPARepository bpaRepository;

	@Override
	public IssueFix issueFix(IssueFixRequest issueFixRequest) {

		validator.validateIssueFix(issueFixRequest);

		String applicationNo = issueFixRequest.getIssueFix().getApplicationNo();

		String tenantId = issueFixRequest.getIssueFix().getTenantId();

		Map<String, Boolean> isDataUpdateNeeded = new HashMap<>();

		BPASearchCriteria searchCriteria = BPASearchCriteria.builder().tenantId(tenantId).applicationNo(applicationNo)
				.build();

		List<BPA> bpa = bpaRepository.getBPAData(searchCriteria, new ArrayList<String>());

		if (CollectionUtils.isEmpty(bpa) || bpa.size() > 1) {
			throw new CustomException("SEARCH_ERROR",
					"Either no or multiple applications found with the mentioned application number !!");
		}

		if (bpa.get(0).getStatus().equalsIgnoreCase(IssueFixConstants.APPROVED)) {
			throw new CustomException("APPLICATION_STATUS_ERROR",
					"Application is already approved. Demand can't be modified for approved application !!");
		}

		Boolean isPaymentReceived = checkIfPaymentReceived(issueFixRequest);

		Demand demandToBeUpdated = checkIfDemandUpdatedProperly(issueFixRequest, isDataUpdateNeeded);			

		List<String> installmentToBeDeleted = checkIfInstallmentToBeDeleted(issueFixRequest, isDataUpdateNeeded);
		
		if (!isDataUpdateNeeded.containsKey(IssueFixConstants.IS_DEMAND_DELETE_NEEDED)
				&& !isDataUpdateNeeded.containsKey(IssueFixConstants.IS_INSTALLMENT_DELETE_NEEDED)) {
			throw new CustomException("NO_DUPLICATES_FOUND",
					"No duplicate found for sanc fee demand & installment with the mentioned application number !!");
		}

		updateDataIfNeeded(demandToBeUpdated, isPaymentReceived,
				isDataUpdateNeeded, issueFixRequest.getIssueFix());

		return issueFixRequest.getIssueFix();
	}


	private List<String> checkIfInstallmentToBeDeleted(IssueFixRequest issueFixRequest,
			Map<String, Boolean> isDataUpdateNeeded) {
		List<String> installment = null;
		List<String> installmentData = repository.getInstallment(issueFixRequest.getIssueFix());
		List<String> duplicateInstallments = repository.checkDuplicates(issueFixRequest.getIssueFix());

		if (CollectionUtils.isEmpty(installmentData)) {
			log.info("No installments were found for application number :"
					+ issueFixRequest.getIssueFix().getApplicationNo());
		}

		if (CollectionUtils.isEmpty(duplicateInstallments)) {
			log.info("No duplicate installment were found for application number :"
					+ issueFixRequest.getIssueFix().getApplicationNo());
		}else {
			installment = duplicateInstallments;
			isDataUpdateNeeded.put(IssueFixConstants.IS_INSTALLMENT_DELETE_NEEDED, true);
		}		

		return installment;
	}

	/**
	 * Execute the update queries here
	 * 
	 * @param isDataUpdateNeeded
	 * @param connectionToBeUpdated
	 * @param demandToBeUpdated
	 * @param processInstance
	 */
	@Transactional
	private void updateDataIfNeeded(Demand demandToBeUpdated,
		 Boolean isPaymentReceived, Map<String, Boolean> isDataUpdateNeeded, IssueFix issueFix) {

		if (isDataUpdateNeeded.containsKey(IssueFixConstants.IS_DEMAND_DELETE_NEEDED)
				&& isDataUpdateNeeded.get(IssueFixConstants.IS_DEMAND_DELETE_NEEDED)) {
			repository.deleteDemandDetail(demandToBeUpdated);
			repository.deleteDemand(demandToBeUpdated);
			repository.updateInstallmentDemandId(issueFix);
		}

		if ((isDataUpdateNeeded.containsKey(IssueFixConstants.IS_DEMAND_DELETE_NEEDED)
				&& isDataUpdateNeeded.get(IssueFixConstants.IS_DEMAND_DELETE_NEEDED))) {
			repository.expireBill(demandToBeUpdated.getConsumerCode());

		}

		if (isDataUpdateNeeded.containsKey(IssueFixConstants.IS_INSTALLMENT_DELETE_NEEDED)
				&& isDataUpdateNeeded.get(IssueFixConstants.IS_INSTALLMENT_DELETE_NEEDED)) {
			repository.updateInstallment(issueFix);
		}


	}

	/**
	 * check weather is payment completed flag is updated or not
	 * 
	 * @param issueFixRequest
	 * @param isDataUpdateNeeded
	 */
	private Demand checkIfDemandUpdatedProperly(IssueFixRequest issueFixRequest,
			Map<String, Boolean> isDataUpdateNeeded) {
		Demand demand = new Demand();

		String tenantId = issueFixRequest.getIssueFix().getTenantId();

		String applicationNumber = issueFixRequest.getIssueFix().getApplicationNo();

		DemandSearchCriteria demandSearchCriteria = DemandSearchCriteria.builder().consumerCode(applicationNumber)
				.businessService(IssueFixConstants.BPA_SAN_FEE).tenantId(tenantId).build();

		List<Demand> demands = repository.getDemands(demandSearchCriteria);

		if (demands.size() > 1) {
			log.info("Nos. of sanc fee demand found for application number : " + applicationNumber + " - "
					+ demands.size());
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
				.businessService(IssueFixConstants.BPA_SAN_FEE).tenantId(tenantId).build();

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
