package org.egov.bpa.repository.querybuilder;

import java.util.List;
import java.util.Map;

import org.egov.bpa.web.model.BPASearchCriteria;
import org.egov.bpa.web.model.InstallmentSearchCriteria;
import org.egov.bpa.web.model.collection.DemandSearchCriteria;
import org.egov.bpa.web.model.collection.PaymentSearchCriteria;
import org.egov.bpa.web.model.issuefix.IssueFix;
import org.egov.bpa.web.model.regularization.RegularizationSearchCriteria;
import org.egov.bpa.web.model.workflow.WorkFlowSearchCriteria;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

@Component
public class IssueFixQueryBuilder {

	public static final String SELECT_PAYMENT_SQL = "SELECT py.*,pyd.*,"
			+ "py.id as py_id,py.tenantId as py_tenantId,py.totalAmountPaid as py_totalAmountPaid,py.createdBy as py_createdBy,py.createdtime as py_createdtime,"
			+ "py.lastModifiedBy as py_lastModifiedBy,py.lastmodifiedtime as py_lastmodifiedtime,py.additionalDetails as py_additionalDetails,"
			+ "pyd.id as pyd_id, pyd.tenantId as pyd_tenantId, pyd.manualreceiptnumber as manualreceiptnumber,pyd.manualreceiptdate as manualreceiptdate, pyd.createdBy as pyd_createdBy,pyd.createdtime as pyd_createdtime,pyd.lastModifiedBy as pyd_lastModifiedBy,"
			+ "pyd.lastmodifiedtime as pyd_lastmodifiedtime,pyd.additionalDetails as pyd_additionalDetails"
			+ " FROM egcl_payment py  " + " INNER JOIN egcl_paymentdetail pyd ON pyd.paymentid = py.id "
			+ " INNER JOIN egcl_bill bill ON bill.id = pyd.billid "
			+ " INNER JOIN egcl_billdetial bd ON bd.billid = bill.id ";

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
			+ "dmdl.tenantid AS dltenantid,dmdl.additionaldetails as detailadditionaldetails "
			+ "FROM egbs_demand_v1 dmd " + "INNER JOIN egbs_demanddetail_v1 dmdl ON dmd.id=dmdl.demandid "
			+ "AND dmd.tenantid=dmdl.tenantid ";

	public static final String PAYMENT_QUERY_ORDER_BY_CLAUSE = " py.transactiondate DESC ";

	public static final String DEMAND_QUERY_ORDER_BY_CLAUSE = " dmd.taxperiodfrom DESC";

	public static final String WF_QUERY_ORDER_BY_CLAUSE = " wf.lastmodifiedtime DESC";

	public static final String DEMAND_UPDATE_QUERY = "update egbs_demand_v1 " + " set ispaymentcompleted = true "
			+ " where consumercode = ?";

	public static final String DEMAND_DETAIL_UPDATE_QUERY = "update egbs_demanddetail_v1"
			+ " set collectionamount =  taxamount " + " where id = ?";

	public static final String DEMAND_DELETE_QUERY = "delete from egbs_demand_v1 where consumercode = ? and businessservice = ? and id = ?";
	
	public static final String DEMAND_V1_DELETE_QUERY = "\r\n"
			+ "WITH duplicates AS (\r\n"
			+ "    SELECT id, ROW_NUMBER() OVER (PARTITION BY consumercode ORDER BY id) AS rnum\r\n"
			+ "    FROM egbs_demand_v1\r\n"
			+ "    WHERE consumercode = ? AND businessservice= ? \r\n"
			+ ")\r\n"
			+ "DELETE FROM egbs_demand_v1\r\n"
			+ "WHERE id IN (\r\n"
			+ "    SELECT id FROM duplicates WHERE rnum > 1\r\n"
			+ ") AND consumercode = ?";

	public static final String DEMAND_DETAIL_DELETE_QUERY = "delete from egbs_demanddetail_v1 where demandid = ?";
	
	public static final String DEMAND_DETAIL_DELETE_QUERY_BY_ID = "delete from egbs_demanddetail_v1 where id = ?";
	
	public static final String INSTALLMENT_DELETE_QUERY = "delete from eg_bpa_installment where consumercode = ?";
	
	public static final String DSC_DELETE_QUERY = "delete from eg_bpa_dscdetails where applicationno = ?";

	public static final String PLAN_DSC_DELETE_QUERY = "delete from eg_bpa_plan_dscdetails where applicationno = ?";

	public static final String APPLICATION_UPDATE_QUERY = "update eg_bpa_buildingplan "
			+ " set status = ?, applicationdate = ?, lastmodifiedtime = ? " + " where applicationno = ?";

	public static final String APPLICATION_UPDATE_QUERY_FOR_SEND_BACK = "update eg_bpa_buildingplan "
			+ " set status = ? , additionaldetails = additionaldetails - 'san_estimate'" + " where applicationno = ?";
	
	public static final String APPLICATION_UPDATE_QUERY_FOR_ONE_STEP_BACK = "update eg_bpa_buildingplan "
			+ " set status = ?  where applicationno = ?";
	
	public static final String DELETE_ADDITIONAL_DETAILS_SAN_FEE_ESTIMATE = "update eg_bpa_buildingplan "
			+ " set additionaldetails = additionaldetails - 'san_estimate'" + " where applicationno = ?";
	
	public static final String DELETE_ADDITIONAL_DETAILS_ESTIMATE = "update eg_bpa_buildingplan "
			+ " set additionaldetails = additionaldetails - 'estimate'" + " where applicationno = ?";
	
	public static final String APPLICATION_UPDATE_QUERY_FOR_SANC_FEE = "update eg_bpa_buildingplan "
			+ " set status = ?, lastmodifiedtime = ? , approvalno = ?" + " where applicationno = ?";

	public static final String INSERT_WORKFLOW_QUERY = "INSERT INTO public.eg_wf_processinstance_v2 "
			+ "(id, tenantid, businessservice, businessid, action, status, comment, assigner, assignee, statesla, previousstatus, createdby, lastmodifiedby, createdtime, lastmodifiedtime, modulename, businessservicesla, rating) "
			+ "VALUES( ? , ? , ? , ? , 'PAY' , ? , NULL, ? , NULL, 43200000, NULL, ? , ? , ? , ? , 'bpa-services', 259052163, NULL)";

	public static final String DELETE_WORKFLOW_QUERY = "delete from public.eg_wf_processinstance_v2 where businessid = ? and id = ?";

	public static final String BILL_EXPIRE_QUERY = "update egbs_bill_v1 ebv " + "set status ='EXPIRED' "
			+ "where id in (select billid from egbs_billdetail_v1 ebv2 where consumercode in (?)) and status = 'ACTIVE'";

	public static final String PROCESS_INSTANCE_QUERY = "select * from eg_wf_processinstance_v2 wf ";

	public static final String UPDATE_DSC_DETAILS = "update eg_bpa_dscdetails set documenttype=null, documentid=null where id =?";

	public static final String UPDATE_PLAN_DSC_DETAILS = "update eg_bpa_plan_dscdetails set documenttype=null, documentid=null where id =?";

	public static final String UPDATE_DSC_APPROVER = "update eg_bpa_dscdetails ehe set approvedby =? where ehe.applicationno =? and ehe.tenantid=?";
	
	public static final String UPDATE_PLAN_DSC_APPROVER = "update eg_bpa_plan_dscdetails ehe set approvedby =? where ehe.applicationno =? and ehe.tenantid=?";


	public static final String DELETE_PROCESS_INSTANCE_RECORD = "delete from public.eg_wf_processinstance_v2 ";

	private static final String UUID_SEARCH = "select uuid from eg_hrms_employee ehe ";
	
	private static final String DELETE_DUPLICATE = "WITH CTE AS (SELECT *, ROW_NUMBER() OVER(PARTITION BY applicationno"
			+ " ORDER BY id) AS DuplicateCount FROM eg_bpa_dscdetails where applicationno = ? and tenantid = ?)"
			+ "delete from eg_bpa_dscdetails ebd where applicationno = ? and id not in "
			+ "(SELECT id FROM CTE where DuplicateCount = 1)";
	
	private static final String DELETE_PLAN_DSC_DUPLICATE = "WITH CTE AS (SELECT *, ROW_NUMBER() OVER(PARTITION BY applicationno"
			+ " ORDER BY id) AS DuplicateCount FROM eg_bpa_plan_dscdetails where applicationno = ? and tenantid = ?)"
			+ "delete from eg_bpa_plan_dscdetails ebd where applicationno = ? and id not in "
			+ "(SELECT id FROM CTE where DuplicateCount = 1)";

	private static final String DSC_SEARCH = "select id from eg_bpa_dscdetails ebd ";
	
	private static final String PLAN_DSC_SEARCH = "select id from eg_bpa_plan_dscdetails ebd ";
	
	private static final String PROCESSINSTANCE_ID="with foo as(select id, action from eg_wf_processinstance_v2 where businessid = ? order by lastmodifiedtime desc limit 1) select foo.id from foo where foo.action in ('SEND_TO_CITIZEN','SEND_BACK_TO_CITIZEN', 'SENDBACK_TO_ARCHITECT_FOR_REWORK')";
	
	public static final String CURRENT_STATUS_QUERY = "select applicationstatus from eg_wf_state_v2 st where uuid in (select status from eg_wf_processinstance_v2 pi where pi.businessid = ? order by lastmodifiedtime desc limit 1); ";

	private static final String ASSIGNER_UPDATE="update eg_wf_processinstance_v2 set assigner = ? ,createdby = ? ,lastmodifiedby = ? "
			+ "where businessid = ? and id = ?";
	
	
	private static final String APPLICATIONDOC_ID="select id from eg_tl_applicationdocument where tradelicensedetailid ="
			+"(select id from eg_tl_tradelicensedetail where tradelicenseid = "
			+ "(select id from eg_tl_tradelicense where applicationnumber = ?))";
	
	private static final String UPDATE="update eg_tl_applicationdocument set active = true where id in (";
	
	private static final String UPDATE_ADDITIONAL_DETAILS_UNSIGNED_BP = "UPDATE eg_bpa_buildingplan \r\n"
			+ "SET additionaldetails = additionaldetails - 'unsignedBuildingPlanLayoutDetails'\r\n"
			+ "WHERE applicationno = ?";
	
	private static final String UPDATE_ADDITIONAL_DETAILS_BP_LAYOUT_IS_SIGNED = "UPDATE eg_bpa_buildingplan \r\n"
			+ "SET additionaldetails = additionaldetails - 'buildingPlanLayoutIsSigned' \r\n"
			+ "WHERE applicationno = ?";

	private static final String DELETE_SIGNED_BPL_DOCUMENT_QUERY = "delete from eg_bpa_document where id = ? and documenttype = 'BPD.SIGNED.BPL'";
	
	private static final String SEARCH_PAYMENT_ISSUE_APPLICATIONS = "select issues.tenantid, issues.applicationno, issues.module from (select bpa.tenantid, bpa.applicationno, txn.module "
			+ " from eg_bpa_buildingplan bpa inner join eg_pg_transactions txn on txn.consumer_code=bpa.applicationno where bpa.status in ('PENDING_APPL_FEE') and txn.txn_status='SUCCESS' and txn.module='BPA.NC_APP_FEE' union all select bpa.tenantid, bpa.applicationno, txn.module from eg_bpa_buildingplan bpa inner join eg_pg_transactions txn on txn.consumer_code=bpa.applicationno where bpa.status ='PENDING_SANC_FEE_PAYMENT' and txn.txn_status='SUCCESS' and txn.module='BPA.NC_SAN_FEE') issues ";
    
	private static final String SEARCH_STATUS_MISMATCH_ISSUE_APPLICATIONS = "select ebb.tenantid,ebb.applicationno,ewpv.action as actionInProcessInstance,ebb.status as currentStatus, ewsv.applicationstatus as expectedStatus "
			+ "from eg_bpa_buildingplan ebb inner join (select distinct on(businessid) * from eg_wf_processinstance_v2 order by businessid,createdtime desc) ewpv on ewpv.businessid=ebb.applicationno inner join eg_wf_state_v2 ewsv on ewsv.uuid=ewpv.status where ebb.status<>ewsv.applicationstatus and ebb.status not in ('DELETED') and ewsv.applicationstatus not in ('CITIZEN_APPROVAL_INPROCESS','INITIATED','PENDING_APPL_FEE','INPROGRESS')";
	
	private static final String SEARCH_PAYMENT_ISSUE_APPLICATIONS_REGULARIZATION = "select issues.tenantid, issues.applicationno, issues.module from ( select ebra.tenantid, ebra.applicationno, txn.module "
			+ "from eg_bpa_regularization_application ebra inner join eg_pg_transactions txn on txn.consumer_code = ebra.applicationno where ebra.status in ('PENDING_APPL_FEE') and txn.txn_status = 'SUCCESS' and txn.module = 'BPA.REG_APP_FEE' union all select ebra.tenantid, ebra.applicationno, txn.module from eg_bpa_regularization_application ebra inner join eg_pg_transactions txn on txn.consumer_code = ebra.applicationno where ebra.status = 'PENDING_SANC_FEE_PAYMENT' and txn.txn_status = 'SUCCESS' and txn.module = 'BPA.REG_SAN_FEE') issues";
	
	private static final String SEARCH_STATUS_MISMATCH_ISSUE_APPLICATIONS_REGULARIZATION = "select ebra.tenantid, ebra.applicationno, ebra.apptype as applicationtype, ewpv.action as actionInProcessInstance, ebra.status as currentStatus, ewsv.applicationstatus as expectedStatus "
			+ "from eg_bpa_regularization_application ebra inner join ( select distinct on (businessid) * from eg_wf_processinstance_v2 order by businessid, createdtime desc) ewpv on ewpv.businessid = ebra.applicationno inner join eg_wf_state_v2 ewsv on ewsv.uuid = ewpv.status where ebra.status <> ewsv.applicationstatus and ebra.status not in ('DELETED') and ewsv.applicationstatus not in ('CITIZEN_APPROVAL_INPROCESS', 'INITIATED', 'PENDING_APPL_FEE', 'INPROGRESS')";
	
	public static final String REWORK_HISTORY_UPDATE_QUERY = "update eg_bpa_buildingplan "
			+ " set reworkhistory = ? " + " where applicationno = ?";
	
	public String getIDs() {	
		
		return APPLICATIONDOC_ID;
	}
	
	public String update(int length) {
		
		StringBuilder queryBuilder = new StringBuilder(UPDATE);
	    for (int i = 0; i < length; i++) {
	        queryBuilder.append(" ?");
	        if (i != length - 1)
	            queryBuilder.append(",");
	    }
	    queryBuilder.append(")");
	    queryBuilder.append(" and active is null ");
	    return queryBuilder.toString();
		
//		return UPDATE;
	}
    		
	
	public String getProcessInstanceId() {	
		
		return PROCESSINSTANCE_ID;
	}
	
	public String getApplicationCurrentStatusQuery() {	
		return CURRENT_STATUS_QUERY;
	}
	
	public String getUpdateAssignerQuery() {
		return ASSIGNER_UPDATE;
		
	}

	public String searchDscQuery(String issueSubtype) {

		if (issueSubtype == null) {
			throw new IllegalArgumentException("Issue subtype cannot be null");
		}

		if (issueSubtype.equalsIgnoreCase("PERMIT_DSC")) {
			return DELETE_DUPLICATE;
		} else if (issueSubtype.equalsIgnoreCase("BASE_LAYER_DSC")) {
			return DELETE_PLAN_DSC_DUPLICATE;
		} else {
			throw new IllegalArgumentException("Unknown issue subtype: " + issueSubtype);
		}
	}

	public String getDSC(IssueFix issueFix, List<Object> preparedStmtList) {
		StringBuilder builder = new StringBuilder();
		if (issueFix.getIssueSubtype().equalsIgnoreCase("PERMIT_DSC")) {
			builder.append(DSC_SEARCH);
		} else if (issueFix.getIssueSubtype().equalsIgnoreCase("BASE_LAYER_DSC")) {
			builder.append(PLAN_DSC_SEARCH);
		}

		addClauseIfRequired(preparedStmtList, builder);
		builder.append(" ebd.applicationno =? ");
		preparedStmtList.add(issueFix.getApplicationNo());

		addClauseIfRequired(preparedStmtList, builder);
		builder.append(" ebd.tenantid =? ");
		preparedStmtList.add(issueFix.getTenantId());

		return builder.toString();
	}
	
	private static final String DELETE_DUPLICATE_INSTALLMENT = "delete from eg_bpa_installment where id not in (\r\n"
			+ "select distinct on (installmentno, taxheadcode) id from eg_bpa_installment where consumercode = ?\r\n"
			+ "group by installmentno, taxheadcode, id)\r\n"
			+ "and consumercode = ?";
	
	private static final String UPDATE_INSTALLMENT_DEMANDID = "update eg_bpa_installment set demandid = null where consumercode = ? ";

	private static final String INSTALLMENT_SEARCH = "select id from eg_bpa_installment ebi";
	
	private static final String CHECK_DUPLICATES = "with foo as(select installmentno, taxheadcode, count(*) as count from eg_bpa_installment where consumercode = ?\r\n"
			+ "group by installmentno, taxheadcode)select count from foo where count >1;";

	public String getInstallmentDeleteQuery() {
		return DELETE_DUPLICATE_INSTALLMENT;
	}
	
	public String getUpdateInstallmentDemandIdQuery() {
		return UPDATE_INSTALLMENT_DEMANDID;
	}

	public String getInstallment(IssueFix issueFix, List<Object> preparedStmtList) {


		StringBuilder builder = new StringBuilder(INSTALLMENT_SEARCH);

		addClauseIfRequired(preparedStmtList, builder);
		builder.append(" ebi.consumercode =? ");
		preparedStmtList.add(issueFix.getApplicationNo());

		return builder.toString();		
	}
	
	public String checkDuplicates() {

		return CHECK_DUPLICATES;		
	}

	private static final String GET_INSTALLMENT_QUERY = "SELECT ebi.id as ebi_id,ebi.tenantid as ebi_tenantid,ebi.installmentno as ebi_installmentno,ebi.status as ebi_status,ebi.consumercode as ebi_consumercode,ebi.taxheadcode as ebi_taxheadcode,ebi.taxamount as ebi_taxamount,ebi.demandid as ebi_demandid,ebi.ispaymentcompletedindemand as ebi_ispaymentcompletedindemand,ebi.additional_details as ebi_additional_details,ebi.createdby as ebi_createdby,ebi.lastmodifiedby as ebi_lastmodifiedby,ebi.createdtime as ebi_createdtime,ebi.lastmodifiedtime as ebi_lastmodifiedtime "
			+ "FROM eg_bpa_installment ebi ";

	private static final String GET_BPA_DSC_DETAILS_QUERY = " SELECT ebb.additionaldetails as buildingadditionaldetails, ebd.* FROM eg_bpa_dscdetails ebd inner join eg_bpa_buildingplan ebb on ebb.applicationno = ebd.applicationno ";

	private static final String GET_BPA_PLAN_DSC_DETAILS_QUERY = " SELECT ebb.additionaldetails as buildingadditionaldetails, ebd.* FROM eg_bpa_plan_dscdetails ebd inner join eg_bpa_buildingplan ebb on ebb.applicationno = ebd.applicationno ";

	public String getUUID(String newApproval, String tenantId, List<Object> preparedStmtList) {

		StringBuilder builder = new StringBuilder(UUID_SEARCH);

		if (newApproval != null) {
			addClauseIfRequired(preparedStmtList, builder);
			builder.append(" ehe.code =? ");
			preparedStmtList.add(newApproval);
		}
		
		if (tenantId != null) {
			addClauseIfRequired(preparedStmtList, builder);
			builder.append(" ehe.tenantid =? ");
			preparedStmtList.add(tenantId);
		}
		
		addClauseIfRequired(preparedStmtList, builder);
		builder.append(" ehe.active is true ");

		return builder.toString();
	}

	public String getPaymentSearchQuery(PaymentSearchCriteria paymentSearchCriteria,
			Map<String, Object> preparedStatementValues) {
		StringBuilder selectQuery = new StringBuilder(SELECT_PAYMENT_SQL);

		if (!StringUtils.isEmpty(paymentSearchCriteria.getTenantId())) {
			addClauseIfRequired(preparedStatementValues, selectQuery);
			selectQuery.append(" py.tenantId =:tenantId ");
			preparedStatementValues.put("tenantId", paymentSearchCriteria.getTenantId());

		}

		if (!StringUtils.isEmpty(paymentSearchCriteria.getConsumerCode())) {
			addClauseIfRequired(preparedStatementValues, selectQuery);
			selectQuery.append(" bill.consumerCode in (:consumerCode) ");
			preparedStatementValues.put("consumerCode", paymentSearchCriteria.getConsumerCode());

		}

		if (!StringUtils.isEmpty(paymentSearchCriteria.getBusinessService())) {
			addClauseIfRequired(preparedStatementValues, selectQuery);
			selectQuery.append(" pyd.businessService IN (:businessService) ");
			preparedStatementValues.put("businessService", paymentSearchCriteria.getBusinessService());

		}

		addOrderByClause(selectQuery, PAYMENT_QUERY_ORDER_BY_CLAUSE);

		return selectQuery.toString();
	}

	private static void addOrderByClause(StringBuilder query, String columnName) {
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

		if (!StringUtils.isEmpty(demandSearchCriteria.getTenantId())) {
			addClauseIfRequired(preparedStatementValues, selectQuery);
			selectQuery.append(" dmd.tenantid = :tenantId and  dmd.status != 'CANCELLED'");
			preparedStatementValues.put("tenantId", demandSearchCriteria.getTenantId());

		}

		if (!StringUtils.isEmpty(demandSearchCriteria.getConsumerCode())) {
			addClauseIfRequired(preparedStatementValues, selectQuery);
			selectQuery.append(" dmd.consumercode in (:consumerCode) ");
			preparedStatementValues.put("consumerCode", demandSearchCriteria.getConsumerCode());

		}

		if (!StringUtils.isEmpty(demandSearchCriteria.getBusinessService())) {
			addClauseIfRequired(preparedStatementValues, selectQuery);
			selectQuery.append(" dmd.businessservice IN (:businessService) ");
			preparedStatementValues.put("businessService", demandSearchCriteria.getBusinessService());

		}

		addOrderByClause(selectQuery, DEMAND_QUERY_ORDER_BY_CLAUSE);

		return selectQuery.toString();
	}

	public String getDemandUpdateQuery() {
		return DEMAND_UPDATE_QUERY;
	}

	public String getDemandDetailUpdateQuery() {
		return DEMAND_DETAIL_UPDATE_QUERY;
	}

	public String getDemandDeleteQuery() {
		return DEMAND_DELETE_QUERY;
	}
	
	public String getDemandV1DeleteQuery() {
		return DEMAND_V1_DELETE_QUERY;
	}

	public String getDemandDetailDeleteQuery() {
		return DEMAND_DETAIL_DELETE_QUERY;
	}
	
	public String getDemandDetailDeleteQueryById() {
		return DEMAND_DETAIL_DELETE_QUERY_BY_ID;
	}

	public String getProcessInstancesQuery(WorkFlowSearchCriteria workFlowSearchCriteria,
			Map<String, Object> preparedStatementValues) {

		StringBuilder selectQuery = new StringBuilder(PROCESS_INSTANCE_QUERY);

		if (!StringUtils.isEmpty(workFlowSearchCriteria.getTenantId())) {
			addClauseIfRequired(preparedStatementValues, selectQuery);
			selectQuery.append(" wf.tenantid = :tenantId ");
			preparedStatementValues.put("tenantId", workFlowSearchCriteria.getTenantId());

		}

		if (!StringUtils.isEmpty(workFlowSearchCriteria.getBusinessId())) {
			addClauseIfRequired(preparedStatementValues, selectQuery);
			selectQuery.append(" wf.businessid = :businessId ");
			preparedStatementValues.put("businessId", workFlowSearchCriteria.getBusinessId());

		}

		addOrderByClause(selectQuery, WF_QUERY_ORDER_BY_CLAUSE);

		return selectQuery.toString();
	}

	public String getApplicationUpdateQuery() {
		return APPLICATION_UPDATE_QUERY;
	}

	public String getApplicationUpdateQueryForSendBack() {
		return APPLICATION_UPDATE_QUERY_FOR_SEND_BACK;
	}
	
	public String getApplicationUpdateQueryForOneStepBack() {
		return APPLICATION_UPDATE_QUERY_FOR_ONE_STEP_BACK;
	}
	
	public String getDeleteAdditionalDetailsSanFeeEstimateQuery() {
		return DELETE_ADDITIONAL_DETAILS_SAN_FEE_ESTIMATE;
	}
	
	public String getDeleteAdditionalDetailsEstimateQuery() {
		return DELETE_ADDITIONAL_DETAILS_ESTIMATE;
	}
	
	
	public String getInsertWorkflowQuery() {
		return INSERT_WORKFLOW_QUERY;
	}

	public String getDeleteWorkflowQuery() {
		return DELETE_WORKFLOW_QUERY;
	}
	
	public String getDeleteInstallmentQuery() {
		return INSTALLMENT_DELETE_QUERY;
	}
	
	public String getDeleteDscQuery() {
		return DSC_DELETE_QUERY;
	}

	public String getBillExpireQuery() {
		return BILL_EXPIRE_QUERY;
	}

	public String getApplicationUpdateQueryForSancFee() {
		return APPLICATION_UPDATE_QUERY_FOR_SANC_FEE;
	}

	public String getInstallmentSearchQuery(InstallmentSearchCriteria criteria, List<Object> preparedStmtList) {

		StringBuilder builder = new StringBuilder(GET_INSTALLMENT_QUERY);

		if (criteria.getConsumerCode() != null) {
			addClauseIfRequired(preparedStmtList, builder);
			builder.append(" ebi.consumercode=? ");
			preparedStmtList.add(criteria.getConsumerCode());
		}

		builder.append(" ORDER BY ebi_installmentno ASC");
		return builder.toString();
		// return addPaginationWrapper(builder.toString(), preparedStmtList, criteria);

	}

	public String getUpdateDscDetailsQuery() {
		return UPDATE_DSC_DETAILS;
	}
	
	public String getUpdatePlanDscDetailsQuery() {
		return UPDATE_PLAN_DSC_DETAILS;
	}

	public String getUpdateDscApproverQuery(String issueSubtype) {
		if (issueSubtype == null) {
	        throw new IllegalArgumentException("Issue subtype cannot be null");
	    }
		
		if (issueSubtype.equalsIgnoreCase("PERMIT_DSC")) {
			return UPDATE_DSC_APPROVER;
		} else if (issueSubtype.equalsIgnoreCase("BASE_LAYER_DSC")) {
			return UPDATE_PLAN_DSC_APPROVER;
		} else {
			throw new IllegalArgumentException("Unknown issue subtype: " + issueSubtype);
		}
	}

	public String getApplicationStatusMismatchIssueQuery(List<String> idList, List<Object> preparedStatementList) {

		StringBuilder deleteQuery = new StringBuilder(DELETE_PROCESS_INSTANCE_RECORD);

		if (!CollectionUtils.isEmpty(idList)) {
			addClauseIfRequired(preparedStatementList, deleteQuery);
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

	public String getBPADscDetailsQuery(BPASearchCriteria criteria, List<Object> preparedStmtList) {
		StringBuilder builder = new StringBuilder(GET_BPA_DSC_DETAILS_QUERY);
		if (criteria.getTenantId() != null) {
			addClauseIfRequired(preparedStmtList, builder);
			builder.append(" ebd.tenantid = ? ");
			preparedStmtList.add(criteria.getTenantId());
		}
		if (criteria.getApplicationNo() != null) {
			addClauseIfRequired(preparedStmtList, builder);
			builder.append(" ebd.applicationno = ? ");
			preparedStmtList.add(criteria.getApplicationNo());
		}

		builder.append(" AND  ebd.documentid is null ");
		return builder.toString();
	}

	public String getdeleteBPLSigneddocumentQuery() {
		return DELETE_SIGNED_BPL_DOCUMENT_QUERY;		
	}
	
	public String getPaymentIssueAppliactionsQuery() {
		return SEARCH_PAYMENT_ISSUE_APPLICATIONS;		
	}
	
	public String getStatusMismatchAppliactionsQuery() {
		return SEARCH_STATUS_MISMATCH_ISSUE_APPLICATIONS;		
	}

	public String getupdateAdditionalDetailsUnSignedBP() {
		return UPDATE_ADDITIONAL_DETAILS_UNSIGNED_BP;		

	}

	public String getupdateAdditionalDetailsBPLayoutIsSigned() {
		return UPDATE_ADDITIONAL_DETAILS_BP_LAYOUT_IS_SIGNED;
	}

	public static final String APPLICATION_STAUS_UPDATE_QUERY = "update eg_bpa_buildingplan "
			+ " set status = ?, lastmodifiedtime = ?" + " where applicationno = ?";

	public String getApplicationStatusUpdateQuery() {
		return APPLICATION_STAUS_UPDATE_QUERY;
	}
	
	public static final String DELETE_OTHER_FEE_DETAILS_QUERY = "update eg_bpa_buildingplan "
			+ " set additionaldetails = additionaldetails - 'otherFees' where applicationno = ?";

	public String getDeleteOtherFeeDetailsQuery() {
		return DELETE_OTHER_FEE_DETAILS_QUERY;
	}
	
	public static final String REG_APPLICATION_UPDATE_QUERY_FOR_SEND_BACK = "update eg_bpa_regularization_application "
			+ " set status = ? , additionaldetails = additionaldetails - 'san_estimate'" + " where applicationno = ?";
	
	public String getRegularizationApplicationUpdateQueryForSendBack() {
		return REG_APPLICATION_UPDATE_QUERY_FOR_SEND_BACK;
	}
	
	public static final String REG_APPLICATION_UPDATE_QUERY_FOR_ONE_STEP_BACK = "update eg_bpa_regularization_application "
			+ " set status = ? where applicationno = ?";
	
	public String getRegularizationApplicationUpdateQueryForOneStepBack() {
		return REG_APPLICATION_UPDATE_QUERY_FOR_ONE_STEP_BACK;
	}
	
	public static final String REG_DELETE_ADDITIONAL_DETAILS_SAN_FEE_ESTIMATE = "update eg_bpa_regularization_application "
			+ " set additionaldetails = additionaldetails - 'san_estimate'" + " where applicationno = ?";
	
	public String getRegularizationDeleteAdditionalDetailsSanFeeEstimateQuery() {
		return REG_DELETE_ADDITIONAL_DETAILS_SAN_FEE_ESTIMATE;
	}
	
	public static final String REG_DELETE_ADDITIONAL_DETAILS_ESTIMATE = "update eg_bpa_regularization_application "
			+ " set additionaldetails = additionaldetails - 'estimate'" + " where applicationno = ?";
	
	public String getRegularizationDeleteAdditionalDetailsEstimateQuery() {
		return REG_DELETE_ADDITIONAL_DETAILS_ESTIMATE;
	}
	
	public static final String REG_DSC_DELETE_QUERY = "delete from eg_bpa_regularization_dscdetails where applicationno = ?";
	
	public String getRegularizationDeleteDscQuery() {
		return REG_DSC_DELETE_QUERY;
	}

	public static final String DELETE_PREVIEW_PERMIT_LETTER_FILE_STORE_ID_QUERY = "update eg_bpa_buildingplan "
			+ " set additionaldetails = additionaldetails - 'previewPermitLetterFileStoreId' where applicationno = ?";
	
	public String getDeleteAdditionalDetailsPreviewPermitLetterFileStoreIdQuery() {
		return DELETE_PREVIEW_PERMIT_LETTER_FILE_STORE_ID_QUERY;
	}

	public static final String DELETE_UNSIGNED_BUILDING_PLAN_LAYOUT_DETAILS_QUERY = "update eg_bpa_buildingplan "
			+ " set additionaldetails = additionaldetails - 'unsignedBuildingPlanLayoutDetails' where applicationno = ?";
	
	public String getDeleteAdditionalDetailsUnsignedBuildingPlanLayoutDetailsQuery() {
		return DELETE_UNSIGNED_BUILDING_PLAN_LAYOUT_DETAILS_QUERY;
	}

	public static final String DELETE_BUILDING_PLAN_LAYOUT_IS_SIGNED_QUERY = "update eg_bpa_buildingplan "
			+ " set additionaldetails = additionaldetails - 'buildingPlanLayoutIsSigned' where applicationno = ?";
	
	public String getDeleteAdditionalDetailsBuildingPlanLayoutIsSignedQuery() {
		return DELETE_BUILDING_PLAN_LAYOUT_IS_SIGNED_QUERY;
	}

	public static final String DELETE_DSC_DETAILS_QUERY = "update eg_bpa_dscdetails set documenttype=null, documentid=null where applicationno = ? ";
	
	public String getDeleteDscDetailsQuery() {
		return DELETE_DSC_DETAILS_QUERY;
	}
	
	public static final String REG_APPLICATION_UPDATE_QUERY = "update eg_bpa_regularization_application "
			+ " set status = ?, applicationdate = ?, lastmodifiedtime = ? " + " where applicationno = ?";
	
	public String getRegularizationApplicationUpdateQuery() {
		return REG_APPLICATION_UPDATE_QUERY;
	}
	
	public static final String REG_APPLICATION_UPDATE_QUERY_FOR_SANC_FEE = "update eg_bpa_regularization_application "
			+ " set status = ?, lastmodifiedtime = ? , approvalno = ?" + " where applicationno = ?";

	public String getRegularizationApplicationUpdateQueryForSancFee() {
		return REG_APPLICATION_UPDATE_QUERY_FOR_SANC_FEE;
	}
	
	public static final String GET_WORKFLOW_STATE_BY_BUSINESSSERVICE_AND_STATUS = "select uuid from eg_wf_state_v2 "
			+ " where businessserviceid = (select uuid from eg_wf_businessservice_v2  where businessservice = ? ) "
			+ " and applicationstatus = ? ";
	
	public String getWorkflowStatusQuery() {
		return GET_WORKFLOW_STATE_BY_BUSINESSSERVICE_AND_STATUS;
	}

	public String getPaymentIssueAppliactionsQueryForRegularization() {
		return SEARCH_PAYMENT_ISSUE_APPLICATIONS_REGULARIZATION;
	}

	public String getStatusMismatchAppliactionsQueryForRegularization() {
		return SEARCH_STATUS_MISMATCH_ISSUE_APPLICATIONS_REGULARIZATION;
	}
	
	public static final String REG_UPDATE_DSC_APPROVER = "update eg_bpa_regularization_dscdetails dsc set approvedby =? where dsc.applicationno =? and dsc.tenantid=?";

	public String getUpdateRegularizationDscApproverQuery() {
		return REG_UPDATE_DSC_APPROVER;
	}

	private static final String REG_DSC_SEARCH = "select id from eg_bpa_regularization_dscdetails dsc ";
	
	public String getRegularizationDSCQuery(IssueFix issueFix, List<Object> preparedStmtList) {
		StringBuilder builder = new StringBuilder(REG_DSC_SEARCH);

		addClauseIfRequired(preparedStmtList, builder);
		builder.append(" dsc.applicationno =? ");
		preparedStmtList.add(issueFix.getApplicationNo());

		addClauseIfRequired(preparedStmtList, builder);
		builder.append(" dsc.tenantid =? ");
		preparedStmtList.add(issueFix.getTenantId());

		return builder.toString();	
	}
	
	private static final String REG_DELETE_DUPLICATE = "WITH CTE AS (SELECT *, ROW_NUMBER() OVER(PARTITION BY applicationno"
			+ " ORDER BY id) AS DuplicateCount FROM eg_bpa_regularization_dscdetails where applicationno = ? and tenantid = ?)"
			+ "delete from eg_bpa_regularization_dscdetails ebd where applicationno = ? and id not in "
			+ "(SELECT id FROM CTE where DuplicateCount = 1)";

	public String deleteRegularizationDuplicateDscQuery() {
		return REG_DELETE_DUPLICATE;
	}

	public static final String REG_DELETE_UNSIGNED_BUILDING_PLAN_LAYOUT_DETAILS_QUERY = "update eg_bpa_regularization_application "
			+ " set additionaldetails = additionaldetails - 'unsignedBuildingPlanLayoutDetails' where applicationno = ?";
	
	public String getRegDeleteAdditionalDetailsUnsignedBuildingPlanLayoutDetailsQuery() {
		return REG_DELETE_UNSIGNED_BUILDING_PLAN_LAYOUT_DETAILS_QUERY;
	}

	public static final String REG_DELETE_BUILDING_PLAN_LAYOUT_IS_SIGNED_QUERY = "update eg_bpa_regularization_application "
			+ " set additionaldetails = additionaldetails - 'buildingPlanLayoutIsSigned' where applicationno = ?";
	
	public String getRegDeleteAdditionalDetailsBuildingPlanLayoutIsSignedQuery() {
		return REG_DELETE_BUILDING_PLAN_LAYOUT_IS_SIGNED_QUERY;
	}
	
	public static final String REG_DELETE_UNSIGNED_SITE_PLAN_LAYOUT_DETAILS_QUERY = "update eg_bpa_regularization_application "
			+ " set additionaldetails = additionaldetails - 'unsignedSitePlanLayoutDetails' where applicationno = ?";
	
	public String getRegDeleteAdditionalDetailsUnsignedSitePlanLayoutDetailsQuery() {
		return REG_DELETE_UNSIGNED_SITE_PLAN_LAYOUT_DETAILS_QUERY;
	}

	public static final String REG_DELETE_SITE_PLAN_LAYOUT_IS_SIGNED_QUERY = "update eg_bpa_regularization_application "
			+ " set additionaldetails = additionaldetails - 'sitePlanLayoutIsSigned' where applicationno = ?";
	
	public String getRegDeleteAdditionalDetailsSitePlanLayoutIsSignedQuery() {
		return REG_DELETE_SITE_PLAN_LAYOUT_IS_SIGNED_QUERY;
	}

	public static final String REG_DELETE_DSC_DETAILS_QUERY = "update eg_bpa_regularization_dscdetails set documenttype=null, documentid=null where applicationno = ? ";
	
	public String getRegularizationDeleteDscDetailsQuery() {
		return REG_DELETE_DSC_DETAILS_QUERY;
	}

	public String getRegularizationDscDetailsSearchQuery(RegularizationSearchCriteria searchCriteria,
			List<Object> preparedStmtList) {
		StringBuilder query = new StringBuilder("SELECT dscdetails.* FROM eg_bpa_regularization_dscdetails dscdetails ");
		query.append(" INNER JOIN eg_bpa_regularization_application application ON application.applicationno = dscdetails.applicationno  ");
		
		if (!ObjectUtils.isEmpty(searchCriteria.getTenantId())) {
			addClauseIfRequired(preparedStmtList, query);
			query.append(" dscdetails.tenantid = ? ");
			preparedStmtList.add(searchCriteria.getTenantId());
		}
		if (!ObjectUtils.isEmpty(searchCriteria.getApplicationNo())) {
			addClauseIfRequired(preparedStmtList, query);
			query.append(" dscdetails.applicationno = ? ");
			preparedStmtList.add(searchCriteria.getApplicationNo());
		}
		query.append(" AND (dscdetails.documentid IS NULL OR dscdetails.documentid = '') ");
		return query.toString();	
	}

	public String getBPAPlanDscDetailsQuery(BPASearchCriteria criteria, List<Object> preparedStmtList) {
		StringBuilder builder = new StringBuilder(GET_BPA_PLAN_DSC_DETAILS_QUERY);
		if (criteria.getTenantId() != null) {
			addClauseIfRequired(preparedStmtList, builder);
			builder.append(" ebd.tenantid = ? ");
			preparedStmtList.add(criteria.getTenantId());
		}
		if (criteria.getApplicationNo() != null) {
			addClauseIfRequired(preparedStmtList, builder);
			builder.append(" ebd.applicationno = ? ");
			preparedStmtList.add(criteria.getApplicationNo());
		}

		builder.append(" AND  ebd.documentid is null ");
		return builder.toString();
	}

	public String getDeletePlanDscQuery() {
		return PLAN_DSC_DELETE_QUERY;
	}
	
	public String getReworkHistoryUpdateQuery() {
		return REWORK_HISTORY_UPDATE_QUERY;
	}
}
