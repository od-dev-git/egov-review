package org.egov.bpa.calculator.services;

import java.util.LinkedHashMap;

import javax.validation.constraints.NotNull;

import org.apache.commons.lang3.ObjectUtils;
import org.egov.bpa.calculator.config.BPACalculatorConfig;
import org.egov.bpa.calculator.repository.ServiceRequestRepository;
import org.egov.bpa.calculator.utils.BPACalculatorConstants;
import org.egov.bpa.calculator.web.models.RequestInfoWrapper;
import org.egov.bpa.calculator.web.models.demolition.Demolition;
import org.egov.bpa.calculator.web.models.demolition.DemolitionResponse;
import org.egov.common.contract.request.RequestInfo;
import org.egov.tracer.model.CustomException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class DemolitionService {

	@Autowired
	private ServiceRequestRepository serviceRequestRepository;

	@Autowired
	private ObjectMapper mapper;

	@Autowired
	private BPACalculatorConfig config;

	public Demolition getDemolition(@NotNull RequestInfo requestInfo, @NotNull String tenantId,
			@NotNull String applicationNo) {

		StringBuilder url = getDemolitionSearchURL();
		url.append("tenantId=");
		url.append(tenantId);
		url.append("&");
		url.append("applicationNo=");
		url.append(applicationNo);

		LinkedHashMap responseMap = null;
		responseMap = (LinkedHashMap) serviceRequestRepository.fetchResult(url, new RequestInfoWrapper(requestInfo));

		DemolitionResponse demolitionResponse;
		try {
			demolitionResponse = mapper.convertValue(responseMap, DemolitionResponse.class);
		} catch (IllegalArgumentException e) {
			throw new CustomException(BPACalculatorConstants.PARSING_ERROR,
					"Error while parsing response of Demolition Search");
		}

		if (ObjectUtils.isEmpty(demolitionResponse)) {
			throw new CustomException(BPACalculatorConstants.PARSING_ERROR,
					"No value found in response of Demolition Search");
		}
		return demolitionResponse.getDemolitions().get(0);
	}

	private StringBuilder getDemolitionSearchURL() {

		StringBuilder url = new StringBuilder(config.getBpaHost());
		url.append(config.getDemolitionSearchEndpoint());
		url.append("?");
		return url;
	}

}
