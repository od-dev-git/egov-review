package org.egov.pt.repository;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.validation.constraints.NotNull;

import org.egov.pt.models.Property;
import org.egov.pt.models.collection.DemandSearchCriteria;
import org.egov.pt.models.collection.Payment;
import org.egov.pt.models.collection.PaymentSearchCriteria;
import org.egov.pt.models.enums.CreationReason;
import org.egov.pt.models.issuefix.PaymentIssueFix;
import org.egov.pt.models.issuefix.StatusMismatchIssueFix;
import org.egov.pt.models.workflow.ProcessInstance;
import org.egov.pt.models.workflow.WorkFlowSearchCriteria;
import org.egov.pt.repository.builder.IssueFixQueryBuilder;
import org.egov.pt.repository.rowmapper.DemandRowMapper;
import org.egov.pt.repository.rowmapper.IssueFixPropertyRowMapper;
import org.egov.pt.repository.rowmapper.PaymentIssueFixRowMapper;
import org.egov.pt.repository.rowmapper.PaymentRowMapper;
import org.egov.pt.repository.rowmapper.ProcessInstanceRowMapper;
import org.egov.pt.repository.rowmapper.StatusMismatchIssueRowMapper;
import org.egov.pt.util.IssueFixConstants;
import org.egov.pt.web.contracts.Demand;
import org.egov.pt.web.contracts.DemandDetail;
import org.egov.tracer.model.CustomException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import lombok.extern.slf4j.Slf4j;

@Repository
@Slf4j
public class IssueFixRepository {

    @Autowired
    private IssueFixQueryBuilder issueFixQueryBuilder;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @Autowired
    private IssueFixPropertyRowMapper issueFixPropertyRowMapper;
    
    @Autowired
    private PaymentIssueFixRowMapper paymentIssueFixRowMapper;
    
	@Autowired
    private StatusMismatchIssueRowMapper statusMismatchIssueRowMapper;
    
    public String getPropertyId(String id){
    	String query= issueFixQueryBuilder.getPropertyId();
        List<Object> preparedStatementValues = new ArrayList<>();
        preparedStatementValues.add(id);
        String propertyId= jdbcTemplate.queryForObject(query, preparedStatementValues.toArray(), String.class);
        return propertyId;
    }


    public void updateApplicationStatusMismatch(List<String> id){
        List<Object> preparedStatementList = new ArrayList<>();
        String applicationStatusMismatchIssueQuery= issueFixQueryBuilder.getApplicationStatusMismatchIssueQuery(id,preparedStatementList);

        jdbcTemplate.update(applicationStatusMismatchIssueQuery, preparedStatement -> {
            try {
                for(int i=0;i<id.size();i++) {
                    preparedStatement.setString(i+1, id.get(i));
                }
            } catch (SQLException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        });
    }

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

    public List<ProcessInstance> getProcessInstances(WorkFlowSearchCriteria workFlowSearchCriteria) {

        Map<String, Object> preparedStatementValues = new HashMap<>();

        String queryForWFSearch = issueFixQueryBuilder.getProcessInstancesQuery(workFlowSearchCriteria,
                preparedStatementValues);

        List<ProcessInstance> processInstances = namedParameterJdbcTemplate.query(queryForWFSearch, preparedStatementValues,
                new ProcessInstanceRowMapper());

        return processInstances;
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
                    ps.setString(5, IssueFixConstants.PAY_STATUS);
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

    public void updateApplication(Property property, Payment payment) {

        String updateApplicationQuery = issueFixQueryBuilder.getApplicationUpdateQuery();

        jdbcTemplate.update(updateApplicationQuery, new PreparedStatementSetter() {

            @Override
            public void setValues(PreparedStatement ps) {
                try {

                    ps.setString(1, IssueFixConstants.STATUS_ACTIVE);
                    ps.setLong(2, payment.getAuditDetails().getCreatedTime());
                    ps.setString(3, property.getAcknowldgementNumber());
                } catch (SQLException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        });

    }

    public void updateInactiveApplicationStatus(Property property){
        String updateInactiveApplicationsQuery= issueFixQueryBuilder.getApplicationActivateUpdateQuery();
        Map<String, Object> inputs = new HashMap<>();
        inputs.put("acknowldgementnumber", property.getAcknowldgementNumber());
        namedParameterJdbcTemplate.update(updateInactiveApplicationsQuery, inputs);
    }

    public List<Property> searchProperty(String propertyId){
        String query= issueFixQueryBuilder.getPropertySearchQuery();
        List<Object> preparedStatementValues = new ArrayList<>();
        preparedStatementValues.add(propertyId);
        List<Property> propertyList= jdbcTemplate.query(query, preparedStatementValues.toArray(), issueFixPropertyRowMapper);
        return propertyList;
    }
    
    public List<Property> searchPropertyByAcknowledgementNo(String acknowledgementNo){
        String query= issueFixQueryBuilder.getPropertySearchByAcknowledgmentNoQuery();
        List<Object> preparedStatementValues = new ArrayList<>();
        preparedStatementValues.add(acknowledgementNo);
        List<Property> propertyList= jdbcTemplate.query(query, preparedStatementValues.toArray(), issueFixPropertyRowMapper);
        return propertyList;
    }
    
    public List<Property> searchPropertyByAcknowledgementNoAndTenantid(String acknowledgementNo,String tenantid){
        String query= issueFixQueryBuilder.getPropertySearchByAcknowledgmentNoAndTenantidQuery();
        List<Object> preparedStatementValues = new ArrayList<>();
        preparedStatementValues.add(acknowledgementNo);
        preparedStatementValues.add(tenantid);
        List<Property> propertyList= jdbcTemplate.query(query, preparedStatementValues.toArray(), issueFixPropertyRowMapper);
        return propertyList;
    }
    
    public List<PaymentIssueFix> getPaymentIssueApplications() {
		String query = issueFixQueryBuilder.getPaymentIssueAppliactionsQuery();
		log.info("*************" + query);
		List<PaymentIssueFix> paymentIssueFixApplications = jdbcTemplate.query(query, paymentIssueFixRowMapper);
		return paymentIssueFixApplications;
	}
    
	public List<StatusMismatchIssueFix> getStatusMismatchApplications() {
		String query = issueFixQueryBuilder.getStatusMismatchAppliactionsQuery();
		System.out.println("*************" + query);
		List<StatusMismatchIssueFix> statusMismatchIssueFixs = jdbcTemplate.query(query,
				statusMismatchIssueRowMapper);
		return statusMismatchIssueFixs;
	}


	@Transactional
	public void updateApplicationForStepBack(Property property) {

		String updateApplicationQuery = issueFixQueryBuilder.getApplicationUpdateQueryForSendBack();

		jdbcTemplate.update(updateApplicationQuery, new PreparedStatementSetter() {

			@Override
			public void setValues(PreparedStatement ps) {
				try {
					ps.setString(1, IssueFixConstants.STATUS_INWORKFLOW);
					ps.setString(2, property.getAcknowldgementNumber());
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		});

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


		public void deleteDemandForApplicationStepBack(String demandID) {
			String updateApplicationQuery = issueFixQueryBuilder.getDeleteDemandForApplicationStepBack();

			jdbcTemplate.update(updateApplicationQuery, new PreparedStatementSetter() {

				@Override
				public void setValues(PreparedStatement ps) {
					try {
						ps.setString(1, demandID);
					} catch (SQLException e) {
						e.printStackTrace();
					}
				}
			});
		}


		public void insertDemandForApplicationStepBackInAudit(Demand demand) {
		    if (demand == null) {
		        throw new CustomException("INVALID_DEMAND", "Demand object cannot be null.");
		    }

		    String insertDemandQuery = issueFixQueryBuilder.getInsertDemandQueryForApplicationStepBackInAudit();
		    
		    // Prepare values for the prepared statement from the demand object
		    jdbcTemplate.update(insertDemandQuery, new PreparedStatementSetter() {

		        @Override
		        public void setValues(PreparedStatement ps) {
		            try {
		            	ps.setString(1, UUID.randomUUID().toString());
		            	ps.setString(2, demand.getId());
		            	ps.setString(3, demand.getConsumerCode());
		            	ps.setString(4, demand.getConsumerType());
		            	ps.setString(5, demand.getBusinessService());
		            	ps.setString(6, demand.getPayer() != null ? demand.getPayer().getUuid() : null);
		            	ps.setLong(7, demand.getTaxPeriodFrom());
		            	ps.setLong(8, demand.getTaxPeriodTo());
		            	ps.setString(9, demand.getAuditDetails().getCreatedBy());
		            	ps.setLong(10, demand.getAuditDetails().getCreatedTime());
		            	ps.setString(11, demand.getTenantId());
		            	ps.setBigDecimal(12, demand.getMinimumAmountPayable());
		            	ps.setString(13, demand.getStatus().toString());
		            	ps.setObject(14, demand.getAdditionalDetails());
		            	ps.setLong(15, demand.getBillExpiryTime());
		            	ps.setBoolean(16, demand.getIsPaymentCompleted());
		            	ps.setLong(17, demand.getFixedBillExpiryDate());
		            } catch (SQLException e) {
		                throw new CustomException("SQL_INSERT_ERROR", "Error while setting values for inserting demand: " + e.getMessage());
		            }
		        }
		    });
		}


		public void insertDemandDetailsForApplicationStepBackInAudit(List<DemandDetail> demandDetails) {
			String insertDemandDetailQuery = issueFixQueryBuilder.getInsertDemandDetailsQueryForApplicationStepBackInAudit();

		    for (DemandDetail detail : demandDetails) {
		        jdbcTemplate.update(insertDemandDetailQuery, new PreparedStatementSetter() {
		            @Override
		            public void setValues(PreparedStatement ps) {
		                try {
		                    ps.setString(1, detail.getId());
		                    ps.setString(2, detail.getDemandId());
		                    ps.setString(3, detail.getId()); 
		                    ps.setString(4, detail.getTaxHeadMasterCode());
		                    ps.setBigDecimal(5, detail.getTaxAmount());
		                    ps.setBigDecimal(6, detail.getCollectionAmount());
		                    ps.setString(7, detail.getAuditDetails().getCreatedBy());
		                    ps.setLong(8, detail.getAuditDetails().getCreatedTime());
		                    ps.setString(9, detail.getTenantId());
		                    ps.setObject(10,null); 
		                } catch (SQLException e) {
		                    e.printStackTrace();
		                }
		            }
		        });
		    }
		}



		public void deleteDemandDetailsForApplicationStepBack(List<DemandDetail> demandDetails) {
			String deleteDemandDetailQuery = issueFixQueryBuilder.getDeleteDemandDetailsQueryForApplicationStepBack();


		    for (DemandDetail detail : demandDetails) {
		        jdbcTemplate.update(deleteDemandDetailQuery, new PreparedStatementSetter() {
		            @Override
		            public void setValues(PreparedStatement ps) {
		                try {
		                    ps.setString(1, detail.getId()); 
		                } catch (SQLException e) {
		                    e.printStackTrace();
		                }
		            }
		        });
		    }
		}






	
}
