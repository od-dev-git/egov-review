package org.egov.bpa.repository.rowmapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.egov.bpa.web.model.AuditDetails;
import org.egov.bpa.web.model.CompletionCertificate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;

@Component
public class CompletionCertificateRowMapper implements ResultSetExtractor<List<CompletionCertificate>> {

    @Autowired
    private ObjectMapper mapper;

    @Override
    public List<CompletionCertificate> extractData(ResultSet rs) throws SQLException, DataAccessException {

        Map<String, CompletionCertificate> certMap = new LinkedHashMap<>();

        while (rs.next()) {

            String id = rs.getString("id");
            CompletionCertificate cert = certMap.get(id);

            if (cert == null) {

                // Handle nullable timestamp
                Long lastModifiedTime = rs.getLong("lastmodifiedtime");
                if (rs.wasNull()) {
                    lastModifiedTime = null;
                }

                // Handle JSONB additionalDetails
                String json = rs.getString("additionaldetails");
                Object additionalDetails = new Gson().fromJson(
                        (json == null || json.equals("{}") || json.equals("null")) ? null : json,
                        Object.class
                );

                // AuditDetails
                AuditDetails auditDetails = AuditDetails.builder()
                        .createdBy(rs.getString("createdby"))
                        .createdTime(rs.getLong("createdtime"))
                        .lastModifiedBy(rs.getString("lastmodifiedby"))
                        .lastModifiedTime(lastModifiedTime)
                        .build();

                // Build CompletionCertificate
                cert = CompletionCertificate.builder()
                        .id(id)
                        .certificateNo(rs.getString("certificateno"))
                        .tenantId(rs.getString("tenantid"))
                        .applicantName(rs.getString("applicantname"))
                        .applicantAddress(rs.getString("applicantaddress"))
                        .bpaPermitNumber(rs.getString("bpapermitnumber"))
                        .bpaPermitDate(rs.getLong("bpapermitdate"))
                        .plotNo(rs.getString("plotno"))
                        .khataNo(rs.getString("khatano"))
                        .mouza(rs.getString("mouza"))
                        .architectName(rs.getString("architectname"))
                        .pmoName(rs.getString("pmoname"))
                        .architectAddress(rs.getString("architectaddress"))
                        .phaseWiseCompletion(rs.getString("phasewisecompletion"))
                        .completionDate(rs.getLong("completiondate"))
                        .status(rs.getString("status"))
                        .completionFilestoreId(rs.getString("completionfilestoreid"))
                        .additionalDetails(additionalDetails)
                        .auditDetails(auditDetails)
                        .build();

                certMap.put(id, cert);
            }
        }

        return new ArrayList<>(certMap.values());
    }
}

