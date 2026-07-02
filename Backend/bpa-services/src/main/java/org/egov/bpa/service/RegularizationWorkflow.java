package org.egov.bpa.service;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.validation.Valid;

import org.egov.bpa.config.BPAConfiguration;
import org.egov.bpa.repository.ServiceRequestRepository;
import org.egov.bpa.util.BPAErrorConstants;
import org.egov.bpa.util.RegularizationConstants;
import org.egov.bpa.web.model.Notice;
import org.egov.bpa.web.model.NoticeRequest;
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
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;

import lombok.extern.slf4j.Slf4j;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;

@Service
@Slf4j
public class RegularizationWorkflow {

	private static final String TENANTIDKEY = "tenantId";

	private static final String BUSINESSSERVICEKEY = "businessService";

	private static final String ACTIONKEY = "action";

	private static final String COMMENTKEY = "comment";

	private static final String MODULENAMEKEY = "moduleName";

	private static final String BUSINESSIDKEY = "businessId";

	private static final String DOCUMENTSKEY = "documents";

	private static final String ASSIGNEEKEY = "assignes";

	private static final String MODULENAMEVALUE = "BPA";

	private static final String UUIDKEY = "uuid";

	private static final String WORKFLOWREQUESTARRAYKEY = "ProcessInstances";

	private static final String REQUESTINFOKEY = "RequestInfo";

	private static final String PROCESSINSTANCESJOSNKEY = "$.ProcessInstances";

	private static final String BUSINESSIDJOSNKEY = "$.businessId";

	private static final String STATUSJSONKEY = "$.state.applicationStatus";

	@Autowired
	private RestTemplate rest;

	@Autowired
	private BPAConfiguration config;

	@Autowired
	private ObjectMapper mapper;

	@Autowired
	private ServiceRequestRepository serviceRequestRepository;

	/**
	 * Executes workflow transition for a regularization application.
	 * 
	 * <p>
	 * This method orchestrates the workflow transition process:
	 * <ol>
	 * <li>Builds the workflow request JSON with action, assignees, and
	 * documents</li>
	 * <li>Calls the workflow service to execute the transition</li>
	 * <li>Parses the response and updates the regularization status</li>
	 * </ol>
	 * </p>
	 * 
	 * @param requestInfo    The request info containing user context
	 * @param regularization The regularization application for workflow transition
	 * @throws CustomException if workflow service call fails
	 */
	public void callWorkFlow(RequestInfo requestInfo, Regularization regularization) {

		// Step 1: Build the workflow request JSON
		JSONObject workFlowRequest = buildWorkflowRequest(requestInfo, regularization);

		// Step 2: Execute workflow transition
		String response = executeWorkflowTransition(workFlowRequest);

		// Step 3: Parse response and update regularization status
		updateRegularizationStatus(regularization, response);
	}

	/**
	 * Builds the workflow request JSON object.
	 * 
	 * @param requestInfo    The request info
	 * @param regularization The regularization entity
	 * @return The constructed workflow request JSON
	 */
	private JSONObject buildWorkflowRequest(RequestInfo requestInfo, Regularization regularization) {

		// Build process instance object
		JSONObject processInstance = buildProcessInstance(regularization);

		// Create request array
		JSONArray processInstanceArray = new JSONArray();
		processInstanceArray.add(processInstance);

		// Build final request
		JSONObject workFlowRequest = new JSONObject();
		workFlowRequest.put(REQUESTINFOKEY, requestInfo);
		workFlowRequest.put(WORKFLOWREQUESTARRAYKEY, processInstanceArray);

		return workFlowRequest;
	}

	/**
	 * Builds a single process instance JSON object for workflow transition.
	 * 
	 * @param regularization The regularization entity
	 * @return The constructed process instance JSON
	 */
	private JSONObject buildProcessInstance(Regularization regularization) {

		JSONObject processInstance = new JSONObject();

		// Set basic workflow fields
		processInstance.put(BUSINESSIDKEY, regularization.getApplicationNo());
		processInstance.put(TENANTIDKEY, regularization.getTenantId());
		processInstance.put(BUSINESSSERVICEKEY, regularization.getBusinessService());
		processInstance.put(MODULENAMEKEY, MODULENAMEVALUE);
		processInstance.put(ACTIONKEY, regularization.getWorkflow().getAction());
		processInstance.put(COMMENTKEY, regularization.getWorkflow().getComments());

		// Add assignees if present
		addAssigneesToProcessInstance(processInstance, regularization);

		// Add verification documents
		processInstance.put(DOCUMENTSKEY, regularization.getWorkflow().getVarificationDocuments());

		return processInstance;
	}

	/**
	 * Adds assignees to the process instance if present in the workflow.
	 * 
	 * <p>
	 * Converts the list of assignee UUIDs to the format expected by workflow
	 * service.
	 * </p>
	 * 
	 * @param processInstance The process instance JSON to add assignees to
	 * @param regularization  The regularization containing workflow with assignees
	 */
	private void addAssigneesToProcessInstance(JSONObject processInstance, Regularization regularization) {

		if (!CollectionUtils.isEmpty(regularization.getWorkflow().getAssignes())) {
			List<Map<String, String>> uuidMaps = new LinkedList<>();

			regularization.getWorkflow().getAssignes().forEach(assignee -> {
				Map<String, String> uuidMap = new HashMap<>();
				uuidMap.put(UUIDKEY, assignee);
				uuidMaps.add(uuidMap);
			});

			processInstance.put(ASSIGNEEKEY, uuidMaps);
		}
	}

	/**
	 * Executes the workflow transition by calling the workflow service.
	 * 
	 * @param workFlowRequest The workflow request JSON
	 * @return The response string from workflow service
	 * @throws CustomException if the workflow call fails
	 */
	private String executeWorkflowTransition(JSONObject workFlowRequest) {

		String response = null;

		try {
			response = rest.postForObject(config.getWfHost().concat(config.getWfTransitionPath()), workFlowRequest,
					String.class);

		} catch (HttpClientErrorException e) {
			handleHttpClientError(e);
		} catch (Exception e) {
			throw new CustomException(RegularizationConstants.EG_WF_ERROR,
					"Exception occured while integrating with workflow : " + e.getMessage());
		}

		return response;
	}

	/**
	 * Handles HTTP client errors from workflow service call.
	 * 
	 * <p>
	 * Extracts error details from the response body and throws a CustomException.
	 * </p>
	 * 
	 * @param e The HTTP client error exception
	 * @throws CustomException with extracted error details
	 */
	private void handleHttpClientError(HttpClientErrorException e) {

		DocumentContext responseContext = JsonPath.parse(e.getResponseBodyAsString());
		List<Object> errors = null;

		try {
			errors = responseContext.read("$.Errors");
		} catch (PathNotFoundException pnfe) {
			log.error(RegularizationConstants.WF_KEY_NOT_FOUND,
					" Unable to read the json path in error object : " + pnfe.getMessage());
			throw new CustomException(RegularizationConstants.WF_KEY_NOT_FOUND,
					" Unable to read the json path in error object : " + pnfe.getMessage());
		}

		throw new CustomException(RegularizationConstants.EG_WF_ERROR, errors.toString());
	}

	/**
	 * Parses the workflow response and updates the regularization status.
	 * 
	 * @param regularization The regularization to update
	 * @param response       The workflow service response
	 */
	private void updateRegularizationStatus(Regularization regularization, String response) {

		// Parse response JSON
		DocumentContext responseContext = JsonPath.parse(response);
		List<Map<String, Object>> responseArray = responseContext.read(PROCESSINSTANCESJOSNKEY);

		// Build map of business ID to status
		Map<String, String> idStatusMap = new HashMap<>();
		responseArray.forEach(object -> {
			DocumentContext instanceContext = JsonPath.parse(object);
			idStatusMap.put(instanceContext.read(BUSINESSIDJOSNKEY), instanceContext.read(STATUSJSONKEY));
		});

		// Set status on regularization from workflow response
		regularization.setStatus(idStatusMap.get(regularization.getApplicationNo()));
	}

	/**
	 * Get BusinessService Object from this method. Basically the data present in
	 * businessService Table
	 * 
	 * @param regularization
	 * @param requestInfo
	 * @param applicationNo
	 * @return BusinessService
	 */
	public BusinessService getBusinessService(Regularization regularization, RequestInfo requestInfo,
			String applicationNo) {
		StringBuilder url = getSearchURLWithParams(regularization, true, null);
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
	 * Get Workflow search url from this method
	 * 
	 * @param regularization
	 * @param businessService
	 * @param applicationNo
	 * @return
	 */
	private StringBuilder getSearchURLWithParams(Regularization regularization, boolean businessService,
			String applicationNo) {
		StringBuilder url = new StringBuilder(config.getWfHost());
		if (businessService) {
			url.append(config.getWfBusinessServiceSearchPath());
		} else {
			url.append(config.getWfProcessPath());
		}
		url.append("?tenantId=");
		url.append(regularization.getTenantId());
		if (businessService) {
			url.append("&businessServices=");
			url.append(regularization.getBusinessService());
		} else {
			url.append("&businessIds=");
			url.append(applicationNo);
		}
		return url;
	}

	/**
	 * Get the current state from businessService object
	 * 
	 * @param status
	 * @param businessService
	 * @return
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
	 * Get the current state object from businessService object
	 * 
	 * @param status
	 * @param businessService
	 * @return
	 */
	public State getCurrentStateObj(String status, BusinessService businessService) {
		for (State state : businessService.getStates()) {
			if (state.getApplicationStatus() != null
					&& state.getApplicationStatus().equalsIgnoreCase(status.toString()))
				return state;
		}
		return null;
	}

	/**
	 * Check if the state can be updated or not
	 * 
	 * @param status
	 * @param businessService
	 * @return
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
	 * Retrieves process instances for a regularization application from the
	 * workflow service.
	 * 
	 * <p>
	 * This method fetches the workflow process instances associated with an
	 * application:
	 * <ol>
	 * <li>Builds the workflow process search URL with application details</li>
	 * <li>Calls the workflow service to retrieve process instances</li>
	 * <li>Parses and returns the list of process instances</li>
	 * </ol>
	 * </p>
	 * 
	 * @param application The regularization application to get process instances
	 *                    for
	 * @param requestInfo The request info containing user context
	 * @param history     If true, includes historical process instances; if false,
	 *                    only current
	 * @return List of process instances, or null if none found
	 * @throws CustomException if parsing the response fails
	 */
	public List<ProcessInstance> getProcessInstances(Regularization application, RequestInfo requestInfo,
			boolean history) {

		// Step 1: Build workflow process search URL
		StringBuilder url = buildProcessInstanceSearchUrl(application, history);

		// Step 2: Fetch process instances from workflow service
		ProcessInstanceResponse processInstanceResponse = fetchProcessInstanceResponse(url, requestInfo);

		// Step 3: Return process instances if available
		return extractProcessInstances(processInstanceResponse);
	}

	/**
	 * Builds the workflow process instance search URL.
	 * 
	 * @param application The regularization application
	 * @param history     Whether to include historical process instances
	 * @return The constructed URL
	 */
	private StringBuilder buildProcessInstanceSearchUrl(Regularization application, boolean history) {

		StringBuilder url = new StringBuilder(config.getWfHost());
		url.append(config.getWfProcessPath());
		url.append("?tenantId=").append(application.getTenantId());
		url.append("&businessIds=").append(application.getApplicationNo());

		// Add history flag if requested
		if (history) {
			url.append("&history=true");
		}

		return url;
	}

	/**
	 * Fetches process instance response from the workflow service.
	 * 
	 * @param url         The workflow service URL
	 * @param requestInfo The request info
	 * @return The process instance response, or null if no response
	 * @throws CustomException if parsing fails
	 */
	private ProcessInstanceResponse fetchProcessInstanceResponse(StringBuilder url, RequestInfo requestInfo) {

		RequestInfoWrapper requestInfoWrapper = RequestInfoWrapper.builder().requestInfo(requestInfo).build();

		try {
			Object response = serviceRequestRepository.fetchResult(url, requestInfoWrapper);

			if (response != null) {
				return mapper.convertValue(response, ProcessInstanceResponse.class);
			}

		} catch (IllegalArgumentException e) {
			throw new CustomException(BPAErrorConstants.PARSING_ERROR,
					"Failed to parse workflow process instance response");
		}

		return null;
	}

	/**
	 * Extracts process instances from the response.
	 * 
	 * @param processInstanceResponse The response from workflow service
	 * @return List of process instances, or null if response is null
	 */
	private List<ProcessInstance> extractProcessInstances(ProcessInstanceResponse processInstanceResponse) {

		if (processInstanceResponse != null) {
			return processInstanceResponse.getProcessInstances();
		}

		return null;
	}

	/**
	 * Executes workflow transition for a notice (Show Cause Notice, Refusal SCN,
	 * etc.).
	 * 
	 * <p>
	 * This method orchestrates the workflow transition process for notices:
	 * <ol>
	 * <li>Builds the workflow request JSON with notice details, action, and
	 * assignees</li>
	 * <li>Calls the workflow service to execute the transition</li>
	 * <li>Parses the response (status is not set back on notice in this
	 * implementation)</li>
	 * </ol>
	 * </p>
	 * 
	 * @param request The notice request containing notice and workflow details
	 * @throws CustomException if workflow service call fails
	 */
	public void callWorkFlowforNotice(@Valid NoticeRequest request) {

		// Step 1: Build the workflow request JSON for notice
		JSONObject workFlowRequest = buildNoticeWorkflowRequest(request);

		// Step 2: Execute workflow transition
		String response = executeNoticeWorkflowTransition(workFlowRequest);

		// Step 3: Parse response (for logging/validation purposes)
		parseNoticeWorkflowResponse(response);
	}

	/**
	 * Builds the workflow request JSON object for a notice.
	 * 
	 * @param request The notice request
	 * @return The constructed workflow request JSON
	 */
	private JSONObject buildNoticeWorkflowRequest(NoticeRequest request) {

		// Build process instance object for notice
		JSONObject processInstance = buildNoticeProcessInstance(request.getnotice());

		// Create request array
		JSONArray processInstanceArray = new JSONArray();
		processInstanceArray.add(processInstance);

		// Build final request
		JSONObject workFlowRequest = new JSONObject();
		workFlowRequest.put(REQUESTINFOKEY, request.getRequestInfo());
		workFlowRequest.put(WORKFLOWREQUESTARRAYKEY, processInstanceArray);

		return workFlowRequest;
	}

	/**
	 * Builds a single process instance JSON object for notice workflow transition.
	 * 
	 * @param notice The notice entity
	 * @return The constructed process instance JSON
	 */
	private JSONObject buildNoticeProcessInstance(Notice notice) {

		JSONObject processInstance = new JSONObject();

		// Set basic workflow fields using letter number as business ID
		processInstance.put(BUSINESSIDKEY, notice.getLetterNo());
		processInstance.put(TENANTIDKEY, notice.getTenantid());
		processInstance.put(BUSINESSSERVICEKEY, notice.getBusinessService());
		processInstance.put(MODULENAMEKEY, MODULENAMEVALUE);
		processInstance.put(ACTIONKEY, notice.getWorkflow().getAction());
		processInstance.put(COMMENTKEY, notice.getWorkflow().getComments());

		// Add assignees if present
		addAssigneesToNoticeProcessInstance(processInstance, notice);

		// Add verification documents
		processInstance.put(DOCUMENTSKEY, notice.getWorkflow().getVarificationDocuments());

		return processInstance;
	}

	/**
	 * Adds assignees to the notice process instance if present in the workflow.
	 * 
	 * <p>
	 * Converts the list of assignee UUIDs to the format expected by workflow
	 * service.
	 * </p>
	 * 
	 * @param processInstance The process instance JSON to add assignees to
	 * @param notice          The notice containing workflow with assignees
	 */
	private void addAssigneesToNoticeProcessInstance(JSONObject processInstance, Notice notice) {

		if (!CollectionUtils.isEmpty(notice.getWorkflow().getAssignes())) {
			List<Map<String, String>> uuidMaps = new LinkedList<>();

			notice.getWorkflow().getAssignes().forEach(assignee -> {
				Map<String, String> uuidMap = new HashMap<>();
				uuidMap.put(UUIDKEY, assignee);
				uuidMaps.add(uuidMap);
			});

			processInstance.put(ASSIGNEEKEY, uuidMaps);
		}
	}

	/**
	 * Executes the notice workflow transition by calling the workflow service.
	 * 
	 * @param workFlowRequest The workflow request JSON
	 * @return The response string from workflow service
	 * @throws CustomException if the workflow call fails
	 */
	private String executeNoticeWorkflowTransition(JSONObject workFlowRequest) {

		String response = null;

		try {
			response = rest.postForObject(config.getWfHost().concat(config.getWfTransitionPath()), workFlowRequest,
					String.class);

		} catch (HttpClientErrorException e) {
			handleNoticeHttpClientError(e);
		} catch (Exception e) {
			throw new CustomException(BPAErrorConstants.EG_WF_ERROR,
					"Exception occured while integrating with workflow : " + e.getMessage());
		}

		return response;
	}

	/**
	 * Handles HTTP client errors from notice workflow service call.
	 * 
	 * <p>
	 * Extracts error details from the response body and throws a CustomException.
	 * </p>
	 * 
	 * @param e The HTTP client error exception
	 * @throws CustomException with extracted error details
	 */
	private void handleNoticeHttpClientError(HttpClientErrorException e) {

		DocumentContext responseContext = JsonPath.parse(e.getResponseBodyAsString());
		List<Object> errors = null;

		try {
			errors = responseContext.read("$.Errors");
		} catch (PathNotFoundException pnfe) {
			log.error(BPAErrorConstants.EG_BPA_WF_ERROR_KEY_NOT_FOUND,
					" Unable to read the json path in error object : " + pnfe.getMessage());
			throw new CustomException(BPAErrorConstants.EG_BPA_WF_ERROR_KEY_NOT_FOUND,
					" Unable to read the json path in error object : " + pnfe.getMessage());
		}

		throw new CustomException(BPAErrorConstants.EG_WF_ERROR, errors.toString());
	}

	/**
	 * Parses the notice workflow response for validation/logging purposes.
	 * 
	 * <p>
	 * Note: Unlike regularization workflow, the status is not set back on the
	 * notice object in the current implementation.
	 * </p>
	 * 
	 * @param response The workflow service response
	 */
	private void parseNoticeWorkflowResponse(String response) {

		// Parse response JSON
		DocumentContext responseContext = JsonPath.parse(response);
		List<Map<String, Object>> responseArray = responseContext.read(PROCESSINSTANCESJOSNKEY);

		// Build map of business ID to status (for potential future use or logging)
		Map<String, String> idStatusMap = new HashMap<>();
		responseArray.forEach(object -> {
			DocumentContext instanceContext = JsonPath.parse(object);
			idStatusMap.put(instanceContext.read(BUSINESSIDJOSNKEY), instanceContext.read(STATUSJSONKEY));
		});

		// Note: Status is not set back on notice in current implementation
	}

}
