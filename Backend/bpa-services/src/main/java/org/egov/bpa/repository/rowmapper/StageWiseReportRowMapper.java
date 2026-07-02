package org.egov.bpa.repository.rowmapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.egov.bpa.web.model.AuditDetails;
import org.egov.bpa.web.model.StageWiseReport;
import org.egov.bpa.web.model.StageWiseReport.LevelType;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class StageWiseReportRowMapper implements ResultSetExtractor<List<StageWiseReport>> {

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public List<StageWiseReport> extractData(ResultSet rs) throws SQLException, DataAccessException {
        List<StageWiseReport> reports = new ArrayList<>();

        while (rs.next()) {
            StageWiseReport report = new StageWiseReport();

            report.setId(rs.getString("id"));
            report.setApplicationNo(rs.getString("application_no"));
            report.setLevelType(LevelType.valueOf(rs.getString("level_type")));
            report.setBlockNo(rs.getString("block_no"));
            report.setFloorNo(rs.getString("floor_no"));

            
            report.setDocumentDetails(parseJsonField(rs.getObject("document_details")));
            report.setAdditionalDetails(parseJsonField(rs.getObject("additional_details")));

            report.setStatus(rs.getString("status"));
            report.setApprovalNo(rs.getString("approval_no"));

          
            AuditDetails audit = AuditDetails.builder()
                    .createdBy(rs.getString("created_by"))
                    .lastModifiedBy(rs.getString("last_modified_by"))
                    .createdTime(rs.getLong("created_time"))
                    .lastModifiedTime(rs.getLong("last_modified_time"))
                    .build();

            report.setAuditDetails(audit);

            reports.add(report);
        }

        return reports;
    }
    
    /**
     * Handles JSONB objects safely for PostgreSQL
     */
    private Object parseJsonField(Object jsonObj) {
        try {
            if (jsonObj == null) return null;

            if (jsonObj instanceof String) {
                return mapper.readValue((String) jsonObj, Object.class);
            }

            // For PGobject (json / jsonb)
            String value = jsonObj.toString();
            return mapper.readValue(value, Object.class);

        } catch (Exception e) {
            return null; // fail-safe
        }
    }
    
}
