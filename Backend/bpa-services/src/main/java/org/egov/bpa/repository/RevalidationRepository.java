package org.egov.bpa.repository;

import java.util.ArrayList;
import java.util.List;

import org.egov.bpa.config.BPAConfiguration;
import org.egov.bpa.producer.Producer;
import org.egov.bpa.repository.querybuilder.RevalidationQueryBuilder;
import org.egov.bpa.repository.rowmapper.RevalidationRowMapper;
import org.egov.bpa.web.model.Revalidation;
import org.egov.bpa.web.model.RevalidationSearchCriteria;
import org.egov.bpa.web.model.Revalidation;
import org.egov.bpa.web.model.RevalidationRequest;
import org.egov.bpa.web.model.RevalidationSearchCriteria;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class RevalidationRepository {

	@Autowired
	private BPAConfiguration config;

	@Autowired
	private Producer producer;

	@Autowired
	private RevalidationQueryBuilder queryBuilder;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private RevalidationRowMapper rowMapper;

	/**
	 * Pushes the request on save topic through kafka
	 *
	 * @param preapprovedPlanRequest The PreapprovedPlanRequest create request
	 */
	public void save(RevalidationRequest revalidationRequest) {
		producer.push(config.getSaveRevalidationTopicName(), revalidationRequest);
	}

	/**
	 * Revalidation search in database
	 *
	 * @param criteria The Revalidation Search criteria
	 * @return List of Revalidation from search
	 */
	public List<Revalidation> getRevalidationData(RevalidationSearchCriteria criteria) {
		List<Object> preparedStmtList = new ArrayList<>();
		String query = queryBuilder.getrevalidationSearchQuery(criteria, preparedStmtList);
		List<Revalidation> revalidation = jdbcTemplate.query(query, preparedStmtList.toArray(), rowMapper);
		return revalidation;
	}
	
	/**
	 * pushes the request on update topic through kafka
	 * 
	 * @param revalidationRequest
	 */
	public void update(RevalidationRequest revalidationRequest) {
		producer.push(config.getUpdateRevalidationTopicName(), revalidationRequest);
	}

}
