package org.egov.bpa.repository.rowmapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.egov.bpa.web.model.AuditDetails;
import org.egov.bpa.web.model.DscDetails;
import org.egov.tracer.model.CustomException;
import org.postgresql.util.PGobject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class BPADigitalSignedCertificateRowMapper implements ResultSetExtractor<List<DscDetails>> {

	@Autowired
	private ObjectMapper mapper;

	public List<DscDetails> extractData(ResultSet rs) throws SQLException, DataAccessException {
		Map<String, DscDetails> dscDetailsMap = new LinkedHashMap<>();

		while (rs.next()) {
			String id = rs.getString("id");
			DscDetails dscDetail = dscDetailsMap.get(id);

			if (dscDetail == null) {
				Long lastModifiedTime = rs.getLong("lastModifiedTime");
				if (rs.wasNull()) {
					lastModifiedTime = null;
				}

				AuditDetails auditdetails = AuditDetails.builder().createdBy(rs.getString("createdBy"))
						.createdTime(rs.getLong("createdTime")).lastModifiedBy(rs.getString("lastModifiedBy"))
						.lastModifiedTime(lastModifiedTime).build();

				dscDetail = DscDetails.builder().auditDetails(auditdetails).tenantId(rs.getString("tenantid"))
						.documentType(rs.getString("documenttype")).documentId(rs.getString("documentid"))
						.applicationNo(rs.getString("applicationno")).approvedBy(rs.getString("approvedby"))
						.id(id).build();

				try {
					PGobject pgObj = (PGobject) rs.getObject("additionaldetails");

					if (pgObj != null) {
						JsonNode additionalDetail = mapper.readTree(pgObj.getValue());
						dscDetail.setAdditionalDetails(additionalDetail);
					}
				} catch (Exception e) {
					throw new CustomException("PARSING ERROR",
							"The DSC Details additionalDetail json cannot be parsed");
				}
				
				try {
					PGobject pgObj = (PGobject) rs.getObject("buildingadditionaldetails");

					if (pgObj != null) {
						JsonNode additionalDetail = mapper.readTree(pgObj.getValue());
						dscDetail.setBuildingAdditionalDetails(additionalDetail);
					}
				} catch (Exception e) {
					throw new CustomException("PARSING ERROR",
							"The DSC Details buildingplan additionalDetail json cannot be parsed");
				}

				dscDetailsMap.put(id, dscDetail);
			}

		}

		return new ArrayList<>(dscDetailsMap.values());

	}

}