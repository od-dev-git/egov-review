package org.egov.edcr.od;

import static org.egov.edcr.utility.DcrConstants.DECIMALDIGITS_MEASUREMENTS;
import static org.egov.edcr.utility.DcrConstants.ROUNDMODE_MEASUREMENTS;

import java.math.BigDecimal;
import java.util.List;

import org.egov.common.entity.edcr.Block;
import org.egov.common.entity.edcr.Building;
import org.egov.common.entity.edcr.Floor;
import org.egov.common.entity.edcr.Occupancy;
import org.egov.common.entity.edcr.OccupancyPercentage;
import org.egov.common.entity.edcr.OccupancyTypeHelper;
import org.egov.common.entity.edcr.Plan;
import org.egov.edcr.constants.DxfFileConstants;
import org.egov.edcr.feature.ProvisionService;

public class FeeCalculationUtill {

	public static final BigDecimal MIN_PLOT_SIZE_FOR_EWS = BigDecimal.valueOf(2000);

	public static void checkShelterFeePrevalidation(Plan pl) {
		OccupancyTypeHelper helper = pl.getVirtualBuilding().getMostRestrictiveFarHelper();

		String ocType = "";

		if (helper == null || helper.getType() == null || helper.getSubtype() == null)
			return;

		if (helper != null && helper.getType() != null)
			ocType = helper.getType().getCode();

		if (DxfFileConstants.OC_MIXED_USE.equals(ocType) || (pl.getPlanInformation().getOccupancyPercentages() != null
				&& pl.getPlanInformation().getOccupancyPercentages().size() > 1)) {
			
			calculateFeesForMixedUse(pl);
			
			

		} else {
			
			boolean isShelterFeeRequired = false;
			BigDecimal totalEWSFeeEffectiveArea = BigDecimal.ZERO;
			BigDecimal totalCarpetAreaWithoutEWSAndLIG = BigDecimal.ZERO;
			BigDecimal totalCarpetAreaOfEWSAndLIG = BigDecimal.ZERO;

			if (pl != null && pl.getPlanInformation().getPlotArea().compareTo(MIN_PLOT_SIZE_FOR_EWS) >= 0)
				if (DxfFileConstants.PLOTTED_DETACHED_OR_INDIVIDUAL_RESIDENTIAL_BUILDING
						.equals(helper.getSubtype().getCode())
						|| DxfFileConstants.SEMI_DETACHED.equals(helper.getSubtype().getCode())
						|| DxfFileConstants.ROW_HOUSING.equals(helper.getSubtype().getCode())
						|| DxfFileConstants.APARTMENT_BUILDING.equals(helper.getSubtype().getCode())
						|| DxfFileConstants.HOUSING_PROJECT.equals(helper.getSubtype().getCode())
						|| DxfFileConstants.STUDIO_APARTMENTS.equals(helper.getSubtype().getCode())
						|| DxfFileConstants.MEDIUM_INCOME_HOUSING.equals(helper.getSubtype().getCode())) {

//				BigDecimal totalEwsBUA = pl.getTotalEWSAreaInPlot();

					// total carpet area of the EWS and LIG occupancy is calculated here.
					for (Block blk : pl.getBlocks()) {
						Building building = blk.getBuilding();

						for (Floor flr : building.getFloors()) {
							List<Occupancy> occupancies = flr.getOccupancies();
							for (Occupancy occupancy : occupancies) {

								if (occupancy.getTypeHelper() != null && occupancy.getTypeHelper().getSubtype() != null
										&& (DxfFileConstants.EWS
												.equals(occupancy.getTypeHelper().getSubtype().getCode())
												|| DxfFileConstants.LOW_INCOME_HOUSING
														.equals(occupancy.getTypeHelper().getSubtype().getCode()))) {

									totalCarpetAreaOfEWSAndLIG = totalCarpetAreaOfEWSAndLIG
											.add(occupancy.getCarpetArea() != null ? occupancy.getCarpetArea()
													: BigDecimal.ZERO);

								}

							}
						}
					}

//				BigDecimal totalBUA = pl.getVirtualBuilding().getTotalFloorArea();

					// total carpet area of all occupancies excluding EWS and LIG is calculated
					// here.
					for (Block blk : pl.getBlocks()) {
						Building building = blk.getBuilding();

						for (Floor flr : building.getFloors()) {
							List<Occupancy> occupancies = flr.getOccupancies();
							for (Occupancy occupancy : occupancies) {

								if (occupancy.getTypeHelper() != null && occupancy.getTypeHelper().getSubtype() != null
										&& !(DxfFileConstants.EWS
												.equals(occupancy.getTypeHelper().getSubtype().getCode())
												|| DxfFileConstants.LOW_INCOME_HOUSING
														.equals(occupancy.getTypeHelper().getSubtype().getCode()))) {

									totalCarpetAreaWithoutEWSAndLIG = totalCarpetAreaWithoutEWSAndLIG
											.add(occupancy.getCarpetArea() != null ? occupancy.getCarpetArea()
													: BigDecimal.ZERO);

								}

							}
						}
					}

//				BigDecimal totalEwsRequiredArea = totalBUA.multiply(new BigDecimal("0.10")).setScale(2,
//						BigDecimal.ROUND_HALF_UP);

					BigDecimal totalEwsRequiredArea = totalCarpetAreaWithoutEWSAndLIG.multiply(new BigDecimal("0.10"))
							.setScale(2, BigDecimal.ROUND_HALF_UP);

					long totalNumberOfDu = pl.getPlanInformation().getTotalNoOfDwellingUnits();

					BigDecimal plotArea = pl.getPlanInformation().getPlotArea();
					BigDecimal plotAreaInAcre = pl.getPlanInformation().getPlotArea()
							.divide(ProvisionService.ACRE_TO_SQ_MT, DECIMALDIGITS_MEASUREMENTS, ROUNDMODE_MEASUREMENTS);

					if (totalNumberOfDu > 8 && plotArea.compareTo(new BigDecimal("2000")) > 0
							&& plotAreaInAcre.compareTo(ProvisionService.PLOT_AREA_FOUR_ACRE) <= 0) {
						isShelterFeeRequired = true;
						if (totalCarpetAreaOfEWSAndLIG.compareTo(totalEwsRequiredArea) >= 0) {
							isShelterFeeRequired = false;
							totalEWSFeeEffectiveArea = BigDecimal.ZERO;
						} else {
							if (DxfFileConstants.YES.equalsIgnoreCase(pl.getPlanInfoProperties().get(
									DxfFileConstants.HAS_PROJECT_PROVIDED_MIN_10_PER_BUA_FOR_EWS_WITHIN_5_KM_FROM_PROJECT_SITE))) {
								isShelterFeeRequired = false;
								totalEWSFeeEffectiveArea = totalEwsRequiredArea.subtract(totalCarpetAreaOfEWSAndLIG);
							} else {
								isShelterFeeRequired = true;
								totalEWSFeeEffectiveArea = totalEwsRequiredArea.subtract(totalCarpetAreaOfEWSAndLIG);
							}
						}

					}

				}
			// The total EWS fee effective area is now changed from builtup area to carpet
			// area.
		
			pl.getPlanInformation().setShelterFeeRequired(isShelterFeeRequired);
			pl.setTotalEWSFeeEffectiveArea(totalEWSFeeEffectiveArea);
		}



	}

	private static void calculateFeesForMixedUse(Plan pl) {
		
		boolean isShelterFeeRequired = false;
		BigDecimal totalEWSFeeEffectiveArea = BigDecimal.ZERO;
		BigDecimal totalCarpetAreaOfEWSAndLIG = BigDecimal.ZERO;
		BigDecimal totalResidentialCarpetArea = BigDecimal.ZERO;
		BigDecimal carpetAreaOfEWSAndLIG = BigDecimal.ZERO;
		
		for (OccupancyPercentage occupancyPercentage : pl.getPlanInformation().getOccupancyPercentages().values()) {
			// copy code
			String subOcTypeCode = occupancyPercentage.getSubOccupancyCode();
			
			if (pl != null && pl.getPlanInformation().getPlotArea().compareTo(MIN_PLOT_SIZE_FOR_EWS) >= 0)
				if (DxfFileConstants.PLOTTED_DETACHED_OR_INDIVIDUAL_RESIDENTIAL_BUILDING
						.equals(subOcTypeCode)
						|| DxfFileConstants.SEMI_DETACHED.equals(subOcTypeCode)
						|| DxfFileConstants.ROW_HOUSING.equals(subOcTypeCode)
						|| DxfFileConstants.APARTMENT_BUILDING.equals(subOcTypeCode)
						|| DxfFileConstants.HOUSING_PROJECT.equals(subOcTypeCode)
						|| DxfFileConstants.STUDIO_APARTMENTS.equals(subOcTypeCode)
						|| DxfFileConstants.MEDIUM_INCOME_HOUSING.equals(subOcTypeCode)) {
					
					//total residential carpet area without EWS and LIG					
					totalResidentialCarpetArea = totalResidentialCarpetArea.add(occupancyPercentage.getTotalCarpetArea());
					
					//total EWS and LIG carpet area
					for (Block blk : pl.getBlocks()) {
						Building building = blk.getBuilding();

						for (Floor flr : building.getFloors()) {
							List<Occupancy> occupancies = flr.getOccupancies();
							for (Occupancy occupancy : occupancies) {

								if (occupancy.getTypeHelper() != null && occupancy.getTypeHelper().getSubtype() != null
										&& (DxfFileConstants.EWS
												.equals(occupancy.getTypeHelper().getSubtype().getCode())
												|| DxfFileConstants.LOW_INCOME_HOUSING
														.equals(occupancy.getTypeHelper().getSubtype().getCode()))) {

									carpetAreaOfEWSAndLIG = carpetAreaOfEWSAndLIG
											.add(occupancy.getCarpetArea() != null ? occupancy.getCarpetArea()
													: BigDecimal.ZERO);

								}

							}
						}
					}
					
					totalCarpetAreaOfEWSAndLIG = totalCarpetAreaOfEWSAndLIG.add(carpetAreaOfEWSAndLIG);
									
				}

			
		}
		
		BigDecimal totalEwsRequiredArea = totalResidentialCarpetArea.multiply(new BigDecimal("0.10"))
				.setScale(2, BigDecimal.ROUND_HALF_UP);

		long totalNumberOfDu = pl.getPlanInformation().getTotalNoOfDwellingUnits();

		BigDecimal plotArea = pl.getPlanInformation().getPlotArea();
		BigDecimal plotAreaInAcre = pl.getPlanInformation().getPlotArea()
				.divide(ProvisionService.ACRE_TO_SQ_MT, DECIMALDIGITS_MEASUREMENTS, ROUNDMODE_MEASUREMENTS);

		if (totalNumberOfDu > 8 && plotArea.compareTo(new BigDecimal("2000")) > 0
				&& plotAreaInAcre.compareTo(ProvisionService.PLOT_AREA_FOUR_ACRE) <= 0) {
			isShelterFeeRequired = true;
			if (totalCarpetAreaOfEWSAndLIG.compareTo(totalEwsRequiredArea) >= 0) {
				isShelterFeeRequired = false;
				totalEWSFeeEffectiveArea = BigDecimal.ZERO;
			} else {
				if (DxfFileConstants.YES.equalsIgnoreCase(pl.getPlanInfoProperties().get(
						DxfFileConstants.HAS_PROJECT_PROVIDED_MIN_10_PER_BUA_FOR_EWS_WITHIN_5_KM_FROM_PROJECT_SITE))) {
					isShelterFeeRequired = false;
					totalEWSFeeEffectiveArea = totalEwsRequiredArea.subtract(totalCarpetAreaOfEWSAndLIG);
				} else {
					isShelterFeeRequired = true;
					totalEWSFeeEffectiveArea = totalEwsRequiredArea.subtract(totalCarpetAreaOfEWSAndLIG);
				}
			}

		}
		
		pl.getPlanInformation().setShelterFeeRequired(isShelterFeeRequired);
		pl.setTotalEWSFeeEffectiveArea(totalEWSFeeEffectiveArea);
		
	}
}
