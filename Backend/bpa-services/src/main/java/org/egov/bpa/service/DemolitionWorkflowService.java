package org.egov.bpa.service;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.egov.bpa.config.BPAConfiguration;
import org.egov.bpa.repository.ServiceRequestRepository;
import org.egov.bpa.util.BPAErrorConstants;
import org.egov.bpa.util.RegularizationConstants;
import org.egov.bpa.web.model.RequestInfoWrapper;
import org.egov.bpa.web.model.demolition.Demolition;
import org.egov.bpa.web.model.demolition.DemolitionRequest;
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
public class DemolitionWorkflowService {

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
	 * Executes workflow transition for a demolition application.
	 * 
	 * <p>
	 * This method orchestrates the complete workflow transition lifecycle:
	 * <ul>
	 * <li>Builds workflow process instance request with application details</li>
	 * <li>Includes action, comments, assignees, and verification documents</li>
	 * <li>Calls workflow service to execute state transition</li>
	 * <li>Updates demolition status from workflow response</li>
	 * </ul>
	 * 
	 * <p>
	 * <strong>Business Logic:</strong>
	 * <ul>
	 * <li>Workflow engine manages state transitions based on configured business
	 * rules</li>
	 * <li>Each action (FORWARD, APPROVE, REJECT, etc.) triggers specific state
	 * changes</li>
	 * <li>Assignees receive task assignments when workflow progresses</li>
	 * <li>Status is synchronized between workflow engine and application</li>
	 * </ul>
	 * 
	 * @param request the demolition request containing application and workflow
	 *                details
	 * @throws CustomException if workflow service call fails or returns invalid
	 *                         response
	 */
	public void callWorkFlow(DemolitionRequest request) {

		RequestInfo requestInfo = request.getRequestInfo();
		Demolition demolition = request.getDemolition();

		log.info("Initiating workflow transition for application: {}, action: {}", demolition.getApplicationNo(),
				demolition.getWorkflow().getAction());

		// Step 1: Build workflow process instance
		JSONObject processInstance = buildProcessInstance(demolition);

		// Step 2: Build complete workflow request
		JSONObject workflowRequest = buildWorkflowRequest(requestInfo, processInstance);

		// Step 3: Call workflow service
		String response = executeWorkflowTransition(workflowRequest, demolition.getApplicationNo());

		// Step 4: Extract status from response
		String newStatus = extractStatusFromResponse(response, demolition.getApplicationNo());

		// Step 5: Update demolition status
		demolition.setStatus(newStatus);

		log.info("Workflow transition completed for application: {}, new status: {}", demolition.getApplicationNo(),
				newStatus);
	}

	/**
	 * Builds the workflow process instance JSON object.
	 * 
	 * <p>
	 * This method constructs the process instance with all required fields:
	 * <ul>
	 * <li><strong>businessId:</strong> Application number for tracking</li>
	 * <li><strong>tenantId:</strong> Tenant/ULB identifier</li>
	 * <li><strong>businessService:</strong> Workflow configuration name</li>
	 * <li><strong>moduleName:</strong> Module identifier (BPA)</li>
	 * <li><strong>action:</strong> Workflow action to execute</li>
	 * <li><strong>comment:</strong> User's comment for this transition</li>
	 * <li><strong>assignees:</strong> Users to assign the task to (optional)</li>
	 * <li><strong>documents:</strong> Verification documents (optional)</li>
	 * </ul>
	 * 
	 * @param demolition the demolition application
	 * @return the JSON process instance object
	 */
	private JSONObject buildProcessInstance(Demolition demolition) {
		String wfTenantId = demolition.getTenantId();

		JSONObject processInstance = new JSONObject();
		processInstance.put(BUSINESSIDKEY, demolition.getApplicationNo());
		processInstance.put(TENANTIDKEY, wfTenantId);
		processInstance.put(BUSINESSSERVICEKEY, demolition.getBusinessService());
		processInstance.put(MODULENAMEKEY, MODULENAMEVALUE);
		processInstance.put(ACTIONKEY, demolition.getWorkflow().getAction());
		processInstance.put(COMMENTKEY, demolition.getWorkflow().getComments());

		// Add assignees if present
		if (!CollectionUtils.isEmpty(demolition.getWorkflow().getAssignes())) {
			List<Map<String, String>> assigneeMaps = buildAssigneeMaps(demolition.getWorkflow().getAssignes());
			processInstance.put(ASSIGNEEKEY, assigneeMaps);
		}

		// Add verification documents if present
		processInstance.put(DOCUMENTSKEY, demolition.getWorkflow().getVarificationDocuments());

		return processInstance;
	}

	/**
	 * Builds assignee maps for workflow process instance.
	 * 
	 * <p>
	 * Converts list of assignee UUIDs to the format required by workflow service.
	 * Each assignee is represented as a map with UUID key.
	 * 
	 * @param assignees the list of assignee UUIDs
	 * @return list of assignee maps
	 */
	private List<Map<String, String>> buildAssigneeMaps(List<String> assignees) {
		List<Map<String, String>> assigneeMaps = new LinkedList<>();

		assignees.forEach(assigneeUuid -> {
			Map<String, String> assigneeMap = new HashMap<>();
			assigneeMap.put(UUIDKEY, assigneeUuid);
			assigneeMaps.add(assigneeMap);
		});

		return assigneeMaps;
	}

	/**
	 * Builds the complete workflow request with RequestInfo and process instances.
	 * 
	 * @param requestInfo     the request information for authentication
	 * @param processInstance the process instance to transition
	 * @return the complete workflow request JSON
	 */
	private JSONObject buildWorkflowRequest(RequestInfo requestInfo, JSONObject processInstance) {
		JSONArray processInstancesArray = new JSONArray();
		processInstancesArray.add(processInstance);

		JSONObject workflowRequest = new JSONObject();
		workflowRequest.put(REQUESTINFOKEY, requestInfo);
		workflowRequest.put(WORKFLOWREQUESTARRAYKEY, processInstancesArray);

		return workflowRequest;
	}

	/**
	 * Executes the workflow transition by calling the workflow service.
	 * 
	 * <p>
	 * This method handles the external service call with comprehensive error
	 * handling:
	 * <ul>
	 * <li>HTTP client errors are parsed to extract workflow-specific error
	 * messages</li>
	 * <li>Generic exceptions are caught and wrapped with context</li>
	 * <li>Application number is included in error messages for traceability</li>
	 * </ul>
	 * 
	 * @param workflowRequest the workflow request JSON
	 * @param applicationNo   the application number (for error messages)
	 * @return the workflow service response string
	 * @throws CustomException if service call fails
	 */
	private String executeWorkflowTransition(JSONObject workflowRequest, String applicationNo) {
		String workflowUrl = config.getWfHost().concat(config.getWfTransitionPath());

		try {
			return rest.postForObject(workflowUrl, workflowRequest, String.class);

		} catch (HttpClientErrorException e) {
			// Extract and throw workflow-specific errors
			String errorMessage = extractWorkflowError(e, applicationNo);
			throw new CustomException(RegularizationConstants.EG_WF_ERROR, errorMessage);

		} catch (Exception e) {
			log.error("Workflow service call failed for application: {}", applicationNo, e);
			throw new CustomException(RegularizationConstants.EG_WF_ERROR,
					String.format("Exception occurred while integrating with workflow for application %s: %s",
							applicationNo, e.getMessage()));
		}
	}

	/**
	 * Extracts workflow-specific error messages from HTTP client error exception.
	 * 
	 * @param e             the HTTP client error exception
	 * @param applicationNo the application number (for error context)
	 * @return the extracted error message
	 */
	private String extractWorkflowError(HttpClientErrorException e, String applicationNo) {
		try {
			DocumentContext responseContext = JsonPath.parse(e.getResponseBodyAsString());
			List<Object> errors = responseContext.read("$.Errors");
			return String.format("Workflow errors for application %s: %s", applicationNo, errors.toString());

		} catch (PathNotFoundException pnfe) {
			log.error("Unable to read error path from workflow response for application: {}", applicationNo, pnfe);
			throw new CustomException(RegularizationConstants.WF_KEY_NOT_FOUND,
					String.format("Unable to read the json path in error object for application %s: %s", applicationNo,
							pnfe.getMessage()));
		}
	}

	/**
	 * Extracts the new application status from workflow service response.
	 * 
	 * <p>
	 * The workflow response contains process instances with updated status. This
	 * method parses the response and extracts the status for the specific
	 * application.
	 * 
	 * @param response      the workflow service response JSON string
	 * @param applicationNo the application number to extract status for
	 * @return the new application status
	 */
	private String extractStatusFromResponse(String response, String applicationNo) {
		DocumentContext responseContext = JsonPath.parse(response);
		List<Map<String, Object>> processInstances = responseContext.read(PROCESSINSTANCESJOSNKEY);

		Map<String, String> applicationStatusMap = new HashMap<>();

		processInstances.forEach(processInstance -> {
			DocumentContext instanceContext = JsonPath.parse(processInstance);
			String businessId = instanceContext.read(BUSINESSIDJOSNKEY);
			String status = instanceContext.read(STATUSJSONKEY);
			applicationStatusMap.put(businessId, status);
		});

		return applicationStatusMap.get(applicationNo);
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
	public BusinessService getBusinessService(Demolition demolition, RequestInfo requestInfo, String applicationNo) {

		StringBuilder url = getSearchURLWithParams(demolition, true, null);
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
	private StringBuilder getSearchURLWithParams(Demolition demolition, boolean businessService, String applicationNo) {
		StringBuilder url = new StringBuilder(config.getWfHost());
		if (businessService) {
			url.append(config.getWfBusinessServiceSearchPath());
		} else {
			url.append(config.getWfProcessPath());
		}
		url.append("?tenantId=");
		url.append(demolition.getTenantId());
		if (businessService) {
			url.append("&businessServices=");
			url.append(demolition.getBusinessService());
		} else {
			url.append("&businessIds=");
			url.append(applicationNo);
		}
		return url;
	}

	public State getCurrentStateObj(String status, BusinessService businessService) {
		for (State state : businessService.getStates()) {
			if (state.getApplicationStatus() != null
					&& state.getApplicationStatus().equalsIgnoreCase(status.toString()))
				return state;
		}
		return null;
	}

	public boolean isStateUpdatable(String status, BusinessService businessService) {
		for (State state : businessService.getStates()) {
			if (state.getApplicationStatus() != null
					&& state.getApplicationStatus().equalsIgnoreCase(status.toString()))
				return state.getIsStateUpdatable();
		}
		return Boolean.FALSE;
	}

	public String getCurrentState(String status, BusinessService businessService) {
		for (State state : businessService.getStates()) {
			if (state.getApplicationStatus() != null
					&& state.getApplicationStatus().equalsIgnoreCase(status.toString()))
				return state.getState();
		}
		return null;
	}

	/**
	 * Retrieves workflow process instances for a demolition application.
	 * 
	 * <p>
	 * This method fetches process instances from the workflow service:
	 * <ul>
	 * <li>Retrieves current process instance by default</li>
	 * <li>Optionally retrieves complete workflow history</li>
	 * <li>Returns workflow state, actions, assignees, and transition history</li>
	 * </ul>
	 * 
	 * <p>
	 * <strong>Business Logic:</strong>
	 * <ul>
	 * <li><strong>Current Instance:</strong> Shows current workflow state and
	 * available actions</li>
	 * <li><strong>History Mode:</strong> Shows complete audit trail of all workflow
	 * transitions</li>
	 * <li>Process instances track who acted, when, what action was taken, and
	 * comments</li>
	 * <li>Essential for workflow UI to display current state and action
	 * buttons</li>
	 * </ul>
	 * 
	 * <p>
	 * <strong>Use Cases:</strong>
	 * <ul>
	 * <li>Determining available workflow actions for current user</li>
	 * <li>Displaying workflow history in application details</li>
	 * <li>Identifying current assignee for task assignment</li>
	 * <li>Audit trail and compliance reporting</li>
	 * </ul>
	 * 
	 * @param demolition  the demolition application
	 * @param requestInfo the request information for authentication
	 * @param history     true to retrieve complete workflow history, false for
	 *                    current state only
	 * @return list of process instances (null if not found)
	 * @throws CustomException if workflow service call fails or response parsing
	 *                         fails
	 */
	public List<ProcessInstance> getProcessInstances(Demolition demolition, RequestInfo requestInfo, boolean history) {

		log.debug("Fetching process instances for application: {}, history: {}", demolition.getApplicationNo(),
				history);

		// Step 1: Build workflow process search URL
		StringBuilder url = buildProcessSearchUrl(demolition, history);

		// Step 2: Call workflow service
		Object response = callWorkflowSearchService(url, requestInfo);

		// Step 3: Parse response to process instance objects
		ProcessInstanceResponse processInstanceResponse = parseProcessInstanceResponse(response,
				demolition.getApplicationNo());

		// Step 4: Extract and return process instances
		return extractProcessInstances(processInstanceResponse);
	}

	/**
	 * Builds the workflow process instance search URL.
	 * 
	 * <p>
	 * URL format:
	 * 
	 * <pre>
	 * {wfHost}{wfProcessPath}?tenantId={tenantId}&businessIds={applicationNo}&history={true/false}
	 * </pre>
	 * 
	 * @param demolition the demolition application
	 * @param history    flag to include workflow history
	 * @return the complete workflow search URL
	 */
	private StringBuilder buildProcessSearchUrl(Demolition demolition, boolean history) {
		StringBuilder url = new StringBuilder(config.getWfHost());
		url.append(config.getWfProcessPath());
		url.append("?tenantId=").append(demolition.getTenantId());
		url.append("&businessIds=").append(demolition.getApplicationNo());

		if (history) {
			url.append("&history=true");
		}

		return url;
	}

	/**
	 * Calls the workflow service to search for process instances.
	 * 
	 * @param url         the workflow service URL
	 * @param requestInfo the request information for authentication
	 * @return the raw response from workflow service (null if not found)
	 */
	private Object callWorkflowSearchService(StringBuilder url, RequestInfo requestInfo) {
		RequestInfoWrapper requestInfoWrapper = RequestInfoWrapper.builder().requestInfo(requestInfo).build();

		return serviceRequestRepository.fetchResult(url, requestInfoWrapper);
	}

	/**
	 * Parses the raw workflow service response to ProcessInstanceResponse object.
	 * 
	 * <p>
	 * This method handles the JSON to object conversion with error handling to
	 * provide clear error messages if parsing fails.
	 * 
	 * @param response      the raw response from workflow service
	 * @param applicationNo the application number (for error context)
	 * @return the parsed process instance response (null if response is null)
	 * @throws CustomException if JSON parsing fails
	 */
	private ProcessInstanceResponse parseProcessInstanceResponse(Object response, String applicationNo) {
		if (response == null) {
			log.debug("No process instances found for application: {}", applicationNo);
			return null;
		}

		try {
			return mapper.convertValue(response, ProcessInstanceResponse.class);

		} catch (IllegalArgumentException e) {
			log.error("Failed to parse workflow response for application: {}", applicationNo, e);
			throw new CustomException(BPAErrorConstants.PARSING_ERROR,
					String.format("Failed to parse workflow process instance response for application %s: %s",
							applicationNo, e.getMessage()));
		}
	}

	/**
	 * Extracts process instances list from the response object.
	 * 
	 * @param processInstanceResponse the process instance response
	 * @return list of process instances (null if response is null)
	 */
	private List<ProcessInstance> extractProcessInstances(ProcessInstanceResponse processInstanceResponse) {
		if (processInstanceResponse != null) {
			return processInstanceResponse.getProcessInstances();
		}
		return null;
	}

}
