package org.egov.bpa.repository;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.validation.Valid;

import org.egov.bpa.config.BPAConfiguration;
import org.egov.bpa.repository.querybuilder.IssueFixQueryBuilder;
import org.egov.bpa.repository.rowmapper.BPADigitalSignedCertificateRowMapper;
import org.egov.bpa.repository.rowmapper.DemandRowMapper;
import org.egov.bpa.repository.rowmapper.InstallmentRowMapper;
import org.egov.bpa.repository.rowmapper.PaymentIssueFixRowMapper;
import org.egov.bpa.repository.rowmapper.PaymentRowMapper;
import org.egov.bpa.repository.rowmapper.ProcessInstanceRowMapper;
import org.egov.bpa.repository.rowmapper.StatusMismatchIssueRowMapper;
import org.egov.bpa.util.IssueFixConstants;
import org.egov.bpa.web.model.BPA;
import org.egov.bpa.web.model.BPASearchCriteria;
import org.egov.bpa.web.model.DscDetails;
import org.egov.bpa.web.model.Installment;
import org.egov.bpa.web.model.InstallmentSearchCriteria;
import org.egov.bpa.web.model.collection.Demand;
import org.egov.bpa.web.model.collection.DemandDetail;
import org.egov.bpa.web.model.collection.DemandSearchCriteria;
import org.egov.bpa.web.model.collection.Payment;
import org.egov.bpa.web.model.collection.PaymentSearchCriteria;
import org.egov.bpa.web.model.idgen.IdResponse;
import org.egov.bpa.web.model.issuefix.IssueFix;
import org.egov.bpa.web.model.issuefix.PaymentIssueFix;
import org.egov.bpa.web.model.issuefix.StatusMismatchIssueFix;
import org.egov.bpa.web.model.workflow.ProcessInstance;
import org.egov.bpa.web.model.workflow.WorkFlowSearchCriteria;
import org.egov.common.contract.request.RequestInfo;
import org.egov.tracer.model.CustomException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Repository
public class IssueFixRepository {

	@Autowired
	private IssueFixQueryBuilder issueFixQueryBuilder;

	@Autowired
	private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

	@Autowired
	InstallmentRowMapper installmentRowmapper;

	@Autowired
	private BPADigitalSignedCertificateRowMapper dscRowMapper;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private IdGenRepository idGenRepository;
	
	@Autowired
	private PaymentIssueFixRowMapper paymentIssueFixRowMapper;
	
	@Autowired
	private StatusMismatchIssueRowMapper statusMismatchIssueFixRowMapper;

	@Autowired
	private BPAConfiguration config;

	public List<Payment> getPayments(PaymentSearchCriteria paymentSearchCriteria) {

		Map<String, Object> preparedStatementValues = new HashMap<>();

		String queryForPaymentSearch = issueFixQueryBuilder.getPaymentSearchQuery(paymentSearchCriteria,
				preparedStatementValues);

		List<Payment> payments = namedParameterJdbcTemplate.query(queryForPaymentSearch, preparedStatementValues,
				new PaymentRowMapper());

		return payments;

	}

	public List<Demand> getDemands(DemandSearchCriteria demandSearchCriteria) {

		Map<String, Object> preparedStatementValues = new HashMap<>();

		String queryForDemandSearch = issueFixQueryBuilder.getDemandSearchQuery(demandSearchCriteria,
				preparedStatementValues);

		List<Demand> demands = namedParameterJdbcTemplate.query(queryForDemandSearch, preparedStatementValues,
				new DemandRowMapper());

		return demands;

	}

	public void updateDemand(Demand demand) {

		String updateDemandQuery = issueFixQueryBuilder.getDemandUpdateQuery();

		jdbcTemplate.update(updateDemandQuery, new PreparedStatementSetter() {

			@Override
			public void setValues(PreparedStatement ps) {
				try {
					ps.setString(1, demand.getConsumerCode());
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});

	}

	public void updateDemandDetail(DemandDetail demandDetail) {

		String updateDemandDetailQuery = issueFixQueryBuilder.getDemandDetailUpdateQuery();

		jdbcTemplate.update(updateDemandDetailQuery, new PreparedStatementSetter() {

			@Override
			public void setValues(PreparedStatement ps) {
				try {
					ps.setString(1, demandDetail.getId());
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});

	}

	public List<ProcessInstance> getProcessInstances(WorkFlowSearchCriteria workFlowSearchCriteria) {

		Map<String, Object> preparedStatementValues = new HashMap<>();

		String queryForWFSearch = issueFixQueryBuilder.getProcessInstancesQuery(workFlowSearchCriteria,
				preparedStatementValues);

		List<ProcessInstance> processInstances = namedParameterJdbcTemplate.query(queryForWFSearch,
				preparedStatementValues, new ProcessInstanceRowMapper());

		return processInstances;
	}

	public void updateApplication(BPA bpa, Payment payment) {

		String updateApplicationQuery = issueFixQueryBuilder.getApplicationUpdateQuery();

		// String applicationType = bpa.getApplicationType();

		jdbcTemplate.update(updateApplicationQuery, new PreparedStatementSetter() {

			@Override
			public void setValues(PreparedStatement ps) {
				try {
					if (bpa.getBusinessService().equalsIgnoreCase(IssueFixConstants.BPA5_BS)) {
						ps.setString(1, IssueFixConstants.BPA_PENDING_FORWARD);
					} else {
						ps.setString(1, IssueFixConstants.BPA_DOC_VERIFICATION_INPROGRESS);
					}
					ps.setLong(2, payment.getAuditDetails().getCreatedTime());
					ps.setLong(3, payment.getAuditDetails().getCreatedTime());
					ps.setString(4, bpa.getApplicationNo());
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});

	}

	public void updateWorkflow(ProcessInstance processInstance, Payment payment) {

		String insertWorkFlowQuery = issueFixQueryBuilder.getInsertWorkflowQuery();

		String businessService = processInstance.getBusinessService();

		jdbcTemplate.update(insertWorkFlowQuery, new PreparedStatementSetter() {

			@Override
			public void setValues(PreparedStatement ps) {
				try {
					ps.setString(1, UUID.randomUUID().toString());
					ps.setString(2, processInstance.getTenantId());
					ps.setString(3, businessService);
					ps.setString(4, processInstance.getBusinessId());
					if (businessService.equalsIgnoreCase(IssueFixConstants.BPA1_BS)) {
						ps.setString(5, IssueFixConstants.BPA1_AFTER_APP_FEE_PAYMENT_STATUS);
					} else if (businessService.equalsIgnoreCase(IssueFixConstants.BPA2_BS)) {
						ps.setString(5, IssueFixConstants.BPA2_AFTER_APP_FEE_PAYMENT_STATUS);
					} else if (businessService.equalsIgnoreCase(IssueFixConstants.BPA3_BS)) {
						ps.setString(5, IssueFixConstants.BPA3_AFTER_APP_FEE_PAYMENT_STATUS);
					} else if (businessService.equalsIgnoreCase(IssueFixConstants.BPA4_BS)) {
						ps.setString(5, IssueFixConstants.BPA4_AFTER_APP_FEE_PAYMENT_STATUS);
					} else if (businessService.equalsIgnoreCase(IssueFixConstants.BPA5_BS)) {
						ps.setString(5, IssueFixConstants.BPA5_AFTER_APP_FEE_PAYMENT_STATUS);
					}else if (businessService.equalsIgnoreCase(IssueFixConstants.BPA7_BS)) {
						ps.setString(5, IssueFixConstants.BPA7_AFTER_APP_FEE_PAYMENT_STATUS);
					}
					ps.setString(6, payment.getPayerId());
					ps.setString(7, payment.getPayerId());
					ps.setString(8, payment.getPayerId());
					ps.setLong(9, payment.getAuditDetails().getCreatedTime());
					ps.setLong(10, payment.getAuditDetails().getCreatedTime());

				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});

	}

	public void expireBill(String consumerCode) {

		String billExpireQuery = issueFixQueryBuilder.getBillExpireQuery();

		jdbcTemplate.update(billExpireQuery, new PreparedStatementSetter() {

			@Override
			public void setValues(PreparedStatement ps) {
				try {
					ps.setString(1, consumerCode);
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});
	}

	public void updateApplicationForSancFeeIssueFix(BPA bpa, Payment payment, RequestInfo requestInfo) {

		String updateApplicationQuery = issueFixQueryBuilder.getApplicationUpdateQueryForSancFee();
		List<IdResponse> idResponses = idGenRepository.getId(requestInfo, bpa.getTenantId(),
				config.getPermitNoIdgenName(), config.getPermitNoIdgenFormat(), 1).getIdResponses();
		bpa.setApprovalNo(idResponses.get(0).getId());

		jdbcTemplate.update(updateApplicationQuery, new PreparedStatementSetter() {

			@Override
			public void setValues(PreparedStatement ps) {
				try {
					ps.setString(1, IssueFixConstants.APPROVED);
					ps.setLong(2, payment.getAuditDetails().getCreatedTime());
					ps.setString(3, bpa.getApprovalNo());
					ps.setString(4, bpa.getApplicationNo());
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});

	}

	public void updateWorkflowForSancFee(ProcessInstance processInstance, Payment payment) {

		String insertWorkFlowQuery = issueFixQueryBuilder.getInsertWorkflowQuery();

		String businessService = processInstance.getBusinessService();

		jdbcTemplate.update(insertWorkFlowQuery, new PreparedStatementSetter() {

			@Override
			public void setValues(PreparedStatement ps) {
				try {
					ps.setString(1, UUID.randomUUID().toString());
					ps.setString(2, processInstance.getTenantId());
					ps.setString(3, businessService);
					ps.setString(4, processInstance.getBusinessId());
					if (businessService.equalsIgnoreCase(IssueFixConstants.BPA1_BS)) {
						ps.setString(5, IssueFixConstants.BPA1_AFTER_SANC_FEE_PAYMENT_STATUS);
					} else if (businessService.equalsIgnoreCase(IssueFixConstants.BPA2_BS)) {
						ps.setString(5, IssueFixConstants.BPA2_AFTER_SANC_FEE_PAYMENT_STATUS);
					} else if (businessService.equalsIgnoreCase(IssueFixConstants.BPA3_BS)) {
						ps.setString(5, IssueFixConstants.BPA3_AFTER_SANC_FEE_PAYMENT_STATUS);
					} else if (businessService.equalsIgnoreCase(IssueFixConstants.BPA4_BS)) {
						ps.setString(5, IssueFixConstants.BPA4_AFTER_SANC_FEE_PAYMENT_STATUS);
					} else if (businessService.equalsIgnoreCase(IssueFixConstants.BPA5_BS)) {
						ps.setString(5, IssueFixConstants.BPA5_AFTER_SANC_FEE_PAYMENT_STATUS);
					}else if (businessService.equalsIgnoreCase(IssueFixConstants.BPA7_BS)) {
						ps.setString(5, IssueFixConstants.BPA7_AFTER_SANC_FEE_PAYMENT_STATUS);
					}
					ps.setString(6, payment.getPayerId());
					ps.setString(7, payment.getPayerId());
					ps.setString(8, payment.getPayerId());
					ps.setLong(9, payment.getAuditDetails().getCreatedTime());
					ps.setLong(10, payment.getAuditDetails().getCreatedTime());

				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});

	}

	public void expireDSCDetails(String id) {

		String expireDSCDetailsQuery = issueFixQueryBuilder.getUpdateDscDetailsQuery();
		jdbcTemplate.update(expireDSCDetailsQuery, preparedStatement -> {
			try {
				preparedStatement.setString(1, id);
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		});

	}
	
	public void expirePlanDSCDetails(String id) {

		String expireDSCDetailsQuery = issueFixQueryBuilder.getUpdatePlanDscDetailsQuery();
		jdbcTemplate.update(expireDSCDetailsQuery, preparedStatement -> {
			try {
				preparedStatement.setString(1, id);
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		});

	}

	public void updateApplicationStatusMismatch(List<String> id) {
		List<Object> preparedStatementList = new ArrayList<>();
		String applicationStatusMismatchIssueQuery = issueFixQueryBuilder.getApplicationStatusMismatchIssueQuery(id,
				preparedStatementList);

		jdbcTemplate.update(applicationStatusMismatchIssueQuery, preparedStatement -> {
			try {
				for (int i = 0; i < id.size(); i++) {
					preparedStatement.setString(i + 1, id.get(i));
				}
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		});
	}

	public List<String> getUUID(String newApproval, String tenantId) {

		List<Object> preparedStmtList = new ArrayList<>();

		String query = issueFixQueryBuilder.getUUID(newApproval, tenantId, preparedStmtList);
		List<String> data = jdbcTemplate.queryForList(query, preparedStmtList.toArray(), String.class);
		return data;
	}

	public void updateDscApprover(BPA bpa, String newApprover, String issueSubtype) {

//		List<Object> preparedStatementList = new ArrayList<>();
		String updateDscApproverQuery = issueFixQueryBuilder.getUpdateDscApproverQuery(issueSubtype);

		jdbcTemplate.update(updateDscApproverQuery, preparedStatement -> {
			try {

				preparedStatement.setString(1, newApprover);
				preparedStatement.setString(2, bpa.getApplicationNo());
				preparedStatement.setString(3, bpa.getTenantId());

			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		});

	}

	public List<String> getDSC(IssueFix issueFix) {

		List<Object> preparedStmtList = new ArrayList<>();

		String query = issueFixQueryBuilder.getDSC(issueFix, preparedStmtList);
		System.out.println("Query: " + query);
		System.out.println("preparedStmtList: " + preparedStmtList);
		List<String> data = jdbcTemplate.queryForList(query, preparedStmtList.toArray(), String.class);
		System.out.println("Data: " + data);
		return data;
	}

	public void updateDSC(IssueFix issueFix) {

		String query = issueFixQueryBuilder.searchDscQuery(issueFix.getIssueSubtype());
		System.out.println("Query: " + query);
		jdbcTemplate.update(query, preparedStatement -> {
			try {
				preparedStatement.setString(1, issueFix.getApplicationNo());
				preparedStatement.setString(2, issueFix.getTenantId());
				preparedStatement.setString(3, issueFix.getApplicationNo());
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		});
	}

	public void deleteDemand(Demand demand) {

		String updateDemandQuery = issueFixQueryBuilder.getDemandDeleteQuery();

		jdbcTemplate.update(updateDemandQuery, new PreparedStatementSetter() {

			@Override
			public void setValues(PreparedStatement ps) {
				try {
					ps.setString(1, demand.getConsumerCode());
					ps.setString(2, demand.getBusinessService());
					ps.setString(3, demand.getId());
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});

	}

	public void deleteDemandDetail(Demand demand) {

		String updateDemandDetailQuery = issueFixQueryBuilder.getDemandDetailDeleteQuery();

		jdbcTemplate.update(updateDemandDetailQuery, new PreparedStatementSetter() {

			@Override
			public void setValues(PreparedStatement ps) {
				try {
					ps.setString(1, demand.getId());
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});

	}
	
	public void updateInstallmentDemandId(IssueFix issueFix) {

		String updateDeamndIdQuery = issueFixQueryBuilder.getUpdateInstallmentDemandIdQuery();

		log.info("Update Deamnd Id Query: " + updateDeamndIdQuery);
		jdbcTemplate.update(updateDeamndIdQuery, preparedStatement -> {
			try {
				preparedStatement.setString(1, issueFix.getApplicationNo());
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		});
	}

    
	@Transactional
	public void updateApplicationForStepBack(BPA bpa) {

		String updateApplicationQuery = issueFixQueryBuilder.getApplicationUpdateQueryForSendBack();
		String deleteAdditionalDetailsEstimate = issueFixQueryBuilder.getDeleteAdditionalDetailsEstimateQuery();

		jdbcTemplate.update(updateApplicationQuery, new PreparedStatementSetter() {

			@Override
			public void setValues(PreparedStatement ps) {
				try {
					ps.setString(1, IssueFixConstants.BPA_STATUS_APPROVAL_INPROGRESS);
					ps.setString(2, bpa.getApplicationNo());
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});

		jdbcTemplate.update(deleteAdditionalDetailsEstimate, new PreparedStatementSetter() {

			@Override
			public void setValues(PreparedStatement ps) {
				try {
					ps.setString(1, bpa.getApplicationNo());
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});

	}

	public void updateWorkflowForStepBack(ProcessInstance processInstance) {

		String insertWorkFlowQuery = issueFixQueryBuilder.getDeleteWorkflowQuery();

		String businessService = processInstance.getBusinessService();

		jdbcTemplate.update(insertWorkFlowQuery, new PreparedStatementSetter() {

			@Override
			public void setValues(PreparedStatement ps) {
				try {
					ps.setString(1, processInstance.getBusinessId());
					ps.setString(2, processInstance.getId());

				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});

	}

	public void deleteInstallments(BPA bpa) {

		String updateDemandDetailQuery = issueFixQueryBuilder.getDeleteInstallmentQuery();

		jdbcTemplate.update(updateDemandDetailQuery, new PreparedStatementSetter() {

			@Override
			public void setValues(PreparedStatement ps) {
				try {
					ps.setString(1, bpa.getApplicationNo());
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});

	}

	public List<String> getProcessInstanceId(String businessId) {

		List<Object> preparedStmtList = new ArrayList<>();

		String query = issueFixQueryBuilder.getProcessInstanceId();
		preparedStmtList.add(businessId);
		List<String> data = jdbcTemplate.queryForList(query, preparedStmtList.toArray(), String.class);
		return data;
	}

	public void updateAssigner(IssueFix issueFix, String data, String processInstanceId) {

		String query = issueFixQueryBuilder.getUpdateAssignerQuery();
		jdbcTemplate.update(query, preparedStatement -> {
			try {

				preparedStatement.setString(1, data);
				preparedStatement.setString(2, data);
				preparedStatement.setString(3, data);
				preparedStatement.setString(4, issueFix.getApplicationNo());
				preparedStatement.setString(5, processInstanceId);

			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		});

	}

	public void deleteDsc(BPA bpa) {

		String updateDemandDetailQuery = issueFixQueryBuilder.getDeleteDscQuery();

		jdbcTemplate.update(updateDemandDetailQuery, new PreparedStatementSetter() {

			@Override
			public void setValues(PreparedStatement ps) {
				try {
					ps.setString(1, bpa.getApplicationNo());
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});

	}

	public List<Installment> getInstallments(InstallmentSearchCriteria criteria) {
		List<Object> preparedStmtList = new ArrayList<>();
		String query = issueFixQueryBuilder.getInstallmentSearchQuery(criteria, preparedStmtList);
		System.out.println("*************" + query);
		System.out.println("*********" + preparedStmtList);
		List<Installment> installments = jdbcTemplate.query(query, preparedStmtList.toArray(), installmentRowmapper);
		return installments;
	}

	public List<DscDetails> getDscDetails(BPASearchCriteria criteria) {
		List<Object> preparedStmtList = new ArrayList<>();
		String query = issueFixQueryBuilder.getBPADscDetailsQuery(criteria, preparedStmtList);
		System.out.println("*************" + query);
		System.out.println("*********" + preparedStmtList);
		List<DscDetails> dscDetails = jdbcTemplate.query(query, preparedStmtList.toArray(), dscRowMapper);
		return dscDetails;
	}
	
	public List<PaymentIssueFix> getPaymentIssueApplications() {
		String query = issueFixQueryBuilder.getPaymentIssueAppliactionsQuery();
		System.out.println("*************" + query);
		List<PaymentIssueFix> paymentIssueFixApplications = jdbcTemplate.query(query, paymentIssueFixRowMapper);
		return paymentIssueFixApplications;
	}
	
	public List<StatusMismatchIssueFix> getStatusMismatchApplications() {
		String query = issueFixQueryBuilder.getStatusMismatchAppliactionsQuery();
		System.out.println("*************" + query);
		List<StatusMismatchIssueFix> statusMismatchIssueFixs = jdbcTemplate.query(query, statusMismatchIssueFixRowMapper);
		return statusMismatchIssueFixs;
	}

	public List<String> getInstallment(IssueFix issueFix) {

		List<Object> preparedStmtList = new ArrayList<>();

		String query = issueFixQueryBuilder.getInstallment(issueFix, preparedStmtList);
		System.out.println("Query: " + query);
		System.out.println("preparedStmtList: " + preparedStmtList);
		List<String> data = jdbcTemplate.queryForList(query, preparedStmtList.toArray(), String.class);
		System.out.println("Data: " + data);
		return data;
	}

	public List<String> checkDuplicates(IssueFix issueFix) {

		List<Object> preparedStmtList = new ArrayList<>();

		String query = issueFixQueryBuilder.checkDuplicates();
		preparedStmtList.add(issueFix.getApplicationNo());
		System.out.println("Query: " + query);
		System.out.println("preparedStmtList: " + preparedStmtList);
		List<String> data = jdbcTemplate.queryForList(query, preparedStmtList.toArray(), String.class);
		System.out.println("DuplicateData: " + data);
		return data;
	}
    
	@Transactional
	public void updateInstallment(IssueFix issueFix) {

		String deleteDuplidateInstallemntQuery = issueFixQueryBuilder.getInstallmentDeleteQuery();
		String updateDeamndIdQuery = issueFixQueryBuilder.getUpdateInstallmentDemandIdQuery();

		log.info("Delete Duplidate Installemnt Query: " + deleteDuplidateInstallemntQuery);
		jdbcTemplate.update(deleteDuplidateInstallemntQuery, preparedStatement -> {
			try {
				preparedStatement.setString(1, issueFix.getApplicationNo());
				preparedStatement.setString(2, issueFix.getApplicationNo());
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		});

		log.info("Update Deamnd Id Query: " + updateDeamndIdQuery);
		jdbcTemplate.update(updateDeamndIdQuery, preparedStatement -> {
			try {
				preparedStatement.setString(1, issueFix.getApplicationNo());
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		});
	}

	public void update(List<String> data) {

		String query = issueFixQueryBuilder.update(data.size());
		System.out.println("Query: " + query);
		jdbcTemplate.update(query, preparedStatement -> {
			try {

				for (int i = 0; i < data.size(); i++) {
					preparedStatement.setString(i + 1, data.get(i));
				}

			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		});

	}

	public List<String> getIDs(String applicationNo) {

		List<Object> preparedStmtList = new ArrayList<>();

		String query = issueFixQueryBuilder.getIDs();
		preparedStmtList.add(applicationNo);
		System.out.println("Query: " + query);
		System.out.println("preparedStmtList: " + preparedStmtList);
		List<String> data = jdbcTemplate.queryForList(query, preparedStmtList.toArray(), String.class);
		return data;
	}

	public void deleteDemandV1(Demand demand) {

		String updateDemandQuery = issueFixQueryBuilder.getDemandV1DeleteQuery();

		jdbcTemplate.update(updateDemandQuery, new PreparedStatementSetter() {

			@Override
			public void setValues(PreparedStatement ps) {
				try {
					ps.setString(1, demand.getConsumerCode());
					ps.setString(2, demand.getBusinessService());
					ps.setString(3, demand.getConsumerCode());
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});
	}
	
	public void deleteBPLSignedDocument(String signedBPLDocId) {

		String deleteBPLSigneddocumentQuery = issueFixQueryBuilder.getdeleteBPLSigneddocumentQuery();

		jdbcTemplate.update(deleteBPLSigneddocumentQuery, new PreparedStatementSetter() {

			@Override
			public void setValues(PreparedStatement ps) {
				try {
					ps.setString(1, signedBPLDocId );			
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});
	
	}

	public void updateBPLSignedDocuments(IssueFix issuefix) {
		
		String updateAdditionalDetailsUnSignedBP = issueFixQueryBuilder.getupdateAdditionalDetailsUnSignedBP();

		
		jdbcTemplate.update(updateAdditionalDetailsUnSignedBP, new PreparedStatementSetter() {

			@Override
			public void setValues(PreparedStatement ps) {
				try {
					ps.setString(1, issuefix.getApplicationNo());
						} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});
		
		String updateAdditionalDetailsBPLayoutIsSigned = issueFixQueryBuilder.getupdateAdditionalDetailsBPLayoutIsSigned();
		jdbcTemplate.update(updateAdditionalDetailsBPLayoutIsSigned, new PreparedStatementSetter() {

			@Override
			public void setValues(PreparedStatement ps) {
				try {
					ps.setString(1, issuefix.getApplicationNo());
						} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});
	
	
	}

	public List<String> getUUIDByTenant(String newApprover, String tenantId) {
		// TODO Auto-generated method stub
		return null;
	}

	
	public void deleteDemandDetails(List<DemandDetail> demandDetails) {
		String updateDemandDetailQuery = issueFixQueryBuilder.getDemandDetailDeleteQueryById();

		demandDetails.forEach(demandDetail ->{
			jdbcTemplate.update(updateDemandDetailQuery, new PreparedStatementSetter() {

				@Override
				public void setValues(PreparedStatement ps) {
					try {
						ps.setString(1, demandDetail.getId());
					} catch (SQLException e) {
						e.printStackTrace();
					}
				}
			});
		});
		
	}

	

	public void updateApplicationStatus(IssueFix issueFix) {
		String updateApplicationStatusQuery = issueFixQueryBuilder.getApplicationStatusUpdateQuery();
		
		jdbcTemplate.update(updateApplicationStatusQuery, new PreparedStatementSetter() {

			@Override
			public void setValues(PreparedStatement ps) {
				try {
					ps.setString(1, IssueFixConstants.DELETED);
					ps.setLong(2, System.currentTimeMillis());
					ps.setString(3, issueFix.getApplicationNo());
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		});
	}

	
	public void deleteOtherFeeFromAdditionalDetails(IssueFix issueFix) {
		String deleteOtherFeeDetailsQuery = issueFixQueryBuilder.getDeleteOtherFeeDetailsQuery();
		
		jdbcTemplate.update(deleteOtherFeeDetailsQuery, new PreparedStatementSetter() {

			@Override
			public void setValues(PreparedStatement ps) {
				try {
					ps.setString(1, issueFix.getApplicationNo());
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		});
	}

	
	@Transactional
	public void deleteDSCDetails(String applicationNo) {
		String deleteAdditionalDetailsPreviewPermitLetterFileStoreId = issueFixQueryBuilder.getDeleteAdditionalDetailsPreviewPermitLetterFileStoreIdQuery();
		String deleteAdditionalDetailsUnsignedBuildingPlanLayoutDetails = issueFixQueryBuilder.getDeleteAdditionalDetailsUnsignedBuildingPlanLayoutDetailsQuery();
		String deleteAdditionalDetailsBuildingPlanLayoutIsSigned = issueFixQueryBuilder.getDeleteAdditionalDetailsBuildingPlanLayoutIsSignedQuery();
		String deleteDSCDetailsQuery = issueFixQueryBuilder.getDeleteDscDetailsQuery();
		
		jdbcTemplate.update(deleteAdditionalDetailsPreviewPermitLetterFileStoreId, preparedStatement -> {
			try {
				preparedStatement.setString(1, applicationNo);
			} catch (SQLException e) {
				e.printStackTrace();
			}
		});
		
		jdbcTemplate.update(deleteAdditionalDetailsUnsignedBuildingPlanLayoutDetails, preparedStatement -> {
			try {
				preparedStatement.setString(1, applicationNo);
			} catch (SQLException e) {
				e.printStackTrace();
			}
		});
		
		jdbcTemplate.update(deleteAdditionalDetailsBuildingPlanLayoutIsSigned, preparedStatement -> {
			try {
				preparedStatement.setString(1, applicationNo);
			} catch (SQLException e) {
				e.printStackTrace();
			}
		});
		
		jdbcTemplate.update(deleteDSCDetailsQuery, preparedStatement -> {
			try {
				preparedStatement.setString(1, applicationNo);
			} catch (SQLException e) {
				e.printStackTrace();
			}
		});
		
	}
	
	
	public void updateApplicationForOneStepBack(BPA bpa) {
		List<Object> preparedStmtList = new ArrayList<>();
		String updateApplicationQuery = issueFixQueryBuilder.getApplicationUpdateQueryForOneStepBack();
		String deleteAdditionalDetailsSanFeeEstimate = issueFixQueryBuilder.getDeleteAdditionalDetailsSanFeeEstimateQuery();
		String deleteAdditionalDetailsEstimate = issueFixQueryBuilder.getDeleteAdditionalDetailsEstimateQuery();
		
		String query = issueFixQueryBuilder.getApplicationCurrentStatusQuery();
		preparedStmtList.add(bpa.getApplicationNo());
		List<String> updateStatus = jdbcTemplate.queryForList(query, preparedStmtList.toArray(), String.class);
		if(!CollectionUtils.isEmpty(updateStatus)) {
			jdbcTemplate.update(updateApplicationQuery, new PreparedStatementSetter() {
	
				@Override
				public void setValues(PreparedStatement ps) {
					try {
						ps.setString(1, updateStatus.get(0));
						ps.setString(2, bpa.getApplicationNo());
					} catch (SQLException e) {
						e.printStackTrace();
					}
				}
			});
			if (bpa.getStatus().equalsIgnoreCase(IssueFixConstants.PENDING_SANC_FEE)) {
				jdbcTemplate.update(deleteAdditionalDetailsSanFeeEstimate, new PreparedStatementSetter() {
					
					@Override
					public void setValues(PreparedStatement ps) {
						try {
							ps.setString(1, bpa.getApplicationNo());
						} catch (SQLException e) {
							e.printStackTrace();
						}
					}
				});
				
				jdbcTemplate.update(deleteAdditionalDetailsEstimate, new PreparedStatementSetter() {
		
					@Override
					public void setValues(PreparedStatement ps) {
						try {
							ps.setString(1, bpa.getApplicationNo());
						} catch (SQLException e) {
							e.printStackTrace();
						}
					}
				});
			}
		} else {
			log.error("No update status found for application no : " + bpa.getApplicationNo());
			throw new CustomException("APPLICATION_UPDATE_ISSUE", "No update status found for this application");
		}
	}

	public List<DscDetails> getPlanDscDetails(BPASearchCriteria criteria) {
		List<Object> preparedStmtList = new ArrayList<>();
		String query = issueFixQueryBuilder.getBPAPlanDscDetailsQuery(criteria, preparedStmtList);
		System.out.println("*************" + query);
		System.out.println("*********" + preparedStmtList);
		List<DscDetails> dscDetails = jdbcTemplate.query(query, preparedStmtList.toArray(), dscRowMapper);
		return dscDetails;
	}

	public void deletePlanDsc(@Valid BPA bpa) {
		String updateDemandDetailQuery = issueFixQueryBuilder.getDeletePlanDscQuery();

		jdbcTemplate.update(updateDemandDetailQuery, new PreparedStatementSetter() {

			@Override
			public void setValues(PreparedStatement ps) {
				try {
					ps.setString(1, bpa.getApplicationNo());
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});
		
	}

	public void updateReworkHistory(BPA bpa) {
		String updateReworkHistory = issueFixQueryBuilder.getReworkHistoryUpdateQuery();

		// String applicationType = bpa.getApplicationType();

		jdbcTemplate.update(updateReworkHistory, new PreparedStatementSetter() {

			@Override
			public void setValues(PreparedStatement ps) {
				try {
					ps.setString(1, bpa.getApplicationNo());
					ps.setObject(2, bpa.getReWorkHistory());				
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});
		
	}
	
}
