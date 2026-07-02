package org.egov.bpa.repository.rowmapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.egov.bpa.web.model.scheduler.PendingApplicationDetails;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.stereotype.Component;

@Component
public class EdcrDataPullRowMapper implements ResultSetExtractor<List<PendingApplicationDetails>> {

	@Override
	public List<PendingApplicationDetails> extractData(ResultSet rs) throws SQLException, DataAccessException {

		List<PendingApplicationDetails> pendingApplications = new ArrayList<>();
		while (rs.next()) {
			String id = rs.getString("id");
			PendingApplicationDetails pendingApplication = 
					PendingApplicationDetails.builder()
					.applicationNo(rs.getString("applicationNo"))
					.id(id)
					.tenantId(rs.getString("tenantId"))
					.build();
			pendingApplications.add(pendingApplication);
		}
		return pendingApplications;
	}

}
