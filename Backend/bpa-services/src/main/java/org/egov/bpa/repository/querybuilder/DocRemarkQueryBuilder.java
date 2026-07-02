package org.egov.bpa.repository.querybuilder;

import java.util.List;

import javax.validation.Valid;

import org.egov.bpa.config.BPAConfiguration;
import org.egov.bpa.web.model.DocRemarkSearchCriteria;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

@Component
public class DocRemarkQueryBuilder {

	@Autowired
	private BPAConfiguration config;

	private static final String QUERY = "select * from eg_bpa_document_remark";

	private final String paginationWrapper = "SELECT * FROM "
			+ "(SELECT *, DENSE_RANK() OVER (ORDER BY lastModifiedTime DESC) offset_ FROM " + "({})"
			+ " result) result_offset " + "WHERE offset_ > ? AND offset_ <= ?";

	public String docRemarkSearchQuery(@Valid DocRemarkSearchCriteria criteria, List<Object> preparedStmtList) {
        StringBuilder builder = new StringBuilder(QUERY);
        List<String> ids = criteria.getIds();
        List<String> documentCodes = criteria.getDocumentCode();

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
        if (criteria.getBusinessId() != null) {
            addClauseIfRequired(preparedStmtList, builder);
            builder.append(" businessid=?");
            preparedStmtList.add(criteria.getBusinessId());
        }

        if (criteria.getFromDate() != null && criteria.getToDate() != null) {
            addClauseIfRequired(preparedStmtList, builder);
            builder.append(" createdtime BETWEEN ").append(criteria.getFromDate()).append(" AND ")
                    .append(criteria.getToDate());
        } else if (criteria.getFromDate() != null && criteria.getToDate() == null) {
            addClauseIfRequired(preparedStmtList, builder);
            builder.append(" createdtime >= ").append(criteria.getFromDate());
        }
        builder.append("  order by createdtime desc");
        return addPaginationWrapper(builder.toString(), preparedStmtList, criteria);

    }

	private void addToPreparedStatement(List<Object> preparedStmtList, List<String> ids) {
		ids.forEach(id -> {
			preparedStmtList.add(id);
		});

	}

	private Object createQuery(List<String> ids) {
		StringBuilder builder = new StringBuilder();
		int length = ids.size();
		System.out.println("ids" + length);
		for (int i = 0; i < length; i++) {
			builder.append(" ?");
			if (i != length - 1)
				builder.append(",");
		}
		return builder.toString();
	}

	private void addClauseIfRequired(List<Object> values, StringBuilder builder) {
		if (values.isEmpty())
			builder.append(" WHERE ");
		else {
			builder.append(" AND ");
		}

	}

	private String addPaginationWrapper(String query, List<Object> preparedStmtList, DocRemarkSearchCriteria criteria) {

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

}
