package org.egov.bpa.repository;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.lang3.ObjectUtils;
import org.egov.bpa.config.BPAConfiguration;
import org.egov.bpa.repository.querybuilder.IssueFixQueryBuilder;
import org.egov.bpa.repository.rowmapper.DemandRowMapper;
import org.egov.bpa.repository.rowmapper.PaymentIssueFixRowMapper;
import org.egov.bpa.repository.rowmapper.PaymentRowMapper;
import org.egov.bpa.repository.rowmapper.ProcessInstanceRowMapper;
import org.egov.bpa.repository.rowmapper.RegularizationDscDetailsRowMapper;
import org.egov.bpa.repository.rowmapper.StatusMismatchIssueRowMapper;
import org.egov.bpa.util.IssueFixConstants;
import org.egov.bpa.web.model.collection.Demand;
import org.egov.bpa.web.model.collection.DemandDetail;
import org.egov.bpa.web.model.collection.DemandSearchCriteria;
import org.egov.bpa.web.model.collection.Payment;
import org.egov.bpa.web.model.collection.PaymentSearchCriteria;
import org.egov.bpa.web.model.idgen.IdResponse;
import org.egov.bpa.web.model.issuefix.IssueFix;
import org.egov.bpa.web.model.issuefix.PaymentIssueFix;
import org.egov.bpa.web.model.issuefix.StatusMismatchIssueFix;
import org.egov.bpa.web.model.regularization.Regularization;
import org.egov.bpa.web.model.regularization.RegularizationDscDetails;
import org.egov.bpa.web.model.regularization.RegularizationSearchCriteria;
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
import org.springframework.util.StringUtils;

@Repository
public class RegularizationIssueFixRepository {

	@Autowired
	private IssueFixQueryBuilder issueFixQueryBuilder;

	@Autowired
	private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

	@Autowired
	private JdbcTemplate jdbcTemplate;
	
	@Autowired
	private BPAConfiguration config;
	
	@Autowired
	private IdGenRepository idGenRepository;
	
	@Autowired
	private RegularizationDscDetailsRowMapper dscDetailsRowMapper;


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



	public void expireBill(String consumerCode) {

		String billExpireQuery = issueFixQueryBuilder.getBillExpireQuery();

		jdbcTemplate.update(billExpireQuery, new PreparedStatementSetter() {

			@Override
			public void setValues(PreparedStatement ps) {
				try {
					ps.setString(1, consumerCode);
				} catch (SQLException e) {
					e.printStackTrace();
				}
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
					e.printStackTrace();
				}
			}
		});

	}
	

    
	@Transactional
	public void updateApplicationForStepBack(Regularization regularization) {

		String updateApplicationQuery = issueFixQueryBuilder.getRegularizationApplicationUpdateQueryForSendBack();
		
		jdbcTemplate.update(updateApplicationQuery, new PreparedStatementSetter() {

			@Override
			public void setValues(PreparedStatement ps) {
				try {
					ps.setString(1, IssueFixConstants.BPA_STATUS_APPROVAL_INPROGRESS);
					ps.setString(2, regularization.getApplicationNo());
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		});

		if(!ObjectUtils.isEmpty(regularization.getAdditionalDetails())) {
			String deleteAdditionalDetailsEstimate = issueFixQueryBuilder.getRegularizationDeleteAdditionalDetailsEstimateQuery();

			jdbcTemplate.update(deleteAdditionalDetailsEstimate, new PreparedStatementSetter() {

				@Override
				public void setValues(PreparedStatement ps) {
					try {
						ps.setString(1, regularization.getApplicationNo());
					} catch (SQLException e) {
						e.printStackTrace();
					}
				}
			});
		}

	}

	
	public void updateWorkflowForStepBack(ProcessInstance processInstance) {
		String insertWorkFlowQuery = issueFixQueryBuilder.getDeleteWorkflowQuery();

		jdbcTemplate.update(insertWorkFlowQuery, new PreparedStatementSetter() {

			@Override
			public void setValues(PreparedStatement ps) {
				try {
					ps.setString(1, processInstance.getBusinessId());
					ps.setString(2, processInstance.getId());

				} catch (SQLException e) {
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

	
	public void deleteDscDetails(Regularization regularization) {

		String deleteDscDetailQuery = issueFixQueryBuilder.getDeleteDscQuery();

		jdbcTemplate.update(deleteDscDetailQuery, new PreparedStatementSetter() {
			@Override
			public void setValues(PreparedStatement ps) {
				try {
					ps.setString(1, regularization.getApplicationNo());
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		});

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
					e.printStackTrace();
				}
			}
		});
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

	
	public void updateApplication(Regularization regularization, Payment payment) {
		String updateApplicationQuery = issueFixQueryBuilder.getRegularizationApplicationUpdateQuery();

		jdbcTemplate.update(updateApplicationQuery, new PreparedStatementSetter() {

			@Override
			public void setValues(PreparedStatement ps) {
				try {
					
					ps.setString(1, IssueFixConstants.REG_DOC_VERIFICATION_INPROGRESS);
					ps.setLong(2, payment.getAuditDetails().getCreatedTime());
					ps.setLong(3, payment.getAuditDetails().getCreatedTime());
					ps.setString(4, regularization.getApplicationNo());
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		});
		
	}

	
	@Transactional
	public void updateWorkflow(ProcessInstance processInstance, Payment payment) {
		
		String getWorkFlowStatusQuery = issueFixQueryBuilder.getWorkflowStatusQuery();
		String insertWorkFlowQuery = issueFixQueryBuilder.getInsertWorkflowQuery();
		
		String businessService = processInstance.getBusinessService();
		
		List<Object> preparedStmtList = new ArrayList<>();
		preparedStmtList.add(businessService);
		preparedStmtList.add(IssueFixConstants.REG_DOC_VERIFICATION_INPROGRESS);
		
		List<String> stateIdList = jdbcTemplate.queryForList(getWorkFlowStatusQuery, preparedStmtList.toArray(), String.class);
		
		if(!CollectionUtils.isEmpty(stateIdList) && !StringUtils.isEmpty(stateIdList.get(0))) {
			String stateId = stateIdList.get(0);
			jdbcTemplate.update(insertWorkFlowQuery, new PreparedStatementSetter() {
				
				@Override
				public void setValues(PreparedStatement ps) {
					try {
						ps.setString(1, UUID.randomUUID().toString());
						ps.setString(2, processInstance.getTenantId());
						ps.setString(3, businessService);
						ps.setString(4, processInstance.getBusinessId());
						ps.setString(5, stateId);
						ps.setString(6, payment.getPayerId());
						ps.setString(7, payment.getPayerId());
						ps.setString(8, payment.getPayerId());
						ps.setLong(9, payment.getAuditDetails().getCreatedTime());
						ps.setLong(10, payment.getAuditDetails().getCreatedTime());

					} catch (SQLException e) {
						e.printStackTrace();
					}
				}
			});
		} 
	}

	
	public void updateApplicationForSancFeeIssueFix(Regularization regularization, Payment payment,
			RequestInfo requestInfo) {
		String updateApplicationQuery = issueFixQueryBuilder.getRegularizationApplicationUpdateQueryForSancFee();
		List<IdResponse> idResponses = idGenRepository.getId(requestInfo, regularization.getTenantId(),
				config.getRegularizationPermitName(), config.getRegularizationPermitFormat(), 1).getIdResponses();
		regularization.setApprovalNo(idResponses.get(0).getId());

		jdbcTemplate.update(updateApplicationQuery, new PreparedStatementSetter() {

			@Override
			public void setValues(PreparedStatement ps) {
				try {
					ps.setString(1, IssueFixConstants.APPROVED);
					ps.setLong(2, payment.getAuditDetails().getCreatedTime());
					ps.setString(3, regularization.getApprovalNo());
					ps.setString(4, regularization.getApplicationNo());
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		});
	}

	
	@Transactional
	public void updateWorkflowForSancFee(ProcessInstance processInstance, Payment payment) {
		String getWorkFlowStatusQuery = issueFixQueryBuilder.getWorkflowStatusQuery();
		String insertWorkFlowQuery = issueFixQueryBuilder.getInsertWorkflowQuery();
		
		String businessService = processInstance.getBusinessService();
		
		List<Object> preparedStmtList = new ArrayList<>();
		preparedStmtList.add(businessService);
		preparedStmtList.add(IssueFixConstants.APPROVED);
		
		List<String> stateIdList = jdbcTemplate.queryForList(getWorkFlowStatusQuery, preparedStmtList.toArray(), String.class);
		
		if(!CollectionUtils.isEmpty(stateIdList) && !StringUtils.isEmpty(stateIdList.get(0))) {
			String stateId = stateIdList.get(0);
			jdbcTemplate.update(insertWorkFlowQuery, new PreparedStatementSetter() {
	
				@Override
				public void setValues(PreparedStatement ps) {
					try {
						ps.setString(1, UUID.randomUUID().toString());
						ps.setString(2, processInstance.getTenantId());
						ps.setString(3, businessService);
						ps.setString(4, processInstance.getBusinessId());
						ps.setString(5, stateId);
						ps.setString(6, payment.getPayerId());
						ps.setString(7, payment.getPayerId());
						ps.setString(8, payment.getPayerId());
						ps.setLong(9, payment.getAuditDetails().getCreatedTime());
						ps.setLong(10, payment.getAuditDetails().getCreatedTime());
	
					} catch (SQLException e) {
						e.printStackTrace();
					}
				}
			});
		}
	}

	
	public void updateApplicationStatusMismatch(List<String> idList) {
		List<Object> preparedStatementList = new ArrayList<>();
		String applicationStatusMismatchIssueQuery = issueFixQueryBuilder.getApplicationStatusMismatchIssueQuery(idList, preparedStatementList);

		jdbcTemplate.update(applicationStatusMismatchIssueQuery, preparedStatement -> {
			try {
				for (int i = 0; i < idList.size(); i++) {
					preparedStatement.setString(i + 1, idList.get(i));
				}
			} catch (SQLException e) {
				e.printStackTrace();
			}
		});
		
	}

	public List<PaymentIssueFix> getPaymentIssueApplications() {
		String query = issueFixQueryBuilder.getPaymentIssueAppliactionsQueryForRegularization();
		System.out.println("*************" + query);
		List<PaymentIssueFix> paymentIssueFixApplications = jdbcTemplate.query(query, new PaymentIssueFixRowMapper());
		return paymentIssueFixApplications;
	}

	public List<StatusMismatchIssueFix> getStatusMismatchApplications() {
		String query = issueFixQueryBuilder.getStatusMismatchAppliactionsQueryForRegularization();
		System.out.println("*************" + query);
		List<StatusMismatchIssueFix> statusMismatchIssueFixs = jdbcTemplate.query(query, new StatusMismatchIssueRowMapper());
		return statusMismatchIssueFixs;
	}

	public void updateRegularizationDscApprover(Regularization regularization, String newApprover) {
		String updateRegularizationDscApproverQuery = issueFixQueryBuilder.getUpdateRegularizationDscApproverQuery();

		jdbcTemplate.update(updateRegularizationDscApproverQuery, preparedStatement -> {
			try {
				preparedStatement.setString(1, newApprover);
				preparedStatement.setString(2, regularization.getApplicationNo());
				preparedStatement.setString(3, regularization.getTenantId());

			} catch (SQLException e) {
				e.printStackTrace();
			}
		});
	}

	
	public List<String> getRegularizationDSC(IssueFix issueFix) {
		List<Object> preparedStmtList = new ArrayList<>();
		String query = issueFixQueryBuilder.getRegularizationDSCQuery(issueFix, preparedStmtList);
		System.out.println("Query: " + query);
		System.out.println("preparedStmtList: " + preparedStmtList);
		
		List<String> data = jdbcTemplate.queryForList(query, preparedStmtList.toArray(), String.class);
		System.out.println("Data: " + data);
		return data;
	}
	

	public void updateRegularizationDSC(IssueFix issueFix) {
		String query = issueFixQueryBuilder.deleteRegularizationDuplicateDscQuery();
		System.out.println("Query: " + query);
		jdbcTemplate.update(query, preparedStatement -> {
			try {
				preparedStatement.setString(1, issueFix.getApplicationNo());
				preparedStatement.setString(2, issueFix.getTenantId());
				preparedStatement.setString(3, issueFix.getApplicationNo());
			} catch (SQLException e) {
				e.printStackTrace();
			}
		});
	}

	@Transactional
	public void deleteRegularizationDSCDetails(String applicationNo) {
		String deleteAdditionalDetailsUnsignedSitePlanLayoutDetails = issueFixQueryBuilder.getRegDeleteAdditionalDetailsUnsignedSitePlanLayoutDetailsQuery();
		String deleteAdditionalDetailsSitePlanLayoutIsSigned = issueFixQueryBuilder.getRegDeleteAdditionalDetailsSitePlanLayoutIsSignedQuery();
		
		String deleteAdditionalDetailsUnsignedBuildingPlanLayoutDetails = issueFixQueryBuilder.getRegDeleteAdditionalDetailsUnsignedBuildingPlanLayoutDetailsQuery();
		String deleteAdditionalDetailsBuildingPlanLayoutIsSigned = issueFixQueryBuilder.getRegDeleteAdditionalDetailsBuildingPlanLayoutIsSignedQuery();
		
		String deleteDSCDetailsQuery = issueFixQueryBuilder.getRegularizationDeleteDscDetailsQuery();
		
		jdbcTemplate.update(deleteAdditionalDetailsUnsignedSitePlanLayoutDetails, preparedStatement -> {
			try {
				preparedStatement.setString(1, applicationNo);
			} catch (SQLException e) {
				e.printStackTrace();
			}
		});
		
		jdbcTemplate.update(deleteAdditionalDetailsSitePlanLayoutIsSigned, preparedStatement -> {
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

	
	@Transactional
	public void updateApplicationForOneStepBack(Regularization regularization) {
		List<Object> preparedStmtList = new ArrayList<>();
		String query = issueFixQueryBuilder.getApplicationCurrentStatusQuery();
		preparedStmtList.add(regularization.getApplicationNo());
		List<String> updateStatus = jdbcTemplate.queryForList(query, preparedStmtList.toArray(), String.class);
		if(!CollectionUtils.isEmpty(updateStatus)) {
			String updateApplicationQuery = issueFixQueryBuilder.getRegularizationApplicationUpdateQueryForOneStepBack();
			
			jdbcTemplate.update(updateApplicationQuery, new PreparedStatementSetter() {
	
				@Override
				public void setValues(PreparedStatement ps) {
					try {
						ps.setString(1, updateStatus.get(0));
						ps.setString(2, regularization.getApplicationNo());
					} catch (SQLException e) {
						e.printStackTrace();
					}
				}
			});
			
			if (regularization.getStatus().equalsIgnoreCase(IssueFixConstants.PENDING_SANC_FEE) && 
					!(ObjectUtils.isEmpty(regularization.getAdditionalDetails()))) {
				String deleteAdditionalDetailsSanFeeEstimate = issueFixQueryBuilder.getRegularizationDeleteAdditionalDetailsSanFeeEstimateQuery();
				String deleteAdditionalDetailsEstimate = issueFixQueryBuilder.getRegularizationDeleteAdditionalDetailsEstimateQuery();
				
				jdbcTemplate.update(deleteAdditionalDetailsSanFeeEstimate, new PreparedStatementSetter() {
					
					@Override
					public void setValues(PreparedStatement ps) {
						try {
							ps.setString(1, regularization.getApplicationNo());
						} catch (SQLException e) {
							e.printStackTrace();
						}
					}
				});
				
				jdbcTemplate.update(deleteAdditionalDetailsEstimate, new PreparedStatementSetter() {
		
					@Override
					public void setValues(PreparedStatement ps) {
						try {
							ps.setString(1, regularization.getApplicationNo());
						} catch (SQLException e) {
							e.printStackTrace();
						}
					}
				});
			}
			
		} else {
			throw new CustomException("APPLICATION_UPDATE_ISSUE", "No update status found for this application");
		}
	}

	public List<RegularizationDscDetails> searchRegularizationDscDetails(RegularizationSearchCriteria searchCriteria,
			RequestInfo requestInfo) {
		List<Object> preparedStmtList = new ArrayList<>();
	    String query = issueFixQueryBuilder.getRegularizationDscDetailsSearchQuery(searchCriteria, preparedStmtList);
	    List<RegularizationDscDetails> regularizationDscDetailsList =  jdbcTemplate.query(query, preparedStmtList.toArray(), dscDetailsRowMapper);
		
		if(regularizationDscDetailsList.isEmpty())
			return Collections.emptyList();
		return regularizationDscDetailsList;
	}
	
	

}
