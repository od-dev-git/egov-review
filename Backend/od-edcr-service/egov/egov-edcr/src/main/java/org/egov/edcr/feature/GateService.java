package org.egov.edcr.feature;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.egov.common.entity.edcr.Block;
import org.egov.common.entity.edcr.Measurement;
import org.egov.common.entity.edcr.OccupancyTypeHelper;
import org.egov.common.entity.edcr.Plan;
import org.egov.common.entity.edcr.Result;
import org.egov.common.entity.edcr.ScrutinyDetail;
import org.egov.edcr.constants.DxfFileConstants;
import org.egov.edcr.od.OdishaUtill;
import org.springframework.stereotype.Service;

@Service
public class GateService extends FeatureProcess {
	public static int COLOR_CODE_MAINGATE = 1;
	public static String MAINGATE_LAYER_NAME = "MAIN_GATE";

	@Override
	public Plan validate(Plan plan) {
		return plan;
	}

	@Override
	public Plan process(Plan plan) {
		ScrutinyDetail scrutinyDetail = new ScrutinyDetail();
		scrutinyDetail.setKey("Common_Gate Service");
		scrutinyDetail.addColumnHeading(1, RULE_NO);
		scrutinyDetail.addColumnHeading(2, DESCRIPTION);
		scrutinyDetail.addColumnHeading(3, REQUIRED);
		scrutinyDetail.addColumnHeading(4, PROVIDED);
		scrutinyDetail.addColumnHeading(5, STATUS);

	
            BigDecimal getBuildingHeight=OdishaUtill.getMaxBuildingHeight(plan);
            boolean flage=false;
            
            OccupancyTypeHelper typeHelper=plan.getVirtualBuilding().getMostRestrictiveFarHelper();
            
            if(DxfFileConstants.HOUSING_PROJECT.equals(typeHelper.getSubtype().getCode())
            		|| DxfFileConstants.APARTMENT_BUILDING.equals(typeHelper.getSubtype().getCode())
                	|| OdishaUtill.isSpecialBuilding(plan)	) {
            	flage=true;
            }
            
			if (getBuildingHeight.compareTo(new BigDecimal(15))>=0 || flage) {

				
				List<Measurement> maingateMeasurementList = getGateDimesions(plan);
				if (!maingateMeasurementList.isEmpty()) {
					int i = 1;
					for (Measurement measurement : maingateMeasurementList) {
						Map<String, String> details = new HashMap<>();
						BigDecimal providedHeightMainGate = measurement.getHeight();
						BigDecimal providedWidthMainGate = measurement.getWidth();

						details.put(RULE_NO, "ODA Rule 71 (1)");
						details.put(DESCRIPTION, "MainGate width " + i);
						details.put(REQUIRED, "width>=6");
						details.put(PROVIDED,
								"width = " + providedWidthMainGate);

						if (providedWidthMainGate.compareTo(new BigDecimal(6)) >= 0)
							details.put(STATUS, Result.Accepted.getResultVal());
						else
							details.put(STATUS, Result.Not_Accepted.getResultVal());
						
						scrutinyDetail.addDetail(details);

						if(providedHeightMainGate!=null && providedHeightMainGate.compareTo(BigDecimal.ZERO)>0) {
							Map<String, String> details2 = new HashMap<>();

							details2.put(RULE_NO, "ODA Rule 71 (1)");
							details2.put(DESCRIPTION, "Height of Main Gate "+i+" Archway");
							details2.put(REQUIRED, "height>=5");
							details2.put(PROVIDED,
									"height = " + providedHeightMainGate);

							if (providedHeightMainGate.compareTo(new BigDecimal(5)) >= 0)
								details2.put(STATUS, Result.Accepted.getResultVal());
							else
								details2.put(STATUS, Result.Not_Accepted.getResultVal());
							
							scrutinyDetail.addDetail(details2);
						}
						
						i++;
					}

				} else {
					plan.addError("mainGate", "Main gate not defined");
				}
			} 
		plan.getReportOutput().getScrutinyDetails().add(scrutinyDetail);

		return plan;
	}
	private List<Measurement> getGateDimesions(Plan pl) {
		
		List<Measurement> mainGate = new ArrayList<Measurement>();
		if (pl.getGate() != null) {

			for (Measurement measurement : pl.getGate().getGates()) {
				if (MAINGATE_LAYER_NAME.equals(measurement.getName()))
					mainGate.add(measurement);
			}

		}

		return mainGate;
	}

	@Override
	public Map<String, Date> getAmendments() {
		return new LinkedHashMap<>();
	}
}
