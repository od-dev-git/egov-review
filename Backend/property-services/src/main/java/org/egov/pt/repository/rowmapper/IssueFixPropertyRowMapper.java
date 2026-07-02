package org.egov.pt.repository.rowmapper;

import org.egov.pt.models.AuditDetails;
import org.egov.pt.models.Property;
import org.egov.pt.models.enums.Channel;
import org.egov.pt.models.enums.CreationReason;
import org.egov.pt.models.enums.Status;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Component
public class IssueFixPropertyRowMapper  implements ResultSetExtractor<List<Property>> {
    @Override
    public List<Property> extractData(ResultSet rs) throws SQLException, DataAccessException {
        List<Property> propertyList=new ArrayList<>();
        while (rs.next()){

            AuditDetails auditDetails=getAuditDetail(rs);
            Property property= Property.builder()
                    .source(org.egov.pt.models.enums.Source.fromValue(rs.getString("source")))
                    .creationReason(CreationReason.fromValue(rs.getString("creationReason")))
                    .acknowldgementNumber(rs.getString("acknowldgementNumber"))
                    .status(Status.fromValue(rs.getString("status")))
                    .ownershipCategory(rs.getString("ownershipcategory"))
                    .channel(Channel.fromValue(rs.getString("channel")))
                    .superBuiltUpArea(rs.getBigDecimal("superbuiltuparea"))
                    .usageCategory(rs.getString("usagecategory"))
                    .oldPropertyId(rs.getString("oldPropertyId"))
                    .propertyType(rs.getString("propertytype"))
                    .propertyId(rs.getString("propertyid"))
                    .accountId(rs.getString("accountid"))
                    .noOfFloors(rs.getLong("noOfFloors"))
                    .surveyId(rs.getString("surveyId"))
                    .auditDetails(auditDetails)
                    .tenantId(rs.getString("tenantId"))
                    .id(rs.getString("id")).build();

            propertyList.add(property);
        }
        return propertyList;
    }

    private AuditDetails getAuditDetail(ResultSet rs) throws SQLException {
        return AuditDetails.builder().createdBy(rs.getString("createdBy"))
                        .createdTime(rs.getLong("createdTime")).lastModifiedBy(rs.getString("lastModifiedBy"))
                        .lastModifiedTime(rs.getLong("lastModifiedTime")).build();

    }

}

