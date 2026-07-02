package org.egov.bpa.web.controller;

import org.egov.bpa.service.issuefix.IIssueFixService;
import org.egov.bpa.service.issuefix.IssueFixService;
import org.egov.bpa.util.IssueFixConstants;
import org.egov.bpa.util.ResponseInfoFactory;
import org.egov.bpa.web.model.RequestInfoWrapper;
import org.egov.bpa.web.model.issuefix.IssueFix;
import org.egov.bpa.web.model.issuefix.IssueFixRequest;
import org.egov.bpa.web.model.issuefix.IssueFixResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/v1/bpa")
public class IssueFixController {

    @Autowired
    @Qualifier("issueFixService")
    private IssueFixService issueFixService;

    @Autowired
    private ResponseInfoFactory responseInfoFactory;


	@PostMapping(value = "/_issuefix")
	public ResponseEntity<IssueFixResponse> fixIssue(@Valid @RequestBody IssueFixRequest issueFixRequest) {

		IssueFix issueFix = issueFixService.issueFix(issueFixRequest);
		IssueFixResponse issueFixResponse = IssueFixResponse.builder()
				.responseInfo(
						responseInfoFactory.createResponseInfoFromRequestInfo(issueFixRequest.getRequestInfo(), true))
				.issueFix(issueFix).status(IssueFixConstants.FIXED).build();
		return new ResponseEntity<>(issueFixResponse, HttpStatus.OK);
	}
    
	@PostMapping(value = "/_revenueVillage/_search")
	public ResponseEntity<IssueFixResponse> getRevenueVillages(@Valid @RequestBody IssueFixRequest issueFixRequest) {
		IssueFix issueFix = issueFixService.getRevenueVillages(issueFixRequest);
		IssueFixResponse issueFixResponse = IssueFixResponse.builder()
				.responseInfo(
						responseInfoFactory.createResponseInfoFromRequestInfo(issueFixRequest.getRequestInfo(), true))
				.issueFix(issueFix).status(IssueFixConstants.FIXED).build();
		return new ResponseEntity<>(issueFixResponse, HttpStatus.OK);
	}
	
	@PostMapping(value = "/_automatePaymentIssueFix")
	public ResponseEntity<IssueFixResponse> automatePaymentIssueFix(@Valid @RequestBody RequestInfoWrapper requestInfoWrapper) {
        issueFixService.automatePaymentIssueFix(requestInfoWrapper.getRequestInfo());		
		return new ResponseEntity<>(HttpStatus.OK);
	}
	
	@PostMapping(value = "/_automateStatusMismatchIssueFix")
	public ResponseEntity<IssueFixResponse> automateStatusMismatchIssueFix(@Valid @RequestBody RequestInfoWrapper requestInfoWrapper) {
        issueFixService.automateStatusMismatchIssueFix(requestInfoWrapper.getRequestInfo());		
		return new ResponseEntity<>(HttpStatus.OK);
	}
}
