package org.egov.edcr.feature;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.egov.common.entity.edcr.Plan;
import org.egov.common.entity.edcr.Result;
import org.egov.common.entity.edcr.ScrutinyDetail;
import org.egov.edcr.constants.DxfFileConstants;
import org.springframework.stereotype.Service;

@Service
public class AffectedLandArea extends FeatureProcess {

	private static final Logger LOG = Logger.getLogger(AffectedLandArea.class);
	private static final String RULE1 = "ODA Rule 9(5-ii), 36(2), 89(2)";
	

	@Override
	public Map<String, Date> getAmendments() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Plan validate(Plan pl) {
		List<org.egov.common.entity.edcr.AffectedLandArea> affectedLandAreas = pl.getAffectedLandAreas();
		List<BigDecimal> widthDimensions = new ArrayList<>();
		affectedLandAreas.stream().filter(aff -> !DxfFileConstants.FEATURE_RESTRICTED_AREA.equals(aff.getName()))
				.map(aff -> aff.getWidthDimensions()).forEach(widthDimensions::addAll);
		if (widthDimensions != null && widthDimensions.size() > 1) {
			pl.addError("AffectedLandAreawidthDimensions", "Multiple dimensions found for 'AffectedLandArea'. Only 1 dimension allowed.");
		}
		return pl;
	}

	@Override
	public Plan process(Plan pl) {
		validate(pl);
		ScrutinyDetail scrutinyDetail = new ScrutinyDetail();
		scrutinyDetail.addColumnHeading(1, RULE_NO);
		scrutinyDetail.addColumnHeading(2, DESCRIPTION);
		scrutinyDetail.addColumnHeading(4, PROVIDED);
		scrutinyDetail.addColumnHeading(5, STATUS);
		scrutinyDetail.setKey("Common_Affected Land");

		for (org.egov.common.entity.edcr.AffectedLandArea affectedLandArea : pl.getAffectedLandAreas()) {
			// affected area
			if (affectedLandArea.getMeasurements() != null && !affectedLandArea.getMeasurements().isEmpty()) {
				Map<String, String> details = new HashMap<>();
				details.put(RULE_NO, RULE1);
				details.put(DESCRIPTION, affectedLandArea.getName() + " affected area");
				BigDecimal area = affectedLandArea.getMeasurements().stream().map(l -> l.getArea())
						.reduce(BigDecimal::add).orElse(BigDecimal.ZERO).setScale(2, BigDecimal.ROUND_HALF_UP);
				details.put(PROVIDED, area.toString());
				details.put(STATUS, Result.Verify.getResultVal());
				scrutinyDetail.addDetail(details);
			}
			// affected road width
			if (affectedLandArea.getWidthDimensions() != null && !affectedLandArea.getWidthDimensions().isEmpty()) {
				Map<String, String> details1 = new HashMap<>();
				details1.put(RULE_NO, RULE1);
				details1.put(DESCRIPTION, affectedLandArea.getName() + " affected road width");
				BigDecimal width = affectedLandArea.getWidthDimensions().stream().reduce(BigDecimal::add)
						.orElse(BigDecimal.ZERO).setScale(2, BigDecimal.ROUND_HALF_UP);
				details1.put(PROVIDED, width.toString());
				details1.put(STATUS, Result.Verify.getResultVal());
				scrutinyDetail.addDetail(details1);
			}
		}
		pl.getReportOutput().getScrutinyDetails().add(scrutinyDetail);
		return pl;
	}

}
