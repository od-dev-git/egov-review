package org.egov.bpa.calculator.repository;

import java.util.ArrayList;
import java.util.List;

import org.egov.bpa.calculator.repository.querybuilder.RevisionQueryBuilder;
import org.egov.bpa.calculator.repository.rowmapper.RevisionRowMapper;
import org.egov.bpa.calculator.web.models.Revision;
import org.egov.bpa.calculator.web.models.RevisionSearchCriteria;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class RevisionRepository {

	@Autowired
	private RevisionQueryBuilder queryBuilder;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private RevisionRowMapper rowMapper;


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

}
