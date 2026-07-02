package org.egov.bpa.service;

import java.util.Collections;
import java.util.List;

import org.egov.bpa.config.BPAConfiguration;
import org.egov.bpa.repository.AccreditedPersonRepository;
import org.egov.bpa.util.BPAUtil;
import org.egov.bpa.web.model.accreditedperson.AccreditedPerson;
import org.egov.bpa.web.model.accreditedperson.AccreditedPersonRequest;
import org.egov.bpa.web.model.accreditedperson.AccreditedPersonSearchCriteria;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class AccreditedPersonService {

	@Autowired
	private AccreditedPersonRepository repository;

	@Autowired
	private BPAUtil util;

	@Autowired
	private BPAConfiguration config;

	@Autowired
	EnrichmentService enrichmentService;

	/**
	 * does all the validations required to create BPA Record in the system
	 * 
	 * @param bpaRequest
	 * @return
	 */
	public AccreditedPerson create(AccreditedPersonRequest accreditedPersonRequest) {
		enrichmentService.enrichAccreditedPersonCreateRequest(accreditedPersonRequest);

		repository.save(accreditedPersonRequest);
		return accreditedPersonRequest.getAccreditedPerson();
	}

	/**
	 * Returns the accredited persons list
	 * 
	 * @param criteria    The object containing the parameters on which to search
	 * @param requestInfo The search request's requestInfo
	 * @return List of accredited persons for the given criteria
	 */
	public List<AccreditedPerson> getAccreditedPersonsFromCriteria(AccreditedPersonSearchCriteria criteria) {
		List<AccreditedPerson> accreditedPersons = repository.getAccreditedPersonData(criteria);
		if (accreditedPersons.isEmpty())
			return Collections.emptyList();
		return accreditedPersons;
	}

}
