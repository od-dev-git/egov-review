package org.egov.bpa.calculator.repository.querybuilder;

import java.util.List;

import org.egov.bpa.calculator.config.BPACalculatorConfig;
import org.egov.bpa.calculator.web.models.RevisionSearchCriteria;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

@Component
public class RevisionQueryBuilder {

	@Autowired
	private BPACalculatorConfig config;

	private static final String LEFT_OUTER_JOIN_STRING = " LEFT OUTER JOIN ";

	private static final String QUERY = "SELECT ebpr.id as ebpr_id,ebpr.isSujogExistingApplication as ebpr_isSujogExistingApplication,ebpr.tenantid as ebpr_tenantId,ebpr.bpaApplicationNo as ebpr_bpaApplicationNo,ebpr.bpaApplicationId as ebpr_bpaApplicationId,ebpr.refBpaApplicationNo as ebpr_refBpaApplicationNo,ebpr.refPermitNo as ebpr_refPermitNo,ebpr.refPermitDate as ebpr_refPermitDate,ebpr.refPermitExpiryDate as ebpr_refPermitExpiryDate,ebpr.refApplicationDetails as ebpr_refApplicationDetails,ebpr.createdby as ebpr_createdBy,ebpr.lastmodifiedby as ebpr_lastModifiedBy,ebpr.createdtime as ebpr_createdTime,ebpr.lastmodifiedtime as ebpr_lastModifiedTime"
			+ ", ebprd.id as ebprd_id, ebprd.documenttype as ebprd_documenttype, ebprd.filestoreid as ebprd_filestoreid, ebprd.documentuid as ebprd_documentuid, ebprd.revisionId as ebprd_revisionId, ebprd.additionaldetails as ebprd_additionaldetails, ebprd.createdby as ebprd_createdby, ebprd.lastmodifiedby as ebprd_lastmodifiedby, ebprd.createdtime as ebprd_createdtime, ebprd.lastmodifiedtime as ebprd_lastmodifiedtime "
			+ " FROM eg_bpa_buildingplan_revision ebpr "
			+  LEFT_OUTER_JOIN_STRING
			+ " eg_bpa_revision_documents ebprd on ebprd.revisionId=ebpr.id";

	private final String paginationWrapper = "SELECT * FROM "
			+ "(SELECT *, DENSE_RANK() OVER (ORDER BY ebpr_lastModifiedTime DESC) offset_ FROM " + "({})"
			+ " result) result_offset " + "WHERE offset_ > ? AND offset_ <= ?";

	private final String countWrapper = "SELECT COUNT(DISTINCT(bpa_id)) FROM ({INTERNAL_QUERY}) as bpa_count";

	/**
	 *
	 */
	public String getRevisionSearchQuery(RevisionSearchCriteria criteria, List<Object> preparedStmtList) {

		StringBuilder builder = new StringBuilder(QUERY);

		if (criteria.getTenantId() != null) {
			if (criteria.getTenantId().split("\\.").length == 1) {

				addClauseIfRequired(preparedStmtList, builder);
				builder.append(" ebpr.tenantid like ?");
				preparedStmtList.add('%' + criteria.getTenantId() + '%');
			} else {
				addClauseIfRequired(preparedStmtList, builder);
				builder.append(" ebpr.tenantid=? ");
				preparedStmtList.add(criteria.getTenantId());
			}
		}

		if (Boolean.TRUE.equals(criteria.isSujogExistingApplication())) {
			addClauseIfRequired(preparedStmtList, builder);
			builder.append(" ebpr.isSujogExistingApplication=? ");
			preparedStmtList.add(criteria.isSujogExistingApplication());
		}
		if (criteria.getBpaApplicationNo() != null) {
			addClauseIfRequired(preparedStmtList, builder);
			builder.append(" ebpr.bpaApplicationNo=? ");
			preparedStmtList.add(criteria.getBpaApplicationNo());
		}
		if (criteria.getBpaApplicationId() != null) {
			addClauseIfRequired(preparedStmtList, builder);
			builder.append(" ebpr.bpaApplicationId=? ");
			preparedStmtList.add(criteria.getBpaApplicationId());
		}
		if (criteria.getRefBpaApplicationNo() != null) {
			addClauseIfRequired(preparedStmtList, builder);
			builder.append(" ebpr.refBpaApplicationNo=? ");
			preparedStmtList.add(criteria.getRefBpaApplicationNo());
		}
		if (criteria.getRefPermitNo() != null) {
			addClauseIfRequired(preparedStmtList, builder);
			builder.append(" ebpr.refPermitNo=? ");
			preparedStmtList.add(criteria.getRefPermitNo());
		}
		if (criteria.getRefPermitDate() != null) {
			addClauseIfRequired(preparedStmtList, builder);
			builder.append(" ebpr.refPermitDate=? ");
			preparedStmtList.add(criteria.getRefPermitDate());
		}
		if (criteria.getRefPermitExpiryDate() != null) {
			addClauseIfRequired(preparedStmtList, builder);
			builder.append(" ebpr.refPermitExpiryDate=? ");
			preparedStmtList.add(criteria.getRefPermitExpiryDate());
		}

		List<String> ids = criteria.getIds();
		if (!CollectionUtils.isEmpty(ids)) {
			addClauseIfRequired(preparedStmtList, builder);
			builder.append(" ebpr.id IN (").append(createQuery(ids)).append(")");
			addToPreparedStatement(preparedStmtList, ids);
		}

		if (criteria.getFromDate() != null && criteria.getToDate() != null) {
			addClauseIfRequired(preparedStmtList, builder);
			builder.append(" ebpr.createdtime BETWEEN ").append(criteria.getFromDate()).append(" AND ")
					.append(criteria.getToDate());
		} else if (criteria.getFromDate() != null && criteria.getToDate() == null) {
			addClauseIfRequired(preparedStmtList, builder);
			builder.append(" ebpr.createdtime >= ").append(criteria.getFromDate());
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
	private String addPaginationWrapper(String query, List<Object> preparedStmtList,
			RevisionSearchCriteria criteria) {

		int limit = 10;
		int offset = 0;
		int maxLimit = 1000;
		String finalQuery = paginationWrapper.replace("{}", query);

		if (criteria.getLimit() == null && criteria.getOffset() == null) {
			limit = maxLimit;
		}

		if (criteria.getLimit() != null && criteria.getLimit() <= maxLimit)
			limit = criteria.getLimit();

		if (criteria.getLimit() != null && criteria.getLimit() > maxLimit) {
			limit = maxLimit;
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

	private String addCountWrapper(String query) {
		return countWrapper.replace("{INTERNAL_QUERY}", query);
	}

}
