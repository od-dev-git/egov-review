package org.egov.bpa.repository.rowmapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.egov.bpa.web.model.AuditDetails;
import org.egov.bpa.web.model.regularization.RegularizationFieldInspection;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.stereotype.Component;

import com.google.gson.Gson;

@Component
public class RegularizationFIDetailsRowMapper implements ResultSetExtractor<List<RegularizationFieldInspection>> {

	@Override
	public List<RegularizationFieldInspection> extractData(ResultSet rs) throws SQLException, DataAccessException {
		// TODO Auto-generated method stub

		Map<String, RegularizationFieldInspection> fieldMap = new LinkedHashMap<String, RegularizationFieldInspection>();

		while (rs.next()) {
			String id = rs.getString("id");
			RegularizationFieldInspection fieldInspection = fieldMap.get(id);
			if (fieldInspection == null) {
				Long lastModifiedTime = rs.getLong("lastModifiedTime");
				if (rs.wasNull())
					lastModifiedTime = null;

				AuditDetails auditdetails = AuditDetails.builder().createdBy(rs.getString("createdBy"))
						.createdTime(rs.getLong("createdTime")).lastModifiedBy(rs.getString("lastModifiedBy"))
						.lastModifiedTime(lastModifiedTime).build();

				Object additionalDetails = new Gson().fromJson(rs.getString("additionalDetails").equals("{}")
						|| rs.getString("additionalDetails").equals("null") ? null : rs.getString("additionalDetails"),
						Object.class);

				Object approachroad = new Gson().fromJson(
						rs.getString("approachroad").equals("{}") || rs.getString("approachroad").equals("null") ? null
								: rs.getString("approachroad"),
						Object.class);

				Object sitesituation = new Gson().fromJson(
						rs.getString("sitesituation").equals("{}") || rs.getString("sitesituation").equals("null")
								? null
								: rs.getString("sitesituation"),
						Object.class);

				Object reportDetails = new Gson().fromJson(
						rs.getString("report_details").equals("{}") || rs.getString("report_details").equals("null")
								? null
								: rs.getString("report_details"),
						Object.class);

				Object buildingsituation = new Gson().fromJson(rs.getString("buildingsituation").equals("{}")
						|| rs.getString("buildingsituation").equals("null") ? null : rs.getString("buildingsituation"),
						Object.class);
				
				Optional<String> setbackString = Optional.ofNullable(rs.getString("setback"));
				Object buildingRegularizationSetback = setbackString.map(setback -> {
					if ("{}".equals(setback) || "null".equals(setback)) {
						return null;
					} else {
						return new Gson().fromJson(setback, Object.class);
					}
				}).orElse(null);

				fieldInspection = RegularizationFieldInspection.builder().applicationno(rs.getString("applicationno"))
						.id(id).tenantId(rs.getString("tenantid")).approachRoad(approachroad)
						.buildingSituation(buildingsituation).siteSituation(sitesituation).reportDetails(reportDetails)
						.buildingRegularizationSetback(buildingRegularizationSetback)
						.additionalDetails(additionalDetails).auditDetails(auditdetails).build();

				fieldMap.put(id, fieldInspection);

			}

		}
		return new ArrayList<>(fieldMap.values());
	}
}
