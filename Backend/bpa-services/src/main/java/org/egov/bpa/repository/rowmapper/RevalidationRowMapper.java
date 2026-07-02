package org.egov.bpa.repository.rowmapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.egov.bpa.web.model.AuditDetails;
import org.egov.bpa.web.model.Document;
import org.egov.bpa.web.model.Revalidation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;

@Component
public class RevalidationRowMapper implements ResultSetExtractor<List<Revalidation>> {

	@Autowired
	private ObjectMapper mapper;

	/**
	 * extract the data from the resultset and prepare the Revision Object
	 * 
	 * @see org.springframework.jdbc.core.ResultSetExtractor#extractData(java.sql.ResultSet)
	 */
	@SuppressWarnings("rawtypes")
	@Override
	public List<Revalidation> extractData(ResultSet rs) throws SQLException, DataAccessException {

		Map<String, Revalidation> revalidationMap = new LinkedHashMap<String, Revalidation>();

		while (rs.next()) {
			String id = rs.getString("ebpr_id");
			Revalidation currentRevalidation = revalidationMap.get(id);
			String tenantId = rs.getString("ebpr_tenantid");
			if (currentRevalidation == null) {
				Long lastModifiedTime = rs.getLong("ebpr_lastModifiedTime");
				if (rs.wasNull()) {
					lastModifiedTime = null;
				}

				Object refApplicationDetails = new Gson()
						.fromJson(rs.getString("ebpr_refApplicationDetails").equals("{}")
								|| rs.getString("ebpr_refApplicationDetails").equals("null") ? null
										: rs.getString("ebpr_refApplicationDetails"),
								Object.class);

				AuditDetails auditdetails = AuditDetails.builder().createdBy(rs.getString("ebpr_createdBy"))
						.createdTime(rs.getLong("ebpr_createdTime")).lastModifiedBy(rs.getString("ebpr_lastModifiedBy"))
						.lastModifiedTime(lastModifiedTime).build();

				currentRevalidation = Revalidation.builder().id(id).isSujogExistingApplication(rs.getBoolean("ebpr_isSujogExistingApplication"))
						.isConstructionPresent(rs.getBoolean("isConstructionPresent"))
						.isConstructionAsPerApprovedPlan(rs.getBoolean("isConstructionAsPerApprovedPlan"))
						.tenantId(tenantId).bpaApplicationNo(rs.getString("ebpr_bpaApplicationNo"))
						.bpaApplicationId(rs.getString("ebpr_bpaApplicationId"))
						.refBpaApplicationNo(rs.getString("ebpr_refBpaApplicationNo"))
						.permitNo(rs.getString("ebpr_permitNo"))
						.permitDate(rs.getLong("ebpr_permitDate"))
						.permitExpiryDate(rs.getLong("ebpr_permitExpiryDate"))
						.refApplicationDetails(refApplicationDetails)
						.auditDetails(auditdetails).build();
				revalidationMap.put(id, currentRevalidation);
			}

			addChildrenToProperty(rs, currentRevalidation);
		}

		return new ArrayList<>(revalidationMap.values());

	}

	/**
	 * add child objects to the Revision from the results set
	 * 
	 * @param rs
	 * @param currentRevalidation
	 * @throws SQLException
	 */
	@SuppressWarnings("unused")
	private void addChildrenToProperty(ResultSet rs, Revalidation currentRevalidation) throws SQLException {

		String tenantId = currentRevalidation.getTenantId();

		// Documents-
		String documentId = rs.getString("ebprd_id");
		Object docDetails = null;
		if (rs.getString("ebprd_additionalDetails") != null) {
			docDetails = new Gson().fromJson(rs.getString("ebprd_additionalDetails").equals("{}")
					|| rs.getString("ebprd_additionalDetails").equals("null") ? null
							: rs.getString("ebprd_additionalDetails"),
					Object.class);
		}
		if (documentId != null) {
			Document document = Document.builder().documentType(rs.getString("ebprd_documenttype"))
					.fileStoreId(rs.getString("ebprd_filestoreid")).id(documentId).additionalDetails(docDetails)
					.documentUid(rs.getString("ebprd_documentuid")).build();
			currentRevalidation.addDocumentsItem(document);
		}
	}

}
