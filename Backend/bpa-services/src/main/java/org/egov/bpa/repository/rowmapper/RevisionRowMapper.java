package org.egov.bpa.repository.rowmapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.egov.bpa.web.model.AuditDetails;
import org.egov.bpa.web.model.Document;
import org.egov.bpa.web.model.Revision;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;

@Component
public class RevisionRowMapper implements ResultSetExtractor<List<Revision>> {

	@Autowired
	private ObjectMapper mapper;

	/**
	 * extract the data from the resultset and prepare the Revision Object
	 * 
	 * @see org.springframework.jdbc.core.ResultSetExtractor#extractData(java.sql.ResultSet)
	 */
	@SuppressWarnings("rawtypes")
	@Override
	public List<Revision> extractData(ResultSet rs) throws SQLException, DataAccessException {

		Map<String, Revision> revisionMap = new LinkedHashMap<String, Revision>();

		while (rs.next()) {
			String id = rs.getString("ebpr_id");
			Revision currentRevision = revisionMap.get(id);
			String tenantId = rs.getString("ebpr_tenantid");
			if (currentRevision == null) {
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

				currentRevision = Revision.builder().id(id).isSujogExistingApplication(rs.getBoolean("ebpr_isSujogExistingApplication"))
						.tenantId(tenantId).bpaApplicationNo(rs.getString("ebpr_bpaApplicationNo"))
						.bpaApplicationId(rs.getString("ebpr_bpaApplicationId"))
						.refBpaApplicationNo(rs.getString("ebpr_refBpaApplicationNo"))
						.refPermitNo(rs.getString("ebpr_refPermitNo"))
						.refPermitDate(rs.getLong("ebpr_refPermitDate"))
						.refPermitExpiryDate(rs.getLong("ebpr_refPermitExpiryDate"))
						.refApplicationDetails(refApplicationDetails)
						.auditDetails(auditdetails).build();
				revisionMap.put(id, currentRevision);
			}

			addChildrenToProperty(rs, currentRevision);
		}

		return new ArrayList<>(revisionMap.values());

	}

	/**
	 * add child objects to the Revision from the results set
	 * 
	 * @param rs
	 * @param revision
	 * @throws SQLException
	 */
	@SuppressWarnings("unused")
	private void addChildrenToProperty(ResultSet rs, Revision revision) throws SQLException {

		String tenantId = revision.getTenantId();

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
			revision.addDocumentsItem(document);
		}
	}

}
