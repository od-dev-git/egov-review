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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.egov.common.entity.edcr.Block;
import org.egov.common.entity.edcr.Floor;
import org.egov.common.entity.edcr.Measurement;
import org.egov.common.entity.edcr.OccupancyTypeHelper;
import org.egov.common.entity.edcr.Plan;
import org.egov.common.entity.edcr.Result;
import org.egov.common.entity.edcr.ScrutinyDetail;
import org.egov.common.entity.edcr.SupplyLine;
import org.egov.edcr.constants.DxfFileConstants;
import org.egov.edcr.od.OdishaUtill;
import org.python.core.exceptions;
import org.springframework.stereotype.Service;

@Service
public class SupplyLineUtility extends FeatureProcess {
	private static final String RULE_51 = "ODA Rule 74 (8)";
	private static final String RULE_51_DESCRIPTION = "Recharging pits";
	private static final int COLOR_CODE_RECHARGING_PITS = 1;
	private static final int COLOR_CODE_SEPTIC_TANK = 2;
	private static final int COLOR_CODE_SEWAGE_TREATMENT_PLANT = 3;
	private static final String SEWERAGE_CONNECTIONS_DECLARATION = "Is Project Having Sewerage Connection?";

	@Override
	public Plan validate(Plan pl) {
		return pl;
	}

	@Override
	public Plan process(Plan pl) {

		processRechargingPits(pl);
		processWasteWaterProvision(pl);
		return pl;
	}
	
	private void processWasteWaterProvision(Plan pl) {
		scrutinyDetail = new ScrutinyDetail();
		scrutinyDetail.addColumnHeading(1, RULE_NO);
		scrutinyDetail.addColumnHeading(2, DESCRIPTION);
		scrutinyDetail.addColumnHeading(3, REQUIRED);
		scrutinyDetail.addColumnHeading(4, PROVIDED);
		scrutinyDetail.addColumnHeading(5, STATUS);
		scrutinyDetail.setKey("Common_Waste water provision");
		String subRule = RULE_51;
		
		String sewerageconnectionsDeclaration = pl.getPlanInfoProperties().get(DxfFileConstants.IS_PROJECT_HAVING_SEWERAGE_CONNECTION);
		
		if(DxfFileConstants.NO.equalsIgnoreCase(sewerageconnectionsDeclaration)) {
			setReportOutputDetails(pl, subRule, SEWERAGE_CONNECTIONS_DECLARATION, DxfFileConstants.MANDATORY,sewerageconnectionsDeclaration, Result.Accepted.getResultVal());
			
			SupplyLine septicTank = getSupplyLineByColorCode(pl.getUtility().getSupplyLine(), COLOR_CODE_SEPTIC_TANK);
			SupplyLine sewageTreatmentPlant = getSupplyLineByColorCode(pl.getUtility().getSupplyLine(), COLOR_CODE_SEWAGE_TREATMENT_PLANT);
			
			if((septicTank == null || septicTank.getSupplyLines() == null || septicTank.getSupplyLines().size() == 0 )
				&& (sewageTreatmentPlant == null || sewageTreatmentPlant.getSupplyLines() == null || sewageTreatmentPlant.getSupplyLines().size() == 0)) {
				pl.addError("WasteWaterProvisionError", "Septic tank or STP is mandatory in case Sewerage connection is not present.");
				return;
			}
			
			// septicTank
			for(Measurement measurement:septicTank.getSupplyLines()) {
				setReportOutputDetails(pl, "NBC Norms", "Septic tank - width", DxfFileConstants.NA,measurement.getWidth().setScale(2,BigDecimal.ROUND_HALF_UP).toString(), Result.Verify.getResultVal());
				setReportOutputDetails(pl, "NBC Norms", "Septic tank - length", DxfFileConstants.NA,measurement.getLength().setScale(2,BigDecimal.ROUND_HALF_UP).toString(), Result.Verify.getResultVal());
			}
			
			// sewageTreatmentPlant
			for(Measurement measurement:sewageTreatmentPlant.getSupplyLines()) {
				setReportOutputDetails(pl, "NBC Norms", "Sewage Treatment Plant - width", DxfFileConstants.NA,measurement.getWidth().setScale(2,BigDecimal.ROUND_HALF_UP).toString(), Result.Verify.getResultVal());
				setReportOutputDetails(pl, "NBC Norms", "Sewage Treatment Plant - length", DxfFileConstants.NA,measurement.getLength().setScale(2,BigDecimal.ROUND_HALF_UP).toString(), Result.Verify.getResultVal());
			}
		}else if(DxfFileConstants.YES.equalsIgnoreCase(sewerageconnectionsDeclaration)){
			setReportOutputDetails(pl, "ODA Rule 50(4), 74(8)", SEWERAGE_CONNECTIONS_DECLARATION, DxfFileConstants.MANDATORY,sewerageconnectionsDeclaration, Result.Accepted.getResultVal());
		}else {
			pl.addError("sewerageconnectionsDeclaration", DxfFileConstants.IS_PROJECT_HAVING_SEWERAGE_CONNECTION+" declaration is not defind or invalid value.");
		}
		
	}
	
	private void processRechargingPits(Plan pl) {

		HashMap<String, String> errors = new HashMap<>();

		scrutinyDetail = new ScrutinyDetail();
		scrutinyDetail.addColumnHeading(1, RULE_NO);
		scrutinyDetail.addColumnHeading(2, DESCRIPTION);
		scrutinyDetail.addColumnHeading(3, REQUIRED);
		scrutinyDetail.addColumnHeading(4, PROVIDED);
		scrutinyDetail.addColumnHeading(5, STATUS);
		scrutinyDetail.setKey("Common_Recharging pits");
		String subRule = RULE_51;
		String subRuleDesc = RULE_51_DESCRIPTION;
		BigDecimal expectedTankCapacity = BigDecimal.ZERO;
		BigDecimal plotArea = pl.getPlot() != null ? pl.getPlot().getArea() : BigDecimal.ZERO;

		boolean isRechargingPitsRequired = false;
		if (plotArea.compareTo(new BigDecimal("300")) >= 0) {
			isRechargingPitsRequired = true;
		}

		SupplyLine supplyLine = getSupplyLineByColorCode(pl.getUtility().getSupplyLine(), COLOR_CODE_RECHARGING_PITS);
		int count = supplyLine.getSupplyLines().size();

		// total volum
		BigDecimal totalVol = BigDecimal.ZERO;
		BigDecimal height = BigDecimal.ZERO;
		BigDecimal totalRequiredVol = new BigDecimal("6");
		if (supplyLine.getDistances() != null && supplyLine.getDistances().get(COLOR_CODE_RECHARGING_PITS)!=null)
			height = supplyLine.getDistances().get(COLOR_CODE_RECHARGING_PITS).size() > 0
					? supplyLine.getDistances().get(COLOR_CODE_RECHARGING_PITS).get(0)
					: BigDecimal.ZERO;
		for (Measurement measurement : supplyLine.getSupplyLines()) {
			totalVol = totalVol.add(measurement.getArea().multiply(height));
		}
		totalVol = totalVol.setScale(2, BigDecimal.ROUND_HALF_UP);

		BigDecimal totalRoofArea = OdishaUtill.getTotalTopMostRoofArea(pl);
		int multiplicand = 0;

		if (totalRoofArea.remainder(new BigDecimal("100")).compareTo(BigDecimal.ZERO) == 0)
			multiplicand = totalRoofArea.divide(new BigDecimal("100")).intValue();
		else
			multiplicand = totalRoofArea.divide(new BigDecimal("100")).intValue() + 1;

		totalRequiredVol = totalRequiredVol.multiply(new BigDecimal(multiplicand + ""));
		
		if(supplyLine.getSupplyLines()!=null & supplyLine.getSupplyLines().size()>0) {
			if(totalRoofArea.compareTo(BigDecimal.ZERO)<=0)
				pl.addError("totalRoofArea", "Roof Area layer not provided in plan.");
		}

		if (!isRechargingPitsRequired) {

			if (supplyLine.getSupplyLines() != null && supplyLine.getSupplyLines().size() > 0) {
				setReportOutputDetails(pl, "ODA Rule 47(6)", "Recharging pits Count", DxfFileConstants.OPTIONAL,
						supplyLine.getSupplyLines().size() + "", Result.Accepted.getResultVal());
			} else {
				setReportOutputDetails(pl, "ODA Rule 47(6)", "Recharging pits Count", DxfFileConstants.OPTIONAL,
						"Not Defined in the plan", Result.Accepted.getResultVal());
			}

			setReportOutputDetails(pl, "ODA Annexure - IV(1-i)", "Dimension of recharging pits", DxfFileConstants.OPTIONAL,
					totalVol + " cubic meters", Result.Accepted.getResultVal());

		} else if (isRechargingPitsRequired) {
			if ((supplyLine.getSupplyLines() != null && supplyLine.getSupplyLines().size() > 0)
					&& (supplyLine.getSupplyLines() != null && supplyLine.getSupplyLines().size() > 0))
				setReportOutputDetails(pl, "ODA ANNEXURE IV(1-i)", "Recharging pits Count", DxfFileConstants.MANDATORY,
						supplyLine.getSupplyLines().size() + "", Result.Accepted.getResultVal());
			else
				setReportOutputDetails(pl, "ODA ANNEXURE IV(1-i)", "Recharging pits Count", DxfFileConstants.MANDATORY,
						supplyLine.getSupplyLines().size() + "", Result.Not_Accepted.getResultVal());

			if (totalVol.compareTo(totalRequiredVol) >= 0)
				setReportOutputDetails(pl, "ODA Annexure - IV(1-i)", "Dimension of recharging pits", totalRequiredVol + " cubic meters",
						totalVol + " cubic meters", Result.Accepted.getResultVal());
			else
				setReportOutputDetails(pl, "ODA Annexure - IV(1-i)", "Dimension of recharging pits", totalRequiredVol + " cubic meters",
						totalVol + " cubic meters", Result.Not_Accepted.getResultVal());
		}

	}

	private void setReportOutputDetails(Plan pl, String ruleNo, String ruleDesc, String expected, String actual,
			String status) {
		Map<String, String> details = new HashMap<>();
		details.put(RULE_NO, ruleNo);
		details.put(DESCRIPTION, ruleDesc);
		details.put(REQUIRED, expected);
		details.put(PROVIDED, actual);
		details.put(STATUS, status);
		scrutinyDetail.getDetail().add(details);
		pl.getReportOutput().getScrutinyDetails().add(scrutinyDetail);
	}

	private SupplyLine getSupplyLineByColorCode(SupplyLine supplyLine, int colorCode) {
		SupplyLine supplyLine2 = new SupplyLine();
		List<Measurement> supplyLines = new ArrayList<Measurement>();
		if (supplyLine != null && supplyLine.getSupplyLines() != null)
			supplyLines = supplyLine.getSupplyLines().stream()
					.filter(measurement -> measurement.getColorCode() == colorCode).collect(Collectors.toList());
		supplyLine2.setSupplyLines(supplyLines);

		List<BigDecimal> distances = new ArrayList<>();
		if (supplyLine != null && supplyLine.getDistances() != null)
			distances = supplyLine.getDistances().get(colorCode);

		Map<Integer, List<BigDecimal>> map = new HashMap<>();
		map.put(colorCode, distances);
		supplyLine2.setDistances(map);

		return supplyLine2;
	}

	@Override
	public Map<String, Date> getAmendments() {
		return new LinkedHashMap<>();
	}
}
