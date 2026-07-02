package org.egov.pt.repository.rowmapper;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.egov.pt.models.Address;
import org.egov.pt.models.Assessment;
import org.egov.pt.models.Assessment.Source;
import org.egov.pt.models.AuditDetails;
import org.egov.pt.models.Document;
import org.egov.pt.models.GeoLocation;
import org.egov.pt.models.Locality;
import org.egov.pt.models.UnitUsage;
import org.egov.pt.models.enums.Status;
import org.egov.tracer.model.CustomException;
import org.postgresql.util.PGobject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class AssessmentRowMapper implements ResultSetExtractor<List<Assessment>> {


	@Autowired
	private ObjectMapper mapper;

	@Override
	public List<Assessment> extractData(ResultSet rs) throws SQLException, DataAccessException {


		Map<String, Assessment> assessmentMap = new HashMap<>();
		while(rs.next()) {
			String currentAssessmentId = rs.getString("ass_assessmentid");
			Assessment assessment = assessmentMap.get(currentAssessmentId);
			if(null == assessment) {
				assessment = Assessment.builder()
						.id(rs.getString("ass_assessmentid"))
						.assessmentNumber(rs.getString("ass_assessmentnumber"))
						.status(Status.fromValue(rs.getString("ass_status")))
						.tenantId(rs.getString("ass_tenantid"))
						.assessmentDate(rs.getLong("ass_assessmentdate"))
						.financialYear(rs.getString("ass_financialyear"))
						.propertyId(rs.getString("ass_propertyid"))
						.source(Source.fromValue(rs.getString("ass_source")))
						.unitUsageList(new ArrayList<>())
						.documents(new HashSet<>())
						.address(getAddress(rs, rs.getString("ass_tenantid"))).build();

				try {
					PGobject obj = (PGobject) rs.getObject("ass_additionaldetails");
					if (obj != null) {
						JsonNode propertyAdditionalDetails = mapper.readTree(obj.getValue());
						assessment.setAdditionalDetails(propertyAdditionalDetails);
					}
				} catch (IOException e) {
					throw new CustomException("PARSING ERROR", "The assessment additionaldetails json cannot be parsed");
				}

				UnitUsage unitUsage = getUnitUsage(rs);
				if(unitUsage!=null)
					assessment.getUnitUsageList().add(unitUsage);

				Document document = getDocument(rs);
				if(document!=null)
					assessment.getDocuments().add(document);


				AuditDetails auditDetails = AuditDetails.builder().createdBy(rs.getString("ass_createdby"))
						.createdTime(rs.getLong("ass_createdtime")).lastModifiedBy(rs.getString("ass_lastmodifiedby"))
						.lastModifiedTime(rs.getLong("ass_lastmodifiedtime")).build();
				assessment.setAuditDetails(auditDetails);

				assessmentMap.put(assessment.getId(), assessment);
			}else {
				if(getUnitUsage(rs) != null)
					assessment.getUnitUsageList().add(getUnitUsage(rs));
				if(getDocument(rs) != null)
					assessment.getDocuments().add(getDocument(rs));
			}
		}

		return new ArrayList<>(assessmentMap.values());
	}



	private UnitUsage getUnitUsage(ResultSet rs) throws SQLException {
		if(null == rs.getString("us_id"))
			return null;

		AuditDetails auditDetails = AuditDetails.builder().createdBy(rs.getString("us_createdby"))
				.createdTime(rs.getLong("us_createdtime")).lastModifiedBy(rs.getString("us_lastmodifiedby"))
				.lastModifiedTime(rs.getLong("us_lastmodifiedtime")).build();

		if(StringUtils.hasText(rs.getString("us_id"))) {
			return UnitUsage.builder().id(rs.getString("us_id"))
					.occupancyDate(rs.getLong("us_occupancydate"))
					.occupancyType((rs.getString("us_occupancytype")))
					.usageCategory(rs.getString("us_usagecategory"))
					.tenantId(rs.getString("us_tenantid"))
					.unitId(rs.getString("us_unitid"))
					.auditDetails(auditDetails)
					.build();
		} else return null;
	}



	private Document getDocument(ResultSet rs) throws SQLException {
		if(null == rs.getString("doc_id"))
			return null;

		AuditDetails auditDetails = AuditDetails.builder().createdBy(rs.getString("doc_createdby"))
				.createdTime(rs.getLong("doc_createdtime")).lastModifiedBy(rs.getString("doc_lastmodifiedby"))
				.lastModifiedTime(rs.getLong("doc_lastmodifiedtime")).build();

		return Document.builder().id(rs.getString("doc_id"))
				.status(Status.fromValue(rs.getString("doc_status")))
				.documentType(rs.getString("doc_documenttype"))
				.documentUid(rs.getString("doc_documentuid"))
				.fileStoreId(rs.getString("doc_filestoreid"))
				.auditDetails(auditDetails)
				.build();
	}

     
	private Address getAddress(ResultSet rs, String tenanId) throws SQLException {

		Locality locality = Locality.builder().code(rs.getString("locality")).build();

		GeoLocation geoLocation = GeoLocation.builder().longitude(rs.getDouble("longitude"))
				.latitude(rs.getDouble("latitude")).build();

		return Address.builder().additionalDetails(getadditionalDetail(rs, "addressadditionaldetails"))
				.buildingName(rs.getString("buildingname")).landmark(rs.getString("landmark"))
				.district(rs.getString("district")).country(rs.getString("country")).pincode(rs.getString("pincode"))
				.doorNo(rs.getString("doorNo")).plotNo(rs.getString("plotNo")).region(rs.getString("region"))
				.street(rs.getString("street")).id(rs.getString("addressid")).state(rs.getString("state"))
				.city(rs.getString("city")).ward(rs.getString("ward")).geoLocation(geoLocation).locality(locality)
				.tenantId(tenanId).build();
	}

	private JsonNode getadditionalDetail(ResultSet rs, String key) throws SQLException {

		JsonNode propertyAdditionalDetails = null;

		try {

			PGobject obj = (PGobject) rs.getObject(key);
			if (obj != null) {
				propertyAdditionalDetails = mapper.readTree(obj.getValue());
			}

		} catch (IOException e) {
			throw new CustomException("PARSING ERROR", "The propertyAdditionalDetail json cannot be parsed");
		}

		if (propertyAdditionalDetails.isEmpty())
			propertyAdditionalDetails = null;

		return propertyAdditionalDetails;

	}


}
