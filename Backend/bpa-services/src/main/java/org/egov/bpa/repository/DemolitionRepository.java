package org.egov.bpa.repository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.validation.Valid;

import org.egov.bpa.config.BPAConfiguration;
import org.egov.bpa.producer.Producer;
import org.egov.bpa.repository.querybuilder.DemolitionQueryBuilder;
import org.egov.bpa.repository.rowmapper.BPAVillageRowmapper;
import org.egov.bpa.repository.rowmapper.DemolitionApprovedByDocListRowMapper;
import org.egov.bpa.repository.rowmapper.DemolitionApprovedByMeRowMapper;
import org.egov.bpa.repository.rowmapper.DemolitionFIDetailsRowMapper;
import org.egov.bpa.repository.rowmapper.DemolitionRowMapper;
import org.egov.bpa.web.model.BPAVillage;
import org.egov.bpa.web.model.VillageSearchCriteria;
import org.egov.bpa.web.model.demolition.Demolition;
import org.egov.bpa.web.model.demolition.DemolitionApprovedByApplicationSearch;
import org.egov.bpa.web.model.demolition.DemolitionDocumentList;
import org.egov.bpa.web.model.demolition.DemolitionFISearchCriteria;
import org.egov.bpa.web.model.demolition.DemolitionFieldInspection;
import org.egov.bpa.web.model.demolition.DemolitionFieldInspectionRequest;
import org.egov.bpa.web.model.demolition.DemolitionRequest;
import org.egov.bpa.web.model.demolition.DemolitionSearchCriteria;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Repository
public class DemolitionRepository {
	
	@Autowired
	private BPAConfiguration config;

	@Autowired
	private Producer producer;
	
	@Autowired
	private JdbcTemplate jdbcTemplate;
	
	@Autowired
	private DemolitionRowMapper demolitionRowMapper;
	
	@Autowired
	private DemolitionQueryBuilder queryBuilder;
	
	@Autowired
	DemolitionFIDetailsRowMapper demolitionFIRowmapper;
	
	/**
	 * Send message to kafka save topic from here Save-topic: save-bd-application
	 * 
	 * @param demolitoinRequest
	 */
	public void save(DemolitionRequest demolitoinRequest) {
		producer.push(config.getSaveDemolitionTopic(), demolitoinRequest);
	}

	public List<Demolition> getDemolitions(@Valid DemolitionSearchCriteria criteria) {
		List<Object> preparedStmtList = new ArrayList<>();
		String query = queryBuilder.getDemolitionSearchQuery(criteria, preparedStmtList);
		List<Demolition> demolitions = jdbcTemplate.query(query, preparedStmtList.toArray(),
				demolitionRowMapper);
		return demolitions;
	}

	/**
	 * Send message to kafka update topic from here update-topic: update-bd-application
	 * 
	 * @param demolitoinRequest
	 */
	public void update(@Valid DemolitionRequest demolitoinRequest) {
		producer.push(config.getUpdateDemolitionTopic(), demolitoinRequest);
		
	}

	public List<BPAVillage> getDemolitionVillageData(@Valid VillageSearchCriteria criteria) {

		List<Object> preparedStmtList = new ArrayList<>();

		// get query from query builder here
		String queryToGetVillageData = queryBuilder.getVillageDataQuery(criteria, preparedStmtList);

		log.info("Query for Village Search ::: " + queryToGetVillageData);

		List<BPAVillage> villageData = jdbcTemplate.query(queryToGetVillageData, preparedStmtList.toArray(),
				new BPAVillageRowmapper());

		return villageData;
	}

	public List<DemolitionApprovedByApplicationSearch> getApprovedbyData(String uuid,
			@Valid DemolitionSearchCriteria criteria) {
		
		List<Object> preparedStmtList = new ArrayList<>();

		String query = queryBuilder.getApplicationAprovedBy(uuid, preparedStmtList, criteria);
		log.info("query inside method getApprovedbyData:" + query);
		log.info("prepareStmtList:" + preparedStmtList);
		List<DemolitionApprovedByApplicationSearch> demolitionData = jdbcTemplate.query(query, preparedStmtList.toArray(),
				new DemolitionApprovedByMeRowMapper());

		return demolitionData;
	}

	public List<DemolitionDocumentList> getdocumentDataForApproveBy(List<String> demolitionIds) {

		List<Object> preparedStmtList = new ArrayList<>();
		String query = queryBuilder.getDocumentApprovedBy(demolitionIds, preparedStmtList);

		List<DemolitionDocumentList> docData = jdbcTemplate.query(query, preparedStmtList.toArray(),
				new DemolitionApprovedByDocListRowMapper());

		return docData;
	}

	/**
	 * Push data to kafka for permit letter preview update API
	 * 
	 * @param demolitionRequest
	 */
	public void updatePermitLetterRequest(@Valid DemolitionRequest demolitionRequest) {
		producer.push(config.getUpdateDemolitionPermitLetterPreview(), demolitionRequest);	
	}
	
	public void savefieldInspectionReport(@Valid DemolitionFieldInspectionRequest request) {
		producer.push(config.getSaveDemolitionFieldInspectionTopicName(), request);
	}
	
	public List<DemolitionFieldInspection> getfieldInspectionReport(
			@Valid DemolitionFISearchCriteria criteria) {
		List<Object> preparedStmtList = new ArrayList<>();
		String query = queryBuilder.getfieldinspectionReportDetails(criteria, preparedStmtList);
		List<DemolitionFieldInspection> report = jdbcTemplate.query(query, preparedStmtList.toArray(),
				new DemolitionFIDetailsRowMapper());
		return report;
	}

	public void updateFieldInspectionReport(@Valid DemolitionFieldInspectionRequest request) {
		producer.push(config.getUpdateDemolitionFieldInspectionTopicName(), request);
		
	}


}
