package org.egov.bpa.repository.rowmapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.egov.bpa.web.model.regularization.RegularizationDocumentList;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.stereotype.Component;

import com.google.gson.Gson;

@Component
public class RegularizationApprovedByDocListRowMapper implements ResultSetExtractor<List<RegularizationDocumentList>> {

	@Override
	public List<RegularizationDocumentList> extractData(ResultSet rs) throws SQLException, DataAccessException {

		Map<String, RegularizationDocumentList> approvalMap = new LinkedHashMap<String, RegularizationDocumentList>();
		while (rs.next()) {

			String documentId = rs.getString("bpa_doc_id");
			RegularizationDocumentList docList = approvalMap.get(documentId);
			if (docList == null) {

				Object docDetails = null;
				if (rs.getString("doc_details") != null) {
					docDetails = new Gson().fromJson(
							rs.getString("doc_details").equals("{}") || rs.getString("doc_details").equals("null")
									? null
									: rs.getString("doc_details"),
							Object.class);
				}

				if (documentId != null) {
					docList = RegularizationDocumentList.builder().documentType(rs.getString("bpa_doc_documenttype"))
							.fileStoreId(rs.getString("bpa_doc_filestore")).id(documentId)
							.regularizationId(rs.getString("regularizationid")).additionalDetails(docDetails)
							.documentUid(rs.getString("documentuid")).build();

				}

			}
			approvalMap.put(documentId, docList);
		}
		return new ArrayList<>(approvalMap.values());
	}
}
