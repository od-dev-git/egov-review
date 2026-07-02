package org.egov.bpa.repository.rowmapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

import org.egov.bpa.web.model.FeePendingApplication;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.stereotype.Component;

@Component
public class FeePendingApplicationRowMapper implements ResultSetExtractor<List<FeePendingApplication>>{

	@Override
	public List<FeePendingApplication> extractData(ResultSet rs) throws SQLException, DataAccessException {
		
		List<FeePendingApplication> feePendingApplications = new LinkedList<>();
		
		while(rs.next()) {
			
			FeePendingApplication feePendingApplication = FeePendingApplication.builder()
					.applicatioNo(rs.getString("bpa_appno")).status(rs.getString("bpa_status"))
					.businessService(rs.getString("bpa_businessservice")).approvedDate(rs.getLong("wf_createdtime"))
					.tenantId(rs.getString("bpa_tenantid")).action(rs.getString("wf_action")).build();
			feePendingApplications.add(feePendingApplication);
		}
		
		return feePendingApplications;
	}

}
