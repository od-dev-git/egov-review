package org.egov.bpa.repository.rowmapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.egov.bpa.web.model.workflow.ProcessInstance;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.stereotype.Component;

@Component
public class ProcessInstanceRowMapper implements ResultSetExtractor<List<ProcessInstance>> {

	@Override
	public List<ProcessInstance> extractData(ResultSet rs) throws SQLException, DataAccessException {

		List<ProcessInstance> processInstances = new ArrayList<>();

		while(rs.next()) {

			ProcessInstance processInstance = new ProcessInstance();
			processInstance.setId(rs.getString("id"));
			processInstance.setTenantId(rs.getString("tenantid"));
			processInstance.setBusinessService(rs.getString("businessservice"));
			processInstance.setBusinessId(rs.getString("businessid"));
			processInstance.setAction(rs.getString("action"));
			processInstance.setComment(rs.getString("comment"));

			processInstances.add(processInstance);
		}

		return processInstances;
	}

}
