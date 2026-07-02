package org.egov.bpa.repository.rowmapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.egov.bpa.web.model.AuditDetails;
import org.egov.bpa.web.model.regularization.RegularizationApprovedByApplicationSearch;
import org.egov.bpa.web.model.regularization.RegularizationDscDetails;
import org.egov.tracer.model.CustomException;
import org.postgresql.util.PGobject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;

@Component
public class RegularizationApprovedByRowMapper
		implements ResultSetExtractor<List<RegularizationApprovedByApplicationSearch>> {

	@Autowired
	private ObjectMapper mapper;

	@Override
	public List<RegularizationApprovedByApplicationSearch> extractData(ResultSet rs)
			throws SQLException, DataAccessException {
		Map<String, RegularizationApprovedByApplicationSearch> approvalMap = new LinkedHashMap<String, RegularizationApprovedByApplicationSearch>();
		while (rs.next()) {
			String id = rs.getString("id");
			RegularizationApprovedByApplicationSearch bpas = approvalMap.get(id);
			Object buildingadditionaldetails = null;
			
			if (bpas == null) {

				Long lastModifiedTime = rs.getLong("lastModifiedTime");
				if (rs.wasNull()) {
					lastModifiedTime = null;
				}

				AuditDetails auditdetails = AuditDetails.builder().createdBy(rs.getString("createdBy"))
						.createdTime(rs.getLong("createdTime")).lastModifiedBy(rs.getString("lastModifiedBy"))
						.lastModifiedTime(lastModifiedTime).build();

				RegularizationDscDetails dscDetail = RegularizationDscDetails.builder().auditDetails(auditdetails)
						.tenantId(rs.getString("tenantid")).documentType(rs.getString("documenttype"))
						.documentId(rs.getString("documentid")).applicationNo(rs.getString("applicationno"))
						.approvedBy(rs.getString("approvedby")).id(id).build();
				try {
					
					JsonNode additionalDetails = new Gson().fromJson(rs.getString("additionaldetails").equals("{}")
							|| rs.getString("additionaldetails").equals("null") ? null : rs.getString("additionaldetails"),
							JsonNode.class);
					dscDetail.setAdditionalDetails(additionalDetails);

				} catch (Exception e) {
					throw new CustomException("PARSING ERROR",
							"The DSC Details additionalDetail json cannot be parsed");
				}

				try {
					
				 buildingadditionaldetails = new Gson().fromJson(rs.getString("buildingadditionaldetails").equals("{}")
							|| rs.getString("buildingadditionaldetails").equals("null") ? null : rs.getString("buildingadditionaldetails"),
									Object.class);
					
				} catch (Exception e) {
					throw new CustomException("PARSING ERROR", "The buildingadditionaldetails json cannot be parsed");
				}

				bpas = RegularizationApprovedByApplicationSearch.builder()
						.applicationstatus(rs.getString("applicationstatus"))
						.workflowstate(rs.getString("workflowstate"))
						.regularizationAdditionalDetails(buildingadditionaldetails).regularizationId(rs.getString("regularizationid"))
						.dscDetails(dscDetail).appType(rs.getString("apptype")).build();

				approvalMap.put(id, bpas);
			}
			// addChildrenToProperty(rs, bpas);
		}
		return new ArrayList<>(approvalMap.values());
	}

}
