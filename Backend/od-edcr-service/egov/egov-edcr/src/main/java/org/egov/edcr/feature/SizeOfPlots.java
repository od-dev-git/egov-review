package org.egov.edcr.feature;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.egov.common.entity.edcr.OccupancyTypeHelper;
import org.egov.common.entity.edcr.Plan;
import org.egov.common.entity.edcr.Result;
import org.egov.common.entity.edcr.ScrutinyDetail;
import org.egov.edcr.constants.DxfFileConstants;
import org.springframework.stereotype.Service;

@Service
public class SizeOfPlots extends FeatureProcess {

	@Override
	public Map<String, Date> getAmendments() {
		// TODO Auto-generated method stub
		return new LinkedHashMap<>();
	}

	@Override
	public Plan validate(Plan pl) {
		// TODO Auto-generated method stub
		return pl;
	}

	@Override
	public Plan process(Plan pl) {
		scrutinyDetail = new ScrutinyDetail();
		scrutinyDetail.setKey("Common_Size of Plots");
		scrutinyDetail.addColumnHeading(1, RULE_NO);
		scrutinyDetail.addColumnHeading(2, DESCRIPTION);
		scrutinyDetail.addColumnHeading(3, REQUIRED);
		scrutinyDetail.addColumnHeading(4, PROVIDED);
		scrutinyDetail.addColumnHeading(5, STATUS);

		OccupancyTypeHelper occupancyTypeHelper = pl.getVirtualBuilding().getMostRestrictiveFarHelper();
		boolean status = false;
		String expectedValue = "";
		String providedValue = "";

		if (DxfFileConstants.ROW_HOUSING.equals(occupancyTypeHelper.getSubtype().getCode())) {
			if (pl.getPlot().getArea().compareTo(new BigDecimal(30)) >= 0) {

				status = true;

			}
			expectedValue = "total area >=30";
			providedValue = pl.getPlot().getArea().toString();

		} else if (DxfFileConstants.CNG_MOTHER_STATION.equals(occupancyTypeHelper.getSubtype().getCode())) {
			if (pl.getPlot().getArea().compareTo(new BigDecimal(1080)) <= 0) {
				status = true;

			}
			expectedValue = "total area <=1080";
			providedValue = pl.getPlot().getArea().toString();

		} else if (DxfFileConstants.FARM_HOUSE.equals(occupancyTypeHelper.getSubtype().getCode())) {
			if (pl.getPlot().getArea().compareTo(new BigDecimal(10000)) >= 0) {
				status = true;

			}
			expectedValue = "total area >=10000";
			providedValue = pl.getPlot().getArea().toString();

		} else if (DxfFileConstants.COUNTRY_HOMES.equals(occupancyTypeHelper.getSubtype().getCode())) {
			if (pl.getPlot().getArea().compareTo(new BigDecimal(2000)) >= 0) {
				status = true;

			}
			expectedValue = "total area >=2000";
			providedValue = pl.getPlot().getArea().toString();

		} else if (DxfFileConstants.PETROL_PUMP_ONLY_FILLING_STATION
				.equals(occupancyTypeHelper.getSubtype().getCode())) {
			if (pl.getPlot().getArea().compareTo(new BigDecimal(510)) >= 0) {
				if (pl.getPlot().getFrontYard().getArea().compareTo(new BigDecimal(30)) >= 0) {
					status = true;
				}

			}

			expectedValue = "total area>=510 and frontage>=30";
			providedValue = "total area -" + pl.getPlot().getArea().toString() + " frontage -"
					+ pl.getPlot().getFrontYard().getArea();

		} else if (DxfFileConstants.PETROL_PUMP_FILLING_STATION_AND_SERVICE_STATION
				.equals(occupancyTypeHelper.getSubtype().getCode())) {
			if (pl.getPlot().getArea().compareTo(new BigDecimal(1080)) >= 0) {
				if (pl.getPlot().getFrontYard().getArea().compareTo(new BigDecimal(30)) >= 0) {
					status = true;
				}
			}
			expectedValue = "total area>=1080 and frontage>=30";
			providedValue = "total area -" + pl.getPlot().getArea().toString() + " frontage -"
					+ pl.getPlot().getFrontYard().getArea();
		} else {
			status = true;

		}

		Map<String, String> details = new HashMap<>();
		details.put(RULE_NO, "");
		details.put(DESCRIPTION, "");
		details.put(REQUIRED, expectedValue.toString());
		details.put(PROVIDED, providedValue != null ? providedValue.toString() : "");
		details.put(STATUS, status ? Result.Accepted.getResultVal() : Result.Not_Accepted.getResultVal());
		scrutinyDetail.getDetail().add(details);
		pl.getReportOutput().getScrutinyDetails().add(scrutinyDetail);
		return pl;
	}

}
