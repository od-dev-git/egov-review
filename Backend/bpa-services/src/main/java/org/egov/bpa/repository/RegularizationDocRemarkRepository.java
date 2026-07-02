package org.egov.bpa.repository;

import java.util.ArrayList;
import java.util.List;

import org.egov.bpa.config.BPAConfiguration;
import org.egov.bpa.producer.Producer;
import org.egov.bpa.repository.querybuilder.RegularizationQueryBuilder;
import org.egov.bpa.repository.rowmapper.RegularizationDocRemarkMapper;
import org.egov.bpa.web.model.regularization.RegularizationDocRemark;
import org.egov.bpa.web.model.regularization.RegularizationDocRemarkRequest;
import org.egov.bpa.web.model.regularization.RegularizationDocRemarkSearchCriteria;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class RegularizationDocRemarkRepository {

	@Autowired
	private BPAConfiguration config;

	@Autowired
	private Producer producer;

	@Autowired
	private RegularizationQueryBuilder queryBuilder;

	@Autowired
	private RegularizationDocRemarkMapper rowMapper;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	
	/**
	 * Send message to kafka save topic from here
	 * @param docRemarkRequest
	 */
	public void save(RegularizationDocRemarkRequest docRemarkRequest) {
		producer.push(config.getSaveRegularizationDocRemarkTopic(), docRemarkRequest);
	}

	
	
	/**
	 * Send message to kafka update topic from here
	 * @param docRemarkRequest
	 */
	public void update(RegularizationDocRemarkRequest docRemarkRequest) {
		producer.push(config.getUpdateRegularizationDocRemarkTopic(), docRemarkRequest);
	}

	
	
	/**
	 * Get Doc Remark from DB
	 * @param searchCriteria
	 * @return List<RegularizationDocRemark>
	 */
	public List<RegularizationDocRemark> getRegularizationDocRemark(RegularizationDocRemarkSearchCriteria searchCriteria) {
		List<Object> preparedStmtList = new ArrayList<>();
		String query = queryBuilder.regularizationDocRemarkSearchQuery(searchCriteria, preparedStmtList);
		List<RegularizationDocRemark> docRemark = jdbcTemplate.query(query, preparedStmtList.toArray(), rowMapper);
		return docRemark;
	}


}
