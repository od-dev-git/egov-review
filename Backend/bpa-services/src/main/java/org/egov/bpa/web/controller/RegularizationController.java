package org.egov.bpa.web.controller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.validation.Valid;

import org.egov.bpa.service.RegularizationAutoEscalationService;
import org.egov.bpa.service.RegularizationService;
import org.egov.bpa.util.ResponseInfoFactory;
import org.egov.bpa.web.model.BPADocUploadResponse;
import org.egov.bpa.web.model.BPAFeePendingResponse;
import org.egov.bpa.web.model.BPASearchCriteria;
import org.egov.bpa.web.model.FeePendingApplication;
import org.egov.bpa.web.model.RegularizationDraft;
import org.egov.bpa.web.model.RegularizationDraftRequest;
import org.egov.bpa.web.model.RegularizationDraftResponse;
import org.egov.bpa.web.model.RegularizationDraftSearchCriteria;
import org.egov.bpa.web.model.RequestInfoWrapper;
import org.egov.bpa.web.model.VillageSearchCriteria;
import org.egov.bpa.web.model.regularization.Regularization;
import org.egov.bpa.web.model.regularization.RegularizationApprovedByApplicationResponse;
import org.egov.bpa.web.model.regularization.RegularizationApprovedByApplicationSearch;
import org.egov.bpa.web.model.regularization.RegularizationDigitalSignCertificateResponse;
import org.egov.bpa.web.model.regularization.RegularizationDocUploadRequest;
import org.egov.bpa.web.model.regularization.RegularizationDocUploadResponse;
import org.egov.bpa.web.model.regularization.RegularizationDocumentList;
import org.egov.bpa.web.model.regularization.RegularizationDscDetails;
import org.egov.bpa.web.model.regularization.RegularizationFISearchCriteria;
import org.egov.bpa.web.model.regularization.RegularizationFieldInspection;
import org.egov.bpa.web.model.regularization.RegularizationFieldInspectionRequest;
import org.egov.bpa.web.model.regularization.RegularizationFieldInspectionResponse;
import org.egov.bpa.web.model.regularization.RegularizationRequest;
import org.egov.bpa.web.model.regularization.RegularizationResponse;
import org.egov.bpa.web.model.regularization.RegularizationSearchCriteria;
import org.egov.bpa.web.model.regularization.RegularizationVillage;
import org.egov.bpa.web.model.regularization.RegularizationVillageResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/regularization")
public class RegularizationController {
	
	@Autowired
	private RegularizationService regularizationService;
	
	@Autowired
	private ResponseInfoFactory responseInfoFactory;
	
	@Autowired
	private RegularizationAutoEscalationService autoEscalationService;
	
	/**
	 * Create Request controller for Regularization
	 * @param regularizationRequest
	 * @return
	 */
	@PostMapping(value = "/_create")
	public ResponseEntity<RegularizationResponse> create(@Valid @RequestBody RegularizationRequest regularizationRequest) {
		
		Regularization regularization = regularizationService.create(regularizationRequest);
		
		RegularizationResponse response = RegularizationResponse.builder().regularizations(Collections.singletonList(regularization)).responseInfo(
				responseInfoFactory.createResponseInfoFromRequestInfo(regularizationRequest.getRequestInfo(), true))
				.build();
		
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	
	/**
	 * Searches Regularization belonging BPA based on criteria
	 * 
	 * @param requestInfoWrapper
	 * @param criteria
	 * @return RegularizationResponse
	 */
	@PostMapping(value = "/_search")
	public ResponseEntity<RegularizationResponse> searchRegularization(@Valid @RequestBody RequestInfoWrapper requestInfoWrapper,
			@Valid @ModelAttribute RegularizationSearchCriteria searchCriteria) {

		List<Regularization> regularizations = regularizationService.searchRegularization(searchCriteria, requestInfoWrapper.getRequestInfo());
		RegularizationResponse response = RegularizationResponse.builder()
								.regularizations(regularizations)
								.responseInfo(responseInfoFactory.createResponseInfoFromRequestInfo(requestInfoWrapper.getRequestInfo(), true))
								.build();
		return new ResponseEntity<>(response, HttpStatus.OK);
	}
	
	/**
	 * Update Request controller For Regularization
	 * 
	 * @param regularizationRequest
	 * @return
	 */
	@PostMapping(value = "/_update")
	public ResponseEntity<RegularizationResponse> update(@Valid @RequestBody RegularizationRequest regularizationRequest) {

		Regularization regularization = regularizationService.update(regularizationRequest);

		RegularizationResponse response = RegularizationResponse.builder()
				.regularizations(Collections.singletonList(regularization)).responseInfo(responseInfoFactory
						.createResponseInfoFromRequestInfo(regularizationRequest.getRequestInfo(), true))
				.build();

		return new ResponseEntity<>(response, HttpStatus.OK);
	}
	
	
	
	/**
	 * Get fee Estimates for regularizaion from this controller
	 * 
	 * @param regularizationRequest
	 * @return
	 */
	@PostMapping(value = { "/_estimate" })
	public ResponseEntity<Object> getFeeEstimate(@RequestBody Object regularizationRequest) {
		Object response = regularizationService.getFeeEstimateFromBpaCalculator(regularizationRequest);
		return new ResponseEntity<>(response, HttpStatus.OK);
	}
	
	/**
	 * Controller to trigger Auto escalation and show cause notice for Regularization
	 * 
	 * @param requestInfoWrapper
	 */
	@PostMapping(value = "/_autoEscalation")
	public void processRegularizationAutoEscalation(@RequestBody RequestInfoWrapper requestInfoWrapper) {
		autoEscalationService.processEscalationAndShowCause(requestInfoWrapper.getRequestInfo());
	}
	
	
	
	/**
	 * Update Regularization DSC Details
	 * 
	 * @param regularizationRequest
	 * @return RegularizationResponse
	 */
	@PostMapping(value = {"/_updateDscDetails"})
    public ResponseEntity<RegularizationResponse> updateRegularizationDscDetails(@Valid @RequestBody RegularizationRequest regularizationRequest) {
        
		Regularization regularization = regularizationService.updateRegularizationDscDetails(regularizationRequest);

		RegularizationResponse response = RegularizationResponse.builder()
				.regularizations(Collections.singletonList(regularization))
				.responseInfo(responseInfoFactory.createResponseInfoFromRequestInfo(regularizationRequest.getRequestInfo(), true))
				.build();

		return new ResponseEntity<>(response, HttpStatus.OK);
    }
	
	
	
	/**
	 * Searches Regularization Digital Sign Certificate belonging BPA based on criteria
	 * 
	 * @param requestInfoWrapper
	 * @param searchCriteria
	 * @return RegularizationDigitalSignCertificateResponse
	 */
	@PostMapping(value = { "/_searchDscDetails" })
	public ResponseEntity<RegularizationDigitalSignCertificateResponse> searchRegularizationDscDetails(@Valid @RequestBody RequestInfoWrapper requestInfoWrapper,
			@Valid @ModelAttribute RegularizationSearchCriteria searchCriteria) {
		List<RegularizationDscDetails> dscDetails = regularizationService.searchRegularizationDscDetails(searchCriteria, requestInfoWrapper.getRequestInfo());

		RegularizationDigitalSignCertificateResponse response = RegularizationDigitalSignCertificateResponse.builder()
				.dscDetails(dscDetails)
				.responseInfo(responseInfoFactory.createResponseInfoFromRequestInfo(requestInfoWrapper.getRequestInfo(), true))
				.build();
		return new ResponseEntity<>(response, HttpStatus.OK);

	}
	
	/**
	 * Permit Letter FileStore ID to be saved in AdditionalDetails of Regularization
	 * Application
	 * 
	 * @param regularizationRequest
	 * @return
	 */
	@PostMapping(value = "/_permitLetterUpdate")
	public ResponseEntity<RegularizationResponse> permitLetterUpdate(
			@Valid @RequestBody RegularizationRequest regularizationRequest) {

		Regularization regularization = regularizationService.permitLetterUpdate(regularizationRequest);

		List<Regularization> regularizations = new ArrayList<>();
		regularizations.add(regularization);
		RegularizationResponse response = RegularizationResponse.builder().regularizations(regularizations)
				.responseInfo(responseInfoFactory
						.createResponseInfoFromRequestInfo(regularizationRequest.getRequestInfo(), true))
				.build();
		return new ResponseEntity<>(response, HttpStatus.OK);

	}
	
	/**
	 * Search all the applications which are approved by Logged in User, for DSC
	 * Sign
	 * 
	 * @param requestInfoWrapper
	 * @param criteria
	 * @return
	 */
	@PostMapping(value = "/_ApprovedByMe")
	public ResponseEntity<RegularizationApprovedByApplicationResponse> searchApplicationApprovedBy(
			@Valid @RequestBody RequestInfoWrapper requestInfoWrapper,
			@Valid @ModelAttribute RegularizationSearchCriteria criteria) {

		List<RegularizationApprovedByApplicationSearch> regularizations = regularizationService
				.searchApplicationApprovedBy(criteria, requestInfoWrapper.getRequestInfo().getUserInfo().getUuid());

		RegularizationApprovedByApplicationResponse response = RegularizationApprovedByApplicationResponse.builder()
				.RegularizationApprovedByApplicationSearch(regularizations).responseInfo(responseInfoFactory
						.createResponseInfoFromRequestInfo(requestInfoWrapper.getRequestInfo(), true))
				.build();
		return new ResponseEntity<>(response, HttpStatus.OK);
	}
	
	
	/**
	 * Create Field Inspection Report
	 * @param request
	 * @return FieldInspectionResponse
	 */
	@PostMapping(value = "/_createFieldInspectionReport")
	public ResponseEntity<RegularizationFieldInspectionResponse> create(@Valid @RequestBody RegularizationFieldInspectionRequest request){
		RegularizationFieldInspection fieldInspection = regularizationService.createFieldInspectionReport(request);
		RegularizationFieldInspectionResponse response = RegularizationFieldInspectionResponse.builder()
				.fieldInspection(Collections.singletonList(fieldInspection))
				.responseInfo(responseInfoFactory.createResponseInfoFromRequestInfo(request.getRequestInfo(), true))
				.build();
		return new ResponseEntity<>(response, HttpStatus.OK);
		
	}
	
	/**
	 * Search Field Inspection Report
	 * 
	 * @param requestInfoWrapper
	 * @param criteria
	 * @return
	 */
	@PostMapping(value = "/_searchFieldInspectionReport")
	public ResponseEntity<RegularizationFieldInspectionResponse> search(
			@Valid @RequestBody RequestInfoWrapper requestInfoWrapper,
			@Valid @ModelAttribute RegularizationFISearchCriteria criteria) {
		List<RegularizationFieldInspection> fieldInspections = regularizationService
				.searchFieldInspectionReport(criteria, requestInfoWrapper.getRequestInfo());
		RegularizationFieldInspectionResponse response = RegularizationFieldInspectionResponse.builder()
				.fieldInspection(fieldInspections).responseInfo(responseInfoFactory
						.createResponseInfoFromRequestInfo(requestInfoWrapper.getRequestInfo(), true))
				.build();
		return new ResponseEntity<>(response, HttpStatus.OK);

	}
	
	/**
	 * Update Field Inspection Report
	 * @param request
	 * @return FieldInspectionResponse
	 */
	@PostMapping(value = "/_updateFieldInspectionReport")
	public ResponseEntity<RegularizationFieldInspectionResponse> update(@Valid @RequestBody RegularizationFieldInspectionRequest request){
		RegularizationFieldInspection fieldInspection = regularizationService.updateFieldInspectionReport(request);
		RegularizationFieldInspectionResponse response = RegularizationFieldInspectionResponse.builder()
				.fieldInspection(Collections.singletonList(fieldInspection))
				.responseInfo(responseInfoFactory.createResponseInfoFromRequestInfo(request.getRequestInfo(), true))
				.build();
		return new ResponseEntity<>(response, HttpStatus.OK);
		
	}
	
	/**
	 * API to create FI Report for Updated FI Payload in sync with Mobile app
	 * 
	 * @param request
	 * @return
	 */
	@PostMapping(value = "/_createFieldInspectionReportV2")
	public ResponseEntity<RegularizationFieldInspectionResponse> createFIReportV2(@Valid @RequestBody RegularizationFieldInspectionRequest request){
		RegularizationFieldInspection fieldInspection = regularizationService.createFieldInspectionReportV2(request);
		RegularizationFieldInspectionResponse response = RegularizationFieldInspectionResponse.builder()
				.fieldInspection(Collections.singletonList(fieldInspection))
				.responseInfo(responseInfoFactory.createResponseInfoFromRequestInfo(request.getRequestInfo(), true))
				.build();
		return new ResponseEntity<>(response, HttpStatus.OK);	
	}
	
	/**
	 * API to update FI Report for Updated FI Payload in sync with Mobile app
	 * 
	 * @param request
	 * @return
	 */
	@PostMapping(value = "/_updateFieldInspectionReportV2")
	public ResponseEntity<RegularizationFieldInspectionResponse> updateFIReportV2(@Valid @RequestBody RegularizationFieldInspectionRequest request){
		RegularizationFieldInspection fieldInspection = regularizationService.updateFieldInspectionReportV2(request);
		RegularizationFieldInspectionResponse response = RegularizationFieldInspectionResponse.builder()
				.fieldInspection(Collections.singletonList(fieldInspection))
				.responseInfo(responseInfoFactory.createResponseInfoFromRequestInfo(request.getRequestInfo(), true))
				.build();
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
	public ResponseEntity<RegularizationVillageResponse> searchVillage(@Valid @RequestBody RequestInfoWrapper requestInfoWrapper,
			@Valid @ModelAttribute VillageSearchCriteria criteria) {

		List<RegularizationVillage> regularizationVillage = regularizationService.searchVillage(criteria, requestInfoWrapper.getRequestInfo());

		RegularizationVillageResponse response = RegularizationVillageResponse.builder()
				.regularizationVillage(regularizationVillage).responseInfo(responseInfoFactory
						.createResponseInfoFromRequestInfo(requestInfoWrapper.getRequestInfo(), true))
				.build();
		return new ResponseEntity<>(response, HttpStatus.OK);
	}
	
	/**
	 * Serch applications which are pending at Fee Payment for more than 30 days
	 * 
	 * @param requestInfoWrapper
	 * @param criteria
	 * @return
	 */
	@PostMapping(value = "/_searchRegularizationFeePending")
	public ResponseEntity<BPAFeePendingResponse> searchBPAFeePending(
			@Valid @RequestBody RequestInfoWrapper requestInfoWrapper,
			@Valid @ModelAttribute BPASearchCriteria criteria) {

		List<FeePendingApplication> feePendingApplications = regularizationService.searchRegularizationFeePending(criteria,
				requestInfoWrapper.getRequestInfo());

		BPAFeePendingResponse response = BPAFeePendingResponse.builder().feePendingApplications(feePendingApplications)
				.responseInfo(responseInfoFactory.createResponseInfoFromRequestInfo(requestInfoWrapper.getRequestInfo(),
						true))
				.build();
		return new ResponseEntity<>(response, HttpStatus.OK);
	}
	
	/**
	 * Upload documents in application, even after approval
	 * 
	 * @param request
	 * @return
	 */
	@PostMapping(value = "/_offlinedocupload")
	public ResponseEntity<RegularizationDocUploadResponse> offlineDocUpload(@Valid @RequestBody RegularizationDocUploadRequest request) {
		List<RegularizationDocumentList> documents = regularizationService.uploadDocument(request.getDocUploadRequest(),
				request.getRequestInfo());
		
		RegularizationDocUploadResponse response = RegularizationDocUploadResponse.builder()
				.regularizationDocuments(documents).auditDetails(request.getDocUploadRequest().getAuditDetails())
				.responseInfo(responseInfoFactory.createResponseInfoFromRequestInfo(request.getRequestInfo(), true))
				.build();
		return new ResponseEntity<>(response,HttpStatus.OK);
	}
	
	@PostMapping(value = "/_savedraft")
	public ResponseEntity<RegularizationDraftResponse> saveDraft(@Valid @RequestBody RegularizationDraftRequest request) {
		RegularizationDraft draft = regularizationService.save(request);
		List<RegularizationDraft> drafts = new ArrayList<RegularizationDraft>();
		drafts.add(draft);
		RegularizationDraftResponse response = RegularizationDraftResponse.builder().regularizationDraft(drafts)
				.responseInfo(responseInfoFactory.createResponseInfoFromRequestInfo(request.getRequestInfo(), true))
				.build();
		return new ResponseEntity<>(response, HttpStatus.OK);
	}
	
	@PostMapping(value = "/_searchdraft")
	public ResponseEntity<RegularizationDraftResponse> search(@Valid @RequestBody RequestInfoWrapper requestInfoWrapper,
			@Valid @ModelAttribute RegularizationDraftSearchCriteria criteria) {

		List<RegularizationDraft> drafts = regularizationService.search(criteria, requestInfoWrapper.getRequestInfo());

		RegularizationDraftResponse response = RegularizationDraftResponse.builder().regularizationDraft(drafts).responseInfo(
				responseInfoFactory.createResponseInfoFromRequestInfo(requestInfoWrapper.getRequestInfo(), true))
				.build();
		return new ResponseEntity<>(response, HttpStatus.OK);
	}
}
