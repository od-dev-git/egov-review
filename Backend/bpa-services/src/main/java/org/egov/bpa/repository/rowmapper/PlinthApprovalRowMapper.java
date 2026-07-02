package org.egov.bpa.repository.rowmapper;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.egov.bpa.web.model.AuditDetails;
import org.egov.bpa.web.model.Document;
import org.egov.bpa.web.model.PlinthApproval;
import org.postgresql.util.PGobject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;

@Component
public class PlinthApprovalRowMapper implements ResultSetExtractor<List<PlinthApproval>> {

	@Autowired
	private ObjectMapper mapper;

	/**
	 * extract the data from the resultset and prepare the BPA Object
	 * 
	 * @see org.springframework.jdbc.core.ResultSetExtractor#extractData(java.sql.ResultSet)
	 */
	@SuppressWarnings("rawtypes")
	@Override
	public List<PlinthApproval> extractData(ResultSet rs) throws SQLException, DataAccessException {

		Map<String, PlinthApproval> plinthMap = new LinkedHashMap<String, PlinthApproval>();

		while (rs.next()) {
			String id = rs.getString("pla_id");
			String applicationNo = rs.getString("pla_applicationno");
			String approvalNo = rs.getString("pla_approvalno");
			String bpaAapplicationNo = rs.getString("pla_bpaapplicationno");
			PlinthApproval currentPlinth = plinthMap.get(id);
			String tenantId = rs.getString("pla_tenantid");
			if (currentPlinth == null) {
				Long lastModifiedTime = rs.getLong("pla_lastmodifiedtime");
				if (rs.wasNull()) {
					lastModifiedTime = null;
				}

				Optional<String> additionalDetailsStr = Optional.ofNullable(rs.getString("pla_additionaldetails"));
				Object additionalDetails = additionalDetailsStr.filter(s -> !s.isEmpty() && !s.equals("null"))
						.map(add -> new Gson().fromJson(add, Object.class)).orElse(null);

				Optional<String> declarationDetailsStr = Optional.ofNullable(rs.getString("pla_declaration_details"));
				Object declarationDetails = declarationDetailsStr.filter(s -> !s.isEmpty() && !s.equals("null"))
						.map(add -> new Gson().fromJson(add, Object.class)).orElse(null);

				Optional<String> accreditedPersonDetailsStr = Optional
						.ofNullable(rs.getString("pla_accredited_person_details"));
				Object accreditedPersonDetails = accreditedPersonDetailsStr
						.filter(s -> !s.isEmpty() && !s.equals("null"))
						.map(add -> new Gson().fromJson(add, Object.class)).orElse(null);

				Optional<String> pmoDetailsStr = Optional.ofNullable(rs.getString("pla_pmo_details"));
				Object pmoDetails = pmoDetailsStr.filter(s -> !s.isEmpty() && !s.equals("null"))
						.map(add -> new Gson().fromJson(add, Object.class)).orElse(null);

				AuditDetails auditdetails = AuditDetails.builder().createdBy(rs.getString("pla_createdby"))
						.createdTime(rs.getLong("pla_createdtime")).lastModifiedBy(rs.getString("pla_lastmodifiedby"))
						.lastModifiedTime(lastModifiedTime).build();

				currentPlinth = PlinthApproval.builder().auditDetails(auditdetails).applicationNo(applicationNo)
						.status(rs.getString("pla_status")).tenantId(tenantId).approvalNo(approvalNo)
						.bpaApplicationNo(bpaAapplicationNo).id(id).additionalDetails(additionalDetails)
						.accreditedPersonDetails(accreditedPersonDetails).declarationDetails(declarationDetails)
						.pmoDetails(pmoDetails).bpaApprover(rs.getString("pla_bpaapprover")).build();

				plinthMap.put(id, currentPlinth);
			}
			addChildrenToProperty(rs, currentPlinth);

		}

		return new ArrayList<>(plinthMap.values());

	}

	/**
	 * add child objects to the BPA fro the results set
	 * 
	 * @param rs
	 * @param plinthApproval
	 * @throws SQLException
	 */
	@SuppressWarnings("unused")
	private void addChildrenToProperty(ResultSet rs, PlinthApproval plinthApproval) throws SQLException {

		String tenantId = plinthApproval.getTenantId();
		AuditDetails auditdetails = AuditDetails.builder().createdBy(rs.getString("doc_createdby"))
				.createdTime(rs.getLong("doc_createdtime")).lastModifiedBy(rs.getString("doc_lastmodifiedby"))
				.lastModifiedTime(rs.getLong("doc_lastmodifiedtime")).build();

		if (plinthApproval == null) {
			PGobject pgObj = (PGobject) rs.getObject("pla_additionaldetails");
			JsonNode additionalDetail = null;
			try {
				additionalDetail = mapper.readTree(pgObj.getValue());
			} catch (IOException e) {
				e.printStackTrace();
			}
			plinthApproval.setAdditionalDetails(additionalDetail);
		}

		String documentId = rs.getString("doc_id");
		Object docDetails = null;
		if (rs.getString("doc_additionaldetails") != null) {
			docDetails = new Gson().fromJson(rs.getString("doc_additionaldetails").equals("{}")
					|| rs.getString("doc_additionaldetails").equals("null") ? null
							: rs.getString("doc_additionaldetails"),
					Object.class);
		}

		if (documentId != null) {
			Document document = Document.builder().documentType(rs.getString("doc_documenttype"))
					.fileStoreId(rs.getString("doc_filestoreid")).id(documentId).additionalDetails(docDetails)
					.documentUid(rs.getString("doc_documentuid")).build();
			plinthApproval.addDocumentsItem(document);
		}

	}

}
