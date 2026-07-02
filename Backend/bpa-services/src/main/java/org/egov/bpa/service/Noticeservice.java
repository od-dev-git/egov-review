package org.egov.bpa.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.validation.Valid;

import org.egov.bpa.repository.BPARepository;
import org.egov.bpa.repository.ScnRepository;
import org.egov.bpa.util.BPAConstants;
import org.egov.bpa.util.BPAErrorConstants;
import org.egov.bpa.web.model.BPA;
import org.egov.bpa.web.model.BPARequest;
import org.egov.bpa.web.model.BPASearchCriteria;
import org.egov.bpa.web.model.Notice;
import org.egov.bpa.web.model.NoticeRequest;
import org.egov.bpa.web.model.NoticeSearchCriteria;
import org.egov.bpa.workflow.WorkflowIntegrator;
import org.egov.common.contract.request.RequestInfo;
import org.egov.tracer.model.CustomException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class Noticeservice {

	@Autowired
	private ScnRepository repository;

	@Autowired
	private BPARepository bpaRepository;

	@Autowired
	private WorkflowIntegrator wfIntegrator;

	@Autowired
	private EnrichmentService enrichmentService;

	@Autowired
	private BPAService bpaService;

	public Notice create(@Valid NoticeRequest request) {

		RequestInfo requestInfo = request.getRequestInfo();
		log.info("aopp:" + request.getnotice().getBusinessid());
		enrichmentService.enrichScnCreateRequestV2(request);
		ValidateCreateRequest(request);
//		if (!request.getnotice().getLetterType().equalsIgnoreCase(BPAConstants.LETTERTYPERVK)) {
//			wfIntegrator.callWorkFlowfornotice(request);
//		}
		repository.save(request);

		return request.getnotice();

	}

	private void ValidateCreateRequest(@Valid NoticeRequest request) {

		NoticeSearchCriteria criteria = new NoticeSearchCriteria();
		// criteria.setBusinessid(request.getnotice().getBusinessid());
		criteria.setLetterNo(request.getnotice().getLetterNo());
		List<Notice> noticeSearchResult = repository.getNoticeData(criteria);

		if (!CollectionUtils.isEmpty(noticeSearchResult)) {
			throw new CustomException("Create Error", "Failed to create found duplicate letter no.");

		}

//		if (!CollectionUtils.isEmpty(noticeSearchResult)) {
//			throw new CustomException("Create Error", "Failed to create found duplicate letter no.");
//	}
//		BPASearchCriteria bpacriteria = new BPASearchCriteria();
//		List<String> edcrNos = new ArrayList<>();
//		bpacriteria.setApplicationNo(request.getnotice().getBusinessid());
//		bpacriteria.setTenantId(request.getnotice().getTenantid());
//		List<BPA> bpa = bpaRepository.getBPAData(bpacriteria, edcrNos);
//		log.info("bpa dsc  doctype:" + bpa.get(0).getDscDetails().get(0).getDocumentType());
//		//log.info();
//		
//		if (!bpa.get(0).getStatus().equalsIgnoreCase(BPAConstants.APPROVED_STATE)
//				|| (bpa.get(0).getDscDetails() != null && bpa.get(0).getDscDetails().get(0).getDocumentId() == null)) {
//			throw new CustomException("Create Error", "Failed to create notice bpa application still in progress!");
//		}
		// log.info("bpa object:" + bpa);
	}

	public List<Notice> searchNotice(@Valid NoticeSearchCriteria criteria) {
		// TODO Auto-generated method stub
		Map<String, String> errorMap = new HashMap<>();
//		if (criteria.getBusinessid() == null) {
//			errorMap.put("SearchError", "please provide bussiness id to search a  notice.");
//		}
		if (!errorMap.isEmpty())
			throw new CustomException(errorMap);
		else {
			List<Notice> noticeSearchResult = repository.getNoticeData(criteria);
			// System.out.println("result:"+noticeSearchResult);
			if (noticeSearchResult.isEmpty()) {
				return Collections.emptyList();
			} else {
				return noticeSearchResult;

			}
		}
	}

	/**
	 * Updates an existing notice with workflow transition.
	 * 
	 * <p>
	 * This method orchestrates the complete notice update lifecycle:
	 * <ul>
	 * <li>Validates notice exists and is unique</li>
	 * <li>Enriches notice with audit details</li>
	 * <li>Executes workflow transition</li>
	 * <li>Handles special REVOKE action logic (BPA status update, revoke notice
	 * creation)</li>
	 * <li>Persists updated notice</li>
	 * </ul>
	 * 
	 * <p>
	 * <strong>Business Logic:</strong>
	 * <ul>
	 * <li><strong>REVOKE Action:</strong> When a notice is revoked:
	 * <ul>
	 * <li>Updates associated BPA application with revocation flag</li>
	 * <li>Creates a new revoke notice from additional details</li>
	 * <li>Marks BPA as "isActionRevoked" for tracking</li>
	 * </ul>
	 * </li>
	 * <li><strong>Workflow Integration:</strong> All updates go through workflow
	 * service</li>
	 * <li><strong>Validation:</strong> Ensures exactly one matching notice
	 * exists</li>
	 * </ul>
	 * 
	 * @param request the notice update request
	 * @return the updated notice
	 * @throws CustomException if notice not found, multiple notices found, or
	 *                         validation fails
	 */
	public Notice update(@Valid NoticeRequest request) {

		Notice notice = request.getnotice();

		log.info("Updating notice with letter number: {}", notice.getLetterNo());

		// Step 1: Validate notice ID exists
		validateNoticeId(notice);

		// Step 2: Retrieve and validate existing notice
		Notice existingNotice = retrieveAndValidateExistingNotice(notice);

		// Step 3: Enrich and execute workflow
		enrichmentService.enrichScnUpdateRequestV2(request);
		wfIntegrator.callWorkFlowfornotice(request);

		// Step 4: Handle REVOKE action if applicable
		if (isRevokeAction(notice)) {
			handleRevokeAction(request, existingNotice);
		}

		// Step 5: Persist updated notice
		repository.update(request);

		log.info("Notice update completed successfully for letter number: {}", notice.getLetterNo());

		return request.getnotice();
	}

	/**
	 * Validates that the notice ID is present.
	 * 
	 * @param notice the notice to validate
	 * @throws CustomException if notice ID is null
	 */
	private void validateNoticeId(Notice notice) {
		if (notice.getId() == null) {
			throw new CustomException("UPDATE_ERROR", "Failed to update notice: notice ID is required");
		}
	}

	/**
	 * Retrieves and validates existing notice from database.
	 * 
	 * <p>
	 * <strong>Validation Rules:</strong>
	 * <ul>
	 * <li>Exactly one notice must exist with the given letter number</li>
	 * <li>Zero results = notice doesn't exist</li>
	 * <li>Multiple results = data integrity issue</li>
	 * </ul>
	 * 
	 * @param notice the notice with search criteria
	 * @return the existing notice from database
	 * @throws CustomException if no notice found or multiple notices found
	 */
	private Notice retrieveAndValidateExistingNotice(Notice notice) {
		NoticeSearchCriteria criteria = new NoticeSearchCriteria();
		criteria.setLetterNo(notice.getLetterNo());
		criteria.setTenantid(notice.getTenantid());

		List<Notice> noticeSearchResult = repository.getNoticeData(criteria);

		if (CollectionUtils.isEmpty(noticeSearchResult)) {
			throw new CustomException(BPAErrorConstants.UPDATE_ERROR,
					String.format("Failed to update: No notice found with letter number %s", notice.getLetterNo()));
		}

		if (noticeSearchResult.size() > 1) {
			throw new CustomException(BPAErrorConstants.UPDATE_ERROR, String.format(
					"Failed to update: Multiple notices found with letter number %s. " + "Data integrity issue.",
					notice.getLetterNo()));
		}

		return noticeSearchResult.get(0);
	}

	/**
	 * Determines if the workflow action is REVOKE.
	 * 
	 * @param notice the notice with workflow action
	 * @return true if action is REVOKE, false otherwise
	 */
	private boolean isRevokeAction(Notice notice) {
		return notice.getWorkflow() != null && BPAConstants.REVOKE.equalsIgnoreCase(notice.getWorkflow().getAction());
	}

	/**
	 * Handles the complete REVOKE action workflow.
	 * 
	 * <p>
	 * This method:
	 * <ul>
	 * <li>Retrieves associated BPA application</li>
	 * <li>Updates BPA with revocation flag</li>
	 * <li>Creates new revoke notice if additional details provided</li>
	 * </ul>
	 * 
	 * @param request        the notice request
	 * @param existingNotice the existing notice with business context
	 */
	private void handleRevokeAction(NoticeRequest request, Notice existingNotice) {
		log.info("Processing REVOKE action for notice: {}", request.getnotice().getLetterNo());

		// Retrieve and update BPA application
		BPA bpa = retrieveBpaForRevoke(existingNotice);
		updateBpaWithRevocationFlag(bpa, request.getRequestInfo());

		// Create revoke notice if additional details present
		Object additionalDetails = request.getnotice().getAdditionalDetails();
		if (Objects.nonNull(additionalDetails)) {
			createRevokeNotice(additionalDetails, request.getRequestInfo());
		}
	}

	/**
	 * Retrieves BPA application for revoke action.
	 * 
	 * @param notice the notice containing business ID (BPA application number)
	 * @return the BPA application
	 */
	private BPA retrieveBpaForRevoke(Notice notice) {
		BPASearchCriteria bpaCriteria = new BPASearchCriteria();
		bpaCriteria.setApplicationNo(notice.getBusinessid());
		bpaCriteria.setTenantId(notice.getTenantid());

		List<BPA> bpaList = bpaRepository.getBPAData(bpaCriteria, new ArrayList<>());

		if (CollectionUtils.isEmpty(bpaList)) {
			throw new CustomException(BPAErrorConstants.UPDATE_ERROR,
					String.format("BPA application not found for business ID: %s", notice.getBusinessid()));
		}

		return bpaList.get(0);
	}

	/**
	 * Updates BPA application with revocation flag.
	 * 
	 * <p>
	 * <strong>Business Logic:</strong> Marks the BPA application as revoked by
	 * setting "isActionRevoked" flag in additional details. This allows tracking
	 * which applications had notices that were subsequently revoked.
	 * 
	 * @param bpa         the BPA application to update
	 * @param requestInfo the request information
	 */
	@SuppressWarnings("unchecked")
	private void updateBpaWithRevocationFlag(BPA bpa, RequestInfo requestInfo) {
		// Add revocation flag to additional details
		Map<String, Object> additionalDetails = (Map<String, Object>) bpa.getAdditionalDetails();
		if (additionalDetails == null) {
			additionalDetails = new HashMap<>();
			bpa.setAdditionalDetails(additionalDetails);
		}
		additionalDetails.put("isActionRevoked", true);

		// Update BPA application
		BPARequest bpaRequest = new BPARequest();
		bpaRequest.setRequestInfo(requestInfo);
		bpaRequest.setBPA(bpa);

		bpaService.update(bpaRequest);

		log.info("BPA application marked as revoked: {}", bpa.getApplicationNo());
	}

	/**
	 * Creates a new revoke notice from additional details.
	 * 
	 * <p>
	 * When a notice is revoked, a new revoke notice document is created containing
	 * the revocation details. The additional details map contains all required
	 * fields for the revoke notice.
	 * 
	 * @param additionalDetails the additional details containing revoke notice
	 *                          information
	 * @param requestInfo       the request information
	 */
	@SuppressWarnings("unchecked")
	private void createRevokeNotice(Object additionalDetails, RequestInfo requestInfo) {
		Map<String, String> applicationDetails = (Map<String, String>) additionalDetails;

		// Build revoke notice from additional details
		Notice revokeNotice = Notice.builder().businessid(applicationDetails.get(BPAConstants.BUSINESSIDS))
				.filestoreid(applicationDetails.get(BPAConstants.FILESTOREIDS))
				.LetterNo(applicationDetails.get(BPAConstants.LETTERNO))
				.letterType(applicationDetails.get(BPAConstants.LETTERTYPE))
				.tenantid(applicationDetails.get(BPAConstants.TENANTIDS)).build();

		// Create revoke notice
		NoticeRequest revokeNoticeRequest = new NoticeRequest();
		revokeNoticeRequest.setRequestInfo(requestInfo);
		revokeNoticeRequest.notice(revokeNotice);

		create(revokeNoticeRequest);

		log.info("Revoke notice created successfully for business ID: {}", revokeNotice.getBusinessid());
	}

	public Notice showCauseReply(@Valid NoticeRequest request) {
		// this.updateAdditionalDetails(request);
		repository.update(request);
		return request.getnotice();
	}

	public NoticeRequest updateAdditionalDetails(NoticeRequest noticeRequest) {

		if (noticeRequest == null || noticeRequest.getnotice() == null || noticeRequest.getnotice().getId() == null) {
			throw new IllegalArgumentException("Invalid input parameters");
		}

		Map<String, Object> additionalDetails = (Map<String, Object>) noticeRequest.getnotice().getAdditionalDetails();
		if (additionalDetails != null) {
			for (Map.Entry<String, Object> entry : additionalDetails.entrySet()) {
				String key = entry.getKey();
				Object value = entry.getValue();
				additionalDetails.put(key, value);
			}
		}
		noticeRequest.getnotice().setAdditionalDetails(additionalDetails);
		return noticeRequest;
	}

	public Notice createShowCauseNoticeWithInWF(@Valid NoticeRequest request) {

		RequestInfo requestInfo = request.getRequestInfo();
		log.info("aopp:" + request.getnotice().getBusinessid());
		enrichmentService.enrichScnCreateRequestV2(request);
		ValidateCreateRequest(request);
		repository.save(request);

		return request.getnotice();

	}
}
