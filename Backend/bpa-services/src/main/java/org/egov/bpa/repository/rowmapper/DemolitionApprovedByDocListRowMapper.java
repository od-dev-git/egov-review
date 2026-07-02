package org.egov.bpa.repository.rowmapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.egov.bpa.web.model.demolition.DemolitionDocumentList;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.stereotype.Component;

import com.google.gson.Gson;

@Component
public class DemolitionApprovedByDocListRowMapper implements ResultSetExtractor<List<DemolitionDocumentList>> {

	@Override
	public List<DemolitionDocumentList> extractData(ResultSet rs) throws SQLException, DataAccessException {

		Map<String, DemolitionDocumentList> approvalMap = new LinkedHashMap<String, DemolitionDocumentList>();
		while (rs.next()) {

			String documentId = rs.getString("bpa_doc_id");
			DemolitionDocumentList docList = approvalMap.get(documentId);
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
					docList = DemolitionDocumentList.builder().documentType(rs.getString("bpa_doc_documenttype"))
							.fileStoreId(rs.getString("bpa_doc_filestore")).id(documentId)
							.demolitionId(rs.getString("demolitionid")).additionalDetails(docDetails)
							.documentUid(rs.getString("documentuid")).build();

				}

			}
			approvalMap.put(documentId, docList);
		}
		return new ArrayList<>(approvalMap.values());
	}

}
