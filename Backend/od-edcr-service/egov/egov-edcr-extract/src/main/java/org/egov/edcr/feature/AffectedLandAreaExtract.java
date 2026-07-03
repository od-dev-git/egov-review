
package org.egov.edcr.feature;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.collections.map.HashedMap;
import org.apache.log4j.Logger;
import org.egov.common.entity.edcr.AffectedLandArea;
import org.egov.common.entity.edcr.Measurement;
import org.egov.edcr.constants.DxfFileConstants;
import org.egov.edcr.entity.blackbox.MeasurementDetail;
import org.egov.edcr.entity.blackbox.PlanDetail;
import org.egov.edcr.service.LayerNames;
import org.egov.edcr.utility.Util;
import org.kabeja.dxf.DXFLWPolyline;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AffectedLandAreaExtract extends FeatureExtract {
	private static final Logger LOG = Logger.getLogger(AffectedLandAreaExtract.class);
	@Autowired
	private LayerNames layerNames;

	@Override
	public PlanDetail validate(PlanDetail planDetail) {
		return planDetail;
	}

	@Override
	public PlanDetail extract(PlanDetail planDetail) {
		Map<String, Integer> map = planDetail.getSubFeatureColorCodesMaster().get("AffectedLandArea");
		Map<Integer, String> affectedLandAreaFeature = new HashedMap();
		if (!map.isEmpty())
			affectedLandAreaFeature = map.entrySet().stream()
					.collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));
		List<DXFLWPolyline> affectedLandAreas = Util.getPolyLinesByLayer(planDetail.getDoc(),
				layerNames.getLayerName("LAYER_NAME_AFFECTED_LAND_AREA"));
		Map<Integer, List<Measurement>> affectedLandAreaMap = new HashMap<>();
		if (!affectedLandAreas.isEmpty()) {
			List<Measurement> surrenderRoadMeasurements = affectedLandAreas.stream()
					.map(polyline -> new MeasurementDetail(polyline, true)).collect(Collectors.toList());
			affectedLandAreaMap = surrenderRoadMeasurements.stream()
					.collect(Collectors.groupingBy(Measurement::getColorCode));
		}
		List<AffectedLandArea> list = new ArrayList<AffectedLandArea>();
		BigDecimal plotBndryDeductionArea = BigDecimal.ZERO;
		for (Map.Entry<Integer, List<Measurement>> e : affectedLandAreaMap.entrySet()) {
			AffectedLandArea affectedLandArea = new AffectedLandArea();
			affectedLandArea.setColorCode(e.getKey());
			String name = affectedLandAreaFeature.get(e.getKey());
			if(name==null) {
				planDetail.addError("AffectedLandArea"+e.getKey(), "Color code used for 'AffectedLandArea' is not as per drawing manual");
				continue;
			}
			affectedLandArea.setName(name);
			affectedLandArea.setMeasurements(e.getValue());
			affectedLandArea.setWidthDimensions(Util.getListOfDimensionByColourCode(planDetail,
					layerNames.getLayerName("LAYER_NAME_AFFECTED_LAND_AREA"), e.getKey()));
			list.add(affectedLandArea);
			BigDecimal totalArea=e.getValue().stream().map(mea -> mea.getArea()).reduce(BigDecimal::add).orElse(BigDecimal.ZERO);
			plotBndryDeductionArea = totalArea.add(plotBndryDeductionArea).setScale(2,BigDecimal.ROUND_HALF_UP);
		}
		if (list != null) {
			planDetail.setAffectedLandAreas(list);
		}
		
		//update AffectedRoadWidth
		List<org.egov.common.entity.edcr.AffectedLandArea> aff = planDetail.getAffectedLandAreas();
		List<BigDecimal> wds = new ArrayList<>();
		aff.stream().filter(af -> !DxfFileConstants.FEATURE_RESTRICTED_AREA.equals(af.getName())).map(af -> af.getWidthDimensions()).forEach(wds::addAll);
		BigDecimal extraRoadWidth=wds.stream().reduce(BigDecimal::add).orElse(BigDecimal.ZERO);
		if(extraRoadWidth!=null && planDetail.getPlanInformation().getRoadWidth().compareTo(BigDecimal.ZERO)>0) {
			planDetail.getPlanInformation().setSurrenderRoadWidth(extraRoadWidth.setScale(2,BigDecimal.ROUND_HALF_UP));
			//Discussion point on OBPAS Module progress MOM subject - june 7th 2022 
			//planDetail.getPlanInformation().setTotalRoadWidth(extraRoadWidth.add(planDetail.getPlanInformation().getRoadWidth()).setScale(2,BigDecimal.ROUND_HALF_UP));
			planDetail.getPlanInformation().setTotalRoadWidth(planDetail.getPlanInformation().getRoadWidth().setScale(2,BigDecimal.ROUND_HALF_UP));
		}
		
		//Update all plotBndryDeductionArea in plot
		planDetail.getPlot().setPlotBndryDeductionArea(plotBndryDeductionArea);
		planDetail.getPlanInformation().setTotalPlotArea(planDetail.getPlot().getPlotBndryArea().add(plotBndryDeductionArea).setScale(2,BigDecimal.ROUND_HALF_UP));
		return planDetail;
	}

}
