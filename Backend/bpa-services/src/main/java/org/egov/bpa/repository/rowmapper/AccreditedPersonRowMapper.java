package org.egov.bpa.repository.rowmapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.egov.bpa.web.model.AuditDetails;
import org.egov.bpa.web.model.accreditedperson.AccreditedPerson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class AccreditedPersonRowMapper implements ResultSetExtractor<List<AccreditedPerson>> {

	@Autowired
	private ObjectMapper mapper;

	/**
	 * extract the data from the resultset and prepare the Revision Object
	 * 
	 * @see org.springframework.jdbc.core.ResultSetExtractor#extractData(java.sql.ResultSet)
	 */
	@SuppressWarnings("rawtypes")
	@Override
	public List<AccreditedPerson> extractData(ResultSet rs) throws SQLException, DataAccessException {

		Map<String, AccreditedPerson> accreditedPersonMap = new LinkedHashMap<String, AccreditedPerson>();

		while (rs.next()) {
			String id = rs.getString("ebpa_id");
			AccreditedPerson currentAccreditedPerson = accreditedPersonMap.get(id);
			if (currentAccreditedPerson == null) {
				Long lastModifiedTime = rs.getLong("ebpa_lastModifiedTime");
				if (rs.wasNull()) {
					lastModifiedTime = null;
				}

				AuditDetails auditdetails = AuditDetails.builder().createdBy(rs.getString("ebpa_createdBy"))
						.createdTime(rs.getLong("ebpa_createdTime")).lastModifiedBy(rs.getString("ebpa_lastModifiedBy"))
						.lastModifiedTime(lastModifiedTime).build();

				currentAccreditedPerson = AccreditedPerson.builder().id(id)
						.userUUID(rs.getString("ebpa_user_uuid"))
						.userId(rs.getLong("ebpa_user_id"))
						.personName(rs.getString("ebpa_person_name"))
						.firmName(rs.getString("ebpa_firm_name"))
						.accreditationNo(rs.getString("ebpa_accreditation_no"))
						.certificateIssueDate(rs.getLong("ebpa_certificate_issue_date"))
						.validTill(rs.getLong("ebpa_valid_till"))
						.auditDetails(auditdetails).build();
				accreditedPersonMap.put(id, currentAccreditedPerson);
			}
		}

		return new ArrayList<>(accreditedPersonMap.values());

	}

}
