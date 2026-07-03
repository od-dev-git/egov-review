package org.egov.edcr.feature;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.egov.common.entity.edcr.Block;
import org.egov.common.entity.edcr.Floor;
import org.egov.common.entity.edcr.Measurement;
import org.egov.common.entity.edcr.TypicalFloor;
import org.egov.edcr.entity.blackbox.MeasurementDetail;
import org.egov.edcr.entity.blackbox.PlanDetail;
import org.egov.edcr.service.LayerNames;
import org.egov.edcr.utility.Util;
import org.kabeja.dxf.DXFDimension;
import org.kabeja.dxf.DXFLWPolyline;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ApprovedConstructionExtract extends FeatureExtract {
	private static final Logger LOG = Logger.getLogger(ApprovedConstructionExtract.class);
	@Autowired
	private LayerNames layerNames;

	@Override
	public PlanDetail extract(PlanDetail pl) {
		if (LOG.isDebugEnabled())
			LOG.debug("Starting of Approved Construction Extract......");

		if (!pl.getBlocks().isEmpty())
			for (Block block : pl.getBlocks())
				if (block.getBuilding() != null && !block.getBuilding().getFloors().isEmpty())
					outside: for (Floor floor : block.getBuilding().getFloors()) {
						if (!block.getTypicalFloor().isEmpty())
							for (TypicalFloor tp : block.getTypicalFloor())
								if (tp.getRepetitiveFloorNos().contains(floor.getNumber()))
									for (Floor allFloors : block.getBuilding().getFloors())
										if (allFloors.getNumber().equals(tp.getModelFloorNo()))
											if (!allFloors.getApprovedConstruction().isEmpty()
												 || !allFloors.getDemolitionArea().isEmpty()	) {
												floor.setApprovedConstruction(allFloors.getApprovedConstruction());
												floor.setDemolitionArea(allFloors.getDemolitionArea());
												continue outside;
											}
						String layerNameApprovedConstruction = String.format(
								layerNames.getLayerName("LAYER_NAME_BLK_FLR_APPROVED_CONSTRUCTION"), block.getNumber(),
								floor.getNumber());

						List<DXFLWPolyline> dxflwPolylines = Util.getPolyLinesByLayer(pl.getDoc(),
								layerNameApprovedConstruction);

						List<Measurement> approvedConstruction = dxflwPolylines.stream()
								.map(polyline -> new MeasurementDetail(polyline, true)).collect(Collectors.toList());
						if (approvedConstruction != null && !approvedConstruction.isEmpty())
							floor.setApprovedConstruction(approvedConstruction);
						
						//LAYER_NAME_BLK_FLR_DEMOLITION_AREA
						String layerNameDemolitionArea = String.format(
								layerNames.getLayerName("LAYER_NAME_BLK_FLR_DEMOLITION_AREA"), block.getNumber(),
								floor.getNumber());

						List<DXFLWPolyline> dxflwPolylinesDemolitionArea = Util.getPolyLinesByLayer(pl.getDoc(),
								layerNameDemolitionArea);

						List<Measurement> demolitionArea = dxflwPolylinesDemolitionArea.stream()
								.map(polyline -> new MeasurementDetail(polyline, true)).collect(Collectors.toList());
						if (demolitionArea != null && !demolitionArea.isEmpty())
							floor.setDemolitionArea(demolitionArea);
					}

		if (LOG.isDebugEnabled())
			LOG.debug("End of Approved Construction Extract......");
		return pl;
	}

	@Override
	public PlanDetail validate(PlanDetail pl) {
		return pl;
	}

}
