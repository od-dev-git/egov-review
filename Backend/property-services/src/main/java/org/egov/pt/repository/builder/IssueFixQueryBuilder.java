package org.egov.pt.repository.builder;

import org.egov.pt.models.collection.DemandSearchCriteria;
import org.egov.pt.models.collection.PaymentSearchCriteria;
import org.egov.pt.models.workflow.WorkFlowSearchCriteria;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

@Component
public class IssueFixQueryBuilder {

    public static final String DELETE_PROCESS_INSTANCE_RECORD ="delete from public.eg_wf_processinstance_v2 ";

    public static final String SELECT_PAYMENT_SQL = "SELECT py.*,pyd.*," +
            "py.id as py_id,py.tenantId as py_tenantId,py.totalAmountPaid as py_totalAmountPaid,py.createdBy as py_createdBy,py.createdtime as py_createdtime," +
            "py.lastModifiedBy as py_lastModifiedBy,py.lastmodifiedtime as py_lastmodifiedtime,py.additionalDetails as py_additionalDetails," +
            "pyd.id as pyd_id, pyd.tenantId as pyd_tenantId, pyd.manualreceiptnumber as manualreceiptnumber,pyd.manualreceiptdate as manualreceiptdate, pyd.createdBy as pyd_createdBy,pyd.createdtime as pyd_createdtime,pyd.lastModifiedBy as pyd_lastModifiedBy," +
            "pyd.lastmodifiedtime as pyd_lastmodifiedtime,pyd.additionalDetails as pyd_additionalDetails" +
            " FROM egcl_payment py  " +
            " INNER JOIN egcl_paymentdetail pyd ON pyd.paymentid = py.id " +
            " INNER JOIN egcl_bill bill ON bill.id = pyd.billid " +
            " INNER JOIN egcl_billdetial bd ON bd.billid = bill.id " ;

    public static final String BASE_DEMAND_QUERY = "SELECT dmd.id AS did,dmd.consumercode AS dconsumercode,"
            + "dmd.consumertype AS dconsumertype,dmd.businessservice AS dbusinessservice,dmd.payer,"
            + "dmd.billexpirytime AS dbillexpirytime, dmd.fixedBillExpiryDate as dfixedBillExpiryDate, "
            + "dmd.taxperiodfrom AS dtaxperiodfrom,dmd.taxperiodto AS dtaxperiodto,"
            + "dmd.minimumamountpayable AS dminimumamountpayable,dmd.createdby AS dcreatedby,"
            + "dmd.lastmodifiedby AS dlastmodifiedby,dmd.createdtime AS dcreatedtime,"
            + "dmd.lastmodifiedtime AS dlastmodifiedtime,dmd.tenantid AS dtenantid,dmd.status,"
            + "dmd.additionaldetails as demandadditionaldetails,dmd.ispaymentcompleted as ispaymentcompleted,"

            + "dmdl.id AS dlid,dmdl.demandid AS dldemandid,dmdl.taxheadcode AS dltaxheadcode,"
            + "dmdl.taxamount AS dltaxamount,dmdl.collectionamount AS dlcollectionamount,"
            + "dmdl.createdby AS dlcreatedby,dmdl.lastModifiedby AS dllastModifiedby,"
            + "dmdl.createdtime AS dlcreatedtime,dmdl.lastModifiedtime AS dllastModifiedtime,"
            + "dmdl.tenantid AS dltenantid,dmdl.additionaldetails as detailadditionaldetails " + "FROM egbs_demand_v1 dmd "
            + "INNER JOIN egbs_demanddetail_v1 dmdl ON dmd.id=dmdl.demandid " + "AND dmd.tenantid=dmdl.tenantid ";


    public static final String PAYMENT_QUERY_ORDER_BY_CLAUSE = " py.transactiondate DESC ";

    public static final String DEMAND_QUERY_ORDER_BY_CLAUSE = " dmd.taxperiodfrom DESC";

    public static final String WF_QUERY_ORDER_BY_CLAUSE = " wf.lastmodifiedtime DESC";

    public static final String PROCESS_INSTANCE_QUERY = "select * from eg_wf_processinstance_v2 wf ";

    public static final String INSERT_WORKFLOW_QUERY = "INSERT INTO public.eg_wf_processinstance_v2 "
            + "(id, tenantid, businessservice, businessid, action, status, comment, assigner, assignee, statesla, previousstatus, createdby, lastmodifiedby, createdtime, lastmodifiedtime, modulename, businessservicesla, rating) "
            + "VALUES( ? , ? , ? , ? , 'PAY' , ? , NULL, ? , NULL, 43200000, NULL, ? , ? , ? , ? , 'PT', 259052163, NULL)";

    public static final String DEMAND_UPDATE_QUERY = "update egbs_demand_v1 "
            + " set ispaymentcompleted = true "
            + " where consumercode = ?";

    public static final String DEMAND_DETAIL_UPDATE_QUERY = "update egbs_demanddetail_v1"
            + " set collectionamount =  taxamount "
            + " where id = ?";

    public static final String BILL_EXPIRE_QUERY = "update egbs_bill_v1 ebv "
            + "set status ='EXPIRED' "
            + "where id in (select billid from egbs_billdetail_v1 ebv2 where consumercode in (?)) and status = 'ACTIVE'";

    public static final String APPLICATION_UPDATE_QUERY = "update eg_pt_property "
            + " set status = ?, lastmodifiedtime = ? "
            + " where acknowldgementnumber = ?";

    public static final String APPLICATION_STATUS_ACTIVE_UPDATE_QUERY ="update eg_pt_property set status ='ACTIVE' where acknowldgementnumber=:acknowldgementnumber ";

    public static final String PROPERTY_SEARCH_QUERY ="Select * from eg_pt_property property where propertyid=?";
    
    public static final String PROPERTY_SEARCH_BY_ACKNOWLDGEMENT_NO_QUERY ="Select * from eg_pt_property where acknowldgementnumber=?";
    
    public static final String PROPERTY_ID_FETCH_QUERY ="Select propertyid from eg_pt_property property where acknowldgementnumber=?";
    
	public static final String GET_PAYMENT_ISSUES_APPLICATIONS_QUERY = "select pt.acknowldgementnumber, pt.tenantid, txn.module "
			+ "from eg_pt_property pt "
			+ "inner join eg_pg_transactions txn on txn.consumer_code=pt.acknowldgementnumber "
			+ "where pt.status in ('INWORKFLOW') and pt.creationreason = 'MUTATION' and txn.txn_status='SUCCESS' and txn.module='PT.MUTATION' ";
	
	private static final String SEARCH_STATUS_MISMATCH_ISSUE_APPLICATIONS = "select ebb.tenantid, ebb.acknowldgementnumber, ewpv.action as actionInProcessInstance, ebb.status as currentStatus, ewsv.applicationstatus as expectedStatus "
			+ " from eg_pt_property ebb inner join ( select distinct on (businessid) * from eg_wf_processinstance_v2 order by businessid, createdtime desc) ewpv on ewpv.businessid = ebb.acknowldgementnumber  inner join eg_wf_state_v2 ewsv on ewsv.uuid = ewpv.status where ebb.status <> ewsv.applicationstatus and ebb.createdtime >= ( SELECT (EXTRACT(EPOCH from DATE_TRUNC('year', current_timestamp))*1000 + 19800000)) and ebb.status not in ('INACTIVE','DEACTIVATED')";
	
	public static final String APPLICATION_UPDATE_QUERY_FOR_SEND_BACK = "update eg_pt_property "
			+ " set status = ?  where propertyid = ?";
	
	public static final String DELETE_WORKFLOW_QUERY = "delete from public.eg_wf_processinstance_v2 where businessid = ? and id = ?";

	public static final String DELETE_DEMAND_QUERY_FOR_APPPLICATION_STEP_BACK = "delete from public.egbs_demand_v1 where id = ?";

	public static final String DELETE_DEMAND_DETAIL_QUERY_FOR_APPPLICATION_STEP_BACK = "DELETE FROM public.egbs_demanddetail_v1 WHERE id = ?";

	public static final String INSERT_DEMAND_QUERY_FOR_APPPLICATION_STEP_BACK_IN_AUDIT = "INSERT INTO public.egbs_demand_v1_audit "
			+ "(id, demandid, consumercode, consumertype, businessservice, payer, taxperiodfrom, taxperiodto, createdby, createdtime, tenantid, minimumamountpayable, status, additionaldetails, billexpirytime, ispaymentcompleted, fixedbillexpirydate) "
			+ "VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?); ";
	
	public static final String INSERT_DEMAND_DETAIL_QUERY_FOR_APPPLICATION_STEP_BACK_IN_AUDIT = "INSERT INTO public.egbs_demanddetail_v1_audit "
            + "(id, demandid, demanddetailid, taxheadcode, taxamount, collectionamount, createdby, createdtime, tenantid, additionaldetails) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
	
    public static final String PROPERTY_SEARCH_BY_ACKNOWLDGEMENT_NO_AND_TENANTID_QUERY ="Select * from eg_pt_property where acknowldgementnumber=? and tenantid=? ";

	
    public String getApplicationStatusMismatchIssueQuery(List<String> idList, List<Object> preparedStatementList){

        StringBuilder deleteQuery = new StringBuilder(DELETE_PROCESS_INSTANCE_RECORD);

        if(!CollectionUtils.isEmpty(idList)){
            addClauseIfRequired(preparedStatementList,deleteQuery);
            deleteQuery.append("id in (").append(createQuery(idList)).append(")");
            preparedStatementList.add(idList);
        }


        return deleteQuery.toString();
    }

    private String createQuery(List<String> ids) {
        StringBuilder builder = new StringBuilder();
        int length = ids.size();
        for (int i = 0; i < length; i++) {
            builder.append(" ?");
            if (i != length - 1)
                builder.append(",");
        }
        return builder.toString();
    }
    private void addClauseIfRequired(List<Object> values, StringBuilder queryString) {
        if (values.isEmpty())
            queryString.append(" WHERE ");
        else {
            queryString.append(" AND");
        }
    }

    public String getPaymentSearchQuery(PaymentSearchCriteria paymentSearchCriteria,
                                        Map<String, Object> preparedStatementValues) {
        StringBuilder selectQuery = new StringBuilder(SELECT_PAYMENT_SQL);

        if(!StringUtils.isEmpty(paymentSearchCriteria.getTenantId())) {
            addClauseIfRequired(preparedStatementValues, selectQuery);
            selectQuery.append(" py.tenantId =:tenantId ");
            preparedStatementValues.put("tenantId", paymentSearchCriteria.getTenantId());

        }

        if(!StringUtils.isEmpty(paymentSearchCriteria.getConsumerCode())) {
            addClauseIfRequired(preparedStatementValues, selectQuery);
            selectQuery.append(" bill.consumerCode in (:consumerCode) ");
            preparedStatementValues.put("consumerCode", paymentSearchCriteria.getConsumerCode());

        }

        addOrderByClause(selectQuery, PAYMENT_QUERY_ORDER_BY_CLAUSE);

        return selectQuery.toString();
    }

    private static void addOrderByClause(StringBuilder query,String columnName) {
        query.append(" ORDER BY " + columnName);
    }

    private void addClauseIfRequired(Map<String, Object> preparedStatementValues, StringBuilder selectQuery) {

        if (preparedStatementValues.isEmpty())
            selectQuery.append(" WHERE ");
        else {
            selectQuery.append(" AND");
        }

    }

    public String getDemandSearchQuery(DemandSearchCriteria demandSearchCriteria,
                                       Map<String, Object> preparedStatementValues) {

        StringBuilder selectQuery = new StringBuilder(BASE_DEMAND_QUERY);

        if(!StringUtils.isEmpty(demandSearchCriteria.getTenantId())) {
            addClauseIfRequired(preparedStatementValues, selectQuery);
            selectQuery.append(" dmd.tenantid = :tenantId and  dmd.status != 'CANCELLED'");
            preparedStatementValues.put("tenantId", demandSearchCriteria.getTenantId());

        }

        if(!StringUtils.isEmpty(demandSearchCriteria.getConsumerCode())) {
            addClauseIfRequired(preparedStatementValues, selectQuery);
            selectQuery.append(" dmd.consumercode in (:consumerCode) ");
            preparedStatementValues.put("consumerCode", demandSearchCriteria.getConsumerCode());

        }

        addOrderByClause(selectQuery, DEMAND_QUERY_ORDER_BY_CLAUSE);

        return selectQuery.toString();
    }

    public String getProcessInstancesQuery(WorkFlowSearchCriteria workFlowSearchCriteria,
                                           Map<String, Object> preparedStatementValues) {

        StringBuilder selectQuery = new StringBuilder(PROCESS_INSTANCE_QUERY);

        if(!StringUtils.isEmpty(workFlowSearchCriteria.getTenantId())) {
            addClauseIfRequired(preparedStatementValues, selectQuery);
            selectQuery.append(" wf.tenantid = :tenantId ");
            preparedStatementValues.put("tenantId", workFlowSearchCriteria.getTenantId());

        }

        if(!StringUtils.isEmpty(workFlowSearchCriteria.getBusinessId())) {
            addClauseIfRequired(preparedStatementValues, selectQuery);
            selectQuery.append(" wf.businessid = :businessId ");
            preparedStatementValues.put("businessId", workFlowSearchCriteria.getBusinessId());

        }

        addOrderByClause(selectQuery, WF_QUERY_ORDER_BY_CLAUSE);

        return selectQuery.toString();
    }

    public String getInsertWorkflowQuery() {
        return INSERT_WORKFLOW_QUERY;
    }

    public String getDemandUpdateQuery() {
        return DEMAND_UPDATE_QUERY;
    }

    public String getDemandDetailUpdateQuery() {
        return DEMAND_DETAIL_UPDATE_QUERY;
    }

    public String getBillExpireQuery() {
        return BILL_EXPIRE_QUERY;
    }

    public String getApplicationUpdateQuery() {
        return APPLICATION_UPDATE_QUERY;
    }

    public String getApplicationActivateUpdateQuery(){
        return APPLICATION_STATUS_ACTIVE_UPDATE_QUERY;
    }

    public String getPropertySearchQuery(){
        return PROPERTY_SEARCH_QUERY;
    }
    
    public String getPropertySearchByAcknowledgmentNoQuery(){
        return PROPERTY_SEARCH_BY_ACKNOWLDGEMENT_NO_QUERY;
    }
    
    public String getPropertyId(){
        return PROPERTY_ID_FETCH_QUERY;
    }
    
    public String getPaymentIssueAppliactionsQuery() {
        return GET_PAYMENT_ISSUES_APPLICATIONS_QUERY;
    }
    
    public String getStatusMismatchAppliactionsQuery() {
		return SEARCH_STATUS_MISMATCH_ISSUE_APPLICATIONS;		
	}
    
    public String getApplicationUpdateQueryForSendBack() {
		return APPLICATION_UPDATE_QUERY_FOR_SEND_BACK;
	}

	public String getDeleteWorkflowQuery() {
		return DELETE_WORKFLOW_QUERY;
	}

	public String getDeleteDemandForApplicationStepBack() {
		return DELETE_DEMAND_QUERY_FOR_APPPLICATION_STEP_BACK;
	}

	public String getInsertDemandQueryForApplicationStepBackInAudit() {
		return INSERT_DEMAND_QUERY_FOR_APPPLICATION_STEP_BACK_IN_AUDIT;
	}

	public String getDeleteDemandDetailsQueryForApplicationStepBack() {
		return DELETE_DEMAND_DETAIL_QUERY_FOR_APPPLICATION_STEP_BACK;
	}

	public String getInsertDemandDetailsQueryForApplicationStepBackInAudit() {
		return INSERT_DEMAND_DETAIL_QUERY_FOR_APPPLICATION_STEP_BACK_IN_AUDIT;
	}

	public String getPropertySearchByAcknowledgmentNoAndTenantidQuery() {
        return PROPERTY_SEARCH_BY_ACKNOWLDGEMENT_NO_AND_TENANTID_QUERY;
	}
}
