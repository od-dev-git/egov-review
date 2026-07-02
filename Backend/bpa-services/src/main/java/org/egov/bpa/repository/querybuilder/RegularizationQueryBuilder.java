package org.egov.bpa.repository.querybuilder;

import java.util.List;

import javax.validation.Valid;

import org.egov.bpa.config.BPAConfiguration;
import org.egov.bpa.util.BPAConstants;
import org.egov.bpa.web.model.BPASearchCriteria;
import org.egov.bpa.web.model.RegularizationDraftSearchCriteria;
import org.egov.bpa.web.model.VillageSearchCriteria;
import org.egov.bpa.web.model.regularization.RegularizationDocRemarkSearchCriteria;
import org.egov.bpa.web.model.regularization.RegularizationFISearchCriteria;
import org.egov.bpa.web.model.regularization.RegularizationSearchCriteria;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

@Component
public class RegularizationQueryBuilder {
	
	@Autowired
	private BPAConfiguration config;
	
	public static final String PAGINATION_WRAPPER = "SELECT * FROM "
			+ "(SELECT *, DENSE_RANK() OVER (ORDER BY regularization_createdtime DESC) offset_ FROM " + "({})"
			+ " result) result_offset " + "WHERE offset_ > ? AND offset_ <= ?";
	
	public static final String REGULARIZATION_SEARCH_QUERY = "select regularization.id , regularization.tenantid , regularization.applicationno , regularization.apptype , regularization.applicationdate , regularization.approvaldate , regularization.status , regularization.applicationtype , regularization.servicetype , regularization.servicesubtype , regularization.businessservice , regularization.accountid , regularization.approvalno , regularization.permitexpirydate , regularization.additionaldetails , regularization.createdby as regularization_createdby, regularization.lastmodifiedby as regularization_lastmodifiedby, regularization.createdtime as regularization_createdtime, regularization.lastmodifiedtime as regularization_lastmodifiedtime, "
            + " landinfo.id as landid, landinfo.tenantid as landtenantid, landinfo.regularizationid , landinfo.regularizationtype , landinfo.totalplotarea , landinfo.accessroadwidth , landinfo.additionaldetails as landadditionaldetails, landinfo.createdby as landcreatedby, landinfo.lastmodifiedby as landlastmodifiedby, landinfo.createdtime as landcreatedtime, landinfo.lastmodifiedtime as landlastmodifiedtime,"
            + " buildinginfo.id as buildingid, buildinginfo.tenantid as buildingtenantid, buildinginfo.regularizationid , buildinginfo.basefar , buildinginfo.maxpermissiblefar , buildinginfo.approvedfar , buildinginfo.asbuiltfar , buildinginfo.farstatus , buildinginfo.totalprovidedbua , buildinginfo.totalapprovedbua , buildinginfo.totalunauthorizedbua , buildinginfo.totalunauthareaonsbwithinnorms , buildinginfo.totalunauthareaonsbbeyondnormsbutunder5 , buildinginfo.totalunauthareaonsbbeyondnormsbutunder10, buildinginfo.hasprojectprovidedmin10percentbuaforewswithin5kmfromprojectsite, buildinginfo.numberoftemporarystructures, buildinginfo.projectvalueifeidpfeeapplicableforproject, buildinginfo.totalnoofdwellingunits, buildinginfo.isshelterfeeapplicable, buildinginfo.iseidpfeeapplicable, buildinginfo.iscwwcfeeapplicable, buildinginfo.effectiveewsarea, buildinginfo.issecuritydepositrequired, buildinginfo.tdrfarrelaxation, buildinginfo.farfeerate, buildinginfo.unauthorizedsbwithinnormsrate, buildinginfo.unauthorizedsbbnunder5rate, buildinginfo.unauthorizedsbbnunder10rate, buildinginfo.buildingblocks , buildinginfo.additionaldetails as buildingadditionaldetails, buildinginfo.createdby as buildingcreatedby, buildinginfo.lastmodifiedby as buildinglastmodifiedby, buildinginfo.createdtime as buildingcreatedtime, buildinginfo.lastmodifiedtime as buildinglastmodifiedtime,"
            + " plotinfo.id as plotinfoid , plotinfo.tenantid as plottenantid , plotinfo.landinfoid as plotlandinfoid, plotinfo.district , plotinfo.tehsil , plotinfo.village , plotinfo.plotno , plotinfo.subplotno , plotinfo.subsubplotno , plotinfo.plotarea , plotinfo.khata , plotinfo.kisam , plotinfo.landownername , plotinfo.gpaholdername , plotinfo.saledeedno , plotinfo.saledeeddate , plotinfo.bmvvalue , plotinfo.plottobegifted , plotinfo.areatobegifted , plotinfo.reasonforgift , plotinfo.active as isactive, plotinfo.additionaldetails as plotinfoadditionaldetails , plotinfo.createdby as plotinfocreatedby , plotinfo.lastmodifiedby as plotinfolastmodifiedby , plotinfo.createdtime as plotinfocreatedtime , plotinfo.lastmodifiedtime as plotinfolastmodifiedtime , "
            + " doc.id as documentid, doc.documenttype , doc.filestoreid , doc.documentuid , doc.additionaldetails as documentadditionaldetails, doc.createdby as documentcreatedby, doc.lastmodifiedby as documentlastmodifiedby , doc.createdtime as documentcreatedtime, doc.lastmodifiedtime as documentlastmodifiedtime, "
            + " dscdetails.id as dscdetailsid, dscdetails.tenantid as dscdetailstenantid , dscdetails.documenttype as dscdetailsdocumenttype, dscdetails.documentid as dscdetailsdocumentid, dscdetails.regularizationid as dscdetailsregularizationid, dscdetails.applicationno as dscdetailsapplicationno, dscdetails.approvedby as dscdetailsapprovedby, dscdetails.additionaldetails as dscdetailsadditionaldetails, dscdetails.createdby as dscdetailscreatedby, dscdetails.lastmodifiedby as dscdetailslastmodifiedby , dscdetails.createdtime as dscdetailscreatedtime, dscdetails.lastmodifiedtime as dscdetailslastmodifiedtime, "
            + " owners.id as ownersid, owners.uuid , owners.isprimaryowner , owners.ownershippercentage , owners.institutionid as ownersinstitutionid , owners.regularizationid , owners.relationship , owners.additionaldetails as ownersadditionaldetails , owners.createdby as ownerscreatedby , owners.lastmodifiedby as ownerslastmodifiedby , owners.createdtime as ownerscreatedtime , owners.lastmodifiedtime as ownerslastmodifiedtime , " 
            + " institution.id as institutionid, institution.tenantid as institutiontenantid, institution.type , institution.designation , institution.nameofauthorizedperson , institution.additionaldetails as institutionadditionaldetails, institution.createdby as institutioncreatedby, institution.lastmodifiedby as institutionlastmodifiedby, institution.createdtime as institutioncreatedtime , institution.lastmodifiedtime as institutionlastmodifiedtime "
            + " FROM eg_bpa_regularization_application regularization "
            + " LEFT OUTER JOIN eg_bpa_regularization_landinfo landinfo ON landinfo.regularizationid = regularization.id "
            + " LEFT OUTER JOIN eg_bpa_regularization_buildinginfo buildinginfo ON buildinginfo.regularizationid = regularization.id "
            + " LEFT OUTER JOIN eg_bpa_regularization_plotinfo plotinfo ON plotinfo.landinfoid = landinfo.id "
            + " LEFT OUTER JOIN eg_bpa_regularization_document doc ON doc.regularizationid = regularization.id"
            + " LEFT OUTER JOIN eg_bpa_regularization_dscdetails dscdetails ON dscdetails.regularizationid = regularization.id"
            + " LEFT OUTER JOIN eg_bpa_regularization_owners owners ON owners.regularizationid = regularization.id "
            + " LEFT OUTER JOIN eg_bpa_regularization_institution institution ON institution.regularizationid = regularization.id AND institution.id = owners.institutionid ";


	private static final String REGULARIZATION_ASSIGNEE = "select assignee  from public.eg_wf_assignee_v2  ";
	
	private static final String DSC_PAGINATION_WRAPPER = "SELECT * FROM "
			+ "(SELECT *, DENSE_RANK() OVER (ORDER BY lastModifiedTime DESC) offset_ FROM " + "({})"
			+ " result) result_offset " + "WHERE offset_ > ? AND offset_ <= ?";

	private static final String REGULARIZATION_APPLICATION_APPROVEDBY_QUERY = " select distinct on (ebrd.applicationno) ebrd.*, st.state as workflowstate, st.applicationstatus, ebra.additionaldetails as buildingadditionaldetails, ebra.id as regularization_id, ebra.apptype"
            + "         from eg_bpa_regularization_dscdetails ebrd left outer join eg_wf_processinstance_v2 pi on pi.businessid = ebrd.applicationno left outer join\r\n"
            + "         eg_wf_state_v2 st on st.uuid = pi.status left outer join eg_bpa_regularization_application ebra on ebra.applicationno = ebrd.applicationno ";

    private static final String REGULARIZATION_APPLICATION_APPROVEDBYDOCUMENTLIST_QUERY = "select additionalDetails as doc_details,documenttype as bpa_doc_documenttype,filestoreid as bpa_doc_filestore, id as bpa_doc_id,documentuid,regularizationid \r\n"
            + " from eg_bpa_regularization_document";
	
    private static final String FIELDINSPECTOR_DETAILS_QUERY = "SELECT id, applicationno, tenantid, approachroad, sitesituation, buildingsituation, report_details, additionaldetails, setback, createdby, lastmodifiedby, createdtime, lastmodifiedtime\r\n"
			+ "	FROM eg_bpa_regularization_fieldinspection_details ";
	
    
    private static final String DOC_REMARK_SEARCH_QUERY = "select * from eg_bpa_regularization_document_remark";

	private static final String DOC_REMARK_PAGINATION_WRAPPER = "SELECT * FROM "
			+ "(SELECT *, DENSE_RANK() OVER (ORDER BY lastModifiedTime DESC) offset_ FROM " + "({})"
			+ " result) result_offset " + "WHERE offset_ > ? AND offset_ <= ?";
    
	private static final String AUTO_ESCALATED = " LEFT OUTER JOIN (select distinct on(businessid) * from eg_wf_processinstance_v2 order by businessid,createdtime desc) pi on pi.businessid = regularization.applicationno left outer join eg_wf_assignee_v2 asg on asg.processinstanceid = pi.id LEFT OUTER JOIN eg_wf_state_v2 st on st.uuid = pi.status where modulename = 'bpa-services' and pi.comment = ? ";

	private static final String ABOUT_TO_ESCALATE = " LEFT OUTER JOIN (select distinct on(businessid) * from eg_wf_processinstance_v2 order by businessid,createdtime desc) pi on pi.businessid = regularization.applicationno left outer join eg_wf_assignee_v2 asg on asg.processinstanceid = pi.id LEFT OUTER JOIN eg_wf_state_v2 st on st.uuid = pi.status where floor(((EXTRACT(EPOCH FROM (SELECT NOW())) * 1000)-pi.lastmodifiedtime)/86400000) >= 4 and st.isterminatestate = false and modulename = 'bpa-services' AND asg.assignee =?";

	private static final String REULARIZATION_NOT_IN_STATUS = "  regularization.status not in ('SHOW_CAUSE_REPLY_VERIFICATION_PENDING', 'SHOW_CAUSE_ISSUED', 'REJECTED', 'PERMIT_REVOKED', 'PENDING_SANC_FEE_PAYMENT', 'PENDING_FORWARD', 'PENDING_APPL_FEE', 'INPROGRESS', 'INITIATED', 'CITIZEN_APPROVAL_INPROCESS', 'APPROVED', 'DELETED') and regularization.approvalno is null ";

	private static final String QUERY_FOR_VILLAGE_DATA = " select distinct on (applicationno) applicationno as app_no, status, village from eg_bpa_regularization_application reg "
			+ " inner join eg_bpa_regularization_landinfo land on land.regularizationid = reg.id "
			+ " inner join eg_bpa_regularization_plotinfo plot on plot.landinfoid = land.id ";
	
	private static final String QUERY_FOR_REGULARIZATION_AT_PENDING_SANC_FEE = "select distinct on(wf.createdtime) *, bpa.applicationno as bpa_appno, bpa.tenantid as bpa_tenantid, bpa.businessservice as bpa_businessservice, wf.createdtime as wf_createdtime, bpa.status as bpa_status, wf.action as wf_action from eg_bpa_regularization_application bpa "
			+ " inner join eg_wf_processinstance_v2 wf on wf.businessid = bpa.applicationno "
			+ " where bpa.status ='PENDING_SANC_FEE_PAYMENT' and action in ('APPROVE') ";
	
	private static final String REGULARIZATION_DRAFT_SEARCH_QUERY =  "SELECT id, draftno, tenantid, additionaldetails, status, createdby, lastmodifiedby, createdtime, lastmodifiedtime "
	          + " FROM public.eg_regularization_draft erd " ;
	
	/**
	 * Get the regularization search final query from here
	 * 
	 * @param searchCriteria
	 * @param preparedStmtList
	 * @return final query
	 */
	public String getRegularizationSearchQuery(RegularizationSearchCriteria searchCriteria, List<Object> preparedStmtList) {
		
		StringBuilder query = new StringBuilder(REGULARIZATION_SEARCH_QUERY);
		
		// Add TenantId in the where clause of query here if exists in Search Criteria
		if (!ObjectUtils.isEmpty(searchCriteria.getTenantId())) {
			if (searchCriteria.getTenantId().split("\\.").length == 1) {
				addClauseIfRequired(query, preparedStmtList);
				query.append(" regularization.tenantid like ?");
				preparedStmtList.add('%' + searchCriteria.getTenantId() + '%');
			} else {
				addClauseIfRequired(query, preparedStmtList);
				query.append(" regularization.tenantid = ?");
				preparedStmtList.add(searchCriteria.getTenantId());
			}
		}
		
		// Add Regularization Ids in the where clause of query here if exists in Search Criteria
		if (!CollectionUtils.isEmpty(searchCriteria.getIds())) {
			addClauseIfRequired(query, preparedStmtList);
			query.append(" regularization.id IN (").append(createQuery(searchCriteria.getIds())).append(")");
			addToPreparedStatement(preparedStmtList, searchCriteria.getIds());
		}
		
		// Add Application Number in the where clause of query here if exists in Search Criteria
		if (!ObjectUtils.isEmpty(searchCriteria.getApplicationNo())) {
			if (searchCriteria.getApplicationNo().length() == 6) {
				addClauseIfRequired(query, preparedStmtList);
				query.append(" regularization.applicationno like ?");
				preparedStmtList.add("%" + searchCriteria.getApplicationNo());
			} else {
				addClauseIfRequired(query, preparedStmtList);
				query.append(" regularization.applicationno = ?");
				preparedStmtList.add(searchCriteria.getApplicationNo());
			}
		}
		
		// Add Approval No in the where clause of query here if exists in Search Criteria
		if (!ObjectUtils.isEmpty(searchCriteria.getApprovalNo())) {
			addClauseIfRequired(query, preparedStmtList);
			query.append(" regularization.approvalno = ?");
			preparedStmtList.add(searchCriteria.getApprovalNo());
		}
		
		// Add Status in the where clause of query here if exists in Search Criteria
		if (!ObjectUtils.isEmpty(searchCriteria.getStatus())) {
			addClauseIfRequired(query, preparedStmtList);
			query.append(" regularization.status = ?");
			preparedStmtList.add(searchCriteria.getStatus());
		} else {
			addClauseIfRequired(query, preparedStmtList);
			query.append(" regularization.status != ?");
			preparedStmtList.add(String.valueOf(BPAConstants.BPA_DELETED));
		}
		
		// Add From Date in the where clause of query here if exists in Search Criteria
		if (!ObjectUtils.isEmpty(searchCriteria.getFromDate())) {
			addClauseIfRequired(query, preparedStmtList);
			query.append(" regularization.createdtime >= ?");
			preparedStmtList.add(searchCriteria.getFromDate());
		} 
		
		// Add To Date in the where clause of query here if exists in Search Criteria
		if (!ObjectUtils.isEmpty(searchCriteria.getToDate())) {
			addClauseIfRequired(query, preparedStmtList);
			query.append(" regularization.createdtime <= ?");
			preparedStmtList.add(searchCriteria.getToDate());
		} 
		
		// Add OwnerIds in the query, if available
		if (!ObjectUtils.isEmpty(searchCriteria.getOwnerIds())) {
			addClauseIfRequired(query, preparedStmtList);
			query.append(" owners.uuid in (").append(createQuery(searchCriteria.getOwnerIds())).append(")");
			addToPreparedStatement(preparedStmtList, searchCriteria.getOwnerIds());
		}
		
		// Add createdby in the query, if available
		if (!ObjectUtils.isEmpty(searchCriteria.getCreatedBy())) {
			addClauseIfRequired(query, preparedStmtList);
			query.append(" regularization.createdby in (").append(createQuery(searchCriteria.getCreatedBy())).append(")");
			addToPreparedStatement(preparedStmtList, searchCriteria.getCreatedBy());
		}
		
		return addPaginationWrapper(query.toString(), preparedStmtList, searchCriteria, PAGINATION_WRAPPER);
	}

	
	/**
	 * 
	 * @param query
	 * @param preparedStmtList
	 */
	private void addClauseIfRequired(StringBuilder query, List<Object> preparedStmtList) {
		if (preparedStmtList.isEmpty()) {
			query.append(" WHERE ");
		} else {
			query.append(" AND ");
		}
	}
	
	
	/**
	 * produce a query input for the multiple values
	 * 
	 * @param ids
	 * @return
	 */
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
	 * 
	 * @param query            prepared Query
	 * @param preparedStmtList values to be replased on the query
	 * @param criteria         bpa search criteria
	 * @param paginationWrapper 
	 * @return the query by replacing the placeholders with preparedStmtList
	 */
	private String addPaginationWrapper(String query, List<Object> preparedStmtList, RegularizationSearchCriteria criteria, String paginationWrapper) {

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


	/**
	 * Method to get query for Regularization Applications in Workflow
	 * 
	 * @return query in String
	 */
	public String getApplicationsInWorkflowQuery() {
		
		StringBuilder query = new StringBuilder(REGULARIZATION_SEARCH_QUERY);
		query.append(" where regularization.status not in ('SHOW_CAUSE_REPLY_VERIFICATION_PENDING', 'SHOW_CAUSE_ISSUED', 'REJECTED', 'PERMIT_REVOKED', 'PENDING_SANC_FEE_PAYMENT', 'PENDING_FORWARD', 'PENDING_APPL_FEE', 'INPROGRESS', 'INITIATED', 'CITIZEN_APPROVAL_INPROCESS', 'APPROVED', 'DELETED') and regularization.approvalno is null ");
		return query.toString();
	}


	public String getAssigneeByprocessInstanceId(String processInstanceId, List<Object> preparedStmtList) {
		StringBuilder builder = new StringBuilder(REGULARIZATION_ASSIGNEE);

		if (processInstanceId != null) {
			addClauseIfRequired(preparedStmtList, builder);
			builder.append(" processinstanceid =?");
			preparedStmtList.add(processInstanceId);
		}

		builder.append(" order by createdtime desc  limit 1 ");

		return builder.toString();
	}


	private void addClauseIfRequired(List<Object> preparedStmtList, StringBuilder queryString) {
		if (preparedStmtList.isEmpty())
			queryString.append(" WHERE ");

		else {
			queryString.append(" AND");
		}
	}


	/**
	 * Get the regularization dsc details search final query from here
	 * 
	 * @param searchCriteria
	 * @param preparedStmtList
	 * @return final query
	 */
	public String getRegularizationDscDetailsSearchQuery(RegularizationSearchCriteria searchCriteria,
			List<Object> preparedStmtList) {
		
		StringBuilder query = new StringBuilder("SELECT dscdetails.* FROM eg_bpa_regularization_dscdetails dscdetails ");
		query.append(" INNER JOIN eg_bpa_regularization_application application ON application.applicationno = dscdetails.applicationno  ");
		
		if (!ObjectUtils.isEmpty(searchCriteria.getTenantId())) {
			addClauseIfRequired(query, preparedStmtList);
			query.append(" dscdetails.tenantid = ? ");
			preparedStmtList.add(searchCriteria.getTenantId());
		}
		if (!ObjectUtils.isEmpty(searchCriteria.getApprovedBy())) {
			addClauseIfRequired(query, preparedStmtList);
			query.append(" dscdetails.approvedby = ? ");
			preparedStmtList.add(searchCriteria.getApprovedBy());
		}

		addClauseIfRequired(query, preparedStmtList);
		query.append(" application.status = ? ");
		preparedStmtList.add("APPROVED");

		query.append(" AND (dscdetails.documentid IS NULL OR dscdetails.documentid = '') ");
		
		
		return addPaginationWrapper(query.toString(), preparedStmtList, searchCriteria, DSC_PAGINATION_WRAPPER);
	}
	
	/**
	 * Get Query for Approved by from here
	 * 
	 * @param uuid
	 * @param preparedStmtList
	 * @param criteria
	 * @return
	 */
	public String getApplicationAprovedBy(String uuid, List<Object> preparedStmtList,
            @Valid RegularizationSearchCriteria criteria) {
        StringBuilder builder = new StringBuilder(REGULARIZATION_APPLICATION_APPROVEDBY_QUERY);
        addClauseIfRequired(preparedStmtList, builder);

        if (uuid != null) {

            builder.append(" ebrd.approvedby = ? and ebra.status='APPROVED'");
            preparedStmtList.add(uuid);
        }
        builder.append("  order by ebrd.applicationno, pi.createdtime desc");
        return addDscPaginationWrapper(builder.toString(), preparedStmtList, criteria);

    }
    
	/**
	 * Get query for Documents for the approved applications 
	 * 
	 * @param bpids
	 * @param preparedStmtList
	 * @return
	 */
    public String getDocumentApprovedBy(List<String> bpids, List<Object> preparedStmtList) {

        StringBuilder builder = new StringBuilder(REGULARIZATION_APPLICATION_APPROVEDBYDOCUMENTLIST_QUERY);

        if (bpids != null) {
            addClauseIfRequired(preparedStmtList, builder);
            builder.append(" regularizationid IN (").append(createQuery(bpids)).append(")");
			addToPreparedStatement(preparedStmtList, bpids);

        }
        return builder.toString();
    }
    
    /**
     * Add Limits and Offset if passed
     * 
     * @param query
     * @param preparedStmtList
     * @param criteria
     * @return
     */
    private String addDscPaginationWrapper(String query, List<Object> preparedStmtList, RegularizationSearchCriteria criteria) {
        int limit = config.getDefaultLimit();
        int offset = config.getDefaultOffset();
        String finalQuery = DSC_PAGINATION_WRAPPER.replace("{}", query);

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

    /**
     * Get Search FI final query from here
     * 
     * @param criteria
     * @param preparedStmtList
     * @return
     */
	public String getfieldinspectionReportDetails(@Valid RegularizationFISearchCriteria criteria,
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

	
	

	/**
	 * Get the regularization Doc Remark search final query from here
	 * 
	 * @param searchCriteria
	 * @param preparedStmtList
	 * @return final query
	 */
	public String regularizationDocRemarkSearchQuery(RegularizationDocRemarkSearchCriteria searchCriteria,
			List<Object> preparedStmtList) {
		StringBuilder builder = new StringBuilder(DOC_REMARK_SEARCH_QUERY);
        List<String> ids = searchCriteria.getIds();
        List<String> documentCodes = searchCriteria.getDocumentCode();

        if (!CollectionUtils.isEmpty(ids)) {
            addClauseIfRequired(preparedStmtList, builder);
            builder.append(" id IN (").append(createQuery(ids)).append(") ");
            addToPreparedStatement(preparedStmtList, ids);

        }
        
        if (!CollectionUtils.isEmpty(documentCodes)) {
            addClauseIfRequired(preparedStmtList, builder);
            builder.append(" documentcode IN (").append(createQuery(documentCodes)).append(") ");
            addToPreparedStatement(preparedStmtList, documentCodes);

        }
        if (searchCriteria.getBusinessId() != null) {
            addClauseIfRequired(preparedStmtList, builder);
            builder.append(" businessid=?");
            preparedStmtList.add(searchCriteria.getBusinessId());
        }

        if (searchCriteria.getFromDate() != null && searchCriteria.getToDate() != null) {
            addClauseIfRequired(preparedStmtList, builder);
            builder.append(" createdtime BETWEEN ")
            		.append(searchCriteria.getFromDate())
            		.append(" AND ")
                    .append(searchCriteria.getToDate());
            
        } else if (searchCriteria.getFromDate() != null && searchCriteria.getToDate() == null) {
            addClauseIfRequired(preparedStmtList, builder);
            builder.append(" createdtime >= ").append(searchCriteria.getFromDate());
        }
        
        builder.append("  order by createdtime desc");
        return addDocRemarkPaginationWrapper(builder.toString(), preparedStmtList, searchCriteria);

	}

	
	
	/**
     * Add Limits and Offset if passed
     * 
     * @param query
     * @param preparedStmtList
     * @param criteria
     * @return final query with limit and offset
     */
    private String addDocRemarkPaginationWrapper(String query, List<Object> preparedStmtList, RegularizationDocRemarkSearchCriteria criteria) {
        int limit = config.getDefaultLimit();
        int offset = config.getDefaultOffset();
        String finalQuery = DOC_REMARK_PAGINATION_WRAPPER.replace("{}", query);

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


	public String getAboutToEscalate(RegularizationSearchCriteria searchCriteria, String uuid,
			List<Object> preparedStmtList) {
		StringBuilder queryBuilder = new StringBuilder(REGULARIZATION_SEARCH_QUERY);
		queryBuilder.append(ABOUT_TO_ESCALATE);
		preparedStmtList.add(uuid);
		
		addClauseIfRequired(queryBuilder, preparedStmtList);
		queryBuilder.append(REULARIZATION_NOT_IN_STATUS);
		
		return addPaginationWrapper(queryBuilder.toString(), preparedStmtList, searchCriteria, PAGINATION_WRAPPER);
	}


	public String getRegularizationAutoEscalated(RegularizationSearchCriteria searchCriteria, String uuid,
			List<Object> preparedStmtList) {
		StringBuilder queryBuilder = new StringBuilder(REGULARIZATION_SEARCH_QUERY);
		queryBuilder.append(AUTO_ESCALATED);
		preparedStmtList.add(BPAConstants.AUTO_ESCALATED_COMMENT);

		// Add TenantId in the where clause of query here if exists in Search Criteria
		if (!ObjectUtils.isEmpty(searchCriteria.getTenantId())) {
			addClauseIfRequired(queryBuilder, preparedStmtList);
			queryBuilder.append(" regularization.tenantid = ?");
			preparedStmtList.add(searchCriteria.getTenantId());
		}
		addClauseIfRequired(queryBuilder, preparedStmtList);
		queryBuilder.append(REULARIZATION_NOT_IN_STATUS);
		
		return addPaginationWrapper(queryBuilder.toString(), preparedStmtList, searchCriteria, PAGINATION_WRAPPER);
	}


	public String getRegularizationAutoEscalatedToMe(RegularizationSearchCriteria searchCriteria, String uuid,
			List<Object> preparedStmtList) {
		StringBuilder queryBuilder = new StringBuilder(REGULARIZATION_SEARCH_QUERY);
		queryBuilder.append(AUTO_ESCALATED);
		preparedStmtList.add(BPAConstants.AUTO_ESCALATED_COMMENT);
		if (!ObjectUtils.isEmpty(uuid)) {
			addClauseIfRequired(queryBuilder, preparedStmtList);
			queryBuilder.append(" asg.assignee = ? ");
			preparedStmtList.add(uuid);
		}
		addClauseIfRequired(queryBuilder, preparedStmtList);
		queryBuilder.append(REULARIZATION_NOT_IN_STATUS);
		
		return addPaginationWrapper(queryBuilder.toString(), preparedStmtList, searchCriteria, PAGINATION_WRAPPER);
	}

	/**
	 * Method to get Village Search Query
	 * 
	 * @param criteria
	 * @param preparedStmtList
	 * @return
	 */
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
	public String getRegularizationFeePendingSearchQuery(@Valid BPASearchCriteria criteria,
			List<Object> preparedStmtList) {
		
		StringBuilder query = new StringBuilder(QUERY_FOR_REGULARIZATION_AT_PENDING_SANC_FEE);

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


	public String getRegularizationDraftSearchQuery(@Valid RegularizationDraftSearchCriteria criteria,
			List<Object> preparedStmtList) {
		
		StringBuilder builder = new StringBuilder(REGULARIZATION_DRAFT_SEARCH_QUERY);

		if (criteria.getTenantId() != null) {
			if (criteria.getTenantId().split("\\.").length == 1) {

				addClauseIfRequired(preparedStmtList, builder);
				builder.append(" erd.tenantid like ?");
				preparedStmtList.add('%' + criteria.getTenantId() + '%');
			} else {
				addClauseIfRequired(preparedStmtList, builder);
				builder.append(" erd.tenantid=? ");
				preparedStmtList.add(criteria.getTenantId());
			}
		}

		List<String> ids = criteria.getIds();
		if (!CollectionUtils.isEmpty(ids)) {
			addClauseIfRequired(preparedStmtList, builder);
			builder.append(" erd.id IN (").append(createQuery(ids)).append(")");
			addToPreparedStatement(preparedStmtList, ids);
		}

		String draftNumber = criteria.getDraftNo();
		if (draftNumber != null) {
			addClauseIfRequired(preparedStmtList, builder);
			builder.append(" erd.draftno = ?");
			preparedStmtList.add(criteria.getDraftNo());
		}

		String status = criteria.getStatus();
		if (status != null) {
			addClauseIfRequired(preparedStmtList, builder);
			builder.append(" erd.status = ?");
			preparedStmtList.add(criteria.getStatus());
		}
		
		if (criteria.getCreatedBy() != null) {
			addClauseIfRequired(preparedStmtList, builder);
			builder.append(" erd.createdby = ?");
			preparedStmtList.add(criteria.getCreatedBy());
		}

		return builder.toString();

	}
    
}
