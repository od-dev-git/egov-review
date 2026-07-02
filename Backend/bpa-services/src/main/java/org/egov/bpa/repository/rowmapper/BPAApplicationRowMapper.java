package org.egov.bpa.repository.rowmapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


import org.egov.bpa.web.model.BpaApplicationSearch;
import org.egov.common.contract.request.User;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.stereotype.Component;

@Component
public class BPAApplicationRowMapper implements ResultSetExtractor<List<BpaApplicationSearch>> {

	@Override
	public List<BpaApplicationSearch> extractData(ResultSet rs) throws SQLException, DataAccessException {
		Map<String, BpaApplicationSearch> buildingMap = new LinkedHashMap<String, BpaApplicationSearch>();
		
		
		while (rs.next()) {
			String id = rs.getString("id");
			
			Long bussinesservicesla =  rs.getLong("businessservicesla");
		     Long lastmodifiedtime = rs.getLong("bpa_lastModifiedTime");
			 Long timeSinceLastAction = System.currentTimeMillis() - lastmodifiedtime;
			 bussinesservicesla = bussinesservicesla-timeSinceLastAction;
			 String assigneeUuid = rs.getString("assigneeuuid");
             User assignee;
             assignee = User.builder().uuid(assigneeUuid).build();
//             String username = rs.getString("username");
//             User assignee;
//             assignee = User.builder().userName(username).build();
//			
			//System.out.println("id:"+id);
			BpaApplicationSearch bpas = buildingMap.get(id);
			if(bpas==null) {
			bpas = BpaApplicationSearch.builder().id(id).applicationNo(rs.getString("applicationNo")).businessService(rs.getString("businessService")).
					tenantId(rs.getString("tenantId")).applicationstatus(rs.getString("applicationstatus")).assigneeuuid(rs.getString("assigneeuuid")).workflowstate(rs.getString("workflowstate"))
					.landId(rs.getString("landId")).bussinesservicesla(bussinesservicesla).assignee(assignee).build();
			buildingMap.put(id, bpas);
		}
		}
		return new ArrayList<>(buildingMap.values());
	}

}
