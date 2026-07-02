package org.egov.bpa.repository;

import java.util.ArrayList;
import java.util.List;

import javax.validation.Valid;

import org.egov.bpa.config.BPAConfiguration;
import org.egov.bpa.producer.Producer;
import org.egov.bpa.repository.querybuilder.PreapprovedPlanQueryBuilder;
import org.egov.bpa.repository.querybuilder.ScnQueryBuilder;
import org.egov.bpa.repository.rowmapper.NoticeMapper;

import org.egov.bpa.web.model.Notice;
import org.egov.bpa.web.model.NoticeRequest;
import org.egov.bpa.web.model.NoticeSearchCriteria;
import org.egov.bpa.web.model.PreapprovedPlanRequest;




import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import lombok.extern.slf4j.Slf4j;


@Slf4j
@Repository
public class ScnRepository {
	
	@Autowired
	private BPAConfiguration config;

	@Autowired
	private Producer producer;

	@Autowired
	private ScnQueryBuilder queryBuilder;
	
	@Autowired
	private NoticeMapper rowMapper;

	@Autowired
	private JdbcTemplate jdbcTemplate;
	
	
	
	/**
	 * Pushes the request on save topic through kafka
	 *
	 * @param ScnRequest The ScnRequest create request
	 */
	public void save(NoticeRequest noticeRequest) {
		producer.push(config.getSavenoticeTopicName(), noticeRequest);
	}

	public void update(NoticeRequest noticeRequest) {
		producer.push(config.getUpdatenoticeTopicName(), noticeRequest);
	}


	public List<Notice> getNoticeData(@Valid NoticeSearchCriteria scnSearchCriteria) {
		List<Object> preparedStmtList = new ArrayList<>();
		String query = queryBuilder.getNoticeSearchQuery(scnSearchCriteria,preparedStmtList);
		log.info("Notice Search Query : "+query);
		log.info("preparedStmtList : "+preparedStmtList);
		List<Notice> scnNotice = jdbcTemplate.query(query, preparedStmtList.toArray(), rowMapper);
		//System.out.println(scnNotice);
		return scnNotice;
		
	}
	
	/**
	 * Search regularization notice from DB 
	 * 
	 * @param scnSearchCriteria
	 * @return
	 */
	public List<Notice> getNoticeDataForRegularization(@Valid NoticeSearchCriteria scnSearchCriteria) {
		List<Object> preparedStmtList = new ArrayList<>();
		String query = queryBuilder.getNoticeSearchQueryForRegularization(scnSearchCriteria,preparedStmtList);
		List<Notice> scnNotice = jdbcTemplate.query(query, preparedStmtList.toArray(), rowMapper);
		return scnNotice;
		
	}

	/**
	 * push the regularization notice request to kafka
	 * 
	 * @param request
	 */
	public void saveForRegularization(@Valid NoticeRequest request) {
		producer.push(config.getSaveRegularizationNotice(), request);	
	}
	
	/**
	 * update the regularization notice request through kafka
	 * 
	 * @param request
	 */
	public void updateForRegularization(NoticeRequest noticeRequest) {
		producer.push(config.getUpdateRegularizationNotice(), noticeRequest);
	}

}
