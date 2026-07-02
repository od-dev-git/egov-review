package org.egov.bpa.service;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.validation.Valid;

import org.egov.bpa.repository.RegularizationRepository;
import org.egov.bpa.repository.ScnRepository;
import org.egov.bpa.util.BPAConstants;
import org.egov.bpa.util.BPAErrorConstants;
import org.egov.bpa.web.model.Notice;
import org.egov.bpa.web.model.NoticeRequest;
import org.egov.bpa.web.model.NoticeSearchCriteria;
import org.egov.bpa.web.model.regularization.Regularization;
import org.egov.bpa.web.model.regularization.RegularizationRequest;
import org.egov.bpa.web.model.regularization.RegularizationSearchCriteria;
import org.egov.common.contract.request.RequestInfo;
import org.egov.tracer.model.CustomException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class RegularizationNoticeService {

	@Autowired
	private ScnRepository repository;

	@Autowired
	private RegularizationRepository regularizationRepository;

	@Autowired
	private RegularizationWorkflow wfIntegrator;

	@Autowired
	private RegularizationEnrichmentService enrichmentService;

	@Autowired
	private RegularizationService regularizationService;

	/**
	 * Service layer for create notice
	 * 
	 * @param request
	 * @return
	 */
	public Notice createNotice(@Valid NoticeRequest request) {
		RequestInfo requestInfo = request.getRequestInfo();
		log.info("Appl Number For Notice creation : " + request.getnotice().getBusinessid());
		enrichmentService.enrichScnCreateRequest(request);
		validateCreateRequest(request);
		repository.saveForRegularization(request);

		return request.getnotice();
	}

	/**
	 * Service layer for search notice
	 * 
	 * @param request
	 * @return
	 */
	public List<Notice> searchNotice(@Valid NoticeSearchCriteria criteria) {
		Map<String, String> errorMap = new HashMap<>();
		if (!errorMap.isEmpty())
			throw new CustomException(errorMap);
		else {
			List<Notice> noticeSearchResult = repository.getNoticeDataForRegularization(criteria);
			if (noticeSearchResult.isEmpty()) {
				return Collections.emptyList();
			} else {
				return noticeSearchResult;
			}
		}
	}

	/**
	 * Service layer for updating an existing notice.
	 * 
	 * <p>
	 * This method orchestrates the notice update process by performing:
	 * <ol>
	 * <li>Validates that the notice has a valid ID for update</li>
	 * <li>Fetches and validates the existing notice from the database</li>
	 * <li>Enriches the notice with update metadata</li>
	 * <li>Triggers workflow transition for the notice</li>
	 * <li>Handles REVOKE action - updates regularization and creates revoke
	 * notice</li>
	 * <li>Persists the updated notice to the database</li>
	 * </ol>
	 * </p>
	 * 
	 * @param request The notice request containing the notice to update
	 * @return The updated notice
	 * @throws CustomException if notice ID is missing or notice not found
	 */
	public Notice updateNotice(@Valid NoticeRequest request) {

		Notice notice = request.getnotice();

		// Step 1: Validate that notice has an ID for update
		validateNoticeForUpdate(notice);

		// Step 2: Fetch and validate existing notice from database
		fetchAndValidateExistingNotice(notice);

		// Step 3: Enrich notice with update metadata
		enrichmentService.enrichScnUpdateRequest(request);

		// Step 4: Trigger workflow transition
		wfIntegrator.callWorkFlowforNotice(request);

		// Step 5: Handle REVOKE action if applicable
		if (isRevokeAction(notice)) {
			processRevokeAction(request);
		}

		// Step 6: Persist the updated notice
		repository.updateForRegularization(request);

		return request.getnotice();
	}

	/**
	 * Validates that the notice has a valid ID for update operation.
	 * 
	 * @param notice The notice to validate
	 * @throws CustomException if notice ID is null
	 */
	private void validateNoticeForUpdate(Notice notice) {
		if (notice.getId() == null) {
			throw new CustomException("Update Error", "Failed to update notice no notice found!");
		}
	}

	/**
	 * Fetches and validates the existing notice from the database.
	 * 
	 * <p>
	 * Ensures that exactly one notice exists with the given letter number and
	 * tenant ID. Throws an exception if none or multiple notices are found.
	 * </p>
	 * 
	 * @param notice The notice containing search criteria (letterNo, tenantId)
	 * @throws CustomException if no notice found or multiple notices found
	 */
	private void fetchAndValidateExistingNotice(Notice notice) {

		NoticeSearchCriteria criteria = new NoticeSearchCriteria();
		criteria.setLetterNo(notice.getLetterNo());
		criteria.setTenantid(notice.getTenantid());

		List<Notice> noticeSearchResult = repository.getNoticeDataForRegularization(criteria);

		// Validate exactly one notice exists
		if (CollectionUtils.isEmpty(noticeSearchResult) || noticeSearchResult.size() > 1) {
			throw new CustomException(BPAErrorConstants.UPDATE_ERROR,
					"Failed to Update the Notice, Found None or multiple applications!");
		}
	}

	/**
	 * Checks if the workflow action is a REVOKE action.
	 * 
	 * @param notice The notice containing workflow action
	 * @return true if action is REVOKE, false otherwise
	 */
	private boolean isRevokeAction(Notice notice) {
		return notice.getWorkflow() != null && notice.getWorkflow().getAction() != null
				&& notice.getWorkflow().getAction().equalsIgnoreCase(BPAConstants.REVOKE);
	}

	/**
	 * Processes the REVOKE action for a notice.
	 * 
	 * <p>
	 * This method handles the revocation workflow:
	 * <ul>
	 * <li>Updates the associated regularization application with revoke flag</li>
	 * <li>Creates a revoke notice if additional details are provided</li>
	 * </ul>
	 * </p>
	 * 
	 * @param request The notice request being processed
	 */
	private void processRevokeAction(NoticeRequest request) {

		Notice notice = request.getnotice();
		RequestInfo requestInfo = request.getRequestInfo();

		// Update the associated regularization application
		updateRegularizationForRevoke(notice, requestInfo);

		// Create revoke notice if additional details are provided
		createRevokeNoticeIfRequired(notice, requestInfo);
	}

	/**
	 * Updates the regularization application with the revoke flag.
	 * 
	 * <p>
	 * Fetches the regularization associated with the notice and updates it with the
	 * "isActionRevoked" flag in additional details.
	 * </p>
	 * 
	 * @param notice      The notice containing business ID (application number)
	 * @param requestInfo The request info for API calls
	 */
	private void updateRegularizationForRevoke(Notice notice, RequestInfo requestInfo) {

		// Build search criteria for regularization
		RegularizationSearchCriteria searchCriteria = new RegularizationSearchCriteria();
		searchCriteria.setApplicationNo(notice.getBusinessid());
		searchCriteria.setTenantId(notice.getTenantid());

		// Fetch the associated regularization
		List<Regularization> regularizations = regularizationRepository.searchRegularization(searchCriteria,
				requestInfo);

		// Build and update the regularization request
		RegularizationRequest regularizationRequest = new RegularizationRequest();
		regularizationRequest.setRequestInfo(requestInfo);
		regularizationRequest.setRegularization(regularizations.get(0));

		// Add revoke flag to additional details
		addAttributesInAdditionDetails(regularizationRequest);

		// Update the regularization
		regularizationService.update(regularizationRequest);
	}

	/**
	 * Creates a revoke notice if additional details are provided.
	 * 
	 * @param notice      The notice containing additional details
	 * @param requestInfo The request info for API calls
	 */
	private void createRevokeNoticeIfRequired(Notice notice, RequestInfo requestInfo) {

		Object additionalDetails = notice.getAdditionalDetails();

		if (Objects.nonNull(additionalDetails)) {
			createRevokeNotice(additionalDetails, requestInfo);
		}
	}

	/**
	 * If notice is revoke, update the notice accordingly
	 * 
	 * @param additionalDetails
	 * @param requestInfo
	 */
	private void createRevokeNotice(Object additionalDetails, @Valid RequestInfo requestInfo) {

		Map<String, String> ApplicationDetails = (Map<String, String>) additionalDetails;
		NoticeRequest noticeRquest = new NoticeRequest();
		noticeRquest.setRequestInfo(requestInfo);
		Notice notice = new Notice();
		notice.setBusinessid(ApplicationDetails.get(BPAConstants.BUSINESSIDS));
		notice.setFilestoreid(ApplicationDetails.get(BPAConstants.FILESTOREIDS));
		notice.setLetterNo(ApplicationDetails.get(BPAConstants.LETTERNO));
		notice.setLetterType(ApplicationDetails.get(BPAConstants.LETTERTYPE));
		notice.setTenantid(ApplicationDetails.get(BPAConstants.TENANTIDS));
		noticeRquest.notice(notice);
		create(noticeRquest);

	}

	/**
	 * Create notice for Regularization
	 * 
	 * @param noticeRquest
	 * @return
	 */
	private Notice create(NoticeRequest noticeRquest) {
		RequestInfo requestInfo = noticeRquest.getRequestInfo();
		log.info("App Number for Regularization Revoke Notice: " + noticeRquest.getnotice().getBusinessid());
		enrichmentService.enrichScnCreateRequest(noticeRquest);
		validateCreateRequest(noticeRquest);
		repository.saveForRegularization(noticeRquest);
		return noticeRquest.getnotice();
	}

	/**
	 * Put isAction Revoked flag in Regularization Additional details
	 * 
	 * @param regularizationRequest
	 */
	private void addAttributesInAdditionDetails(RegularizationRequest regularizationRequest) {
		Map<String, Object> additionalDetails = (Map) regularizationRequest.getRegularization().getAdditionalDetails();
		additionalDetails.put("isActionRevoked", true);
	}

	/**
	 * update notice after Show cause reply
	 * 
	 * @param request
	 * @return
	 */
	public Notice showCauseReply(@Valid NoticeRequest request) {
		repository.updateForRegularization(request);
		return request.getnotice();
	}

	/**
	 * Validate notice creation request
	 * 
	 * @param request
	 */
	private void validateCreateRequest(@Valid NoticeRequest request) {

		NoticeSearchCriteria criteria = new NoticeSearchCriteria();
		criteria.setLetterNo(request.getnotice().getLetterNo());
		List<Notice> noticeSearchResult = repository.getNoticeDataForRegularization(criteria);

		if (!CollectionUtils.isEmpty(noticeSearchResult)) {
			throw new CustomException("Create Error", "Failed to create found duplicate letter no.");

		}
	}

	/**
	 * Create notice in eg_bpa_regularization_notice table
	 * 
	 * @param noticeRequest
	 * @return
	 */
	public Notice createShowCauseNoticeWithInWF(NoticeRequest noticeRequest) {

		RequestInfo requestInfo = noticeRequest.getRequestInfo();
		log.info("App Number for Notice:" + noticeRequest.getnotice().getBusinessid());
		enrichmentService.enrichScnCreateRequest(noticeRequest);
		validateCreateRequest(noticeRequest);
		repository.saveForRegularization(noticeRequest);

		return noticeRequest.getnotice();

	}
}