package org.egov.bpa.repository.rowmapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.egov.bpa.web.model.AuditDetails;
import org.egov.bpa.web.model.demolition.DemolitionApprovedByApplicationSearch;
import org.egov.tracer.model.CustomException;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.stereotype.Component;

import com.google.gson.Gson;

@Component
public class DemolitionApprovedByMeRowMapper
		implements ResultSetExtractor<List<DemolitionApprovedByApplicationSearch>> {

	@Override
	public List<DemolitionApprovedByApplicationSearch> extractData(ResultSet rs)
			throws SQLException, DataAccessException {

		Map<String, DemolitionApprovedByApplicationSearch> approvalMap = new LinkedHashMap<String, DemolitionApprovedByApplicationSearch>();
		while (rs.next()) {
			String id = rs.getString("id");
			DemolitionApprovedByApplicationSearch bpas = approvalMap.get(id);
			Object demolitionAdditionalDetails = null;

			if (bpas == null) {

				Long lastModifiedTime = rs.getLong("lastModifiedTime");
				if (rs.wasNull()) {
					lastModifiedTime = null;
				}

				AuditDetails auditdetails = AuditDetails.builder().createdBy(rs.getString("createdBy"))
						.createdTime(rs.getLong("createdTime")).lastModifiedBy(rs.getString("lastModifiedBy"))
						.lastModifiedTime(lastModifiedTime).build();

				try {

					demolitionAdditionalDetails = new Gson()
							.fromJson(rs.getString("demolition_additionaldetails").equals("{}")
									|| rs.getString("demolition_additionaldetails").equals("null") ? null
											: rs.getString("demolition_additionaldetails"),
									Object.class);

				} catch (Exception e) {
					throw new CustomException("PARSING ERROR", "The demolitionAdditionalDetails json cannot be parsed");
				}

				bpas = DemolitionApprovedByApplicationSearch.builder()
						.applicationstatus(rs.getString("applicationstatus"))
						.workflowstate(rs.getString("workflowstate"))
						.demolitionAdditionalDetails(demolitionAdditionalDetails)
						.tenantId(rs.getString("tenantid"))
						.demolitionId(rs.getString("demolition_id")).applicationType(rs.getString("applicationtype"))
						.applicationNo(rs.getString("applicationno"))
						.build();

				approvalMap.put(id, bpas);
			}
			// addChildrenToProperty(rs, bpas);
		}
		return new ArrayList<>(approvalMap.values());

	}

}
