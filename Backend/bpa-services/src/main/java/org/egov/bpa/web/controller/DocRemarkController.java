package org.egov.bpa.web.controller;

import java.util.ArrayList;
import java.util.List;

import javax.validation.Valid;

import org.egov.bpa.service.DocRemarkService;
import org.egov.bpa.util.BPAUtil;
import org.egov.bpa.util.ResponseInfoFactory;
import org.egov.bpa.web.model.DocRemark;
import org.egov.bpa.web.model.DocRemarkRequest;
import org.egov.bpa.web.model.DocRemarkResponse;
import org.egov.bpa.web.model.DocRemarkSearchCriteria;
import org.egov.bpa.web.model.RequestInfoWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/docRemark")
public class DocRemarkController {

	@Autowired
	private DocRemarkService service;

	@Autowired
	private BPAUtil bpaUtil;

	@Autowired
	private ResponseInfoFactory responseInfoFactory;

	@PostMapping(value = "/_create")
	public ResponseEntity<DocRemarkResponse> create(@Valid @RequestBody DocRemarkRequest request) {
		bpaUtil.defaultJsonPathConfig();
		DocRemark docRemark = service.create(request);
		List<DocRemark> docRemarks = new ArrayList<DocRemark>();
		docRemarks.add(docRemark);
		DocRemarkResponse response = DocRemarkResponse.builder().docRemarks(docRemarks)
				.responseInfo(responseInfoFactory
						.createResponseInfoFromRequestInfo(request.getRequestInfo(), true))
				.build();
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	@PostMapping(value = "/_search")
	public ResponseEntity<DocRemarkResponse> search(@Valid @RequestBody RequestInfoWrapper requestInfoWrapper,
			@Valid @ModelAttribute DocRemarkSearchCriteria criteria) {
		List<DocRemark> docRemarks = service.search(criteria);
		DocRemarkResponse response = DocRemarkResponse.builder().docRemarks(docRemarks).responseInfo(
				responseInfoFactory.createResponseInfoFromRequestInfo(requestInfoWrapper.getRequestInfo(), true))
				.build();
		return new ResponseEntity<>(response, HttpStatus.OK);

	}

	@PostMapping(value = "/_update")
	public ResponseEntity<DocRemarkResponse> update(@Valid @RequestBody DocRemarkRequest request) {
		DocRemark docRemark = service.update(request);
		List<DocRemark> docRemarks = new ArrayList<DocRemark>();
		docRemarks.add(docRemark);
		DocRemarkResponse response = DocRemarkResponse.builder().docRemarks(docRemarks)
				.responseInfo(responseInfoFactory.createResponseInfoFromRequestInfo(request.getRequestInfo(), true))
				.build();
		return new ResponseEntity<>(response, HttpStatus.OK);

	}

}
