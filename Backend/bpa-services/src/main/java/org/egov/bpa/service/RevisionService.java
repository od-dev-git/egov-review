package org.egov.bpa.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import org.egov.bpa.repository.RevisionRepository;
import org.egov.bpa.util.BPAConstants;
import org.egov.bpa.util.BPAErrorConstants;
import org.egov.bpa.web.model.Revision;
import org.egov.bpa.web.model.RevisionRequest;
import org.egov.bpa.web.model.RevisionSearchCriteria;
import org.egov.common.contract.request.RequestInfo;
import org.egov.tracer.model.CustomException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class RevisionService {

	@Autowired
	private RevisionRepository repository;

	@Autowired
	EnrichmentService enrichmentService;

	/**
	 * does all the validations required to create BPA Record in the system
	 * 
	 * @param bpaRequest
	 * @return
	 */
	public Revision create(RevisionRequest revisionRequest) {
		RequestInfo requestInfo = revisionRequest.getRequestInfo();
		validateRevisionAlreadyExists(revisionRequest);
		enrichmentService.enrichRevisionCreateRequest(revisionRequest);
		// Object mdmsData = util.mDMSCall(requestInfo, tenantId);
		// TODO validations
		repository.save(revisionRequest);
		return revisionRequest.getRevision();
	}

	/**
	 * Validates that no revision request already exists for the given application.
	 * 
	 * <p>
	 * This method performs three validation checks:
	 * <ol>
	 * <li>Checks if revision exists for the BPA application number</li>
	 * <li>Checks if revision exists for the reference BPA application number
	 * (excluding rejected applications)</li>
	 * <li>Checks if revision exists for the reference permit number (only for
	 * non-SUJOG applications)</li>
	 * </ol>
	 * </p>
	 * 
	 * @param revisionRequest The revision request to validate
	 * @throws CustomException if revision already exists for any of the identifiers
	 */
	private void validateRevisionAlreadyExists(RevisionRequest revisionRequest) {

		Revision revision = revisionRequest.getRevision();

		// Validation 1: Check by BPA Application Number
		validateByBpaApplicationNo(revision.getBpaApplicationNo());

		// Validation 2: Check by Reference BPA Application Number (excluding rejected)
		validateByRefBpaApplicationNo(revision.getRefBpaApplicationNo());

		// Validation 3: Check by Reference Permit Number (for non-SUJOG only)
		validateByRefPermitNo(revision.getRefPermitNo(), revision.isSujogExistingApplication());
	}

	/**
	 * Validates that no revision exists for the given BPA application number.
	 * 
	 * @param bpaApplicationNo The BPA application number to check
	 * @throws CustomException if revision already exists
	 */
	private void validateByBpaApplicationNo(String bpaApplicationNo) {

		if (StringUtils.isEmpty(bpaApplicationNo)) {
			return;
		}

		RevisionSearchCriteria searchCriteria = RevisionSearchCriteria.builder().bpaApplicationNo(bpaApplicationNo)
				.build();

		List<Revision> existingRevisions = repository.getRevisionData(searchCriteria);

		if (Objects.nonNull(existingRevisions) && !existingRevisions.isEmpty()) {
			throw new CustomException(
					"Found already existing revision data for given bpaApplicationNo:" + bpaApplicationNo,
					"Found already existing revision data for given bpaApplicationNo:" + bpaApplicationNo);
		}
	}

	/**
	 * Validates that no active revision exists for the given reference BPA
	 * application number.
	 * 
	 * <p>
	 * This validation excludes rejected applications - if all existing revisions
	 * for the reference application number are rejected, a new revision can be
	 * created.
	 * </p>
	 * 
	 * @param refBpaApplicationNo The reference BPA application number to check
	 * @throws CustomException if active (non-rejected) revision already exists
	 */
	private void validateByRefBpaApplicationNo(String refBpaApplicationNo) {

		if (StringUtils.isEmpty(refBpaApplicationNo)) {
			return;
		}

		RevisionSearchCriteria searchCriteria = RevisionSearchCriteria.builder()
				.refBpaApplicationNo(refBpaApplicationNo).build();

		List<Revision> existingRevisions = repository.getRevisionData(searchCriteria);

		if (CollectionUtils.isEmpty(existingRevisions)) {
			return;
		}

		// Filter out rejected applications
		List<Revision> activeRevisions = filterOutRejectedRevisions(existingRevisions);

		if (!activeRevisions.isEmpty()) {
			throw new CustomException(
					"Found already existing revision data for given RefBpaApplicationNo:" + refBpaApplicationNo,
					"Found already existing revision data for given RefBpaApplicationNo:" + refBpaApplicationNo);
		}
	}

	/**
	 * Filters out rejected revisions from the list.
	 * 
	 * <p>
	 * Checks the status of each revision's BPA application and removes those that
	 * are in REJECTED status.
	 * </p>
	 * 
	 * @param revisions The list of revisions to filter
	 * @return List of active (non-rejected) revisions
	 */
	private List<Revision> filterOutRejectedRevisions(List<Revision> revisions) {

		List<String> rejectedApplications = new ArrayList<>();

		for (Revision revision : revisions) {
			if (revision == null || revision.getBpaApplicationNo() == null) {
				log.info("Skipping null or invalid revision entry.");
				continue;
			}

			String status = repository.getRevisionApplicationStatus(revision.getBpaApplicationNo());
			log.info("ApplicationNo: {}, Status: {}", revision.getBpaApplicationNo(), status);

			if (BPAConstants.STATUS_REJECTED.equalsIgnoreCase(status)) {
				rejectedApplications.add(revision.getBpaApplicationNo());
			}
		}

		// Remove rejected applications from the list
		if (!rejectedApplications.isEmpty()) {
			log.info("Filtering out {} rejected applications.", rejectedApplications.size());
			revisions.removeIf(revision -> rejectedApplications.contains(revision.getBpaApplicationNo()));
		}

		return revisions;
	}

	/**
	 * Validates that no revision exists for the given reference permit number.
	 * 
	 * <p>
	 * This validation only applies to non-SUJOG applications
	 * (isSujogExistingApplication = false). For SUJOG applications, this validation
	 * is skipped.
	 * </p>
	 * 
	 * @param refPermitNo                The reference permit number to check
	 * @param isSujogExistingApplication Whether this is a SUJOG existing
	 *                                   application
	 * @throws CustomException if revision already exists for non-SUJOG application
	 */
	private void validateByRefPermitNo(String refPermitNo, boolean isSujogExistingApplication) {

		if (StringUtils.isEmpty(refPermitNo)) {
			return;
		}

		// Skip validation for SUJOG applications
		if (isSujogExistingApplication) {
			return;
		}

		RevisionSearchCriteria searchCriteria = RevisionSearchCriteria.builder().refPermitNo(refPermitNo).build();

		List<Revision> existingRevisions = repository.getRevisionData(searchCriteria);

		if (Objects.nonNull(existingRevisions) && !existingRevisions.isEmpty()) {
			throw new CustomException("APPLICATION_EXIST_ERROR",
					"In my applications, there might be an existing application with " + refPermitNo
							+ " permit number, Kindly delete that and create a new application.");
		}
	}

	/**
	 * Returns the revision with enriched owners from user service
	 * 
	 * @param criteria    The object containing the parameters on which to search
	 * @param requestInfo The search request's requestInfo
	 * @return List of revision for the given criteria
	 */
	public List<Revision> getRevisionFromCriteria(RevisionSearchCriteria criteria) {
		List<Revision> revisions = repository.getRevisionData(criteria);
		if (revisions.isEmpty())
			return Collections.emptyList();
		return revisions;
	}

	/**
	 * Updates an existing revision request in the system.
	 * 
	 * <p>
	 * This method orchestrates the update process:
	 * <ol>
	 * <li>Validates that the revision has a valid ID</li>
	 * <li>Fetches and validates the existing revision from database</li>
	 * <li>Validates that only the original creator can update the revision</li>
	 * <li>Preserves the original audit details (created by, created time)</li>
	 * <li>Enriches the request with update metadata</li>
	 * <li>Persists the updated revision via repository</li>
	 * </ol>
	 * </p>
	 * 
	 * @param revisionRequest The update request containing revision details
	 * @return The updated revision entity
	 * @throws CustomException if revision ID is missing, not found, or unauthorized
	 *                         update
	 */
	@SuppressWarnings("unchecked")
	public Revision update(RevisionRequest revisionRequest) {

		RequestInfo requestInfo = revisionRequest.getRequestInfo();
		Revision revision = revisionRequest.getRevision();

		// Step 1: Validate revision ID is present
		validateRevisionIdPresent(revision);

		// Step 2: Fetch and validate existing revision from database
		Revision existingRevision = fetchAndValidateExistingRevision(revisionRequest);

		// Step 3: Validate only the creator can update the revision
		validateCreatorAuthorization(existingRevision, requestInfo);

		// Step 4: Preserve original audit details (created by, created time)
		revision.setAuditDetails(existingRevision.getAuditDetails());

		// Step 5: Enrich request with update metadata
		enrichmentService.enrichRevisionUpdateRequest(revisionRequest);

		// Step 6: Persist update to database
		repository.update(revisionRequest);

		return revisionRequest.getRevision();
	}

	/**
	 * Validates that the revision has a valid ID for update operation.
	 * 
	 * @param revision The revision entity to validate
	 * @throws CustomException if revision ID is null
	 */
	private void validateRevisionIdPresent(Revision revision) {

		if (revision.getId() == null) {
			throw new CustomException(BPAErrorConstants.UPDATE_ERROR, "Revision not found in the System" + revision);
		}
	}

	/**
	 * Fetches and validates the existing revision from the database.
	 * 
	 * <p>
	 * Ensures that exactly one revision exists with the given ID and tenant ID.
	 * </p>
	 * 
	 * @param revisionRequest The request containing revision with ID to search
	 * @return The existing revision from database
	 * @throws CustomException if no revision found or multiple revisions found
	 */
	private Revision fetchAndValidateExistingRevision(RevisionRequest revisionRequest) {

		List<Revision> searchResult = getRevisionsWithId(revisionRequest);

		if (CollectionUtils.isEmpty(searchResult) || searchResult.size() > 1) {
			throw new CustomException(BPAErrorConstants.UPDATE_ERROR,
					"Failed to Update the Application, Found None or multiple revision application!");
		}

		return searchResult.get(0);
	}

	/**
	 * Validates that only the original creator can update the revision.
	 * 
	 * <p>
	 * Compares the creator's UUID from audit details with the current user's UUID
	 * to ensure only authorized updates are allowed.
	 * </p>
	 * 
	 * @param existingRevision The existing revision from database
	 * @param requestInfo      The request info containing current user details
	 * @throws CustomException if current user is not the original creator
	 */
	private void validateCreatorAuthorization(Revision existingRevision, RequestInfo requestInfo) {

		String createdBy = existingRevision.getAuditDetails().getCreatedBy();
		String modifiedBy = requestInfo.getUserInfo().getUuid();

		if (!createdBy.equals(modifiedBy)) {
			log.error("Only creator can update the revision. Created by user: {}", createdBy);
			throw new CustomException(BPAErrorConstants.UPDATE_ERROR,
					"Failed to Update the Application, Only creator could update the revision");
		}
	}

	/**
	 * Returns Revision from db for the update request
	 * 
	 * @param request The update request
	 * @return List of Revision
	 */
	public List<Revision> getRevisionsWithId(RevisionRequest request) {
		RevisionSearchCriteria criteria = new RevisionSearchCriteria();
		List<String> ids = new LinkedList<>();
		ids.add(request.getRevision().getId());
		criteria.setTenantId(request.getRevision().getTenantId());
		criteria.setIds(ids);
		List<Revision> revisions = repository.getRevisionData(criteria);
		return revisions;
	}

}
