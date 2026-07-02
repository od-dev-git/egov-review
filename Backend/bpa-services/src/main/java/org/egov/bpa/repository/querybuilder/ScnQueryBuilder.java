package org.egov.bpa.repository.querybuilder;

import java.util.List;

import javax.validation.Valid;

import org.egov.bpa.config.BPAConfiguration;
import org.egov.bpa.web.model.NoticeSearchCriteria;
import org.egov.bpa.web.model.PreapprovedPlanSearchCriteria;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;


@Component
public class ScnQueryBuilder {
	
	@Autowired
	private BPAConfiguration config;
	
	private static final String QUERY = "select notice.id,notice.businessid as businessid,notice.letter_number as letterNumber,notice.filestoreid as filestoreid, "
			+ "notice.letter_type as letterType,notice.tenantid as tenantid,notice.notice_reminder_count,notice.createdby,notice.lastmodifiedby, "
			+ "notice.createdtime,notice.lastmodifiedtime,notice.isClosed,notice.additionalDetails "
			+ "from "
			+ "eg_bpa_notice notice "
			+ "inner join eg_bpa_buildingplan bpa on bpa.applicationno = notice.businessid ";
	
	private static final String QUERY_REGULARIZATION = "select notice.id,notice.businessid as businessid,notice.letter_number as letterNumber,notice.filestoreid as filestoreid, "
			+ "notice.letter_type as letterType,notice.tenantid as tenantid,notice.notice_reminder_count,notice.createdby,notice.lastmodifiedby, "
			+ "notice.createdtime,notice.lastmodifiedtime,notice.isClosed,notice.additionalDetails "
			+ "from "
			+ "eg_bpa_regularization_notice notice "
			+ "inner join eg_bpa_regularization_application bpa on bpa.applicationno = notice.businessid ";
	
	private final String paginationWrapper = "SELECT * FROM "
			+ "(SELECT *, DENSE_RANK() OVER (ORDER BY lastModifiedTime DESC) offset_ FROM " + "({})"
			+ " result) result_offset " + "WHERE offset_ > ? AND offset_ <= ?";

	public String getNoticeSearchQuery( @Valid NoticeSearchCriteria SearchCriteria, List<Object> preparedStmtList) {
		StringBuilder builder = new StringBuilder(QUERY);
		List<String> ids = SearchCriteria.getIds();
		//System.out.println(ids);
		if (!CollectionUtils.isEmpty(ids)) {
			addClauseIfRequired(preparedStmtList, builder);
			builder.append(" notice.id IN (").append(createQuery(ids)).append(")");
			addToPreparedStatement(preparedStmtList, ids);
		
	}
		if(SearchCriteria.getBusinessid()!=null) {
			addClauseIfRequired(preparedStmtList, builder);
			builder.append(" notice.businessid=?");
			preparedStmtList.add(SearchCriteria.getBusinessid());
		}
		if(SearchCriteria.getLetterNo()!=null) {
			addClauseIfRequired(preparedStmtList, builder);
			builder.append(" notice.letter_number=?");
			preparedStmtList.add(SearchCriteria.getLetterNo());
		}
		if(SearchCriteria.getFilestoreid()!=null) {
			addClauseIfRequired(preparedStmtList, builder);
			builder.append(" notice.filestoreid=?");
			preparedStmtList.add(SearchCriteria.getFilestoreid());
		}
		if(SearchCriteria.getTenantid()!=null) {
			addClauseIfRequired(preparedStmtList, builder);
			builder.append(" notice.tenantid=?");
			preparedStmtList.add(SearchCriteria.getTenantid());
		}
		
		if(SearchCriteria.getLetterType()!=null) {
			addClauseIfRequired(preparedStmtList, builder);
			builder.append(" notice.letter_type=?");
			preparedStmtList.add(SearchCriteria.getLetterType());
		}
		if(SearchCriteria.getIsClosed() != null ) {
			addClauseIfRequired(preparedStmtList, builder);
			builder.append(" notice.isClosed=?");
			preparedStmtList.add(SearchCriteria.getIsClosed());
		}
		
		if(SearchCriteria.getCreatedBy() != null ) {
			addClauseIfRequired(preparedStmtList, builder);
			builder.append(" notice.createdby=?");
			preparedStmtList.add(SearchCriteria.getCreatedBy());
		}
		
		if (!CollectionUtils.isEmpty(SearchCriteria.getBpaStatus())) {
			addClauseIfRequired(preparedStmtList, builder);
			builder.append(" bpa.status IN (").append(createQuery(SearchCriteria.getBpaStatus())).append(")");
			addToPreparedStatement(preparedStmtList, SearchCriteria.getBpaStatus());
		}
		
		if (SearchCriteria.getFromDate() != null && SearchCriteria.getToDate() != null) {
			addClauseIfRequired(preparedStmtList, builder);
			builder.append(" notice.createdtime BETWEEN ").append(SearchCriteria.getFromDate()).append(" AND ")
					.append(SearchCriteria.getToDate());
		} else if (SearchCriteria.getFromDate() != null && SearchCriteria.getToDate() == null) {
			addClauseIfRequired(preparedStmtList, builder);
			builder.append(" notice.createdtime >= ").append(SearchCriteria.getFromDate());
		}
		builder.append(" order by notice.createdtime desc");
		return addPaginationWrapper(builder.toString(), preparedStmtList, SearchCriteria);

	}

	private void addToPreparedStatement(List<Object> preparedStmtList, List<String> ids) {
		ids.forEach(id -> {
			preparedStmtList.add(id);
		});

	}

	private Object createQuery(List<String> ids) {
		StringBuilder builder = new StringBuilder();
		int length = ids.size();
		System.out.println("ids"+length);
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
			builder.append(" AND");
		}
		
	}
	private String addPaginationWrapper(String query, List<Object> preparedStmtList,
			NoticeSearchCriteria criteria) {

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
	
	public String getNoticeSearchQueryForRegularization(@Valid NoticeSearchCriteria SearchCriteria,
			List<Object> preparedStmtList) {

		StringBuilder builder = new StringBuilder(QUERY_REGULARIZATION);
		List<String> ids = SearchCriteria.getIds();
		// System.out.println(ids);
		if (!CollectionUtils.isEmpty(ids)) {
			addClauseIfRequired(preparedStmtList, builder);
			builder.append(" notice.id IN (").append(createQuery(ids)).append(")");
			addToPreparedStatement(preparedStmtList, ids);

		}
		if (SearchCriteria.getBusinessid() != null) {
			addClauseIfRequired(preparedStmtList, builder);
			builder.append(" notice.businessid=?");
			preparedStmtList.add(SearchCriteria.getBusinessid());
		}
		if (SearchCriteria.getLetterNo() != null) {
			addClauseIfRequired(preparedStmtList, builder);
			builder.append(" notice.letter_number=?");
			preparedStmtList.add(SearchCriteria.getLetterNo());
		}
		if (SearchCriteria.getFilestoreid() != null) {
			addClauseIfRequired(preparedStmtList, builder);
			builder.append(" notice.filestoreid=?");
			preparedStmtList.add(SearchCriteria.getFilestoreid());
		}
		if (SearchCriteria.getTenantid() != null) {
			addClauseIfRequired(preparedStmtList, builder);
			builder.append(" notice.tenantid=?");
			preparedStmtList.add(SearchCriteria.getTenantid());
		}

		if (SearchCriteria.getLetterType() != null) {
			addClauseIfRequired(preparedStmtList, builder);
			builder.append(" notice.letter_type=?");
			preparedStmtList.add(SearchCriteria.getLetterType());
		}
		if (SearchCriteria.getIsClosed() != null) {
			addClauseIfRequired(preparedStmtList, builder);
			builder.append(" notice.isClosed=?");
			preparedStmtList.add(SearchCriteria.getIsClosed());
		}

		if (SearchCriteria.getCreatedBy() != null) {
			addClauseIfRequired(preparedStmtList, builder);
			builder.append(" notice.createdby=?");
			preparedStmtList.add(SearchCriteria.getCreatedBy());
		}

		if (!CollectionUtils.isEmpty(SearchCriteria.getBpaStatus())) {
			addClauseIfRequired(preparedStmtList, builder);
			builder.append(" bpa.status IN (").append(createQuery(SearchCriteria.getBpaStatus())).append(")");
			addToPreparedStatement(preparedStmtList, SearchCriteria.getBpaStatus());
		}

		if (SearchCriteria.getFromDate() != null && SearchCriteria.getToDate() != null) {
			addClauseIfRequired(preparedStmtList, builder);
			builder.append(" notice.createdtime BETWEEN ").append(SearchCriteria.getFromDate()).append(" AND ")
					.append(SearchCriteria.getToDate());
		} else if (SearchCriteria.getFromDate() != null && SearchCriteria.getToDate() == null) {
			addClauseIfRequired(preparedStmtList, builder);
			builder.append(" notice.createdtime >= ").append(SearchCriteria.getFromDate());
		}
		builder.append(" order by notice.createdtime desc");
		return addPaginationWrapper(builder.toString(), preparedStmtList, SearchCriteria);
	}

}
