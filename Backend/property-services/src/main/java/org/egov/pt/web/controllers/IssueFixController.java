package org.egov.pt.web.controllers;



import org.egov.pt.service.issuefix.IssueFixService;
import org.egov.pt.util.IssueFixConstants;
import org.egov.pt.util.ResponseInfoFactory;
import org.egov.pt.web.contracts.RequestInfoWrapper;
import org.egov.pt.models.issuefix.IssueFix;
import org.egov.pt.models.issuefix.IssueFixRequest;
import org.egov.pt.models.issuefix.IssueFixResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/property")
public class IssueFixController {

    @Autowired
    @Qualifier("issueFixService")
    private IssueFixService issueFixService;

    @Autowired
    private ResponseInfoFactory responseInfoFactory;


    @PostMapping(value = "/_issuefix")
    public ResponseEntity<IssueFixResponse> fixIssue(@Valid @RequestBody IssueFixRequest issueFixRequest,
                                                     @RequestHeader HttpHeaders headers){

        IssueFix issueFix=issueFixService.issueFix(issueFixRequest,headers);
        IssueFixResponse issueFixResponse= IssueFixResponse.builder()
        		.responseInfo(responseInfoFactory.createResponseInfoFromRequestInfo(issueFixRequest.getRequestInfo(), true))
        		.issueFix(issueFix)
        		.status(IssueFixConstants.FIXED)
        		.build();
        return new ResponseEntity<>(issueFixResponse, HttpStatus.OK);
    }
    
    @PostMapping(value = "/_automateMutationPaymentIssueFix")
	public ResponseEntity<IssueFixResponse> automatePaymentIssueFix(@RequestBody RequestInfoWrapper requestInfoWrapper) {
        issueFixService.automatePaymentIssueFix(requestInfoWrapper.getRequestInfo());		
		return new ResponseEntity<>(HttpStatus.OK);
	}
    
	@PostMapping(value = "/_automateStatusMismatchIssueFix")
	public ResponseEntity<IssueFixResponse> automateStatusMismatchIssueFix(@Valid @RequestBody RequestInfoWrapper requestInfoWrapper) {
        issueFixService.automateStatusMismatchIssueFix(requestInfoWrapper.getRequestInfo());		
		return new ResponseEntity<>(HttpStatus.OK);
	}
}
