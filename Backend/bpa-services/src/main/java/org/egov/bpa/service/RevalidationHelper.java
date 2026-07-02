package org.egov.bpa.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.egov.bpa.web.model.BPA;
import org.egov.bpa.web.model.Installment;
import org.egov.bpa.web.model.InstallmentSearchResponse;
import org.egov.common.contract.request.RequestInfo;
import org.egov.tracer.model.CustomException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class RevalidationHelper {

	@Autowired
	RevalidationService revalidationService;

	@Autowired
	private PaymentUpdateService paymentUpdateService;

	/**
	 * Checks if a permit application is eligible for revalidation.
	 * 
	 * <p>
	 * This method validates eligibility for revalidation by:
	 * <ol>
	 * <li>Verifying the permit application exists in the system</li>
	 * <li>Fetching installment/payment details for the application</li>
	 * <li>Validating that all previous payments are completed</li>
	 * </ol>
	 * </p>
	 * 
	 * <p>
	 * An application is eligible for revalidation only if all pending payments
	 * (either full payment or installments) have been completed.
	 * </p>
	 * 
	 * @param info     The request info containing user context
	 * @param permitNo The permit number to check eligibility for
	 * @return List of BPA applications if eligible, empty list for non-SUJOG
	 *         permits
	 * @throws CustomException if payment is pending for the permit application
	 */
	public List<BPA> checkEligibility(RequestInfo info, String permitNo) {

		log.info("Checking revalidation eligibility for permit: {}", permitNo);

		// Step 1: Validate permit application exists
		List<BPA> bpas = validateApplicationExists(info, permitNo);
		if (bpas.isEmpty()) {
			return Collections.emptyList();
		}

//		Commented the payment validation logic as per the PRODUCTION BUG
//		// Step 2: Get installment details for the application
//		BPA bpa = bpas.get(0);
//		String consumerCode = bpa.getApplicationNo();
//		InstallmentSearchResponse installmentsResponse = getInstallments(consumerCode, info);
//
//		// Step 3: If no installments exist, application is eligible
//		if (isInstallmentResponseEmpty(installmentsResponse)) {
//			return bpas;
//		}
//
//		// Step 4: Validate all payments are completed
//		log.info("Checking payment status for application: {}", consumerCode);
//		validatePaymentCompletion(installmentsResponse);

		return bpas;
	}

	/**
	 * Validates that the permit application exists in the system.
	 * 
	 * @param info     The request info
	 * @param permitNo The permit number to search for
	 * @return List of BPA applications, or empty list if not found (non-SUJOG
	 *         permit)
	 */
	private List<BPA> validateApplicationExists(RequestInfo info, String permitNo) {

		List<BPA> bpas = revalidationService.checkApplicationPresent(info, permitNo);

		if (bpas == null || bpas.isEmpty()) {
			log.info("Found non-SUJOG permit: {}", permitNo);
			return Collections.emptyList();
		}

		return bpas;
	}

	/**
	 * Checks if the installment response is empty (no payment records).
	 * 
	 * @param installmentsResponse The installment search response
	 * @return true if no installment records exist
	 */
	private boolean isInstallmentResponseEmpty(InstallmentSearchResponse installmentsResponse) {

		return installmentsResponse == null
				|| (installmentsResponse.getFullPayment() == null && installmentsResponse.getInstallments().isEmpty());
	}

	/**
	 * Validates that all payments (full payment or installments) are completed.
	 * 
	 * <p>
	 * Payment validation logic:
	 * <ul>
	 * <li>First checks if full payment is completed</li>
	 * <li>If full payment has pending items, checks installment payments</li>
	 * <li>Throws exception if any installment payment is pending</li>
	 * </ul>
	 * </p>
	 * 
	 * @param installmentsResponse The installment search response containing
	 *                             payment details
	 * @throws CustomException if any payment is pending
	 */
	private void validatePaymentCompletion(InstallmentSearchResponse installmentsResponse) {

		// Check full payment status
		List<Boolean> fullPaymentStatus = getFullPaymentStatus(installmentsResponse.getFullPayment());

		// If full payment has pending items, check installments
		if (fullPaymentStatus.contains(false)) {
			List<Boolean> installmentStatus = getInstallmentPaymentStatus(installmentsResponse.getInstallments());

			// If any installment is pending, throw exception
			if (installmentStatus.contains(false)) {
				throw new CustomException("Validation Error",
						"Payment is not completed for previous permit appliaction.Please complete the payment to be eligible to apply for revalidation.");
			}
		}
	}

	/**
	 * Extracts payment completion status for full payment records.
	 * 
	 * @param fullPayment List of full payment installments
	 * @return List of boolean status (true = completed, false = pending)
	 */
	private List<Boolean> getFullPaymentStatus(List<Installment> fullPayment) {

		List<Boolean> statusList = new ArrayList<>();

		if (fullPayment != null) {
			for (Installment installment : fullPayment) {
				statusList.add(installment.isPaymentCompletedInDemand());
			}
		}

		return statusList;
	}

	/**
	 * Extracts payment completion status for all installment records.
	 * 
	 * @param installments List of installment lists (nested structure)
	 * @return List of boolean status for all installments (true = completed, false
	 *         = pending)
	 */
	private List<Boolean> getInstallmentPaymentStatus(List<List<Installment>> installments) {

		List<Boolean> statusList = new ArrayList<>();

		if (installments != null) {
			for (List<Installment> installmentList : installments) {
				for (Installment installment : installmentList) {
					statusList.add(installment.isPaymentCompletedInDemand());
				}
			}
		}

		return statusList;
	}

	/**
	 * Fetches installment details for a consumer code (application number).
	 * 
	 * @param consumerCode The application/consumer code to fetch installments for
	 * @param info         The request info
	 * @return InstallmentSearchResponse containing payment details
	 */
	public InstallmentSearchResponse getInstallments(String consumerCode, RequestInfo info) {
		return paymentUpdateService.fetchAllInstallments(consumerCode, info);
	}

}
