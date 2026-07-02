package org.egov.bpa.workflow;

import java.util.List;
import java.util.Map;

import org.egov.bpa.config.BPAConfiguration;
import org.egov.bpa.repository.ServiceRequestRepository;
import org.egov.bpa.util.BPAConstants;
import org.egov.bpa.util.BPAErrorConstants;
import org.egov.bpa.web.model.BPA;
import org.egov.bpa.web.model.BpaApplicationSearch;
import org.egov.bpa.web.model.RequestInfoWrapper;
import org.egov.bpa.web.model.regularization.Regularization;
import org.egov.bpa.web.model.workflow.BusinessService;
import org.egov.bpa.web.model.workflow.BusinessServiceResponse;
import org.egov.bpa.web.model.workflow.ProcessInstance;
import org.egov.bpa.web.model.workflow.ProcessInstanceResponse;
import org.egov.bpa.web.model.workflow.State;
import org.egov.common.contract.request.RequestInfo;
import org.egov.tracer.model.CustomException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.TypeRef;

@Service
public class WorkflowService {

	private BPAConfiguration config;

	private ServiceRequestRepository serviceRequestRepository;

	private ObjectMapper mapper;

	@Autowired
	public WorkflowService(BPAConfiguration config, ServiceRequestRepository serviceRequestRepository,
			ObjectMapper mapper) {
		this.config = config;
		this.serviceRequestRepository = serviceRequestRepository;
		this.mapper = mapper;
	}

	/**
	 * Get the workflow config for the given tenant
	 * 
	 * @param tenantId    The tenantId for which businessService is requested
	 * @param requestInfo The RequestInfo object of the request
	 * @return BusinessService for the the given tenantId
	 */
	public BusinessService getBusinessService(BPA bpa, RequestInfo requestInfo, String applicationNo) {
		StringBuilder url = getSearchURLWithParams(bpa, true, null);
		RequestInfoWrapper requestInfoWrapper = RequestInfoWrapper.builder().requestInfo(requestInfo).build();
		Object result = serviceRequestRepository.fetchResult(url, requestInfoWrapper);
		BusinessServiceResponse response = null;
		try {
			response = mapper.convertValue(result, BusinessServiceResponse.class);
		} catch (IllegalArgumentException e) {
			throw new CustomException(BPAErrorConstants.PARSING_ERROR, "Failed to parse response of calculate");
		}
		return response.getBusinessServices().get(0);
	}

	/**
	 * Creates url for search based on given tenantId
	 *
	 * @param tenantId The tenantId for which url is generated
	 * @return The search url
	 */
	private StringBuilder getSearchURLWithParams(BPA bpa, boolean businessService, String applicationNo) {
		StringBuilder url = new StringBuilder(config.getWfHost());
		if (businessService) {
			url.append(config.getWfBusinessServiceSearchPath());
		} else {
			url.append(config.getWfProcessPath());
		}
		url.append("?tenantId=");
		url.append(bpa.getTenantId());
		if (businessService) {
			url.append("&businessServices=");
			url.append(bpa.getBusinessService());
		} else {
			url.append("&businessIds=");
			url.append(applicationNo);
		}
		return url;
	}

	/**
	 * Returns boolean value to specifying if the state is updatable
	 * 
	 * @param statusEnum      The stateCode of the bpa
	 * @param businessService The BusinessService of the application flow
	 * @return State object to be fetched
	 */
	public Boolean isStateUpdatable(String status, BusinessService businessService) {
		for (org.egov.bpa.web.model.workflow.State state : businessService.getStates()) {
			if (state.getApplicationStatus() != null
					&& state.getApplicationStatus().equalsIgnoreCase(status.toString()))
				return state.getIsStateUpdatable();
		}
		return Boolean.FALSE;
	}

	/**
	 * Returns State name fo the current state of the document
	 * 
	 * @param statusEnum      The stateCode of the bpa
	 * @param businessService The BusinessService of the application flow
	 * @return State String to be fetched
	 */
	public String getCurrentState(String status, BusinessService businessService) {
		for (State state : businessService.getStates()) {
			if (state.getApplicationStatus() != null
					&& state.getApplicationStatus().equalsIgnoreCase(status.toString()))
				return state.getState();
		}
		return null;
	}

	/**
	 * Returns State Obj fo the current state of the document
	 * 
	 * @param statusEnum      The stateCode of the bpa
	 * @param businessService The BusinessService of the application flow
	 * @return State object to be fetched
	 */
	public State getCurrentStateObj(String status, BusinessService businessService) {
		for (State state : businessService.getStates()) {
			if (state.getApplicationStatus() != null
					&& state.getApplicationStatus().equalsIgnoreCase(status.toString()))
				return state;
		}
		return null;
	}

	public List<ProcessInstance> getProcessInstances(BPA bpa, RequestInfo requestInfo,
			boolean history) {
//		StringBuilder url = getSearchURLWithParams(bpa, false, bpa.getApplicationNo());
		StringBuilder url = new StringBuilder(config.getWfHost());
		url.append(config.getWfProcessPath());
		url.append("?tenantId=");
		url.append(bpa.getTenantId());
		url.append("&businessIds=");
		url.append(bpa.getApplicationNo());
		if (history) {
			url.append("&history=true");
		}
		RequestInfoWrapper requestInfoWrapper = RequestInfoWrapper.builder().requestInfo(requestInfo).build();
		ProcessInstanceResponse processInstanceResponse = null;
		try {
			Object response = serviceRequestRepository.fetchResult(url, requestInfoWrapper);
			if (response != null)
				processInstanceResponse = mapper.convertValue(response, ProcessInstanceResponse.class);
		} catch (IllegalArgumentException e) {
			throw new CustomException(BPAErrorConstants.PARSING_ERROR, "Failed to parse response of calculate");
		}
		if (processInstanceResponse != null)
			return processInstanceResponse.getProcessInstances();

		return null;
	}

	
	
	/**
	 * Get Regularization ProcessInstances
	 * 
	 * @param regularization
	 * @param requestInfo
	 * @param history
	 * @return List<ProcessInstance>
	 */
	public List<ProcessInstance> getRegularizationProcessInstances(Regularization regularization,
			RequestInfo requestInfo, boolean history) {
		StringBuilder url = new StringBuilder(config.getWfHost());
		url.append(config.getWfProcessPath());
		url.append("?tenantId=");
		url.append(regularization.getTenantId());
		url.append("&businessIds=");
		url.append(regularization.getApplicationNo());
		if (history) {
			url.append("&history=true");
		}
		RequestInfoWrapper requestInfoWrapper = RequestInfoWrapper.builder().requestInfo(requestInfo).build();
		ProcessInstanceResponse processInstanceResponse = null;
		try {
			Object response = serviceRequestRepository.fetchResult(url, requestInfoWrapper);
			if (response != null)
				processInstanceResponse = mapper.convertValue(response, ProcessInstanceResponse.class);
		} catch (IllegalArgumentException e) {
			throw new CustomException(BPAErrorConstants.PARSING_ERROR, "Failed to parse response of calculate");
		}
		if (processInstanceResponse != null)
			return processInstanceResponse.getProcessInstances();

		return null;
	}

	public List<ProcessInstance> getProcessInstanceForIssueFix(RequestInfo requestInfo, String applicationNo,
															   String tenantId, String businessServiceValue, Boolean history) {
		StringBuilder url = getProcessInstanceSearchURLIssueFix(tenantId, applicationNo, businessServiceValue,history);
		RequestInfoWrapper requestInfoWrapper = RequestInfoWrapper.builder().requestInfo(requestInfo).build();
		Object result = serviceRequestRepository.fetchResult(url, requestInfoWrapper);
		ProcessInstanceResponse response = null;
		try {
			response = mapper.convertValue(result, ProcessInstanceResponse.class);
		} catch (IllegalArgumentException e) {
			throw new CustomException("PARSING ERROR", "Failed to parse response of process instance");
		}
		return response.getProcessInstances();
	}

	private StringBuilder getProcessInstanceSearchURLIssueFix(String tenantId, String applicationNo, String businessServiceValue, Boolean history) {
		StringBuilder url = new StringBuilder(config.getWfHost());
		url.append(config.getWfProcessPath());
		url.append("?tenantId=");
		url.append(tenantId);
		url.append("&businessservices=");
		url.append(businessServiceValue);
		url.append("&businessIds=");
		url.append(applicationNo);
		url.append("&history=");
		url.append(history);
		return url;

	}
}
