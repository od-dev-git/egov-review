package org.egov.bpa.repository.rowmapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

import org.egov.bpa.web.model.regularization.RegularizationVillage;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.stereotype.Component;

@Component
public class RegularizationVillageRowmapper implements ResultSetExtractor<List<RegularizationVillage>> {

	@Override
	public List<RegularizationVillage> extractData(ResultSet rs) throws SQLException, DataAccessException {

		List<RegularizationVillage> responseList = new LinkedList<>();

		while (rs.next()) {

			RegularizationVillage response = RegularizationVillage.builder().applicatioNo(rs.getString("app_no"))
					.status(rs.getString("status")).village(rs.getString("village")).build();

			responseList.add(response);

		}

		return responseList;

	}

}
