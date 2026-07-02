package org.egov.bpa.repository.rowmapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.egov.bpa.web.model.AuditDetails;
import org.egov.bpa.web.model.DocRemark;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;

@Component
public class DocRemarkMapper implements ResultSetExtractor<List<DocRemark>> {

	@Autowired
	private ObjectMapper mapper;

	@SuppressWarnings("rawtypes")
	@Override
	public List<DocRemark> extractData(ResultSet rs) throws SQLException, DataAccessException {

		Map<String, DocRemark> DocRemarkMap = new LinkedHashMap<String, DocRemark>();

		while (rs.next()) {
			String id = rs.getString("id");
			System.out.println("id:" + id);
			DocRemark docRemark = DocRemarkMap.get(id);

			if (docRemark == null) {
				Long lastModifiedTime = rs.getLong("lastModifiedTime");
				if (rs.wasNull())
					lastModifiedTime = null;

				Object additionalDetails = new Gson()
						.fromJson(rs.getString("additionalDetails").equals("{}")
								|| rs.getString("additionalDetails").equals("null") ? null
										: rs.getString("additionalDetails"),
								Object.class);

				AuditDetails auditdetails = AuditDetails.builder().createdBy(rs.getString("createdBy"))
						.createdTime(rs.getLong("createdTime")).lastModifiedBy(rs.getString("lastModifiedBy"))
						.lastModifiedTime(lastModifiedTime).build();

				docRemark = DocRemark.builder().id(id).businessId(rs.getString("businessId"))
						.documentCode(rs.getString("documentCode")).auditDetails(auditdetails)
						.isUpdatable(rs.getBoolean("isUpdatable")).additionalDetails(additionalDetails).build();

				DocRemarkMap.put(id, docRemark);
			}
		}
		return new ArrayList<>(DocRemarkMap.values());
	}

}
