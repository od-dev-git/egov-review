package org.egov.edcr.feature;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.egov.common.entity.edcr.Block;
import org.egov.common.entity.edcr.Occupancy;
import org.egov.common.entity.edcr.OccupancyType;
import org.egov.common.entity.edcr.Plan;
import org.egov.common.entity.edcr.Result;
import org.egov.common.entity.edcr.ScrutinyDetail;
import org.egov.edcr.constants.DxfFileConstants;
import org.egov.edcr.od.OdishaUtill;
import org.springframework.stereotype.Service;

@Service
public class Helipad extends FeatureProcess {

	private static final Logger LOG = Logger.getLogger(Helipad.class);

	@Override
	public Plan validate(Plan pl) {
		return pl;
	}

	@Override
	public Plan process(Plan pl) {

		ScrutinyDetail scrutinyDetail = new ScrutinyDetail();
		scrutinyDetail.setKey("Common_Helipad Provision");
		scrutinyDetail.addColumnHeading(1, RULE_NO);
		scrutinyDetail.addColumnHeading(2, DESCRIPTION);
		scrutinyDetail.addColumnHeading(3, REQUIRED);
		scrutinyDetail.addColumnHeading(4, PROVIDED);
		scrutinyDetail.addColumnHeading(5, STATUS);
		
		BigDecimal buildingHeight=OdishaUtill.getMaxBuildingHeight(pl);
		boolean isMandatory=buildingHeight.compareTo(new BigDecimal("200"))>=0;
		boolean isProvided = DxfFileConstants.YES.equals(pl.getPlanInfoProperties().get(DxfFileConstants.PROVISION_FOR_HELIPAD_PRESENT));
		boolean status = isMandatory && isProvided?true:!isMandatory?true:false;
		
		Map<String, String> details = new HashMap<>();
		details.put(RULE_NO, "ODA Rule 43 (v)");
		details.put(DESCRIPTION, "Provision for Helipad");
		details.put(REQUIRED, isMandatory?DxfFileConstants.MANDATORY:DxfFileConstants.OPTIONAL);
		details.put(PROVIDED, isProvided?DxfFileConstants.PROVIDED:DxfFileConstants.NA);
		details.put(STATUS, status ? Result.Accepted.getResultVal() : Result.Not_Accepted.getResultVal());
		scrutinyDetail.getDetail().add(details);
	
		pl.getReportOutput().getScrutinyDetails().add(scrutinyDetail);
		return pl;

	}

	@Override
	public Map<String, Date> getAmendments() {
		return new LinkedHashMap<>();
	}

}
