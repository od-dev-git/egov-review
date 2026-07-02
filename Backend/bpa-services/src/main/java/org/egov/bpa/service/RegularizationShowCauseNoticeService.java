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
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.validation.Valid;

import org.egov.bpa.config.BPAConfiguration;
import org.egov.bpa.repository.IdGenRepository;
import org.egov.bpa.repository.RegularizationRepository;
import org.egov.bpa.repository.ScnRepository;
import org.egov.bpa.repository.ServiceRequestRepository;
import org.egov.bpa.util.BPAConstants;
import org.egov.bpa.util.NotificationUtil;
import org.egov.bpa.util.RegularizationConstants;
import org.egov.bpa.web.model.Notice;
import org.egov.bpa.web.model.NoticeRequest;
import org.egov.bpa.web.model.NoticeSearchCriteria;
import org.egov.bpa.web.model.RequestInfoWrapper;
import org.egov.bpa.web.model.Workflow;
import org.egov.bpa.web.model.idgen.IdResponse;
import org.egov.bpa.web.model.landInfo.OwnerInfo;
import org.egov.bpa.web.model.regularization.Regularization;
import org.egov.bpa.web.model.regularization.RegularizationRequest;
import org.egov.bpa.web.model.regularization.RegularizationSearchCriteria;
import org.egov.bpa.web.model.user.UserDetailResponse;
import org.egov.bpa.web.model.workflow.BusinessService;
import org.egov.bpa.web.model.workflow.ProcessInstance;
import org.egov.common.contract.request.RequestInfo;
import org.egov.tracer.model.CustomException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jayway.jsonpath.JsonPath;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class RegularizationShowCauseNoticeService {

	@Autowired
	private BPAConfiguration config;

	@Autowired
	private RegularizationNoticeService noticeService;

	@Autowired
	private IdGenRepository idGenRepository;

	@Autowired
	private ServiceRequestRepository serviceRequestRepository;

	@Autowired
	private RegularizationWorkflow workflowIntegrator;

	@Autowired
	private RegularizationRepository regularizationRepository;

	@Autowired
	private UserService userService;

	@Autowired
	private NotificationUtil util;

	@Autowired
	private ScnRepository scnRepository;

	@Autowired
	private NotificationUtil notificationUtil;

	@Autowired
	private RegularizationUserService regularizationUserService;

	private static final String DEFAULT_MSG_ID = "20170310130900|en_IN";

	/**
	 * Process show cause for Regularizations if enabled in config
	 * 
	 * @param showCauseRegularizations
	 * @param businessServices
	 * @param requestInfo
	 */
	public void processShowCauseNoticeWithInWorkflow(Map<Regularization, ProcessInstance> showCauseRegularizations,
			Map<String, BusinessService> businessServices, RequestInfo requestInfo) {

		if (config.isBpaShowcauseNoticeEnabled()) {
			for (Map.Entry<Regularization, ProcessInstance> entry : showCauseRegularizations.entrySet()) {
				Regularization regularization = entry.getKey();
				ProcessInstance processInstance = entry.getValue();
				try {
					log.info("Processing for ShowCause Notice for regularization " + regularization.getApplicationNo());
					processShowCauseNotice(regularization, processInstance,
							businessServices.get(regularization.getBusinessService()), requestInfo);
					log.info("Process completed for ShowCause Notice for regularization "
							+ regularization.getApplicationNo());
				} catch (Exception e) {
					log.info("Failed ShowCause Notice for regularization " + regularization.getApplicationNo()
							+ " with error message - " + e.getMessage());
				}
			}
		}
	}

	/**
	 * Process show cause for a single application here
	 * 
	 * @param regularization
	 * @param processInstance
	 * @param businessService
	 * @param requestInfo
	 */
	private void processShowCauseNotice(Regularization regularization, ProcessInstance processInstance,
			BusinessService businessService, RequestInfo requestInfo) {
		List<Notice> notices = getActiveSncINWFNotice(regularization.getApplicationNo(), regularization.getTenantId());

		if (notices != null && notices.size() == 0) {
			try {
				log.info("Started processing for notice genration for Regularization");
				requestInfo.setUserInfo(processInstance.getAssigner());
				createShowCauseNotice(regularization, processInstance, requestInfo);
				log.info("Notice genration completed for Regularization");
			} catch (Exception e) {
				log.error("Error while Notice genration for Regularization ");
				throw e;
			}
		}

	}

	/**
	 * Creates a Show Cause Notice for a regularization application by generating a
	 * PDF document.
	 * 
	 * <p>
	 * This method orchestrates the show cause notice creation process:
	 * <ol>
	 * <li>Enriches regularization with plot and khata numbers for PDF
	 * generation</li>
	 * <li>Generates a unique letter number for the notice</li>
	 * <li>Builds and sends PDF generation request to PDF service</li>
	 * <li>Creates and persists the notice record with generated PDF filestore
	 * ID</li>
	 * </ol>
	 * </p>
	 * 
	 * @param regularization  The regularization application for which notice is
	 *                        being created
	 * @param processInstance The current workflow process instance
	 * @param requestInfo     The request info containing user context
	 */
	private void createShowCauseNotice(Regularization regularization, ProcessInstance processInstance,
			RequestInfo requestInfo) {

		// Step 1: Prepare regularization request
		RegularizationRequest regularizationRequest = buildRegularizationRequest(regularization, requestInfo);

		// Step 2: Enrich regularization with plot and khata numbers for PDF
		setPlotAndKhataNumbersinRegularization(regularization, requestInfo);

		// Step 3: Generate unique letter number for the notice
		String scnLetterNo = generateShowCauseLetterNumber(regularizationRequest);

		// Step 4: Generate PDF and get filestore ID
		String filestoreId = generateShowCausePdf(regularization, processInstance, requestInfo, scnLetterNo);

		// Step 5: Create and persist the notice record
		createAndPersistNotice(regularization, processInstance, requestInfo, scnLetterNo, filestoreId);
	}

	/**
	 * Builds a RegularizationRequest object from the regularization and request
	 * info.
	 * 
	 * @param regularization The regularization entity
	 * @param requestInfo    The request info
	 * @return The constructed RegularizationRequest
	 */
	private RegularizationRequest buildRegularizationRequest(Regularization regularization, RequestInfo requestInfo) {

		RegularizationRequest regularizationRequest = new RegularizationRequest();
		regularizationRequest.setRequestInfo(requestInfo);
		regularizationRequest.setRegularization(regularization);
		return regularizationRequest;
	}

	/**
	 * Generates a unique letter number for the show cause notice using ID
	 * generation service.
	 * 
	 * @param regularizationRequest The regularization request containing tenant
	 *                              info
	 * @return The generated letter number
	 */
	private String generateShowCauseLetterNumber(RegularizationRequest regularizationRequest) {

		Regularization regularization = regularizationRequest.getRegularization();

		List<IdResponse> idResponses = idGenRepository
				.getId(regularizationRequest.getRequestInfo(), regularization.getTenantId(),
						config.getRegularizationNoticeName(), config.getRegularizationNoticeFormat(), 1)
				.getIdResponses();

		return idResponses.get(0).getId();
	}

	/**
	 * Generates the Show Cause Notice PDF by calling the PDF service.
	 * 
	 * <p>
	 * Builds the PDF request JSON with regularization details, process instance,
	 * current date, and letter number, then calls the PDF service endpoint.
	 * </p>
	 * 
	 * @param regularization  The regularization entity
	 * @param processInstance The workflow process instance
	 * @param requestInfo     The request info
	 * @param scnLetterNo     The generated letter number
	 * @return The filestore ID of the generated PDF
	 */
	@SuppressWarnings("unchecked")
	private String generateShowCausePdf(Regularization regularization, ProcessInstance processInstance,
			RequestInfo requestInfo, String scnLetterNo) {

		String tenantId = regularization.getTenantId();

		// Build PDF service URI
		StringBuilder uri = buildPdfServiceUri(tenantId, RegularizationConstants.SHOW_CAUSE_KEY);

		// Build PDF request JSON
		ObjectNode requestJson = buildShowCausePdfRequest(regularization, processInstance, requestInfo, scnLetterNo);

		// Call PDF service and get filestore ID
		LinkedHashMap<String, Object> fetchResult = (LinkedHashMap<String, Object>) serviceRequestRepository
				.fetchResult(uri, requestJson);
		List<String> filestoreIds = (List<String>) fetchResult.get("filestoreIds");

		return filestoreIds.get(0);
	}

	/**
	 * Builds the PDF service URI with the specified key and tenant ID.
	 * 
	 * @param tenantId The tenant ID
	 * @param pdfKey   The PDF template key
	 * @return The constructed URI
	 */
	private StringBuilder buildPdfServiceUri(String tenantId, String pdfKey) {

		StringBuilder uri = new StringBuilder(config.getPdfhost());
		uri.append(config.getPdfContextPath());
		uri.append(config.getPdfSearchEndpoint());
		uri.append("?key=").append(pdfKey);
		uri.append("&tenantId=").append(tenantId);

		return uri;
	}

	/**
	 * Builds the JSON request payload for PDF generation.
	 * 
	 * @param regularization  The regularization entity
	 * @param processInstance The workflow process instance
	 * @param requestInfo     The request info
	 * @param scnLetterNo     The letter number
	 * @return The constructed JSON request
	 */
	private ObjectNode buildShowCausePdfRequest(Regularization regularization, ProcessInstance processInstance,
			RequestInfo requestInfo, String scnLetterNo) {

		ObjectMapper objectMapper = new ObjectMapper();

		// Set message ID for localization
		String msgId = getMsgId(requestInfo);
		log.info("Message ID: {}", msgId);
		requestInfo.setMsgId(msgId);

		// Convert objects to JSON nodes
		ObjectNode processInstanceJson = objectMapper.valueToTree(processInstance);
		ObjectNode regularizationJson = objectMapper.valueToTree(regularization);
		ObjectNode requestInfoJson = objectMapper.valueToTree(requestInfo);

		// Add additional fields to regularization JSON
		LocalDate now = LocalDate.now();
		regularizationJson.put("currentDate", now.toString());
		regularizationJson.put("scnLetterNo", scnLetterNo.trim());
		regularizationJson.set("processInstance", processInstanceJson);

		// Build final request JSON
		ObjectNode requestJson = objectMapper.createObjectNode();
		requestJson.set("RequestInfo", requestInfoJson);

		ArrayNode regularizationArrayNode = objectMapper.createArrayNode();
		regularizationArrayNode.add(regularizationJson);
		requestJson.set("Regularization", regularizationArrayNode);

		return requestJson;
	}

	/**
	 * Creates and persists the notice record with the generated PDF.
	 * 
	 * @param regularization  The regularization entity
	 * @param processInstance The workflow process instance
	 * @param requestInfo     The request info
	 * @param scnLetterNo     The letter number
	 * @param filestoreId     The filestore ID of the generated PDF
	 */
	private void createAndPersistNotice(Regularization regularization, ProcessInstance processInstance,
			RequestInfo requestInfo, String scnLetterNo, String filestoreId) {

		// Build additional details with assignee
		HashMap<String, Object> additionalDetails = new HashMap<>();
		additionalDetails.put("assignee", processInstance.getAssigner().getUuid());

		// Build notice entity
		Notice notice = Notice.builder().businessid(regularization.getApplicationNo())
				.tenantid(regularization.getTenantId()).filestoreid(filestoreId).LetterNo(scnLetterNo)
				.letterType(BPAConstants.SHOWCAUSE_LETTER_TYPE_SCN_IN_WF).isClosed(false)
				.additionalDetails(additionalDetails).build();

		// Create notice request and persist
		NoticeRequest noticeRequest = NoticeRequest.builder().requestInfo(requestInfo).notice(notice).build();

		noticeService.createShowCauseNoticeWithInWF(noticeRequest);
	}

	/**
	 * Set PlotNumbers and Khata Number, so that same can be passed to PDF service
	 * 
	 * @param regularization
	 * @param requestInfo
	 */
	private void setPlotAndKhataNumbersinRegularization(Regularization regularization, RequestInfo requestInfo) {

		String tenantId = regularization.getTenantId();

		Set<String> plotNos = regularization.getLandRegularizationInfo().getPlotInfo().stream()
				.map(item -> item.getPlotNo()).collect(Collectors.toSet());

		Set<String> khataNos = regularization.getLandRegularizationInfo().getPlotInfo().stream()
				.map(item -> item.getKhata()).collect(Collectors.toSet());

		Set<String> villages = regularization.getLandRegularizationInfo().getPlotInfo().stream()
				.map(item -> "OD_" + tenantId.replace("od.", "").toUpperCase() + "_REVENUE_" + item.getVillage())
				.collect(Collectors.toSet());

		String localizationMessages = getLocalizationMessages(tenantId, requestInfo);

		Set<String> localizedVillageNames = villages.stream().map(item -> getVillageName(item, localizationMessages))
				.collect(Collectors.toSet());

		String villageNames = String.join(", ", localizedVillageNames);

		String plotNumbers = String.join(", ", plotNos);

		String khataNumbers = String.join(", ", khataNos);

		regularization.setPlotNumbers(plotNumbers);
		regularization.setKhataNumbers(khataNumbers);
		regularization.setVillages(villageNames);
	}

	/**
	 * Search notice from Notice Service from notice criteria
	 * 
	 * @param applicationNo
	 * @param tenantId
	 * @return
	 */
	private List<Notice> getActiveSncINWFNotice(String applicationNo, String tenantId) {
		NoticeSearchCriteria noticeSearchCriteria = NoticeSearchCriteria.builder()
				.letterType(BPAConstants.SHOWCAUSE_LETTER_TYPE_SCN_IN_WF).businessid(applicationNo).tenantid(tenantId)
				.isClosed(false).build();
		return noticeService.searchNotice(noticeSearchCriteria);
	}

	/**
	 * Processes a pull back request for a regularization application after show
	 * cause notice.
	 * 
	 * <p>
	 * This method handles the scenario where a citizen responds to a show cause
	 * notice by pulling back the application for further action. The process
	 * involves:
	 * <ol>
	 * <li>Validates the pull back request (notice exists, not closed, authorized
	 * user, time elapsed)</li>
	 * <li>Retrieves the regularization application and its workflow state</li>
	 * <li>Determines the appropriate workflow action based on current state</li>
	 * <li>Forwards the application back to the original assignee</li>
	 * <li>Closes the show cause notice</li>
	 * </ol>
	 * </p>
	 * 
	 * @param requestInfoWrapper The request info wrapper containing user context
	 * @param applicationNo      The application number of the regularization
	 * @param noticeId           The ID of the show cause notice
	 * @return The updated (closed) notice
	 * @throws CustomException if validation fails
	 */
	public Notice processRegularizationApplicationPullBack(@Valid RequestInfoWrapper requestInfoWrapper,
			String applicationNo, String noticeId) {

		RequestInfo reqInfo = requestInfoWrapper.getRequestInfo();

		// Step 1: Validate the pull back request
		Notice notice = validateRegularizationApplicationPullBack(requestInfoWrapper, applicationNo, noticeId);

		// Step 2: Extract the original assignee from notice additional details
		String assigneeUuid = extractAssigneeFromNotice(notice);

		// Step 3: Fetch the regularization application
		Regularization regularization = fetchRegularizationByApplicationNo(applicationNo, reqInfo);

		// Step 4: Get current workflow process instance
		List<ProcessInstance> processInstances = workflowIntegrator.getProcessInstances(regularization, reqInfo, false);

		// Step 5: Determine the appropriate workflow action based on current state
		String workflowAction = determineWorkflowActionForPullBack(processInstances);

		// Step 6: Update request info with citizen details for workflow transition
		RequestInfo requestInfo = updateCitizenRequestInfo(regularization, processInstances, reqInfo);

		// Step 7: Build and execute workflow request
		RegularizationRequest regularizationRequest = buildPullBackWorkflowRequest(regularization, requestInfo,
				workflowAction, assigneeUuid);

		// Step 8: Execute workflow transition
		workflowIntegrator.callWorkFlow(requestInfo, regularization);
		regularizationRepository.update(regularizationRequest, true);

		// Step 9: Close the notice and return
		return closeNoticeAfterPullBack(notice, requestInfo);
	}

	/**
	 * Extracts the assignee UUID from the notice's additional details.
	 * 
	 * @param notice The notice containing additional details
	 * @return The assignee UUID, or null if not present
	 */
	private String extractAssigneeFromNotice(Notice notice) {

		ObjectNode additionalDetails = (ObjectNode) notice.getAdditionalDetails();

		if (additionalDetails != null && additionalDetails.has("assignee")) {
			return additionalDetails.get("assignee").textValue();
		}

		return null;
	}

	/**
	 * Fetches a regularization application by its application number.
	 * 
	 * @param applicationNo The application number to search for
	 * @param requestInfo   The request info for the search
	 * @return The regularization application
	 */
	private Regularization fetchRegularizationByApplicationNo(String applicationNo, RequestInfo requestInfo) {

		RegularizationSearchCriteria criteria = new RegularizationSearchCriteria();
		criteria.setApplicationNo(applicationNo);

		return regularizationRepository.searchRegularization(criteria, requestInfo).get(0);
	}

	/**
	 * Determines the appropriate workflow action for pull back based on the current
	 * process state.
	 * 
	 * <p>
	 * The action is determined as follows:
	 * <ul>
	 * <li>If current action is SENDBACK_TO_ARCHITECT_FOR_REWORK → use
	 * FORWARD_TO_APPROVER</li>
	 * <li>If current action is SENDBACKTOCITIZEN → use FORWARD</li>
	 * <li>Default → use FORWARD</li>
	 * </ul>
	 * </p>
	 * 
	 * @param processInstances The list of process instances
	 * @return The appropriate workflow action key
	 */
	private String determineWorkflowActionForPullBack(List<ProcessInstance> processInstances) {

		// Default action is FORWARD
		String actionKey = BPAConstants.ACTION_FORWORD;

		if (processInstances != null && !processInstances.isEmpty() && processInstances.get(0) != null) {
			ProcessInstance processInstance = processInstances.get(0);
			String currentAction = processInstance.getAction();

			// Determine action based on current workflow state
			if (BPAConstants.ACTION_SENDBACK_TO_ARCHITECT_FOR_REWORK.equals(currentAction)) {
				actionKey = "FORWARD_TO_APPROVER";
			} else if (BPAConstants.ACTION_SENDBACKTOCITIZEN.equals(currentAction)) {
				actionKey = BPAConstants.ACTION_FORWORD;
			}
		}

		return actionKey;
	}

	/**
	 * Builds the regularization request with workflow details for pull back action.
	 * 
	 * @param regularization The regularization entity
	 * @param requestInfo    The request info
	 * @param workflowAction The workflow action to execute
	 * @param assigneeUuid   The UUID of the assignee to forward to
	 * @return The constructed regularization request
	 */
	private RegularizationRequest buildPullBackWorkflowRequest(Regularization regularization, RequestInfo requestInfo,
			String workflowAction, String assigneeUuid) {

		// Build workflow object
		Workflow workflow = new Workflow();
		workflow.setAction(workflowAction);
		workflow.setAssignes(Arrays.asList(assigneeUuid));
		workflow.setComments("Application pulled backed");

		// Set workflow on regularization
		regularization.setWorkflow(workflow);

		// Build and return request
		RegularizationRequest regularizationRequest = new RegularizationRequest();
		regularizationRequest.setRequestInfo(requestInfo);
		regularizationRequest.setRegularization(regularization);

		return regularizationRequest;
	}

	/**
	 * Closes the show cause notice after successful pull back.
	 * 
	 * @param notice      The notice to close
	 * @param requestInfo The request info
	 * @return The updated notice
	 */
	private Notice closeNoticeAfterPullBack(Notice notice, RequestInfo requestInfo) {

		notice.setClosed(true);

		NoticeRequest noticeRequest = NoticeRequest.builder().notice(notice).requestInfo(requestInfo).build();

		return noticeService.showCauseReply(noticeRequest);
	}

	/**
	 * set citizen details in request info
	 * 
	 * @param regularization
	 * @param processInstances
	 * @param requestInfo
	 * @return
	 */
	private RequestInfo updateCitizenRequestInfo(Regularization regularization, List<ProcessInstance> processInstances,
			RequestInfo requestInfo) {

		if (processInstances != null && processInstances.get(0) != null) {
			ProcessInstance processInstance = processInstances.get(0);
			String assigneeuuid = regularizationRepository.getAssigneeByprocessInstanceId(processInstance.getId());
			if (assigneeuuid == null)
				assigneeuuid = regularization.getAuditDetails().getCreatedBy();
			UserDetailResponse userDetailResponse = null;
			userDetailResponse = userService.getUserByUUID(assigneeuuid, requestInfo);
			requestInfo.setUserInfo(RegularizationAutoEscalationService.copyUser(userDetailResponse.getUser().get(0)));
		}
		return requestInfo;
	}

	/**
	 * Validate the regularization pull back request
	 * 
	 * @param requestInfoWrapper
	 * @param applicationNo
	 * @param noticeId
	 * @return
	 */
	private Notice validateRegularizationApplicationPullBack(@Valid RequestInfoWrapper requestInfoWrapper,
			String applicationNo, String noticeId) {

		if (applicationNo == null || noticeId == null) {
			throw new IllegalArgumentException("Invalid input parameters, applicationNo and noticeId cannot be null.");
		}
		List<String> ids = new LinkedList<>();
		ids.add(noticeId);
		NoticeSearchCriteria noticeSearchCriteria = NoticeSearchCriteria.builder()
				.letterType(BPAConstants.SHOWCAUSE_LETTER_TYPE_SCN_IN_WF).businessid(applicationNo).ids(ids).build();
		List<Notice> notices = noticeService.searchNotice(noticeSearchCriteria);
		if (notices.isEmpty()) {
			throw new CustomException("Search Error", "No notice found for applicationNo:" + applicationNo
					+ "& noticeId: " + noticeId + "in letterType: " + BPAConstants.SHOWCAUSE_LETTER_TYPE_SCN_IN_WF);
		}
		Notice notice = notices.get(0);
		if (notice.isClosed()) {
			throw new CustomException("Update Error",
					"Notice is already closed for applicationNo:" + applicationNo + "& noticeId: " + noticeId);
		}
		String assigneeValue = null;
		ObjectNode additionalDetails = (ObjectNode) notice.getAdditionalDetails();

		if (additionalDetails != null) {
			assigneeValue = (String) additionalDetails.get("assignee").textValue();
		}
		if (assigneeValue == null) {
			throw new CustomException("Update Error",
					"Assignee not found in notice for :" + applicationNo + "& noticeId: " + noticeId);
		}
		if (!requestInfoWrapper.getRequestInfo().getUserInfo().getUuid().equals(assigneeValue)) {
			throw new CustomException("Update Error", "You are not authorized to update Notice for applicationNo:"
					+ applicationNo + "& noticeId: " + noticeId);
		}
		Long currentTimeMillis = System.currentTimeMillis();
		Long createdTime = notice.getAuditDetails().getCreatedTime();
		Long period = TimeUnit.MILLISECONDS.toDays(currentTimeMillis - createdTime);
		if (period < config.getBpaNoticeRevokePeriod()) {
			throw new CustomException("Update Error",
					"It has not been " + config.getBpaNoticeRevokePeriod() + " days since notice was created");
		}
		return notice;
	}

	public String getLocalizationMessages(String tenantId, RequestInfo requestInfo) {

		LinkedHashMap responseMap = (LinkedHashMap) serviceRequestRepository.fetchResult(getUri(tenantId, requestInfo),
				requestInfo);
		String jsonString = new JSONObject(responseMap).toString();
		return jsonString;
	}

	private StringBuilder getUri(String tenantId, RequestInfo requestInfo) {

		if (config.getIsLocalizationStateLevel())
			tenantId = tenantId.split("\\.")[0];

		String locale = "en_IN";
		if (!StringUtils.isEmpty(requestInfo.getMsgId()) && requestInfo.getMsgId().split("|").length >= 2)
			locale = requestInfo.getMsgId().split("\\|")[1];

		StringBuilder uri = new StringBuilder();
		uri.append(config.getLocalizationHost()).append(config.getLocalizationContextPath())
				.append(config.getLocalizationSearchEndpoint()).append("?").append("locale=").append(locale)
				.append("&tenantId=").append(tenantId).append("&module=").append(RegularizationConstants.SEARCH_MODULE);
		return uri;

	}

	public String getVillageName(String code, String localizationMessage) {
		String path = "$..messages[?(@.code==\"{}\")].message";
		path = path.replace("{}", code);
		String villageName = null;
		try {
			List data = JsonPath.parse(localizationMessage).read(path);
			if (!CollectionUtils.isEmpty(data))
				villageName = data.get(0).toString();
			else
				log.error("Fetching from localization failed with code " + code);
		} catch (Exception e) {
			log.warn("Fetching from localization failed", e);
		}
		return villageName;
	}

	/**
	 * Creates a Refusal Show Cause Notice for a regularization application.
	 * 
	 * <p>
	 * This method handles the creation of refusal show cause notices which are
	 * issued when an application is being considered for rejection. The process
	 * involves:
	 * <ol>
	 * <li>Fetches and validates the regularization application</li>
	 * <li>Enriches regularization with owner details and plot/khata numbers</li>
	 * <li>Validates that no existing refusal SCN exists for the application</li>
	 * <li>Extracts and enriches refusal reasons from notice additional details</li>
	 * <li>Generates a unique letter number for the notice</li>
	 * <li>Generates the refusal SCN PDF document</li>
	 * <li>Creates and persists the notice record</li>
	 * </ol>
	 * </p>
	 * 
	 * @param notice      The notice object containing business ID and refusal
	 *                    reasons
	 * @param requestInfo The request info containing user context
	 * @return The created refusal show cause notice
	 * @throws CustomException if regularization not found or refusal SCN already
	 *                         exists
	 */
	public Notice createRefusalShowCauseNotice(Notice notice, RequestInfo requestInfo) {

		// Step 1: Fetch and validate regularization application
		Regularization regularization = fetchAndValidateRegularizationForRefusalScn(notice, requestInfo);

		// Step 2: Enrich regularization with plot and khata numbers for PDF
		setPlotAndKhataNumbersinRegularization(regularization, requestInfo);

		// Step 3: Validate no existing refusal SCN exists
		validateRefusalShowCauseNoticeCreate(regularization);

		// Step 4: Build regularization request
		RegularizationRequest regularizationRequest = buildRegularizationRequest(regularization, requestInfo);

		// Step 5: Extract and enrich refusal reasons
		List<String> refusalReasons = getRefusalReasons(notice);
		String refusalReasonsEnriched = enrichRefusalReasons(regularizationRequest, refusalReasons);

		// Step 6: Get current process instance with action taker details
		ProcessInstance processInstance = getProcessInstanceWithActionTaker(regularization, requestInfo);

		// Step 7: Generate unique letter number
		String scnLetterNo = generateShowCauseLetterNumber(regularizationRequest);

		// Step 8: Generate refusal SCN PDF
		String filestoreId = generateRefusalShowCausePdf(regularization, processInstance, requestInfo, scnLetterNo,
				refusalReasonsEnriched);

		// Step 9: Create and persist the notice
		return createAndPersistRefusalNotice(regularization, processInstance, requestInfo, scnLetterNo, filestoreId,
				notice);
	}

	/**
	 * Fetches and validates the regularization application for refusal SCN
	 * creation.
	 * 
	 * <p>
	 * Searches for the regularization by application number and enriches with owner
	 * details.
	 * </p>
	 * 
	 * @param notice      The notice containing business ID (application number)
	 * @param requestInfo The request info for search and enrichment
	 * @return The validated and enriched regularization application
	 * @throws CustomException if no regularization found
	 */
	private Regularization fetchAndValidateRegularizationForRefusalScn(Notice notice, RequestInfo requestInfo) {

		RegularizationSearchCriteria searchCriteria = RegularizationSearchCriteria.builder()
				.applicationNo(notice.getBusinessid()).tenantId(notice.getTenantid()).build();

		List<Regularization> regularizations = regularizationRepository.searchRegularization(searchCriteria,
				requestInfo);

		if (CollectionUtils.isEmpty(regularizations)) {
			throw new CustomException("NO BPA EXIST",
					"No Regularization Application exist for application number: " + notice.getBusinessid());
		}

		// Enrich with owner details
		getOwnersDetails(regularizations, requestInfo);

		return regularizations.get(0);
	}

	/**
	 * Gets the current process instance and updates with action taker details.
	 * 
	 * @param regularization The regularization entity
	 * @param requestInfo    The request info containing action taker
	 * @return The process instance with updated action taker name
	 */
	private ProcessInstance getProcessInstanceWithActionTaker(Regularization regularization, RequestInfo requestInfo) {

		ProcessInstance processInstance = new ProcessInstance();

		List<ProcessInstance> processInstances = workflowIntegrator.getProcessInstances(regularization, requestInfo,
				false);

		if (!processInstances.isEmpty()) {
			processInstance = processInstances.get(0);

			// Update with action taker name
			String actionTakenBy = requestInfo.getUserInfo().getName();
			log.info("Action taken by user: {}", actionTakenBy);
			processInstance.getAssigner().setName(actionTakenBy);
		}

		return processInstance;
	}

	/**
	 * Generates the Refusal Show Cause Notice PDF by calling the PDF service.
	 * 
	 * <p>
	 * Builds the PDF request JSON with regularization details, refusal reasons,
	 * owner information, and other required fields.
	 * </p>
	 * 
	 * @param regularization         The regularization entity
	 * @param processInstance        The workflow process instance
	 * @param requestInfo            The request info
	 * @param scnLetterNo            The generated letter number
	 * @param refusalReasonsEnriched The enriched refusal reasons string
	 * @return The filestore ID of the generated PDF
	 */
	@SuppressWarnings("unchecked")
	private String generateRefusalShowCausePdf(Regularization regularization, ProcessInstance processInstance,
			RequestInfo requestInfo, String scnLetterNo, String refusalReasonsEnriched) {

		String tenantId = regularization.getTenantId();

		// Build PDF service URI
		StringBuilder uri = buildPdfServiceUri(tenantId, RegularizationConstants.REFUSAL_SHOW_CAUSE_KEY);

		// Build PDF request JSON
		ObjectNode requestJson = buildRefusalShowCausePdfRequest(regularization, processInstance, requestInfo,
				scnLetterNo, refusalReasonsEnriched);

		// Call PDF service and get filestore ID
		LinkedHashMap<String, Object> fetchResult = (LinkedHashMap<String, Object>) serviceRequestRepository
				.fetchResult(uri, requestJson);
		List<String> filestoreIds = (List<String>) fetchResult.get("filestoreIds");

		return filestoreIds.get(0);
	}

	/**
	 * Builds the JSON request payload for Refusal Show Cause Notice PDF generation.
	 * 
	 * <p>
	 * This method orchestrates the PDF request construction:
	 * <ol>
	 * <li>Converts domain objects to JSON nodes</li>
	 * <li>Enriches regularization JSON with additional fields for PDF template</li>
	 * <li>Builds the final request JSON structure</li>
	 * </ol>
	 * </p>
	 * 
	 * @param regularization         The regularization entity
	 * @param processInstance        The workflow process instance
	 * @param requestInfo            The request info
	 * @param scnLetterNo            The letter number
	 * @param refusalReasonsEnriched The enriched refusal reasons
	 * @return The constructed JSON request
	 */
	private ObjectNode buildRefusalShowCausePdfRequest(Regularization regularization, ProcessInstance processInstance,
			RequestInfo requestInfo, String scnLetterNo, String refusalReasonsEnriched) {

		ObjectMapper objectMapper = new ObjectMapper();

		// Step 1: Convert domain objects to JSON nodes
		ObjectNode regularizationJson = objectMapper.valueToTree(regularization);
		ObjectNode processInstanceJson = objectMapper.valueToTree(processInstance);
		ObjectNode requestInfoJson = objectMapper.valueToTree(requestInfo);

		// Step 2: Enrich regularization JSON with additional fields for PDF template
		enrichRegularizationJsonForRefusalPdf(objectMapper, regularizationJson, regularization, processInstanceJson,
				scnLetterNo, refusalReasonsEnriched);

		// Step 3: Build and return the final request JSON
		return buildFinalPdfRequest(objectMapper, requestInfoJson, regularizationJson);
	}

	/**
	 * Enriches the regularization JSON with additional fields required for refusal
	 * SCN PDF.
	 * 
	 * <p>
	 * Adds the following fields to the regularization JSON:
	 * <ul>
	 * <li>currentDate - Current date for the notice</li>
	 * <li>scnLetterNo - Letter number for the notice</li>
	 * <li>processInstance - Workflow process instance details</li>
	 * <li>refusalReason - Enriched refusal reasons text</li>
	 * <li>plotNumber, mouza, serviceType - Property details</li>
	 * <li>ownerName, ownerAddress - Primary owner details</li>
	 * </ul>
	 * </p>
	 * 
	 * @param objectMapper           The Jackson object mapper
	 * @param regularizationJson     The regularization JSON to enrich
	 * @param regularization         The regularization entity for extracting values
	 * @param processInstanceJson    The process instance JSON
	 * @param scnLetterNo            The letter number
	 * @param refusalReasonsEnriched The enriched refusal reasons
	 */
	private void enrichRegularizationJsonForRefusalPdf(ObjectMapper objectMapper, ObjectNode regularizationJson,
			Regularization regularization, ObjectNode processInstanceJson, String scnLetterNo,
			String refusalReasonsEnriched) {

		// Add current date
		regularizationJson.put("currentDate", LocalDate.now().toString());

		// Add letter number and process instance
		regularizationJson.set("scnLetterNo", objectMapper.valueToTree(scnLetterNo));
		regularizationJson.set("processInstance", processInstanceJson);

		// Add refusal reasons
		regularizationJson.set("refusalReason", objectMapper.valueToTree(refusalReasonsEnriched));

		// Add property details
		addPropertyDetailsToJson(objectMapper, regularizationJson, regularization);

		// Add owner details
		addOwnerDetailsToJson(objectMapper, regularizationJson, regularization);
	}

	/**
	 * Adds property details (plot number, mouza, service type) to the
	 * regularization JSON.
	 * 
	 * @param objectMapper       The Jackson object mapper
	 * @param regularizationJson The regularization JSON to add fields to
	 * @param regularization     The regularization entity
	 */
	private void addPropertyDetailsToJson(ObjectMapper objectMapper, ObjectNode regularizationJson,
			Regularization regularization) {

		regularizationJson.set("plotNumber", objectMapper.valueToTree(regularization.getPlotNumbers()));
		regularizationJson.set("mouza", objectMapper.valueToTree(regularization.getVillages()));
		regularizationJson.set("serviceType", objectMapper.valueToTree(regularization.getServiceType()));
	}

	/**
	 * Adds primary owner details (name, address) to the regularization JSON.
	 * 
	 * <p>
	 * Uses the first owner in the owners list as the primary owner.
	 * </p>
	 * 
	 * @param objectMapper       The Jackson object mapper
	 * @param regularizationJson The regularization JSON to add fields to
	 * @param regularization     The regularization entity
	 */
	private void addOwnerDetailsToJson(ObjectMapper objectMapper, ObjectNode regularizationJson,
			Regularization regularization) {

		// Get primary owner (first in list)
		OwnerInfo primaryOwner = regularization.getOwners().get(0);

		regularizationJson.set("ownerName", objectMapper.valueToTree(primaryOwner.getName()));
		regularizationJson.set("ownerAddress", objectMapper.valueToTree(primaryOwner.getCorrespondenceAddress()));
	}

	/**
	 * Builds the final PDF request JSON structure.
	 * 
	 * <p>
	 * Creates the request with:
	 * <ul>
	 * <li>RequestInfo - Request metadata</li>
	 * <li>Regularization - Array containing the enriched regularization JSON</li>
	 * </ul>
	 * </p>
	 * 
	 * @param objectMapper       The Jackson object mapper
	 * @param requestInfoJson    The request info JSON
	 * @param regularizationJson The enriched regularization JSON
	 * @return The final request JSON
	 */
	private ObjectNode buildFinalPdfRequest(ObjectMapper objectMapper, ObjectNode requestInfoJson,
			ObjectNode regularizationJson) {

		ObjectNode requestJson = objectMapper.createObjectNode();

		// Add request info
		requestJson.set("RequestInfo", requestInfoJson);

		// Add regularization as array (PDF service expects array format)
		ArrayNode regularizationArrayNode = objectMapper.createArrayNode();
		regularizationArrayNode.add(regularizationJson);
		requestJson.set("Regularization", regularizationArrayNode);

		return requestJson;
	}

	/**
	 * Creates and persists the refusal show cause notice record.
	 * 
	 * @param regularization  The regularization entity
	 * @param processInstance The workflow process instance
	 * @param requestInfo     The request info
	 * @param scnLetterNo     The letter number
	 * @param filestoreId     The filestore ID of the generated PDF
	 * @param originalNotice  The original notice containing additional details
	 * @return The created notice
	 */
	@SuppressWarnings("unchecked")
	private Notice createAndPersistRefusalNotice(Regularization regularization, ProcessInstance processInstance,
			RequestInfo requestInfo, String scnLetterNo, String filestoreId, Notice originalNotice) {

		// Get additional details from original notice and add assignee
		HashMap<String, Object> additionalDetails = (HashMap<String, Object>) originalNotice.getAdditionalDetails();
		if (!ObjectUtils.isEmpty(processInstance) && processInstance.getAssigner() != null) {
			additionalDetails.put("assignee", processInstance.getAssigner().getUuid());
		}

		// Build notice entity
		Notice createdNotice = Notice.builder().businessid(regularization.getApplicationNo())
				.tenantid(regularization.getTenantId()).filestoreid(filestoreId).LetterNo(scnLetterNo)
				.letterType(BPAConstants.REFUSAL_SHOWCAUSE_LETTER_TYPE).isClosed(false)
				.additionalDetails(additionalDetails).build();

		// Create notice request and persist
		NoticeRequest noticeRequest = NoticeRequest.builder().requestInfo(requestInfo).notice(createdNotice).build();

		noticeService.createShowCauseNoticeWithInWF(noticeRequest);

		return noticeRequest.getnotice();
	}

	private String enrichRefusalReasons(Regularization regularization, RequestInfo requestInfo,
			List<String> refusalReasons) {

		log.info("Reasons Before Enrich" + refusalReasons);
		String localizationMessages = util.getLocalizationMessages(regularization.getTenantId(), requestInfo);
		List<String> enrichedReasons = new ArrayList();

//		List<String> enrichedReasons = reasons.stream()
//				.map(reason -> notificationUtil.getMessageTemplate(reason, localizationMessages))
//				.filter(StringUtils::isNotEmpty).collect(Collectors.toList());

		refusalReasons.stream().map(reason -> {
			String message = util.getMessageTemplate(reason, localizationMessages);
			return StringUtils.isEmpty(message) ? reason : message;
		}).forEach(enrichedReasons::add);
		return enrichedReasons.stream().collect(Collectors.joining(","));
	}

	private String enrichRefusalReasons(RegularizationRequest regularizationRequest, List<String> refusalReasons) {
		log.info("Reasons Before Enrich" + refusalReasons);
		String localizationMessages = notificationUtil.getLocalizationMessages(
				regularizationRequest.getRegularization().getTenantId(), regularizationRequest.getRequestInfo());
		List<String> enrichedReasons = new ArrayList();

//		List<String> enrichedReasons = reasons.stream()
//				.map(reason -> notificationUtil.getMessageTemplate(reason, localizationMessages))
//				.filter(StringUtils::isNotEmpty).collect(Collectors.toList());

		refusalReasons.stream().map(reason -> {
			String message = notificationUtil.getMessageTemplate(reason, localizationMessages);
			return StringUtils.isEmpty(message) ? reason : message;
		}).forEach(enrichedReasons::add);
		return enrichedReasons.stream().collect(Collectors.joining(","));
	}

	/**
	 * Extracts refusal reasons from the notice's additional details.
	 * 
	 * <p>
	 * This method processes the refusal show cause notice reasons:
	 * <ol>
	 * <li>Extracts the refusalShowCauseNoticeReason map from additional
	 * details</li>
	 * <li>Converts the map entries to a list format</li>
	 * <li>Filters for active reasons (value = "true") and manual reasons</li>
	 * </ol>
	 * </p>
	 * 
	 * <p>
	 * Reasons are stored in additional details as a map where:
	 * <ul>
	 * <li>Keys are reason codes (e.g., "REASON_1", "REASON_2")</li>
	 * <li>Values are "true"/"false" for predefined reasons</li>
	 * <li>MANUAL_OTHER_REASON key contains custom text reason</li>
	 * </ul>
	 * </p>
	 * 
	 * @param notice The notice containing refusal reasons in additional details
	 * @return List of active refusal reason codes and manual reason text
	 */
	@SuppressWarnings("unchecked")
	private List<String> getRefusalReasons(Notice notice) {

		log.info("Preparing SCN reasons payload");

		// Step 1: Get additional details from notice
		HashMap<String, Object> additionalDetail = (HashMap<String, Object>) notice.getAdditionalDetails();

		if (CollectionUtils.isEmpty(additionalDetail)) {
			return new ArrayList<>();
		}

		// Step 2: Extract refusal reasons map from additional details
		Map<String, Object> refusalScnReasonsMap = extractRefusalReasonsMap(additionalDetail);

		if (refusalScnReasonsMap == null) {
			return new ArrayList<>();
		}

		// Step 3: Convert map to list of entries
		List<Map<String, String>> refusalScnReasonsList = convertReasonsMapToList(refusalScnReasonsMap);

		// Step 4: Filter and collect active reasons
		List<String> refusalScnReasons = filterActiveReasons(refusalScnReasonsList);

		log.info("Extracted refusal SCN reasons: {}", refusalScnReasons);
		return refusalScnReasons;
	}

	/**
	 * Extracts the refusal show cause notice reasons map from additional details.
	 * 
	 * @param additionalDetail The additional details map from the notice
	 * @return The refusal reasons map, or null if not present
	 */
	@SuppressWarnings("unchecked")
	private Map<String, Object> extractRefusalReasonsMap(HashMap<String, Object> additionalDetail) {

		return (Map<String, Object>) additionalDetail.get("refusalShowCauseNoticeReason");
	}

	/**
	 * Converts the refusal reasons map to a list of map entries.
	 * 
	 * <p>
	 * Each entry in the result list contains a single key-value pair from the
	 * original reasons map.
	 * </p>
	 * 
	 * @param refusalScnReasonsMap The map of reason codes to values
	 * @return List of single-entry maps
	 */
	private List<Map<String, String>> convertReasonsMapToList(Map<String, Object> refusalScnReasonsMap) {

		List<Map<String, String>> refusalScnReasonsList = new ArrayList<>();

		for (Map.Entry<String, Object> entry : refusalScnReasonsMap.entrySet()) {
			Map<String, String> reasonEntry = new HashMap<>();
			reasonEntry.put(entry.getKey(), entry.getValue().toString());
			refusalScnReasonsList.add(reasonEntry);
		}

		return refusalScnReasonsList;
	}

	/**
	 * Filters and collects active refusal reasons from the reasons list.
	 * 
	 * <p>
	 * Processing rules:
	 * <ul>
	 * <li>Includes reason codes where value equals "true" (case-insensitive)</li>
	 * <li>Excludes MANUAL_OTHER_REASON from the "true" check</li>
	 * <li>Includes the value of MANUAL_OTHER_REASON if present (custom text)</li>
	 * </ul>
	 * </p>
	 * 
	 * @param refusalScnReasonsList The list of reason entries to filter
	 * @return List of active reason codes and manual reason text
	 */
	private List<String> filterActiveReasons(List<Map<String, String>> refusalScnReasonsList) {

		if (CollectionUtils.isEmpty(refusalScnReasonsList)) {
			return new ArrayList<>();
		}

		return refusalScnReasonsList.stream().flatMap(map -> extractActiveReasonsFromEntry(map).stream())
				.collect(Collectors.toList());
	}

	/**
	 * Extracts active reasons from a single reason entry map.
	 * 
	 * @param reasonEntry A single reason entry map
	 * @return List of active reason keys and/or manual reason value
	 */
	private List<String> extractActiveReasonsFromEntry(Map<String, String> reasonEntry) {

		List<String> activeReasons = new ArrayList<>();

		// Collect keys where value is "true" (excluding MANUAL_OTHER_REASON)
		reasonEntry.entrySet().stream().filter(entry -> isActiveReason(entry)).map(Map.Entry::getKey)
				.forEach(activeReasons::add);

		// Add the value of MANUAL_OTHER_REASON if present (custom text reason)
		Optional.ofNullable(reasonEntry.get("MANUAL_OTHER_REASON")).filter(value -> !value.isEmpty())
				.ifPresent(activeReasons::add);

		return activeReasons;
	}

	/**
	 * Checks if a reason entry represents an active (selected) reason.
	 * 
	 * <p>
	 * A reason is active if:
	 * <ul>
	 * <li>Its value is "true" (case-insensitive)</li>
	 * <li>Its key is not MANUAL_OTHER_REASON (handled separately)</li>
	 * </ul>
	 * </p>
	 * 
	 * @param entry The reason entry to check
	 * @return true if the reason is active
	 */
	private boolean isActiveReason(Map.Entry<String, String> entry) {

		return "true".equalsIgnoreCase(entry.getValue()) && !"MANUAL_OTHER_REASON".equals(entry.getKey());
	}

	private void validateRefusalShowCauseNoticeCreate(Regularization regularization) {
		String applicationNo = regularization.getApplicationNo();
		NoticeSearchCriteria noticeSearchCriteria = NoticeSearchCriteria.builder().businessid(applicationNo)
				.letterType(BPAConstants.REFUSAL_SHOWCAUSE_LETTER_TYPE).build();

		List<Notice> notices = scnRepository.getNoticeDataForRegularization(noticeSearchCriteria);
		;

		if (!CollectionUtils.isEmpty(notices)) {
			String letterNo = notices.get(0).getLetterNo();
			throw new CustomException("NOTICE_EXIST_ERROR", String.format(
					"Refusal SCN already exists for business ID: %s. Letter Number: %s", applicationNo, letterNo));
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

	/**
	 * Enriches regularization applications with owner details from the user
	 * service.
	 * 
	 * <p>
	 * This method performs the following operations:
	 * <ol>
	 * <li>Extracts all unique owner UUIDs from the regularization list</li>
	 * <li>Fetches user details for all owners in a single API call</li>
	 * <li>Maps user details back to corresponding owners in each
	 * regularization</li>
	 * </ol>
	 * </p>
	 * 
	 * @param regularizationList The list of regularizations to enrich with owner
	 *                           details
	 * @param requestInfo        The request info for user service API calls
	 */
	public void getOwnersDetails(List<Regularization> regularizationList, RequestInfo requestInfo) {

		// Step 1: Extract all owner UUIDs from regularizations
		List<String> ownersUuids = extractOwnerUuidsFromRegularizations(regularizationList);

		// Early exit if no owner UUIDs found
		if (CollectionUtils.isEmpty(ownersUuids)) {
			log.debug("No owner UUIDs found to enrich");
			return;
		}

		// Step 2: Fetch user details for all owners from user service
		UserDetailResponse response = regularizationUserService.getUsersInfo(ownersUuids, requestInfo);

		// Step 3: Map user details back to owners
		if (!ObjectUtils.isEmpty(response) && !CollectionUtils.isEmpty(response.getUser())) {
			mapUserDetailsToRegularizationOwners(regularizationList, response);
		}
	}

	/**
	 * Extracts all unique owner UUIDs from a list of regularizations.
	 * 
	 * @param regularizationList The list of regularizations
	 * @return List of owner UUIDs
	 */
	private List<String> extractOwnerUuidsFromRegularizations(List<Regularization> regularizationList) {

		return regularizationList.stream()
				// Filter regularizations that have owners
				.filter(regularization -> !regularization.getOwners().isEmpty())
				// Flatten to stream of owners
				.flatMap(regularization -> regularization.getOwners().stream())
				// Extract UUID from each owner
				.map(owner -> owner.getUuid())
				// Collect to list
				.collect(Collectors.toList());
	}

	/**
	 * Maps user details from user service response to corresponding owners in
	 * regularizations.
	 * 
	 * <p>
	 * Matches owners by UUID and enriches them with user details (excluding audit
	 * details).
	 * </p>
	 * 
	 * @param regularizationList The list of regularizations containing owners
	 * @param response           The user service response containing user details
	 */
	private void mapUserDetailsToRegularizationOwners(List<Regularization> regularizationList,
			UserDetailResponse response) {

		response.getUser().forEach(user -> {
			regularizationList.forEach(regularization -> {
				regularization.getOwners().forEach(owner -> {
					// Match owner by UUID and enrich with user details
					if (owner.getUuid().equals(user.getUuid())) {
						owner.addUserWithoutAuditDetail(user);
					}
				});
			});
		});
	}

}