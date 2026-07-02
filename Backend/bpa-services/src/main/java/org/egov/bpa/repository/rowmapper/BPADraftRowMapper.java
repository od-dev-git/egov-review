package org.egov.bpa.repository.rowmapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.egov.bpa.web.model.AuditDetails;
import org.egov.bpa.web.model.BPA;
import org.egov.bpa.web.model.BPADraft;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;

@Component
public class BPADraftRowMapper implements ResultSetExtractor<List<BPADraft>>{
	
	@Autowired
	private ObjectMapper mapper;
	
	@SuppressWarnings("rawtypes")
	@Override
	public List<BPADraft> extractData(ResultSet rs) throws SQLException, DataAccessException {

		Map<String, BPADraft> draftMap = new LinkedHashMap<String, BPADraft>();

		while (rs.next()) {
			String id = rs.getString("id");
			String tenantId = rs.getString("tenantid");

			BPADraft currentDraft = draftMap.get(id);
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

				currentDraft = BPADraft.builder().id(id).tenantId(tenantId)
						.edcrNo(rs.getString("edcrno")).auditDetails(auditdetails).status(rs.getString("status"))
						.additionalDetails(additionalDetails).build();

				draftMap.put(id, currentDraft);
			}

		}

		return new ArrayList<>(draftMap.values());

	}

}
