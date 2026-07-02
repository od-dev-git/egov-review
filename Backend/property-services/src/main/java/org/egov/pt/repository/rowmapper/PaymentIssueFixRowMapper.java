package org.egov.pt.repository.rowmapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.egov.pt.models.issuefix.PaymentIssueFix;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.stereotype.Component;

@Component
public class PaymentIssueFixRowMapper implements ResultSetExtractor<List<PaymentIssueFix>> {

	List<PaymentIssueFix> paymentIssueFixList = new ArrayList<>();

	@Override
	public List<PaymentIssueFix> extractData(ResultSet rs) throws SQLException, DataAccessException {
		while (rs.next()) {
			PaymentIssueFix ps = PaymentIssueFix.builder().acknowledgementNumber(rs.getString("acknowldgementnumber"))
					.tenantId(rs.getString("tenantid")).businessService(rs.getString("module")).build();
			paymentIssueFixList.add(ps);
		}
		return paymentIssueFixList;
	}

}