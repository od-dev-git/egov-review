package org.egov.bpa.repository;

import java.util.ArrayList;
import java.util.List;

import org.egov.bpa.config.BPAConfiguration;
import org.egov.bpa.producer.Producer;
import org.egov.bpa.repository.querybuilder.RevisionQueryBuilder;
import org.egov.bpa.repository.rowmapper.RevisionRowMapper;
import org.egov.bpa.web.model.Revision;
import org.egov.bpa.web.model.RevisionRequest;
import org.egov.bpa.web.model.RevisionSearchCriteria;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class RevisionRepository {

	@Autowired
	private BPAConfiguration config;

	@Autowired
	private Producer producer;

	@Autowired
	private RevisionQueryBuilder queryBuilder;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private RevisionRowMapper rowMapper;

	/**
	 * Pushes the request on save topic through kafka
	 *
	 * @param preapprovedPlanRequest The PreapprovedPlanRequest create request
	 */
	public void save(RevisionRequest revisionRequest) {
		producer.push(config.getSaveRevisionTopicName(), revisionRequest);
	}

	/**
	 * Revision search in database
	 *
	 * @param criteria The Revision Search criteria
	 * @return List of Revision from search
	 */
	public List<Revision> getRevisionData(RevisionSearchCriteria criteria) {
		List<Object> preparedStmtList = new ArrayList<>();
		String query = queryBuilder.getRevisionSearchQuery(criteria, preparedStmtList);
		List<Revision> revisions = jdbcTemplate.query(query, preparedStmtList.toArray(), rowMapper);
		return revisions;
	}
	
	/**
	 * pushes the request on update topic through kafka
	 * 
	 * @param revisionRequest
	 */
	public void update(RevisionRequest revisionRequest) {
		producer.push(config.getUpdateRevisionTopicName(), revisionRequest);
	}

	public String getRevisionApplicationStatus(String bpaApplicationNo) {
		List<Object> preparedStmtList = new ArrayList<>();
		String query = queryBuilder.getRevisionApplicationStatusSearchQuery(bpaApplicationNo, preparedStmtList);
		String status = jdbcTemplate.queryForObject(query,preparedStmtList.toArray(), String.class);
		return status;
	}

}
