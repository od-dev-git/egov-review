package org.egov.bpa.web.model.demolition;

import java.util.Arrays;
import java.util.List;

import org.egov.bpa.config.BPAConfiguration;
import org.egov.bpa.repository.ServiceRequestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DemolitionCalculationService {
	
	@Autowired
	private ServiceRequestRepository serviceRequestRepository;

	@Autowired
	private BPAConfiguration config;
	
	/**
	 * Call BPA Calcultor's Estimate API here to get the required estimates
	 * 
	 * @param demolitionRequest
	 * @return
	 */
	public Object callBpaCalculatorEstimate(Object demolitionRequest) {
		StringBuilder url = new StringBuilder();
		url.append(this.config.getCalculatorHost());
		url.append(this.config.getDemolitionEstimateEndpoint());
		return serviceRequestRepository.fetchResult(url, demolitionRequest);
	}
	
	/**
	 * Calculation Method for Demolition, will call BPA calculator
	 * 
	 * @param request
	 * @param feeType
	 */
	public void addCalculation(DemolitionRequest request) {
		
		DemolitionCalculationRequest calculationRequest = new DemolitionCalculationRequest();
		calculationRequest.setRequestInfo(request.getRequestInfo());
		
		DemolitionCalculationCriteria criteria = new DemolitionCalculationCriteria();
		criteria.setApplicationNo(request.getDemolition().getApplicationNo());
		criteria.setDemolition(request.getDemolition());
		criteria.setTenantId(request.getDemolition().getTenantId());
		
		List<DemolitionCalculationCriteria> criterias = Arrays.asList(criteria);
		calculationRequest.setCalculationCriteria(criterias);
		
		StringBuilder url = new StringBuilder();
		url.append(this.config.getCalculatorHost());
		url.append(this.config.getDemolitionCalculationEndpoint());
		serviceRequestRepository.fetchResult(url, calculationRequest);
	}

}
