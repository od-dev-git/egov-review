package org.egov.pt.repository.builder;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.egov.pt.config.PropertyConfiguration;
import org.egov.pt.models.PropertyCriteria;
import org.egov.pt.util.CommonUtils;
import org.egov.tracer.model.CustomException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

@Component
public class PropertyQueryBuilder {
	
	@Autowired
	private PropertyConfiguration config;

	private static final String SELECT = "SELECT ";
	private static final String INNER_JOIN = "INNER JOIN";
	private static final String LEFT_JOIN  =  "LEFT OUTER JOIN";
	private static final String AND_QUERY = " AND ";
	private static final String SEARCHTYPE_APPLICATION = "APPLICATION";
	
	private static String PROEPRTY_AUDIT_QUERY = "select property from eg_pt_property_audit where propertyid=?";

	private static String PROEPRTY_ID_QUERY = "select propertyid from eg_pt_property where id in (select propertyid from eg_pt_owner where userid IN {replace})";
	
	private static String REPLACE_STRING = "{replace}";
	
	private static String WITH_CLAUSE_QUERY = " WITH propertyresult AS ({replace}) SELECT * FROM propertyresult "
											+ "INNER JOIN (SELECT propertyid, min(statusorder) as minorder FROM propertyresult GROUP BY propertyid) as minresult "
											+ "ON minresult.propertyid=propertyresult.propertyid AND minresult.minorder=propertyresult.statusorder";
	

	 // Select query
	
	private static String propertySelectValues = "property.id as pid, property.propertyid, property.tenantid as ptenantid, surveyid, accountid, oldpropertyid, property.status as propertystatus, acknowldgementnumber, propertytype, ownershipcategory,property.usagecategory as pusagecategory, creationreason, nooffloors, landarea, property.superbuiltuparea as propertysbpa, linkedproperties, source, channel, property.createdby as pcreatedby, property.lastmodifiedby as plastmodifiedby, property.createdtime as pcreatedtime,"
			+ " property.lastmodifiedtime as plastmodifiedtime, property.additionaldetails as padditionaldetails, property.ddnno as ddnno, property.legacyHoldingNo as legacyHoldingNo, (CASE WHEN property.status='ACTIVE' then 0 WHEN property.status='INWORKFLOW' then 1 WHEN property.status='DEACTIVATED' then 2 WHEN property.status='INACTIVE' then 3 ELSE 4 END) as statusorder, ";

	private static String assessmentSelectValues = "property.id as pid, property.propertyid, property.tenantid as ptenantid, surveyid, accountid, oldpropertyid, assessment.status as propertystatus, assessment.assessmentnumber acknowldgementnumber, propertytype, ownershipcategory,property.usagecategory as pusagecategory, 'ASSESSMENT' as creationreason, nooffloors, landarea, property.superbuiltuparea as propertysbpa, linkedproperties, property.source, property.channel, assessment.createdby as pcreatedby, assessment.lastmodifiedby as plastmodifiedby, assessment.createdtime as pcreatedtime,"
			+ " assessment.lastmodifiedtime as plastmodifiedtime, property.additionaldetails as padditionaldetails, property.ddnno as ddnno, property.legacyHoldingNo as legacyHoldingNo, (CASE WHEN assessment.status='ACTIVE' then 0 WHEN assessment.status='INWORKFLOW' then 1 WHEN assessment.status='INACTIVE' then 2 ELSE 3 END) as statusorder, ";

	private static String addressSelectValues = "address.tenantid as adresstenantid, address.id as addressid, address.propertyid as addresspid, ward, latitude, longitude, doorno, plotno, buildingname, street, landmark, city, pincode, locality, district, region, state, country, address.createdby as addresscreatedby, address.lastmodifiedby as addresslastmodifiedby, address.createdtime as addresscreatedtime, address.lastmodifiedtime as addresslastmodifiedtime, address.additionaldetails as addressadditionaldetails, " ;

	private static String institutionSelectValues = "institution.id as institutionid,institution.propertyid as institutionpid, institution.tenantid as institutiontenantid, institution.name as institutionname, institution.type as institutiontype, designation, nameofauthorizedperson, institution.createdby as institutioncreatedby, institution.lastmodifiedby as institutionlastmodifiedby, institution.createdtime as institutioncreatedtime, institution.lastmodifiedtime as institutionlastmodifiedtime, ";

	private static String propertyDocSelectValues = "pdoc.id as pdocid, pdoc.tenantid as pdoctenantid, pdoc.entityid as pdocentityid, pdoc.documenttype as pdoctype, pdoc.filestoreid as pdocfilestore, pdoc.documentuid as pdocuid, pdoc.status as pdocstatus, ";

	private static String ownerSelectValues = "owner.tenantid as owntenantid, ownerInfoUuid, owner.propertyid as ownpropertyid, userid, owner.status as ownstatus, isprimaryowner, ownertype, ownershippercentage, owner.institutionid as owninstitutionid, relationship, owner.createdby as owncreatedby, owner.createdtime as owncreatedtime,owner.lastmodifiedby as ownlastmodifiedby, owner.lastmodifiedtime as ownlastmodifiedtime, ";

	private static String ownerDocSelectValues = " owndoc.id as owndocid, owndoc.tenantid as owndoctenantid, owndoc.entityid as owndocentityId, owndoc.documenttype as owndoctype, owndoc.filestoreid as owndocfilestore, owndoc.documentuid as owndocuid, owndoc.status as owndocstatus, ";
	
	private static String UnitSelectValues = "unit.id as unitid, unit.tenantid as unittenantid, unit.propertyid as unitpid, floorno, unittype, unit.usagecategory as unitusagecategory, occupancytype, occupancydate, carpetarea, builtuparea, plintharea, unit.superbuiltuparea as unitspba, arv, constructiontype, constructiondate, dimensions, unit.active as isunitactive, unit.createdby as unitcreatedby, unit.createdtime as unitcreatedtime, unit.lastmodifiedby as unitlastmodifiedby, unit.lastmodifiedtime as unitlastmodifiedtime ";
	
	

	private static final String QUERY = SELECT 
			
			+	propertySelectValues    
			
			+   addressSelectValues     
			
			+   institutionSelectValues 
			
			+   propertyDocSelectValues
			
			+   ownerSelectValues 
			
			+   ownerDocSelectValues  
			
			+   UnitSelectValues
			
			+   " FROM EG_PT_PROPERTY property " 
			
			+   INNER_JOIN +  " EG_PT_ADDRESS address         ON property.id = address.propertyid " 
			
			+   LEFT_JOIN  +  " EG_PT_INSTITUTION institution ON property.id = institution.propertyid " 
			
			+   LEFT_JOIN  +  " EG_PT_DOCUMENT pdoc           ON property.id = pdoc.entityid "
			
			+   INNER_JOIN +  " EG_PT_OWNER owner             ON property.id = owner.propertyid " 
			
			+   LEFT_JOIN  +  " EG_PT_DOCUMENT owndoc         ON owner.ownerinfouuid = owndoc.entityid "
			
			+	LEFT_JOIN  +  " EG_PT_UNIT unit		          ON property.id =  unit.propertyid "
			
			+ " WHERE ";
	
	private static final String ASMT_QUERY = SELECT 

			+	assessmentSelectValues    

			+   addressSelectValues     

			+   institutionSelectValues 

			+   propertyDocSelectValues

			+   ownerSelectValues 

			+   ownerDocSelectValues  

			+   UnitSelectValues

			+   " FROM EG_PT_PROPERTY property " 

			+   INNER_JOIN +  " EG_PT_ASMT_ASSESSMENT assessment ON property.propertyid = assessment.propertyid "

			+   INNER_JOIN +  " EG_PT_ADDRESS address         ON property.id = address.propertyid " 

			+   LEFT_JOIN  +  " EG_PT_INSTITUTION institution ON property.id = institution.propertyid " 

			+   LEFT_JOIN  +  " EG_PT_DOCUMENT pdoc           ON property.id = pdoc.entityid "

			+   INNER_JOIN +  " EG_PT_OWNER owner             ON property.id = owner.propertyid " 

			+   LEFT_JOIN  +  " EG_PT_DOCUMENT owndoc         ON owner.ownerinfouuid = owndoc.entityid "

			+	LEFT_JOIN  +  " EG_PT_UNIT unit		          ON property.id =  unit.propertyid "

			+ " WHERE ";

	private static final String DISTINCT_TENANTIDS_CRITERIA = "SELECT distinct(tenantid) FROM eg_pt_property epp";

	private final String paginationWrapper = "SELECT * FROM "
			+ "(SELECT *, DENSE_RANK() OVER (ORDER BY plastmodifiedtime DESC, pid) offset_ FROM " + "({})" + " result) result_offset "
			+ "WHERE offset_ > ? AND offset_ <= ?";
	
	private static String propertySelectColumns = "property.id as pid, property.propertyid, property.tenantid as ptenantid, property.surveyid, property.accountid, property.oldpropertyid, property.status as propertystatus, property.acknowldgementnumber, property.propertytype, property.ownershipcategory,property.usagecategory as pusagecategory, property.creationreason, property.nooffloors, property.landarea, property.superbuiltuparea as propertysbpa, property.linkedproperties, property.source, property.channel, property.createdby as pcreatedby, property.lastmodifiedby as plastmodifiedby, property.createdtime as pcreatedtime,"
			+ " property.lastmodifiedtime as plastmodifiedtime, property.additionaldetails as padditionaldetails, (CASE WHEN property.status='ACTIVE' then 0 WHEN property.status='INWORKFLOW' then 1 WHEN property.status='INACTIVE' then 2 ELSE 3 END) as statusorder ";
	
	private static final String ACTIVE_ASSESMENT_FOR_CURRENT_FINYEAR_QUERY = SELECT 
			+ "assessment.propertyid, assessment.status"
			+ " FROM EG_PT_ASMT_ASSESSMENT assessment "
			+ " WHERE assessment.assessmentdate = (SELECT MAX(assessment2.assessmentdate) " 
			+ " FROM EG_PT_ASMT_ASSESSMENT assessment2 "
			+ " WHERE assessment2.propertyid = assessment.propertyid and assessment2.status = 'ACTIVE') " 
			+ " and assessment.status = 'ACTIVE' and assessment.financialyear = ? "
			+ " and not exists ( select 1 from eg_pt_asmt_assessment assessment3 "
			+ " where assessment.propertyid = assessment3.propertyid "
			+ " and assessment3.status='INWORKFLOW' ) "
			;
	
	
	private static final String ACTIVE_PROPERTY_ASSESMENT__FOR_CURRENT_FINYEAR_QUERY = SELECT 
			+ propertySelectColumns
			+   " FROM EG_PT_PROPERTY property " 
			+   INNER_JOIN 
			+ "("+ ACTIVE_ASSESMENT_FOR_CURRENT_FINYEAR_QUERY +") assessment ON property.propertyid = assessment.propertyid ";

	private static final String COUNT_OF_PROPERTY = "select COUNT(distinct ept.propertyid) as no_of_properties, "
			+ "COUNT(*) as no_of_applications from eg_pt_property ept inner join eg_pt_owner epo "
			+ "on ept.id = epo.propertyid where epo.userid in {replace} ";
	
	private final String COUNT_NOOF_ACTIVE_PROPERTY_BY_TENATID = "select count(property) FROM EG_PT_PROPERTY property  where property.status IN ('ACTIVE')";

	private final String QUERY_TO_GET_LEGACY_PROPERTY_DETAILS = "select details.tenantid, details.oldpropertyid, details.sujogpropertyid, details.legacyholdingid from eg_pt_property_legacy_details details ";
	
	private final String COUNT_NOOF_DISTINCT_PROPERTY_BY_TENATID_HAVING_ASMT_IN_CURRENTFY = SELECT 
			+ "	distinct "
			+ " assessment.propertyid from eg_pt_asmt_assessment assessment "
			+ " inner join eg_pt_property epp on epp.propertyid = assessment.propertyid "
			+ " where epp.status ='ACTIVE' and  "
			+ "	assessment.financialyear = ? ";
	
	private final String COUNT_NOOF_ACTIVE_PROPERTY_BY_TENATID_FOR_ASSESSMENT_JOB = SELECT 
			+ "  count (distinct epp.propertyid) from eg_pt_property epp "
			+ " inner join eg_pt_asmt_assessment epaa on epaa.propertyid = epp.propertyid "
			+ " and epp.status ='ACTIVE' "
			+ " and epaa.financialyear = ? "
			+ " and not exists ( select 1 from eg_pt_asmt_assessment assessment3 "
			+ "	where epp.propertyid = assessment3.propertyid and( assessment3.financialyear = ? "
			+ "	or (assessment3.financialyear = ? "
			+ "	and assessment3.status = 'INWORKFLOW') )) ";
	
	private String addPaginationWrapper(String query, List<Object> preparedStmtList, PropertyCriteria criteria) {
		
		
		Long limit = config.getDefaultLimit();
		Long offset = config.getDefaultOffset();
		String finalQuery = paginationWrapper.replace("{}", query);

		if (criteria.getLimit() != null && criteria.getLimit() <= config.getMaxSearchLimit())
			limit = criteria.getLimit();

		if (criteria.getLimit() != null && criteria.getLimit() > config.getMaxSearchLimit())
			limit = config.getMaxSearchLimit();

		if (criteria.getOffset() != null)
			offset = criteria.getOffset();

		preparedStmtList.add(offset);
		preparedStmtList.add(limit + offset);

		return finalQuery;
	}

	/**
	 * 
	 * @param criteria
	 * @param preparedStmtList
	 * @return
	 */
	public String getPropertySearchQuery(PropertyCriteria criteria, List<Object> preparedStmtList,Boolean isPlainSearch) {

		Boolean isEmpty = CollectionUtils.isEmpty(criteria.getPropertyIds())
					&& CollectionUtils.isEmpty(criteria.getAcknowledgementIds())
					&& CollectionUtils.isEmpty(criteria.getOldpropertyids())
					&& CollectionUtils.isEmpty(criteria.getUuids())
					&& CollectionUtils.isEmpty(criteria.getDdnNos())
					&& CollectionUtils.isEmpty(criteria.getLegacyHoldingNos())
					&& null == criteria.getMobileNumber()
					&& null == criteria.getName()
					&& null == criteria.getWard()
					&& null == criteria.getLocality();
		
		if(isEmpty)
			throw new CustomException("EG_PT_SEARCH_ERROR"," No criteria given for the property search");
		
		StringBuilder builder = new StringBuilder(QUERY);
		Boolean appendAndQuery = false;
		if(isPlainSearch)
		{
			Set<String> tenantIds = criteria.getTenantIds();
			if(!CollectionUtils.isEmpty(tenantIds))
			{
				//addClauseIfRequired(preparedStmtList,builder);
				builder.append("property.tenantid IN (").append(createQuery(tenantIds)).append(")");
				addToPreparedStatement(preparedStmtList,tenantIds);
				appendAndQuery = true;
			}
			if(criteria.getTenantId()!=null && !criteria.getTenantId().equals(config.getStateLevelTenantId()))
			{
				builder.append("property.tenantid=?");
				preparedStmtList.add(criteria.getTenantId());
				appendAndQuery = true;
			}
		}
		else
		{
			if(criteria.getTenantId()!=null && !criteria.getTenantId().equals(config.getStateLevelTenantId()))
			{
				addClauseIfRequired(preparedStmtList,builder);
				builder.append("property.tenantid=?");
				preparedStmtList.add(criteria.getTenantId());
			}
		}
		if (criteria.getFromDate() != null)
		{
			addClauseIfRequired(preparedStmtList,builder);
			// If user does NOT specify toDate, take today's date as the toDate by default
			if (criteria.getToDate() == null)
			{
				criteria.setToDate(Instant.now().toEpochMilli());
			}
			builder.append("property.createdTime BETWEEN ? AND ?");
			preparedStmtList.add(criteria.getFromDate());
			preparedStmtList.add(criteria.getToDate());
		}
		else
		{
			if(criteria.getToDate()!=null)
			{
				throw new CustomException("INVALID SEARCH", "From Date should be mentioned first");
			}
		}

		if (null != criteria.getStatus()) {

			if(appendAndQuery)
				builder.append(AND_QUERY);
			builder.append("property.status = ?");
			preparedStmtList.add(criteria.getStatus());
			appendAndQuery= true;
		}
		
		if (null != criteria.getLocality()) {

			if(appendAndQuery)
				builder.append(AND_QUERY);
			builder.append("address.locality = ?");
			preparedStmtList.add(criteria.getLocality());
			appendAndQuery= true;
		}

		Set<String> propertyIds = criteria.getPropertyIds();
		if (!CollectionUtils.isEmpty(propertyIds)) {

			if(appendAndQuery)
				builder.append(AND_QUERY);
			builder.append("property.propertyid IN (").append(createQuery(propertyIds)).append(")");
			addToPreparedStatementWithUpperCase(preparedStmtList, propertyIds);
			appendAndQuery= true;
		}
		
		Set<String> acknowledgementIds = criteria.getAcknowledgementIds();
		if (!CollectionUtils.isEmpty(acknowledgementIds)) {

			if(appendAndQuery)
				builder.append(AND_QUERY);
			builder.append("property.acknowldgementnumber IN (").append(createQuery(acknowledgementIds)).append(")");
			addToPreparedStatementWithUpperCase(preparedStmtList, acknowledgementIds);
			appendAndQuery= true;
		}
		
		Set<String> uuids = criteria.getUuids();
		if (!CollectionUtils.isEmpty(uuids)) {

			if(appendAndQuery)
				builder.append(AND_QUERY);
			builder.append("property.id IN (").append(createQuery(uuids)).append(")");
			addToPreparedStatement(preparedStmtList, uuids);
			appendAndQuery= true;
		}

		Set<String> oldpropertyids = criteria.getOldpropertyids();
		if (!CollectionUtils.isEmpty(oldpropertyids)) {

			if(appendAndQuery)
				builder.append(AND_QUERY);
			builder.append("property.oldpropertyid IN (").append(createQuery(oldpropertyids)).append(")");
			addToPreparedStatement(preparedStmtList, oldpropertyids);
			appendAndQuery= true;
		}
		
		if(criteria.getWard() != null) {
			if(appendAndQuery)
				builder.append(AND_QUERY);
			builder.append(" address.ward = ? ");
			preparedStmtList.add(criteria.getWard());
			appendAndQuery= true;
		}

		Set<String> ddnNos = criteria.getDdnNos();
		if(!CollectionUtils.isEmpty(ddnNos)){
			if(appendAndQuery)
				builder.append(AND_QUERY);
			builder.append("property.ddnNo IN (").append(createQuery(ddnNos)).append(")");
			addToPreparedStatementWithUpperCase(preparedStmtList, ddnNos);
			appendAndQuery= true;
		}

		Set<String> legacyHoldingNos= criteria.getLegacyHoldingNos();
		if(!CollectionUtils.isEmpty(legacyHoldingNos)){
			if(appendAndQuery)
				builder.append(AND_QUERY);
			builder.append("property.legacyHoldingNo IN (").append(createQuery(legacyHoldingNos)).append(")");
			addToPreparedStatementWithUpperCase(preparedStmtList, legacyHoldingNos);
			appendAndQuery= true;
		}
		/* If searchType = APPLICATION then return applications only I */
		if (StringUtils.hasText(criteria.getSearchType()) && criteria.getSearchType().equalsIgnoreCase(SEARCHTYPE_APPLICATION)) {
			return addPaginationWrapper(builder.toString(), preparedStmtList, criteria);
		}

		String withClauseQuery = WITH_CLAUSE_QUERY.replace(REPLACE_STRING, builder);
		return addPaginationWrapper(withClauseQuery, preparedStmtList, criteria);
	}

	public String getAssessmentSearchQuery(PropertyCriteria criteria, List<Object> preparedStmtList,Boolean isPlainSearch) {

		Boolean isEmpty = CollectionUtils.isEmpty(criteria.getPropertyIds())
					&& CollectionUtils.isEmpty(criteria.getAcknowledgementIds())
					&& CollectionUtils.isEmpty(criteria.getOldpropertyids())
					&& CollectionUtils.isEmpty(criteria.getUuids())
					&& null == criteria.getMobileNumber()
					&& null == criteria.getName();

		if(isEmpty)
			throw new CustomException("EG_PT_SEARCH_ERROR"," No criteria given for the property search");

		StringBuilder builder = new StringBuilder(ASMT_QUERY);
		Boolean appendAndQuery = false;
		if(isPlainSearch)
		{
			Set<String> tenantIds = criteria.getTenantIds();
			if(!CollectionUtils.isEmpty(tenantIds))
			{
				addClauseIfRequired(preparedStmtList,builder);
				builder.append("property.tenantid IN (").append(createQuery(tenantIds)).append(")");
				addToPreparedStatement(preparedStmtList,tenantIds);
			}
		}
		else
		{
			if(criteria.getTenantId()!=null)
			{
				addClauseIfRequired(preparedStmtList,builder);
				builder.append("property.tenantid=?");
				preparedStmtList.add(criteria.getTenantId());
			}
		}
		if (criteria.getFromDate() != null)
		{
			addClauseIfRequired(preparedStmtList,builder);
			// If user does NOT specify toDate, take today's date as the toDate by default
			if (criteria.getToDate() == null)
			{
				criteria.setToDate(Instant.now().toEpochMilli());
			}
			builder.append("property.createdTime BETWEEN ? AND ?");
			preparedStmtList.add(criteria.getFromDate());
			preparedStmtList.add(criteria.getToDate());
		}
		else
		{
			if(criteria.getToDate()!=null)
			{
				throw new CustomException("INVALID SEARCH", "From Date should be mentioned first");
			}
		}

		if (null != criteria.getStatus()) {

			if(appendAndQuery)
				builder.append(AND_QUERY);
			builder.append("property.status = ?");
			preparedStmtList.add(criteria.getStatus());
			appendAndQuery= true;
		}

		if (null != criteria.getLocality()) {

			if(appendAndQuery)
				builder.append(AND_QUERY);
			builder.append("address.locality = ?");
			preparedStmtList.add(criteria.getLocality());
			appendAndQuery= true;
		}

		Set<String> propertyIds = criteria.getPropertyIds();
		if (!CollectionUtils.isEmpty(propertyIds)) {

			if(appendAndQuery)
				builder.append(AND_QUERY);
			builder.append("property.propertyid IN (").append(createQuery(propertyIds)).append(")");
			addToPreparedStatementWithUpperCase(preparedStmtList, propertyIds);
			appendAndQuery= true;
		}

		Set<String> acknowledgementIds = criteria.getAcknowledgementIds();
		if (!CollectionUtils.isEmpty(acknowledgementIds)) {

			if(appendAndQuery)
				builder.append(AND_QUERY);
			builder.append("assessment.assessmentnumber IN (").append(createQuery(acknowledgementIds)).append(")");
			addToPreparedStatementWithUpperCase(preparedStmtList, acknowledgementIds);
			appendAndQuery= true;
		}

		Set<String> uuids = criteria.getUuids();
		if (!CollectionUtils.isEmpty(uuids)) {

			if(appendAndQuery)
				builder.append(AND_QUERY);
			builder.append("property.id IN (").append(createQuery(uuids)).append(")");
			addToPreparedStatement(preparedStmtList, uuids);
			appendAndQuery= true;
		}

		Set<String> oldpropertyids = criteria.getOldpropertyids();
		if (!CollectionUtils.isEmpty(oldpropertyids)) {

			if(appendAndQuery)
				builder.append(AND_QUERY);
			builder.append("property.oldpropertyid IN (").append(createQuery(oldpropertyids)).append(")");
			addToPreparedStatement(preparedStmtList, oldpropertyids);
			appendAndQuery= true;
		}

		String withClauseQuery = WITH_CLAUSE_QUERY.replace(REPLACE_STRING, builder);
		return addPaginationWrapper(withClauseQuery, preparedStmtList, criteria);
	}
	
	public String getPropertyQueryForBulkSearch(PropertyCriteria criteria, List<Object> preparedStmtList,Boolean isPlainSearch) {

		Boolean isEmpty = CollectionUtils.isEmpty(criteria.getPropertyIds())
				&& CollectionUtils.isEmpty(criteria.getUuids());

		if(isEmpty)
			throw new CustomException("EG_PT_SEARCH_ERROR"," No propertyid or uuid given for the property Bulk search");

		StringBuilder builder = new StringBuilder(QUERY);
		Boolean appendAndQuery = false;
		if(isPlainSearch)
		{
			Set<String> tenantIds = criteria.getTenantIds();
			if(!CollectionUtils.isEmpty(tenantIds))
			{
				addClauseIfRequired(preparedStmtList,builder);
				builder.append("property.tenantid IN (").append(createQuery(tenantIds)).append(")");
				addToPreparedStatement(preparedStmtList,tenantIds);
			}
		}
		else
		{
			if(criteria.getTenantId()!=null)
			{
				addClauseIfRequired(preparedStmtList,builder);
				builder.append("property.tenantid=?");
				preparedStmtList.add(criteria.getTenantId());
			}
		}
		if (criteria.getFromDate() != null)
		{
			addClauseIfRequired(preparedStmtList,builder);
			// If user does NOT specify toDate, take today's date as the toDate by default
			if (criteria.getToDate() == null)
			{
				criteria.setToDate(Instant.now().toEpochMilli());
			}
			builder.append("property.createdTime BETWEEN ? AND ?");
			preparedStmtList.add(criteria.getFromDate());
			preparedStmtList.add(criteria.getToDate());
		}
		else
		{
			if(criteria.getToDate()!=null)
			{
				throw new CustomException("INVALID SEARCH", "From Date should be mentioned first");
			}
		}

		Set<String> propertyIds = criteria.getPropertyIds();
		if (!CollectionUtils.isEmpty(propertyIds)) {
			if(appendAndQuery)
				builder.append(AND_QUERY);
			builder.append("property.propertyid IN (").append(createQuery(propertyIds)).append(")");
			addToPreparedStatementWithUpperCase(preparedStmtList, propertyIds);
			appendAndQuery= true;
		}

		Set<String> uuids = criteria.getUuids();
		if (!CollectionUtils.isEmpty(uuids)) {

			if(appendAndQuery)
				builder.append(AND_QUERY);
			builder.append("property.id IN (").append(createQuery(uuids)).append(")");
			addToPreparedStatement(preparedStmtList, uuids);
			appendAndQuery= true;
		}
		return addPaginationWrapper(builder.toString(), preparedStmtList, criteria);
	}

	public String getPropertyIdsQuery(Set<String> ownerIds, List<Object> preparedStmtList) {

		StringBuilder query = new StringBuilder("(");
		query.append(createQuery(ownerIds));
		addToPreparedStatement(preparedStmtList, ownerIds);
		query.append(")");
		
		return PROEPRTY_ID_QUERY.replace(REPLACE_STRING, query).toString();
	}

	private String createQuery(Set<String> ids) {
		StringBuilder builder = new StringBuilder();
		int length = ids.size();
		for (int i = 0; i < length; i++) {
			builder.append(" ?");
			if (i != length - 1)
				builder.append(",");
		}
		return builder.toString();
	}

	private void addToPreparedStatement(List<Object> preparedStmtList, Set<String> ids) {
		ids.forEach(id -> {
			preparedStmtList.add(id);
		});
	}
	
	private void addToPreparedStatementWithUpperCase(List<Object> preparedStmtList, Set<String> ids) {
		ids.forEach(id -> {
			preparedStmtList.add(id.toUpperCase());
		});
	}

	private static void addClauseIfRequired(List<Object> values,StringBuilder queryString) {
		if (values.isEmpty())
			queryString.append(" WHERE ");
		else {
			queryString.append(" AND");
		}
	}

	public String getpropertyAuditQuery() {
		return PROEPRTY_AUDIT_QUERY;
	}
	
	public String getLegacyPropertyDeatilsQuery(PropertyCriteria criteria, List<Object> preparedStmtList) {
		
		StringBuilder query = new StringBuilder(QUERY_TO_GET_LEGACY_PROPERTY_DETAILS);
		
		if(criteria.getTenantId()!=null){
			query.append(" WHERE details.tenantid=?");
			preparedStmtList.add(criteria.getTenantId());
		}
		
		Set<String> legacyPropertyIds =criteria.getLegacyPropertyId();
		if(legacyPropertyIds !=  null){
			query.append(AND_QUERY);
			query.append(" legacyholdingid = ").append(createQuery(legacyPropertyIds));
			addToPreparedStatementWithUpperCase(preparedStmtList, legacyPropertyIds);
		}
		
		Set<String> propertyIds = criteria.getPropertyIds();
		if(propertyIds != null){
			query.append(AND_QUERY);
			query.append(" sujogpropertyid = ").append(createQuery(propertyIds));
			addToPreparedStatementWithUpperCase(preparedStmtList, propertyIds);
		}
		
		Set<String> oldPropertyIds = criteria.getOldpropertyids();
		if(oldPropertyIds != null) {
			query.append(AND_QUERY);
			query.append(" oldpropertyid = ").append(createQuery(oldPropertyIds));
			addToPreparedStatementWithUpperCase(preparedStmtList, oldPropertyIds);	
		}
		return query.toString();
	}

	public String getDistinctTenantIds() {
		return DISTINCT_TENANTIDS_CRITERIA;
	}
	
	
	/**
	 * 
	 * @param criteria
	 * @param preparedStmtList
	 * @return
	 */
	public String getActivePropertyWithActiveCurrentFinYearAssesmentSearchQuery(PropertyCriteria criteria,String financialYear, List<Object> preparedStmtList) {
		
		if(Objects.isNull(financialYear))
			throw new CustomException("EG_PT_SEARCH_ERROR","Financial year is mandatory for search criteria");
		
		Long limit = config.getDefaultLimit();
		Long offset = config.getDefaultOffset();
		
		StringBuilder builder = new StringBuilder(ACTIVE_PROPERTY_ASSESMENT__FOR_CURRENT_FINYEAR_QUERY);
		
		preparedStmtList.add(financialYear);
		
		if(criteria.getTenantId()!=null)
		{
			builder.append(" WHERE property.tenantid=?");
			preparedStmtList.add(criteria.getTenantId());
		}
		
		builder.append(AND_QUERY);
		builder.append(" property.status IN ('ACTIVE')");
		
		builder.append(AND_QUERY);
		builder.append(" assessment.status IN ('ACTIVE')");
		
		builder.append(" order by property.propertyid");
		
		if (criteria.getOffset() != null) {
			builder.append(" offset ? ");
			offset = criteria.getOffset();
			preparedStmtList.add(offset);
		}
		
		if (criteria.getLimit() != null ) {
			builder.append(" limit ? ");
			limit = criteria.getLimit();
			preparedStmtList.add(limit);
		}
		
		return builder.toString();
	}

	/**
	 * 
	 * @param criteria
	 * @param preparedStmtList
	 * @return
	 */
	public String getPropertyCountQuery(PropertyCriteria criteria, List<Object> preparedStmtList) {

		Set<String> ownerIds = criteria.getOwnerIds();
		StringBuilder condition = new StringBuilder("(");
		condition.append(createQuery(ownerIds));
		addToPreparedStatement(preparedStmtList, ownerIds);
		condition.append(")");

		StringBuilder query = new StringBuilder(COUNT_OF_PROPERTY.replace(REPLACE_STRING, condition));

		if (criteria.getTenantId() != null) {
			query.append(AND_QUERY);
			query.append(" ept.tenantid = ?");
			preparedStmtList.add(criteria.getTenantId());
		}

		return query.toString();
	}
	
	/**
	 * 
	 * @param criteria
	 * @param preparedStmtList
	 * @return
	 */
	public String getCountOfActivePropertyByTenantId(PropertyCriteria criteria, List<Object> preparedStmtList) {
		StringBuilder builder = new StringBuilder(COUNT_NOOF_ACTIVE_PROPERTY_BY_TENATID);
		if(criteria.getTenantId()!=null)
		{
			builder.append(AND_QUERY);
			builder.append(" property.tenantid=?");
			preparedStmtList.add(criteria.getTenantId());
		}
		
		return builder.toString();
	}

	public String getCountOfActivePropertyByTenantIdNotHavingAnyAssessmentForCurrentFY(PropertyCriteria criteria,String financialYear,
			List<Object> preparedStmtList) {
		if(Objects.isNull(financialYear))
			throw new CustomException("EG_PT_SEARCH_ERROR","Financial year is mandatory for search criteria");

		StringBuilder builder = new StringBuilder(COUNT_NOOF_ACTIVE_PROPERTY_BY_TENATID_FOR_ASSESSMENT_JOB);
		String previuosFinancialYear = CommonUtils.getPreviousFinancialYear(financialYear);
		preparedStmtList.add(previuosFinancialYear);		
		preparedStmtList.add(financialYear);
		preparedStmtList.add(previuosFinancialYear);		

		if(criteria.getTenantId()!=null)
		{
			builder.append(AND_QUERY);
			builder.append(" epp.tenantid=?");
			preparedStmtList.add(criteria.getTenantId());
		}
		
		return builder.toString();
	}

	public String getPropertiesHavingCurrentFYAssessment(PropertyCriteria criteria, String financialYear,
			List<Object> preparedStmtList) {
		if(Objects.isNull(financialYear))
			throw new CustomException("EG_PT_SEARCH_ERROR","Financial year is mandatory for search criteria");
		
		StringBuilder builder = new StringBuilder(COUNT_NOOF_DISTINCT_PROPERTY_BY_TENATID_HAVING_ASMT_IN_CURRENTFY);
		preparedStmtList.add(financialYear);
		
		if(criteria.getTenantId()!=null)
		{
			builder.append(AND_QUERY);
			builder.append(" assessment.tenantid=?");
			preparedStmtList.add(criteria.getTenantId());
		}
		
		return builder.toString();
	}

	public String getExistingActiveOwners(PropertyCriteria criteria, List<Object> preparedStmtList) {
		// TODO Auto-generated method stub
		StringBuilder builder = new StringBuilder("select ownerinfouuid from eg_pt_owner where ");

		builder.append("ownerinfouuid in  (").append(createQuery(criteria.getOwnerIds())).append(") and status ='ACTIVE' ");
		addToPreparedStatement(preparedStmtList, criteria.getOwnerIds());

		return builder.toString();
	}

}
