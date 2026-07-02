package org.egov.bpa.calculator.validators;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.egov.bpa.calculator.web.models.Installment;
import org.egov.bpa.calculator.web.models.InstallmentRequest;
import org.egov.tracer.model.CustomException;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class InstallmentValidator {

	/**
     * validates the demand generation request from installments
     * @param InstallmentRequest The installment request
     * @param allInstallments List of all installments
     * @param installmentsToGenerateDemand List of installments to generate demand from
     */
	public void validateInstallmentNoSequence(InstallmentRequest request, List<Installment> allInstallments) {
		log.info("inside method validateInstallmentNoSequence");
		List<Integer> installmentNos=request.getInstallmentSearchCriteria().getInstallmentNos();
		if(CollectionUtils.isEmpty(installmentNos))
			throw new CustomException("installmentNos could not be null or empty","installmentNos could not be null or empty");
		int firstNo=installmentNos.get(0);
		if (firstNo == -1 && installmentNos.size() > 1)
			throw new CustomException("could not generate demand for both full payment and installment",
					"could not generate demand for both full payment and installment");
		if (firstNo == -1 && installmentNos.size() == 1)
			return;
		// new logic for sequence-
		for (int i = 0; i < installmentNos.size(); i++) {
			if ((i != (installmentNos.size() - 1)) && ((installmentNos.get(i) + 1) != installmentNos.get(i + 1)))
				throw new CustomException("please send installmentNos in sequence",
						"please send installmentNos in sequence");
		}
			
	}
	
	/**
	 * validates the demand generation request from installments for all scenarios
	 * 
	 * @param InstallmentRequest           The installment request
	 * @param allInstallments              List of all installments
	 * @param installmentsToGenerateDemand List of installments to generate demand
	 *                                     from
	 */
	public void validateForDemandGeneration(InstallmentRequest request, List<Installment> allInstallments,
			List<Installment> installmentsToGenerateDemand) {
		log.info("inside method validateForDemandGeneration");
		List<Integer> installmentNos=request.getInstallmentSearchCriteria().getInstallmentNos();
		//if already 1st installment demand generated, do not allow demand generation of -1 installments-
		if (installmentNos.size() == 1 && installmentNos.get(0) == -1) {
			validateInstallmentAlreadyInProgress(allInstallments);
		}
		//similarly if already opted full payment, do not allow generation of demands for 1,... installments-
		if (installmentNos.size() == 1 && installmentNos.get(0) == 1) {
			validateInstallmentAlreadyInProgress(allInstallments);
		}
		// no need to check payment status if firstInstallmentNo=1.Only check if demand
		// already generated
		if (installmentNos.get(0) == 1) {
			validateDemandId(installmentsToGenerateDemand);
		}
		
		// Validate Demand exist or not for installement nos 2,3,4
		if (installmentNos.contains(2) || installmentNos.contains(3) || installmentNos.contains(4)) {
			validateDemandId(installmentsToGenerateDemand);
		}
		
		validatePaymentStatusAndDemandIdBothOfPreviousIns(request, allInstallments, installmentsToGenerateDemand);
	}
	
	/**
	 * validates the demand generation request for consumercode
	 * 
	 * @param InstallmentRequest The installment request
	 */
	public void validateConsumerCode(InstallmentRequest request) {
		log.info("inside method validateConsumerCode");
		if (StringUtils.isEmpty(request.getInstallmentSearchCriteria().getConsumerCode()))
			throw new CustomException("consumerCode is mandatory to search installments",
					"consumerCode is mandatory to search installments");
	}
	
	private void validateInstallmentAlreadyInProgress(List<Installment> allInstallments) {
		boolean isInstallmentInProgress = false;
		for (Installment installment : allInstallments) {
			//check if first installment has demandId, it means already opted for payment by installment-
			if (installment.getInstallmentNo() == 1 && !StringUtils.isEmpty(installment.getDemandId())) {
				isInstallmentInProgress = true;
				break;
			}
			if (isInstallmentInProgress)
				throw new CustomException(
						"payment by installment already in progress.Please select payment type Installment",
						"payment by installment already in progress.Please select payment type Installment");
			//check if -1 installment has demandId, it means already opted for full payment-
			if (installment.getInstallmentNo() == -1 && !StringUtils.isEmpty(installment.getDemandId())) {
				isInstallmentInProgress = true;
				break;
			}
		}
		if (isInstallmentInProgress)
			throw new CustomException(
					"Demand already generated for full payment, please proceed to pay",
					"Demand already generated for full payment, please proceed to pay");
		/*
		Optional<Installment> firstInstallmentWithDemandGenerated = allInstallments.stream()
				.filter(installment -> ((installment.getInstallmentNo() == 1)
						&& (!StringUtils.isEmpty(installment.getDemandId()))))
				.findAny();
		if (firstInstallmentWithDemandGenerated.isPresent())
			throw new CustomException(
					"payment by installment already in progress.Please select payment type Installment",
					"payment by installment already in progress.Please select payment type Installment");
					*/
	
	}
	
	private void validateDemandId(List<Installment> installmentsToGenerateDemand) {
		// validate if demand already generated--
		Set<Integer> alreadyGeneratedDemandsForInstallmentNo = new HashSet<>();
		for (Installment installment : installmentsToGenerateDemand) {
			if (!StringUtils.isEmpty(installment.getDemandId()))
				alreadyGeneratedDemandsForInstallmentNo.add(installment.getInstallmentNo());
		}
		if (!alreadyGeneratedDemandsForInstallmentNo.isEmpty()) {
			String installmentNosPresent = alreadyGeneratedDemandsForInstallmentNo.toString().replace("[", "")
					.replace("]", "");
			throw new CustomException(
					"already generated demand for installmentNo: " + installmentNosPresent
							+ " ,Please make payment first",
					"already generated demand for installmentNo: " + installmentNosPresent
							+ " ,Please make payment first");
		}
	}

	private void validatePaymentStatusAndDemandIdBothOfPreviousIns(InstallmentRequest request,
			List<Installment> allInstallments, List<Installment> installmentsToGenerateDemand) {
		List<Integer> installmentNos = request.getInstallmentSearchCriteria().getInstallmentNos();
		int firstInstallmentNo = installmentNos.get(0);
		//no need to check previous if first installmentNo in request is 1-
		if(firstInstallmentNo==1)
			return;
		List<Installment> previousInstallments = allInstallments.stream()
				.filter(installment -> installment.getInstallmentNo() != -1
						&& installment.getInstallmentNo() < firstInstallmentNo)
				.collect(Collectors.toList());
		Set<Integer> previousInstallmentsNotPaid = new HashSet<>();
		for (Installment installment : previousInstallments) {
			// check payment should be complete and demandId should not be null-
			if (!installment.isPaymentCompletedInDemand() || StringUtils.isEmpty(installment.getDemandId()))
				previousInstallmentsNotPaid.add(installment.getInstallmentNo());
		}
		if (!previousInstallmentsNotPaid.isEmpty()) {
			String installmentNosNotPaid = previousInstallmentsNotPaid.toString().replace("[", "").replace("]", "");
			throw new CustomException(
					"please complete payment for previous installmentNo " + installmentNosNotPaid + " first:",
					"please complete payment for previous installmentNo " + installmentNosNotPaid + " first:");
		}
	}
	
}
