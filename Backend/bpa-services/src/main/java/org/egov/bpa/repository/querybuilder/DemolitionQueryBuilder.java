package org.egov.bpa.repository.querybuilder;

import java.util.List;

import javax.validation.Valid;

import org.egov.bpa.config.BPAConfiguration;
import org.egov.bpa.web.model.VillageSearchCriteria;
import org.egov.bpa.web.model.demolition.DemolitionFISearchCriteria;
import org.egov.bpa.web.model.demolition.DemolitionSearchCriteria;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

@Component
public class DemolitionQueryBuilder {
	
	@Autowired
	private BPAConfiguration config;
	
	private static final String QUERY_FOR_VILLAGE_DATA = "select distinct on (applicationno)applicationno as bpa_app, status as bpa_status, mauza as village "
			+ "from eg_bpa_demolition_application demolition inner join eg_bpa_demolition_landinfo landinfo "
			+ "on landinfo.demolitionid = demolition.id ";
	
	private static final String DEMOLITION_APPLICATION_APPROVEDBY_QUERY = "	select ebra.id, ebra.tenantid as tenantid, ebra.applicationno, ebra.status as workflowstate, "
			+ " ebra.status as applicationstatus, ebra.additionaldetails as demolition_additionaldetails, ebra.id as demolition_id, ebra.applicationtype as applicationtype,"
			+ " pi.createdby, pi.createdtime, pi.lastmodifiedby, pi.lastmodifiedtime "
			+ " from eg_bpa_demolition_application ebra inner join eg_wf_processinstance_v2 pi on ebra.applicationno = pi.businessid ";
	
	private static final String DSC_PAGINATION_WRAPPER = "SELECT * FROM "
			+ "(SELECT *, DENSE_RANK() OVER (ORDER BY lastModifiedTime DESC) offset_ FROM " + "({})"
			+ " result) result_offset " + "WHERE offset_ > ? AND offset_ <= ?";

	private static final String PAGINATION_WRAPPER = "SELECT * FROM "
			+ "(SELECT *, DENSE_RANK() OVER (ORDER BY app_createdtime DESC) offset_ FROM " + "({})"
			+ " result) result_offset " + "WHERE offset_ > ? AND offset_ <= ?";
	
	private static final String DEMOLITION_APPLICATION_APPROVEDBYDOCUMENTLIST_QUERY = " select additionalDetails as doc_details, documenttype as bpa_doc_documenttype, "
			+ " filestoreid as bpa_doc_filestore, id as bpa_doc_id, documentuid, demolitionid from eg_bpa_demolition_document ebdd ";
	
	private static final String DEMOLITION_SEARCH_QUERY = "SELECT "
			+ " app.id AS app_id,"
			+ " app.tenantid AS app_tenantid,"
			+ " app.applicationno AS app_applicationno,"
			+ " app.status AS app_status,"
			+ " app.applicationtype AS app_applicationtype,"
			+ " app.servicetype AS app_servicetype,"
			+ " app.plotdetails AS app_plotdetails,"
			+ " app.additionaldetails AS app_additionaldetails,"
			+ " app.applicationdate AS app_applicationdate,"
			+ " app.approvalno AS app_approvalno,"
			+ " app.approvaldate AS app_approvaldate,"
			+ " app.businessservice AS app_businessservice,"
			+ " app.accountid AS app_accountid,"
			+ " app.createdby AS app_createdby,"
			+ " app.lastmodifiedby AS app_lastmodifiedby,"
			+ " app.createdtime AS app_createdtime,"
			+ " app.lastmodifiedtime AS app_lastmodifiedtime,"
			+ " landinfo.id AS landinfo_id,"
			+ " landinfo.tenantid AS landinfo_tenantid,"
			+ " landinfo.demolitionid AS landinfo_demolitionid,"
			+ " landinfo.totallandarea AS landinfo_totallandarea,"
			+ " landinfo.plotnumber AS landinfo_plotnumber,"
			+ " landinfo.mauza AS landinfo_mauza,"
			+ " landinfo.landownername AS landinfo_landownername,"
			+ " landinfo.createdby AS landinfo_createdby,"
			+ " landinfo.lastmodifiedby AS landinfo_lastmodifiedby,"
			+ " landinfo.createdtime AS landinfo_createdtime,"
			+ " landinfo.lastmodifiedtime AS landinfo_lastmodifiedtime,"
			+ " blockinfo.id AS blockinfo_id,"
			+ " blockinfo.tenantid AS blockinfo_tenantid,"
			+ " blockinfo.demolitionlandinfoid AS blockinfo_demolitionlandinfoid,"
			+ " blockinfo.anyapprovedarea AS blockinfo_anyapprovedarea,"
			+ " blockinfo.occupancy AS blockinfo_occupancy,"
			+ " blockinfo.totalbua AS blockinfo_totalbua,"
			+ " blockinfo.nooffloors AS blockinfo_nooffloors,"
			+ " blockinfo.setbackdetails AS blockinfo_setbackdetails,"
			+ " blockinfo.buildingdistance AS blockinfo_buildingdistance,"
			+ " blockinfo.buildingheight AS blockinfo_buildingheight,"
			+ " blockinfo.totalbuatobedemolished AS blockinfo_totalbuatobedemolished,"
			+ " blockinfo.createdby AS blockinfo_createdby,"
			+ " blockinfo.lastmodifiedby AS blockinfo_lastmodifiedby,"
			+ " blockinfo.createdtime AS blockinfo_createdtime,"
			+ " blockinfo.lastmodifiedtime AS blockinfo_lastmodifiedtime,"
			+ " addr.id AS addr_id,"
			+ " addr.tenantid AS addr_tenantid,"
			+ " addr.doorno AS addr_doorno,"
			+ " addr.plotno AS addr_plotno,"
			+ " addr.landmark AS addr_landmark,"
			+ " addr.city AS addr_city,"
			+ " addr.district AS addr_district,"
			+ " addr.region AS addr_region,"
			+ " addr.state AS addr_state,"
			+ " addr.country AS addr_country,"
			+ " addr.locality AS addr_locality,"
			+ " addr.pincode AS addr_pincode,"
			+ " addr.additiondetails AS addr_additiondetails,"
			+ " addr.buildingname AS addr_buildingname,"
			+ " addr.street AS addr_street,"
			+ " addr.landinfoid AS addr_landinfoid,"
			+ " addr.createdby AS addr_createdby,"
			+ " addr.lastmodifiedby AS addr_lastmodifiedby,"
			+ " addr.createdtime AS addr_createdtime,"
			+ " addr.lastmodifiedtime AS addr_lastmodifiedtime,"
			+ " geo.id AS geo_id,"
			+ " geo.latitude AS geo_latitude,"
			+ " geo.longitude AS geo_longitude,"
			+ " geo.addressid AS geo_addressid,"
			+ " geo.additionaldetails AS geo_additionaldetails,"
			+ " geo.createdby AS geo_createdby,"
			+ " geo.lastmodifiedby AS geo_lastmodifiedby,"
			+ " geo.createdtime AS geo_createdtime,"
			+ " geo.lastmodifiedtime AS geo_lastmodifiedtime,"
			+ " owner.id AS owner_id,"
			+ " owner.uuid AS owner_uuid,"
			+ " owner.isprimaryowner AS owner_isprimaryowner,"
			+ " owner.ownershippercentage AS owner_ownershippercentage,"
			+ " owner.institutionid AS owner_institutionid,"
			+ " owner.additionaldetails AS owner_additionaldetails,"
			+ " owner.demolitionid AS owner_demolitionid,"
			+ " owner.relationship AS owner_relationship,"
			+ " owner.createdby AS owner_createdby,"
			+ " owner.lastmodifiedby AS owner_lastmodifiedby,"
			+ " owner.createdtime AS owner_createdtime,"
			+ " owner.lastmodifiedtime AS owner_lastmodifiedtime,"
			+ " doc.id as document_id,"
			+ " doc.documenttype as document_type,"
			+ " doc.filestoreid as document_filestoreid,"
			+ " doc.documentuid as document_uid,"
			+ " doc.additionaldetails as document_additionaldetails,"
			+ " doc.createdby as document_createdby,"
			+ " doc.lastmodifiedby as document_lastmodifiedby,"
			+ " doc.createdtime as document_createdtime,"
			+ " doc.lastmodifiedtime as document_lastmodifiedtime"
			+ " FROM eg_bpa_demolition_application app"
			+ " LEFT OUTER JOIN eg_bpa_demolition_landinfo landinfo "
			+ " ON app.id = landinfo.demolitionid"
			+ " LEFT OUTER JOIN eg_bpa_demolition_blockinfo blockinfo "
			+ " ON landinfo.id = blockinfo.demolitionlandinfoid"
			+ " LEFT OUTER JOIN eg_bpa_demolition_address addr "
			+ " ON landinfo.id = addr.landinfoid"
			+ " LEFT OUTER JOIN eg_bpa_demolition_geolocation geo "
			+ " ON addr.id = geo.addressid"
			+ " LEFT OUTER JOIN eg_bpa_demolition_owners owner "
			+ " ON app.id = owner.demolitionid"
			+ " LEFT OUTER JOIN eg_bpa_demolition_document doc "
			+ " ON app.id = doc.demolitionid ";
	
    private static final String FIELDINSPECTOR_DETAILS_QUERY = "SELECT id, applicationno, tenantid, approachroad, sitesituation, buildingsituation, report_details, additionaldetails, setback, createdby, lastmodifiedby, createdtime, lastmodifiedtime\r\n"
			+ "	FROM eg_bpa_demolition_fieldinspection_details ";

	public String getDemolitionSearchQuery(DemolitionSearchCriteria criteria, List<Object> preparedStmtList) {

		StringBuilder builder = new StringBuilder(DEMOLITION_SEARCH_QUERY);

		if (criteria.getTenantId() != null) {
			if (criteria.getTenantId().split("\\.").length == 1) {

				addClauseIfRequired(preparedStmtList, builder);
				builder.append(" app.tenantid like ?");
				preparedStmtList.add('%' + criteria.getTenantId() + '%');
			} else {
				addClauseIfRequired(preparedStmtList, builder);
				builder.append(" app.tenantid=? ");
				preparedStmtList.add(criteria.getTenantId());
			}
		}

		List<String> ids = criteria.getIds();
		if (!CollectionUtils.isEmpty(ids)) {
			addClauseIfRequired(preparedStmtList, builder);
			builder.append(" app.id IN (").append(createQuery(ids)).append(")");
			addToPreparedStatement(preparedStmtList, ids);
		}

		String approvalNo = criteria.getApprovalNo();
		if (approvalNo != null) {
			addClauseIfRequired(preparedStmtList, builder);
			builder.append(" app.approvalNo = ?");
			preparedStmtList.add(criteria.getApprovalNo());
		}

		String applicationNo = criteria.getApplicationNo();
		if (applicationNo != null) {
			if (applicationNo.length() == 6) {
				addClauseIfRequired(preparedStmtList, builder);
				builder.append(" app.applicationNo like ?");
				preparedStmtList.add("%" + criteria.getApplicationNo());
			} else {
				addClauseIfRequired(preparedStmtList, builder);
				builder.append(" app.applicationNo =?");
				preparedStmtList.add(criteria.getApplicationNo());
			}
		}

		String status = criteria.getStatus();
		if (status != null) {
			addClauseIfRequired(preparedStmtList, builder);
			builder.append(" app.status = ?");
			preparedStmtList.add(criteria.getStatus());

		}

		String businessService = criteria.getBusinessService();
		if (!StringUtils.isEmpty(businessService)) {
			addClauseIfRequired(preparedStmtList, builder);
			builder.append(" app.businessService = ? ");
			preparedStmtList.add(criteria.getStatus());
		}
		
		if (!ObjectUtils.isEmpty(criteria.getCreatedBy())) {
			addClauseIfRequired(preparedStmtList, builder);
			builder.append(" app.createdby in (").append(createQuery(criteria.getCreatedBy()))
					.append(")");
			addToPreparedStatement(preparedStmtList, criteria.getCreatedBy());
		}

		//return builder.toString();
		return addPaginationWrapper(builder.toString(), preparedStmtList, criteria, PAGINATION_WRAPPER);

	}

	private String addPaginationWrapper(String query, List<Object> preparedStmtList, DemolitionSearchCriteria criteria,
			String paginationWrapper) {
		
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
	 * add if clause to the Statement if required or else AND
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

	public String getApplicationAprovedBy(String uuid, List<Object> preparedStmtList,
			@Valid DemolitionSearchCriteria criteria) {

		StringBuilder builder = new StringBuilder(DEMOLITION_APPLICATION_APPROVEDBY_QUERY);
		addClauseIfRequired(preparedStmtList, builder);

		if (uuid != null) {

			builder.append(" ebra.status = 'APPROVED' and pi.action = 'APPROVE' and pi.createdby = ? ");
			preparedStmtList.add(uuid);
		}
		builder.append("  order by ebra.applicationno, pi.createdtime desc ");
		//return builder.toString();
		return addDscPaginationWrapper(builder.toString(), preparedStmtList, criteria);
	}

	private String addDscPaginationWrapper(String query, List<Object> preparedStmtList,
			@Valid DemolitionSearchCriteria criteria) {
		
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

	public String getDocumentApprovedBy(List<String> demolitionIds, List<Object> preparedStmtList) {
		
		StringBuilder builder = new StringBuilder(DEMOLITION_APPLICATION_APPROVEDBYDOCUMENTLIST_QUERY);

        if (demolitionIds != null) {
            addClauseIfRequired(preparedStmtList, builder);
            builder.append(" demolitionid IN (").append(createQuery(demolitionIds)).append(")");
			addToPreparedStatement(preparedStmtList, demolitionIds);
        }
        return builder.toString();
	}
	
	public String getfieldinspectionReportDetails(@Valid DemolitionFISearchCriteria criteria,
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


}
