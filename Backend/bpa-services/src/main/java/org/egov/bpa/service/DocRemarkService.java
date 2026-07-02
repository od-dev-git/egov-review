package org.egov.bpa.service;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.validation.Valid;

import org.egov.bpa.repository.DocRemarkRepository;
import org.egov.bpa.web.model.DocRemark;
import org.egov.bpa.web.model.DocRemarkRequest;
import org.egov.bpa.web.model.DocRemarkSearchCriteria;
import org.egov.tracer.model.CustomException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class DocRemarkService {

	@Autowired
	private DocRemarkRepository repository;

	@Autowired
	private EnrichmentService enrichmentService;

	/**
	 * Creates a new document remark for a BPA application.
	 * 
	 * <p>
	 * <strong>Business Logic:</strong> Document remarks allow officials to add
	 * comments and request clarifications on specific documents submitted by
	 * citizens. Each document can have only one remark record with multiple
	 * comments.
	 * 
	 * @param request the document remark request
	 * @return the created document remark
	 * @throws CustomException if duplicate businessId and documentCode combination
	 *                         exists
	 */
	public DocRemark create(@Valid DocRemarkRequest request) {

		log.info("Creating document remark for businessId: {}, documentCode: {}",
				request.getDocRemark().getBusinessId(), request.getDocRemark().getDocumentCode());

		validateCreateRequest(request);
		enrichmentService.enrichDocRemarkCreateRequest(request);
		repository.save(request);

		return request.getDocRemark();
	}

	/**
	 * Validates that no duplicate document remark exists.
	 * 
	 * <p>
	 * <strong>Business Rule:</strong> Only one remark record is allowed per
	 * combination of businessId (application number) and documentCode. Multiple
	 * comments are stored within the same remark's additional details.
	 * 
	 * @param request the document remark request to validate
	 * @throws CustomException if duplicate found
	 */
	private void validateCreateRequest(@Valid DocRemarkRequest request) {

		DocRemarkSearchCriteria criteria = new DocRemarkSearchCriteria();
		criteria.setBusinessId(request.getDocRemark().getBusinessId());

		String documentCode = request.getDocRemark().getDocumentCode();
		List<String> docCodes = new LinkedList<>();
		docCodes.add(documentCode);
		criteria.setDocumentCode(docCodes);

		List<DocRemark> docRemarkSearchResult = repository.getDocRemark(criteria);

		if (!CollectionUtils.isEmpty(docRemarkSearchResult)) {
			throw new CustomException("DUPLICATE_DOC_REMARK",
					String.format("Document remark already exists for businessId: %s and documentCode: %s",
							request.getDocRemark().getBusinessId(), documentCode));
		}
	}

	/**
	 * Searches for document remarks based on provided criteria.
	 * 
	 * <p>
	 * This method retrieves document remarks and enriches them with comment counts:
	 * <ul>
	 * <li>Fetches remarks matching the search criteria</li>
	 * <li>Calculates the number of comments for each remark</li>
	 * <li>Returns empty list if no remarks found</li>
	 * </ul>
	 * 
	 * <p>
	 * <strong>Business Logic:</strong> Comment count helps UI display the number of
	 * clarifications/comments without loading full comment history.
	 * 
	 * @param criteria the search criteria
	 * @return list of document remarks with comment counts (empty if none found)
	 * @throws CustomException if validation errors exist in criteria
	 */
	public List<DocRemark> search(@Valid DocRemarkSearchCriteria criteria) {

		// Step 1: Validate search criteria
		validateSearchCriteria();

		// Step 2: Execute search
		List<DocRemark> docRemarks = repository.getDocRemark(criteria);

		// Step 3: Enrich with comment counts and return
		return enrichWithCommentCounts(docRemarks);
	}

	/**
	 * Validates the search criteria.
	 * 
	 * <p>
	 * Currently uses an error map pattern for extensibility. Additional validations
	 * can be added to the error map as needed.
	 * 
	 * @throws CustomException if validation errors exist
	 */
	private void validateSearchCriteria() {
		Map<String, String> errorMap = new HashMap<>();

		// Add validation logic here as needed
		// Example: if (criteria.getBusinessId() == null)
		// errorMap.put("BUSINESS_ID_REQUIRED", "Business ID is required");

		if (!errorMap.isEmpty()) {
			throw new CustomException(errorMap);
		}
	}

	/**
	 * Enriches document remarks with comment counts from additional details.
	 * 
	 * <p>
	 * This method:
	 * <ul>
	 * <li>Extracts comments list from additional details JSON</li>
	 * <li>Counts the number of comments</li>
	 * <li>Sets the count on the remark object for UI display</li>
	 * </ul>
	 * 
	 * @param docRemarks the list of document remarks to enrich
	 * @return the enriched list (empty list if input is empty)
	 */
	private List<DocRemark> enrichWithCommentCounts(List<DocRemark> docRemarks) {
		if (CollectionUtils.isEmpty(docRemarks)) {
			return Collections.emptyList();
		}

		docRemarks.forEach(docRemark -> {
			int commentCount = extractCommentCount(docRemark);
			docRemark.setDocCommentCount(commentCount);
		});

		return docRemarks;
	}

	/**
	 * Extracts comment count from a document remark's additional details.
	 * 
	 * <p>
	 * <strong>Data Structure:</strong> Comments are stored in additional details
	 * as:
	 * 
	 * <pre>
	 * {
	 *   "comment": [
	 *     { "text": "Comment 1", "user": "John" },
	 *     { "text": "Comment 2", "user": "Jane" }
	 *   ]
	 * }
	 * </pre>
	 * 
	 * @param docRemark the document remark
	 * @return the number of comments (0 if none exist)
	 */
	private int extractCommentCount(DocRemark docRemark) {
		if (ObjectUtils.isEmpty(docRemark.getAdditionalDetails())) {
			return 0;
		}

		Map<String, Object> additionalDetails = (Map<String, Object>) docRemark.getAdditionalDetails();
		List<Object> commentList = (List<Object>) additionalDetails.get("comment");

		if (CollectionUtils.isEmpty(commentList)) {
			return 0;
		}

		return commentList.size();
	}

	/**
	 * Updates an existing document remark.
	 * 
	 * <p>
	 * <strong>Business Logic:</strong> Updates allow adding new comments to the
	 * remark's additional details or modifying existing remark metadata.
	 * 
	 * @param request the document remark update request
	 * @return the updated document remark
	 */
	public DocRemark update(@Valid DocRemarkRequest request) {
		updateAdditionalDetails(request);
		enrichmentService.enrichDocRemarkUpdateRequest(request);
		repository.update(request);

		return request.getDocRemark();
	}

	/**
	 * Updates additional details for a document remark.
	 * 
	 * <p>
	 * This method processes the additional details map to ensure all entries are
	 * properly set before persistence.
	 * 
	 * <p>
	 * <strong>Note:</strong> Currently performs a pass-through operation. This
	 * method serves as a placeholder for future additional details transformation
	 * logic (e.g., sanitization, validation, enrichment).
	 * 
	 * @param request the document remark request
	 * @return the request with updated additional details
	 * @throws IllegalArgumentException if request parameters are invalid
	 */
	private DocRemarkRequest updateAdditionalDetails(DocRemarkRequest request) {

		if (request == null || request.getDocRemark() == null || request.getDocRemark().getId() == null) {
			throw new IllegalArgumentException("Invalid input parameters: request, docRemark, and id cannot be null");
		}

		Map<String, Object> additionalDetails = (Map<String, Object>) request.getDocRemark().getAdditionalDetails();

		if (additionalDetails != null) {
			// Process each entry in additional details
			// Currently a pass-through, but allows for future transformation logic
			for (Map.Entry<String, Object> entry : additionalDetails.entrySet()) {
				String key = entry.getKey();
				Object value = entry.getValue();
				additionalDetails.put(key, value);
			}
		}

		request.getDocRemark().setAdditionalDetails(additionalDetails);
		return request;
	}

}
