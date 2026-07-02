package org.egov.bpa.web.controller;

import java.util.ArrayList;
import java.util.List;

import javax.validation.Valid;

import org.egov.bpa.service.AccreditedPersonService;
import org.egov.bpa.util.BPAUtil;
import org.egov.bpa.util.ResponseInfoFactory;
import org.egov.bpa.web.model.Revision;
import org.egov.bpa.web.model.RevisionRequest;
import org.egov.bpa.web.model.RevisionResponse;
import org.egov.bpa.web.model.accreditedperson.AccreditedPerson;
import org.egov.bpa.web.model.accreditedperson.AccreditedPersonRequest;
import org.egov.bpa.web.model.accreditedperson.AccreditedPersonResponse;
import org.egov.bpa.web.model.accreditedperson.AccreditedPersonSearchCriteriaWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/accreditedperson")
public class AccreditedPersonController {

	@Autowired
	private AccreditedPersonService accreditedPersonService;

	@Autowired
	private BPAUtil bpaUtil;

	@Autowired
	private ResponseInfoFactory responseInfoFactory;

	@PostMapping(value = "/_search")
	public ResponseEntity<AccreditedPersonResponse> search(
			@Valid @RequestBody AccreditedPersonSearchCriteriaWrapper criteria) {
		List<AccreditedPerson> accreditedPersons = accreditedPersonService
				.getAccreditedPersonsFromCriteria(criteria.getAccreditedPersonSearchCriteria());
		AccreditedPersonResponse response = AccreditedPersonResponse.builder().accreditedPersons(accreditedPersons)
				.responseInfo(responseInfoFactory.createResponseInfoFromRequestInfo(criteria.getRequestInfo(), true))
				.build();
		return new ResponseEntity<>(response, HttpStatus.OK);
	}
	
	@PostMapping(value = "/_create")
	public ResponseEntity<AccreditedPersonResponse> create(
			@Valid @RequestBody AccreditedPersonRequest accreditedPersonRequest) {
		bpaUtil.defaultJsonPathConfig();
		AccreditedPerson accreditedPerson = accreditedPersonService.create(accreditedPersonRequest);
		List<AccreditedPerson> accreditedPersons = new ArrayList<AccreditedPerson>();
		accreditedPersons.add(accreditedPerson);
		AccreditedPersonResponse response = AccreditedPersonResponse.builder().accreditedPersons(accreditedPersons)
				.responseInfo(responseInfoFactory
						.createResponseInfoFromRequestInfo(accreditedPersonRequest.getRequestInfo(), true))
				.build();
		return new ResponseEntity<>(response, HttpStatus.OK);
	}
}
