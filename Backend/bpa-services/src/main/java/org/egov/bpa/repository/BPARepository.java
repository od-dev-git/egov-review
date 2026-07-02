package org.egov.bpa.repository;

import java.util.ArrayList;
import java.util.List;

import javax.validation.Valid;

import org.egov.bpa.config.BPAConfiguration;
import org.egov.bpa.producer.Producer;
import org.egov.bpa.repository.querybuilder.BPAQueryBuilder;
import org.egov.bpa.repository.rowmapper.*;
import org.egov.bpa.web.model.BPA;
import org.egov.bpa.web.model.BPADocUploadRequest;
import org.egov.bpa.web.model.BPADraft;
import org.egov.bpa.web.model.BPADraftRequest;
import org.egov.bpa.web.model.BPADraftSearchCriteria;
import org.egov.bpa.web.model.BPARequest;
import org.egov.bpa.web.model.BPASearchCriteria;
import org.egov.bpa.web.model.BPAVillage;
import org.egov.bpa.web.model.BpaApplicationSearch;
import org.egov.bpa.web.model.BpaApprovedByApplicationSearch;
import org.egov.bpa.web.model.CompletionCertificate;
import org.egov.bpa.web.model.CompletionCertificateRequest;
import org.egov.bpa.web.model.CompletionCertificateSearchCriteria;
import org.egov.bpa.web.model.DocumentList;
import org.egov.bpa.web.model.DscDetails;
import org.egov.bpa.web.model.FeePendingApplication;
import org.egov.bpa.web.model.FieldInspection;
import org.egov.bpa.web.model.FieldInspectionRequest;
import org.egov.bpa.web.model.FieldInspectionSearchCriteria;
import org.egov.bpa.web.model.PlanningAssistantChecklist;
import org.egov.bpa.web.model.PlanningAssistantChecklistRequest;
import org.egov.bpa.web.model.PlanningAssistantSearchCriteria;
import org.egov.bpa.web.model.PlinthApproval;
import org.egov.bpa.web.model.PlinthApprovalRequest;
import org.egov.bpa.web.model.PlinthApprovalSearchCriteria;
import org.egov.bpa.web.model.StageWiseReport;
import org.egov.bpa.web.model.StageWiseReportRequest;
import org.egov.bpa.web.model.StageWiseReportSearchCriteria;
import org.egov.bpa.web.model.VillageSearchCriteria;
import org.egov.bpa.web.model.issuefix.IssueFix;
import org.egov.bpa.web.model.issuefix.IssueFixRequest;
import org.egov.bpa.web.model.oc.ScrutinyDetails;
import org.egov.bpa.web.model.workflow.ProcessInstance;
import org.egov.common.contract.request.RequestInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Repository
public class BPARepository {

	@Autowired
	private BPAConfiguration config;

	@Autowired
	private Producer producer;

	@Autowired
	private BPAQueryBuilder queryBuilder;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private BPARowMapper rowMapper;

	@Autowired
	private BPAApplicationRowMapper rowMapperApplication;

	@Autowired
	private BPAApprovedByRowMapper rowMapperApplicationApprovedBy;

	@Autowired
	private BPAApprovedByDocListRowMapper rowMapperApprovedByDocList;

	@Autowired
	private BPADigitalSignedCertificateRowMapper dscRowMapper;

	@Autowired
	private BPAReportingRowMapper bpaReportingRowMapper;

	@Autowired
	private FieldInspectorDetailsRowMapper rowMapperfieldInspector;
	
	@Autowired
	private PlanningAssistantChecklistRowmapper planningAssistantRowmapper;
	
	@Autowired
	private PlinthApprovalRowMapper plinthApprovalRowMapper;

	@Autowired
	private OCOutsideScrutinyDetailsRowMapper ocOutsideScrutinyDetailsRowMapper;
	
	@Autowired
	private BPADraftRowMapper bpaDraftRowMapper;
	
	@Autowired
	private CompletionCertificateRowMapper completionCertificateRowMapper;
	
	@Autowired
	private StageWiseReportRowMapper stageWiseReportRowMapper;

	/**
	 * Pushes the request on save topic through kafka
	 *
	 * @param bpaRequest The bpa create request
	 */
	public void save(BPARequest bpaRequest) {
		producer.push(config.getSaveTopic(), bpaRequest);
	}

	public void savefieldInspectionReport(FieldInspectionRequest request) {
		producer.push(config.getSavefieldinspectionTopicName(), request);
	}
	
	public void savePlanningAssistantChecklist(PlanningAssistantChecklistRequest request) {
		producer.push(config.getSavePlanningAssistantChecklistTopicName(), request);
	}
	
	public void updatePlanningAssistantChecklist(PlanningAssistantChecklistRequest request) {
		producer.push(config.getUpdatePlanningAssistantChecklistTopicName(), request);
	}
	
	public void savePlinthApproval(PlinthApprovalRequest request) {
		producer.push(config.getSavePlinthApprovalTopic(), request);
	}
	
	public void upadtePlinthApproval(PlinthApprovalRequest request) {
		producer.push(config.getUpdatePlinthApprovalTopic(), request);
	}
	
	public void save(@Valid BPADraftRequest request) {
		producer.push(config.getBpaSaveDraftTopic(), request);		
	}

	/**
	 * pushes the request on update or workflow update topic through kafaka based on
	 * th isStateUpdatable
	 * 
	 * @param bpaRequest
	 * @param isStateUpdatable
	 */
	public void update(BPARequest bpaRequest, boolean isStateUpdatable) {
		RequestInfo requestInfo = bpaRequest.getRequestInfo();

		BPA bpaForStatusUpdate = null;
		BPA bpaForUpdate = null;

		BPA bpa = bpaRequest.getBPA();

		if (isStateUpdatable) {
			bpaForUpdate = bpa;
		} else {
			bpaForStatusUpdate = bpa;
		}
		if (bpaForUpdate != null)
			producer.push(config.getUpdateTopic(), new BPARequest(requestInfo, bpaForUpdate));

		if (bpaForStatusUpdate != null)
			producer.push(config.getUpdateWorkflowTopic(), new BPARequest(requestInfo, bpaForStatusUpdate));

	}

	public void updatePermitLetterPreview(BPARequest bpaRequest) {
		producer.push(config.getUpdatePermitLetterPreview(), bpaRequest);
	}

	/**
	 * BPA search in database
	 *
	 * @param criteria The BPA Search criteria
	 * @return List of BPA from search
	 */
	public List<BPA> getBPAData(BPASearchCriteria criteria, List<String> edcrNos) {
		List<Object> preparedStmtList = new ArrayList<>();
		String query = queryBuilder.getBPASearchQuery(criteria, preparedStmtList, edcrNos);
		log.info("query inside method getBPAData:" + query);
		log.info("prepareStmtList:" + preparedStmtList);
		List<BPA> BPAData = jdbcTemplate.query(query, preparedStmtList.toArray(), rowMapper);
		return BPAData;
	}

	/**
	 * Searhces not signed document details in database
	 *
	 * @param criteria The tradeLicense Search criteria
	 * @return List of TradeLicense from seach
	 */
	public List<DscDetails> getDscDetails(BPASearchCriteria criteria) {
		List<Object> preparedStmtList = new ArrayList<>();
		String query = queryBuilder.getBPADscDetailsQuery(criteria, preparedStmtList);
		System.out.println("*************" + query);
		System.out.println("*********" + preparedStmtList);
		List<DscDetails> dscDetails = jdbcTemplate.query(query, preparedStmtList.toArray(), dscRowMapper);
		return dscDetails;
	}
	
	public List<DscDetails> getPlanDscDetails(BPASearchCriteria criteria) {
		List<Object> preparedStmtList = new ArrayList<>();
		String query = queryBuilder.getBPAPlanDscDetailsQuery(criteria, preparedStmtList);
		System.out.println("*************" + query);
		System.out.println("*********" + preparedStmtList);
		List<DscDetails> dscDetails = jdbcTemplate.query(query, preparedStmtList.toArray(), dscRowMapper);
		return dscDetails;
	}

	public void updateDscDetails(BPARequest bpaRequest) {
		RequestInfo requestInfo = bpaRequest.getRequestInfo();
		BPA bpa = bpaRequest.getBPA();
		producer.push(config.getUpdateDscDetailsTopic(), new BPARequest(requestInfo, bpa));

	}
	
	public void updatePlanDscDetails(BPARequest bpaRequest) {
		RequestInfo requestInfo = bpaRequest.getRequestInfo();
		BPA bpa = bpaRequest.getBPA();
		producer.push(config.getUpdatePlanDscDetailsTopic(), new BPARequest(requestInfo, bpa));

	}

	public List<BPA> getBpaApplication(RequestInfo requestInfo) {
		List<Object> preparedStmtList = new ArrayList<>();
		String query = queryBuilder.getBPAsSearchQuery(preparedStmtList);
		List<BPA> BPAData = jdbcTemplate.query(query, preparedStmtList.toArray(), bpaReportingRowMapper);
		return BPAData;
	}

	public List<BPA> getBPADataForPlainSearch(BPASearchCriteria criteria, List<String> edcrNos) {
		List<Object> preparedStmtList = new ArrayList<>();
		String query = queryBuilder.getBPASearchQueryForPlainSearch(criteria, preparedStmtList, edcrNos, false);
		List<BPA> BPAData = jdbcTemplate.query(query, preparedStmtList.toArray(), rowMapper);
		return BPAData;
	}

	public List<String> getApprover(String tenantId, String applicationNo) {
		List<Object> preparedStmtList = new ArrayList<>();
		String query = queryBuilder.getApplicationApprover(tenantId, applicationNo, preparedStmtList);
		List<String> approvers = jdbcTemplate.queryForList(query, preparedStmtList.toArray(), String.class);
		return approvers;
	}

	public List<BpaApplicationSearch> getBPAApplicationData(@Valid BPASearchCriteria criteria, List<String> edcrNos) {
		List<Object> preparedStmtList = new ArrayList<>();
		String query = queryBuilder.getBPAApplicationSearchQuery(criteria, preparedStmtList, edcrNos);
		List<BpaApplicationSearch> BPAData = jdbcTemplate.query(query, preparedStmtList.toArray(),
				rowMapperApplication);
		return BPAData;
	}

	public List<BpaApprovedByApplicationSearch> getApprovedbyData(String uuid, @Valid BPASearchCriteria criteria) {
		List<Object> preparedStmtList = new ArrayList<>();

		String query = queryBuilder.getApplicationAprovedBy(uuid, preparedStmtList, criteria);
		log.info("query inside method getApprovedbyData:" + query);
		log.info("prepareStmtList:" + preparedStmtList);
		List<BpaApprovedByApplicationSearch> bpaData = jdbcTemplate.query(query, preparedStmtList.toArray(),
				rowMapperApplicationApprovedBy);

		return bpaData;
	}

	public List<DocumentList> getdocumentDataForApproveBy(List<String> bpids) {
		List<Object> preparedStmtList = new ArrayList<>();
		String query = queryBuilder.getDocumentApprovedBy(bpids, preparedStmtList);

		List<DocumentList> docData = jdbcTemplate.query(query, preparedStmtList.toArray(), rowMapperApprovedByDocList);

		return docData;
	}

	public List<FieldInspection> getfieldInspectionReport(@Valid FieldInspectionSearchCriteria criteria) {
		// TODO Auto-generated method stub
		List<Object> preparedStmtList = new ArrayList<>();
		String query = queryBuilder.getfieldinspectionReportDetails(criteria, preparedStmtList);
		List<FieldInspection> report = jdbcTemplate.query(query, preparedStmtList.toArray(), rowMapperfieldInspector);
		return report;
	}
	
	public List<PlanningAssistantChecklist> getPlanningAssistanctChecklist(
			@Valid PlanningAssistantSearchCriteria criteria) {
		List<Object> preparedStmtList = new ArrayList<>();
		String query = queryBuilder.getPlanningAssistantChecklist(criteria, preparedStmtList);
		List<PlanningAssistantChecklist> report = jdbcTemplate.query(query, preparedStmtList.toArray(),
				planningAssistantRowmapper);
		return report;
	}
	
	public List<PlinthApproval> getPlinthApproval(
			@Valid PlinthApprovalSearchCriteria criteria) {
		List<Object> preparedStmtList = new ArrayList<>();
		String query = queryBuilder.getPlinthApproval(criteria, preparedStmtList);
		List<PlinthApproval> report = jdbcTemplate.query(query, preparedStmtList.toArray(),
				plinthApprovalRowMapper);
		return report;
	}

	public List<BPA> getBpaApplicationWithInWorkflow() {
		List<Object> preparedStmtList = new ArrayList<>();
		String query = queryBuilder.getBpaApplicationWithInWorkflow(preparedStmtList);
		List<BPA> BPAData = jdbcTemplate.query(query, preparedStmtList.toArray(), rowMapper);
		return BPAData;
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
	
	 public List<BPA> getBPAAutoEscalatedToME(BPASearchCriteria criteria, String uuid) {
         List<Object> preparedStmtList = new ArrayList<>();
         String query = queryBuilder.getAutoEscalatedToMe(criteria,uuid,preparedStmtList);
         log.info("Query for AutoEscalatedToMe Search:" + query);
         List<BPA> BPAData = jdbcTemplate.query(query,preparedStmtList.toArray(), rowMapper);
         return BPAData;
     }
 
     public List<BPA> getBPAABoutToEscalate(BPASearchCriteria criteria, String uuid) {
         List<Object> preparedStmtList = new ArrayList<>();
         String query = queryBuilder.getAboutToEscalate(criteria,uuid,preparedStmtList);
         log.info("Query for AboutToEscalate Search:" + query);
         List<BPA> BPAData = jdbcTemplate.query(query,preparedStmtList.toArray(), rowMapper);
         return BPAData;
     }
     
     
     public List<BPA> getBPAAutoEscalated(BPASearchCriteria criteria, String uuid) {
         List<Object> preparedStmtList = new ArrayList<>();
         String query = queryBuilder.getAutoEscalated(criteria,uuid,preparedStmtList);
         log.info("Query for AutoEscalated Search:" + query);
         List<BPA> BPAData = jdbcTemplate.query(query,preparedStmtList.toArray(), rowMapper);
         return BPAData;
     }

	 public List<ScrutinyDetails> getScrutinyDetails(BPA bpa){
		 List<Object> preparedStmtList = new ArrayList<>();
		 String query= queryBuilder.getOCOutsideScrutinyDetails(bpa,preparedStmtList);
		 List<ScrutinyDetails> scrutinyDetails = jdbcTemplate.query(query,preparedStmtList.toArray(), ocOutsideScrutinyDetailsRowMapper);
		 return scrutinyDetails;
	 }
	 
		public void saveDocument(BPADocUploadRequest documentsRequest) {
			producer.push(config.getDocumentUploadTopic(), documentsRequest);
		}

		public void updateDocument(BPADocUploadRequest documentsRequest) {
			producer.push(config.getDocumentUpdateTopic(), documentsRequest);
			
		}

		/**
		 * Repository layer for mapping village and status for input applications
		 * 
		 * @param criteria
		 * @return
		 */
		public List<BPAVillage> getBPAVillagesData(@Valid VillageSearchCriteria criteria) {
			
			List<Object> preparedStmtList = new ArrayList<>();
			
			// get query from query builder here 
			String queryToGetVillageData = queryBuilder.getVillageDataQuery(criteria, preparedStmtList);
			
			log.info("Query for Village Search ::: "+ queryToGetVillageData);
			
			// use rowmapper to map the data from DB using JDBCTemplate
			List<BPAVillage> bpaVillages = jdbcTemplate.query(queryToGetVillageData, preparedStmtList.toArray(),
					new BPAVillageRowmapper());
			
			return bpaVillages;
		}

		public void updateFieldInspectionReport(@Valid FieldInspectionRequest request) {
			producer.push(config.getUpdateFieldInspectionTopicName(), request);	
		}

		public List<FeePendingApplication> searchBPAFeePendingApplications(@Valid BPASearchCriteria criteria,
				RequestInfo requestInfo) {
			
			log.info("Search Criteria :", criteria.toString());
			List<Object> preparedStmtList = new ArrayList<>();
			String query = queryBuilder.getBPAFeePendingSearchQuery(criteria, preparedStmtList);
			log.info("Query for Pending At Sanc Fee Search ::: " + query);
			List<FeePendingApplication> feePendingApplications = jdbcTemplate.query(query, preparedStmtList.toArray(),
					new FeePendingApplicationRowMapper());
			
			return feePendingApplications;
		}

		public List<BPADraft> getBPADraftData(@Valid BPADraftSearchCriteria criteria) {
			List<Object> preparedStmtList = new ArrayList<>();
			String query = queryBuilder.getBPADraftSearchQuery(criteria, preparedStmtList);
			log.info("BPA Draft Search Query" + query);
			log.info("prepareStmtList:" + preparedStmtList);
			List<BPADraft> BPADraftData = jdbcTemplate.query(query, preparedStmtList.toArray(), bpaDraftRowMapper);
			return BPADraftData;
		}
		
		/**
		 * BPA Count in database
		 *
		 * @param criteria The BPA Search criteria
		 * @return List of BPA from search
		 */
		public Integer getBPACountData(BPASearchCriteria criteria, List<String> edcrNos) {
			List<Object> preparedStmtList = new ArrayList<>();
			String query = queryBuilder.getBPACountQuery(criteria, preparedStmtList, edcrNos);
			log.info("query inside method getBPACount Query:" + query);
			log.info("prepareStmtList:" + preparedStmtList);
			return jdbcTemplate.queryForObject(query, preparedStmtList.toArray(), Integer.class);
		}

		public Integer getBPADraftCountData(@Valid BPADraftSearchCriteria criteria) {
			List<Object> preparedStmtList = new ArrayList<>();
			String query = queryBuilder.getBPADraftCountQuery(criteria, preparedStmtList);
			log.info("query inside method getBPADraftCount Query:" + query);
			log.info("prepareStmtList:" + preparedStmtList);
			return jdbcTemplate.queryForObject(query, preparedStmtList.toArray(), Integer.class);
		}

		public void save(@Valid CompletionCertificateRequest completionCertificateRequest) {
			producer.push(config.getSaveCompletionCertificateTopic(), completionCertificateRequest);
		}
		
		public void update(@Valid CompletionCertificateRequest completionCertificateRequest) {
			producer.push(config.getUpdateCompletionCertificateTopic(), completionCertificateRequest);
		}

		public List<CompletionCertificate> getCompletionCertificateData(
				@Valid CompletionCertificateSearchCriteria criteria) {
			List<Object> preparedStmtList = new ArrayList<>();
			String query = queryBuilder.getCompletionCertificateSearchQuery(criteria, preparedStmtList);
			log.info("query inside method getCompletionCertificateData:" + query);
			log.info("prepareStmtList:" + preparedStmtList);
			List<CompletionCertificate> completionCertificateData = jdbcTemplate.query(query, preparedStmtList.toArray(), completionCertificateRowMapper);
			return completionCertificateData;
		}

		public void save(@Valid StageWiseReportRequest stageWiseReportRequest) {
			producer.push(config.getSaveStateWiseReportTopic(), stageWiseReportRequest);
		}
		
		public void update(@Valid StageWiseReportRequest stageWiseReportRequest) {
			producer.push(config.getUpdateStateWiseReportTopic(), stageWiseReportRequest);
		}

		public List<StageWiseReport> getStageWiseReports(@Valid StageWiseReportSearchCriteria criteria) {
			List<Object> preparedStmtList = new ArrayList<>();
			String query = queryBuilder.getStageWiseReportSearchQuery(criteria, preparedStmtList);
			log.info("query inside method getStageWiseReports:" + query);
			log.info("prepareStmtList:" + preparedStmtList);
			List<StageWiseReport> stageWiseReports = jdbcTemplate.query(query, preparedStmtList.toArray(),
					stageWiseReportRowMapper);
			return stageWiseReports;
		}

		

}
