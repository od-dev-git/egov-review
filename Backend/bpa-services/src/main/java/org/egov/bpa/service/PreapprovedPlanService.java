package org.egov.bpa.service;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.egov.bpa.repository.PreapprovedPlanRepository;
import org.egov.bpa.util.BPAErrorConstants;
import org.egov.bpa.web.model.PreapprovedPlan;
import org.egov.bpa.web.model.PreapprovedPlanRequest;
import org.egov.bpa.web.model.PreapprovedPlanSearchCriteria;
import org.egov.common.contract.request.RequestInfo;
import org.egov.tracer.model.CustomException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class PreapprovedPlanService {

	@Autowired
	private PreapprovedPlanRepository repository;

	@Autowired
	EnrichmentService enrichmentService;

	/**
	 * Creates a new preapproved plan.
	 *
	 * <p>
	 * Flow:
	 * <ol>
	 * <li>Validate required data in the request.</li>
	 * <li>Enrich the request for persistence.</li>
	 * <li>Persist and return the created plan.</li>
	 * </ol>
	 */
	public PreapprovedPlan create(PreapprovedPlanRequest preapprovedPlanRequest) {
		validateCreateRequest(preapprovedPlanRequest);
		enrichCreateRequest(preapprovedPlanRequest);
		persistCreate(preapprovedPlanRequest);
		return preapprovedPlanRequest.getPreapprovedPlan();
	}

	/**
	 * Basic guard checks for create requests.
	 */
	private void validateCreateRequest(PreapprovedPlanRequest preapprovedPlanRequest) {
		if (preapprovedPlanRequest == null || preapprovedPlanRequest.getPreapprovedPlan() == null) {
			throw new CustomException(BPAErrorConstants.UPDATE_ERROR,
					"Preapproved plan request is missing required data.");
		}
	}

	/**
	 * Applies enrichment for create before persisting.
	 */
	private void enrichCreateRequest(PreapprovedPlanRequest preapprovedPlanRequest) {
		RequestInfo requestInfo = preapprovedPlanRequest.getRequestInfo();
		// String tenantId =
		// preapprovedPlanRequest.getPreapprovedPlan().getTenantId().split("\\.")[0];
		enrichmentService.enrichPreapprovedPlanCreateRequestV2(preapprovedPlanRequest);
		// Object mdmsData = util.mDMSCall(requestInfo, tenantId);
		// TODO validations
	}

	/**
	 * Persists the preapproved plan create request.
	 */
	private void persistCreate(PreapprovedPlanRequest preapprovedPlanRequest) {
		repository.save(preapprovedPlanRequest);
	}

	/**
	 * Returns the bpa with enriched owners from user service
	 * 
	 * @param criteria    The object containing the parameters on which to search
	 * @param requestInfo The search request's requestInfo
	 * @return List of bpa for the given criteria
	 */
	public List<PreapprovedPlan> getPreapprovedPlanFromCriteria(PreapprovedPlanSearchCriteria criteria) {
		List<PreapprovedPlan> preapprovedPlans = repository.getPreapprovedPlansData(criteria);
		if (preapprovedPlans.isEmpty())
			return Collections.emptyList();
		return preapprovedPlans;
	}

	/**
	 * Updates an existing preapproved plan.
	 *
	 * <p>
	 * Flow:
	 * <ol>
	 * <li>Validate the request has a persisted plan id.</li>
	 * <li>Fetch the existing plan by id and ensure exactly one match.</li>
	 * <li>Preserve audit details, enrich request, and persist the update.</li>
	 * </ol>
	 */
	@SuppressWarnings("unchecked")
	public PreapprovedPlan update(PreapprovedPlanRequest preapprovedPlanRequest) {
		PreapprovedPlan preapprovedPlan = preapprovedPlanRequest.getPreapprovedPlan();

		validatePreapprovedPlanId(preapprovedPlan);
		PreapprovedPlan existingPlan = fetchSinglePlanForUpdate(preapprovedPlanRequest);

		// Keep original audit details before applying update enrichment.
		preapprovedPlan.setAuditDetails(existingPlan.getAuditDetails());
		enrichAndPersistUpdate(preapprovedPlanRequest);

		return preapprovedPlanRequest.getPreapprovedPlan();
	}

	/**
	 * Ensures the update request points to an existing preapproved plan.
	 */
	private void validatePreapprovedPlanId(PreapprovedPlan preapprovedPlan) {
		if (preapprovedPlan.getId() == null) {
			throw new CustomException(BPAErrorConstants.UPDATE_ERROR,
					"Preapproved plan not found in the System" + preapprovedPlan);
		}
	}

	/**
	 * Loads the existing plan for update and guarantees a single match.
	 */
	private PreapprovedPlan fetchSinglePlanForUpdate(PreapprovedPlanRequest preapprovedPlanRequest) {
		List<PreapprovedPlan> searchResult = getPreapprovedPlansWithId(preapprovedPlanRequest);
		if (CollectionUtils.isEmpty(searchResult) || searchResult.size() > 1) {
			throw new CustomException(BPAErrorConstants.UPDATE_ERROR,
					"Failed to Update the Application, Found None or multiple Preapproved plans!");
		}
		return searchResult.get(0);
	}

	/**
	 * Enriches the update request and persists it.
	 */
	private void enrichAndPersistUpdate(PreapprovedPlanRequest preapprovedPlanRequest) {
		// TODO : validations if any
		enrichmentService.enrichPreapprovedPlanUpdateRequest(preapprovedPlanRequest);
		repository.update(preapprovedPlanRequest);
	}

	/**
	 * Returns preapprovedPlans from db for the update request
	 * 
	 * @param request The update request
	 * @return List of preapprovedPlans
	 */
	public List<PreapprovedPlan> getPreapprovedPlansWithId(PreapprovedPlanRequest request) {
		PreapprovedPlanSearchCriteria criteria = new PreapprovedPlanSearchCriteria();
		List<String> ids = new LinkedList<>();
		ids.add(request.getPreapprovedPlan().getId());
		criteria.setTenantId(request.getPreapprovedPlan().getTenantId());
		criteria.setIds(ids);
		List<PreapprovedPlan> preapprovedPlans = repository.getPreapprovedPlansData(criteria);
		return preapprovedPlans;
	}

}