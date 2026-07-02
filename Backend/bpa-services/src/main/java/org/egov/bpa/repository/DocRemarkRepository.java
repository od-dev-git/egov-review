package org.egov.bpa.repository;

import java.util.ArrayList;
import java.util.List;

import javax.validation.Valid;

import org.egov.bpa.config.BPAConfiguration;
import org.egov.bpa.producer.Producer;
import org.egov.bpa.repository.querybuilder.DocRemarkQueryBuilder;
import org.egov.bpa.repository.querybuilder.ScnQueryBuilder;
import org.egov.bpa.repository.rowmapper.DocRemarkMapper;
import org.egov.bpa.repository.rowmapper.NoticeMapper;
import org.egov.bpa.web.model.DocRemark;
import org.egov.bpa.web.model.DocRemarkRequest;
import org.egov.bpa.web.model.DocRemarkSearchCriteria;
import org.egov.bpa.web.model.Notice;
import org.egov.bpa.web.model.NoticeRequest;
import org.egov.bpa.web.model.NoticeSearchCriteria;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class DocRemarkRepository {

	@Autowired
	private BPAConfiguration config;

	@Autowired
	private Producer producer;

	@Autowired
	private DocRemarkQueryBuilder queryBuilder;

	@Autowired
	private DocRemarkMapper rowMapper;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	public void save(DocRemarkRequest docRemarkRequest) {
		producer.push(config.getSaveDocRemarkTopicName(), docRemarkRequest);
	}

	public void update(DocRemarkRequest docRemarkRequest) {
		producer.push(config.getUpdateDocRemarkTopicName(), docRemarkRequest);
	}

	public List<DocRemark> getDocRemark(DocRemarkSearchCriteria criteria) {
		List<Object> preparedStmtList = new ArrayList<>();
		String query = queryBuilder.docRemarkSearchQuery(criteria, preparedStmtList);
		List<DocRemark> docRemark = jdbcTemplate.query(query, preparedStmtList.toArray(), rowMapper);
		return docRemark;

	}

}
