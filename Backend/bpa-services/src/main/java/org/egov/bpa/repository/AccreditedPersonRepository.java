package org.egov.bpa.repository;

import java.util.ArrayList;
import java.util.List;

import org.egov.bpa.config.BPAConfiguration;
import org.egov.bpa.producer.Producer;
import org.egov.bpa.repository.querybuilder.AccreditedPersonQueryBuilder;
import org.egov.bpa.repository.rowmapper.AccreditedPersonRowMapper;
import org.egov.bpa.web.model.accreditedperson.AccreditedPerson;
import org.egov.bpa.web.model.accreditedperson.AccreditedPersonRequest;
import org.egov.bpa.web.model.accreditedperson.AccreditedPersonSearchCriteria;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AccreditedPersonRepository {

	@Autowired
	private BPAConfiguration config;

	@Autowired
	private Producer producer;

	@Autowired
	private AccreditedPersonQueryBuilder queryBuilder;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private AccreditedPersonRowMapper rowMapper;

	/**
	 * Pushes the request on save topic through kafka
	 *
	 * @param AccreditedPersonRequest The Accredited Person create request
	 */
	public void save(AccreditedPersonRequest accreditedPersonRequest) {
		producer.push(config.getSaveAccreditedPersonTopicName(), accreditedPersonRequest);
	}

	/**
	 * Accredited Person search in database
	 *
	 * @param criteria The Accredited Person Search criteria
	 * @return List of Accredited Person from search
	 */
	public List<AccreditedPerson> getAccreditedPersonData(AccreditedPersonSearchCriteria criteria) {
		List<Object> preparedStmtList = new ArrayList<>();
		String query = queryBuilder.getAccreditedPersonSearchQuery(criteria, preparedStmtList);
		List<AccreditedPerson> accreditedPersons = jdbcTemplate.query(query, preparedStmtList.toArray(), rowMapper);
		return accreditedPersons;
	}
	
	/**
	 * pushes the request on update topic through kafka
	 * 
	 * @param revisionRequest
	 */
	/*
	public void update(RevisionRequest revisionRequest) {
		producer.push(config.getUpdateRevisionTopicName(), revisionRequest);
	}
	*/

}
