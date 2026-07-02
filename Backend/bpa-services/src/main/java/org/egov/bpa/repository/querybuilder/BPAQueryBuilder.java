package org.egov.bpa.repository.querybuilder;

import java.util.Calendar;
import java.util.List;

import javax.validation.Valid;

import org.egov.bpa.config.BPAConfiguration;
import org.egov.bpa.util.BPAConstants;
import org.egov.bpa.web.model.BPA;
import org.egov.bpa.web.model.BPADraftSearchCriteria;
import org.egov.bpa.web.model.BPASearchCriteria;
import org.egov.bpa.web.model.CompletionCertificateSearchCriteria;
import org.egov.bpa.web.model.FieldInspectionSearchCriteria;
import org.egov.bpa.web.model.PlanningAssistantSearchCriteria;
import org.egov.bpa.web.model.PlinthApprovalSearchCriteria;
import org.egov.bpa.web.model.StageWiseReportSearchCriteria;
import org.egov.bpa.web.model.VillageSearchCriteria;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

@Component
public class BPAQueryBuilder {

	@Autowired
	private BPAConfiguration config;

	private static final String LEFT_OUTER_JOIN_STRING = " LEFT OUTER JOIN ";
	
	private static final String QUERY_FOR_COUNT = "select count(*) from eg_bpa_buildingplan bpa";

	private static final String QUERY = "SELECT bpa.*,bpadoc.*,bpa.id as bpa_id,bpa.tenantid as bpa_tenantId,bpa.lastModifiedTime as "
			+ "bpa_lastModifiedTime,bpa.createdBy as bpa_createdBy,bpa.lastModifiedBy as bpa_lastModifiedBy,bpa.createdTime as "
			+ "bpa_createdTime,bpa.additionalDetails,bpa.reworkhistory as reWorkHistory,bpa.isRevisionApplication as bpa_isRevisionApplication,bpa.landId as bpa_landId,bpa.isRevalidationApplication, bpa.permitExpiryDate, bpadoc.id as bpa_doc_id, bpadoc.additionalDetails as doc_details, bpadoc.documenttype as bpa_doc_documenttype,bpadoc.filestoreid as bpa_doc_filestore"
			+ ",bpadsc.id as dsc_id,bpadsc.additionaldetails as dsc_additionaldetails,bpadsc.documenttype as dsc_doctype,bpadsc.documentid as dsc_docid,bpadsc.approvedby as dsc_approvedby,bpadsc.applicationno as dsc_applicationno,bpadsc.buildingplanid as dsc_buildingplanid,bpadsc.createdBy as dsc_createdby,bpadsc.lastmodifiedby as dsc_lastmodifiedby,bpadsc.createdtime as dsc_createdtime,bpadsc.lastmodifiedtime as dsc_lastmodifiedtime "
			+ ",planbpadsc.id as plandsc_id,planbpadsc.additionaldetails as plandsc_additionaldetails,planbpadsc.documenttype as plandsc_doctype,planbpadsc.documentid as plandsc_docid,planbpadsc.approvedby as plandsc_approvedby,planbpadsc.applicationno as plandsc_applicationno,planbpadsc.buildingplanid as plandsc_buildingplanid,planbpadsc.createdBy as plandsc_createdby,planbpadsc.lastmodifiedby as plandsc_lastmodifiedby,planbpadsc.createdtime as plandsc_createdtime,planbpadsc.lastmodifiedtime as plandsc_lastmodifiedtime,bpa.correlationid as bpa_correlationid,  notice.id as notice_id "
			+ " FROM eg_bpa_buildingplan bpa" + LEFT_OUTER_JOIN_STRING
			+ "eg_bpa_dscdetails bpadsc ON bpadsc.buildingplanid = bpa.id" + LEFT_OUTER_JOIN_STRING
			+ "eg_bpa_plan_dscdetails planbpadsc ON planbpadsc.buildingplanid = bpa.id" + LEFT_OUTER_JOIN_STRING
			+ "eg_bpa_document bpadoc ON bpadoc.buildingplanid = bpa.id " + LEFT_OUTER_JOIN_STRING
			+ " ( select businessid, min(id) as id from eg_bpa_notice group by businessid ) notice on bpa.applicationno = notice.businessid ";
			

	private final String paginationWrapper = "SELECT * FROM "
			+ "(SELECT *, DENSE_RANK() OVER (ORDER BY bpa_lastModifiedTime DESC) offset_ FROM " + "({})"
			+ " result) result_offset " + "WHERE offset_ > ? AND offset_ <= ?";
	
	private final String draftPaginationWrapper = "SELECT * FROM "
			+ "(SELECT *, DENSE_RANK() OVER (ORDER BY lastmodifiedtime DESC) offset_ FROM " + "({})"
			+ " result) result_offset " + "WHERE offset_ > ? AND offset_ <= ?";

	private static final String DSC_PENDING_QUERY = "SELECT ebb.additionaldetails as buildingadditionaldetails, ebd.* FROM eg_bpa_dscdetails ebd inner join eg_bpa_buildingplan ebb on ebb.applicationno = ebd.applicationno ";
	
	private static final String PLAN_DSC_PENDING_QUERY = "SELECT ebb.additionaldetails as buildingadditionaldetails, ebd.* FROM eg_bpa_plan_dscdetails ebd inner join eg_bpa_buildingplan ebb on ebb.applicationno = ebd.applicationno ";

	
	private final String dscPaginationWrapper = "SELECT * FROM "
			+ "(SELECT *, DENSE_RANK() OVER (ORDER BY lastModifiedTime DESC) offset_ FROM " + "({})"
			+ " result) result_offset " + "WHERE offset_ > ? AND offset_ <= ?";

	private static final String BPA_QUERY = "SELECT bpa.*,bpa.id as bpa_id,bpa.tenantid as bpa_tenantId,bpa.lastModifiedTime as "
			+ "bpa_lastModifiedTime,bpa.createdBy as bpa_createdBy,bpa.lastModifiedBy as bpa_lastModifiedBy,bpa.createdTime as "
			+ "bpa_createdTime,bpa.additionalDetails,bpa.landId as bpa_landId from eg_bpa_buildingplan bpa";

	private final String countWrapper = "SELECT COUNT(DISTINCT(bpa_id)) FROM ({INTERNAL_QUERY}) as bpa_count";

	private static final String BPA_APPROVER_QUERY = "select distinct approvedby from eg_bpa_dscdetails ";
	 

	private static final String BPA_APPLICATION_QUERY = "select distinct on(bpa.applicationno)  bpa.applicationno as applicationno, bpa.id,bpa.tenantid as tenantId,bpa.lastmodifiedtime as bpa_lastModifiedTime,bpa.businessService as businessService,st.state as workflowstate,st.applicationstatus,asg.assignee as assigneeuuid,bpa.landId as landId,pi.businessservicesla,pi.statesla as statesla from eg_bpa_buildingplan bpa\r\n"
			+ "					  join eg_wf_processinstance_v2 pi on pi.businessid = bpa.applicationno left outer join eg_wf_assignee_v2 asg ON asg.processinstanceid = pi.id left outer join eg_wf_state_v2 st ON st.uuid = pi.status ";

	private static final String BPA_APPLICATION_APPROVEDBY_QUERY = "	select distinct on(dsc.applicationno) dsc.*,st.state as workflowstate,st.applicationstatus,ebb.additionaldetails as buildingadditionaldetails,ebb.id as bpa_id, ebb.reworkhistory as reWorkHistory, ebb.businessservice as businessService"
			+ "			from eg_bpa_dscdetails dsc left outer join eg_wf_processinstance_v2 pi on pi.businessid = dsc.applicationno left outer join\r\n"
			+ "			eg_wf_state_v2 st ON st.uuid = pi.status left outer join eg_bpa_buildingplan ebb on ebb.applicationno =dsc.applicationno ";

	private static final String BPA_APPLICATION_APPROVEDBYDOCUMENTLIST_QUERY = "select additionalDetails as doc_details,documenttype as bpa_doc_documenttype,filestoreid as bpa_doc_filestore, id as bpa_doc_id,documentuid,buildingplanid \r\n"
			+ "	from eg_bpa_document";

	private static final String FIELDINSPECTOR_DETAILS_QUERY = "SELECT id, applicationno, tenantid, approachroad, sitesituation, buildingsituation, report_details, additionaldetails, createdby, lastmodifiedby, createdtime, lastmodifiedtime\r\n"
			+ "	FROM eg_bpa_fieldinspection_details";
	
	private static final String PLANNING_ASSISTANT_CHECKLIST_QUERY = "select id, applicationno, tenantid, documents_submitted, plans_submitted, nocs_submitted, builtup_area, setback_details, additionaldetails, createdby, lastmodifiedby, createdtime, lastmodifiedtime "
			+ " FROM eg_bpa_planning_assistant_checklist ";

	private static final String BPA_APPLICATION_WITHIN_WORKFLOW = "SELECT bpa.*,bpadoc.*,bpa.id as bpa_id,bpa.tenantid as bpa_tenantId,bpa.lastModifiedTime as "
			+ "bpa_lastModifiedTime,bpa.createdBy as bpa_createdBy,bpa.lastModifiedBy as bpa_lastModifiedBy,bpa.createdTime as "
			+ "bpa_createdTime,bpa.additionalDetails,bpa.reworkhistory as reWorkHistory,bpa.isRevisionApplication as bpa_isRevisionApplication,bpa.landId as bpa_landId,bpa.isRevalidationApplication, bpa.permitExpiryDate, bpadoc.id as bpa_doc_id, bpadoc.additionalDetails as doc_details, bpadoc.documenttype as bpa_doc_documenttype,bpadoc.filestoreid as bpa_doc_filestore"
			+ ",bpadsc.id as dsc_id,bpadsc.additionaldetails as dsc_additionaldetails,bpadsc.documenttype as dsc_doctype,bpadsc.documentid as dsc_docid,bpadsc.approvedby as dsc_approvedby,bpadsc.applicationno as dsc_applicationno,bpadsc.buildingplanid as dsc_buildingplanid,bpadsc.createdBy as dsc_createdby,bpadsc.lastmodifiedby as dsc_lastmodifiedby,bpadsc.createdtime as dsc_createdtime,bpadsc.lastmodifiedtime as dsc_lastmodifiedtime "
			+ ",planbpadsc.id as plandsc_id,planbpadsc.additionaldetails as plandsc_additionaldetails,planbpadsc.documenttype as plandsc_doctype,planbpadsc.documentid as plandsc_docid,planbpadsc.approvedby as plandsc_approvedby,planbpadsc.applicationno as plandsc_applicationno,planbpadsc.buildingplanid as plandsc_buildingplanid,planbpadsc.createdBy as plandsc_createdby,planbpadsc.lastmodifiedby as plandsc_lastmodifiedby,planbpadsc.createdtime as plandsc_createdtime,planbpadsc.lastmodifiedtime as plandsc_lastmodifiedtime,bpa.correlationid as bpa_correlationid,  notice.id as notice_id "
			+ " FROM eg_bpa_buildingplan bpa" + LEFT_OUTER_JOIN_STRING
			+ "eg_bpa_dscdetails bpadsc ON bpadsc.buildingplanid = bpa.id" + LEFT_OUTER_JOIN_STRING
			+ "eg_bpa_plan_dscdetails planbpadsc ON planbpadsc.buildingplanid = bpa.id" + LEFT_OUTER_JOIN_STRING
			+ "eg_bpa_document bpadoc ON bpadoc.buildingplanid = bpa.id " + LEFT_OUTER_JOIN_STRING
			+ " ( select businessid, min(id) as id from eg_bpa_notice group by businessid ) notice on bpa.applicationno = notice.businessid "
//			+ " where bpa.status not in ('INITIATED','APPROVED','REJECTED') and approvalno is null";
			+ " where bpa.businessservice not in ('BPA5') and bpa.status not in ('SHOW_CAUSE_REPLY_VERIFICATION_PENDING','SHOW_CAUSE_ISSUED','REJECTED','PERMIT_REVOKED','PENDING_SANC_FEE_PAYMENT','PENDING_FORWARD','PENDING_APPL_FEE','INPROGRESS','INITIATED','CITIZEN_APPROVAL_INPROCESS','APPROVED','DELETED') and approvalno is null";

//	private static final String BPA_APPLICATION_WITHIN_WORKFLOW = "select distinct on(bpa.applicationno)  bpa.applicationno as applicationno, bpa.id,bpa.tenantid as tenantId,bpa.lastmodifiedtime as bpa_lastModifiedTime,bpa.businessService as businessService,st.state as workflowstate,st.applicationstatus,asg.assignee as assigneeuuid,bpa.landId as landId,pi.businessservicesla,pi.statesla as statesla from eg_bpa_buildingplan bpa\r\n"
//			+ "	join eg_wf_processinstance_v2 pi on pi.businessid = bpa.applicationno left outer join eg_wf_assignee_v2 asg ON asg.processinstanceid = pi.id left outer join eg_wf_state_v2 st ON st.uuid = pi.status "
//			+ " where bpa.status not in ('INITIATED','APPROVED','REJECTED') and approvalno is null ";

	private static final String BPA_ASSIGNEE = "select assignee  from public.eg_wf_assignee_v2  ";

	private static final String ESCALATED = " left outer join (select distinct on(businessid) * from eg_wf_processinstance_v2 order by businessid,createdtime desc) pi on pi.businessid = bpa.applicationno left outer join eg_wf_assignee_v2 asg on asg.processinstanceid = pi.id left outer join eg_wf_state_v2 st on st.uuid = pi.status where modulename = 'bpa-services' and pi.comment =? ";

	private static final String ABOUT_TO_ESCALATE = " left outer join (select distinct on(businessid) * from eg_wf_processinstance_v2 order by businessid,createdtime desc) pi on pi.businessid = bpa.applicationno left outer join eg_wf_assignee_v2 asg on asg.processinstanceid = pi.id left outer join eg_wf_state_v2 st on st.uuid = pi.status where floor(((EXTRACT(EPOCH FROM (SELECT NOW())) * 1000)-pi.lastmodifiedtime)/86400000) >= 4 and st.isterminatestate = false and modulename = 'bpa-services' AND asg.assignee =?";

	private static final String BPA_NOT_IN_STATUS = " bpa.businessservice not in ('BPA5') and bpa.status not in ('SHOW_CAUSE_REPLY_VERIFICATION_PENDING','SHOW_CAUSE_ISSUED','REJECTED','PERMIT_REVOKED','PENDING_SANC_FEE_PAYMENT','PENDING_FORWARD','PENDING_APPL_FEE','INPROGRESS','INITIATED','CITIZEN_APPROVAL_INPROCESS','APPROVED','DELETED') and approvalno is null ";
	
	private static final String OC_OUTSIDE_SCRUTINY_DETAILS = "Select eboos.id, eboos.bpaid, eboos.tenantId, eboos.infotype, eboos.plotarea, eboos.giftedlandarea, eboos.buildingblocks, eboos.basefar, eboos.maxpermissiblefar, eboos.approvedfar, eboos.providedfar, eboos.tdrfarrelaxation, eboos.totalbua, eboos.totalfloorarea," +
			" eboos.totalcarpetarea, eboos.nooftemporarystructures, eboos.projectvalueforeidp, eboos.isShelterFeeApplicable, eboos.isSecurityDepositRequired, eboos.bmvperacre, eboos.isretentionfeeapplicable, eboos.totalnoofdwellingunits, eboos.occupancyTypeHelperCode, eboos.occupancySubTypeHelperCode, eboos.permitfee, eboos.additionaldetails from eg_bpa_oc_outsidesujog_scrutiny eboos ";
	
	private static final String GET_PLINTH_LEVEL_APPROVAL = "select"
			+ "	pla.id as pla_id,"
			+ "	pla.applicationno as pla_applicationno,"
			+ "	pla.tenantid as pla_tenantid,"
			+ "	pla.bpaapplicationno as pla_bpaapplicationno,"
			+ "	pla.declaration_details as pla_declaration_details,"
			+ "	pla.accredited_person_details as pla_accredited_person_details,"
			+ "	pla.pmo_details as pla_pmo_details,"
			+ "	pla.additionaldetails as pla_additionaldetails,"
			+ "	pla.approvalno as pla_approvalno,"
			+ "	pla.status as pla_status,"
			+ "	pla.bpaapprover as pla_bpaapprover,"
			+ "	pla.createdby as pla_createdby,"
			+ "	pla.lastmodifiedby as pla_lastmodifiedby,"
			+ "	pla.createdtime as pla_createdtime,"
			+ "	pla.lastmodifiedtime as pla_lastmodifiedtime,"
			+ "	doc.id as doc_id,"
			+ "	doc.documenttype as doc_documenttype,"
			+ "	doc.filestoreid as doc_filestoreid,"
			+ "	doc.documentuid as doc_documentuid,"
			+ "	doc.plinthapprovalid as doc_documentuid,"
			+ "	doc.additionaldetails as doc_additionaldetails,"
			+ "	doc.createdby as doc_createdby,"
			+ "	doc.lastmodifiedby as doc_lastmodifiedby,"
			+ "	doc.createdtime as doc_createdtime,"
			+ "	doc.lastmodifiedtime as doc_lastmodifiedtime"
			+ " from"
			+ "	eg_bpa_plinth_level_approval pla"
			+ " inner join eg_bpa_plinth_level_approval_document doc on	pla.id = doc.plinthapprovalid ";

	private static final String QUERY_FOR_VILLAGE_DATA = "select bpa.applicationno as bpa_app, bpa.status as bpa_status, land_address.locality as village from eg_bpa_buildingplan bpa "
			+ "inner join eg_land_landinfo land_info on land_info.id = bpa.landid  "
			+ "inner join eg_land_address land_address on land_address.landinfoid = land_info.id ";
	
	private static final String QUERY_FOR_BPA_AT_PENDING_SANC_FEE = "select distinct on(wf.createdtime) *, bpa.applicationno as bpa_appno, bpa.tenantid as bpa_tenantid, bpa.businessservice as bpa_businessservice, wf.createdtime as wf_createdtime, bpa.status as bpa_status, wf.action as wf_action from eg_bpa_buildingplan bpa "
			+ " inner join eg_wf_processinstance_v2 wf on wf.businessid = bpa.applicationno "
			+ " where bpa.status ='PENDING_SANC_FEE_PAYMENT' and action in ('APPROVE','APPROVE_AND_SEND_FOR_PAYMENT') ";
	
	private static final String BPA_DRAFT_SEARCH_QUERY =  "SELECT id, edcrno, tenantid, additionaldetails, status, createdby, lastmodifiedby, createdtime, lastmodifiedtime "
	          + " FROM public.eg_bpa_draft ebd " ;
	
	private static final String BPA_DRAFT_COUNT_QUERY = "select count(*) from eg_bpa_draft ebd";
	
	private static final String BPA_COMPLETION_CERTIFICATE_SEARCH_QUERY = "SELECT ebcc.id, ebcc.certificateno, ebcc.tenantid, ebcc.applicantname, ebcc.applicantaddress, ebcc.bpapermitnumber, ebcc.bpapermitdate, ebcc.plotno, ebcc.khatano, ebcc.mouza, ebcc.architectname, ebcc.pmoname, ebcc.architectaddress, ebcc.phasewisecompletion, ebcc.completiondate, ebcc.status, ebcc.completionfilestoreid, ebcc.additionaldetails, ebcc.createdby, ebcc.createdtime, ebcc.lastmodifiedby, ebcc.lastmodifiedtime FROM public.eg_bpa_completion_certificate ebcc ";
	
	
	private final String completionPaginationWrapper = "SELECT * FROM "
			+ "(SELECT *, DENSE_RANK() OVER (ORDER BY lastmodifiedtime DESC) offset_ FROM " + "({})"
			+ " result) result_offset " + "WHERE offset_ > ? AND offset_ <= ?";
	
	private static final String BPA_STAGE_WISE_REPORT_SEARCH_QUERY = "SELECT ebs.id AS id, ebs.applicationno AS application_no, ebs.leveltype AS level_type, ebs.blockno AS block_no, ebs.floorno AS floor_no, ebs.documentdetails AS document_details, ebs.additionaldetails AS additional_details, ebs.status AS status, ebs.approvalno AS approval_no, ebs.createdby AS created_by, ebs.lastmodifiedby AS last_modified_by, ebs.createdtime AS created_time, ebs.lastmodifiedtime AS last_modified_time FROM public.eg_bpa_stagewisereport AS ebs ";

	
	/**
	 * To give the Search query based on the requirements.
	 * 
	 * @param criteria         BPA search criteria
	 * @param preparedStmtList values to be replased on the query
	 * @return Final Search Query
	 */
	public String getBPASearchQuery(BPASearchCriteria criteria, List<Object> preparedStmtList, List<String> edcrNos) {

		StringBuilder builder = new StringBuilder(QUERY);

		if (criteria.getTenantId() != null) {
			if (criteria.getTenantId().split("\\.").length == 1) {

				addClauseIfRequired(preparedStmtList, builder);
				builder.append(" bpa.tenantid like ?");
				preparedStmtList.add('%' + criteria.getTenantId() + '%');
			} else {
				addClauseIfRequired(preparedStmtList, builder);
				builder.append(" bpa.tenantid=? ");
				preparedStmtList.add(criteria.getTenantId());
			}
		}

		List<String> ids = criteria.getIds();
		if (!CollectionUtils.isEmpty(ids)) {
			addClauseIfRequired(preparedStmtList, builder);
			builder.append(" bpa.id IN (").append(createQuery(ids)).append(")");
			addToPreparedStatement(preparedStmtList, ids);
		}

		String edcrNumbers = criteria.getEdcrNumber();
		if (edcrNumbers != null) {
			addClauseIfRequired(preparedStmtList, builder);
			builder.append(" bpa.edcrNumber = ?");
			preparedStmtList.add(criteria.getEdcrNumber());
		}

		String applicationNo = criteria.getApplicationNo();
		if (applicationNo != null) {
			if (applicationNo.length() == 6) {
				addClauseIfRequired(preparedStmtList, builder);
				builder.append(" bpa.applicationNo like ?");
				preparedStmtList.add("%" + criteria.getApplicationNo());
			} else {
				addClauseIfRequired(preparedStmtList, builder);
				builder.append(" bpa.applicationNo =?");
				preparedStmtList.add(criteria.getApplicationNo());
			}
		}

		String approvalNo = criteria.getApprovalNo();
		if (approvalNo != null) {
			addClauseIfRequired(preparedStmtList, builder);
			builder.append(" bpa.approvalNo = ?");
			preparedStmtList.add(criteria.getApprovalNo());
		}

		String status = criteria.getStatus();
		if (status != null) {
			addClauseIfRequired(preparedStmtList, builder);
			builder.append(" bpa.status = ?");
			preparedStmtList.add(criteria.getStatus());

		}
		addClauseIfRequired(preparedStmtList, builder);
		builder.append(" bpa.status != ?");
		preparedStmtList.add(String.valueOf(BPAConstants.BPA_DELETED));

		if (Boolean.TRUE.equals(criteria.isRevisionApplication())) {
			addClauseIfRequired(preparedStmtList, builder);
			builder.append(" bpa.isRevisionApplication = ?");
			preparedStmtList.add(criteria.isRevisionApplication());
		}
		
		if (criteria.getIsRevalidationApplication() != null) {
			addClauseIfRequired(preparedStmtList, builder);
			builder.append(" bpa.isRevalidationApplication = ?");
			preparedStmtList.add(criteria.getIsRevalidationApplication());
		}

		Long permitDt = criteria.getApprovalDate();
		if (permitDt != null) {

			Calendar permitDate = Calendar.getInstance();
			permitDate.setTimeInMillis(permitDt);

			int year = permitDate.get(Calendar.YEAR);
			int month = permitDate.get(Calendar.MONTH);
			int day = permitDate.get(Calendar.DATE);

			Calendar permitStrDate = Calendar.getInstance();
			permitStrDate.setTimeInMillis(0);
			permitStrDate.set(year, month, day, 0, 0, 0);

			Calendar permitEndDate = Calendar.getInstance();
			permitEndDate.setTimeInMillis(0);
			permitEndDate.set(year, month, day, 23, 59, 59);
			addClauseIfRequired(preparedStmtList, builder);
			builder.append(" bpa.approvalDate BETWEEN ").append(permitStrDate.getTimeInMillis()).append(" AND ")
					.append(permitEndDate.getTimeInMillis());
		}
		if (criteria.getFromDate() != null && criteria.getToDate() != null) {
			addClauseIfRequired(preparedStmtList, builder);
			builder.append(" bpa.createdtime BETWEEN ").append(criteria.getFromDate()).append(" AND ")
					.append(criteria.getToDate());
		} else if (criteria.getFromDate() != null && criteria.getToDate() == null) {
			addClauseIfRequired(preparedStmtList, builder);
			builder.append(" bpa.createdtime >= ").append(criteria.getFromDate());
		}

		List<String> businessService = criteria.getBusinessService();
		if (!CollectionUtils.isEmpty(businessService)) {
			addClauseIfRequired(preparedStmtList, builder);
			builder.append(" bpa.businessService IN (").append(createQuery(businessService)).append(")");
			addToPreparedStatement(preparedStmtList, businessService);
		}
		List<String> landId = criteria.getLandId();
		List<String> createdBy = criteria.getCreatedBy();
		if (!CollectionUtils.isEmpty(landId)) {
			addClauseIfRequired(preparedStmtList, builder);
			if (!CollectionUtils.isEmpty(createdBy)) {
				builder.append("(");
			}
			builder.append(" bpa.landId IN (").append(createQuery(landId)).append(")");
			addToPreparedStatement(preparedStmtList, landId);
		}

		if (!CollectionUtils.isEmpty(createdBy)) {
			if (!CollectionUtils.isEmpty(landId)) {
				builder.append(" OR ");
			} else {
				addClauseIfRequired(preparedStmtList, builder);
			}
			builder.append(" bpa.createdby IN (").append(createQuery(createdBy)).append(")");
			if (!CollectionUtils.isEmpty(landId)) {
				builder.append(")");
			}
			addToPreparedStatement(preparedStmtList, createdBy);
		}
		String oldPermitNumber = criteria.getOldPermitNumber();
		if (oldPermitNumber != null) {
			addClauseIfRequired(preparedStmtList, builder);
			builder.append(" bpa.additionaldetails ->'oldApplicationDetail'->>'permitNumber' = ?");
			preparedStmtList.add(criteria.getOldPermitNumber());
		}
		
		return addPaginationWrapper(builder.toString(), preparedStmtList, criteria);

	}

	/**
	 * 
	 * @param query            prepared Query
	 * @param preparedStmtList values to be replased on the query
	 * @param criteria         bpa search criteria
	 * @return the query by replacing the placeholders with preparedStmtList
	 */
	private String addPaginationWrapper(String query, List<Object> preparedStmtList, BPASearchCriteria criteria) {

		int limit = config.getDefaultLimit();
		int offset = config.getDefaultOffset();
		String finalQuery = paginationWrapper.replace("{}", query);

		if (criteria.getLimit() == null && criteria.getOffset() == null) {
			limit = config.getMaxSearchLimit();
		}

		if (criteria.getLimit() != null && criteria.getLimit() <= config.getMaxSearchLimit())
			limit = criteria.getLimit();

		if (criteria.getLimit() != null && criteria.getLimit() > config.getMaxSearchLimit()) {
			limit = config.getMaxSearchLimit();
		}

		if (criteria.getOffset() != null)
			offset = criteria.getOffset();

		if (limit == -1) {
			finalQuery = finalQuery.replace("WHERE offset_ > ? AND offset_ <= ?", "");
		} else {
			preparedStmtList.add(offset);
			preparedStmtList.add(limit + offset);
		}

		return finalQuery;

	}
	
	private String addPaginationWrapper(String query, List<Object> preparedStmtList, BPADraftSearchCriteria criteria) {

		int limit = config.getDefaultLimit();
		int offset = config.getDefaultOffset();
		String finalQuery = draftPaginationWrapper.replace("{}", query);

		if (criteria.getLimit() == null && criteria.getOffset() == null) {
			limit = config.getMaxSearchLimit();
		}

		if (criteria.getLimit() != null && criteria.getLimit() <= config.getMaxSearchLimit())
			limit = criteria.getLimit();

		if (criteria.getLimit() != null && criteria.getLimit() > config.getMaxSearchLimit()) {
			limit = config.getMaxSearchLimit();
		}

		if (criteria.getOffset() != null)
			offset = criteria.getOffset();

		if (limit == -1) {
			finalQuery = finalQuery.replace("WHERE offset_ > ? AND offset_ <= ?", "");
		} else {
			preparedStmtList.add(offset);
			preparedStmtList.add(limit + offset);
		}

		return finalQuery;

	}


	/**
	 * add if clause to the Statement if required or elese AND
	 * 
	 * @param values
	 * @param queryString
	 */
	private void addClauseIfRequired(List<Object> values, StringBuilder queryString) {
		if (values.isEmpty())
			queryString.append(" WHERE ");

		else {
			queryString.append(" AND");
		}
	}

	/**
	 * add values to the preparedStatment List
	 * 
	 * @param preparedStmtList
	 * @param ids
	 */
	private void addToPreparedStatement(List<Object> preparedStmtList, List<String> ids) {
		ids.forEach(id -> {
			preparedStmtList.add(id);
		});

	}

	/**
	 * produce a query input for the multiple values
	 * 
	 * @param ids
	 * @return
	 */
	private Object createQuery(List<String> ids) {
		StringBuilder builder = new StringBuilder();
		int length = ids.size();
		for (int i = 0; i < length; i++) {
			builder.append(" ?");
			if (i != length - 1)
				builder.append(",");
		}
		return builder.toString();
	}

	public String getBPADscDetailsQuery(BPASearchCriteria criteria, List<Object> preparedStmtList) {
		StringBuilder builder = new StringBuilder(DSC_PENDING_QUERY);
		if (criteria.getTenantId() != null) {
			addClauseIfRequired(preparedStmtList, builder);
			builder.append(" ebd.tenantid = ? ");
			preparedStmtList.add(criteria.getTenantId());
		}
		if (criteria.getApprovedBy() != null) {
			addClauseIfRequired(preparedStmtList, builder);
			builder.append(" ebd.approvedby = ? ");
			preparedStmtList.add(criteria.getApprovedBy());
		}

		addClauseIfRequired(preparedStmtList, builder);
		builder.append(" ebb.status = ? ");
		preparedStmtList.add("APPROVED");

		builder.append(" AND  ebd.documentid is null ");
		return addDscPaginationWrapper(builder.toString(), preparedStmtList, criteria);
	}
	
	public String getBPAPlanDscDetailsQuery(BPASearchCriteria criteria, List<Object> preparedStmtList) {
		StringBuilder builder = new StringBuilder(PLAN_DSC_PENDING_QUERY);
		if (criteria.getTenantId() != null) {
			addClauseIfRequired(preparedStmtList, builder);
			builder.append(" ebd.tenantid = ? ");
			preparedStmtList.add(criteria.getTenantId());
		}
		if (criteria.getApprovedBy() != null) {
			addClauseIfRequired(preparedStmtList, builder);
			builder.append(" ebd.approvedby = ? ");
			preparedStmtList.add(criteria.getApprovedBy());
		}

		addClauseIfRequired(preparedStmtList, builder);
		builder.append(" ebb.status = ? ");
		preparedStmtList.add("APPROVED");

		builder.append(" AND  ebd.documentid is null ");
		return addDscPaginationWrapper(builder.toString(), preparedStmtList, criteria);
	}

	private String addDscPaginationWrapper(String query, List<Object> preparedStmtList, BPASearchCriteria criteria) {
		int limit = config.getDefaultLimit();
		int offset = config.getDefaultOffset();
		String finalQuery = dscPaginationWrapper.replace("{}", query);

		if (criteria.getLimit() != null && criteria.getLimit() <= config.getMaxSearchLimit())
			limit = criteria.getLimit();

		if (criteria.getLimit() != null && criteria.getLimit() > config.getMaxSearchLimit())
			limit = config.getMaxSearchLimit();

		if (criteria.getOffset() != null)
			offset = criteria.getOffset();

		if (limit == -1) {
			finalQuery = finalQuery.replace("WHERE offset_ > ? AND offset_ <= ?", "");
		} else {
			preparedStmtList.add(offset);
			preparedStmtList.add(limit + offset);
		}

		return finalQuery;
	}

	public String getBPAsSearchQuery(List<Object> preparedStmtList) {
		StringBuilder builder = new StringBuilder(BPA_QUERY);
		return builder.toString();
	}

	private String addCountWrapper(String query) {
		return countWrapper.replace("{INTERNAL_QUERY}", query);
	}

	public String getBPASearchQueryForPlainSearch(BPASearchCriteria criteria, List<Object> preparedStmtList,
			List<String> edcrNos, boolean isCount) {

		StringBuilder builder = new StringBuilder(QUERY);

		if (criteria.getTenantId() != null) {
			if (criteria.getTenantId().split("\\.").length == 1) {

				addClauseIfRequired(preparedStmtList, builder);
				builder.append(" bpa.tenantid like ?");
				preparedStmtList.add('%' + criteria.getTenantId() + '%');
			} else {
				addClauseIfRequired(preparedStmtList, builder);
				builder.append(" bpa.tenantid=? ");
				preparedStmtList.add(criteria.getTenantId());
			}
		}

		if (isCount)
			return addCountWrapper(builder.toString());

		return addPaginationWrapper(builder.toString(), preparedStmtList, criteria);

	}

	public String getApplicationApprover(String tenantId, String applicationNo, List<Object> preparedStmtList) {
		StringBuilder queryBuilder = new StringBuilder(BPA_APPROVER_QUERY);

		addClauseIfRequired(preparedStmtList, queryBuilder);
		queryBuilder.append(" tenantid = ? ");
		preparedStmtList.add(tenantId);

		addClauseIfRequired(preparedStmtList, queryBuilder);
		queryBuilder.append(" applicationno = ? ");
		preparedStmtList.add(applicationNo);

		return queryBuilder.toString();
	}

	public String getBPAApplicationSearchQuery(@Valid BPASearchCriteria criteria, List<Object> preparedStmtList,
			List<String> edcrNos) {
		StringBuilder builder = new StringBuilder(BPA_APPLICATION_QUERY);

		if (criteria.getTenantId() != null) {
			if (criteria.getTenantId().split("\\.").length == 1) {

				addClauseIfRequired(preparedStmtList, builder);
				builder.append(" bpa.tenantid like ?");
				preparedStmtList.add('%' + criteria.getTenantId() + '%');
			} else {
				addClauseIfRequired(preparedStmtList, builder);
				builder.append(" bpa.tenantid=? ");
				preparedStmtList.add(criteria.getTenantId());
			}
		}

		List<String> ids = criteria.getIds();
		if (!CollectionUtils.isEmpty(ids)) {
			addClauseIfRequired(preparedStmtList, builder);
			builder.append(" bpa.id IN (").append(createQuery(ids)).append(")");
			addToPreparedStatement(preparedStmtList, ids);
		}

		String edcrNumbers = criteria.getEdcrNumber();
		if (edcrNumbers != null) {
			addClauseIfRequired(preparedStmtList, builder);
			builder.append(" bpa.edcrNumber = ?");
			preparedStmtList.add(criteria.getEdcrNumber());
		}

		String applicationNo = criteria.getApplicationNo();
		if (applicationNo != null) {
			addClauseIfRequired(preparedStmtList, builder);
			builder.append(" bpa.applicationNo =?");
			preparedStmtList.add(criteria.getApplicationNo());
		}

		String approvalNo = criteria.getApprovalNo();
		if (approvalNo != null) {
			addClauseIfRequired(preparedStmtList, builder);
			builder.append(" bpa.approvalNo = ?");
			preparedStmtList.add(criteria.getApprovalNo());
		}

		String status = criteria.getStatus();
		if (status != null) {
			addClauseIfRequired(preparedStmtList, builder);
			builder.append(" bpa.status = ?");
			preparedStmtList.add(criteria.getStatus());

		}
		Long permitDt = criteria.getApprovalDate();
		if (permitDt != null) {

			Calendar permitDate = Calendar.getInstance();
			permitDate.setTimeInMillis(permitDt);

			int year = permitDate.get(Calendar.YEAR);
			int month = permitDate.get(Calendar.MONTH);
			int day = permitDate.get(Calendar.DATE);

			Calendar permitStrDate = Calendar.getInstance();
			permitStrDate.setTimeInMillis(0);
			permitStrDate.set(year, month, day, 0, 0, 0);

			Calendar permitEndDate = Calendar.getInstance();
			permitEndDate.setTimeInMillis(0);
			permitEndDate.set(year, month, day, 23, 59, 59);
			addClauseIfRequired(preparedStmtList, builder);
			builder.append(" bpa.approvalDate BETWEEN ").append(permitStrDate.getTimeInMillis()).append(" AND ")
					.append(permitEndDate.getTimeInMillis());
		}
		if (criteria.getFromDate() != null && criteria.getToDate() != null) {
			addClauseIfRequired(preparedStmtList, builder);
			builder.append(" bpa.createdtime BETWEEN ").append(criteria.getFromDate()).append(" AND ")
					.append(criteria.getToDate());
		} else if (criteria.getFromDate() != null && criteria.getToDate() == null) {
			addClauseIfRequired(preparedStmtList, builder);
			builder.append(" bpa.createdtime >= ").append(criteria.getFromDate());
		}

		List<String> businessService = criteria.getBusinessService();
		if (!CollectionUtils.isEmpty(businessService)) {
			addClauseIfRequired(preparedStmtList, builder);
			builder.append(" bpa.businessService IN (").append(createQuery(businessService)).append(")");
			addToPreparedStatement(preparedStmtList, businessService);
		}
		List<String> landId = criteria.getLandId();
		List<String> createdBy = criteria.getCreatedBy();
		if (!CollectionUtils.isEmpty(landId)) {
			addClauseIfRequired(preparedStmtList, builder);
			if (!CollectionUtils.isEmpty(createdBy)) {
				builder.append("(");
			}
			builder.append(" bpa.landId IN (").append(createQuery(landId)).append(")");
			addToPreparedStatement(preparedStmtList, landId);
		}

		if (!CollectionUtils.isEmpty(createdBy)) {
			if (!CollectionUtils.isEmpty(landId)) {
				builder.append(" OR ");
			} else {
				addClauseIfRequired(preparedStmtList, builder);
			}
			builder.append(" bpa.createdby IN (").append(createQuery(createdBy)).append(")");
			if (!CollectionUtils.isEmpty(landId)) {
				builder.append(")");
			}
			addToPreparedStatement(preparedStmtList, createdBy);
		}
		builder.append("  order by bpa.applicationno,pi.createdtime desc");
		return addPaginationWrapper(builder.toString(), preparedStmtList, criteria);
	}

	public String getApplicationAprovedBy(String uuid, List<Object> preparedStmtList,
			@Valid BPASearchCriteria criteria) {
		StringBuilder builder = new StringBuilder(BPA_APPLICATION_APPROVEDBY_QUERY);
		addClauseIfRequired(preparedStmtList, builder);

		if (uuid != null) {

			builder.append(" dsc.approvedby = ? and ebb.status='APPROVED'");
			preparedStmtList.add(uuid);
		}
		builder.append("  order by dsc.applicationno, pi.createdtime desc");
		return addDscPaginationWrapper(builder.toString(), preparedStmtList, criteria);

	}

	public String getDocumentApprovedBy(List<String> bpids, List<Object> preparedStmtList) {

		StringBuilder builder = new StringBuilder(BPA_APPLICATION_APPROVEDBYDOCUMENTLIST_QUERY);

		if (bpids != null) {
			addClauseIfRequired(preparedStmtList, builder);
			// buildingplanid
			builder.append(" buildingplanid IN (").append(createQuery(bpids)).append(")");
			addToPreparedStatement(preparedStmtList, bpids);

		}
		return builder.toString();
	}

	public String getfieldinspectionReportDetails(@Valid FieldInspectionSearchCriteria criteria,
			List<Object> preparedStmtList) {

		StringBuilder builder = new StringBuilder(FIELDINSPECTOR_DETAILS_QUERY);

		String applicationNo = criteria.getApplicationNo();
		if (applicationNo != null) {
			addClauseIfRequired(preparedStmtList, builder);
			builder.append(" applicationno =?");
			preparedStmtList.add(criteria.getApplicationNo());
		}

		return builder.toString();
	}
	
	public String getPlanningAssistantChecklist(@Valid PlanningAssistantSearchCriteria criteria,
			List<Object> preparedStmtList) {

		StringBuilder builder = new StringBuilder(PLANNING_ASSISTANT_CHECKLIST_QUERY);

		String applicationNo = criteria.getApplicationNo();
		if (applicationNo != null) {
			addClauseIfRequired(preparedStmtList, builder);
			builder.append(" applicationno =?");
			preparedStmtList.add(criteria.getApplicationNo());
		}

		return builder.toString();
	}
	
	public String getPlinthApproval(@Valid PlinthApprovalSearchCriteria criteria,
			List<Object> preparedStmtList) {

		StringBuilder builder = new StringBuilder(GET_PLINTH_LEVEL_APPROVAL);

		if (criteria.getTenantId() != null) {
			if (criteria.getTenantId().split("\\.").length == 1) {

				addClauseIfRequired(preparedStmtList, builder);
				builder.append(" pla.tenantid like ?");
				preparedStmtList.add('%' + criteria.getTenantId() + '%');
			} else {
				addClauseIfRequired(preparedStmtList, builder);
				builder.append(" pla.tenantid=? ");
				preparedStmtList.add(criteria.getTenantId());
			}
		}

		List<String> ids = criteria.getIds();
		if (!CollectionUtils.isEmpty(ids)) {
			addClauseIfRequired(preparedStmtList, builder);
			builder.append(" pla.id IN (").append(createQuery(ids)).append(")");
			addToPreparedStatement(preparedStmtList, ids);
		}

		String applicationNo = criteria.getApplicationNo();
		if (applicationNo != null) {
			if (applicationNo.length() == 6) {
				addClauseIfRequired(preparedStmtList, builder);
				builder.append(" pla.applicationNo like ?");
				preparedStmtList.add("%" + criteria.getApplicationNo());
			} else {
				addClauseIfRequired(preparedStmtList, builder);
				builder.append(" pla.applicationNo =?");
				preparedStmtList.add(criteria.getApplicationNo());
			}
		}

		String bpaApplicationNo = criteria.getBpaApplicationNo();
		if (bpaApplicationNo != null) {
			if (bpaApplicationNo.length() == 6) {
				addClauseIfRequired(preparedStmtList, builder);
				builder.append(" pla.bpaapplicationNo like ?");
				preparedStmtList.add("%" + criteria.getBpaApplicationNo());
			} else {
				addClauseIfRequired(preparedStmtList, builder);
				builder.append(" pla.bpaapplicationNo =?");
				preparedStmtList.add(criteria.getBpaApplicationNo());
			}
		}

		if (criteria.getStatus() != null) {
			addClauseIfRequired(preparedStmtList, builder);
			builder.append(" pla.status like ?");
			preparedStmtList.add("%" + criteria.getStatus());
		}
		
		if (criteria.getBpaApprover() != null) {
			addClauseIfRequired(preparedStmtList, builder);
			builder.append(" pla.bpaapprover = ?");
			preparedStmtList.add(criteria.getBpaApprover());
		}

		return builder.toString();
	}

	public String getBpaApplicationWithInWorkflow(List<Object> preparedStmtList) {
		StringBuilder builder = new StringBuilder(BPA_APPLICATION_WITHIN_WORKFLOW);
		return builder.toString();
	}

	public String getAssigneeByprocessInstanceId(String processInstanceId, List<Object> preparedStmtList) {
		StringBuilder builder = new StringBuilder(BPA_ASSIGNEE);

		if (processInstanceId != null) {
			addClauseIfRequired(preparedStmtList, builder);
			builder.append(" processinstanceid =?");
			preparedStmtList.add(processInstanceId);
		}

		builder.append(" order by createdtime desc  limit 1 ");

		return builder.toString();
	}

	public String getAutoEscalatedToMe(BPASearchCriteria criteria, String uuid, List<Object> preparedStmtList) {
		StringBuilder queryBuilder = new StringBuilder(QUERY);

		queryBuilder.append(ESCALATED);
		preparedStmtList.add(BPAConstants.AUTO_ESCALATED_COMMENT);
		queryBuilder.append(" and asg.assignee =?");
		preparedStmtList.add(uuid);
		
		addClauseIfRequired(preparedStmtList, queryBuilder);
		queryBuilder.append(BPA_NOT_IN_STATUS);
		return queryBuilder.toString();
	}

	public String getAboutToEscalate(BPASearchCriteria criteria, String uuid, List<Object> preparedStmtList) {
		StringBuilder queryBuilder = new StringBuilder(QUERY);
		queryBuilder.append(ABOUT_TO_ESCALATE);
		preparedStmtList.add(uuid);
		
		addClauseIfRequired(preparedStmtList, queryBuilder);
		queryBuilder.append(BPA_NOT_IN_STATUS);
		return queryBuilder.toString();
	}

	public String getAutoEscalated(BPASearchCriteria criteria, String uuid, List<Object> preparedStmtList) {
		StringBuilder queryBuilder = new StringBuilder(QUERY);
		queryBuilder.append(ESCALATED);
		preparedStmtList.add(BPAConstants.AUTO_ESCALATED_COMMENT);

		if (criteria.getTenantId() != null) {
			addClauseIfRequired(preparedStmtList, queryBuilder);
			queryBuilder.append("  bpa.tenantid=?");
			preparedStmtList.add(criteria.getTenantId());
		}
		addClauseIfRequired(preparedStmtList, queryBuilder);
		queryBuilder.append(BPA_NOT_IN_STATUS);
		return queryBuilder.toString();
	}

	public String getOCOutsideScrutinyDetails(BPA bpa, List<Object> preparedStmtList){
		StringBuilder queryBuilder = new StringBuilder(OC_OUTSIDE_SCRUTINY_DETAILS);

		if(bpa.getId()!=null){
			addClauseIfRequired(preparedStmtList, queryBuilder);
			queryBuilder.append(" eboos.bpaid=? ");
			preparedStmtList.add(bpa.getId());
		}
		if(bpa.getTenantId()!=null){
			addClauseIfRequired(preparedStmtList, queryBuilder);
			queryBuilder.append(" eboos.tenantid=? ");
			preparedStmtList.add(bpa.getTenantId());
		}
		return queryBuilder.toString();
	}

	public String getVillageDataQuery(@Valid VillageSearchCriteria criteria, List<Object> preparedStmtList) {
		
		StringBuilder query = new StringBuilder(QUERY_FOR_VILLAGE_DATA); 
		
		// enrich the variables in query i.e. application numbers
		List<String> applicationNos = criteria.getApplicationNos();
		if (!CollectionUtils.isEmpty(applicationNos)) {
			addClauseIfRequired(preparedStmtList, query);
			query.append(" applicationno IN (").append(createQuery(applicationNos)).append(")");
			addToPreparedStatement(preparedStmtList, applicationNos);
		}
		
		return query.toString();
	}

	/**
	 * Get Query to get applications pending at sanc fee here
	 * 
	 * @param criteria
	 * @param preparedStmtList
	 * @return
	 */
	public String getBPAFeePendingSearchQuery(@Valid BPASearchCriteria criteria, List<Object> preparedStmtList) {

		StringBuilder query = new StringBuilder(QUERY_FOR_BPA_AT_PENDING_SANC_FEE);

		if (!ObjectUtils.isEmpty(criteria.getTenantId())) {
			query.append(" and bpa.tenantid = ?");
			preparedStmtList.add(criteria.getTenantId());
		}
		
		if (!ObjectUtils.isEmpty(criteria.getApprovedBy())) {
			query.append(" and wf.createdby = ?");
			preparedStmtList.add(criteria.getApprovedBy());
		}
		
		query.append(" order by wf.createdtime desc ");
		
		return query.toString();
	}
	
	
	public String getBPADraftSearchQuery(BPADraftSearchCriteria criteria, List<Object> preparedStmtList) {

		StringBuilder builder = new StringBuilder(BPA_DRAFT_SEARCH_QUERY);

		if (criteria.getTenantId() != null) {
			if (criteria.getTenantId().split("\\.").length == 1) {

				addClauseIfRequired(preparedStmtList, builder);
				builder.append(" ebd.tenantid like ?");
				preparedStmtList.add('%' + criteria.getTenantId() + '%');
			} else {
				addClauseIfRequired(preparedStmtList, builder);
				builder.append(" ebd.tenantid=? ");
				preparedStmtList.add(criteria.getTenantId());
			}
		}

		List<String> ids = criteria.getIds();
		if (!CollectionUtils.isEmpty(ids)) {
			addClauseIfRequired(preparedStmtList, builder);
			builder.append(" ebd.id IN (").append(createQuery(ids)).append(")");
			addToPreparedStatement(preparedStmtList, ids);
		}

		String edcrNumber = criteria.getEdcrNo();
		if (edcrNumber != null) {
			addClauseIfRequired(preparedStmtList, builder);
			builder.append(" ebd.edcrno = ?");
			preparedStmtList.add(criteria.getEdcrNo());
		}

		String status = criteria.getStatus();
		if (status != null) {
			addClauseIfRequired(preparedStmtList, builder);
			builder.append(" ebd.status = ?");
			preparedStmtList.add(criteria.getStatus());
		}
		
		
		if (criteria.getCreatedBy() != null) {
			addClauseIfRequired(preparedStmtList, builder);
			builder.append(" ebd.createdby = ?");
			preparedStmtList.add(criteria.getCreatedBy());
		}

		return addPaginationWrapper(builder.toString(), preparedStmtList, criteria);

	}
	
	public String getBPACountQuery(BPASearchCriteria criteria, List<Object> preparedStmtList, List<String> edcrNos) {

		StringBuilder builder = new StringBuilder(QUERY_FOR_COUNT);

		if (criteria.getTenantId() != null) {
			if (criteria.getTenantId().split("\\.").length == 1) {

				addClauseIfRequired(preparedStmtList, builder);
				builder.append(" bpa.tenantid like ?");
				preparedStmtList.add('%' + criteria.getTenantId() + '%');
			} else {
				addClauseIfRequired(preparedStmtList, builder);
				builder.append(" bpa.tenantid=? ");
				preparedStmtList.add(criteria.getTenantId());
			}
		}

		List<String> ids = criteria.getIds();
		if (!CollectionUtils.isEmpty(ids)) {
			addClauseIfRequired(preparedStmtList, builder);
			builder.append(" bpa.id IN (").append(createQuery(ids)).append(")");
			addToPreparedStatement(preparedStmtList, ids);
		}

		String edcrNumbers = criteria.getEdcrNumber();
		if (edcrNumbers != null) {
			addClauseIfRequired(preparedStmtList, builder);
			builder.append(" bpa.edcrNumber = ?");
			preparedStmtList.add(criteria.getEdcrNumber());
		}

		String applicationNo = criteria.getApplicationNo();
		if (applicationNo != null) {
			if (applicationNo.length() == 6) {
				addClauseIfRequired(preparedStmtList, builder);
				builder.append(" bpa.applicationNo like ?");
				preparedStmtList.add("%" + criteria.getApplicationNo());
			} else {
				addClauseIfRequired(preparedStmtList, builder);
				builder.append(" bpa.applicationNo =?");
				preparedStmtList.add(criteria.getApplicationNo());
			}
		}

		String approvalNo = criteria.getApprovalNo();
		if (approvalNo != null) {
			addClauseIfRequired(preparedStmtList, builder);
			builder.append(" bpa.approvalNo = ?");
			preparedStmtList.add(criteria.getApprovalNo());
		}

		String status = criteria.getStatus();
		if (status != null) {
			addClauseIfRequired(preparedStmtList, builder);
			builder.append(" bpa.status = ?");
			preparedStmtList.add(criteria.getStatus());

		}
		addClauseIfRequired(preparedStmtList, builder);
		builder.append(" bpa.status != ?");
		preparedStmtList.add(String.valueOf(BPAConstants.BPA_DELETED));

		if (Boolean.TRUE.equals(criteria.isRevisionApplication())) {
			addClauseIfRequired(preparedStmtList, builder);
			builder.append(" bpa.isRevisionApplication = ?");
			preparedStmtList.add(criteria.isRevisionApplication());
		}
		if (criteria.getIsRevalidationApplication() != null) {
			addClauseIfRequired(preparedStmtList, builder);
			builder.append(" bpa.isRevalidationApplication = ?");
			preparedStmtList.add(criteria.getIsRevalidationApplication());
		}

		Long permitDt = criteria.getApprovalDate();
		if (permitDt != null) {

			Calendar permitDate = Calendar.getInstance();
			permitDate.setTimeInMillis(permitDt);

			int year = permitDate.get(Calendar.YEAR);
			int month = permitDate.get(Calendar.MONTH);
			int day = permitDate.get(Calendar.DATE);

			Calendar permitStrDate = Calendar.getInstance();
			permitStrDate.setTimeInMillis(0);
			permitStrDate.set(year, month, day, 0, 0, 0);

			Calendar permitEndDate = Calendar.getInstance();
			permitEndDate.setTimeInMillis(0);
			permitEndDate.set(year, month, day, 23, 59, 59);
			addClauseIfRequired(preparedStmtList, builder);
			builder.append(" bpa.approvalDate BETWEEN ").append(permitStrDate.getTimeInMillis()).append(" AND ")
					.append(permitEndDate.getTimeInMillis());
		}
		if (criteria.getFromDate() != null && criteria.getToDate() != null) {
			addClauseIfRequired(preparedStmtList, builder);
			builder.append(" bpa.createdtime BETWEEN ").append(criteria.getFromDate()).append(" AND ")
					.append(criteria.getToDate());
		} else if (criteria.getFromDate() != null && criteria.getToDate() == null) {
			addClauseIfRequired(preparedStmtList, builder);
			builder.append(" bpa.createdtime >= ").append(criteria.getFromDate());
		}

		List<String> businessService = criteria.getBusinessService();
		if (!CollectionUtils.isEmpty(businessService)) {
			addClauseIfRequired(preparedStmtList, builder);
			builder.append(" bpa.businessService IN (").append(createQuery(businessService)).append(")");
			addToPreparedStatement(preparedStmtList, businessService);
		}
		List<String> landId = criteria.getLandId();
		List<String> createdBy = criteria.getCreatedBy();
		if (!CollectionUtils.isEmpty(landId)) {
			addClauseIfRequired(preparedStmtList, builder);
			if (!CollectionUtils.isEmpty(createdBy)) {
				builder.append("(");
			}
			builder.append(" bpa.landId IN (").append(createQuery(landId)).append(")");
			addToPreparedStatement(preparedStmtList, landId);
		}

		if (!CollectionUtils.isEmpty(createdBy)) {
			if (!CollectionUtils.isEmpty(landId)) {
				builder.append(" OR ");
			} else {
				addClauseIfRequired(preparedStmtList, builder);
			}
			builder.append(" bpa.createdby IN (").append(createQuery(createdBy)).append(")");
			if (!CollectionUtils.isEmpty(landId)) {
				builder.append(")");
			}
			addToPreparedStatement(preparedStmtList, createdBy);
		}
		String oldPermitNumber = criteria.getOldPermitNumber();
		if (oldPermitNumber != null) {
			addClauseIfRequired(preparedStmtList, builder);
			builder.append(" bpa.additionaldetails ->'oldApplicationDetail'->>'permitNumber' = ?");
			preparedStmtList.add(criteria.getOldPermitNumber());
		}
		
		return builder.toString();

	}

	public String getBPADraftCountQuery(@Valid BPADraftSearchCriteria criteria, List<Object> preparedStmtList) {
		StringBuilder builder = new StringBuilder(BPA_DRAFT_COUNT_QUERY);

		if (criteria.getTenantId() != null) {
			if (criteria.getTenantId().split("\\.").length == 1) {

				addClauseIfRequired(preparedStmtList, builder);
				builder.append(" ebd.tenantid like ?");
				preparedStmtList.add('%' + criteria.getTenantId() + '%');
			} else {
				addClauseIfRequired(preparedStmtList, builder);
				builder.append(" ebd.tenantid=? ");
				preparedStmtList.add(criteria.getTenantId());
			}
		}

		List<String> ids = criteria.getIds();
		if (!CollectionUtils.isEmpty(ids)) {
			addClauseIfRequired(preparedStmtList, builder);
			builder.append(" ebd.id IN (").append(createQuery(ids)).append(")");
			addToPreparedStatement(preparedStmtList, ids);
		}

		String edcrNumber = criteria.getEdcrNo();
		if (edcrNumber != null) {
			addClauseIfRequired(preparedStmtList, builder);
			builder.append(" ebd.edcrno = ?");
			preparedStmtList.add(criteria.getEdcrNo());
		}

		String status = criteria.getStatus();
		if (status != null) {
			addClauseIfRequired(preparedStmtList, builder);
			builder.append(" ebd.status = ?");
			preparedStmtList.add(criteria.getStatus());
		}
		
		if (criteria.getCreatedBy() != null) {
			addClauseIfRequired(preparedStmtList, builder);
			builder.append(" ebd.createdby = ?");
			preparedStmtList.add(criteria.getCreatedBy());
		}

		return builder.toString();
	}

	public String getCompletionCertificateSearchQuery(@Valid CompletionCertificateSearchCriteria criteria,
			List<Object> preparedStmtList) {
		StringBuilder builder = new StringBuilder(BPA_COMPLETION_CERTIFICATE_SEARCH_QUERY);

		if (criteria.getTenantId() != null) {
			if (criteria.getTenantId().split("\\.").length == 1) {

				addClauseIfRequired(preparedStmtList, builder);
				builder.append(" ebcc.tenantid like ?");
				preparedStmtList.add('%' + criteria.getTenantId() + '%');
			} else {
				addClauseIfRequired(preparedStmtList, builder);
				builder.append(" ebcc.tenantid=? ");
				preparedStmtList.add(criteria.getTenantId());
			}
		}

		List<String> ids = criteria.getIds();
		if (!CollectionUtils.isEmpty(ids)) {
			addClauseIfRequired(preparedStmtList, builder);
			builder.append(" ebcc.id IN (").append(createQuery(ids)).append(")");
			addToPreparedStatement(preparedStmtList, ids);
		}

        if (criteria.getBpaPermitNumber() != null) {
			addClauseIfRequired(preparedStmtList, builder);
			builder.append(" ebcc.bpapermitnumber = ?");
			preparedStmtList.add(criteria.getBpaPermitNumber());
		}		
		
		if (criteria.getCertificateNo() != null) {
			addClauseIfRequired(preparedStmtList, builder);
			builder.append(" ebcc.certificateno = ?");
			preparedStmtList.add(criteria.getCertificateNo());
		}
		
		if (criteria.getCreatedBy() != null) {
			addClauseIfRequired(preparedStmtList, builder);
			builder.append(" ebcc.createdby = ?");
			preparedStmtList.add(criteria.getCreatedBy());
		}	
		
		return addPaginationWrapper(builder.toString(), preparedStmtList, criteria);
	}
	
	private String addPaginationWrapper(String query, List<Object> preparedStmtList, CompletionCertificateSearchCriteria criteria) {

		int limit = config.getDefaultLimit();
		int offset = config.getDefaultOffset();
		String finalQuery = completionPaginationWrapper.replace("{}", query);

		if (criteria.getLimit() == null && criteria.getOffset() == null) {
			limit = config.getMaxSearchLimit();
		}

		if (criteria.getLimit() != null && criteria.getLimit() <= config.getMaxSearchLimit())
			limit = criteria.getLimit();

		if (criteria.getLimit() != null && criteria.getLimit() > config.getMaxSearchLimit()) {
			limit = config.getMaxSearchLimit();
		}

		if (criteria.getOffset() != null)
			offset = criteria.getOffset();

		if (limit == -1) {
			finalQuery = finalQuery.replace("WHERE offset_ > ? AND offset_ <= ?", "");
		} else {
			preparedStmtList.add(offset);
			preparedStmtList.add(limit + offset);
		}

		return finalQuery;

	}

	public String getStageWiseReportSearchQuery(@Valid StageWiseReportSearchCriteria criteria,
			List<Object> preparedStmtList) {
		StringBuilder builder = new StringBuilder(BPA_STAGE_WISE_REPORT_SEARCH_QUERY);

		List<String> ids = criteria.getIds();
		if (!CollectionUtils.isEmpty(ids)) {
			addClauseIfRequired(preparedStmtList, builder);
			builder.append(" ebs.id IN (").append(createQuery(ids)).append(")");
			addToPreparedStatement(preparedStmtList, ids);
		}

        if (criteria.getApplicationNo() != null) {
			addClauseIfRequired(preparedStmtList, builder);
			builder.append(" ebs.applicationno = ?");
			preparedStmtList.add(criteria.getApplicationNo());
		}		
		
		if (criteria.getLevelType() != null) {
			addClauseIfRequired(preparedStmtList, builder);
			builder.append(" ebs.leveltype = ?");
			preparedStmtList.add(criteria.getLevelType());
		}
		
		if (criteria.getBlockNo() != null) {
			addClauseIfRequired(preparedStmtList, builder);
			builder.append(" ebs.blockno = ?");
			preparedStmtList.add(criteria.getBlockNo());
		}
		
		if (criteria.getFloorNo() != null) {
			addClauseIfRequired(preparedStmtList, builder);
			builder.append(" ebs.floorno = ?");
			preparedStmtList.add(criteria.getFloorNo());
		}
		
		if (criteria.getCreatedBy() != null) {
			addClauseIfRequired(preparedStmtList, builder);
			builder.append(" ebs.createdby = ?");
			preparedStmtList.add(criteria.getCreatedBy());
		}	
		
		return builder.toString();
	}

}
