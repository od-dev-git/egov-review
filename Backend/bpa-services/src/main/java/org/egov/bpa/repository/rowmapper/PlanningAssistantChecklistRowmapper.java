package org.egov.bpa.repository.rowmapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.egov.bpa.web.model.AuditDetails;
import org.egov.bpa.web.model.PlanningAssistantChecklist;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.stereotype.Component;

import com.google.gson.Gson;

@Component
public class PlanningAssistantChecklistRowmapper implements ResultSetExtractor<List<PlanningAssistantChecklist>> {

	@Override
	public List<PlanningAssistantChecklist> extractData(ResultSet rs) throws SQLException, DataAccessException {
		// TODO Auto-generated method stub

		Map<String, PlanningAssistantChecklist> fieldMap = new LinkedHashMap<String, PlanningAssistantChecklist>();

		while (rs.next()) {
			String id = rs.getString("id");
			PlanningAssistantChecklist planningAssistantChecklist = fieldMap.get(id);
			if (planningAssistantChecklist == null) {
				Long lastModifiedTime = rs.getLong("lastmodifiedtime");
				if (rs.wasNull())
					lastModifiedTime = null;

				AuditDetails auditdetails = AuditDetails.builder().createdBy(rs.getString("createdby"))
						.createdTime(rs.getLong("createdtime")).lastModifiedBy(rs.getString("lastmodifiedby"))
						.lastModifiedTime(lastModifiedTime).build();

				Optional<String> documentSubmittedStr = Optional.ofNullable(rs.getString("documents_submitted"));
				Object documentSubmitted = documentSubmittedStr.filter(s -> !s.isEmpty() && !s.equals("null"))
						.map(doc -> new Gson().fromJson(doc, Object.class)).orElse(null);

				Optional<String> planSubmittedStr = Optional.ofNullable(rs.getString("plans_submitted"));
				Object planSubmitted = planSubmittedStr.filter(s -> !s.isEmpty() && !s.equals("null"))
						.map(plan -> new Gson().fromJson(plan, Object.class)).orElse(null);

				Optional<String> nocSubmittedStr = Optional.ofNullable(rs.getString("nocs_submitted"));
				Object nocSubmitted = nocSubmittedStr.filter(s -> !s.isEmpty() && !s.equals("null"))
						.map(noc -> new Gson().fromJson(noc, Object.class)).orElse(null);

				Optional<String> builtupAreaStr = Optional.ofNullable(rs.getString("builtup_area"));
				Object builtupArea = builtupAreaStr.filter(s -> !s.isEmpty() && !s.equals("null"))
						.map(area -> new Gson().fromJson(area, Object.class)).orElse(null);

				Optional<String> setbackDetailsStr = Optional.ofNullable(rs.getString("setback_details"));
				Object setbackDetails = setbackDetailsStr.filter(s -> !s.isEmpty() && !s.equals("null"))
						.map(setBack -> new Gson().fromJson(setBack, Object.class)).orElse(null);

				Optional<String> additionalDetailsStr = Optional.ofNullable(rs.getString("additionaldetails"));
				Object additionalDetails = additionalDetailsStr.filter(s -> !s.isEmpty() && !s.equals("null"))
						.map(add -> new Gson().fromJson(add, Object.class)).orElse(null);

				planningAssistantChecklist = PlanningAssistantChecklist.builder()
						.applicationno(rs.getString("applicationno")).id(id).tenantId(rs.getString("tenantid"))
						.documentsSubmitted(documentSubmitted).plansSubmitted(planSubmitted).nocsSubmitted(nocSubmitted)
						.builtupArea(builtupArea).setbackDetails(setbackDetails).additionalDetails(additionalDetails)
						.auditDetails(auditdetails).build();

				fieldMap.put(id, planningAssistantChecklist);

			}

		}
		return new ArrayList<>(fieldMap.values());
	}
}
