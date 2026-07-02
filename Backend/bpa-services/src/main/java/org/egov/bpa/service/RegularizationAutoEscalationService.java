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
import org.egov.bpa.repository.RegularizationRepository;
import org.egov.bpa.repository.ServiceRequestRepository;
import org.egov.bpa.util.BPAConstants;
import org.egov.bpa.util.BPAErrorConstants;
import org.egov.bpa.web.model.RequestInfoWrapper;
import org.egov.bpa.web.model.Workflow;
import org.egov.bpa.web.model.landInfo.OwnerInfo;
import org.egov.bpa.web.model.regularization.Regularization;
import org.egov.bpa.web.model.regularization.RegularizationRequest;
import org.egov.bpa.web.model.user.UserDetailResponse;
import org.egov.bpa.web.model.workflow.Action;
import org.egov.bpa.web.model.workflow.BusinessService;
import org.egov.bpa.web.model.workflow.BusinessServiceResponse;
import org.egov.bpa.web.model.workflow.ProcessInstance;
import org.egov.bpa.web.model.workflow.State;
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
import org.springframework.util.CollectionUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class RegularizationAutoEscalationService {

	@Autowired
	private RegularizationRepository repository;

	@Autowired
	private BPAConfiguration config;

	@Autowired
	private ServiceRequestRepository serviceRequestRepository;

	@Autowired
	private ObjectMapper mapper;

	@Autowired
	private RegularizationWorkflow workflowService;

	@Autowired
	private UserService userService;

	@Autowired
	private RegularizationWorkflow workflowIntegrator;

	@Autowired
	private RegularizationShowCauseNoticeService showCauseNoticeService;

	@Autowired
	private RegularizationService regularizationService;

	/**
	 * Main orchestrator method to process Auto Escalation and Show Cause Notice
	 * creation for eligible regularization applications.
	 * 
	 * <p>
	 * This method performs the following operations in sequence:
	 * <ol>
	 * <li>Retrieves all regularization applications currently in workflow</li>
	 * <li>Enriches applications with owner details from user service</li>
	 * <li>Fetches and caches business service configurations for all
	 * applications</li>
	 * <li>Filters and categorizes applications based on eligibility criteria</li>
	 * <li>Processes auto-escalation for eligible applications within workflow</li>
	 * <li>Generates show cause notices for applications that require citizen
	 * action</li>
	 * </ol>
	 * </p>
	 * 
	 * @param requestInfo The request information containing auth token and user
	 *                    context
	 */
	public void processEscalationAndShowCause(RequestInfo requestInfo) {

		log.info("Starting regularization auto-escalation and show cause notice processing");

		// Step 1: Fetch all regularization applications currently in workflow
		List<Regularization> applications = fetchActiveRegularizationApplications(requestInfo);

		// Early exit if no applications are in workflow
		if (CollectionUtils.isEmpty(applications)) {
			log.info("No regularization applications found in workflow. Exiting process.");
			return;
		}

		// Step 2: Fetch and cache business service configurations for all applications
		Map<String, BusinessService> businessServices = fetchAndCacheBusinessServices(applications, requestInfo);

		// Step 3: Categorize applications into auto-escalation and show cause buckets
		Map<Regularization, ProcessInstance> autoEscalationRegularizations = new HashMap<>();
		Map<Regularization, ProcessInstance> showCauseRegularizations = new HashMap<>();
		categorizeApplicationsByEligibility(applications, businessServices, autoEscalationRegularizations,
				showCauseRegularizations, requestInfo);

		// Step 4: Log the categorized applications for audit trail
		logCategorizedApplications(autoEscalationRegularizations, showCauseRegularizations);

		// Step 5: Process auto-escalation for eligible applications
		processAutoEscalationWithInWorkflow(autoEscalationRegularizations, businessServices, requestInfo);

		// Step 6: Process show cause notice generation for eligible applications
		showCauseNoticeService.processShowCauseNoticeWithInWorkflow(showCauseRegularizations, businessServices,
				requestInfo);

		log.info("Completed regularization auto-escalation and show cause notice processing");
	}

	/**
	 * Fetches all regularization applications currently in workflow and enriches
	 * them with owner details.
	 * 
	 * @param requestInfo The request information for API calls
	 * @return List of regularization applications with enriched owner details
	 */
	private List<Regularization> fetchActiveRegularizationApplications(RequestInfo requestInfo) {
		log.debug("Fetching all regularization applications in workflow");

		// Retrieve applications from repository
		List<Regularization> applications = repository.getRegularizationsInWorkflow();

		// Enrich applications with owner details from user service
		if (!CollectionUtils.isEmpty(applications)) {
			log.debug("Enriching {} applications with owner details", applications.size());
			regularizationService.getOwnersDetails(applications, requestInfo);
		}

		return applications;
	}

	/**
	 * Fetches and caches business service configurations for all unique business
	 * services used by the regularization applications.
	 * 
	 * <p>
	 * This method optimizes API calls by caching business service configurations,
	 * ensuring each unique business service is fetched only once.
	 * </p>
	 * 
	 * @param applications List of regularization applications
	 * @param requestInfo  The request information for API calls
	 * @return Map of business service name to BusinessService object
	 */
	private Map<String, BusinessService> fetchAndCacheBusinessServices(List<Regularization> applications,
			RequestInfo requestInfo) {
		log.debug("Fetching and caching business service configurations");

		Map<String, BusinessService> businessServices = new HashMap<>();

		// Iterate through applications and fetch business services on-demand
		for (Regularization application : applications) {
			String businessServiceName = application.getBusinessService();

			// Fetch business service only if not already cached
			if (!businessServices.containsKey(businessServiceName)) {
				log.debug("Fetching business service configuration for: {}", businessServiceName);
				BusinessService businessService = getBusinessService(businessServiceName, application.getTenantId(),
						requestInfo);
				businessServices.put(businessServiceName, businessService);
			}
		}

		log.info("Cached {} unique business service configurations", businessServices.size());
		return businessServices;
	}

	/**
	 * Categorizes regularization applications into auto-escalation and show cause
	 * buckets based on their workflow state, assignee roles, and elapsed time.
	 * 
	 * <p>
	 * Applications are filtered and categorized as follows:
	 * <ul>
	 * <li><b>Auto-escalation:</b> Applications assigned to employees that have
	 * exceeded the escalation threshold</li>
	 * <li><b>Show cause:</b> Applications assigned to citizens/architects/technical
	 * persons that require action</li>
	 * </ul>
	 * </p>
	 * 
	 * @param applications                  List of all regularization applications
	 * @param businessServices              Map of cached business service
	 *                                      configurations
	 * @param autoEscalationRegularizations Output map for applications eligible for
	 *                                      auto-escalation
	 * @param showCauseRegularizations      Output map for applications eligible for
	 *                                      show cause notices
	 * @param requestInfo                   The request information for API calls
	 */
	private void categorizeApplicationsByEligibility(List<Regularization> applications,
			Map<String, BusinessService> businessServices,
			Map<Regularization, ProcessInstance> autoEscalationRegularizations,
			Map<Regularization, ProcessInstance> showCauseRegularizations, RequestInfo requestInfo) {

		log.debug("Categorizing {} applications by eligibility criteria", applications.size());

		for (Regularization application : applications) {
			try {
				// Get the business service configuration for this application
				BusinessService businessService = businessServices.get(application.getBusinessService());

				// Filter and categorize the application based on eligibility rules
				filterRegularizationApplication(application, businessService, autoEscalationRegularizations,
						showCauseRegularizations, requestInfo);

			} catch (Exception e) {
				log.error("Error categorizing application {}: {}", application.getApplicationNo(), e.getMessage(), e);
			}
		}

		log.info("Categorization complete - Auto-escalation: {}, Show cause: {}", autoEscalationRegularizations.size(),
				showCauseRegularizations.size());
	}

	/**
	 * Logs the application numbers for both auto-escalation and show cause
	 * categories for audit trail and debugging purposes.
	 * 
	 * @param autoEscalationRegularizations Map of applications eligible for
	 *                                      auto-escalation
	 * @param showCauseRegularizations      Map of applications eligible for show
	 *                                      cause notices
	 */
	private void logCategorizedApplications(Map<Regularization, ProcessInstance> autoEscalationRegularizations,
			Map<Regularization, ProcessInstance> showCauseRegularizations) {

		// Log auto-escalation application numbers
		String autoEscalationAppNos = autoEscalationRegularizations.keySet().stream()
				.map(Regularization::getApplicationNo).collect(Collectors.toList()).toString();
		log.info("Auto-escalation eligible applications: {}", autoEscalationAppNos);

		// Log show cause application numbers
		String showCauseAppNos = showCauseRegularizations.keySet().stream().map(Regularization::getApplicationNo)
				.collect(Collectors.toList()).toString();
		log.info("Show cause eligible applications: {}", showCauseAppNos);
	}

	/**
	 * Processes auto-escalation for all eligible regularization applications within
	 * the workflow.
	 * 
	 * <p>
	 * This method iterates through all applications that are eligible for
	 * auto-escalation and triggers the workflow transition. Each application is
	 * processed independently, and failures in one application do not affect the
	 * processing of others.
	 * </p>
	 * 
	 * @param autoEscalationRegularizations Map of applications eligible for
	 *                                      auto-escalation with their process
	 *                                      instances
	 * @param businessServices              Map of cached business service
	 *                                      configurations
	 * @param requestInfo                   The request information for API calls
	 */
	private void processAutoEscalationWithInWorkflow(Map<Regularization, ProcessInstance> autoEscalationRegularizations,
			Map<String, BusinessService> businessServices, RequestInfo requestInfo) {

		// Check if auto-escalation feature is enabled in configuration
		if (config.isBpaAutoescalationEnabled()) {
			log.info("Auto-escalation is enabled. Processing {} applications", autoEscalationRegularizations.size());

			// Iterate through each eligible application
			for (Map.Entry<Regularization, ProcessInstance> entry : autoEscalationRegularizations.entrySet()) {
				Regularization regularization = entry.getKey();
				ProcessInstance processInstance = entry.getValue();

				try {
					log.info("Processing auto-escalation for application: {}", regularization.getApplicationNo());

					// Trigger auto-escalation workflow transition
					processAutoEscalation(regularization, processInstance,
							businessServices.get(regularization.getBusinessService()), requestInfo);

					log.info("Successfully completed auto-escalation for application: {}",
							regularization.getApplicationNo());

				} catch (Exception e) {
					// Log error and continue with next application - failure in one should not
					// affect others
					log.error("Failed auto-escalation for application: {} - Error: {}",
							regularization.getApplicationNo(), e.getMessage(), e);
				}
			}
		} else {
			log.info("Auto-escalation is disabled in configuration. Skipping processing.");
		}
	}

	/**
	 * Processes the auto-escalation workflow transition for a single regularization
	 * application.
	 * 
	 * <p>
	 * This method performs the following steps:
	 * <ol>
	 * <li>Identifies the FORWARD action from the current state</li>
	 * <li>Determines the next state and valid assignee roles</li>
	 * <li>Randomly selects an eligible employee from the assignee pool</li>
	 * <li>Triggers the workflow transition with auto-escalation comment</li>
	 * <li>Updates the application in the database</li>
	 * </ol>
	 * </p>
	 * 
	 * @param regularization  The regularization application to escalate
	 * @param processInstance The current workflow process instance
	 * @param businessService The business service configuration
	 * @param requestInfo     The request information for API calls
	 */
	private void processAutoEscalation(Regularization regularization, ProcessInstance processInstance,
			BusinessService businessService, RequestInfo requestInfo) {

		// Find the FORWARD action from the available actions in current state
		List<Action> nextValidAction = processInstance.getState().getActions().stream()
				.filter(a -> BPAConstants.ACTION_FORWORD.equals(a.getAction())).collect(Collectors.toList());

		if (nextValidAction != null && nextValidAction.size() > 0) {
			Action action = nextValidAction.get(0);

			// Set action and comment for auto-escalation
			String actionKey = BPAConstants.ACTION_FORWORD;
			String commentKey = BPAConstants.AUTO_ESCALATED_COMMENT;

			// Determine the roles that can handle the application in the next state
			String roles = getNextValidUserUUIDByNextState(action.getNextState(), businessService, requestInfo);

			// Fetch all users with the required roles
			List<String> nextAssignees = getNextValidUserUUID(roles, regularization.getTenantId(), true, requestInfo);

			// Randomly select an assignee from the eligible users
			String assigneeKey = getRandomValue(nextAssignees);

			// Build the workflow request with the auto-escalation details
			RegularizationRequest regularizationRequest = new RegularizationRequest();
			RequestInfo requestInfoForWf = RequestInfo.builder().authToken(requestInfo.getAuthToken())
					.userInfo(processInstance.getAssignee()).build();
			Workflow wf = new Workflow();
			wf.setAction(actionKey);
			wf.setAssignes(Arrays.asList(assigneeKey));
			wf.setComments(commentKey);
			regularization.setWorkflow(wf);
			regularizationRequest.setRequestInfo(requestInfoForWf);
			regularizationRequest.setRegularization(regularization);

			// Trigger the workflow transition
			workflowIntegrator.callWorkFlow(requestInfoForWf, regularization);

			// Persist the escalated application state
			repository.update(regularizationRequest, true);
		}

	}

	/**
	 * Get User IDS having specific roles from HRMS API
	 * 
	 * @param roles
	 * @param tenantId
	 * @param isActive
	 * @param requestInfo
	 * @return
	 */
	private List<String> getNextValidUserUUID(String roles, String tenantId, boolean isActive,
			RequestInfo requestInfo) {

		// Build the HRMS search endpoint URL with query parameters
		StringBuilder uri = new StringBuilder(config.getHrmshost());
		uri.append(config.getHrmsContextPath());
		uri.append(config.getHrmsSearchEndpoint());
		if (roles != null) {
			uri.append("?roles=");
			uri.append(roles);
		}
		if (tenantId != null) {
			uri.append("&tenantId=");
			uri.append(tenantId);
		}
		if (isActive = true) {
			uri.append("&isActive=true");
		}

		// Execute API call to fetch employees with specified roles
		RequestInfoWrapper requestInfoWrapper = RequestInfoWrapper.builder().requestInfo(requestInfo).build();
		LinkedHashMap fetchResult = null;
		fetchResult = (LinkedHashMap) serviceRequestRepository.fetchResult(uri, requestInfoWrapper);

		// Parse the response and extract user UUIDs using JSON path
		String jsonString = new JSONObject(fetchResult).toString();
		DocumentContext context = JsonPath.using(Configuration.defaultConfiguration()).parse(jsonString);
		List<String> UUID = context.read("Employees.*.uuid");
		System.out.println(UUID.size());
		return UUID;
	}

	/**
	 * Get Random UUID, so that application can be assigned to it
	 * 
	 * @param nextAssignees
	 * @return
	 */
	private String getRandomValue(List<String> nextAssignees) {

		try {
			// Generate random index within the range of available assignees
			Random rand = new Random();
			int randomNum = rand.nextInt(nextAssignees.size());
			return nextAssignees.get(randomNum);
		} catch (Exception e) {
			// Log error if unable to select random assignee (e.g., empty list)
			log.info("Exception caught while generating random assignee : " + String.valueOf(e));
			throw new CustomException("UNABLE_TO_GET_ASSIGNEE", e.getMessage());
		}
	}

	/**
	 * Get User UUID that can perform action in the next state of appl
	 * 
	 * @param stateUUID
	 * @param businessService
	 * @param requestInfo
	 * @return
	 */
	private String getNextValidUserUUIDByNextState(String stateUUID, BusinessService businessService,
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
	 * Filters and categorizes a single regularization application into
	 * auto-escalation or show cause bucket based on eligibility criteria.
	 * 
	 * <p>
	 * This method evaluates the following criteria:
	 * <ul>
	 * <li>Fetches the current workflow process instance for the application</li>
	 * <li>Calculates elapsed time thresholds for auto-escalation and show
	 * cause</li>
	 * <li>Identifies the current assignee (employee or citizen)</li>
	 * <li>Determines eligibility based on assignee role and time elapsed</li>
	 * </ul>
	 * </p>
	 * 
	 * @param application                   The regularization application to
	 *                                      evaluate
	 * @param businessService               The business service configuration
	 * @param autoEscalationRegularizations Output map for applications eligible for
	 *                                      auto-escalation
	 * @param showCauseRegularizations      Output map for applications eligible for
	 *                                      show cause notices
	 * @param requestInfo                   The request information for API calls
	 */
	private void filterRegularizationApplication(Regularization application, BusinessService businessService,
			Map<Regularization, ProcessInstance> autoEscalationRegularizations,
			Map<Regularization, ProcessInstance> showCauseRegularizations, RequestInfo requestInfo) {

		try {
			// Fetch workflow process instance for the application
			List<ProcessInstance> processInstances = workflowService.getProcessInstances(application, requestInfo,
					false);
			if (processInstances != null && processInstances.get(0) != null) {
				ProcessInstance processInstance = processInstances.get(0);

				// Calculate time thresholds for auto-escalation and show cause
				Long autoEscalationRemainder = getTimeinMileSecForAutoEscalation(
						config.getBpaAutoEscalationRemainderAfterDay(), requestInfo);
				Long autoEscalateDays = getTimeinMileSecForAutoEscalation(config.getBpaAutoEscalationEscalateAfterDay(),
						requestInfo);
				Long showcauseNoticeDays = getTimeinMileSec(config.getBpaShowcauseNoticeGenerateAfterDay());

				// Identify the current assignee from repository
				String assigneeuuid = repository.getAssigneeByprocessInstanceId(processInstance.getId());

				log.info("Auto Escalation Date for Regularization :" + application.getApplicationNo() + " is "
						+ autoEscalateDays);

				// If no assignee found in process instance, check if it's assigned to
				// citizen/architect/technical person
				if (assigneeuuid == null) {
					Set<String> validActionRole = Collections.EMPTY_SET;
					if (processInstance.getState() != null && processInstance.getState().getActions() != null
							&& !processInstance.getState().getActions().isEmpty())
						validActionRole = processInstance.getState().getActions().stream().map(a -> a.getRoles())
								.flatMap(List::stream).collect(Collectors.toSet());

					// If action is with citizen/architect/technical person, use the creator as
					// assignee
					if (validActionRole.contains(BPAConstants.CITIZEN)
							|| validActionRole.contains(BPAConstants.BPA_ARCHITECT)
							|| validActionRole.contains(BPAConstants.BPA_TECHNICALPERSON)) {
						assigneeuuid = application.getAuditDetails().getCreatedBy();
					}
				}

				// Categorize application if assignee is identified
				if (assigneeuuid != null) {
					putEligibleApplications(application, autoEscalationRegularizations, showCauseRegularizations,
							requestInfo, processInstance, autoEscalationRemainder, autoEscalateDays,
							showcauseNoticeDays, assigneeuuid);
				}
			}

		} catch (Exception e) {
			log.error("Error while processing " + application.getApplicationNo());
		}

	}

	/**
	 * Evaluates and places an application into the appropriate bucket
	 * (auto-escalation or show cause) based on assignee role and time elapsed since
	 * creation.
	 * 
	 * <p>
	 * Decision criteria:
	 * <ul>
	 * <li><b>Reminder:</b> Employee assigned, time between reminder threshold and
	 * escalation threshold</li>
	 * <li><b>Auto-escalation:</b> Employee assigned, time exceeded escalation
	 * threshold</li>
	 * <li><b>Show cause:</b> Citizen/Architect/Technical person assigned, time
	 * exceeded show cause threshold</li>
	 * </ul>
	 * Applications in PAY or SKIP_PAY states are excluded from processing.
	 * </p>
	 * 
	 * @param application                   The regularization application to
	 *                                      evaluate
	 * @param autoEscalationRegularizations Output map for auto-escalation eligible
	 *                                      applications
	 * @param showCauseRegularizations      Output map for show cause eligible
	 *                                      applications
	 * @param requestInfo                   The request information for API calls
	 * @param processInstance               The current workflow process instance
	 * @param autoEscalationRemainder       Time threshold for sending reminder
	 *                                      notification (in milliseconds)
	 * @param autoEscalateDays              Time threshold for triggering
	 *                                      auto-escalation (in milliseconds)
	 * @param showcauseNoticeDays           Time threshold for generating show cause
	 *                                      notice (in milliseconds)
	 * @param assigneeuuid                  The UUID of the current assignee
	 */
	private void putEligibleApplications(Regularization application,
			Map<Regularization, ProcessInstance> autoEscalationRegularizations,
			Map<Regularization, ProcessInstance> showCauseRegularizations, RequestInfo requestInfo,
			ProcessInstance processInstance, Long autoEscalationRemainder, Long autoEscalateDays,
			Long showcauseNoticeDays, String assigneeuuid) {

		// Fetch user details for the assignee
		UserDetailResponse userDetailResponse = userService.getUserByUUID(assigneeuuid, requestInfo);

		if (userDetailResponse != null && userDetailResponse.getUser().get(0) != null) {
			// Set the assignee in process instance for further processing
			processInstance.setAssignee(copyUser(userDetailResponse.getUser().get(0)));

			// Extract all roles assigned to the user
			Set<String> roles = userDetailResponse.getUser().get(0).getRoles().stream().map(r -> r.getCode())
					.collect(Collectors.toSet());

			// Determine the next valid actions available in current state
			Set<String> nextValidAction = null;
			if (processInstance.getState() != null && processInstance.getState().getActions() != null
					&& !processInstance.getState().getActions().isEmpty())
				nextValidAction = processInstance.getState().getActions().stream().map(a -> a.getAction())
						.collect(Collectors.toSet());

			// Exclude applications in payment states from escalation/show cause processing
			if (nextValidAction != null && !(nextValidAction.contains(BPAConstants.ACTION_PAY)
					|| nextValidAction.contains(BPAConstants.ACTION_SKIP_PAY))) {

				// Reminder notification for employees approaching escalation threshold
				if ((processInstance.getAuditDetails().getCreatedTime() < autoEscalationRemainder
						&& processInstance.getAuditDetails().getCreatedTime() > autoEscalateDays)
						&& roles.contains(BPAConstants.EMPLOYEE)) {
					sendAutoEscalationNotification(application, processInstance, requestInfo);
				}

				// Auto-escalation for employees who have exceeded the threshold
				if ((processInstance.getAuditDetails().getCreatedTime() < autoEscalateDays)
						&& roles.contains(BPAConstants.EMPLOYEE)) {
					if (config.isBpaAutoescalationEnabled())
						autoEscalationRegularizations.put(application, processInstance);
				}
				// Show cause notice for citizens/architects/technical persons who have exceeded
				// threshold
				else if ((processInstance.getAuditDetails().getCreatedTime() < showcauseNoticeDays)
						&& !roles.contains(BPAConstants.EMPLOYEE)) {
					if (config.isBpaShowcauseNoticeEnabled())
						showCauseRegularizations.put(application, processInstance);
				}
			}
		}
	}

	/**
	 * Write code to send reminder notification here
	 * 
	 * @param application
	 * @param processInstance
	 * @param requestInfo
	 */
	private void sendAutoEscalationNotification(Regularization application, ProcessInstance processInstance,
			RequestInfo requestInfo) {
		// TODO Auto-generated method stub

	}

	/**
	 * Get currenttime-days(mentioned in app.prop) in epoch format from here
	 * 
	 * @param days
	 * @return
	 */
	private Long getTimeinMileSec(int days) {
		Instant now = Instant.now(); // current date
		Instant before5Days = now.minus(Duration.ofDays(days));
		Date dateBefore = Date.from(before5Days);
		return dateBefore.getTime();
	}

	/**
	 * Get currenttime-days (mentioned in app.prop) (Only working days to be
	 * considered) in epoch format from here
	 * 
	 * @param bpaAutoEscalationRemainderAfterDay
	 * @param requestInfo
	 * @return
	 */
	private Long getTimeinMileSecForAutoEscalation(int bpaAutoEscalationRemainderAfterDay, RequestInfo requestInfo) {

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

		LocalDate showCauseDateWithoutHolidays = now.minus(Duration.ofDays(bpaAutoEscalationRemainderAfterDay))
				.atZone(ZoneId.systemDefault()).toLocalDate();

		LocalDate autoEsclationTriggerDate = getSlaEndDate(holidays, showCauseDateWithoutHolidays, currentdate);

		return autoEsclationTriggerDate.atStartOfDay().atZone(ZoneOffset.UTC).toInstant().toEpochMilli();
	}

	/**
	 * Get BusinessService object based on businessService name provided
	 * 
	 * @param businessService
	 * @param tenantId
	 * @param requestInfo
	 * @return
	 */
	private BusinessService getBusinessService(String businessService, String tenantId, RequestInfo requestInfo) {
		try {
			StringBuilder url = new StringBuilder(config.getWfHost());
			url.append(config.getWfBusinessServiceSearchPath());
			url.append("?tenantId=");
			url.append(tenantId);
			url.append("&businessServices=");
			url.append(businessService);

			RequestInfoWrapper requestInfoWrapper = RequestInfoWrapper.builder().requestInfo(requestInfo).build();
			Object result = serviceRequestRepository.fetchResult(url, requestInfoWrapper);
			BusinessServiceResponse response = null;
			response = mapper.convertValue(result, BusinessServiceResponse.class);
			return response.getBusinessServices().get(0);

		} catch (IllegalArgumentException e) {
			throw new CustomException(BPAErrorConstants.PARSING_ERROR, "Failed to parse response of calculate");
		}
	}

	/**
	 * Make MDMS call from here to get the calendor info (holidays)
	 * 
	 * @param requestInfo
	 * @return
	 */
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

	/**
	 * Get module info to call MDMS from here FilePath ->
	 * common-masters.HolidayCalender
	 * 
	 * @return
	 */
	private ModuleDetail getHolidayCalenderRequest() {

		List<MasterDetail> mrMasterDetails = new ArrayList<>();

		mrMasterDetails.add(MasterDetail.builder().name(BPAConstants.HOLIDAY_CALENDER_JSON).build());

		ModuleDetail moduleDetails = ModuleDetail.builder().masterDetails(mrMasterDetails)
				.moduleName(BPAConstants.COMMON_MASTERS_MODULE).build();

		return moduleDetails;
	}

	/**
	 * Get MDMS host and search endpoint
	 * 
	 * @return
	 */
	public StringBuilder getMdmsSearchUrl() {
		return new StringBuilder().append(config.getMdmsHost()).append(config.getMdmsEndPoint());
	}

	/**
	 * Consider holidays while calculating days to Auto escalate
	 * 
	 * @param holidays
	 * @param startDate
	 * @param endDate
	 * @return
	 */
	public LocalDate getSlaEndDate(Set<LocalDate> holidays, LocalDate startDate, LocalDate endDate) {
		for (LocalDate holiday : holidays) {
			if (((holiday.isAfter(startDate) && holiday.isBefore(endDate.plusDays(1))) || holiday.isEqual(startDate))) {
				startDate = startDate.minusDays(1);
			}
		}
		return startDate;
	}

	/**
	 * Copy user from one user
	 * 
	 * @param ownerInfo
	 * @return
	 */
	public static User copyUser(OwnerInfo ownerInfo) {
		return User.builder().id(ownerInfo.getId()).emailId(ownerInfo.getEmailId())
				.mobileNumber(ownerInfo.getMobileNumber()).name(ownerInfo.getName()).roles(ownerInfo.getRoles())
				.tenantId(ownerInfo.getTenantId()).type(ownerInfo.getType()).userName(ownerInfo.getUserName())
				.uuid(ownerInfo.getUuid()).build();
	}
}
