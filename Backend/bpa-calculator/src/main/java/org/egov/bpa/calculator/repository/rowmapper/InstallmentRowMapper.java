package org.egov.bpa.calculator.repository.rowmapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.egov.bpa.calculator.web.models.AuditDetails;
import org.egov.bpa.calculator.web.models.Installment;
import org.egov.bpa.calculator.web.models.Installment.StatusEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;

@Component
public class InstallmentRowMapper implements ResultSetExtractor<List<Installment>> {

	@Autowired
	private ObjectMapper mapper;

	/**
	 * extract the data from the resultset and prepare the Installment Object
	 * 
	 * @see org.springframework.jdbc.core.ResultSetExtractor#extractData(java.sql.ResultSet)
	 */
	@SuppressWarnings("rawtypes")
	@Override
	public List<Installment> extractData(ResultSet rs) throws SQLException, DataAccessException {

		Map<String, Installment> installmentMap = new LinkedHashMap<String, Installment>();

		while (rs.next()) {
			String id = rs.getString("ebi_id");
			Installment currentInstallment = installmentMap.get(id);
			String tenantId = rs.getString("ebi_tenantid");
			if (currentInstallment == null) {
				Long lastModifiedTime = rs.getLong("ebi_lastmodifiedtime");
				if (rs.wasNull()) {
					lastModifiedTime = null;
				}

				Object additionalDetails = new Gson().fromJson(Objects.isNull(rs.getString("ebi_additional_details"))
						|| rs.getString("ebi_additional_details").equals("{}")
						|| rs.getString("ebi_additional_details").equals("null") ? null
								: rs.getString("ebi_additional_details"),
						Object.class);

				AuditDetails auditdetails = AuditDetails.builder().createdBy(rs.getString("ebi_createdby"))
						.createdTime(rs.getLong("ebi_createdtime")).lastModifiedBy(rs.getString("ebi_lastmodifiedby"))
						.lastModifiedTime(lastModifiedTime).build();

				StatusEnum status = StatusEnum.fromValue(rs.getString("ebi_status"));
				
				currentInstallment = Installment.builder().id(id)
						.tenantId(tenantId)
						.installmentNo(rs.getInt("ebi_installmentno"))
						.status(status)
						.consumerCode(rs.getString("ebi_consumercode"))
						.taxHeadCode(rs.getString("ebi_taxheadcode"))
						.taxAmount(rs.getBigDecimal("ebi_taxamount"))
						.demandId(rs.getString("ebi_demandid"))
						.isPaymentCompletedInDemand(rs.getBoolean("ebi_ispaymentcompletedindemand"))
						.additionalDetails(additionalDetails)
						.auditDetails(auditdetails)
						.build();
				installmentMap.put(id, currentInstallment);
			}
		}

		return new ArrayList<>(installmentMap.values());

	}

}
