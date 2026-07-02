package org.egov.bpa.web.controller;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import javax.validation.Valid;

import org.egov.bpa.service.BPAAutoEscalationService;
import org.egov.bpa.service.BPAService;
import org.egov.bpa.util.BPAConstants;
import org.egov.bpa.util.BPAErrorConstants;
import org.egov.bpa.util.BPAUtil;
import org.egov.bpa.util.ResponseInfoFactory;
import org.egov.bpa.web.model.BPA;
import org.egov.bpa.web.model.BPAApplicationResponse;
import org.egov.bpa.web.model.BPAApprovedByApplicationResponse;
import org.egov.bpa.web.model.BPADocUploadRequest;
import org.egov.bpa.web.model.BPADocUploadResponse;
import org.egov.bpa.web.model.BPADraft;
import org.egov.bpa.web.model.BPADraftRequest;
import org.egov.bpa.web.model.BPADraftResponse;
import org.egov.bpa.web.model.BPADraftSearchCriteria;
import org.egov.bpa.web.model.BPAFeePendingResponse;
import org.egov.bpa.web.model.BPARequest;
import org.egov.bpa.web.model.BPAResponse;
import org.egov.bpa.web.model.BPASearchCriteria;
import org.egov.bpa.web.model.BPAVillage;
import org.egov.bpa.web.model.BPAVillageResponse;
import org.egov.bpa.web.model.BpaApplicationSearch;
import org.egov.bpa.web.model.BpaApprovedByApplicationSearch;
import org.egov.bpa.web.model.CompletionCertificate;
import org.egov.bpa.web.model.CompletionCertificateRequest;
import org.egov.bpa.web.model.CompletionCertificateResponse;
import org.egov.bpa.web.model.CompletionCertificateSearchCriteria;
import org.egov.bpa.web.model.DigitalSignCertificateResponse;
import org.egov.bpa.web.model.DocumentList;
import org.egov.bpa.web.model.DscDetails;
import org.egov.bpa.web.model.FeePendingApplication;
import org.egov.bpa.web.model.FieldInspection;
import org.egov.bpa.web.model.FieldInspectionRequest;
import org.egov.bpa.web.model.FieldInspectionResponse;
import org.egov.bpa.web.model.FieldInspectionSearchCriteria;
import org.egov.bpa.web.model.PlanningAssistantChecklist;
import org.egov.bpa.web.model.PlanningAssistantChecklistRequest;
import org.egov.bpa.web.model.PlanningAssistantChecklistResponse;
import org.egov.bpa.web.model.PlanningAssistantSearchCriteria;
import org.egov.bpa.web.model.RequestInfoWrapper;
import org.egov.bpa.web.model.StageWiseReport;
import org.egov.bpa.web.model.StageWiseReportRequest;
import org.egov.bpa.web.model.StageWiseReportResponse;
import org.egov.bpa.web.model.StageWiseReportSearchCriteria;
import org.egov.bpa.web.model.VillageSearchCriteria;
import org.egov.common.contract.request.RequestInfo;
import org.egov.tracer.model.CustomException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/bpa")
public class BPAController {

	@Autowired
	private BPAService bpaService;

	@Autowired
	private BPAUtil bpaUtil;

	@Autowired
	private ResponseInfoFactory responseInfoFactory;
	
	@Autowired
	private BPAAutoEscalationService autoEscalationService;

	@PostMapping(value = "/_create")
	public ResponseEntity<BPAResponse> create(@Valid @RequestBody BPARequest bpaRequest) {
		bpaUtil.defaultJsonPathConfig();
		BPA bpa = bpaService.create(bpaRequest);
		List<BPA> bpas = new ArrayList<BPA>();
		bpas.add(bpa);
		BPAResponse response = BPAResponse.builder().BPA(bpas)
				.responseInfo(responseInfoFactory.createResponseInfoFromRequestInfo(bpaRequest.getRequestInfo(), true))
				.build();
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	@PostMapping(value = "/_update")
	public ResponseEntity<BPAResponse> update(@Valid @RequestBody BPARequest bpaRequest) {
		BPA bpa = bpaService.update(bpaRequest);
		List<BPA> bpas = new ArrayList<BPA>();
		bpas.add(bpa);
		BPAResponse response = BPAResponse.builder().BPA(bpas)
				.responseInfo(responseInfoFactory.createResponseInfoFromRequestInfo(bpaRequest.getRequestInfo(), true))
				.build();
		return new ResponseEntity<>(response, HttpStatus.OK);

	}

	@PostMapping(value = "/_search")
	public ResponseEntity<BPAResponse> search(@Valid @RequestBody RequestInfoWrapper requestInfoWrapper,
			@Valid @ModelAttribute BPASearchCriteria criteria) {

		List<BPA> bpas = bpaService.search(criteria, requestInfoWrapper.getRequestInfo());

		BPAResponse response = BPAResponse.builder().BPA(bpas).responseInfo(
				responseInfoFactory.createResponseInfoFromRequestInfo(requestInfoWrapper.getRequestInfo(), true))
				.build();
		return new ResponseEntity<>(response, HttpStatus.OK);

	}
	
	@PostMapping(value = "/_count")
	public ResponseEntity<Integer> count(@Valid @RequestBody RequestInfoWrapper requestInfoWrapper,
			@Valid @ModelAttribute BPASearchCriteria criteria) {

		Integer count = bpaService.countv2(criteria, requestInfoWrapper.getRequestInfo());
		
		return new ResponseEntity<Integer>(count, HttpStatus.OK);
		
	}

	@PostMapping(value = "/_permitorderedcr")
	public ResponseEntity<Resource> getPdf(@Valid @RequestBody BPARequest bpaRequest) {

		Path path = Paths.get(BPAConstants.EDCR_PDF);
		Resource resource = null;

		bpaService.getEdcrPdf(bpaRequest);
		try {
			resource = new UrlResource(path.toUri());
		} catch (Exception ex) {
			throw new CustomException(BPAErrorConstants.UNABLE_TO_DOWNLOAD, "Unable to download the file");
		}

		return ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + BPAConstants.EDCR_PDF + "\"")
				.body(resource);
	}
	
	@PostMapping(value = {"/_updatedscdetails"})
    public ResponseEntity<BPAResponse> updateDscDetails(@Valid @RequestBody BPARequest bpaRequest) {
        BPA bpa = bpaService.updateDscDetails(bpaRequest);
        List<BPA> bpas=new ArrayList<>();
        bpas.add(bpa);

        BPAResponse response = BPAResponse.builder().BPA(bpas).responseInfo(
                responseInfoFactory.createResponseInfoFromRequestInfo(bpaRequest.getRequestInfo(), true))
                .build();
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
	
	@PostMapping(value = {"/_updateplandscdetails"})
    public ResponseEntity<BPAResponse> updateplanDscDetails(@Valid @RequestBody BPARequest bpaRequest) {
        BPA bpa = bpaService.updatePlanDscDetails(bpaRequest);
        List<BPA> bpas=new ArrayList<>();
        bpas.add(bpa);

        BPAResponse response = BPAResponse.builder().BPA(bpas).responseInfo(
                responseInfoFactory.createResponseInfoFromRequestInfo(bpaRequest.getRequestInfo(), true))
                .build();
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
	
	@PostMapping(value = { "/_searchdscdetails" })
	public ResponseEntity<DigitalSignCertificateResponse> searchDscDetails(
			@Valid @RequestBody RequestInfoWrapper requestInfoWrapper,
			@Valid @ModelAttribute BPASearchCriteria criteria, @RequestHeader HttpHeaders headers) {
		List<DscDetails> dscDetails = bpaService.searchDscDetails(criteria, requestInfoWrapper.getRequestInfo());

		DigitalSignCertificateResponse response = DigitalSignCertificateResponse.builder().dscDetails(dscDetails)
				.responseInfo(responseInfoFactory.createResponseInfoFromRequestInfo(requestInfoWrapper.getRequestInfo(),
						true))
				.build();
		return new ResponseEntity<>(response, HttpStatus.OK);

	}
	
	@PostMapping(value = { "/_searchplandscdetails" })
	public ResponseEntity<DigitalSignCertificateResponse> searchPlanDscDetails(
			@Valid @RequestBody RequestInfoWrapper requestInfoWrapper,
			@Valid @ModelAttribute BPASearchCriteria criteria, @RequestHeader HttpHeaders headers) {
		List<DscDetails> dscDetails = bpaService.searchPlanDscDetails(criteria, requestInfoWrapper.getRequestInfo());

		DigitalSignCertificateResponse response = DigitalSignCertificateResponse.builder().dscDetails(dscDetails)
				.responseInfo(responseInfoFactory.createResponseInfoFromRequestInfo(requestInfoWrapper.getRequestInfo(),
						true))
				.build();
		return new ResponseEntity<>(response, HttpStatus.OK);

	}
	
	/**
	 * Wrapper API to bpa-calculator /_estimate API as
	 * cannot access bpa-calculator APIs from UI directly
	 * 
	 * @param bpaReq The calculation Request
	 * @return Calculation Response
	 */
	@PostMapping(value = { "/_estimate" })
	public ResponseEntity<Object> getFeeEstimate(@RequestBody Object bpaRequest) {
		Object response = bpaService.getFeeEstimateFromBpaCalculator(bpaRequest);
		return new ResponseEntity<>(response, HttpStatus.OK);
	}
	
	/**
	 * Wrapper API to bpa-calculator /_getAllInstallments API as
	 * cannot access bpa-calculator APIs from UI directly
	 * 
	 * @param bpaReq The calculation Request
	 * @return Calculation Response
	 */
	@PostMapping(value = { "/_getAllInstallments" })
	public ResponseEntity<Object> getAllInstallments(@RequestBody Object bpaRequest) {
		Object response = bpaService.getAllInstallmentsFromBpaCalculator(bpaRequest);
		return new ResponseEntity<>(response, HttpStatus.OK);
	}
	
	/**
	 * Wrapper API to bpa-calculator /_getAllInstallments API as
	 * cannot access bpa-calculator APIs from UI directly
	 * 
	 * @param bpaReq The calculation Request
	 * @return Calculation Response
	 */
	@PostMapping(value = { "/_generateDemandFromInstallments" })
	public ResponseEntity<Object> generateDemandFromInstallments(@RequestBody Object bpaRequest) {
		Object response = bpaService.generateDemandFromInstallments(bpaRequest);
		return new ResponseEntity<>(response, HttpStatus.OK);
	}
	
	@PostMapping(value = { "/_mergeScrutinyReportToPermit" })
	public ResponseEntity<Object> mergeScrutinyReportToPermit(@Valid @RequestBody RequestInfoWrapper requestInfoWrapper,
			@Valid @RequestBody BPARequest bpaRequest) {
		return new ResponseEntity<>(
				bpaService.mergeScrutinyReportToPermit(bpaRequest, requestInfoWrapper.getRequestInfo()), HttpStatus.OK);
	}
	
	@PostMapping(value = "/_get")
	public ResponseEntity<BPAResponse> get(@Valid @RequestBody RequestInfoWrapper requestInfoWrapper) {

		List<BPA> bpas = bpaService.searchApplications(requestInfoWrapper.getRequestInfo());

		BPAResponse response = BPAResponse.builder().BPA(bpas).responseInfo(
				responseInfoFactory.createResponseInfoFromRequestInfo(requestInfoWrapper.getRequestInfo(), true))
				.build();
		return new ResponseEntity<>(response, HttpStatus.OK);
	}
	
	@PostMapping(value = "/_plainsearch")
	public ResponseEntity<BPAResponse> plainSearch(@Valid @RequestBody RequestInfoWrapper requestInfoWrapper,
			@Valid @ModelAttribute BPASearchCriteria criteria) {

		List<BPA> bpas = bpaService.plainSearch(criteria, requestInfoWrapper.getRequestInfo());
		BPAResponse response = BPAResponse.builder().BPA(bpas).responseInfo(
				responseInfoFactory.createResponseInfoFromRequestInfo(requestInfoWrapper.getRequestInfo(), true)).build();
		return new ResponseEntity<>(response, HttpStatus.OK);
	}
	
	
	@PostMapping(value = "/_reportsearch")
	public ResponseEntity<BPAResponse> reportSearch(@Valid @RequestBody RequestInfoWrapper requestInfoWrapper,
			@Valid @ModelAttribute BPASearchCriteria criteria) {

		List<BPA> bpas = bpaService.reportSearch(criteria, requestInfoWrapper.getRequestInfo());

		BPAResponse response = BPAResponse.builder().BPA(bpas).responseInfo(
				responseInfoFactory.createResponseInfoFromRequestInfo(requestInfoWrapper.getRequestInfo(), true))
				.build();
		return new ResponseEntity<>(response, HttpStatus.OK);
	}
	
	@PostMapping(value = "/_myApplication")
	public ResponseEntity<BPAApplicationResponse> searchApplication(@Valid @RequestBody RequestInfoWrapper requestInfoWrapper,
			@Valid @ModelAttribute BPASearchCriteria criteria) {

		List<BpaApplicationSearch> bpas = bpaService.searchBPAApplication(criteria, requestInfoWrapper.getRequestInfo());

		BPAApplicationResponse response = BPAApplicationResponse.builder().BpaApplicationSearch(bpas).responseInfo(
				responseInfoFactory.createResponseInfoFromRequestInfo(requestInfoWrapper.getRequestInfo(), true))
				.build();
		return new ResponseEntity<>(response, HttpStatus.OK);
	}
	
	@PostMapping(value = "/_ApprovedByMe")
	public ResponseEntity<BPAApprovedByApplicationResponse> searchApplicationApprovedBy(@Valid @RequestBody RequestInfoWrapper requestInfoWrapper,
			@Valid @ModelAttribute BPASearchCriteria criteria) {
		
		
		List<BpaApprovedByApplicationSearch> bpas = bpaService.searchApplicationApprovedBy(criteria, requestInfoWrapper.getRequestInfo().getUserInfo().getUuid());
		
		BPAApprovedByApplicationResponse response = BPAApprovedByApplicationResponse.builder().bpaApprovedByApplicationSearch(bpas).responseInfo(
				responseInfoFactory.createResponseInfoFromRequestInfo(requestInfoWrapper.getRequestInfo(), true))
				.build();
		return new ResponseEntity<>(response, HttpStatus.OK);
	}
	
	@PostMapping(value = "/_createFieldInspectionReport")
	public ResponseEntity<FieldInspectionResponse> create(@Valid @RequestBody FieldInspectionRequest request){
		FieldInspection fi = bpaService.createFieldInspectionReport(request);
		List<FieldInspection> fis = new ArrayList<FieldInspection>();
		fis.add(fi);
		FieldInspectionResponse response = FieldInspectionResponse.builder().fieldInspection(fis)
				.responseInfo(responseInfoFactory.createResponseInfoFromRequestInfo(request.getRequestInfo(), true))
				.build();
		return new ResponseEntity<>(response, HttpStatus.OK);
		
		
	}
	
	@PostMapping(value = "/_updateFieldInspectionReport")
	public ResponseEntity<FieldInspectionResponse> update(@Valid @RequestBody FieldInspectionRequest request){
		FieldInspection fi = bpaService.updateFieldInspectionReport(request);
		List<FieldInspection> fis = new ArrayList<FieldInspection>();
		fis.add(fi);
		FieldInspectionResponse response = FieldInspectionResponse.builder().fieldInspection(fis)
				.responseInfo(responseInfoFactory.createResponseInfoFromRequestInfo(request.getRequestInfo(), true))
				.build();
		return new ResponseEntity<>(response, HttpStatus.OK);
		
		
	}
	
	@PostMapping(value = "/_searchFieldInspectionReport")
	public ResponseEntity<FieldInspectionResponse> search(@Valid @RequestBody RequestInfoWrapper requestInfoWrapper,
			@Valid @ModelAttribute FieldInspectionSearchCriteria criteria){
		List<FieldInspection> fis = bpaService.searchFieldInspectionReport(criteria, requestInfoWrapper.getRequestInfo());
		FieldInspectionResponse response = FieldInspectionResponse.builder().fieldInspection(fis)
				.responseInfo(responseInfoFactory.createResponseInfoFromRequestInfo(requestInfoWrapper.getRequestInfo(), true))
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
	public ResponseEntity<FieldInspectionResponse> createFIReportV2(@Valid @RequestBody FieldInspectionRequest request){
		FieldInspection fi = bpaService.createFieldInspectionReportV2(request);
		List<FieldInspection> fis = new ArrayList<FieldInspection>();
		fis.add(fi);
		FieldInspectionResponse response = FieldInspectionResponse.builder().fieldInspection(fis)
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
	public ResponseEntity<FieldInspectionResponse> updateFIReportV2(@Valid @RequestBody FieldInspectionRequest request){
		FieldInspection fi = bpaService.updateFieldInspectionReportV2(request);
		List<FieldInspection> fis = new ArrayList<FieldInspection>();
		fis.add(fi);
		FieldInspectionResponse response = FieldInspectionResponse.builder().fieldInspection(fis)
				.responseInfo(responseInfoFactory.createResponseInfoFromRequestInfo(request.getRequestInfo(), true))
				.build();
		return new ResponseEntity<>(response, HttpStatus.OK);
	
	}
	
	@PostMapping(value = "/_permitLetterUpdate")
	public ResponseEntity<BPAResponse> permitLetterUpdate(@Valid @RequestBody BPARequest bpaRequest) {
		
		BPA bpa = bpaService.permitLetterUpdate(bpaRequest);
		
		List<BPA> bpas = new ArrayList<BPA>();
		bpas.add(bpa);
		BPAResponse response = BPAResponse.builder().BPA(bpas)
				.responseInfo(responseInfoFactory.createResponseInfoFromRequestInfo(bpaRequest.getRequestInfo(), true))
				.build();
		return new ResponseEntity<>(response, HttpStatus.OK);

	}
	
	@PostMapping(value = "/_autoEscalation")
	public void processBPAAutoEscalation(@RequestBody RequestInfoWrapper requestInfoWrapper) {
		autoEscalationService.processEscalation(requestInfoWrapper.getRequestInfo());
	}
	
	@PostMapping(value = "/_offlinedocupload")
	public ResponseEntity<BPADocUploadResponse> offlineDocUpload(@Valid @RequestBody BPADocUploadRequest request) {
		List<DocumentList> documents = bpaService.uploadDocument(request.getDocUploadRequest(),
				request.getRequestInfo());
		BPADocUploadResponse response = BPADocUploadResponse.builder().bpaDocuments(documents).auditDetails(request.getDocUploadRequest().getAuditDetails())
				.responseInfo(responseInfoFactory.createResponseInfoFromRequestInfo(request.getRequestInfo(), true))
				.build();
		return new ResponseEntity<>(response,HttpStatus.OK);

	}
	
	@PostMapping(value = "/_createPlanningAssistantChecklist")
	public ResponseEntity<PlanningAssistantChecklistResponse> create(@Valid @RequestBody PlanningAssistantChecklistRequest request) {
		PlanningAssistantChecklist pac = bpaService.createPlanningAssistantChecklist(request);
		List<PlanningAssistantChecklist> pacs = new ArrayList<PlanningAssistantChecklist>();
		pacs.add(pac);
		PlanningAssistantChecklistResponse response = PlanningAssistantChecklistResponse.builder()
				.planningAssistantChecklist(pacs)
				.responseInfo(responseInfoFactory.createResponseInfoFromRequestInfo(request.getRequestInfo(), true))
				.build();
		return new ResponseEntity<>(response, HttpStatus.OK);

	}
	
	@PostMapping(value = "/_searchPlanningAssistantChecklist")
	public ResponseEntity<PlanningAssistantChecklistResponse> search(@Valid @RequestBody RequestInfoWrapper requestInfoWrapper,
			@Valid @ModelAttribute PlanningAssistantSearchCriteria criteria){
		List<PlanningAssistantChecklist> pac = bpaService.searchPlanningAssistanctChecklist(criteria, requestInfoWrapper.getRequestInfo());
		PlanningAssistantChecklistResponse response = PlanningAssistantChecklistResponse.builder().planningAssistantChecklist(pac)
				.responseInfo(responseInfoFactory.createResponseInfoFromRequestInfo(requestInfoWrapper.getRequestInfo(), true))
				.build();
		return new ResponseEntity<>(response, HttpStatus.OK);
		
		
	}
	
	@PostMapping(value = "/_updatePlanningAssistantChecklist")
	public ResponseEntity<PlanningAssistantChecklistResponse> search(@Valid @RequestBody PlanningAssistantChecklistRequest request){
		PlanningAssistantChecklist pac = bpaService.updatePlanningAssistantChecklist(request);
		List<PlanningAssistantChecklist> pacs = new ArrayList<PlanningAssistantChecklist>();
		pacs.add(pac);
		PlanningAssistantChecklistResponse response = PlanningAssistantChecklistResponse.builder().planningAssistantChecklist(pacs)
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
	public ResponseEntity<BPAVillageResponse> searchVillage(@Valid @RequestBody RequestInfoWrapper requestInfoWrapper,
			@Valid @ModelAttribute VillageSearchCriteria criteria) {

		List<BPAVillage> bpaVillage = bpaService.searchVillage(criteria, requestInfoWrapper.getRequestInfo());

		BPAVillageResponse response = BPAVillageResponse.builder().bpaVillage(bpaVillage).responseInfo(
				responseInfoFactory.createResponseInfoFromRequestInfo(requestInfoWrapper.getRequestInfo(), true))
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
	@PostMapping(value = "/_searchBPAFeePending")
	public ResponseEntity<BPAFeePendingResponse> searchBPAFeePending(
			@Valid @RequestBody RequestInfoWrapper requestInfoWrapper,
			@Valid @ModelAttribute BPASearchCriteria criteria) {

		List<FeePendingApplication> feePendingApplications = bpaService.searchBPAFeePending(criteria,
				requestInfoWrapper.getRequestInfo());

		BPAFeePendingResponse response = BPAFeePendingResponse.builder().feePendingApplications(feePendingApplications)
				.responseInfo(responseInfoFactory.createResponseInfoFromRequestInfo(requestInfoWrapper.getRequestInfo(),
						true))
				.build();
		return new ResponseEntity<>(response, HttpStatus.OK);
	}
	
	
	@PostMapping(value = "/_savedraft")
	public ResponseEntity<BPADraftResponse> saveDraft(@Valid @RequestBody BPADraftRequest request) {
		bpaUtil.defaultJsonPathConfig();
		BPADraft draft = bpaService.save(request);
		List<BPADraft> drafts = new ArrayList<BPADraft>();
		drafts.add(draft);
		BPADraftResponse response = BPADraftResponse.builder().bpaDraft(drafts)
				.responseInfo(responseInfoFactory.createResponseInfoFromRequestInfo(request.getRequestInfo(), true))
				.build();
		return new ResponseEntity<>(response, HttpStatus.OK);
	}
	
	@PostMapping(value = "/_searchdraft")
	public ResponseEntity<BPADraftResponse> search(@Valid @RequestBody RequestInfoWrapper requestInfoWrapper,
			@Valid @ModelAttribute BPADraftSearchCriteria criteria) {

		List<BPADraft> drafts = bpaService.search(criteria, requestInfoWrapper.getRequestInfo());

		BPADraftResponse response = BPADraftResponse.builder().bpaDraft(drafts).responseInfo(
				responseInfoFactory.createResponseInfoFromRequestInfo(requestInfoWrapper.getRequestInfo(), true))
				.build();
		return new ResponseEntity<>(response, HttpStatus.OK);
	}
	
	@PostMapping(value = "/_draftcount")
	public ResponseEntity<Integer> draftCount(@Valid @RequestBody RequestInfoWrapper requestInfoWrapper,
			@Valid @ModelAttribute BPADraftSearchCriteria criteria) {

		Integer count = bpaService.draftCount(criteria, requestInfoWrapper.getRequestInfo());
		return new ResponseEntity<Integer>(count, HttpStatus.OK);
	}
	
	@PostMapping(value = { "/_searchdscdetailscount" })
	public ResponseEntity<Integer> searchDscDetailsCount(@Valid @RequestBody RequestInfoWrapper requestInfoWrapper,
			@Valid @ModelAttribute BPASearchCriteria criteria, @RequestHeader HttpHeaders headers) {

		Integer count = bpaService.searchDscDetailsCount(criteria, requestInfoWrapper.getRequestInfo());
		return new ResponseEntity<>(count, HttpStatus.OK);

	}
	
	@PostMapping(value = { "/_searchplandscdetailscount" })
	public ResponseEntity<Integer> searchPlanDscDetailsCount(
			@Valid @RequestBody RequestInfoWrapper requestInfoWrapper,
			@Valid @ModelAttribute BPASearchCriteria criteria, @RequestHeader HttpHeaders headers) {

		Integer count = bpaService.searchPlanDscDetailsCount(criteria, requestInfoWrapper.getRequestInfo());
		return new ResponseEntity<>(count, HttpStatus.OK);

	}
	
	@PostMapping(value = "/_ApprovedByMecount")
	public ResponseEntity<Integer> searchApplicationApprovedByCount(@Valid @RequestBody RequestInfoWrapper requestInfoWrapper,
			@Valid @ModelAttribute BPASearchCriteria criteria) {
		Integer count = bpaService.searchApplicationApprovedByCount(criteria, requestInfoWrapper.getRequestInfo().getUserInfo().getUuid());
		return new ResponseEntity<>(count, HttpStatus.OK);
	}
	
	@PostMapping(value = "/completioncertificate/_create")
	public ResponseEntity<CompletionCertificateResponse> completionCertificateCreate(@Valid @RequestBody CompletionCertificateRequest completionCertificateRequest) {
		bpaUtil.defaultJsonPathConfig();
		CompletionCertificate completionCertificate = bpaService.create(completionCertificateRequest);
		List<CompletionCertificate> completionCertificates = new ArrayList<CompletionCertificate>();
		completionCertificates.add(completionCertificate);
		CompletionCertificateResponse response = CompletionCertificateResponse.builder().completionCertificate(completionCertificates)
				.responseInfo(responseInfoFactory.createResponseInfoFromRequestInfo(completionCertificateRequest.getRequestInfo(), true))
				.build();
		return new ResponseEntity<>(response, HttpStatus.OK);
	}
	
	@PostMapping(value = "/completioncertificate/_search")
	public ResponseEntity<CompletionCertificateResponse> search(
			@Valid @RequestBody RequestInfoWrapper requestInfoWrapper,
			@Valid @ModelAttribute CompletionCertificateSearchCriteria criteria) {

		List<CompletionCertificate> completionCertificates = bpaService.search(criteria,
				requestInfoWrapper.getRequestInfo());

		CompletionCertificateResponse response = CompletionCertificateResponse.builder()
				.completionCertificate(completionCertificates).responseInfo(responseInfoFactory
						.createResponseInfoFromRequestInfo(requestInfoWrapper.getRequestInfo(), true))
				.build();
		return new ResponseEntity<>(response, HttpStatus.OK);

	}
	
	@PostMapping(value = "/completioncertificate/_count")
	public ResponseEntity<Integer> count(@Valid @RequestBody RequestInfoWrapper requestInfoWrapper,
			@Valid @ModelAttribute CompletionCertificateSearchCriteria criteria) {

		Integer count = bpaService.count(criteria, requestInfoWrapper.getRequestInfo());
		return new ResponseEntity<>(count, HttpStatus.OK);

	}
	
	@PostMapping(value = "/stagewisereport/_create")
	public ResponseEntity<StageWiseReportResponse> stageWiseReportCreate(
			@Valid @RequestBody StageWiseReportRequest stageWiseReportRequest) {
		bpaUtil.defaultJsonPathConfig();
		int maxItems = 5000;
		if (stageWiseReportRequest.getStageWiseReports() != null
				&& stageWiseReportRequest.getStageWiseReports().size() > maxItems) {
			throw new CustomException(BPAErrorConstants.PAYLOAD_TOO_LARGE,
					BPAErrorConstants.STAGEWISE_REPORTLIST_SIZE_EXCEED);
		}
		List<StageWiseReport> stageWiseReports = bpaService.create(stageWiseReportRequest);
		StageWiseReportResponse response = StageWiseReportResponse.builder().stageWiseReports(stageWiseReports)
				.responseInfo(responseInfoFactory
						.createResponseInfoFromRequestInfo(stageWiseReportRequest.getRequestInfo(), true))
				.build();
		return new ResponseEntity<>(response, HttpStatus.CREATED);
	}
	
	@PostMapping(value = "/stagewisereport/_update")
	public ResponseEntity<StageWiseReportResponse> stageWiseReportUpdate(
			@Valid @RequestBody StageWiseReportRequest stageWiseReportRequest) {
		bpaUtil.defaultJsonPathConfig();
		int maxItems = 5000;
		if (stageWiseReportRequest.getStageWiseReports() != null
				&& stageWiseReportRequest.getStageWiseReports().size() > maxItems) {
			throw new CustomException(BPAErrorConstants.PAYLOAD_TOO_LARGE,
					BPAErrorConstants.STAGEWISE_REPORTLIST_SIZE_EXCEED);
		}
		List<StageWiseReport> stageWiseReports = bpaService.update(stageWiseReportRequest);
		StageWiseReportResponse response = StageWiseReportResponse.builder().stageWiseReports(stageWiseReports)
				.responseInfo(responseInfoFactory
						.createResponseInfoFromRequestInfo(stageWiseReportRequest.getRequestInfo(), true))
				.build();
		return new ResponseEntity<>(response, HttpStatus.OK);
	}
	
	@PostMapping(value = "/stagewisereport/_search")
	public ResponseEntity<StageWiseReportResponse> search(@Valid @RequestBody RequestInfoWrapper requestInfoWrapper,
			@Valid @ModelAttribute StageWiseReportSearchCriteria criteria) {

		List<StageWiseReport> stageWiseReports = bpaService.search(criteria, requestInfoWrapper.getRequestInfo());

		StageWiseReportResponse response = StageWiseReportResponse.builder().stageWiseReports(stageWiseReports)
				.responseInfo(responseInfoFactory.createResponseInfoFromRequestInfo(requestInfoWrapper.getRequestInfo(),
						true))
				.build();
		return new ResponseEntity<>(response, HttpStatus.OK);

	}
	
	@PostMapping(value = "/stagewisereport/filestoreid/_search")
	public ResponseEntity<String> stageWiseReportFileStoreIdSearch(
			@Valid @RequestBody RequestInfoWrapper requestInfoWrapper) {
		String fileStoreId = bpaService.getStageWiseReportFileStoreId();
		return new ResponseEntity<>(fileStoreId, HttpStatus.OK);

	}
	
	
}
			
		


