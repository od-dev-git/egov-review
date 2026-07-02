package org.egov.bpa.web.controller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.validation.Valid;

import org.egov.bpa.service.DemolitionService;
import org.egov.bpa.util.ResponseInfoFactory;
import org.egov.bpa.web.model.BPAVillage;
import org.egov.bpa.web.model.BPAVillageResponse;
import org.egov.bpa.web.model.RequestInfoWrapper;
import org.egov.bpa.web.model.VillageSearchCriteria;
import org.egov.bpa.web.model.demolition.Demolition;
import org.egov.bpa.web.model.demolition.DemolitionApprovedByApplicationResponse;
import org.egov.bpa.web.model.demolition.DemolitionApprovedByApplicationSearch;
import org.egov.bpa.web.model.demolition.DemolitionFISearchCriteria;
import org.egov.bpa.web.model.demolition.DemolitionFieldInspection;
import org.egov.bpa.web.model.demolition.DemolitionFieldInspectionRequest;
import org.egov.bpa.web.model.demolition.DemolitionFieldInspectionResponse;
import org.egov.bpa.web.model.demolition.DemolitionRequest;
import org.egov.bpa.web.model.demolition.DemolitionResponse;
import org.egov.bpa.web.model.demolition.DemolitionSearchCriteria;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/demolition")
public class DemolitionController {
	
	@Autowired
	private DemolitionService demolitionService;
	
	@Autowired
	private ResponseInfoFactory responseInfoFactory;
	
	/**
	 * Create Request controller for Demolition
	 * @param demolitionRequest
	 * @return
	 */
	@PostMapping(value = "/_create")
	public ResponseEntity<DemolitionResponse> create(@Valid @RequestBody DemolitionRequest request) {

		Demolition demolition = demolitionService.createDemolition(request);

		DemolitionResponse response = DemolitionResponse.builder().demolitions(Collections.singletonList(demolition))
				.responseInfo(responseInfoFactory.createResponseInfoFromRequestInfo(request.getRequestInfo(), true))
				.build();

		return new ResponseEntity<>(response, HttpStatus.OK);

	}
	
	@PostMapping(value = "/_search")
	public ResponseEntity<DemolitionResponse> search(@Valid @RequestBody RequestInfoWrapper requestInfoWrapper,
			@Valid @ModelAttribute DemolitionSearchCriteria criteria) {
		List<Demolition> demolitions = demolitionService.searchDemolition(criteria, requestInfoWrapper.getRequestInfo());
		DemolitionResponse response = DemolitionResponse.builder().demolitions(demolitions).responseInfo(
				responseInfoFactory.createResponseInfoFromRequestInfo(requestInfoWrapper.getRequestInfo(), true))
				.build();
		return new ResponseEntity<>(response, HttpStatus.OK);

	}
	
	/**
	 * Update Request controller for Demolition
	 * @param demolitionRequest
	 * @return
	 */
	@PostMapping(value = "/_update")
	public ResponseEntity<DemolitionResponse> update(@Valid @RequestBody DemolitionRequest request) {

		Demolition demolition = demolitionService.updateDemolition(request);

		DemolitionResponse response = DemolitionResponse.builder().demolitions(Collections.singletonList(demolition))
				.responseInfo(responseInfoFactory.createResponseInfoFromRequestInfo(request.getRequestInfo(), true))
				.build();

		return new ResponseEntity<>(response, HttpStatus.OK);

	}
	
	/**
	 * Get fee Estimates for demolition from this controller
	 * 
	 * @param regularizationRequest
	 * @return
	 */
	@PostMapping(value = { "/_estimate" })
	public ResponseEntity<Object> getFeeEstimate(@RequestBody Object demolitionRequest) {
		Object response = demolitionService.getFeeEstimateFromBpaCalculator(demolitionRequest);
		return new ResponseEntity<>(response, HttpStatus.OK);
	}
	
	/**
	 * Search the Villages and Status for the provided Application Numbers
	 * 
	 * @param requestInfoWrapper
	 * @param criteria
	 * @return
	 */
	@PostMapping(value = "/_searchVillage")
	public ResponseEntity<BPAVillageResponse> searchVillage(@Valid @RequestBody RequestInfoWrapper requestInfoWrapper,
			@Valid @ModelAttribute VillageSearchCriteria criteria) {

		List<BPAVillage> regularizationVillage = demolitionService.searchVillage(criteria, requestInfoWrapper.getRequestInfo());

		BPAVillageResponse response = BPAVillageResponse.builder()
				.bpaVillage(regularizationVillage).responseInfo(responseInfoFactory
						.createResponseInfoFromRequestInfo(requestInfoWrapper.getRequestInfo(), true))
				.build();
		return new ResponseEntity<>(response, HttpStatus.OK);
	}
	
	/**
	 * Search all the applications which are approved by Logged in User, for Offline Sign
	 * 
	 * @param requestInfoWrapper
	 * @param criteria
	 * @return
	 */
	@PostMapping(value = "/_ApprovedByMe")
	public ResponseEntity<DemolitionApprovedByApplicationResponse> searchApplicationApprovedBy(
			@Valid @RequestBody RequestInfoWrapper requestInfoWrapper,
			@Valid @ModelAttribute DemolitionSearchCriteria criteria) {

		List<DemolitionApprovedByApplicationSearch> demolitions = demolitionService
				.searchApplicationApprovedBy(criteria, requestInfoWrapper.getRequestInfo().getUserInfo().getUuid());

		DemolitionApprovedByApplicationResponse response = DemolitionApprovedByApplicationResponse.builder()
				.demolitionApprovedByApplicationSearch(demolitions).responseInfo(responseInfoFactory
						.createResponseInfoFromRequestInfo(requestInfoWrapper.getRequestInfo(), true))
				.build();
		return new ResponseEntity<>(response, HttpStatus.OK);
	}
	
	/**
	 * Permit Letter FileStore ID to be saved in AdditionalDetails of Demolition
	 * Application
	 * 
	 * @param demolitionrequest
	 * @return
	 */
	@PostMapping(value = "/_permitLetterUpdate")
	public ResponseEntity<DemolitionResponse> permitLetterUpdate(
			@Valid @RequestBody DemolitionRequest demolitionRequest) {

		Demolition demolition = demolitionService.permitLetterUpdate(demolitionRequest);

		List<Demolition> demolitions = new ArrayList<>();
		demolitions.add(demolition);
		DemolitionResponse response = DemolitionResponse.builder().demolitions(demolitions)
				.responseInfo(
						responseInfoFactory.createResponseInfoFromRequestInfo(demolitionRequest.getRequestInfo(), true))
				.build();
		return new ResponseEntity<>(response, HttpStatus.OK);

	}
	
	/**
	 * Create Field Inspection Report
	 * @param request
	 * @return FieldInspectionResponse
	 */
	@PostMapping(value = "/_createFieldInspectionReport")
	public ResponseEntity<DemolitionFieldInspectionResponse> create(
			@Valid @RequestBody DemolitionFieldInspectionRequest request) {

		DemolitionFieldInspection fieldInspection = demolitionService.createFieldInspectionReport(request);

		DemolitionFieldInspectionResponse response = DemolitionFieldInspectionResponse.builder()
				.fieldInspection(Collections.singletonList(fieldInspection))
				.responseInfo(responseInfoFactory.createResponseInfoFromRequestInfo(request.getRequestInfo(), true))
				.build();

		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	@PostMapping(value = "/_searchFieldInspectionReport")
	public ResponseEntity<DemolitionFieldInspectionResponse> search(
			@Valid @RequestBody RequestInfoWrapper requestInfoWrapper,
			@Valid @ModelAttribute DemolitionFISearchCriteria criteria) {

		List<DemolitionFieldInspection> fieldInspections = demolitionService.searchFieldInspectionReport(criteria,
				requestInfoWrapper.getRequestInfo());

		DemolitionFieldInspectionResponse response = DemolitionFieldInspectionResponse.builder()
				.fieldInspection(fieldInspections).responseInfo(responseInfoFactory
						.createResponseInfoFromRequestInfo(requestInfoWrapper.getRequestInfo(), true))
				.build();

		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	@PostMapping(value = "/_updateFieldInspectionReport")
	public ResponseEntity<DemolitionFieldInspectionResponse> update(
			@Valid @RequestBody DemolitionFieldInspectionRequest request) {

		DemolitionFieldInspection fieldInspection = demolitionService.updateFieldInspectionReport(request);

		DemolitionFieldInspectionResponse response = DemolitionFieldInspectionResponse.builder()
				.fieldInspection(Collections.singletonList(fieldInspection))
				.responseInfo(responseInfoFactory.createResponseInfoFromRequestInfo(request.getRequestInfo(), true))
				.build();

		return new ResponseEntity<>(response, HttpStatus.OK);
	}
}
