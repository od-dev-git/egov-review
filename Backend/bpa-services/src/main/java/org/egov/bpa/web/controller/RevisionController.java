package org.egov.bpa.web.controller;

import java.util.ArrayList;
import java.util.List;

import javax.validation.Valid;

import org.egov.bpa.service.RevisionService;
import org.egov.bpa.util.BPAUtil;
import org.egov.bpa.util.ResponseInfoFactory;
import org.egov.bpa.web.model.Revision;
import org.egov.bpa.web.model.RevisionRequest;
import org.egov.bpa.web.model.RevisionResponse;
import org.egov.bpa.web.model.RevisionSearchCriteriaWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/revision")
public class RevisionController {

	@Autowired
	private RevisionService revisionService;

	@Autowired
	private BPAUtil bpaUtil;

	@Autowired
	private ResponseInfoFactory responseInfoFactory;

	@PostMapping(value = "/_create")
	public ResponseEntity<RevisionResponse> create(
			@Valid @RequestBody RevisionRequest revisionRequest) {
		bpaUtil.defaultJsonPathConfig();
		Revision revision = revisionService.create(revisionRequest);
		List<Revision> revisions = new ArrayList<Revision>();
		revisions.add(revision);
		RevisionResponse response = RevisionResponse.builder().revision(revisions)
				.responseInfo(responseInfoFactory
						.createResponseInfoFromRequestInfo(revisionRequest.getRequestInfo(), true))
				.build();
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	@PostMapping(value = "/_search")
	public ResponseEntity<RevisionResponse> search(@Valid @RequestBody RevisionSearchCriteriaWrapper criteria) {

		List<Revision> revision = revisionService.getRevisionFromCriteria(criteria.getRevisionSearchCriteria());

		RevisionResponse response = RevisionResponse.builder().revision(revision)
				.responseInfo(responseInfoFactory.createResponseInfoFromRequestInfo(criteria.getRequestInfo(), true))
				.build();
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	@PostMapping(value = "/_update")
	public ResponseEntity<RevisionResponse> update(
			@Valid @RequestBody RevisionRequest revisionRequest) {
		Revision revision = revisionService.update(revisionRequest);
		List<Revision> revisions = new ArrayList<Revision>();
		revisions.add(revision);
		RevisionResponse response = RevisionResponse.builder().revision(revisions)
				.responseInfo(responseInfoFactory
						.createResponseInfoFromRequestInfo(revisionRequest.getRequestInfo(), true))
				.build();
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

}
