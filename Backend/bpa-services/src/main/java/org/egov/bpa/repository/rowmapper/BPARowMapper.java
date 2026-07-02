package org.egov.bpa.repository.rowmapper;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.egov.bpa.util.BPAConstants;
import org.egov.bpa.web.model.AuditDetails;
import org.egov.bpa.web.model.BPA;
import org.egov.bpa.web.model.Document;
import org.egov.bpa.web.model.DscDetails;
import org.egov.bpa.web.model.Notice;
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
public class BPARowMapper implements ResultSetExtractor<List<BPA>> {

	@Autowired
	private ObjectMapper mapper;

	/**
	 * extract the data from the resultset and prepare the BPA Object
	 * @see org.springframework.jdbc.core.ResultSetExtractor#extractData(java.sql.ResultSet)
	 */
	@SuppressWarnings("rawtypes")
	@Override
	public List<BPA> extractData(ResultSet rs) throws SQLException, DataAccessException {

		Map<String, BPA> buildingMap = new LinkedHashMap<String, BPA>();

		while (rs.next()) {
			String id = rs.getString("bpa_id");
			String applicationNo = rs.getString("applicationno");
			String approvalNo = rs.getString("approvalNo");
			BPA currentbpa = buildingMap.get(id);
			String tenantId = rs.getString("bpa_tenantId");
			if (currentbpa == null) {
				Long lastModifiedTime = rs.getLong("bpa_lastModifiedTime");
				if (rs.wasNull()) {
					lastModifiedTime = null;
				}
    
				Object additionalDetails = new Gson().fromJson(rs.getString("additionalDetails").equals("{}")
						|| rs.getString("additionalDetails").equals("null") ? null : rs.getString("additionalDetails"),
						Object.class);
				
				Object reWorkHistory = new Gson().fromJson(
						Objects.isNull(rs.getObject("reWorkHistory")) || rs.getString("reWorkHistory").equals("{}")
								|| rs.getString("reWorkHistory").equals("null") ? null : rs.getString("reWorkHistory"),
								Object.class);
				
				AuditDetails auditdetails = AuditDetails.builder().createdBy(rs.getString("bpa_createdBy"))
						.createdTime(rs.getLong("bpa_createdTime")).lastModifiedBy(rs.getString("bpa_lastModifiedBy"))
						.lastModifiedTime(lastModifiedTime).build();
				
				Map<String, String> map = additionalDetails != null ? (Map) additionalDetails
						: new HashMap<String, String>();
				
				String riskType = map.get("riskType");

				currentbpa = BPA.builder()
						.auditDetails(auditdetails)
						.applicationNo(applicationNo)
						.status(rs.getString("status"))
						.tenantId(tenantId)
						.approvalNo(approvalNo)
						.edcrNumber(rs.getString("edcrnumber"))
						.approvalDate(rs.getLong("approvalDate"))
						.accountId(rs.getString("accountId"))
						.landId(rs.getString("landId"))
						.applicationDate(rs.getLong("applicationDate"))
						.id(id)
						.additionalDetails(additionalDetails)
						.businessService(rs.getString("businessService"))
						.reWorkHistory(reWorkHistory)
						.isRevisionApplication(rs.getBoolean("bpa_isRevisionApplication"))
						.isRevalidationApplication(rs.getBoolean("isRevalidationApplication"))
						.permitExpiryDate(rs.getLong("permitExpiryDate"))
						.isOCOutsideSujogApplication(rs.getBoolean("isOCOutsideSujogApplication"))
						.correlationId(rs.getString("bpa_correlationid"))
						.isNoticeExist(rs.getString("notice_id") != null && !rs.getString("notice_id").isEmpty())
						.riskType(riskType)
						.build();

				buildingMap.put(id, currentbpa);
			}
			addChildrenToProperty(rs, currentbpa);

		}

		return new ArrayList<>(buildingMap.values());

	}

	/**
	 * add child objects to the BPA fro the results set
	 * @param rs
	 * @param bpa
	 * @throws SQLException
	 */
	@SuppressWarnings("unused")
	private void addChildrenToProperty(ResultSet rs, BPA bpa) throws SQLException {

		String tenantId = bpa.getTenantId();
		AuditDetails auditdetails = AuditDetails.builder().createdBy(rs.getString("bpa_createdBy"))
				.createdTime(rs.getLong("bpa_createdTime")).lastModifiedBy(rs.getString("bpa_lastModifiedBy"))
				.lastModifiedTime(rs.getLong("bpa_lastModifiedTime")).build();

		if (bpa == null) {
			PGobject pgObj = (PGobject) rs.getObject("additionaldetail");
			JsonNode additionalDetail = null;
			try {
				additionalDetail = mapper.readTree(pgObj.getValue());
			} catch (IOException e) {
				e.printStackTrace();
			}
			bpa.setAdditionalDetails(additionalDetail);
		}


		String documentId = rs.getString("bpa_doc_id");
		Object docDetails = null;
		if(rs.getString("doc_details") != null) {
			docDetails = new Gson().fromJson(rs.getString("doc_details").equals("{}")
					|| rs.getString("doc_details").equals("null") ? null : rs.getString("doc_details"),
					Object.class);
		}
		
		if (documentId != null) {
			Document document = Document.builder().documentType(rs.getString("bpa_doc_documenttype"))
					.fileStoreId(rs.getString("bpa_doc_filestore"))
					.id(documentId)
					.additionalDetails(docDetails)
					.documentUid(rs.getString("documentUid")).build();
			bpa.addDocumentsItem(document);
		}
		
//		String noticeId = rs.getString("noitce_id");
//		if(noticeId!=null) {
//			Notice notice = Notice.builder().filestoreid(rs.getString("scnfilestoreid")).LetterNo(rs.getString("letterNumber"))
//					.letterType(rs.getString("letterType")).status(rs.getString("noticestatus")).build();
//		
//		}
		
		if(rs.getString("dsc_id")!=null) {
        	DscDetails dscDetail = DscDetails.builder()
                    .documentType(rs.getString("dsc_doctype"))
                    .documentId(rs.getString("dsc_docid"))
                    .applicationNo(rs.getString("dsc_applicationno"))
                    .id(rs.getString("dsc_id"))
                    .tenantId(tenantId)
                    .approvedBy(rs.getString("dsc_approvedby"))
                    .build();

			try {
				PGobject pgObj = (PGobject) rs.getObject("dsc_additionaldetails");

				if (pgObj != null) {
					JsonNode additionalDetail = mapper.readTree(pgObj.getValue());
					dscDetail.setAdditionalDetails(additionalDetail);
				}
			} catch (Exception e) {
				throw new CustomException("PARSING ERROR", "The DSC Details additionalDetail json cannot be parsed");
			}
			bpa.addDscDetailsItem(dscDetail);
        }
		
		
		if (rs.getString("plandsc_id") != null) {
			DscDetails planDscDetail = DscDetails.builder().documentType(rs.getString("plandsc_doctype"))
					.documentId(rs.getString("plandsc_docid")).applicationNo(rs.getString("plandsc_applicationno"))
					.id(rs.getString("plandsc_id")).tenantId(tenantId).approvedBy(rs.getString("plandsc_approvedby"))
					.build();

			try {
				PGobject pgObj = (PGobject) rs.getObject("plandsc_additionaldetails");

				if (pgObj != null) {
					JsonNode additionalDetail = mapper.readTree(pgObj.getValue());
					planDscDetail.setAdditionalDetails(additionalDetail);
				}
			} catch (Exception e) {
				throw new CustomException("PARSING ERROR", "The Plan DSC Details additionalDetail json cannot be parsed");
			}
			bpa.addPlanDscDetailsItem(planDscDetail);
		}
	}
}
