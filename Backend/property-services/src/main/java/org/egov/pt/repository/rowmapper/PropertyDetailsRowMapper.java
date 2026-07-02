package org.egov.pt.repository.rowmapper;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.egov.pt.models.Address;
import org.egov.pt.models.AuditDetails;
import org.egov.pt.models.Institution;
import org.egov.pt.models.Property;
import org.egov.pt.models.enums.Channel;
import org.egov.pt.models.enums.CreationReason;
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
public class PropertyDetailsRowMapper  implements ResultSetExtractor<List<Property>> {

	@Autowired
	private ObjectMapper mapper;

	@Override
	public List<Property> extractData(ResultSet rs) throws SQLException, DataAccessException {

		Map<String, Property> propertyMap = new LinkedHashMap<>();

		while (rs.next()) {

			String propertyUuId = rs.getString("pid");
			Property currentProperty = propertyMap.get(propertyUuId);
			String tenanId = rs.getString("ptenantid");

			if (null == currentProperty) {

//				Address address = getAddress(rs, tenanId);

				AuditDetails auditdetails = getAuditDetail(rs, "property");

				/*String institutionId = rs.getString("institutionid");
				Institution institute = null;
				
				if (null != institutionId) {
					
					institute = Institution.builder()
						.nameOfAuthorizedPerson(rs.getString("nameOfAuthorizedPerson"))
						.tenantId(rs.getString("institutiontenantid"))
						.designation(rs.getString("designation"))
						.name(rs.getString("institutionName"))
						.type(rs.getString("institutionType"))
						.id(institutionId)
						.build();
				}*/

				Double landArea = rs.getDouble("landArea");
				if (rs.wasNull()) {
					landArea = null;
				}

				List<String> linkedProperties = null;
				String linkIdString = rs.getString("linkedProperties");
				if (!StringUtils.isEmpty(linkIdString))
					linkedProperties = Arrays.asList(linkIdString.split(","));
				
				currentProperty = Property.builder()
						.source(org.egov.pt.models.enums.Source.fromValue(rs.getString("source")))
						.creationReason(CreationReason.fromValue(rs.getString("creationReason")))
						.additionalDetails(getadditionalDetail(rs, "padditionalDetails"))
						.acknowldgementNumber(rs.getString("acknowldgementNumber"))
						.status(Status.fromValue(rs.getString("propertystatus")))
						.ownershipCategory(rs.getString("ownershipcategory"))
						.channel(Channel.fromValue(rs.getString("channel")))
						.superBuiltUpArea(rs.getBigDecimal("propertysbpa"))
						.usageCategory(rs.getString("pusagecategory"))
						.oldPropertyId(rs.getString("oldPropertyId"))
						.propertyType(rs.getString("propertytype"))
						.propertyId(rs.getString("propertyid"))
						.accountId(rs.getString("accountid"))
						.noOfFloors(rs.getLong("noOfFloors"))
						.surveyId(rs.getString("surveyId"))
						.linkedProperties(linkedProperties)
						.auditDetails(auditdetails)
//						.institution(institute)
						.landArea(landArea)
						.tenantId(tenanId)
						.id(propertyUuId)
//						.address(address)
						.build();

				
//				addChildrenToProperty(rs, currentProperty);
				propertyMap.put(propertyUuId, currentProperty);
			}

//			addChildrenToProperty(rs, currentProperty);
		}

		return new ArrayList<>(propertyMap.values());

	}
	
	/**
	 * prepares and returns an audit detail object
	 * 
	 * depending on the source the column names of result set will vary
	 * 
	 * @param rs
	 * @return
	 * @throws SQLException
	 */
	private AuditDetails getAuditDetail(ResultSet rs, String source) throws SQLException {
		
		switch (source) {
		
		case "property":
			
			Long lastModifiedTime = rs.getLong("plastModifiedTime");
			if (rs.wasNull()) {
				lastModifiedTime = null;
			}
			
			return AuditDetails.builder().createdBy(rs.getString("pcreatedBy"))
					.createdTime(rs.getLong("pcreatedTime")).lastModifiedBy(rs.getString("plastModifiedBy"))
					.lastModifiedTime(lastModifiedTime).build();
			
		default: 
			return null;
			
		}

	}
	
	/**
	 * method parses the PGobject and returns the JSON node
	 * 
	 * @param rs
	 * @param key
	 * @return
	 * @throws SQLException
	 */
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

		if(propertyAdditionalDetails.isEmpty())
			propertyAdditionalDetails = null;
		
		return propertyAdditionalDetails;

	}

}
