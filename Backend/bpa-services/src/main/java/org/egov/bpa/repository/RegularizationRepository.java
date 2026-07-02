package org.egov.bpa.repository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.validation.Valid;

import org.egov.bpa.config.BPAConfiguration;
import org.egov.bpa.producer.Producer;
import org.egov.bpa.repository.querybuilder.RegularizationQueryBuilder;
import org.egov.bpa.repository.rowmapper.FeePendingApplicationRowMapper;
import org.egov.bpa.repository.rowmapper.RegularizationApprovedByDocListRowMapper;
import org.egov.bpa.repository.rowmapper.RegularizationApprovedByRowMapper;
import org.egov.bpa.repository.rowmapper.RegularizationDraftRowMapper;
import org.egov.bpa.repository.rowmapper.RegularizationDscDetailsRowMapper;
import org.egov.bpa.repository.rowmapper.RegularizationFIDetailsRowMapper;
import org.egov.bpa.repository.rowmapper.RegularizationRowMapper;
import org.egov.bpa.repository.rowmapper.RegularizationVillageRowmapper;
import org.egov.bpa.web.model.BPASearchCriteria;
import org.egov.bpa.web.model.FeePendingApplication;
import org.egov.bpa.web.model.RegularizationDraft;
import org.egov.bpa.web.model.RegularizationDraftRequest;
import org.egov.bpa.web.model.RegularizationDraftSearchCriteria;
import org.egov.bpa.web.model.VillageSearchCriteria;
import org.egov.bpa.web.model.regularization.Regularization;
import org.egov.bpa.web.model.regularization.RegularizationApprovedByApplicationSearch;
import org.egov.bpa.web.model.regularization.RegularizationDocUploadRequest;
import org.egov.bpa.web.model.regularization.RegularizationDocumentList;
import org.egov.bpa.web.model.regularization.RegularizationDscDetails;
import org.egov.bpa.web.model.regularization.RegularizationFISearchCriteria;
import org.egov.bpa.web.model.regularization.RegularizationFieldInspection;
import org.egov.bpa.web.model.regularization.RegularizationFieldInspectionRequest;
import org.egov.bpa.web.model.regularization.RegularizationRequest;
import org.egov.bpa.web.model.regularization.RegularizationSearchCriteria;
import org.egov.bpa.web.model.regularization.RegularizationVillage;
import org.egov.common.contract.request.RequestInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.util.CollectionUtils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Repository
public class RegularizationRepository {
	
	@Autowired
	private BPAConfiguration config;

	@Autowired
	private Producer producer;
	
	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private RegularizationRowMapper rowMapper;
	
	@Autowired
	private RegularizationDscDetailsRowMapper dscDetailsRowMapper;
	
	@Autowired
	private RegularizationQueryBuilder queryBuilder;
	
	@Autowired
	private RegularizationDraftRowMapper regularizationDraftRowMapper;
	
	/**
	 * Send message to kafka save topic from here Save-topic: save-blr-application
	 * 
	 * @param regularizationRequest
	 */
	public void save(RegularizationRequest regularizationRequest) {
		producer.push(config.getSaveRegularizationTopic(), regularizationRequest);
	}

	/**
	 * Send message to kafka update topic from here Update-topic: update-blr-application
	 * 
	 * @param regularizationRequest
	 */
	public void update(RegularizationRequest regularizationRequest, boolean isStateUpdatable) {
		RequestInfo requestInfo = regularizationRequest.getRequestInfo();

		Regularization regularizationForStatusUpdate = null;
		Regularization regularizationForUpdate = null;

		Regularization regularization = regularizationRequest.getRegularization();

		if (isStateUpdatable) {
			regularizationForUpdate = regularization;
		} else {
			regularizationForStatusUpdate = regularization;
		}
		if (regularizationForUpdate != null)
			producer.push(config.getUpdateRegularizationTopic(),
					new RegularizationRequest(requestInfo, regularizationForUpdate));

		if (regularizationForStatusUpdate != null)
			producer.push(config.getUpdateRegularizationTopic(),
					new RegularizationRequest(requestInfo, regularizationForStatusUpdate));

	}
	

	/**
	 * Repository for searching Regularizations from db
	 * 
	 * @param searchCriteria
	 * @param requestInfo
	 * @return List<Regularization>
	 */
	public List<Regularization> searchRegularization(RegularizationSearchCriteria searchCriteria, RequestInfo requestInfo) {
		log.info("Search Criteria :"+ searchCriteria.toString());
		List<Object> preparedStmtList = new ArrayList<>();
	    String query = queryBuilder.getRegularizationSearchQuery(searchCriteria, preparedStmtList);
	    log.info("Query for regularization search:"+ query);
	    List<Regularization> regularizationList =  jdbcTemplate.query(query, preparedStmtList.toArray(), rowMapper);
		
		if(regularizationList.isEmpty())
			return Collections.emptyList();
		return regularizationList;
	}

	/**
	 * Get all the regularization applications which are in Workflow
	 * 
	 * @return List<Regularization>
	 */
	public List<Regularization> getRegularizationsInWorkflow() {
		List<Object> preparedStmtList = new ArrayList<>();
		String query = queryBuilder.getApplicationsInWorkflowQuery();
		log.info("Query to get all the applications in workflow :" + query);
		List<Regularization> applicationsInWorkflow = jdbcTemplate.query(query, preparedStmtList.toArray(), rowMapper);
		return applicationsInWorkflow;
	}

	public String getAssigneeByprocessInstanceId(String processInstanceId) {
		List<Object> preparedStmtList = new ArrayList<>();
		String query = queryBuilder.getAssigneeByprocessInstanceId(processInstanceId, preparedStmtList);
		try {
			String assignee = jdbcTemplate.queryForObject(query, preparedStmtList.toArray(), String.class);
			return assignee;
		} catch (Exception e) {
			log.info("Assignee not found");
			return null;
		}
	}

	
	

	/**
	 * Repository for update Regularizations through Kafka
	 * 
	 * @param regularizationRequest
	 */
	public void updateRegularizationDscDetails(@Valid RegularizationRequest regularizationRequest) {
		//push to kafka topic
		producer.push(config.getUpdateRegularizationDscDetailsTopic(),regularizationRequest);
	}


	
	/**
	 * Repository for searching Regularization Dsc Details from db
	 * 
	 * @param searchCriteria
	 * @param requestInfo
	 * @return List<RegularizationDscDetails>
	 */
	public List<RegularizationDscDetails> searchRegularizationDscDetails(RegularizationSearchCriteria searchCriteria,
			RequestInfo requestInfo) {
		List<Object> preparedStmtList = new ArrayList<>();
	    String query = queryBuilder.getRegularizationDscDetailsSearchQuery(searchCriteria, preparedStmtList);
	    List<RegularizationDscDetails> regularizationDscDetailsList =  jdbcTemplate.query(query, preparedStmtList.toArray(), dscDetailsRowMapper);
		
		if(regularizationDscDetailsList.isEmpty())
			return Collections.emptyList();
		return regularizationDscDetailsList;
	}

	/**
	 * Push the data to Kafka for update through Persister
	 * 
	 * @param regularizationRequest
	 */
	public void updatePermitLetterPreview(@Valid RegularizationRequest regularizationRequest) {
		producer.push(config.getUpdateRegularizationPermitLetterPreview(), regularizationRequest);
	}
	
	/**
	 * Get All the approved by me applications from DB
	 * 
	 * @param uuid
	 * @param criteria
	 * @return
	 */
	public List<RegularizationApprovedByApplicationSearch> getApprovedbyData(String uuid,
			@Valid RegularizationSearchCriteria criteria) {
		List<Object> preparedStmtList = new ArrayList<>();

		String query = queryBuilder.getApplicationAprovedBy(uuid, preparedStmtList, criteria);
		log.info("query inside method getApprovedbyData:" + query);
		log.info("prepareStmtList:" + preparedStmtList);
		List<RegularizationApprovedByApplicationSearch> bpaData = jdbcTemplate.query(query, preparedStmtList.toArray(),
				new RegularizationApprovedByRowMapper());

		return bpaData;
	}
    
	/**
	 * Get Documents for Approved by me applications
	 * 
	 * @param bpids
	 * @return
	 */
	public List<RegularizationDocumentList> getdocumentDataForApproveBy(List<String> bpids) {
		List<Object> preparedStmtList = new ArrayList<>();
		String query = queryBuilder.getDocumentApprovedBy(bpids, preparedStmtList);

		List<RegularizationDocumentList> docData = jdbcTemplate.query(query, preparedStmtList.toArray(),
				new RegularizationApprovedByDocListRowMapper());

		return docData;
	}



	/**
	 * Send message to kafka save topic from here Save-topic: save-blr-application
	 * 
	 * @param FieldInspectionRequest
	 */
	public void savefieldInspectionReport(@Valid RegularizationFieldInspectionRequest request) {
		producer.push(config.getSaveRegularizationFieldInspectionTopicName(), request);
	}

	/**
	 * Get Field Inspections from DB
	 * 
	 * @param criteria
	 * @return
	 */
	public List<RegularizationFieldInspection> getfieldInspectionReport(
			@Valid RegularizationFISearchCriteria criteria) {
		List<Object> preparedStmtList = new ArrayList<>();
		String query = queryBuilder.getfieldinspectionReportDetails(criteria, preparedStmtList);
		List<RegularizationFieldInspection> report = jdbcTemplate.query(query, preparedStmtList.toArray(),
				new RegularizationFIDetailsRowMapper());
		return report;
	}

	
	/**
	 * Get Regularization AboutTo AutoEscalate
	 * @param searchCriteria
	 * @param uuid
	 * @return List<Regularization>
	 */
	public List<Regularization> getRegularizationAboutToAutoEscalate(RegularizationSearchCriteria searchCriteria,
			String uuid) {
		log.info("Search Criteria :" + searchCriteria.toString());
		List<Object> preparedStmtList = new ArrayList<>();
		String query = queryBuilder.getAboutToEscalate(searchCriteria, uuid, preparedStmtList);
	    log.info("Query for AboutToEscalate Search:" + query);
	    List<Regularization> regularizationList =  jdbcTemplate.query(query, preparedStmtList.toArray(), rowMapper);
	    if (CollectionUtils.isEmpty(regularizationList)) 
			return Collections.emptyList();
		return regularizationList;
	}

	
	/**
	 * Get Regularization Auto Escalated
	 * @param searchCriteria
	 * @param uuid
	 * @return List<Regularization>
	 */
	public List<Regularization> getRegularizationAutoEscalated(RegularizationSearchCriteria searchCriteria,
			String uuid) {
		log.info("Search Criteria :" + searchCriteria.toString());
		List<Object> preparedStmtList = new ArrayList<>();
		String query = queryBuilder.getRegularizationAutoEscalated(searchCriteria, uuid, preparedStmtList);
	    log.info("Query for AutoEscalated Search:" + query);
	    List<Regularization> regularizationList =  jdbcTemplate.query(query, preparedStmtList.toArray(), rowMapper);
	    if (CollectionUtils.isEmpty(regularizationList)) 
			return Collections.emptyList();
		return regularizationList;
	}

	
	/**
	 * Get Regularization AutoEscalated ToMe
	 * @param searchCriteria
	 * @param uuid
	 * @return List<Regularization>
	 */
	public List<Regularization> getRegularizationAutoEscalatedToMe(RegularizationSearchCriteria searchCriteria,
			String uuid) {
		log.info("Search Criteria :" + searchCriteria.toString());
		List<Object> preparedStmtList = new ArrayList<>();
		String query = queryBuilder.getRegularizationAutoEscalatedToMe(searchCriteria, uuid, preparedStmtList);
	    log.info("Query for AutoEscalatedToMe Search:" + query);
	    List<Regularization> regularizationList =  jdbcTemplate.query(query, preparedStmtList.toArray(), rowMapper);
	    if (CollectionUtils.isEmpty(regularizationList)) 
			return Collections.emptyList();
		return regularizationList;
	}

	/**
	 * send request to update persister topic
	 * 
	 * @param request
	 */
	public void updateFieldInspectionReport(@Valid RegularizationFieldInspectionRequest request) {
		producer.push(config.getUpdateRegularizationFieldInspectionTopicName(), request);

	}

	/**
	 * Repository layer for mapping village and status for input applications
	 * 
	 * @param criteria
	 * @return
	 */
	public List<RegularizationVillage> getRegularizationVillagesData(@Valid VillageSearchCriteria criteria) {
		
		List<Object> preparedStmtList = new ArrayList<>();
		
		// get query from query builder here 
		String queryToGetVillageData = queryBuilder.getVillageDataQuery(criteria, preparedStmtList);
		
		log.info("Query for Village Search ::: "+ queryToGetVillageData);
		
		// use rowmapper to map the data from DB using JDBCTemplate
		List<RegularizationVillage> regularizationVillages = jdbcTemplate.query(queryToGetVillageData, preparedStmtList.toArray(),
				new RegularizationVillageRowmapper());
		
		return regularizationVillages;
	}

	public List<FeePendingApplication> searchRegularizationFeePendingApplications(@Valid BPASearchCriteria criteria,
			RequestInfo requestInfo) {
		
		log.info("Search Criteria :", criteria.toString());
		List<Object> preparedStmtList = new ArrayList<>();
		String query = queryBuilder.getRegularizationFeePendingSearchQuery(criteria, preparedStmtList);
		log.info("Query for Pending At Sanc Fee Search Regularization ::: " + query);
		List<FeePendingApplication> feePendingApplications = jdbcTemplate.query(query, preparedStmtList.toArray(),
				new FeePendingApplicationRowMapper());
		
		return feePendingApplications;
	}

	public void updateDocument(RegularizationDocUploadRequest updateRequest) {
		producer.push(config.getDocumentRegularizationUpdateTopic(), updateRequest);

	}

	public void saveDocument(RegularizationDocUploadRequest uploadRequest) {
		producer.push(config.getDocumentRegularizationUploadTopic(), uploadRequest);

	}

	public void save(@Valid RegularizationDraftRequest request) {
		producer.push(config.getRegularizationSaveDraftTopic(), request);	
		
	}

	public List<RegularizationDraft> getRegularizationDraftData(@Valid RegularizationDraftSearchCriteria criteria) {
		List<Object> preparedStmtList = new ArrayList<>();
		String query = queryBuilder.getRegularizationDraftSearchQuery(criteria, preparedStmtList);
		log.info("Regularization Draft Search Query" + query);
		log.info("prepareStmtList:" + preparedStmtList);
		List<RegularizationDraft> regularizationDraftData = jdbcTemplate.query(query, preparedStmtList.toArray(), regularizationDraftRowMapper);
		return regularizationDraftData;
	}
	
}
