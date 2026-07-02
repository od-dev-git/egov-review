package org.egov.bpa.calculator.repository.querybuilder;

import java.util.List;

import org.egov.bpa.calculator.web.models.InstallmentSearchCriteria;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

@Component
public class InstallmentQueryBuilder {

	private static final String LEFT_OUTER_JOIN_STRING = " LEFT OUTER JOIN ";

	private static final String QUERY = "SELECT ebi.id as ebi_id,ebi.tenantid as ebi_tenantid,ebi.installmentno as ebi_installmentno,ebi.status as ebi_status,ebi.consumercode as ebi_consumercode,ebi.taxheadcode as ebi_taxheadcode,ebi.taxamount as ebi_taxamount,ebi.demandid as ebi_demandid,ebi.ispaymentcompletedindemand as ebi_ispaymentcompletedindemand,ebi.additional_details as ebi_additional_details,ebi.createdby as ebi_createdby,ebi.lastmodifiedby as ebi_lastmodifiedby,ebi.createdtime as ebi_createdtime,ebi.lastmodifiedtime as ebi_lastmodifiedtime "
			+ "FROM eg_bpa_installment ebi ";

	private final String paginationWrapper = "SELECT * FROM "
			+ "(SELECT *, DENSE_RANK() OVER (ORDER BY ebi_lastModifiedTime DESC) offset_ FROM " + "({})"
			+ " result) result_offset " + "WHERE offset_ > ? AND offset_ <= ?";

	private final String countWrapper = "SELECT COUNT(DISTINCT(ebi_id)) FROM ({INTERNAL_QUERY}) as ebi_count";

	/**
	 *
	 */
	public String getInstallmentSearchQuery(InstallmentSearchCriteria criteria, List<Object> preparedStmtList) {

		StringBuilder builder = new StringBuilder(QUERY);

		if (criteria.getTenantId() != null) {
			if (criteria.getTenantId().split("\\.").length == 1) {

				addClauseIfRequired(preparedStmtList, builder);
				builder.append(" ebi.tenantid like ?");
				preparedStmtList.add('%' + criteria.getTenantId() + '%');
			} else {
				addClauseIfRequired(preparedStmtList, builder);
				builder.append(" ebi.tenantid=? ");
				preparedStmtList.add(criteria.getTenantId());
			}
		}

		if (criteria.getInstallmentNos() != null) {
			addClauseIfRequired(preparedStmtList, builder);
			builder.append(" ebi.installmentno IN (").append(createQueryInInteger(criteria.getInstallmentNos()))
					.append(")");
			addToPreparedStatementIntegervalues(preparedStmtList, criteria.getInstallmentNos());
		}
		if (criteria.getStatus() != null) {
			addClauseIfRequired(preparedStmtList, builder);
			builder.append(" ebi.status=? ");
			preparedStmtList.add(criteria.getStatus());
		}
		if (criteria.getConsumerCode() != null) {
			addClauseIfRequired(preparedStmtList, builder);
			builder.append(" ebi.consumercode=? ");
			preparedStmtList.add(criteria.getConsumerCode());
		}
		if (criteria.getTaxHeadCode() != null) {
			addClauseIfRequired(preparedStmtList, builder);
			builder.append(" ebi.taxheadcode=? ");
			preparedStmtList.add(criteria.getTaxHeadCode());
		}
		if (criteria.getDemandId() != null) {
			addClauseIfRequired(preparedStmtList, builder);
			builder.append(" ebi.demandid=? ");
			preparedStmtList.add(criteria.getDemandId());
		}
		if (criteria.getIsPaymentCompletedInDemand() != null) {
			addClauseIfRequired(preparedStmtList, builder);
			builder.append(" ebi.ispaymentcompletedindemand=? ");
			preparedStmtList.add(criteria.getIsPaymentCompletedInDemand());
		}
		if (criteria.getCreatedBy() != null) {
			addClauseIfRequired(preparedStmtList, builder);
			builder.append(" ebi.createdby=? ");
			preparedStmtList.add(criteria.getCreatedBy());
		}

		List<String> ids = criteria.getIds();
		if (!CollectionUtils.isEmpty(ids)) {
			addClauseIfRequired(preparedStmtList, builder);
			builder.append(" ebi.id IN (").append(createQuery(ids)).append(")");
			addToPreparedStatement(preparedStmtList, ids);
		}

		if (criteria.getFromDate() != null && criteria.getToDate() != null) {
			addClauseIfRequired(preparedStmtList, builder);
			builder.append(" ebi.createdtime BETWEEN ").append(criteria.getFromDate()).append(" AND ")
					.append(criteria.getToDate());
		} else if (criteria.getFromDate() != null && criteria.getToDate() == null) {
			addClauseIfRequired(preparedStmtList, builder);
			builder.append(" ebi.createdtime >= ").append(criteria.getFromDate());
		}
		//sort by installmentno ascending-
		builder.append(" ORDER BY ebi_installmentno ASC");
		return builder.toString();
		//return addPaginationWrapper(builder.toString(), preparedStmtList, criteria);

	}

	/**
	 * 
	 * @param query            prepared Query
	 * @param preparedStmtList values to be replased on the query
	 * @param criteria         bpa search criteria
	 * @return the query by replacing the placeholders with preparedStmtList
	 */
	private String addPaginationWrapper(String query, List<Object> preparedStmtList,
			InstallmentSearchCriteria criteria) {

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
	 * add integer values to the preparedStatment List
	 * 
	 * @param preparedStmtList
	 * @param values
	 */
	private void addToPreparedStatementIntegervalues(List<Object> preparedStmtList, List<Integer> values) {
		values.forEach(value -> {
			preparedStmtList.add(value);
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
	
	/**
	 * produce a query input for the multiple values of integer column
	 * 
	 * @param values
	 * @return
	 */
	private Object createQueryInInteger(List<Integer> values) {
		StringBuilder builder = new StringBuilder();
		int length = values.size();
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
