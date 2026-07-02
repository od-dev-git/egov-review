package org.egov.bpa.service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import org.egov.bpa.config.BPAConfiguration;
import org.egov.bpa.repository.BPARepository;
import org.egov.bpa.repository.ServiceRequestRepository;
import org.egov.bpa.util.BPAConstants;
import org.egov.bpa.util.BPAErrorConstants;
import org.egov.bpa.web.model.BPA;
import org.egov.bpa.web.model.BPARequest;
import org.egov.bpa.web.model.RequestInfoWrapper;
import org.egov.bpa.web.model.Workflow;
import org.egov.bpa.web.model.landInfo.OwnerInfo;
import org.egov.bpa.web.model.user.UserDetailResponse;
import org.egov.bpa.web.model.workflow.Action;
import org.egov.bpa.web.model.workflow.BusinessService;
import org.egov.bpa.web.model.workflow.BusinessServiceResponse;
import org.egov.bpa.web.model.workflow.ProcessInstance;
import org.egov.bpa.web.model.workflow.State;
import org.egov.bpa.workflow.WorkflowIntegrator;
import org.egov.bpa.workflow.WorkflowService;
import org.egov.common.contract.request.RequestInfo;
import org.egov.common.contract.request.User;
import org.egov.mdms.model.MasterDetail;
import org.egov.mdms.model.MdmsCriteria;
import org.egov.mdms.model.MdmsCriteriaReq;
import org.egov.mdms.model.ModuleDetail;
import org.egov.tracer.model.CustomException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class BPAAutoEscalationService {

	@Autowired
	private WorkflowService workflowService;

	@Autowired
	private BPARepository bpaRepository;

	@Autowired
	private UserService userService;

	@Autowired
	private BPAConfiguration config;

	@Autowired
	private ServiceRequestRepository serviceRequestRepository;

	@Autowired
	private WorkflowIntegrator workflowIntegrator;

	@Autowired
	private ObjectMapper mapper;

	@Autowired
	private BPAShowCauseNoticeService showCauseNoticeService;

	/**
	 * Processes auto-escalation and show-cause notices for BPA applications pending
	 * in workflow.
	 * 
	 * <p>
	 * This method orchestrates the complete escalation lifecycle:
	 * <ul>
	 * <li>Retrieves all BPA applications currently in workflow</li>
	 * <li>Filters applications requiring auto-escalation or show-cause notices</li>
	 * <li>Processes auto-escalation to forward applications to next approver</li>
	 * <li>Generates show-cause notices for applications pending with citizens</li>
	 * </ul>
	 * 
	 * <p>
	 * <strong>Business Logic:</strong>
	 * <ul>
	 * <li><strong>Auto-Escalation:</strong> Applications pending with officials
	 * beyond SLA are automatically forwarded to the next approver to prevent
	 * delays</li>
	 * <li><strong>Show-Cause:</strong> Applications pending with citizens beyond
	 * deadline trigger show-cause notices for non-compliance</li>
	 * <li>Business service configurations are cached to avoid redundant
	 * lookups</li>
	 * <li>Processing continues even if individual applications fail
	 * (error-tolerant)</li>
	 * </ul>
	 * 
	 * <p>
	 * <strong>Note:</strong> This method is typically invoked by a scheduled job
	 * (cron/batch process) to handle SLA breaches automatically.
	 * 
	 * @param requestInfo the request information for authentication and context
	 */
	public void processEscalation(RequestInfo requestInfo) {

		// Step 1: Retrieve all applications currently in workflow
		List<BPA> bpas = retrieveInWorkflowApplications();

		// Step 2: Cache business service configurations for performance
		Map<String, BusinessService> businessServices = new HashMap<>();

		// Step 3: Categorize applications needing escalation or show-cause
		EscalationCategories categories = categorizeApplicationsForEscalation(bpas, businessServices, requestInfo);

		// Step 4: Log categorization summary
		logEscalationSummary(categories);

		// Step 5: Process auto-escalation for eligible applications
		processAutoEscalationWithInWorkflow(categories.getAutoEscalationBpas(), businessServices, requestInfo);

		// Step 6: Process show-cause notices for eligible applications
		showCauseNoticeService.processShowCauseNoticeWithInWorkflow(categories.getShowCauseBpas(), businessServices,
				requestInfo);
	}

	/**
	 * Retrieves all BPA applications currently in workflow.
	 * 
	 * @return list of BPA applications pending in workflow stages
	 */
	private List<BPA> retrieveInWorkflowApplications() {
		List<BPA> bpas = bpaRepository.getBpaApplicationWithInWorkflow();
		log.info("Total applications in workflow: {}", bpas.size());
		return bpas;
	}

	/**
	 * Categorizes applications into auto-escalation and show-cause buckets.
	 * 
	 * <p>
	 * This method processes each application to determine if it requires:
	 * <ul>
	 * <li>Auto-escalation (officials not acting within SLA)</li>
	 * <li>Show-cause notice (citizens not responding within deadline)</li>
	 * </ul>
	 * 
	 * @param bpas             the list of applications to categorize
	 * @param businessServices the cache for business service configurations
	 * @param requestInfo      the request information
	 * @return EscalationCategories containing categorized applications
	 */
	private EscalationCategories categorizeApplicationsForEscalation(List<BPA> bpas,
			Map<String, BusinessService> businessServices, RequestInfo requestInfo) {

		Map<BPA, ProcessInstance> autoEscalationBpas = new HashMap<>();
		Map<BPA, ProcessInstance> showCauseBpas = new HashMap<>();

		for (BPA bpa : bpas) {
			try {
				// Get or cache business service configuration
				BusinessService businessService = getOrCacheBusinessService(bpa, businessServices, requestInfo);

				log.info("Processing application: {}", bpa.getApplicationNo());

				// Filter and categorize this application
				filterBpaApplication(bpa, businessService, autoEscalationBpas, showCauseBpas, requestInfo);

			} catch (Exception e) {
				log.error("Error categorizing application {}: {}", bpa.getApplicationNo(), e.getMessage(), e);
			}
		}

		return new EscalationCategories(autoEscalationBpas, showCauseBpas);
	}

	/**
	 * Gets business service from cache or fetches and caches it.
	 * 
	 * <p>
	 * This method optimizes performance by caching business service configurations
	 * to avoid repeated lookups for the same business service type.
	 * 
	 * @param bpa              the BPA application
	 * @param businessServices the business service cache
	 * @param requestInfo      the request information
	 * @return the business service configuration
	 */
	private BusinessService getOrCacheBusinessService(BPA bpa, Map<String, BusinessService> businessServices,
			RequestInfo requestInfo) {

		String businessServiceKey = bpa.getBusinessService();
		BusinessService businessService = businessServices.get(businessServiceKey);

		if (businessService == null) {
			businessService = getBusinessService(businessServiceKey, bpa.getTenantId(), requestInfo);
			businessServices.put(businessServiceKey, businessService);
		}

		return businessService;
	}

	/**
	 * Logs summary of applications categorized for escalation processing.
	 * 
	 * @param categories the categorized applications
	 */
	private void logEscalationSummary(EscalationCategories categories) {
		List<String> autoEscalationApps = categories.getAutoEscalationBpas().keySet().stream()
				.map(BPA::getApplicationNo).collect(Collectors.toList());

		List<String> showCauseApps = categories.getShowCauseBpas().keySet().stream().map(BPA::getApplicationNo)
				.collect(Collectors.toList());

		log.info("Auto-escalation applications ({}): {}", autoEscalationApps.size(), autoEscalationApps);
		log.info("Show-cause notice applications ({}): {}", showCauseApps.size(), showCauseApps);
	}

	/**
	 * Inner class to hold categorized applications for escalation processing.
	 */
	private static class EscalationCategories {
		private final Map<BPA, ProcessInstance> autoEscalationBpas;
		private final Map<BPA, ProcessInstance> showCauseBpas;

		public EscalationCategories(Map<BPA, ProcessInstance> autoEscalationBpas,
				Map<BPA, ProcessInstance> showCauseBpas) {
			this.autoEscalationBpas = autoEscalationBpas;
			this.showCauseBpas = showCauseBpas;
		}

		public Map<BPA, ProcessInstance> getAutoEscalationBpas() {
			return autoEscalationBpas;
		}

		public Map<BPA, ProcessInstance> getShowCauseBpas() {
			return showCauseBpas;
		}
	}

	/**
	 * Processes auto-escalation for applications pending beyond SLA with officials.
	 * 
	 * <p>
	 * This method iterates through applications that require auto-escalation and:
	 * <ul>
	 * <li>Forwards them to the next approver in the workflow chain</li>
	 * <li>Assigns them to a random valid approver for workload distribution</li>
	 * <li>Updates the workflow state and persists changes</li>
	 * </ul>
	 * 
	 * <p>
	 * <strong>Business Logic:</strong> Auto-escalation is triggered when officials
	 * do not act on applications within the configured SLA period. The system
	 * automatically forwards the application to prevent delays in citizen service
	 * delivery.
	 * 
	 * <p>
	 * <strong>Note:</strong> Processing continues even if individual applications
	 * fail, ensuring other applications are not blocked by failures.
	 * 
	 * @param autoEscalationBpas map of BPA applications to their process instances
	 * @param businessServices   cached business service configurations
	 * @param requestInfo        the request information for authentication
	 */
	private void processAutoEscalationWithInWorkflow(Map<BPA, ProcessInstance> autoEscalationBpas,
			Map<String, BusinessService> businessServices, RequestInfo requestInfo) {

		// Check if auto-escalation feature is enabled
		if (!config.isBpaAutoescalationEnabled()) {
			log.info("Auto-escalation is disabled in configuration");
			return;
		}

		// Process each application requiring escalation
		for (Map.Entry<BPA, ProcessInstance> entry : autoEscalationBpas.entrySet()) {
			BPA bpa = entry.getKey();
			ProcessInstance processInstance = entry.getValue();

			try {
				log.info("Processing auto-escalation for application: {}", bpa.getApplicationNo());

				// Perform auto-escalation workflow action
				processAutoEscalation(bpa, processInstance, businessServices.get(bpa.getBusinessService()),
						requestInfo);

				log.info("Successfully completed auto-escalation for application: {}", bpa.getApplicationNo());

			} catch (Exception e) {
				log.error("Failed auto-escalation for application {}: {}", bpa.getApplicationNo(), e.getMessage(), e);
			}
		}
	}

	/**
	 * Performs auto-escalation by forwarding application to the next approver.
	 * 
	 * <p>
	 * This method performs the following operations:
	 * <ul>
	 * <li>Identifies the FORWARD action from available workflow actions</li>
	 * <li>Determines the next valid state and eligible approver roles</li>
	 * <li>Selects a random approver from the eligible pool</li>
	 * <li>Executes the workflow transition with auto-escalation comment</li>
	 * <li>Persists the workflow state change to database</li>
	 * </ul>
	 * 
	 * <p>
	 * <strong>Business Logic:</strong> The application is forwarded to the next
	 * approver in the workflow chain with a system-generated comment indicating
	 * auto-escalation due to SLA breach.
	 * 
	 * @param bpa             the BPA application to escalate
	 * @param processInstance the current process instance with workflow state
	 * @param businessService the business service configuration
	 * @param requestInfo     the request information for authentication
	 */
	private void processAutoEscalation(BPA bpa, ProcessInstance processInstance, BusinessService businessService,
			RequestInfo requestInfo) {

		log.info("Determining next valid action for: {}", bpa.getApplicationNo());

		// Step 1: Find the FORWARD action from available actions
		Action forwardAction = findForwardAction(processInstance);

		if (forwardAction == null) {
			log.error("No Forward Action Found For application: {}", bpa.getApplicationNo());
			return;
		}

		// Step 2: Determine next approver roles and users
		String nextApproverRoles = getNextValidUserUUIDByNextState(forwardAction.getNextState(), businessService,
				requestInfo);
		log.info("Next Valid UserUUID By NextState For App {} is: {}", bpa.getApplicationNo(), nextApproverRoles);

		// Step 3: Get list of eligible approvers
		List<String> eligibleApprovers = getNextValidUserUUID(nextApproverRoles, bpa.getTenantId(), true, requestInfo);

		// Step 4: Select random approver for workload distribution
		String selectedApprover = getRandomValue(eligibleApprovers);

		// Step 5: Build and execute workflow request
		executeAutoEscalationWorkflow(bpa, processInstance, selectedApprover, requestInfo);
	}

	/**
	 * Finds the FORWARD action from the available workflow actions.
	 * 
	 * @param processInstance the process instance containing workflow state
	 * @return the FORWARD action if found, null otherwise
	 */
	private Action findForwardAction(ProcessInstance processInstance) {
		List<Action> forwardActions = processInstance.getState().getActions().stream()
				.filter(action -> BPAConstants.ACTION_FORWORD.equals(action.getAction())).collect(Collectors.toList());

		return (!forwardActions.isEmpty()) ? forwardActions.get(0) : null;
	}

	/**
	 * Executes the auto-escalation workflow transition.
	 * 
	 * <p>
	 * This method:
	 * <ul>
	 * <li>Creates a workflow object with FORWARD action</li>
	 * <li>Assigns the selected approver</li>
	 * <li>Adds auto-escalation comment for audit trail</li>
	 * <li>Calls workflow service to transition state</li>
	 * <li>Persists the updated application to database</li>
	 * </ul>
	 * 
	 * @param bpa             the BPA application
	 * @param processInstance the current process instance
	 * @param assigneeUUID    the UUID of the selected approver
	 * @param requestInfo     the request information
	 */
	private void executeAutoEscalationWorkflow(BPA bpa, ProcessInstance processInstance, String assigneeUUID,
			RequestInfo requestInfo) {

		// Build workflow object with escalation details
		Workflow workflow = new Workflow();
		workflow.setAction(BPAConstants.ACTION_FORWORD);
		workflow.setAssignes(Arrays.asList(assigneeUUID));
		workflow.setComments(BPAConstants.AUTO_ESCALATED_COMMENT);

		// Set workflow on BPA
		bpa.setWorkflow(workflow);

		// Build request with current assignee as actor
		BPARequest bpaRequest = new BPARequest();
		bpaRequest.setRequestInfo(RequestInfo.builder().authToken(requestInfo.getAuthToken())
				.userInfo(processInstance.getAssignee()).build());
		bpaRequest.setBPA(bpa);

		// Execute workflow transition
		workflowIntegrator.callWorkFlow(bpaRequest);

		// Persist updated application
		bpaRepository.update(bpaRequest, true);
	}

	private void filterBpaApplication(BPA bpa, BusinessService businessService,
			Map<BPA, ProcessInstance> autoEscalationBpas, Map<BPA, ProcessInstance> showCauseBpas,
			RequestInfo requestInfo) {

		try {
			List<ProcessInstance> processInstances = workflowService.getProcessInstances(bpa, requestInfo, false);

			if (!processInstances.isEmpty()) {
				ProcessInstance processInstance = processInstances.get(0);
				Long autoEscalationRemainder = getTimeinMileSecForAutoEscalation(
						config.getBpaAutoEscalationRemainderAfterDay(), requestInfo);
				Long autoEscalateDays = getTimeinMileSecForAutoEscalation(config.getBpaAutoEscalationEscalateAfterDay(),
						requestInfo);
				Long showcauseNoticeDays = getTimeinMileSec(config.getBpaShowcauseNoticeGenerateAfterDay());
				String assigneeuuid = bpaRepository.getAssigneeByprocessInstanceId(processInstance.getId());

				log.info("Auto Escalation Date for :" + bpa.getApplicationNo() + " is " + autoEscalateDays);

				if (assigneeuuid == null) {
					Set<String> validActionRole = Collections.EMPTY_SET;
					if (processInstance.getState() != null && processInstance.getState().getActions() != null
							&& !processInstance.getState().getActions().isEmpty())
						validActionRole = processInstance.getState().getActions().stream().map(a -> a.getRoles())
								.flatMap(List::stream).collect(Collectors.toSet());

					if (validActionRole.contains(BPAConstants.CITIZEN)
							|| validActionRole.contains(BPAConstants.BPA_ARCHITECT)
							|| validActionRole.contains(BPAConstants.BPA_TECHNICALPERSON)) {
						assigneeuuid = bpa.getAuditDetails().getCreatedBy();
					}
				}

				log.info("Assignee uuid for " + bpa.getApplicationNo() + " is : " + assigneeuuid);
				UserDetailResponse userDetailResponse = null;
				if (assigneeuuid != null) {
					userDetailResponse = userService.getUserByUUID(assigneeuuid, requestInfo);
					if (userDetailResponse != null && userDetailResponse.getUser().get(0) != null) {
						processInstance.setAssignee(copyUser(userDetailResponse.getUser().get(0)));
						Set<String> roles = userDetailResponse.getUser().get(0).getRoles().stream()
								.map(r -> r.getCode()).collect(Collectors.toSet());
						log.info("User Roles for Assignee in " + bpa.getApplicationNo() + " is : "
								+ roles.stream().map(Object::toString).collect(Collectors.joining(",")));
						Set<String> nextValidAction = null;
						if (processInstance.getState() != null && processInstance.getState().getActions() != null
								&& !processInstance.getState().getActions().isEmpty())
							nextValidAction = processInstance.getState().getActions().stream().map(a -> a.getAction())
									.collect(Collectors.toSet());
						log.info("Next Valid Action for " + bpa.getApplicationNo() + " is : "
								+ nextValidAction.stream().map(Object::toString).collect(Collectors.joining(",")));
						if (nextValidAction != null & !(nextValidAction.contains(BPAConstants.ACTION_PAY)
								|| nextValidAction.contains(BPAConstants.ACTION_SKIP_PAY))) {
							if ((processInstance.getAuditDetails().getCreatedTime() < autoEscalationRemainder
									&& processInstance.getAuditDetails().getCreatedTime() > autoEscalateDays)
									&& roles.contains(BPAConstants.EMPLOYEE)) {// Send Notification
								sendAutoEscalationNotification(bpa, processInstance, requestInfo);
							}

							if ((processInstance.getAuditDetails().getCreatedTime() < autoEscalateDays)
									&& roles.contains(BPAConstants.EMPLOYEE)) {// autoEscalation within workflow
								if (config.isBpaAutoescalationEnabled())
									log.info("Auto Escalation Added for Application : " + bpa.getApplicationNo());
								autoEscalationBpas.put(bpa, processInstance);
							} else if ((processInstance.getAuditDetails().getCreatedTime() < showcauseNoticeDays)
									&& !roles.contains(BPAConstants.EMPLOYEE)) {// showcause notice
								if (config.isBpaShowcauseNoticeEnabled())
									log.info("Show Cause Added for Application : " + bpa.getApplicationNo());
								showCauseBpas.put(bpa, processInstance);
							}
						}
					}

				} else {
					log.error("No Assignee User Found For application : " + bpa.getApplicationNo());
				}
			}
		} catch (Exception e) {
			log.error("Error while processing " + bpa.getApplicationNo());
		}

	}

	private void sendAutoEscalationNotification(BPA bpa, ProcessInstance processInstance, RequestInfo requestInfo) {

	}

	/**
	 * Retrieves business service configuration from the workflow service.
	 * 
	 * <p>
	 * This method fetches the complete workflow configuration for a specific
	 * business service type (e.g., BPA, BPA_OC, BPA_LOW) from the workflow
	 * management service.
	 * 
	 * <p>
	 * <strong>Business Service Configuration includes:</strong>
	 * <ul>
	 * <li>Workflow states and their transitions</li>
	 * <li>Actions available at each state</li>
	 * <li>Role-based permissions for actions</li>
	 * <li>SLA configurations for each state</li>
	 * </ul>
	 * 
	 * <p>
	 * <strong>Use Case:</strong> Business service configurations are used to:
	 * <ul>
	 * <li>Determine valid workflow actions for an application</li>
	 * <li>Calculate SLA breaches and escalation timelines</li>
	 * <li>Identify eligible approvers for workflow transitions</li>
	 * </ul>
	 * 
	 * @param businessServiceKey the business service identifier (e.g., "BPA",
	 *                           "BPA_OC")
	 * @param tenantId           the tenant ID for which to fetch configuration
	 * @param requestInfo        the request information for authentication
	 * @return the business service configuration, or null if not found
	 * @throws CustomException if response parsing fails
	 */
	public BusinessService getBusinessService(String businessServiceKey, String tenantId, RequestInfo requestInfo) {
		try {
			// Step 1: Build workflow service URL
			String workflowSearchUrl = buildWorkflowSearchUrl(businessServiceKey, tenantId);

			// Step 2: Prepare request wrapper
			RequestInfoWrapper requestInfoWrapper = RequestInfoWrapper.builder().requestInfo(requestInfo).build();

			// Step 3: Call workflow service
			Object result = serviceRequestRepository.fetchResult(new StringBuilder(workflowSearchUrl),
					requestInfoWrapper);

			// Step 4: Parse and extract business service
			return extractBusinessServiceFromResponse(result);

		} catch (IllegalArgumentException e) {
			throw new CustomException(BPAErrorConstants.PARSING_ERROR,
					"Failed to parse workflow service response for business service: " + businessServiceKey);
		}
	}

	/**
	 * Builds the workflow service search URL with query parameters.
	 * 
	 * <p>
	 * The URL format is:
	 * 
	 * <pre>
	 * {wfHost}{wfBusinessServiceSearchPath}?tenantId={tenantId}&businessServices={businessServiceKey}
	 * </pre>
	 * 
	 * @param businessServiceKey the business service identifier
	 * @param tenantId           the tenant ID
	 * @return the complete workflow service search URL
	 */
	private String buildWorkflowSearchUrl(String businessServiceKey, String tenantId) {
		StringBuilder url = new StringBuilder(config.getWfHost());
		url.append(config.getWfBusinessServiceSearchPath());
		url.append("?tenantId=").append(tenantId);
		url.append("&businessServices=").append(businessServiceKey);

		return url.toString();
	}

	/**
	 * Extracts business service configuration from the workflow service response.
	 * 
	 * <p>
	 * This method parses the response and returns the first business service
	 * configuration if available. The workflow service typically returns a single
	 * business service when queried by specific business service key.
	 * 
	 * @param result the raw response from workflow service
	 * @return the business service configuration, or null if response is empty
	 */
	private BusinessService extractBusinessServiceFromResponse(Object result) {
		BusinessServiceResponse response = mapper.convertValue(result, BusinessServiceResponse.class);

		if (response != null && !response.getBusinessServices().isEmpty()) {
			return response.getBusinessServices().get(0);
		}

		log.warn("No business service configuration found in workflow service response");
		return null;
	}

	private Long getTimeinMileSec(int days) {
		Instant now = Instant.now(); // current date
		Instant before5Days = now.minus(Duration.ofDays(days));
		Date dateBefore = Date.from(before5Days);
		return dateBefore.getTime();
	}

	public static User copyUser(OwnerInfo ownerInfo) {
		return User.builder().id(ownerInfo.getId()).emailId(ownerInfo.getEmailId())
				.mobileNumber(ownerInfo.getMobileNumber()).name(ownerInfo.getName()).roles(ownerInfo.getRoles())
				.tenantId(ownerInfo.getTenantId()).type(ownerInfo.getType()).userName(ownerInfo.getUserName())
				.uuid(ownerInfo.getUuid()).build();
	}

	public String getNextValidUserUUIDByNextState(String stateUUID, BusinessService businessService,
			RequestInfo requestInfo) {
		StringBuilder roles = new StringBuilder();
		State nextState = businessService.getStateFromUuid(stateUUID);
		Set<String> vroles = new HashSet<>();
		for (Action action : nextState.getActions()) {
			vroles.addAll(action.getRoles());
		}

		for (String s : vroles) {
			roles.append(s + ",");
		}
		return roles.toString().substring(0, roles.length());
	}

	/**
	 * Retrieves list of user UUIDs from HRMS service based on roles and tenant.
	 * 
	 * <p>
	 * This method queries the HRMS (Human Resource Management System) service to
	 * find all employees matching the specified criteria:
	 * <ul>
	 * <li>Filters by role(s) - comma-separated list of role codes</li>
	 * <li>Filters by tenant ID - specific ULB or state</li>
	 * <li>Optionally filters by active status</li>
	 * </ul>
	 * 
	 * <p>
	 * <strong>Business Logic:</strong> This method is used to find eligible
	 * approvers/assignees for workflow tasks. For example:
	 * <ul>
	 * <li>Finding all Planning Assistants in a tenant for assignment</li>
	 * <li>Finding all Engineers for escalation</li>
	 * <li>Retrieving active employees for workload distribution</li>
	 * </ul>
	 * 
	 * <p>
	 * <strong>Note:</strong> Returns empty list if no employees match the criteria.
	 * 
	 * @param roles       comma-separated list of role codes (e.g.,
	 *                    "BPA_VERIFIER,BPA_APPROVER")
	 * @param tenantId    the tenant ID to filter employees
	 * @param isActive    filter for active employees only (true to filter active
	 *                    users)
	 * @param requestInfo the request information for authentication
	 * @return list of employee UUIDs matching the criteria (empty if none found)
	 */
	public List<String> getNextValidUserUUID(String roles, String tenantId, Boolean isActive, RequestInfo requestInfo) {

		// Step 1: Build HRMS search URL with query parameters
		String hrmsSearchUrl = buildHrmsSearchUrl(roles, tenantId, isActive);

		// Step 2: Call HRMS service
		RequestInfoWrapper requestInfoWrapper = RequestInfoWrapper.builder().requestInfo(requestInfo).build();

		LinkedHashMap response = (LinkedHashMap) serviceRequestRepository.fetchResult(new StringBuilder(hrmsSearchUrl),
				requestInfoWrapper);

		// Step 3: Extract and return employee UUIDs
		return extractEmployeeUuidsFromResponse(response);
	}

	/**
	 * Builds HRMS search URL with query parameters.
	 * 
	 * <p>
	 * The URL format is:
	 * 
	 * <pre>
	 * {hrmsHost}{hrmsContextPath}{hrmsSearchEndpoint}?roles={roles}&tenantId={tenantId}&isActive={isActive}
	 * </pre>
	 * 
	 * <p>
	 * <strong>Query Parameters:</strong>
	 * <ul>
	 * <li><strong>roles:</strong> Comma-separated role codes to filter
	 * employees</li>
	 * <li><strong>tenantId:</strong> Tenant identifier for ULB-specific search</li>
	 * <li><strong>isActive:</strong> Filter for active employees (true/false)</li>
	 * </ul>
	 * 
	 * @param roles    the comma-separated role codes (can be null)
	 * @param tenantId the tenant ID (can be null)
	 * @param isActive flag to filter active employees
	 * @return the complete HRMS search URL
	 */
	private String buildHrmsSearchUrl(String roles, String tenantId, Boolean isActive) {
		StringBuilder url = new StringBuilder(config.getHrmshost());
		url.append(config.getHrmsContextPath());
		url.append(config.getHrmsSearchEndpoint());

		// Add query parameters
		boolean firstParam = true;

		if (roles != null) {
			url.append("?roles=").append(roles);
			firstParam = false;
		}

		if (tenantId != null) {
			url.append(firstParam ? "?" : "&");
			url.append("tenantId=").append(tenantId);
			firstParam = false;
		}

		// Fixed: Use comparison operator (==) instead of assignment (=)
		if (Boolean.TRUE.equals(isActive)) {
			url.append(firstParam ? "?" : "&");
			url.append("isActive=true");
		}

		return url.toString();
	}

	/**
	 * Extracts employee UUIDs from HRMS service response.
	 * 
	 * <p>
	 * This method parses the HRMS response using JSONPath to extract all employee
	 * UUIDs from the nested JSON structure.
	 * 
	 * <p>
	 * <strong>JSONPath Expression:</strong> {@code Employees.*.uuid}
	 * <ul>
	 * <li>Navigates to the "Employees" array in the response</li>
	 * <li>Extracts the "uuid" field from each employee object</li>
	 * </ul>
	 * 
	 * @param response the raw response from HRMS service
	 * @return list of employee UUIDs (empty if no employees found)
	 */
	private List<String> extractEmployeeUuidsFromResponse(LinkedHashMap response) {
		String jsonString = new JSONObject(response).toString();
		DocumentContext context = JsonPath.using(Configuration.defaultConfiguration()).parse(jsonString);
		List<String> uuids = context.read("Employees.*[?(@.isActive == true)].uuid");

		log.info("Found {} eligible employees from HRMS", uuids.size());
		return uuids;
	}

	public String getRandomValue(List<String> list) {
		try {
			Random rand = new Random();
			int randomNum = rand.nextInt(list.size());
			return list.get(randomNum);
		} catch (Exception e) {
			log.info("Exception caught while generating random assignee : " + String.valueOf(e));
			throw new CustomException("UNABLE_TO_GET_ASSIGNEE", e.getMessage());
		}
	}

	private Long getTimeinMileSecForAutoEscalation(int bpaShowcauseNoticeGenerateAfterDay, RequestInfo requestInfo) {

		Set<LocalDate> holidays = new HashSet<>();
		Long slaDate = null;
		LocalDate slaEndDate = null;
		List<Integer> sla = new ArrayList<Integer>();
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

		Object mdmsHolidayCalenderData = mdmsCallForCalender(requestInfo);
		List<LinkedHashMap<String, Object>> holidayList = JsonPath.read(mdmsHolidayCalenderData,
				BPAConstants.MDMS_HOLIDAY_CALENDER);

		holidayList.forEach(
				hl -> hl.forEach((k, v) -> holidays.add(LocalDate.parse(String.valueOf(hl.get("date")), formatter))));

		Instant now = Instant.now();

		LocalDate currentdate = now.atZone(ZoneId.systemDefault()).toLocalDate();

		LocalDate showCauseDateWithoutHolidays = now.minus(Duration.ofDays(bpaShowcauseNoticeGenerateAfterDay))
				.atZone(ZoneId.systemDefault()).toLocalDate();

		LocalDate showCauseTriggerDate = getSlaEndDate(holidays, showCauseDateWithoutHolidays, currentdate);

		return showCauseTriggerDate.atStartOfDay().atZone(ZoneOffset.UTC).toInstant().toEpochMilli();
	}

	private Object mdmsCallForCalender(RequestInfo requestInfo) {
		ModuleDetail moduleDetail = getHolidayCalenderRequest();
		List<ModuleDetail> holidayCalenderRequest = new LinkedList<>();
		holidayCalenderRequest.add(moduleDetail);
		MdmsCriteria mdmsCriteria = MdmsCriteria.builder().moduleDetails(holidayCalenderRequest)
				.tenantId(BPAConstants.STATE_TENANT_ID).build();
		MdmsCriteriaReq mdmsCriteriaReq = MdmsCriteriaReq.builder().mdmsCriteria(mdmsCriteria).requestInfo(requestInfo)
				.build();
		Object result = serviceRequestRepository.fetchResult(getMdmsSearchUrl(), mdmsCriteriaReq);
		return result;
	}

	private ModuleDetail getHolidayCalenderRequest() {

		List<MasterDetail> mrMasterDetails = new ArrayList<>();

		mrMasterDetails.add(MasterDetail.builder().name(BPAConstants.HOLIDAY_CALENDER_JSON).build());

		ModuleDetail mrModuleDtls = ModuleDetail.builder().masterDetails(mrMasterDetails)
				.moduleName(BPAConstants.COMMON_MASTERS_MODULE).build();

		return mrModuleDtls;
	}

	public StringBuilder getMdmsSearchUrl() {
		return new StringBuilder().append(config.getMdmsHost()).append(config.getMdmsEndPoint());
	}

	public LocalDate getSlaEndDate(Set<LocalDate> holidays, LocalDate startDate, LocalDate endDate) {
		for (LocalDate holiday : holidays) {
			if (((holiday.isAfter(startDate) && holiday.isBefore(endDate.plusDays(1))) || holiday.isEqual(startDate))) {
				startDate = startDate.minusDays(1);
			}
		}
		return startDate;
	}

}
