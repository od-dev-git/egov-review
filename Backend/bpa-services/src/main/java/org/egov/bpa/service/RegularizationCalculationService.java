package org.egov.bpa.service;

import java.util.Arrays;
import java.util.List;

import org.egov.bpa.config.BPAConfiguration;
import org.egov.bpa.repository.ServiceRequestRepository;
import org.egov.bpa.web.model.regularization.CalculationRequest;
import org.egov.bpa.web.model.regularization.RegularizationCalculationCriteria;
import org.egov.bpa.web.model.regularization.RegularizationRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class RegularizationCalculationService {

	@Autowired
	private ServiceRequestRepository serviceRequestRepository;

	@Autowired
	private BPAConfiguration config;

	/**
	 * Triggers fee calculation for a regularization application by invoking the BPA
	 * Calculator service.
	 * 
	 * <p>
	 * This method orchestrates the calculation process by:
	 * <ol>
	 * <li>Building the calculation criteria from the regularization request</li>
	 * <li>Constructing the calculation request payload</li>
	 * <li>Invoking the BPA Calculator service endpoint</li>
	 * </ol>
	 * </p>
	 * 
	 * @param request The regularization request containing application details and
	 *                request info
	 * @param feeType The type of fee to calculate (e.g., APPLICATION_FEE,
	 *                SANCTION_FEE)
	 */
	public void addCalculation(RegularizationRequest request, String feeType) {

		// Step 1: Build calculation criteria from regularization request
		RegularizationCalculationCriteria criteria = buildCalculationCriteria(request, feeType);

		// Step 2: Create the calculation request payload
		CalculationRequest calculationRequest = createCalculationRequest(request, criteria);

		// Step 3: Invoke BPA Calculator service
		invokeCalculatorService(calculationRequest);
	}

	/**
	 * Builds the calculation criteria object from the regularization request.
	 * 
	 * <p>
	 * Extracts relevant information from the regularization application required
	 * for fee calculation.
	 * </p>
	 * 
	 * @param request The regularization request containing application details
	 * @param feeType The type of fee to be calculated
	 * @return RegularizationCalculationCriteria populated with application details
	 */
	private RegularizationCalculationCriteria buildCalculationCriteria(RegularizationRequest request, String feeType) {

		RegularizationCalculationCriteria criteria = new RegularizationCalculationCriteria();

		// Set application identification details
		criteria.setApplicationNo(request.getRegularization().getApplicationNo());
		criteria.setApplicationType(request.getRegularization().getAppType().toString());
		criteria.setTenantId(request.getRegularization().getTenantId());

		// Set the regularization entity and fee type for calculation
		criteria.setRegularization(request.getRegularization());
		criteria.setFeeType(feeType);

		return criteria;
	}

	/**
	 * Creates the calculation request payload for the BPA Calculator service.
	 * 
	 * @param request  The original regularization request (for extracting
	 *                 RequestInfo)
	 * @param criteria The calculation criteria to be included in the request
	 * @return CalculationRequest ready to be sent to the calculator service
	 */
	private CalculationRequest createCalculationRequest(RegularizationRequest request,
			RegularizationCalculationCriteria criteria) {

		CalculationRequest calculationRequest = new CalculationRequest();

		// Set request metadata from the original request
		calculationRequest.setRequestInfo(request.getRequestInfo());

		// Wrap criteria in a list as the API expects multiple criteria
		List<RegularizationCalculationCriteria> criteriaList = Arrays.asList(criteria);
		calculationRequest.setCalculationCriteria(criteriaList);

		return calculationRequest;
	}

	/**
	 * Invokes the BPA Calculator service to perform fee calculation.
	 * 
	 * @param calculationRequest The calculation request payload
	 */
	private void invokeCalculatorService(CalculationRequest calculationRequest) {

		// Build the calculator service endpoint URL
		StringBuilder url = new StringBuilder();
		url.append(this.config.getCalculatorHost());
		url.append(this.config.getRegularizationCalculateEndPoint());

		// Execute the API call to calculator service
		serviceRequestRepository.fetchResult(url, calculationRequest);
	}

	/**
	 * Invokes the BPA Calculator's Estimate API to retrieve fee estimates for a
	 * regularization application.
	 * 
	 * <p>
	 * This method is typically called during the application preview phase to
	 * provide the applicant with an estimate of applicable fees before formal
	 * submission.
	 * </p>
	 * 
	 * @param regularizationRequest The regularization request containing
	 *                              application details
	 * @return The estimate response from the BPA Calculator service
	 */
	public Object callBpaCalculatorEstimate(Object regularizationRequest) {

		// Build the calculator service estimate endpoint URL
		StringBuilder url = new StringBuilder();
		url.append(this.config.getCalculatorHost());
		url.append(this.config.getRegularizationEsimateEndPoint());

		// Execute the API call and return the estimate response
		return serviceRequestRepository.fetchResult(url, regularizationRequest);
	}
}
