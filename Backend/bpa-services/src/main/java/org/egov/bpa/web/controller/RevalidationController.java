package org.egov.bpa.web.controller;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.validation.Valid;

import org.egov.bpa.service.RevalidationHelper;
import org.egov.bpa.service.RevalidationService;
import org.egov.bpa.util.BPAUtil;
import org.egov.bpa.util.ResponseInfoFactory;
import org.egov.bpa.web.model.BPA;
import org.egov.bpa.web.model.BPAResponse;
import org.egov.bpa.web.model.RequestInfoWrapper;
import org.egov.bpa.web.model.Revalidation;
import org.egov.bpa.web.model.RevalidationRequest;
import org.egov.bpa.web.model.RevalidationResponse;
import org.egov.bpa.web.model.RevalidationSearchCriteriaWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/revalidation")
public class RevalidationController {

	@Autowired
	private RevalidationService revalidationService;

	@Autowired
	private BPAUtil bpaUtil;

	@Autowired
	private ResponseInfoFactory responseInfoFactory;
	
	@Autowired
	private RevalidationHelper helper;
	
	@PostMapping(value = "/_convert")
    public String convert(@RequestParam String fileStoreIds, @RequestParam String tenantId) throws Exception {
        String s= revalidationService.convertExcelToJsonFromUrl(fileStoreIds, tenantId);
        return s;
    }
	
	//localhost:8098/bpa-services/v1/revalidation/file/blockLevelDetailFormat
	@GetMapping("/file/blockLevelDetailFormat")
	@ResponseBody
	public ResponseEntity<Resource> downloadFile() throws IOException {
		ClassPathResource resource = new ClassPathResource("BlockLevelDetailsFormatRevalidation.xlsx");
		InputStream inputStream = resource.getInputStream();
		InputStreamResource inputStreamResource = new InputStreamResource(inputStream);

		HttpHeaders headers = new HttpHeaders();
		headers.setContentDispositionFormData("attachment", "BlockLevelDetailsFormatRevalidation.xlsx");
		headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);

		return ResponseEntity.ok().headers(headers).body(inputStreamResource);
	}

	@PostMapping(value = "/_create")
	public ResponseEntity<RevalidationResponse> create(@Valid @RequestBody RevalidationRequest revalidationRequest) {
		bpaUtil.defaultJsonPathConfig();
		Revalidation revalidation = revalidationService.create(revalidationRequest);
		List<Revalidation> revalidations = new ArrayList<Revalidation>();
		revalidations.add(revalidation);
		RevalidationResponse response = RevalidationResponse.builder().revalidation(revalidations).responseInfo(
				responseInfoFactory.createResponseInfoFromRequestInfo(revalidationRequest.getRequestInfo(), true))
				.build();
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	@PostMapping(value = "/_search")
	public ResponseEntity<RevalidationResponse> search(@Valid @RequestBody RevalidationSearchCriteriaWrapper criteria) {

		List<Revalidation> revalidation = revalidationService
				.getRevalidationFromCriteria(criteria.getRevalidationSearchCriteria());

		RevalidationResponse response = RevalidationResponse.builder().revalidation(revalidation)
				.responseInfo(responseInfoFactory.createResponseInfoFromRequestInfo(criteria.getRequestInfo(), true))
				.build();
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	@PostMapping(value = "/_update")
	public ResponseEntity<RevalidationResponse> update(@Valid @RequestBody RevalidationRequest revalidationRequest) {
		Revalidation revalidation = revalidationService.update(revalidationRequest);
		List<Revalidation> revalidations = new ArrayList<Revalidation>();
		revalidations.add(revalidation);
		RevalidationResponse response = RevalidationResponse.builder().revalidation(revalidations).responseInfo(
				responseInfoFactory.createResponseInfoFromRequestInfo(revalidationRequest.getRequestInfo(), true))
				.build();
		return new ResponseEntity<>(response, HttpStatus.OK);
	}
	
	@PostMapping(value = "/_validateRevalidation")
	public ResponseEntity<BPAResponse> validateRevalidationEligibility(@RequestBody RequestInfoWrapper requestInfoWrapper, @RequestParam String approvalNo) {
		
		List<BPA> bpas = helper.checkEligibility(requestInfoWrapper.getRequestInfo(), approvalNo);
		BPAResponse response = BPAResponse.builder().BPA(bpas)
				.responseInfo(responseInfoFactory.createResponseInfoFromRequestInfo(requestInfoWrapper.getRequestInfo(), true))
				.build();
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

}
