package org.egov.bpa.repository.rowmapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.egov.bpa.web.model.AuditDetails;
import org.egov.bpa.web.model.regularization.RegularizationDocRemark;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.stereotype.Component;

import com.google.gson.Gson;

@Component
public class RegularizationDocRemarkMapper implements ResultSetExtractor<List<RegularizationDocRemark>> {

	@Override
	public List<RegularizationDocRemark> extractData(ResultSet rs) throws SQLException, DataAccessException {

		Map<String, RegularizationDocRemark> docRemarkMap = new LinkedHashMap<>();

		while (rs.next()) {
			String id = rs.getString("id");
			System.out.println("id:" + id);
			RegularizationDocRemark docRemark = docRemarkMap.get(id);

			if (docRemark == null) {
				Long lastModifiedTime = rs.getLong("lastModifiedTime");
				if (rs.wasNull())
					lastModifiedTime = null;

				Object additionalDetails = new Gson()
						.fromJson(rs.getString("additionalDetails").equals("{}")
								|| rs.getString("additionalDetails").equals("null") ? null
										: rs.getString("additionalDetails"),
								Object.class);

				AuditDetails auditdetails = AuditDetails.builder()
						.createdBy(rs.getString("createdBy"))
						.createdTime(rs.getLong("createdTime"))
						.lastModifiedBy(rs.getString("lastModifiedBy"))
						.lastModifiedTime(lastModifiedTime)
						.build();

				docRemark = RegularizationDocRemark.builder()
						.id(id)
						.businessId(rs.getString("businessId"))
						.documentCode(rs.getString("documentCode"))
						.auditDetails(auditdetails)
						.isUpdatable(rs.getBoolean("isUpdatable"))
						.additionalDetails(additionalDetails)
						.build();

				docRemarkMap.put(id, docRemark);
			}
		}
		return new ArrayList<>(docRemarkMap.values());
	}

}
