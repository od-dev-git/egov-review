package org.egov.bpa.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.egov.bpa.config.BPAConfiguration;
import org.egov.bpa.repository.BPARepository;
import org.egov.bpa.repository.IdGenRepository;
import org.egov.bpa.repository.ScnRepository;
import org.egov.bpa.repository.ServiceRequestRepository;
import org.egov.bpa.util.BPAConstants;
import org.egov.bpa.util.NotificationUtil;
import org.egov.bpa.web.model.BPA;
import org.egov.bpa.web.model.BPARequest;
import org.egov.bpa.web.model.BPASearchCriteria;
import org.egov.bpa.web.model.Notice;
import org.egov.bpa.web.model.NoticeRequest;
import org.egov.bpa.web.model.NoticeSearchCriteria;
import org.egov.bpa.web.model.RequestInfoWrapper;
import org.egov.bpa.web.model.Workflow;
import org.egov.bpa.web.model.idgen.IdResponse;
import org.egov.bpa.web.model.landInfo.LandInfo;
import org.egov.bpa.web.model.landInfo.LandSearchCriteria;
import org.egov.bpa.web.model.user.UserDetailResponse;
import org.egov.bpa.web.model.workflow.BusinessService;
import org.egov.bpa.web.model.workflow.ProcessInstance;
import org.egov.bpa.workflow.WorkflowIntegrator;
import org.egov.bpa.workflow.WorkflowService;
import org.egov.common.contract.request.RequestInfo;
import org.egov.tracer.model.CustomException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;

import lombok.extern.slf4j.Slf4j;
import net.logstash.logback.encoder.org.apache.commons.lang.StringUtils;

@Slf4j
@Service
public class BPAShowCauseNoticeService {

	@Autowired
	private Noticeservice noticeservice;

	@Autowired
	private BPAConfiguration config;

	@Autowired
	private IdGenRepository idGenRepository;

	@Autowired
	private EDCRService edcrService;

	@Autowired
	private BPALandService landService;

	@Autowired
	private ServiceRequestRepository serviceRequestRepository;

	@Autowired
	private WorkflowIntegrator workflowIntegrator;

	@Autowired
	private BPARepository bpaRepository;

	@Autowired
	private WorkflowService workflowService;

	@Autowired
	private UserService userService;

	@Autowired
	private ObjectMapper mapper;

	@Autowired
	private ScnRepository scnRepository;

	@Autowired
	private NotificationUtil notificationUtil;

	private static final String DEFAULT_MSG_ID = "20170310130900|en_IN";

	/**
	 * Processes show-cause notices for BPA applications pending with citizens
	 * beyond deadline.
	 * 
	 * <p>
	 * This method orchestrates the show-cause notice generation lifecycle:
	 * <ul>
	 * <li>Iterates through applications eligible for show-cause notices</li>
	 * <li>Generates notices for applications pending citizen action beyond SLA</li>
	 * <li>Handles each application independently to prevent cascading failures</li>
	 * </ul>
	 * 
	 * <p>
	 * <strong>Business Logic:</strong>
	 * <ul>
	 * <li><strong>Show-Cause Notice:</strong> Legal notice sent to citizens when
	 * they fail to respond to application requests within the specified
	 * timeframe</li>
	 * <li>Triggered when application is in SEND_BACK_TO_CITIZEN state beyond
	 * deadline</li>
	 * <li>Provides official documentation of non-compliance for record-keeping</li>
	 * <li>Feature can be enabled/disabled via configuration</li>
	 * </ul>
	 * 
	 * <p>
	 * <strong>Error Handling:</strong> Processing continues even if individual
	 * applications fail to ensure batch processing completes for all eligible
	 * applications.
	 * 
	 * @param autoEscalationBpas map of BPA applications to their process instances
	 *                           requiring notices
	 * @param businessServices   cached business service configurations
	 * @param requestInfo        the request information for authentication and
	 *                           context
	 */
	public void processShowCauseNoticeWithInWorkflow(Map<BPA, ProcessInstance> autoEscalationBpas,
			Map<String, BusinessService> businessServices, RequestInfo requestInfo) {

		// Check if show-cause notice feature is enabled
		if (!config.isBpaShowcauseNoticeEnabled()) {
			log.info("Show-cause notice generation is disabled in configuration");
			return;
		}

		// Process each application requiring show-cause notice
		for (Map.Entry<BPA, ProcessInstance> entry : autoEscalationBpas.entrySet()) {
			BPA bpa = entry.getKey();
			ProcessInstance processInstance = entry.getValue();

			try {
				log.info("Processing show-cause notice for application: {}", bpa.getApplicationNo());

				// Generate show-cause notice
				processShowCauseNotice(bpa, processInstance, businessServices.get(bpa.getBusinessService()),
						requestInfo);

				log.info("Successfully completed show-cause notice for application: {}", bpa.getApplicationNo());

			} catch (Exception e) {
				log.error("Failed to generate show-cause notice for application {}: {}", bpa.getApplicationNo(),
						e.getMessage(), e);
			}
		}
	}

	/**
	 * Processes show-cause notice generation for a single BPA application.
	 * 
	 * <p>
	 * This method:
	 * <ul>
	 * <li>Checks if an active show-cause notice already exists</li>
	 * <li>Generates new notice only if no active notice is present</li>
	 * <li>Uses the assigner's credentials for notice creation</li>
	 * </ul>
	 * 
	 * <p>
	 * <strong>Business Logic:</strong> Only one active show-cause notice should
	 * exist per application at any time. If an active notice already exists, no new
	 * notice is generated to avoid duplicate notifications.
	 * 
	 * @param bpa             the BPA application requiring notice
	 * @param processInstance the current process instance with workflow state
	 * @param businessService the business service configuration
	 * @param requestInfo     the request information for authentication
	 */
	private void processShowCauseNotice(BPA bpa, ProcessInstance processInstance, BusinessService businessService,
			RequestInfo requestInfo) {

		// Step 1: Check for existing active show-cause notices
		List<Notice> activeNotices = getActiveSncINWFNotice(bpa.getApplicationNo(), bpa.getTenantId());

		// Step 2: Generate notice only if no active notice exists
		if (CollectionUtils.isEmpty(activeNotices)) {
			try {
				log.info("Started notice generation for application: {}", bpa.getApplicationNo());

				// Use the assigner's credentials for notice creation
				requestInfo.setUserInfo(processInstance.getAssigner());

				// Generate show-cause notice
				createShowCauseNotice(bpa, processInstance, requestInfo);

				log.info("Notice generation completed for application: {}", bpa.getApplicationNo());

			} catch (Exception e) {
				log.error("Error during notice generation for application {}: {}", bpa.getApplicationNo(),
						e.getMessage(), e);
				throw e;
			}
		} else {
			log.info("Active notice already exists for application: {}, skipping generation", bpa.getApplicationNo());
		}
	}

	/**
	 * Retrieves active show-cause notices for a BPA application.
	 * 
	 * <p>
	 * This method searches for show-cause notices that are:
	 * <ul>
	 * <li>Associated with the specified application number</li>
	 * <li>Of type SCN_IN_WF (show-cause notice in workflow)</li>
	 * <li>Currently active (not closed)</li>
	 * </ul>
	 * 
	 * <p>
	 * <strong>Use Case:</strong> Used to prevent duplicate notice generation by
	 * checking if an active notice already exists for the application.
	 * 
	 * @param applicationNo the BPA application number
	 * @param tenantid      the tenant ID
	 * @return list of active show-cause notices (empty if none found)
	 */
	private List<Notice> getActiveSncINWFNotice(String applicationNo, String tenantid) {
		NoticeSearchCriteria noticeSearchCriteria = NoticeSearchCriteria.builder()
				.letterType(BPAConstants.SHOWCAUSE_LETTER_TYPE_SCN_IN_WF).businessid(applicationNo).tenantid(tenantid)
				.isClosed(false).build();

		return noticeservice.searchNotice(noticeSearchCriteria);
	}

	// genrate showcause notice
	private void createShowCauseNotice(BPA bpa, ProcessInstance processInstance, RequestInfo requestInfo) {
		String tenantId = bpa.getTenantId();
		BPARequest bpaRequest = new BPARequest();
		bpaRequest.setRequestInfo(requestInfo);
		bpaRequest.setBPA(bpa);
		LandSearchCriteria landSearchCriteria = new LandSearchCriteria();
		landSearchCriteria.setIds(Arrays.asList(bpa.getLandId()));
		landSearchCriteria.setTenantId(tenantId);
		ArrayList<LandInfo> landInfo = landService.searchLandInfoToBPA(requestInfo, landSearchCriteria);

		if (landInfo != null && landInfo.size() > 0) {
			LandInfo landData = mapper.convertValue(landInfo.get(0), LandInfo.class);
			bpaRequest.getBPA().setLandInfo(landData);
		}
//		LinkedHashMap edcrDetails = edcrService.getEDCRDetails(bpaRequest);
//		String jsonString = new JSONObject(edcrDetails).toString();
//		DocumentContext context = JsonPath.using(Configuration.defaultConfiguration()).parse(jsonString);
		// List<String> edcrData =
		// context.read("edcrDetail.*.planDetail.planInformation");

		List<String> edcrData = null;

		if (BPAConstants.BPA_PAP_MODULE_CODE.equals(bpa.getBusinessService())) {
			edcrData = this.getEdcrDataForPAP(bpa);
		} else {
			LinkedHashMap edcrDetails = edcrService.getEDCRDetails(bpaRequest);
			String jsonString = new JSONObject(edcrDetails).toString();
			DocumentContext context = JsonPath.using(Configuration.defaultConfiguration()).parse(jsonString);
			edcrData = context.read("edcrDetail.*.planDetail.planInformation");
		}

		List<IdResponse> idResponses = idGenRepository.getId(bpaRequest.getRequestInfo(), bpa.getTenantId(),
				config.getLetterNoIdgenName(), config.getLetterNoIdgenFormat(), 1).getIdResponses();

		String scnLetterNo = idResponses.get(0).getId();

		StringBuilder uri = new StringBuilder(config.getPdfhost());
		uri.append(config.getPdfContextPath());
		uri.append(config.getPdfSearchEndpoint());

		uri.append("?key=");
		uri.append(BPAConstants.SHOW_CAUSE_SYSTEM_KEY);

		uri.append("&tenantId=");
		uri.append(tenantId);

		LocalDate now = LocalDate.now();

		ObjectMapper objectMapper = new ObjectMapper();
		ObjectNode processInstanceJson = objectMapper.valueToTree(processInstance);
		ObjectNode bpaJson = objectMapper.valueToTree(bpa);
		ObjectNode edcrJson = objectMapper.createObjectNode();
		String msgId = getMsgId(requestInfo);
		log.info("Message ID: " + msgId);
		requestInfo.setMsgId(msgId);
		ObjectNode requestInfoJson = objectMapper.valueToTree(requestInfo);

		edcrJson.putPOJO("planInformation", edcrData);

		bpaJson.put("currentDate", now.toString());
		bpaJson.put("scnLetterNo", scnLetterNo);
		bpaJson.set("edcrDetails", edcrJson);
		bpaJson.set("processInstance", processInstanceJson);

		ObjectNode requestJson = objectMapper.createObjectNode();
		requestJson.set("RequestInfo", requestInfoJson);

		ArrayNode bpaArrayNode = objectMapper.createArrayNode();
		bpaArrayNode.add(bpaJson);
		requestJson.set("Bpa", bpaArrayNode);

		LinkedHashMap fetchResult = null;
		log.info("URI SCN pdf call :" + String.valueOf(uri));
		log.info("Request Json for SCN pdf call :" + String.valueOf(requestJson));
		fetchResult = (LinkedHashMap) serviceRequestRepository.fetchResult(uri, requestJson);
		List<String> filestoreIds = (List<String>) fetchResult.get("filestoreIds");
		String filestoreid = filestoreIds.get(0);

		// craete notice
		HashMap additionalDetails = new HashMap<String, Object>();
		additionalDetails.put("assignee", processInstance.getAssigner().getUuid());
		Notice notice = Notice.builder().businessid(bpa.getApplicationNo()).tenantid(bpa.getTenantId())
				.filestoreid(filestoreid).LetterNo(scnLetterNo).letterType(BPAConstants.SHOWCAUSE_LETTER_TYPE_SCN_IN_WF)
				.isClosed(false).additionalDetails(additionalDetails).build();

		NoticeRequest noticeRequest = NoticeRequest.builder().requestInfo(requestInfo).notice(notice).build();
		noticeservice.createShowCauseNoticeWithInWF(noticeRequest);

	}

	/**
	 * Processes BPA application pull-back from show-cause notice.
	 * 
	 * <p>
	 * This method orchestrates the complete pull-back lifecycle:
	 * <ul>
	 * <li>Validates the pull-back request (authorization, notice status,
	 * timing)</li>
	 * <li>Retrieves the BPA application and workflow state</li>
	 * <li>Determines the appropriate workflow action based on current state</li>
	 * <li>Forwards the application back to the original assignee</li>
	 * <li>Closes the active show-cause notice</li>
	 * </ul>
	 * 
	 * <p>
	 * <strong>Business Logic:</strong>
	 * <ul>
	 * <li><strong>Pull-Back:</strong> Allows assignee to retrieve application after
	 * show-cause notice period, indicating readiness to resume processing</li>
	 * <li>Only the original assignee can pull-back the application</li>
	 * <li>Minimum waiting period must elapse before pull-back is allowed</li>
	 * <li>Workflow action varies based on previous state (ARCHITECT_REWORK vs
	 * SENDBACKTOCITIZEN)</li>
	 * </ul>
	 * 
	 * @param requestInfoWrapper the request wrapper containing user authentication
	 * @param applicationNo      the BPA application number
	 * @param noticeId           the show-cause notice ID to close
	 * @return the closed notice with updated status
	 * @throws CustomException if validation fails or workflow transition fails
	 */
	public Notice processBpaApplicationPullBack(RequestInfoWrapper requestInfoWrapper, String applicationNo,
			String noticeId) {

		// Step 1: Validate pull-back request and retrieve notice
		Notice notice = this.validateBpaApplicationPullBack(requestInfoWrapper, applicationNo, noticeId);

		// Step 2: Extract assignee from notice
		String assigneeKey = extractAssigneeFromNotice(notice);

		// Step 3: Retrieve BPA application
		BPA bpa = retrieveBpaApplication(applicationNo);

		// Step 4: Determine workflow action based on current state
		RequestInfo requestInfo = requestInfoWrapper.getRequestInfo();
		List<ProcessInstance> processInstances = workflowService.getProcessInstances(bpa, requestInfo, false);
		String actionKey = determineWorkflowAction(processInstances);

		// Step 5: Update request info with citizen credentials
		requestInfo = updateCitizenRequestInfo(bpa, processInstances, requestInfo);

		// Step 6: Execute workflow transition
		executeWorkflowPullBack(bpa, actionKey, assigneeKey, requestInfo);

		// Step 7: Close the show-cause notice
		return closeNotice(notice, requestInfo);
	}

	/**
	 * Extracts the assignee UUID from notice additional details.
	 * 
	 * @param notice the notice containing assignee information
	 * @return the assignee UUID
	 * @throws CustomException if assignee is not found
	 */
	private String extractAssigneeFromNotice(Notice notice) {
		ObjectNode additionalDetails = (ObjectNode) notice.getAdditionalDetails();

		if (additionalDetails == null || !additionalDetails.has("assignee")) {
			throw new CustomException("ASSIGNEE_NOT_FOUND",
					"Assignee information not found in notice additional details");
		}

		return additionalDetails.get("assignee").textValue();
	}

	/**
	 * Retrieves the BPA application by application number.
	 * 
	 * @param applicationNo the BPA application number
	 * @return the BPA application
	 * @throws CustomException if application not found
	 */
	private BPA retrieveBpaApplication(String applicationNo) {
		BPASearchCriteria criteria = new BPASearchCriteria();
		criteria.setApplicationNo(applicationNo);

		List<BPA> bpas = bpaRepository.getBPAData(criteria, null);

		if (CollectionUtils.isEmpty(bpas)) {
			throw new CustomException("BPA_NOT_FOUND",
					"No BPA application found for application number: " + applicationNo);
		}

		return bpas.get(0);
	}

	/**
	 * Determines the appropriate workflow action based on previous state.
	 * 
	 * <p>
	 * <strong>Action Logic:</strong>
	 * <ul>
	 * <li><strong>SENDBACK_TO_ARCHITECT_FOR_REWORK:</strong> Use
	 * FORWARD_TO_APPROVER to send back to approval stage</li>
	 * <li><strong>SENDBACKTOCITIZEN:</strong> Use FORWARD to send back to
	 * processing</li>
	 * <li><strong>Default:</strong> Use FORWARD action</li>
	 * </ul>
	 * 
	 * @param processInstances the workflow process instances
	 * @return the workflow action key to use
	 */
	private String determineWorkflowAction(List<ProcessInstance> processInstances) {
		String actionKey = BPAConstants.ACTION_FORWORD; // Default action

		if (!CollectionUtils.isEmpty(processInstances)) {
			ProcessInstance processInstance = processInstances.get(0);

			if (BPAConstants.ACTION_SENDBACK_TO_ARCHITECT_FOR_REWORK.equals(processInstance.getAction())) {
				actionKey = "FORWARD_TO_APPROVER";
			} else if (BPAConstants.ACTION_SENDBACKTOCITIZEN.equals(processInstance.getAction())) {
				actionKey = BPAConstants.ACTION_FORWORD;
			}

			log.info("Determined workflow action: {} based on previous action: {}", actionKey,
					processInstance.getAction());
		}

		return actionKey;
	}

	/**
	 * Executes the workflow pull-back transition.
	 * 
	 * <p>
	 * This method:
	 * <ul>
	 * <li>Creates workflow object with action and assignee</li>
	 * <li>Calls workflow service to transition state</li>
	 * <li>Persists the updated BPA application</li>
	 * </ul>
	 * 
	 * @param bpa         the BPA application
	 * @param actionKey   the workflow action to execute
	 * @param assigneeKey the assignee to forward to
	 * @param requestInfo the request information
	 */
	private void executeWorkflowPullBack(BPA bpa, String actionKey, String assigneeKey, RequestInfo requestInfo) {

		// Build workflow object
		Workflow workflow = new Workflow();
		workflow.setAction(actionKey);
		workflow.setAssignes(Arrays.asList(assigneeKey));
		workflow.setComments("Application pulled back from show-cause notice");

		bpa.setWorkflow(workflow);

		// Build request and execute workflow
		BPARequest bpaRequest = new BPARequest();
		bpaRequest.setRequestInfo(requestInfo);
		bpaRequest.setBPA(bpa);

		workflowIntegrator.callWorkFlow(bpaRequest);
		bpaRepository.update(bpaRequest, true);

		log.info("Successfully executed pull-back workflow for application: {}", bpa.getApplicationNo());
	}

	/**
	 * Closes the show-cause notice after successful pull-back.
	 * 
	 * @param notice      the notice to close
	 * @param requestInfo the request information
	 * @return the closed notice
	 */
	private Notice closeNotice(Notice notice, RequestInfo requestInfo) {
		notice.setClosed(true);
		return noticeservice.showCauseReply(NoticeRequest.builder().notice(notice).requestInfo(requestInfo).build());
	}

	private RequestInfo updateCitizenRequestInfo(BPA bpa, List<ProcessInstance> processInstances,
			RequestInfo requestInfo) {
		if (processInstances != null && processInstances.get(0) != null) {
			ProcessInstance processInstance = processInstances.get(0);
			String assigneeuuid = bpaRepository.getAssigneeByprocessInstanceId(processInstance.getId());
			if (assigneeuuid == null)
				assigneeuuid = bpa.getAuditDetails().getCreatedBy();
			UserDetailResponse userDetailResponse = null;
			userDetailResponse = userService.getUserByUUID(assigneeuuid, requestInfo);
			requestInfo.setUserInfo(BPAAutoEscalationService.copyUser(userDetailResponse.getUser().get(0)));
		}
		return requestInfo;
	}

	/**
	 * Validates a BPA application pull-back request before processing.
	 * 
	 * <p>
	 * This method performs comprehensive validation including:
	 * <ul>
	 * <li>Input parameter validation (non-null checks)</li>
	 * <li>Notice existence verification</li>
	 * <li>Notice status validation (must be active/open)</li>
	 * <li>Authorization verification (only original assignee can pull-back)</li>
	 * <li>Minimum waiting period validation</li>
	 * </ul>
	 * 
	 * <p>
	 * <strong>Business Rules:</strong>
	 * <ul>
	 * <li>Only the original assignee who generated the notice can pull-back</li>
	 * <li>Minimum revoke period must elapse before pull-back is allowed</li>
	 * <li>Notice must be active (not already closed)</li>
	 * <li>Notice must be of type SCN_IN_WF (show-cause in workflow)</li>
	 * </ul>
	 * 
	 * @param requestInfoWrapper the request wrapper containing user authentication
	 * @param applicationNo      the BPA application number
	 * @param noticeId           the show-cause notice ID
	 * @return the validated notice if all checks pass
	 * @throws IllegalArgumentException if input parameters are null
	 * @throws CustomException          if any validation fails
	 */
	public Notice validateBpaApplicationPullBack(RequestInfoWrapper requestInfoWrapper, String applicationNo,
			String noticeId) {

		// Step 1: Validate input parameters
		validateInputParameters(applicationNo, noticeId);

		// Step 2: Retrieve and validate notice exists
		Notice notice = retrieveAndValidateNoticeExists(applicationNo, noticeId);

		// Step 3: Validate notice is not already closed
		validateNoticeIsOpen(notice, applicationNo, noticeId);

		// Step 4: Validate user authorization (assignee check)
		validateUserAuthorization(notice, requestInfoWrapper, applicationNo, noticeId);

		// Step 5: Validate minimum waiting period has elapsed
		validateWaitingPeriod(notice, applicationNo, noticeId);

		return notice;
	}

	/**
	 * Validates that required input parameters are not null.
	 * 
	 * @param applicationNo the BPA application number
	 * @param noticeId      the show-cause notice ID
	 * @throws IllegalArgumentException if either parameter is null
	 */
	private void validateInputParameters(String applicationNo, String noticeId) {
		if (applicationNo == null || noticeId == null) {
			throw new IllegalArgumentException("Invalid input parameters, applicationNo and noticeId cannot be null.");
		}
	}

	/**
	 * Retrieves the notice and validates it exists.
	 * 
	 * <p>
	 * This method searches for the notice using:
	 * <ul>
	 * <li>Letter type: SCN_IN_WF (show-cause in workflow)</li>
	 * <li>Business ID: application number</li>
	 * <li>Notice ID: specific notice identifier</li>
	 * </ul>
	 * 
	 * @param applicationNo the BPA application number
	 * @param noticeId      the show-cause notice ID
	 * @return the found notice
	 * @throws CustomException if notice is not found
	 */
	private Notice retrieveAndValidateNoticeExists(String applicationNo, String noticeId) {
		List<String> ids = new LinkedList<>();
		ids.add(noticeId);

		NoticeSearchCriteria noticeSearchCriteria = NoticeSearchCriteria.builder()
				.letterType(BPAConstants.SHOWCAUSE_LETTER_TYPE_SCN_IN_WF).businessid(applicationNo).ids(ids).build();

		List<Notice> notices = noticeservice.searchNotice(noticeSearchCriteria);

		if (notices.isEmpty()) {
			throw new CustomException("NOTICE_NOT_FOUND",
					String.format("No notice found for applicationNo: %s and noticeId: %s in letterType: %s",
							applicationNo, noticeId, BPAConstants.SHOWCAUSE_LETTER_TYPE_SCN_IN_WF));
		}

		return notices.get(0);
	}

	/**
	 * Validates that the notice is still open (not closed).
	 * 
	 * <p>
	 * <strong>Business Rule:</strong> Only open notices can be pulled back. A
	 * closed notice indicates the matter has been resolved or expired.
	 * 
	 * @param notice        the notice to validate
	 * @param applicationNo the application number (for error message)
	 * @param noticeId      the notice ID (for error message)
	 * @throws CustomException if notice is already closed
	 */
	private void validateNoticeIsOpen(Notice notice, String applicationNo, String noticeId) {
		if (notice.isClosed()) {
			throw new CustomException("NOTICE_ALREADY_CLOSED", String.format(
					"Notice is already closed for applicationNo: %s and noticeId: %s", applicationNo, noticeId));
		}
	}

	/**
	 * Validates that the requesting user is authorized to pull-back the
	 * application.
	 * 
	 * <p>
	 * <strong>Authorization Logic:</strong>
	 * <ul>
	 * <li>Extracts the assignee UUID from notice additional details</li>
	 * <li>Compares with the current user's UUID</li>
	 * <li>Only the original assignee can perform pull-back</li>
	 * </ul>
	 * 
	 * <p>
	 * <strong>Business Rule:</strong> Pull-back authorization is limited to the
	 * official who originally generated the show-cause notice to prevent
	 * unauthorized interference with pending matters.
	 * 
	 * @param notice             the notice containing assignee information
	 * @param requestInfoWrapper the request wrapper with current user info
	 * @param applicationNo      the application number (for error message)
	 * @param noticeId           the notice ID (for error message)
	 * @throws CustomException if assignee not found or user is not authorized
	 */
	private void validateUserAuthorization(Notice notice, RequestInfoWrapper requestInfoWrapper, String applicationNo,
			String noticeId) {

		// Extract assignee from notice additional details
		String assigneeValue = null;
		ObjectNode additionalDetails = (ObjectNode) notice.getAdditionalDetails();

		if (additionalDetails != null && additionalDetails.has("assignee")) {
			assigneeValue = additionalDetails.get("assignee").textValue();
		}

		if (assigneeValue == null) {
			throw new CustomException("ASSIGNEE_NOT_FOUND", String.format(
					"Assignee not found in notice for applicationNo: %s and noticeId: %s", applicationNo, noticeId));
		}

		// Verify current user is the assignee
		String currentUserUuid = requestInfoWrapper.getRequestInfo().getUserInfo().getUuid();
		if (!currentUserUuid.equals(assigneeValue)) {
			throw new CustomException("UNAUTHORIZED_USER",
					String.format("You are not authorized to pull-back notice for applicationNo: %s and noticeId: %s. "
							+ "Only the original assignee can perform this action.", applicationNo, noticeId));
		}
	}

	/**
	 * Validates that the minimum waiting period has elapsed since notice creation.
	 * 
	 * <p>
	 * <strong>Business Rule:</strong> A minimum revoke period (configured in days)
	 * must elapse after notice creation before the assignee can pull-back the
	 * application. This ensures the citizen has adequate time to respond before
	 * officials can reclaim the application.
	 * 
	 * @param notice        the notice to check timing on
	 * @param applicationNo the application number (for error message)
	 * @param noticeId      the notice ID (for error message)
	 * @throws CustomException if minimum period has not elapsed
	 */
	private void validateWaitingPeriod(Notice notice, String applicationNo, String noticeId) {
		Long currentTimeMillis = System.currentTimeMillis();
		Long createdTime = notice.getAuditDetails().getCreatedTime();
		Long elapsedDays = TimeUnit.MILLISECONDS.toDays(currentTimeMillis - createdTime);

		Long requiredPeriod = Long.valueOf(config.getBpaNoticeRevokePeriod());

		if (elapsedDays < requiredPeriod) {
			throw new CustomException("WAITING_PERIOD_NOT_ELAPSED",
					String.format(
							"Pull-back not allowed yet. %d days must elapse since notice creation. "
									+ "Notice created %d days ago for applicationNo: %s and noticeId: %s",
							requiredPeriod, elapsedDays, applicationNo, noticeId));
		}

		log.info("Waiting period validation passed: {} days elapsed (required: {}) for application: {}", elapsedDays,
				requiredPeriod, applicationNo);
	}

	private void closeActiveNotice(String applicationNo, String tenantid, RequestInfo requestInfo) {
		List<Notice> notices = getActiveSncINWFNotice(applicationNo, tenantid);
		for (Notice notice : notices) {
			notice.setClosed(true);
			noticeservice.showCauseReply(NoticeRequest.builder().notice(notice).requestInfo(requestInfo).build());
		}
	}

	private List getEdcrDataForPAP(BPA bpa) {
		String village = null;
		if (bpa.getLandInfo() != null && bpa.getLandInfo().getAddress() != null
				&& bpa.getLandInfo().getAddress().getLocality() != null)
			village = bpa.getLandInfo().getAddress().getLocality().getName();
		else
			village = "NA";
		DocumentContext context = JsonPath.parse(bpa.getAdditionalDetails());
		ObjectMapper objectMapper = new ObjectMapper();
		ObjectNode edcr = objectMapper.createObjectNode();

		edcr.put("khataNo", getValueOrDefault(context, "$.planDetail.planInformation.khataNo", "NA"));
		edcr.put("plotNo", getValueOrDefault(context, "$.planDetail.plot.plotNo", "NA"));
		edcr.put("mauza", getValueOrDefault(context, "$.landDetails.revenueVillage", village));

		return Arrays.asList(edcr);
	}

	private String getValueOrDefault(DocumentContext context, String path, String defaultValue) {
		try {
			return context.read(path);
		} catch (PathNotFoundException e) {
			return defaultValue;
		}
	}

	/**
	 * Creates a refusal show-cause notice for a BPA application.
	 * 
	 * <p>
	 * This method orchestrates the complete refusal notice generation lifecycle:
	 * <ul>
	 * <li>Retrieves and validates the BPA application</li>
	 * <li>Validates that no refusal notice already exists</li>
	 * <li>Enriches BPA with land information and EDCR details</li>
	 * <li>Processes and enriches refusal reasons</li>
	 * <li>Generates PDF document via PDF service</li>
	 * <li>Creates and persists the notice record</li>
	 * </ul>
	 * 
	 * <p>
	 * <strong>Business Logic:</strong>
	 * <ul>
	 * <li><strong>Refusal Notice:</strong> Formal notice issued when a BPA
	 * application is rejected, explaining the reasons for refusal</li>
	 * <li>Only one refusal notice can exist per application</li>
	 * <li>Refusal reasons are localized for citizen understanding</li>
	 * <li>Notice includes application details, plot info, and service type</li>
	 * </ul>
	 * 
	 * @param notice      the notice template containing refusal reasons
	 * @param requestInfo the request information for authentication
	 * @return the created notice request with generated PDF
	 * @throws CustomException if BPA not found or refusal notice already exists
	 */
	public NoticeRequest createRefusalShowCauseNotice(Notice notice, RequestInfo requestInfo) {

		// Step 1: Retrieve and validate BPA application
		BPA bpa = retrieveAndValidateBpaForRefusal(notice);

		// Step 2: Validate no refusal notice exists
		validateRefusalShowCauseNoticeCreate(bpa);

		// Step 3: Enrich BPA with land information
		enrichBpaWithLandInfo(bpa, requestInfo);

		// Step 4: Retrieve and enrich workflow information
		ProcessInstance processInstance = enrichWorkflowInformation(bpa, requestInfo);

		// Step 5: Extract and enrich refusal reasons
		List<String> refusalReasons = getRefusalReasons(notice);
		BPARequest bpaRequest = new BPARequest();
		bpaRequest.setRequestInfo(requestInfo);
		bpaRequest.setBPA(bpa);
		String enrichedReasons = enrichRefusalReasons(bpaRequest, refusalReasons);
		log.info("Reasons after enrichment: {}", enrichedReasons);

		// Step 6: Extract plot and service type information
		PlotServiceInfo plotServiceInfo = extractPlotAndServiceInfo(bpa);

		// Step 7: Generate letter number via IdGen
		String letterNumber = generateRefusalLetterNumber(bpa, requestInfo);

		// Step 8: Generate PDF document
		String filestoreId = generateRefusalNoticePdf(bpa, processInstance, enrichedReasons, letterNumber,
				plotServiceInfo, requestInfo);

		// Step 9: Build and persist notice
		Notice refusalNotice = buildRefusalNotice(bpa, notice, letterNumber, filestoreId, processInstance);

		// Step 10: Create and return notice request
		NoticeRequest noticeRequest = NoticeRequest.builder().requestInfo(requestInfo).notice(refusalNotice).build();

		noticeservice.createShowCauseNoticeWithInWF(noticeRequest);

		return noticeRequest;
	}

	/**
	 * Retrieves and validates BPA application for refusal notice generation.
	 * 
	 * @param notice the notice containing application number
	 * @return the BPA application
	 * @throws CustomException if BPA not found
	 */
	private BPA retrieveAndValidateBpaForRefusal(Notice notice) {
		List<String> edcrNos = new ArrayList<>();
		BPASearchCriteria criteria = BPASearchCriteria.builder().applicationNo(notice.getBusinessid())
				.tenantId(notice.getTenantid()).build();

		List<BPA> bpas = bpaRepository.getBPAData(criteria, edcrNos);

		if (CollectionUtils.isEmpty(bpas)) {
			throw new CustomException("NO_BPA_EXIST",
					"No BPA Application exists for application number: " + notice.getBusinessid());
		}

		return bpas.get(0);
	}

	/**
	 * Enriches BPA application with land information from land service.
	 * 
	 * @param bpa         the BPA application to enrich
	 * @param requestInfo the request information
	 */
	private void enrichBpaWithLandInfo(BPA bpa, RequestInfo requestInfo) {
		LandSearchCriteria landSearchCriteria = new LandSearchCriteria();
		landSearchCriteria.setIds(Arrays.asList(bpa.getLandId()));
		landSearchCriteria.setTenantId(bpa.getTenantId());

		ArrayList<LandInfo> landInfoList = landService.searchLandInfoToBPA(requestInfo, landSearchCriteria);

		if (landInfoList != null && !landInfoList.isEmpty()) {
			LandInfo landData = mapper.convertValue(landInfoList.get(0), LandInfo.class);
			bpa.setLandInfo(landData);
		}
	}

	/**
	 * Enriches workflow process instance information.
	 * 
	 * <p>
	 * Updates the assigner's name with the current action taker for display
	 * purposes.
	 * 
	 * @param bpa         the BPA application
	 * @param requestInfo the request information
	 * @return the enriched process instance
	 */
	private ProcessInstance enrichWorkflowInformation(BPA bpa, RequestInfo requestInfo) {
		List<ProcessInstance> processInstances = workflowService.getProcessInstances(bpa, requestInfo, false);

		if (!processInstances.isEmpty()) {
			ProcessInstance processInstance = processInstances.get(0);
			String actionTakenBy = requestInfo.getUserInfo().getName();
			log.info("Action taken by user: {}", actionTakenBy);
			processInstance.getAssigner().setName(actionTakenBy);
			return processInstance;
		}

		return new ProcessInstance();
	}

	/**
	 * Inner class to hold plot number and service type information.
	 */
	private static class PlotServiceInfo {
		private final String plotNumber;
		private final String serviceType;

		public PlotServiceInfo(String plotNumber, String serviceType) {
			this.plotNumber = plotNumber;
			this.serviceType = serviceType;
		}

		public String getPlotNumber() {
			return plotNumber;
		}

		public String getServiceType() {
			return serviceType;
		}
	}

	/**
	 * Extracts plot number and service type from BPA application.
	 * 
	 * <p>
	 * Handles different application types:
	 * <ul>
	 * <li>Pre-Approved Plan (PAP)</li>
	 * <li>Building Plan Scrutiny</li>
	 * <li>Occupancy Certificate</li>
	 * <li>Revalidation</li>
	 * <li>Alteration</li>
	 * </ul>
	 * 
	 * @param bpa the BPA application
	 * @return PlotServiceInfo containing plot number and service type
	 */
	private PlotServiceInfo extractPlotAndServiceInfo(BPA bpa) {
		String plotNumber = "NA";
		String serviceType = "NA";

		if (BPAConstants.BPA_PAP_MODULE_CODE.equals(bpa.getBusinessService())) {
			// Pre-Approved Plan logic
			DocumentContext context = JsonPath.parse(bpa.getAdditionalDetails());
			plotNumber = getValueOrDefault(context, "$.planDetail.plot.plotNo", "NA");
			serviceType = "Pre Approved Plan";
		} else {
			// Regular BPA application logic
			PlotServiceInfo info = extractFromRegularBpa(bpa);
			plotNumber = info.getPlotNumber();
			serviceType = info.getServiceType();
		}

		return new PlotServiceInfo(plotNumber, serviceType);
	}

	/**
	 * Extracts plot and service information from regular BPA applications.
	 * 
	 * @param bpa the BPA application
	 * @return PlotServiceInfo with extracted details
	 */
	private PlotServiceInfo extractFromRegularBpa(BPA bpa) {
		String plotNumber = "NA";
		String serviceType = "NA";

		Map<String, Object> additionalDetails = (Map<String, Object>) bpa.getAdditionalDetails();

		if (!CollectionUtils.isEmpty(additionalDetails)) {
			List<Object> plotList = (List<Object>) additionalDetails.get("plot");
			Map<String, Object> plot = (Map<String, Object>) plotList.get(0);
			String applicationType = (String) additionalDetails.get("applicationType");

			plotNumber = (String) plot.get("plotNumber");

			if (bpa.isRevalidationApplication()) {
				serviceType = "REVALIDATION";
			} else if ("BUILDING_OC_PLAN_SCRUTINY".equalsIgnoreCase(applicationType)) {
				serviceType = "OCCUPANCY CERTIFICATE";
				plotNumber = (String) plot.get("plotNo");
				if (StringUtils.isEmpty(plotNumber)) {
					plotNumber = (String) plot.get("plotNumber");
				}
			} else {
				Object alterationService = additionalDetails.get("alterationService");
				if (!ObjectUtils.isEmpty(alterationService)) {
					serviceType = "ALTERATION";
				} else {
					serviceType = (String) additionalDetails.get("serviceType");
					if (serviceType != null) {
						serviceType = serviceType.replace("_", " ");
					}
				}
			}
		}

		return new PlotServiceInfo(plotNumber, serviceType);
	}

	/**
	 * Generates refusal letter number via IdGen service.
	 * 
	 * @param bpa         the BPA application
	 * @param requestInfo the request information
	 * @return the generated letter number
	 */
	private String generateRefusalLetterNumber(BPA bpa, RequestInfo requestInfo) {
		List<IdResponse> idResponses = idGenRepository.getId(requestInfo, bpa.getTenantId(),
				config.getLetterNoIdgenName(), config.getRefusalLetterNoIdgenFormat(), 1).getIdResponses();

		return idResponses.get(0).getId();
	}

	/**
	 * Generates refusal notice PDF document via PDF service.
	 * 
	 * @param bpa             the BPA application
	 * @param processInstance the workflow process instance
	 * @param refusalReason   the enriched refusal reasons
	 * @param letterNumber    the generated letter number
	 * @param plotServiceInfo the plot and service type information
	 * @param requestInfo     the request information
	 * @return the filestore ID of the generated PDF
	 */
	private String generateRefusalNoticePdf(BPA bpa, ProcessInstance processInstance, String refusalReason,
			String letterNumber, PlotServiceInfo plotServiceInfo, RequestInfo requestInfo) {

		// Build PDF service URL
		StringBuilder uri = new StringBuilder(config.getPdfhost());
		uri.append(config.getPdfContextPath());
		uri.append(config.getPdfSearchEndpoint());
		uri.append("?key=").append(BPAConstants.REFUSAL_SHOW_CAUSE_NOTICE_KEY);
		uri.append("&tenantId=").append(bpa.getTenantId());

		// Build request JSON
		ObjectMapper objectMapper = new ObjectMapper();
		ObjectNode requestJson = buildPdfRequestJson(bpa, processInstance, refusalReason, letterNumber, plotServiceInfo,
				requestInfo, objectMapper);

		// Call PDF service
		log.info("URI Refusal SCN pdf call: {}", uri);
		log.info("Request Json for Refusal SCN pdf call: {}", requestJson);

		LinkedHashMap fetchResult = (LinkedHashMap) serviceRequestRepository.fetchResult(uri, requestJson);
		List<String> filestoreIds = (List<String>) fetchResult.get("filestoreIds");

		return filestoreIds.get(0);
	}

	/**
	 * Builds the JSON request for PDF service.
	 * 
	 * @param bpa             the BPA application
	 * @param processInstance the process instance
	 * @param refusalReason   the refusal reasons
	 * @param letterNumber    the letter number
	 * @param plotServiceInfo the plot and service info
	 * @param requestInfo     the request information
	 * @param objectMapper    the JSON object mapper
	 * @return the built JSON request
	 */
	private ObjectNode buildPdfRequestJson(BPA bpa, ProcessInstance processInstance, String refusalReason,
			String letterNumber, PlotServiceInfo plotServiceInfo, RequestInfo requestInfo, ObjectMapper objectMapper) {

		LocalDate now = LocalDate.now();

		// Build BPA JSON node
		ObjectNode bpaJson = objectMapper.valueToTree(bpa);
		bpaJson.put("currentDate", now.toString());
		bpaJson.set("scnLetterNo", objectMapper.valueToTree(letterNumber));
		bpaJson.set("edcrDetails", objectMapper.createObjectNode());
		bpaJson.set("processInstance", objectMapper.valueToTree(processInstance));
		bpaJson.set("refusalReason", objectMapper.valueToTree(refusalReason));
		bpaJson.set("plotNumber", objectMapper.valueToTree(plotServiceInfo.getPlotNumber()));
		bpaJson.set("serviceType", objectMapper.valueToTree(plotServiceInfo.getServiceType()));

		// Build request JSON
		ObjectNode requestJson = objectMapper.createObjectNode();
		requestJson.set("RequestInfo", objectMapper.valueToTree(requestInfo));

		ArrayNode bpaArrayNode = objectMapper.createArrayNode();
		bpaArrayNode.add(bpaJson);
		requestJson.set("Bpa", bpaArrayNode);

		return requestJson;
	}

	/**
	 * Builds the refusal notice object with all required information.
	 * 
	 * @param bpa             the BPA application
	 * @param originalNotice  the original notice template
	 * @param letterNumber    the generated letter number
	 * @param filestoreId     the PDF filestore ID
	 * @param processInstance the process instance
	 * @return the built notice object
	 */
	private Notice buildRefusalNotice(BPA bpa, Notice originalNotice, String letterNumber, String filestoreId,
			ProcessInstance processInstance) {

		// Get additional details from original notice and add assignee
		HashMap<String, Object> additionalDetails = (HashMap<String, Object>) originalNotice.getAdditionalDetails();
		if (!ObjectUtils.isEmpty(processInstance) && processInstance.getAssigner() != null) {
			additionalDetails.put("assignee", processInstance.getAssigner().getUuid());
		}

		return Notice.builder().businessid(bpa.getApplicationNo()).tenantid(bpa.getTenantId()).filestoreid(filestoreId)
				.LetterNo(letterNumber).letterType(BPAConstants.REFUSAL_SHOWCAUSE_LETTER_TYPE).isClosed(false)
				.additionalDetails(additionalDetails).build();
	}

	/**
	 * @param notice
	 */
	private List<String> getRefusalReasons(Notice notice) {
		log.info("Preparing SCN reasons payload");
		HashMap<String, Object> additionalDetail = (HashMap<String, Object>) notice.getAdditionalDetails();
		List<Map<String, String>> refusalScnReasonsList = new ArrayList<>();
		List<String> refusalScnReasons = new ArrayList<>();
		if (!CollectionUtils.isEmpty(additionalDetail)) {
			Map<String, Object> refusalScnReasonsMap = (Map<String, Object>) additionalDetail
					.get("refusalShowCauseNoticeReason");

			if (refusalScnReasonsMap != null) {

				for (Map.Entry<String, Object> entry : refusalScnReasonsMap.entrySet()) {
					Map<String, String> reasonEntry = new HashMap<>();
					reasonEntry.put(entry.getKey(), entry.getValue().toString());
					refusalScnReasonsList.add(reasonEntry);
				}
			}

			if (!CollectionUtils.isEmpty(refusalScnReasonsList)) {
				refusalScnReasons = refusalScnReasonsList.stream().flatMap(map -> {
					// Collect keys where value is "true"
					List<String> trueKeys = map.entrySet().stream()
							.filter(entry -> "true".equalsIgnoreCase(entry.getValue())
									&& !"MANUAL_OTHER_REASON".equals(entry.getKey()))
							.map(Map.Entry::getKey).collect(Collectors.toList());

					// Add the value of "MANUAL_OTHER_REASON"
					Optional<String> manualReason = Optional.ofNullable(map.get("MANUAL_OTHER_REASON"));
					manualReason.ifPresent(trueKeys::add);

					return trueKeys.stream();
				}).collect(Collectors.toList());
			}
		}
		log.info("refusalScnReasons" + refusalScnReasons);
		return refusalScnReasons;
	}

	private String enrichRefusalReasons(BPARequest bpaRequest, List<String> reasons) {
		log.info("Reasons Before Enrich" + reasons);
		String localizationMessages = notificationUtil.getLocalizationMessages(bpaRequest.getBPA().getTenantId(),
				bpaRequest.getRequestInfo());
		List<String> enrichedReasons = new ArrayList();

//		List<String> enrichedReasons = reasons.stream()
//				.map(reason -> notificationUtil.getMessageTemplate(reason, localizationMessages))
//				.filter(StringUtils::isNotEmpty).collect(Collectors.toList());

		reasons.stream().map(reason -> {
			String message = notificationUtil.getMessageTemplate(reason, localizationMessages);
			return StringUtils.isEmpty(message) ? reason : message;
		}).forEach(enrichedReasons::add);
		return enrichedReasons.stream().collect(Collectors.joining(","));
	}

	/**
	 * @param bpa
	 */
	private void validateRefusalShowCauseNoticeCreate(BPA bpa) {
		String applicationNo = bpa.getApplicationNo();
		NoticeSearchCriteria noticeSearchCriteria = NoticeSearchCriteria.builder().businessid(applicationNo)
				.letterType(BPAConstants.REFUSAL_SHOWCAUSE_LETTER_TYPE).build();

		List<Notice> notices = scnRepository.getNoticeData(noticeSearchCriteria);

		if (!CollectionUtils.isEmpty(notices)) {
			String letterNo = notices.get(0).getLetterNo();
			throw new CustomException("NOTICE_EXIST_ERROR",
					String.format("Refusal SCN already exists for business ID: %s. Letter Number: %s", applicationNo,
							StringUtils.defaultString(letterNo, "N/A")));
		}
	}

	// Method to get message ID with validation
	public String getMsgId(RequestInfo requestInfo) {
		if (requestInfo == null || StringUtils.isEmpty(requestInfo.getMsgId())
				|| !isValidMsgIdFormat(requestInfo.getMsgId())) {
			return DEFAULT_MSG_ID;
		}
		return requestInfo.getMsgId();
	}

	// Helper method to validate the format of msgId
	private boolean isValidMsgIdFormat(String msgId) {
		// Validate that msgId contains the required pipe delimiter and follows the
		// expected format
		return msgId.contains("|");
	}

}
