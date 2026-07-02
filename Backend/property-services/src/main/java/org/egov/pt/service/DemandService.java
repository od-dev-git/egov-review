package org.egov.pt.service;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.egov.common.contract.request.RequestInfo;
import org.egov.pt.repository.ServiceRequestRepository;
import org.egov.pt.util.DemandUtils;
import org.egov.pt.web.contracts.Demand;
import org.egov.pt.web.contracts.DemandResponse;
import org.egov.pt.web.contracts.RequestInfoWrapper;
import org.egov.tracer.model.CustomException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class DemandService {

	@Autowired
	private ServiceRequestRepository serviceRequestRepository;
	
	@Autowired
	private DemandUtils utils;
	
	@Autowired
	private ObjectMapper mapper;

	/**
	 * Searches demand for the given consumerCode and tenantIDd
	 * 
	 * @param tenantId
	 *            The tenantId of the tradeLicense
	 * @param consumerCodes
	 *            The set of consumerCode of the demands
	 * @param requestInfo
	 *            The RequestInfo of the incoming request
	 * @return Lis to demands for the given consumerCode
	 */
	public List<Demand> searchDemand(String tenantId, Set<String> consumerCodes, Long taxPeriodFrom, Long taxPeriodTo,
			String businessService, RequestInfo requestInfo) {
        
		Optional<Object> resultOp = serviceRequestRepository.fetchResult(
				utils.getDemandSearchURL(tenantId, consumerCodes, taxPeriodFrom, taxPeriodTo, businessService),
				RequestInfoWrapper.builder().requestInfo(requestInfo).build());
		try {
			return mapper.convertValue(resultOp.get(), DemandResponse.class).getDemands();
		} catch (IllegalArgumentException e) {
			throw new CustomException("PARSING_ERROR", "Failed to parse response from Demand Search");
		}

	}
}
