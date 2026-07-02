package org.egov.collection.repository.rowmapper;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.egov.collection.web.contract.Remittance;
import org.egov.collection.web.contract.RemittanceDepositWorkDetail;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.stereotype.Service;

@Service
public class RemittanceDepositWorkResultSetExtractor implements ResultSetExtractor<List<RemittanceDepositWorkDetail>> {

    @Override
    public List<RemittanceDepositWorkDetail> extractData(ResultSet resultSet) throws SQLException, DataAccessException {

        Map<String, RemittanceDepositWorkDetail> remittances = new LinkedHashMap<>();

        while (resultSet.next()) {
            String id = resultSet.getString("id");
            Remittance remittance;
            RemittanceDepositWorkDetail remittanceDepositWorkDetail;

			remittanceDepositWorkDetail = RemittanceDepositWorkDetail.builder()
					.referenceDate(resultSet.getLong("referencedate"))
					.referenceNumber(resultSet.getString("referencenumber"))
					.creditAmount(resultSet.getString("creditamount"))
					.reciptNumber(resultSet.getString("receiptnumber"))
					.id(id)
					.build();

			remittances.put(id, remittanceDepositWorkDetail);

        }

        return new ArrayList<>(remittances.values());
    }

    private BigDecimal getBigDecimalValue(BigDecimal amount) {
        return Objects.isNull(amount) ? BigDecimal.ZERO : amount;
    }

}
