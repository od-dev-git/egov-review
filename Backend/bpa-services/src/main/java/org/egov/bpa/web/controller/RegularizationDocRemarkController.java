package org.egov.bpa.web.controller;

import java.util.Collections;
import java.util.List;

import javax.validation.Valid;

import org.egov.bpa.service.RegularizationDocRemarkService;
import org.egov.bpa.util.BPAUtil;
import org.egov.bpa.util.ResponseInfoFactory;
import org.egov.bpa.web.model.RequestInfoWrapper;
import org.egov.bpa.web.model.regularization.RegularizationDocRemark;
import org.egov.bpa.web.model.regularization.RegularizationDocRemarkRequest;
import org.egov.bpa.web.model.regularization.RegularizationDocRemarkResponse;
import org.egov.bpa.web.model.regularization.RegularizationDocRemarkSearchCriteria;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/regularization/docRemark")
public class RegularizationDocRemarkController {

	@Autowired
	private RegularizationDocRemarkService docRemarkService;

	@Autowired
	private BPAUtil bpaUtil;

	@Autowired
	private ResponseInfoFactory responseInfoFactory;

	
	
	/**
	 * Create Document Remark for Regularization
	 * 
	 * @param request
	 * @return ResponseEntity 
	 */
	@PostMapping(value = "/_create")
	public ResponseEntity<RegularizationDocRemarkResponse> create(@Valid @RequestBody RegularizationDocRemarkRequest request) {
		bpaUtil.defaultJsonPathConfig();
		RegularizationDocRemark docRemark = docRemarkService.create(request);
		RegularizationDocRemarkResponse response = RegularizationDocRemarkResponse.builder()
				.docRemarks(Collections.singletonList(docRemark))
				.responseInfo(responseInfoFactory.createResponseInfoFromRequestInfo(request.getRequestInfo(), true))
				.build();

		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	
	
	
	/**
	 * Searches Regularization Documents Remark based on criteria
	 * 
	 * @param requestInfoWrapper
	 * @param criteria
	 * @return List<RegularizationDocRemark>
	 */
	@PostMapping(value = "/_search")
	public ResponseEntity<RegularizationDocRemarkResponse> search(@Valid @RequestBody RequestInfoWrapper requestInfoWrapper,
			@Valid @ModelAttribute RegularizationDocRemarkSearchCriteria criteria) {
		List<RegularizationDocRemark> docRemarks = docRemarkService.search(criteria);
		RegularizationDocRemarkResponse response = RegularizationDocRemarkResponse.builder()
				.docRemarks(docRemarks)
				.responseInfo(responseInfoFactory.createResponseInfoFromRequestInfo(requestInfoWrapper.getRequestInfo(), true))
				.build();
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	
	
	
	/**
	 * Update Documents Remark for Regularization
	 * 
	 * @param request
	 * @return Updated DocRemark
	 */
	@PostMapping(value = "/_update")
	public ResponseEntity<RegularizationDocRemarkResponse> update(@Valid @RequestBody RegularizationDocRemarkRequest request) {
		RegularizationDocRemark docRemark = docRemarkService.update(request);
		RegularizationDocRemarkResponse response = RegularizationDocRemarkResponse.builder()
				.docRemarks(Collections.singletonList(docRemark))
				.responseInfo(responseInfoFactory.createResponseInfoFromRequestInfo(request.getRequestInfo(), true))
				.build();
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

}
