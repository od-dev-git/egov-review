package org.egov.bpa.repository.rowmapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

import org.egov.bpa.web.model.BPAVillage;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.stereotype.Component;

@Component
public class BPAVillageRowmapper implements ResultSetExtractor<List<BPAVillage>>{

	@Override
	public List<BPAVillage> extractData(ResultSet rs) throws SQLException, DataAccessException {

		List<BPAVillage> responseList = new LinkedList<>();

		while (rs.next()) {

			BPAVillage response = BPAVillage.builder().applicatioNo(rs.getString("bpa_app"))
					.status(rs.getString("bpa_status")).village(rs.getString("village")).build();

			responseList.add(response);

		}

		return responseList;

	}

}
