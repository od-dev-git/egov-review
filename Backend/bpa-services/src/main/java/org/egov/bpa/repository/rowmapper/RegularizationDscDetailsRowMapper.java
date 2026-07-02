package org.egov.bpa.repository.rowmapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.egov.bpa.web.model.AuditDetails;
import org.egov.bpa.web.model.regularization.RegularizationDscDetails;
import org.egov.tracer.model.CustomException;
import org.postgresql.util.PGobject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class RegularizationDscDetailsRowMapper implements ResultSetExtractor<List<RegularizationDscDetails>> {

	@Autowired
	private ObjectMapper mapper;
	
	@Override
	public List<RegularizationDscDetails> extractData(ResultSet rs) throws SQLException, DataAccessException {
		Map<String, RegularizationDscDetails> dscDetailsMap = new LinkedHashMap<>();

		while (rs.next()) {
			String id = rs.getString("id");
			RegularizationDscDetails dscDetails = dscDetailsMap.get(id);

			if (dscDetails == null) {
				Long lastModifiedTime = rs.getLong("lastModifiedTime");
				if (rs.wasNull()) {
					lastModifiedTime = null;
				}

				AuditDetails auditdetails = AuditDetails.builder()
						.createdBy(rs.getString("createdBy"))
						.createdTime(rs.getLong("createdTime"))
						.lastModifiedBy(rs.getString("lastModifiedBy"))
						.lastModifiedTime(lastModifiedTime)
						.build();

				dscDetails = RegularizationDscDetails.builder()
						.auditDetails(auditdetails)
						.tenantId(rs.getString("tenantid"))
						.documentType(rs.getString("documenttype"))
						.documentId(rs.getString("documentid"))
						.applicationNo(rs.getString("applicationno"))
						.approvedBy(rs.getString("approvedby"))
						.id(id)
						.build();

				try {
					PGobject pgObj = (PGobject) rs.getObject("additionaldetails");

					if (pgObj != null) {
						JsonNode additionalDetail = mapper.readTree(pgObj.getValue());
						dscDetails.setAdditionalDetails(additionalDetail);
					}
				} catch (Exception e) {
					throw new CustomException("PARSING ERROR", "The DSC Details additionalDetail json cannot be parsed");
				}

				dscDetailsMap.put(id, dscDetails);
			}
		}

		return new ArrayList<>(dscDetailsMap.values());
	}

}
