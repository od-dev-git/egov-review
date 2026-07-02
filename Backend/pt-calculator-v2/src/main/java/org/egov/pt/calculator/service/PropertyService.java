package org.egov.pt.calculator.service;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.egov.common.contract.request.RequestInfo;
import org.egov.pt.calculator.repository.AssessmentRepository;
import org.egov.pt.calculator.repository.PTCalculatorRepository;
import org.egov.pt.calculator.util.CalculatorUtils;
import org.egov.pt.calculator.web.models.CalculationReq;
import org.egov.pt.calculator.web.models.property.Property;
import org.egov.pt.calculator.web.models.property.PropertyCriteria;
import org.egov.pt.calculator.web.models.property.PropertyResponse;
import org.egov.pt.calculator.web.models.property.RequestInfoWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class PropertyService {

	@Autowired
	private CalculatorUtils utils;

	@Autowired
	private PTCalculatorRepository ptCalculatorRepository;

	@Autowired
	private ObjectMapper mapper;
	
	/**
	 * Searches property based on property ids and returns a map
	 *
	 * @param calculationReq
	 * @return Map of property ids to property
	 */
	public Map<String, Property> getPropertyMap(RequestInfoWrapper requestInfoWrapper, PropertyCriteria criteria) {

		
		log.info(" Searched propertyIds ["+ criteria.getPropertyIds() +"]");
		
		StringBuilder url = utils.getPropertySearchQuery(criteria);

		Object res = ptCalculatorRepository.fetchResult(url, requestInfoWrapper);

		PropertyResponse response = mapper.convertValue(res, PropertyResponse.class);

		Map<String, Property> propertyMap = new HashMap<>();

		response.getProperties().forEach(property -> {
			propertyMap.put(property.getPropertyId(), property);
		});

		return propertyMap;
	}
}
