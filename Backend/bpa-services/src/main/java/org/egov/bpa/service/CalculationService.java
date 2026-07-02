package org.egov.bpa.service;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.egov.bpa.config.BPAConfiguration;
import org.egov.bpa.repository.ServiceRequestRepository;
import org.egov.bpa.util.BPAConstants;
import org.egov.bpa.web.model.BPARequest;
import org.egov.bpa.web.model.CalculationReq;
import org.egov.bpa.web.model.CalulationCriteria;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class CalculationService {

	private ServiceRequestRepository serviceRequestRepository;

	private BPAConfiguration config;

	@Autowired
	public CalculationService(ServiceRequestRepository serviceRequestRepository, BPAConfiguration config) {
		this.serviceRequestRepository = serviceRequestRepository;
		this.config = config;
	}

	/**
	 * add calculation for the bpa object based on the FeeType
	 * 
	 * @param bpaRequest
	 * @param feeType
	 */
	public void addCalculation(BPARequest bpaRequest, String feeType) {

		CalculationReq calulcationRequest = new CalculationReq();
		calulcationRequest.setRequestInfo(bpaRequest.getRequestInfo());
		CalulationCriteria calculationCriteria = new CalulationCriteria();
		calculationCriteria.setApplicationNo(bpaRequest.getBPA().getApplicationNo());
		calculationCriteria.setBpa(bpaRequest.getBPA());
		calculationCriteria.setFeeType(feeType);
		calculationCriteria.setTenantId(bpaRequest.getBPA().getTenantId());
		List<CalulationCriteria> criterias = Arrays.asList(calculationCriteria);
		calulcationRequest.setCalulationCriteria(criterias);
		StringBuilder url = new StringBuilder();
		url.append(this.config.getCalculatorHost());
		url.append(this.config.getCalulatorEndPoint());

		this.serviceRequestRepository.fetchResult(url, calulcationRequest);
	}

	/**
	 * Adds calculation for BPA application with enhanced criteria support (Version
	 * 2).
	 * 
	 * <p>
	 * This method orchestrates the calculation request lifecycle:
	 * <ul>
	 * <li>Builds calculation criteria with application details</li>
	 * <li>Enriches criteria with revalidation and revision information</li>
	 * <li>Determines appropriate calculator endpoint based on fee type</li>
	 * <li>Calls calculator service to generate demand</li>
	 * </ul>
	 * 
	 * <p>
	 * <strong>Business Logic:</strong>
	 * <ul>
	 * <li><strong>Installment Flow:</strong> For sanction fees with installment
	 * enabled, uses installment creation endpoint instead of standard
	 * calculator</li>
	 * <li><strong>Revalidation Support:</strong> Handles non-Sujog revalidation
	 * applications differently</li>
	 * <li><strong>Revision Support:</strong> Includes revision data for application
	 * fee calculations</li>
	 * </ul>
	 * 
	 * @param bpaRequest      the BPA request containing application details
	 * @param feeType         the type of fee to calculate (APPLICATION_FEE,
	 *                        SANCTION_FEE, etc.)
	 * @param applicationType the application type (BUILDING_PLAN, BUILDING_PLAN_OC,
	 *                        etc.)
	 * @param serviceType     the service type for calculation
	 */
	public void addCalculationV2(BPARequest bpaRequest, String feeType, String applicationType, String serviceType) {

		log.info("Initiating calculation for application: {}, feeType: {}, applicationType: {}",
				bpaRequest.getBPA().getApplicationNo(), feeType, applicationType);

		// Step 1: Build base calculation criteria
		CalulationCriteria calculationCriteria = buildCalculationCriteria(bpaRequest, feeType, applicationType,
				serviceType);

		// Step 2: Enrich with revalidation information if applicable
		enrichRevalidationDetails(calculationCriteria, bpaRequest);

		// Step 3: Enrich with revision information if applicable
		enrichRevisionDetails(calculationCriteria, bpaRequest, feeType);

		// Step 4: Build calculation request
		CalculationReq calculationRequest = buildCalculationRequest(bpaRequest, calculationCriteria);

		// Step 5: Determine endpoint and call calculator service
		String calculatorUrl = determineCalculatorEndpoint(feeType, applicationType);

		log.info("Calling calculator service: {} for application: {}", calculatorUrl,
				bpaRequest.getBPA().getApplicationNo());

		serviceRequestRepository.fetchResult(new StringBuilder(calculatorUrl), calculationRequest);
	}

	/**
	 * Builds the base calculation criteria with application details.
	 * 
	 * @param bpaRequest      the BPA request
	 * @param feeType         the fee type
	 * @param applicationType the application type
	 * @param serviceType     the service type
	 * @return the built calculation criteria
	 */
	private CalulationCriteria buildCalculationCriteria(BPARequest bpaRequest, String feeType, String applicationType,
			String serviceType) {

		CalulationCriteria criteria = new CalulationCriteria();
		criteria.setApplicationNo(bpaRequest.getBPA().getApplicationNo());
		criteria.setBpa(bpaRequest.getBPA());
		criteria.setFeeType(feeType);
		criteria.setApplicationType(applicationType);
		criteria.setServiceType(serviceType);
		criteria.setTenantId(bpaRequest.getBPA().getTenantId());
		criteria.setIsNonSujogRevalidation(Boolean.FALSE);

		return criteria;
	}

	/**
	 * Enriches calculation criteria with revalidation details.
	 * 
	 * <p>
	 * <strong>Business Logic:</strong> Non-Sujog revalidation applications (legacy
	 * applications not in Sujog system) require special handling in fee calculation
	 * to apply appropriate charges for revalidation.
	 * 
	 * @param criteria   the calculation criteria to enrich
	 * @param bpaRequest the BPA request containing revalidation info
	 */
	private void enrichRevalidationDetails(CalulationCriteria criteria, BPARequest bpaRequest) {
		if (Objects.nonNull(bpaRequest.getRevalidation())) {
			if (!bpaRequest.getRevalidation().isSujogExistingApplication()) {
				criteria.setIsNonSujogRevalidation(Boolean.TRUE);
				log.debug("Non-Sujog revalidation detected for application: {}", criteria.getApplicationNo());
			}
		}
	}

	/**
	 * Enriches calculation criteria with revision details.
	 * 
	 * <p>
	 * <strong>Business Logic:</strong> When calculating application fees for
	 * revised plans, revision details must be included to determine if additional
	 * charges apply for the revision.
	 * 
	 * @param criteria   the calculation criteria to enrich
	 * @param bpaRequest the BPA request containing revision info
	 * @param feeType    the fee type being calculated
	 */
	private void enrichRevisionDetails(CalulationCriteria criteria, BPARequest bpaRequest, String feeType) {
		if (BPAConstants.APPLICATION_FEE_KEY.equalsIgnoreCase(feeType) && Objects.nonNull(bpaRequest.getRevision())) {
			criteria.setRevision(bpaRequest.getRevision());
			log.debug("Revision included in calculation for application: {}", criteria.getApplicationNo());
		}
	}

	/**
	 * Builds the calculation request wrapper.
	 * 
	 * @param bpaRequest the BPA request
	 * @param criteria   the calculation criteria
	 * @return the built calculation request
	 */
	private CalculationReq buildCalculationRequest(BPARequest bpaRequest, CalulationCriteria criteria) {
		CalculationReq request = new CalculationReq();
		request.setRequestInfo(bpaRequest.getRequestInfo());
		request.setCalulationCriteria(Arrays.asList(criteria));
		return request;
	}

	/**
	 * Determines the appropriate calculator endpoint based on fee type and
	 * configuration.
	 * 
	 * <p>
	 * <strong>Endpoint Selection Logic:</strong>
	 * <ul>
	 * <li><strong>Installment Endpoint:</strong> Used when:
	 * <ul>
	 * <li>Fee type is SANCTION_FEE</li>
	 * <li>Installment on approval is enabled in configuration</li>
	 * <li>Application type is NOT Occupancy Certificate (OC)</li>
	 * </ul>
	 * </li>
	 * <li><strong>Standard Calculator Endpoint:</strong> Used for all other
	 * scenarios</li>
	 * </ul>
	 * 
	 * <p>
	 * <strong>Business Rule:</strong> Installments allow citizens to pay sanction
	 * fees in parts rather than full upfront payment, improving affordability. OC
	 * applications are excluded as they don't support installment payments.
	 * 
	 * @param feeType         the fee type being calculated
	 * @param applicationType the application type
	 * @return the complete calculator service URL
	 */
	private String determineCalculatorEndpoint(String feeType, String applicationType) {
		StringBuilder url = new StringBuilder(config.getCalculatorHost());

		boolean useInstallmentEndpoint = shouldUseInstallmentEndpoint(feeType, applicationType);

		if (useInstallmentEndpoint) {
			url.append(config.getCreateInstallmentsEndpoint());
			log.info("Using installment creation endpoint for sanction fee");
		} else {
			url.append(config.getCalulatorEndPoint());
			log.debug("Using standard calculator endpoint");
		}

		return url.toString();
	}

	/**
	 * Determines if installment endpoint should be used.
	 * 
	 * @param feeType         the fee type
	 * @param applicationType the application type
	 * @return true if installment endpoint should be used, false otherwise
	 */
	private boolean shouldUseInstallmentEndpoint(String feeType, String applicationType) {
		return feeType.equalsIgnoreCase(BPAConstants.SANCTION_FEE_KEY) && config.isEnableInstallmentOnApproval()
				&& !applicationType.equalsIgnoreCase(BPAConstants.BUILDING_PLAN_OC);
	}

	/**
	 * Adds calculation for revalidation applications (Version 2).
	 * 
	 * <p>
	 * This method is specifically designed for revalidation applications and
	 * differs from the standard calculation flow by:
	 * <ul>
	 * <li>Always using the standard calculator endpoint (no installments for
	 * revalidation)</li>
	 * <li>Handling non-Sujog legacy applications with special fee calculation</li>
	 * </ul>
	 * 
	 * <p>
	 * <strong>Business Logic:</strong>
	 * <ul>
	 * <li><strong>Revalidation:</strong> Applications requesting renewal of expired
	 * approvals</li>
	 * <li><strong>No Installments:</strong> Revalidation fees must be paid in full
	 * upfront</li>
	 * <li><strong>Non-Sujog Handling:</strong> Legacy applications have different
	 * fee structures</li>
	 * </ul>
	 * 
	 * @param bpaRequest      the BPA request containing application details
	 * @param feeType         the type of fee to calculate
	 * @param applicationType the application type
	 * @param serviceType     the service type for calculation
	 */
	public void addCalculationV2ForRevalidation(BPARequest bpaRequest, String feeType, String applicationType,
			String serviceType) {

		log.info("Initiating revalidation calculation for application: {}, feeType: {}",
				bpaRequest.getBPA().getApplicationNo(), feeType);

		// Step 1: Build base calculation criteria
		CalulationCriteria calculationCriteria = buildCalculationCriteria(bpaRequest, feeType, applicationType,
				serviceType);

		// Step 2: Enrich with revalidation information
		enrichRevalidationDetails(calculationCriteria, bpaRequest);

		// Step 3: Enrich with revision information if applicable
		enrichRevisionDetails(calculationCriteria, bpaRequest, feeType);

		// Step 4: Build calculation request
		CalculationReq calculationRequest = buildCalculationRequest(bpaRequest, calculationCriteria);

		// Step 5: Use standard calculator endpoint (no installments for revalidation)
		StringBuilder url = new StringBuilder(config.getCalculatorHost());
		url.append(config.getCalulatorEndPoint());

		log.info("Calling standard calculator endpoint for revalidation: {}", url);

		serviceRequestRepository.fetchResult(url, calculationRequest);
	}

	/**
	 * call bpa-calculator /_estimate API
	 * 
	 * @param bpaRequest
	 */
	public Object callBpaCalculatorEstimate(Object bpaRequest) {
		StringBuilder url = new StringBuilder();
		url.append(this.config.getCalculatorHost());
		url.append(this.config.getBpaCalculationEstimateEndpoint());
		return this.serviceRequestRepository.fetchResult(url, bpaRequest);
	}

	/**
	 * call bpa-calculator /_getAllInstallments API
	 * 
	 * @param bpaRequest
	 */
	public Object getAllInstallments(Object bpaRequest) {
		StringBuilder url = new StringBuilder();
		url.append(this.config.getCalculatorHost());
		url.append(this.config.getFetchAllInstallmentsEndpoint());
		return this.serviceRequestRepository.fetchResult(url, bpaRequest);
	}

	/**
	 * call bpa-calculator /_generateDemandFromInstallments API
	 * 
	 * @param bpaRequest
	 */
	public Object generateDemandFromInstallments(Object bpaRequest) {
		StringBuilder url = new StringBuilder();
		url.append(this.config.getCalculatorHost());
		url.append(this.config.getGenerateDemandsFromInstallmentsEndpoint());
		return this.serviceRequestRepository.fetchResult(url, bpaRequest);
	}

}
