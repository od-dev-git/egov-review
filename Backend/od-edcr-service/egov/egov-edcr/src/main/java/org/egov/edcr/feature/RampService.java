/*
 * eGov  SmartCity eGovernance suite aims to improve the internal efficiency,transparency,
 * accountability and the service delivery of the government  organizations.
 *
 *  Copyright (C) <2019>  eGovernments Foundation
 *
 *  The updated version of eGov suite of products as by eGovernments Foundation
 *  is available at http://www.egovernments.org
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program. If not, see http://www.gnu.org/licenses/ or
 *  http://www.gnu.org/licenses/gpl.html .
 *
 *  In addition to the terms of the GPL license to be adhered to in using this
 *  program, the following additional terms are to be complied with:
 *
 *      1) All versions of this program, verbatim or modified must carry this
 *         Legal Notice.
 *      Further, all user interfaces, including but not limited to citizen facing interfaces,
 *         Urban Local Bodies interfaces, dashboards, mobile applications, of the program and any
 *         derived works should carry eGovernments Foundation logo on the top right corner.
 *
 *      For the logo, please refer http://egovernments.org/html/logo/egov_logo.png.
 *      For any further queries on attribution, including queries on brand guidelines,
 *         please contact contact@egovernments.org
 *
 *      2) Any misrepresentation of the origin of the material is prohibited. It
 *         is required that all modified versions of this material be marked in
 *         reasonable ways as different from the original version.
 *
 *      3) This license does not grant any rights to any user of the program
 *         with regards to rights under trademark law for use of the trade names
 *         or trademarks of eGovernments Foundation.
 *
 *  In case of any queries, you can reach eGovernments Foundation at contact@egovernments.org.
 */

package org.egov.edcr.feature;

import static org.egov.edcr.constants.DxfFileConstants.A_R;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.egov.common.entity.edcr.Block;
import org.egov.common.entity.edcr.DARamp;
import org.egov.common.entity.edcr.Floor;
import org.egov.common.entity.edcr.Measurement;
import org.egov.common.entity.edcr.OccupancyType;
import org.egov.common.entity.edcr.OccupancyTypeHelper;
import org.egov.common.entity.edcr.Plan;
import org.egov.common.entity.edcr.Ramp;
import org.egov.common.entity.edcr.Result;
import org.egov.common.entity.edcr.ScrutinyDetail;
import org.egov.edcr.constants.DxfFileConstants;
import org.egov.edcr.utility.DcrConstants;
import org.egov.edcr.utility.Util;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

@Service
public class RampService extends FeatureProcess {

    private static final String SUBRULE_50_C_4_B = "ODA Annexure VIII (4-i)";
    private static final String SUBRULE_40 = "40";
    /*
     * private static final String SUBRULE_40_A1 = "40A(1)"; private static final String SUBRULE_40_A_7_DESC =
     * "Minimum number of DA Rooms in block %s "; private static final String SUBRULE_40_A_3_WIDTH_DESC =
     * "Minimum Width of Ramp %s for block %s "; private static final String SUBRULE_40_DESC =
     * "Maximum slope of ramp %s for block %s ";
     */
    private static final String SUBRULE_50_C_4_B_DESCRIPTION = "Maximum slope of ramp %s";

    /*
     * private static final String SUBRULE_40_A_1_DESC = "DA Ramp"; private static final String SUBRULE_40_A_3_SLOPE_DESC =
     * "Maximum Slope of DA Ramp %s for block %s";
     */
    private static final String SUBRULE_50_C_4_B_SLOPE_DESCRIPTION = "Maximum Slope of DA Ramp %s";
    private static final String SUBRULE_50_C_4_B_LENGTH_DESCRIPTION = "Maximum Length of DA Ramp %s";
     private static final String FLOOR = "Floor";
    // private static final String SUBRULE_40_A_3_WIDTH_DESCRIPTION = "Minimum Width of Ramp %s";
    private static final String SUBRULE_50_C_4_B_SLOPE_MAN_DESC = "DA Ramp Defined";

    @Override
    public Plan validate(Plan pl) {
        for (Block block : pl.getBlocks()) {
            if (block.getBuilding() != null && !block.getBuilding().getFloors().isEmpty()) {
                for (Floor floor : block.getBuilding().getFloors()) {
                    List<Ramp> ramps = floor.getRamps();
                    if (ramps != null && !ramps.isEmpty()) {
                        for (Ramp ramp : ramps) {
                            List<Measurement> rampPolyLines = ramp.getRamps();
                            if (rampPolyLines != null && !rampPolyLines.isEmpty()) {
                                validateDimensions(pl, block.getNumber(), floor.getNumber(), ramp.getNumber().toString(),
                                        rampPolyLines);
                            }
                        }
                    }
                }
            }
        }

        // validate necessary
        HashMap<String, String> errors = new HashMap<>();
        OccupancyTypeHelper mostRestrictiveOccupancyType = pl.getVirtualBuilding().getMostRestrictiveFarHelper();
        boolean isDaRampMandatory=isDaRampMandatory(pl);
        if (pl != null && !pl.getBlocks().isEmpty()) {
            blk: for (Block block : pl.getBlocks()) {
                if (pl.getPlot() != null && !Util.checkExemptionConditionForSmallPlotAtBlkLevel(pl.getPlot(), block)
                        && mostRestrictiveOccupancyType != null && mostRestrictiveOccupancyType.getSubtype() != null) {
                    if (!block.getDARamps().isEmpty()) {
                        boolean isSlopeDefined = false;
                        for (DARamp daRamp : block.getDARamps()) {
                            if (daRamp != null && daRamp.getSlope() != null
                                    && daRamp.getSlope().compareTo(BigDecimal.valueOf(0)) > 0) {
                                isSlopeDefined = true;
                            }
                        }
                        if (!isSlopeDefined && isDaRampMandatory) {
                            errors.put(String.format(DcrConstants.RAMP_SLOPE, "", block.getNumber()),
                                    edcrMessageSource.getMessage(DcrConstants.OBJECTNOTDEFINED,
                                            new String[] { String.format(DcrConstants.RAMP_SLOPE, "", block.getNumber()) },
                                            LocaleContextHolder.getLocale()));
                            pl.addErrors(errors);
                        }
                    } else if(isDaRampMandatory){
                        errors.put(String.format("DA Ramp", block.getNumber()),"DA Ramp not defined in block "+block.getNumber());
                        pl.addErrors(errors);
                        break;
                    }
                }
            }
        }
        return pl;
    }

    private boolean getOccupanciesForRamp(OccupancyType occupancyType) {
        return occupancyType.equals(OccupancyType.OCCUPANCY_A2) ||
                occupancyType.equals(OccupancyType.OCCUPANCY_A3) || occupancyType.equals(OccupancyType.OCCUPANCY_A4) ||
                occupancyType.equals(OccupancyType.OCCUPANCY_B1) || occupancyType.equals(OccupancyType.OCCUPANCY_B2) ||
                occupancyType.equals(OccupancyType.OCCUPANCY_B3) ||
                occupancyType.equals(OccupancyType.OCCUPANCY_C) || occupancyType.equals(OccupancyType.OCCUPANCY_C1) ||
                occupancyType.equals(OccupancyType.OCCUPANCY_C2) || occupancyType.equals(OccupancyType.OCCUPANCY_C3) ||
                occupancyType.equals(OccupancyType.OCCUPANCY_D) || occupancyType.equals(OccupancyType.OCCUPANCY_D1) ||
                occupancyType.equals(OccupancyType.OCCUPANCY_D2) || occupancyType.equals(OccupancyType.OCCUPANCY_E) ||
                occupancyType.equals(OccupancyType.OCCUPANCY_F) || occupancyType.equals(OccupancyType.OCCUPANCY_F1) ||
                occupancyType.equals(OccupancyType.OCCUPANCY_F2) || occupancyType.equals(OccupancyType.OCCUPANCY_F3) ||
                occupancyType.equals(OccupancyType.OCCUPANCY_F4);
    }

    private boolean isDaRampMandatory(Plan pl) {
    	OccupancyTypeHelper occupancyTypeHelper=pl.getVirtualBuilding().getMostRestrictiveFarHelper();
    	boolean flage=false;
    	BigDecimal plotArea=pl.getPlot().getArea();
    	
    	if(plotArea.compareTo(new BigDecimal("2000"))<=0)//Changes as per 20/05/2022 request
    		return false;
    	
    	if(DxfFileConstants.OC_PUBLIC_SEMI_PUBLIC_OR_INSTITUTIONAL.equals(occupancyTypeHelper.getType().getCode())
    		|| DxfFileConstants.OC_EDUCATION.equals(occupancyTypeHelper.getType().getCode())
    		|| DxfFileConstants.OC_TRANSPORTATION.equals(occupancyTypeHelper.getType().getCode())
    			) {
    		flage=true;
    	}
    	
    	if( DxfFileConstants.OC_RESIDENTIAL.equals(occupancyTypeHelper.getType().getCode())) {
    		long totalNumberOfDu = pl.getPlanInformation().getTotalNoOfDwellingUnits();
    		if(totalNumberOfDu>4)
    			flage=true;
    	}
    	return flage;
    }
    
    
    
    @Override
    public Plan process(Plan pl) {
        validate(pl);
        boolean valid=false;
        if (pl != null && !pl.getBlocks().isEmpty()) {
            blk: for (Block block : pl.getBlocks()) {
                scrutinyDetail = new ScrutinyDetail();
                scrutinyDetail.addColumnHeading(1, RULE_NO);
                scrutinyDetail.addColumnHeading(2, DESCRIPTION);
                scrutinyDetail.addColumnHeading(3, REQUIRED);
                scrutinyDetail.addColumnHeading(4, PROVIDED);
                scrutinyDetail.addColumnHeading(5, STATUS);
                scrutinyDetail.setKey("Block_" + block.getNumber() + "_" + "DA Ramp - Defined or not");

                ScrutinyDetail scrutinyDetail1 = new ScrutinyDetail();
                scrutinyDetail1.addColumnHeading(1, RULE_NO);
                scrutinyDetail1.addColumnHeading(2, DESCRIPTION);
                scrutinyDetail1.addColumnHeading(3, REQUIRED);
                scrutinyDetail1.addColumnHeading(4, PROVIDED);
                scrutinyDetail1.addColumnHeading(5, STATUS);
                scrutinyDetail1.setKey("Block_" + block.getNumber() + "_" + "DA Ramp - Slope length");

                ScrutinyDetail scrutinyDetail2 = new ScrutinyDetail();
                scrutinyDetail2.addColumnHeading(1, RULE_NO);
                scrutinyDetail2.addColumnHeading(2, DESCRIPTION);
                scrutinyDetail2.addColumnHeading(3, REQUIRED);
                scrutinyDetail2.addColumnHeading(4, PROVIDED);
                scrutinyDetail2.addColumnHeading(5, STATUS);
                scrutinyDetail2.setKey("Block_" + block.getNumber() + "_" + "DA Ramp - Maximum Slope");

//                ScrutinyDetail scrutinyDetail3 = new ScrutinyDetail();
//                scrutinyDetail3.addColumnHeading(1, RULE_NO);
//                scrutinyDetail3.addColumnHeading(2, FLOOR);
//                scrutinyDetail3.addColumnHeading(3, REQUIRED);
//                scrutinyDetail3.addColumnHeading(4, PROVIDED);
//                scrutinyDetail3.addColumnHeading(5, STATUS);
//                scrutinyDetail3.setSubHeading("Minimum number of da rooms");
//                scrutinyDetail3.setKey("Block_" + block.getNumber() + "_" + "DA Room");
//
//                ScrutinyDetail scrutinyDetail4 = new ScrutinyDetail();
//                scrutinyDetail4.addColumnHeading(1, RULE_NO);
//                scrutinyDetail4.addColumnHeading(2, DESCRIPTION);
//                scrutinyDetail4.addColumnHeading(3, FLOOR);
//                scrutinyDetail4.addColumnHeading(4, REQUIRED);
//                scrutinyDetail4.addColumnHeading(5, PROVIDED);
//                scrutinyDetail4.addColumnHeading(6, STATUS);
//                scrutinyDetail4.setKey("Block_" + block.getNumber() + "_" + "Ramp - Minimum Width");
//
//                ScrutinyDetail scrutinyDetail5 = new ScrutinyDetail();
//                scrutinyDetail5.addColumnHeading(1, RULE_NO);
//                scrutinyDetail5.addColumnHeading(2, DESCRIPTION);
//                scrutinyDetail5.addColumnHeading(3, FLOOR);
//                scrutinyDetail5.addColumnHeading(4, REQUIRED);
//                scrutinyDetail5.addColumnHeading(5, PROVIDED);
//                scrutinyDetail5.addColumnHeading(6, STATUS);
//                scrutinyDetail5.setKey("Block_" + block.getNumber() + "_" + "Ramp - Maximum Slope");

                OccupancyTypeHelper mostRestrictiveOccupancyType = pl.getVirtualBuilding().getMostRestrictiveFarHelper();
                if (block.getBuilding() != null && !block.getBuilding().getOccupancies().isEmpty()) {
                	boolean isDaRampMandatory=isDaRampMandatory(pl);
                    if (pl.getPlot() != null && mostRestrictiveOccupancyType != null && mostRestrictiveOccupancyType.getType() != null) {
                        if (!block.getDARamps().isEmpty()) {
                        	
                            boolean isDARampDefined = false;
                            if (block.getDARamps() != null && !block.getDARamps().isEmpty()) {
                            	isDARampDefined = true;
                            }
                            if (isDARampDefined) {
                                setReportOutputDetails(pl, SUBRULE_50_C_4_B, SUBRULE_50_C_4_B_SLOPE_MAN_DESC, isDaRampMandatory?DxfFileConstants.MANDATORY:DxfFileConstants.OPTIONAL,
                                        DcrConstants.OBJECTDEFINED_DESC, Result.Accepted.getResultVal(), scrutinyDetail);
                            } else if(isDaRampMandatory){
                                setReportOutputDetails(pl, SUBRULE_50_C_4_B, SUBRULE_50_C_4_B_SLOPE_MAN_DESC, isDaRampMandatory?DxfFileConstants.MANDATORY:DxfFileConstants.OPTIONAL,
                                        DcrConstants.OBJECTNOTDEFINED_DESC, Result.Not_Accepted.getResultVal(),
                                        scrutinyDetail);
                            }
                            
                            //slope
                            valid = false;
                            if (isDARampDefined) {
                                Map<String, String> mapOfRampNumberAndSlopeValues = new HashMap<>();
                                BigDecimal expectedSlope = BigDecimal.valueOf(1).divide(BigDecimal.valueOf(12), 2,
                                        RoundingMode.HALF_UP);
                                for (DARamp daRamp : block.getDARamps()) {
                                    BigDecimal slope = daRamp.getSlope();
                                    if (slope != null && slope.compareTo(BigDecimal.valueOf(0)) > 0
                                            && expectedSlope != null) {
                                        if (slope.compareTo(expectedSlope) <= 0) {
                                            valid = true;
                                            mapOfRampNumberAndSlopeValues.put("daRampNumber", daRamp.getNumber().toString());
                                            mapOfRampNumberAndSlopeValues.put("slope", slope.toString());
                                            break;
                                        }
                                    }
                                }
                                if (valid) {
                                    setReportOutputDetails(pl, "ODA Annexure VIII (4-i)",
                                            String.format(SUBRULE_50_C_4_B_SLOPE_DESCRIPTION,
                                                    mapOfRampNumberAndSlopeValues.get("daRampNumber")),
                                            "1/12",
                                            mapOfRampNumberAndSlopeValues.get("slope"), Result.Accepted.getResultVal(),
                                            scrutinyDetail2);
                                } else {
                                    setReportOutputDetails(pl, "ODA Annexure VIII (4-i)",
                                            String.format(SUBRULE_50_C_4_B_SLOPE_DESCRIPTION, ""),"1/12",
                                            "Less than 1/12 for all da ramps", Result.Not_Accepted.getResultVal(),
                                            scrutinyDetail2);
                                }
                            }
                            //length
                            valid=false;
                            if (isDARampDefined) {
                                Map<String, String> mapOfRampNumberAndLengthValues = new HashMap<>();
                                BigDecimal expectedLength = new BigDecimal("9");
                                for (DARamp daRamp : block.getDARamps()) {//7.70
                                    BigDecimal length = daRamp.getMeasurements().stream().map(Measurement::getHeight).reduce(BigDecimal::min).get().setScale(2, BigDecimal.ROUND_HALF_UP);
                                    if (expectedLength != null && expectedLength.compareTo(BigDecimal.valueOf(0)) > 0
                                            && expectedLength != null) {
                                        if (length.compareTo(expectedLength) <= 0) {
                                            valid = true;
                                            mapOfRampNumberAndLengthValues.put("daRampNumber", daRamp.getNumber().toString());
                                            mapOfRampNumberAndLengthValues.put("length", length.toString());
                                            break;
                                        }
                                    }
                                }
                                if (valid) {
                                    setReportOutputDetails(pl, "ODA Annexure VIII (4-i)",
                                            String.format(SUBRULE_50_C_4_B_LENGTH_DESCRIPTION,
                                            		mapOfRampNumberAndLengthValues.get("daRampNumber")),
                                            expectedLength.toString(),
                                            mapOfRampNumberAndLengthValues.get("length"), Result.Accepted.getResultVal(),
                                            scrutinyDetail1);
                                } else {
                                    setReportOutputDetails(pl, "ODA Annexure VIII (4-i)",
                                            String.format(SUBRULE_50_C_4_B_LENGTH_DESCRIPTION, ""), expectedLength.toString(),
                                            "Less than 0.08 for all da ramps", Result.Not_Accepted.getResultVal(),
                                            scrutinyDetail1);
                                }
                            }
                            

                        }
                    }

                }
            }
        }
        return pl;
    }
    
    private boolean isComp(OccupancyTypeHelper occupancyTypeHelper) {
    	boolean flage=false;
    		if(DxfFileConstants.OC_PUBLIC_SEMI_PUBLIC_OR_INSTITUTIONAL.equals(occupancyTypeHelper.getType().getCode())
    			|| DxfFileConstants.OC_TRANSPORTATION.equals(occupancyTypeHelper.getType().getCode())
    			|| DxfFileConstants.OC_EDUCATION.equals(occupancyTypeHelper.getType().getCode())
    				) {
    			flage=true;
    		}
    	return flage;
    }

    private void setReportOutputDetails(Plan pl, String ruleNo, String ruleDesc, String expected, String actual, String status,
            ScrutinyDetail scrutinyDetail) {
        Map<String, String> details = new HashMap<>();
        details.put(RULE_NO, ruleNo);
        details.put(DESCRIPTION, ruleDesc);
        details.put(REQUIRED, expected);
        details.put(PROVIDED, actual);
        details.put(STATUS, status);
        scrutinyDetail.getDetail().add(details);
        pl.getReportOutput().getScrutinyDetails().add(scrutinyDetail);
    }

    private void setReportOutputDetailsFloorWise(Plan pl, String ruleNo, String floor, String expected, String actual,
            String status, ScrutinyDetail scrutinyDetail) {
        Map<String, String> details = new HashMap<>();
        details.put(RULE_NO, ruleNo);
        details.put(FLOOR, floor);
        details.put(REQUIRED, expected);
        details.put(PROVIDED, actual);
        details.put(STATUS, status);
        scrutinyDetail.getDetail().add(details);
        pl.getReportOutput().getScrutinyDetails().add(scrutinyDetail);
    }

    private void setReportOutputDetailsFloorWiseWithDescription(Plan pl, String ruleNo, String ruleDesc, String floor,
            String expected, String actual, String status, ScrutinyDetail scrutinyDetail) {
        Map<String, String> details = new HashMap<>();
        details.put(RULE_NO, ruleNo);
        details.put(DESCRIPTION, ruleDesc);
        details.put(FLOOR, floor);
        details.put(REQUIRED, expected);
        details.put(PROVIDED, actual);
        details.put(STATUS, status);
        scrutinyDetail.getDetail().add(details);
        pl.getReportOutput().getScrutinyDetails().add(scrutinyDetail);
    }

    private void validateDimensions(Plan plan, String blockNo, int floorNo, String rampNo,
            List<Measurement> rampPolylines) {
        int count = 0;
        for (Measurement m : rampPolylines) {
            if (m.getInvalidReason() != null && m.getInvalidReason().length() > 0) {
                count++;
            }
        }
        if (count > 0) {
            plan.addError(String.format(DxfFileConstants.LAYER_RAMP_WITH_NO, blockNo, floorNo, rampNo),
                    count + " number of ramp polyline not having only 4 points in layer "
                            + String.format(DxfFileConstants.LAYER_RAMP_WITH_NO, blockNo, floorNo, rampNo));

        }
    }

    @Override
    public Map<String, Date> getAmendments() {
        return new LinkedHashMap<>();
    }
}
