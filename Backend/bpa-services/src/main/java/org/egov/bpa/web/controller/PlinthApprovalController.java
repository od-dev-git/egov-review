package org.egov.bpa.web.controller;

import java.util.ArrayList;
import java.util.List;

import javax.validation.Valid;

import org.egov.bpa.service.BPAService;
import org.egov.bpa.util.BPAUtil;
import org.egov.bpa.util.ResponseInfoFactory;
import org.egov.bpa.web.model.PlanningAssistantChecklist;
import org.egov.bpa.web.model.PlanningAssistantChecklistResponse;
import org.egov.bpa.web.model.PlanningAssistantSearchCriteria;
import org.egov.bpa.web.model.PlinthApproval;
import org.egov.bpa.web.model.PlinthApprovalRequest;
import org.egov.bpa.web.model.PlinthApprovalResponse;
import org.egov.bpa.web.model.PlinthApprovalSearchCriteria;
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
@RequestMapping("/v1/plinthapproval")
public class PlinthApprovalController {

	@Autowired
	private BPAService bpaService;

	@Autowired
	private BPAUtil bpaUtil;

	@Autowired
	private ResponseInfoFactory responseInfoFactory;

	@PostMapping(value = "/_create")
	public ResponseEntity<PlinthApprovalResponse> create(@Valid @RequestBody PlinthApprovalRequest request) {
		PlinthApproval pa = bpaService.createPlinthApproval(request);
		List<PlinthApproval> pas = new ArrayList<PlinthApproval>();
		pas.add(pa);
		PlinthApprovalResponse response = PlinthApprovalResponse.builder().plinthApproval(pas)
				.responseInfo(responseInfoFactory.createResponseInfoFromRequestInfo(request.getRequestInfo(), true))
				.build();
		return new ResponseEntity<>(response, HttpStatus.OK);

	}
	
	@PostMapping(value = "/_search")
	public ResponseEntity<PlinthApprovalResponse> search(@Valid @RequestBody RequestInfoWrapper requestInfoWrapper,
			@Valid @ModelAttribute PlinthApprovalSearchCriteria criteria) {
		List<PlinthApproval> pla = bpaService.serachPlinthApproval(criteria, requestInfoWrapper.getRequestInfo());
		PlinthApprovalResponse response = PlinthApprovalResponse.builder().plinthApproval(pla).responseInfo(
				responseInfoFactory.createResponseInfoFromRequestInfo(requestInfoWrapper.getRequestInfo(), true))
				.build();
		return new ResponseEntity<>(response, HttpStatus.OK);

	}
	
	@PostMapping(value = "/_update")
	public ResponseEntity<PlinthApprovalResponse> update(@Valid @RequestBody PlinthApprovalRequest request) {
		PlinthApproval pa = bpaService.updatePlinthApproval(request);
		List<PlinthApproval> pas = new ArrayList<PlinthApproval>();
		pas.add(pa);
		PlinthApprovalResponse response = PlinthApprovalResponse.builder().plinthApproval(pas)
				.responseInfo(responseInfoFactory.createResponseInfoFromRequestInfo(request.getRequestInfo(), true))
				.build();
		return new ResponseEntity<>(response, HttpStatus.OK);

	}

}
