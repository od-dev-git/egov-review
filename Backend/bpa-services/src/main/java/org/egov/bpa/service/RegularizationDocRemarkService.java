package org.egov.bpa.service;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.validation.Valid;

import org.egov.bpa.repository.RegularizationDocRemarkRepository;
import org.egov.bpa.web.model.regularization.RegularizationDocRemark;
import org.egov.bpa.web.model.regularization.RegularizationDocRemarkRequest;
import org.egov.bpa.web.model.regularization.RegularizationDocRemarkSearchCriteria;
import org.egov.tracer.model.CustomException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class RegularizationDocRemarkService {

	@Autowired
	private RegularizationDocRemarkRepository docRemarkRepository;

	@Autowired
	private RegularizationEnrichmentService enrichmentService;

	/**
	 * Service layer for create Regularization Doc Remark
	 * 
	 * @param request
	 * @return RegularizationDocRemark
	 */
	public RegularizationDocRemark create(@Valid RegularizationDocRemarkRequest request) {
		log.info("businessId:" + request.getDocRemark().getBusinessId());
		validateCreateRequest(request);
		enrichmentService.enrichDocRemarkCreateRequest(request);
		docRemarkRepository.save(request);
		return request.getDocRemark();
	}

	/**
	 * Validate Create Request for DocRemark
	 */
	private void validateCreateRequest(@Valid RegularizationDocRemarkRequest request) {
		RegularizationDocRemarkSearchCriteria criteria = RegularizationDocRemarkSearchCriteria.builder()
				.businessId(request.getDocRemark().getBusinessId())
				.documentCode(Collections.singletonList(request.getDocRemark().getDocumentCode())).build();
		List<RegularizationDocRemark> docRemarkSearchResult = docRemarkRepository.getRegularizationDocRemark(criteria);

		if (!CollectionUtils.isEmpty(docRemarkSearchResult)) {
			throw new CustomException("Create Error", "Failed to create found duplicate businessId and documentCode.");
		}
	}

	/**
	 * Service layer for searching regularization Doc Remark
	 * 
	 * @param searchCriteria
	 * @return List<RegularizationDocRemark>
	 */
	public List<RegularizationDocRemark> search(@Valid RegularizationDocRemarkSearchCriteria searchCriteria) {
		log.info("search: " + searchCriteria.toString());
		List<RegularizationDocRemark> docRemarkList = docRemarkRepository.getRegularizationDocRemark(searchCriteria);
		return docRemarkList;
	}

	/**
	 * Service layer for update regularization Doc Remark
	 * 
	 * @param request
	 * @return RegularizationDocRemark
	 */
	public RegularizationDocRemark update(@Valid RegularizationDocRemarkRequest request) {
		this.updateAdditionalDetails(request);
		enrichmentService.enrichDocRemarkUpdateRequest(request);
		docRemarkRepository.update(request);
		return request.getDocRemark();
	}

	@SuppressWarnings("unchecked")
	public RegularizationDocRemarkRequest updateAdditionalDetails(RegularizationDocRemarkRequest request) {

		if (request == null || request.getDocRemark() == null || request.getDocRemark().getId() == null) {
			throw new IllegalArgumentException("Invalid input parameters");
		}

		Map<String, Object> additionalDetails = (Map<String, Object>) request.getDocRemark().getAdditionalDetails();
		if (additionalDetails != null) {
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
