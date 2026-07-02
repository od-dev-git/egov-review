package org.egov.pt.repository.rowmapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.egov.pt.web.contracts.LegacyProperty;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;


public class LegacyPropertyRowMapper implements ResultSetExtractor<List<LegacyProperty>>{


	    
		List<LegacyProperty> legacyProperty = new ArrayList<>();

		@Override
		public List<LegacyProperty> extractData(ResultSet rs) throws SQLException, DataAccessException {
			
			while(rs.next()) {
				
				LegacyProperty property = new LegacyProperty();
				
				property.setLegacyPropertyId(rs.getString("legacyholdingid"));
				property.setOldPropertyId(rs.getString("oldpropertyid"));
				property.setPropertyId(rs.getString("sujogpropertyid"));
				property.setTenantId(rs.getString("tenantid"));
				
				legacyProperty.add(property);
				
			}
			
			return legacyProperty;
			
		}
		  
		
		
	
	
	
	

}
