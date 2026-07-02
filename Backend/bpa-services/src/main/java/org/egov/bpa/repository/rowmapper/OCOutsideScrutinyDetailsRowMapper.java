package org.egov.bpa.repository.rowmapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.egov.bpa.web.model.oc.BuildingBlockDetails;
import org.egov.bpa.web.model.oc.Fee;
import org.egov.bpa.web.model.oc.PlotDetails;
import org.egov.bpa.web.model.oc.ScrutinyDetails;
import org.egov.tracer.model.CustomException;
import org.postgresql.util.PGobject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Component
public class OCOutsideScrutinyDetailsRowMapper implements ResultSetExtractor<List<ScrutinyDetails>> {

    @Autowired
    ObjectMapper objectMapper;

    @Override
    public List<ScrutinyDetails> extractData(ResultSet rs) throws SQLException, DataAccessException {

        List<ScrutinyDetails> scrutinyDetailsList = new ArrayList<>();
        ScrutinyDetails scrutinyDetailsPermit=null;
        ScrutinyDetails scrutinyDetailsOC=null;

        while(rs.next()){
        	ScrutinyDetails scrutinyDetails = ScrutinyDetails.builder()
                    .id(rs.getString("id"))
                    .scrutinyType(rs.getString("infotype"))
                    .baseFar(rs.getDouble("basefar"))
                    .maxPermissibleFar(rs.getDouble("maxpermissiblefar"))
                    .approvedFar(rs.getDouble("approvedfar"))
                    .providedFar(rs.getDouble("providedfar"))
                    .tdrFarRelaxation(rs.getDouble("tdrfarrelaxation"))
                    .totalBuiltUpArea(rs.getBigDecimal("totalbua"))
                    .totalFloorArea(rs.getBigDecimal("totalfloorarea"))
                    .totalCarpetArea(rs.getBigDecimal("totalcarpetarea"))
                    .numberOfTemporaryStructures(rs.getBigDecimal("nooftemporarystructures"))
                    .projectValueForEIDP(rs.getBigDecimal("projectvalueforeidp"))
                    .isShelterFeeRequired(rs.getBoolean("isShelterFeeApplicable"))
                    .isSecurityDepositRequired(rs.getBoolean("isSecurityDepositRequired"))
                    .benchmarkValuePerAcre(rs.getBigDecimal("bmvperacre"))
                    .isRetentionFeeApplicable(rs.getBoolean("isretentionfeeapplicable"))
                    .totalNoOfDwellingUnits(rs.getLong("totalnoofdwellingunits"))
                    .build();

            BigDecimal plotArea = rs.getBigDecimal("plotarea");
            BigDecimal giftedLandArea= rs.getBigDecimal("giftedlandarea");

            PlotDetails plotDetails = PlotDetails.builder()
                    .plotArea(plotArea)
                    .giftedLandArea(giftedLandArea)
                    .build();
            if(plotArea!=null && giftedLandArea!=null)
            	plotDetails.setNetPlotArea(plotArea.subtract(giftedLandArea));

            scrutinyDetails.setPlotDetails(plotDetails);

            if(rs.getObject("buildingblocks")!=null) {
	            PGobject buildingBlockDetailsObject = (PGobject) rs.getObject("buildingblocks");
	            List<BuildingBlockDetails> buildingBlockDetails = null;
				try {
					buildingBlockDetails = objectMapper.readValue(buildingBlockDetailsObject.toString(),new TypeReference<List<BuildingBlockDetails>>() {});
				} catch (JsonMappingException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (JsonProcessingException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
	            scrutinyDetails.setBuildingBlockDetails(buildingBlockDetails);
            }

            if(rs.getObject("permitFee")!=null) {
	            PGobject permitFeeObject = (PGobject) rs.getObject("permitfee");
	            List<Fee> permitFee=null;
				try {
					permitFee = objectMapper.readValue(permitFeeObject.toString(),new TypeReference<List<Fee>>() {});
				} catch (JsonMappingException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (JsonProcessingException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
	            scrutinyDetails.setPermitFee(permitFee);
            }

            
           

            PGobject additionalDetailsObject = (PGobject) rs.getObject("additionaldetails");
            ObjectNode additionalDetails = null;
            if (additionalDetailsObject != null) {
                try {
                    additionalDetails = objectMapper.readValue(additionalDetailsObject.getValue(), ObjectNode.class);
                } catch (IOException ex) {
                    throw new CustomException("PARSING ERROR", "The additionalDetail json cannot be parsed");
                }
            } else {
                additionalDetails = objectMapper.createObjectNode();
            }

            scrutinyDetails.setAdditionalDetails(additionalDetails);


            if(scrutinyDetails.getScrutinyType().equalsIgnoreCase("PERMIT"))
                scrutinyDetailsPermit=scrutinyDetails;
            else
                scrutinyDetailsOC=scrutinyDetails;

        }
        scrutinyDetailsList.add(0,scrutinyDetailsPermit);
        scrutinyDetailsList.add(1,scrutinyDetailsOC);
        return scrutinyDetailsList;
    }
}
