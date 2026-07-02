package org.egov.bpa.repository.querybuilder;

import java.util.List;
import java.util.Objects;

import org.egov.bpa.config.BPAConfiguration;
import org.egov.bpa.web.model.accreditedperson.AccreditedPersonSearchCriteria;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

@Component
public class AccreditedPersonQueryBuilder {

	@Autowired
	private BPAConfiguration config;

	private static final String LEFT_OUTER_JOIN_STRING = " LEFT OUTER JOIN ";

	private static final String QUERY = "select ebpa.id as ebpa_id, ebpa.user_uuid as ebpa_user_uuid, ebpa.user_id as ebpa_user_id, ebpa.person_name as ebpa_person_name, ebpa.firm_name as ebpa_firm_name,ebpa.accreditation_no as ebpa_accreditation_no, ebpa.certificate_issue_date as ebpa_certificate_issue_date, ebpa.valid_till as ebpa_valid_till, ebpa.createdby as ebpa_createdby, ebpa.lastmodifiedby as ebpa_lastmodifiedby, ebpa.createdtime as ebpa_createdtime, ebpa.lastmodifiedtime as ebpa_lastmodifiedtime"
			+ " FROM eg_bpa_accredited_person ebpa ";

	private final String paginationWrapper = "SELECT * FROM "
			+ "(SELECT *, DENSE_RANK() OVER (ORDER BY ebpa_lastModifiedTime DESC) offset_ FROM " + "({})"
			+ " result) result_offset " + "WHERE offset_ > ? AND offset_ <= ?";

	private final String countWrapper = "SELECT COUNT(DISTINCT(ebpa_id)) FROM ({INTERNAL_QUERY}) as ebp_count";

	/**
	 *
	 */
	public String getAccreditedPersonSearchQuery(AccreditedPersonSearchCriteria criteria, List<Object> preparedStmtList) {

		StringBuilder builder = new StringBuilder(QUERY);
		if (Objects.isNull(criteria))
			criteria = new AccreditedPersonSearchCriteria();

		if (criteria.getUserUUID() != null) {
			addClauseIfRequired(preparedStmtList, builder);
			builder.append(" ebpa.user_uuid=? ");
			preparedStmtList.add(criteria.getUserUUID());
		}
		if (criteria.getUserId() != null) {
			addClauseIfRequired(preparedStmtList, builder);
			builder.append(" ebpa.user_id=? ");
			preparedStmtList.add(criteria.getUserId());
		}
		if (criteria.getPersonName() != null) {
			addClauseIfRequired(preparedStmtList, builder);
			builder.append(" ebpa.person_name=? ");
			preparedStmtList.add(criteria.getPersonName());
		}
		if (criteria.getFirmName() != null) {
			addClauseIfRequired(preparedStmtList, builder);
			builder.append(" ebpa.firm_name=? ");
			preparedStmtList.add(criteria.getFirmName());
		}
		if (criteria.getAccreditationNo() != null) {
			addClauseIfRequired(preparedStmtList, builder);
			builder.append(" ebpa.accreditation_no=? ");
			preparedStmtList.add(criteria.getAccreditationNo());
		}
		if (criteria.getCertificateIssueDate() != null) {
			addClauseIfRequired(preparedStmtList, builder);
			builder.append(" ebpa.certificate_issue_date=? ");
			preparedStmtList.add(criteria.getCertificateIssueDate());
		}

		List<String> ids = criteria.getIds();
		if (!CollectionUtils.isEmpty(ids)) {
			addClauseIfRequired(preparedStmtList, builder);
			builder.append(" ebpa.id IN (").append(createQuery(ids)).append(")");
			addToPreparedStatement(preparedStmtList, ids);
		}

		if (criteria.getFromDate() != null && criteria.getToDate() != null) {
			addClauseIfRequired(preparedStmtList, builder);
			builder.append(" ebpa.createdtime BETWEEN ").append(criteria.getFromDate()).append(" AND ")
					.append(criteria.getToDate());
		} else if (criteria.getFromDate() != null && criteria.getToDate() == null) {
			addClauseIfRequired(preparedStmtList, builder);
			builder.append(" ebpa.createdtime >= ").append(criteria.getFromDate());
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
			AccreditedPersonSearchCriteria criteria) {

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
