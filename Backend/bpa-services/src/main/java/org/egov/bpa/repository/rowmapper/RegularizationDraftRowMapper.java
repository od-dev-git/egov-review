package org.egov.bpa.repository.rowmapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.egov.bpa.web.model.AuditDetails;
import org.egov.bpa.web.model.RegularizationDraft;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;

@Component
public class RegularizationDraftRowMapper implements ResultSetExtractor<List<RegularizationDraft>> {
	
	@Autowired
	private ObjectMapper mapper;
	
	@SuppressWarnings("rawtypes")
	@Override
	public List<RegularizationDraft> extractData(ResultSet rs) throws SQLException, DataAccessException {

		Map<String, RegularizationDraft> draftMap = new LinkedHashMap<String, RegularizationDraft>();

		while (rs.next()) {
			String id = rs.getString("id");
			String tenantId = rs.getString("tenantid");

			RegularizationDraft currentDraft = draftMap.get(id);
			if (currentDraft == null) {
				Long lastModifiedTime = rs.getLong("lastmodifiedtime");
				if (rs.wasNull()) {
					lastModifiedTime = null;
				}

				Object additionalDetails = new Gson().fromJson(rs.getString("additionaldetails").equals("{}")
						|| rs.getString("additionaldetails").equals("null") ? null : rs.getString("additionaldetails"),
						Object.class);

				AuditDetails auditdetails = AuditDetails.builder().createdBy(rs.getString("createdby"))
						.createdTime(rs.getLong("createdtime")).lastModifiedBy(rs.getString("lastmodifiedby"))
						.lastModifiedTime(lastModifiedTime).build();

				currentDraft = RegularizationDraft.builder().id(id).tenantId(tenantId)
						.draftNo(rs.getString("draftno")).auditDetails(auditdetails).status(rs.getString("status"))
						.additionalDetails(additionalDetails).build();

				draftMap.put(id, currentDraft);
			}

		}

		return new ArrayList<>(draftMap.values());

	}


}
