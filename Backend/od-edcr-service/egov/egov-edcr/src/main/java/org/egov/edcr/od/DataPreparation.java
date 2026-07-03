package org.egov.edcr.od;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import org.egov.common.entity.ApplicationType;
import org.egov.common.entity.edcr.Block;
import org.egov.common.entity.edcr.Floor;
import org.egov.common.entity.edcr.Occupancy;
import org.egov.common.entity.edcr.OccupancyTypeHelper;
import org.egov.common.entity.edcr.Plan;
import org.egov.common.entity.edcr.ScrutinyDetail;
import org.egov.edcr.constants.DxfFileConstants;
import org.egov.edcr.constants.OdishaUlbs;

public class DataPreparation {
	private static final boolean BPA5_FLAGE = true;
	public static void updatePlanDetails(Plan pl) {
		updateNmaData(pl);
		updateVirtualBuildingHeight(pl);
		updateBusinessService(pl);
		updateSubOccupancy(pl);
	}
	
	private static void updateNmaData(Plan pl) {
		StringBuffer numberOfStoreys=new StringBuffer();
		StringBuffer buildingHeightExcludingMumty=new StringBuffer();
		StringBuffer floorAreaInSquareMetresStoreyWise=new StringBuffer();
		
		for(Block block:pl.getBlocks()) {
			if(block.getBuilding().getFloors()!=null) {
				numberOfStoreys.append("block "+block.getNumber()+" "+block.getBuilding().getFloors().size()+" \r\n");
				for(Floor floor:block.getBuilding().getFloors()) {
					StringBuffer occupancyArea=new StringBuffer();
					for(Occupancy occupancy:floor.getOccupancies()) {
						if(occupancy.getTypeHelper()!=null & occupancy.getTypeHelper().getType()!=null)
						occupancyArea.append(occupancy.getTypeHelper().getType().getName()+" "+ occupancy.getBuiltUpArea()+" \r\n");
					}
					floorAreaInSquareMetresStoreyWise.append("block "+block.getNumber()+" floor "+floor.getNumber()+" "+occupancyArea.toString()+" \r\n");
				}
			}
			buildingHeightExcludingMumty.append("block"+block.getNumber()+" "+block.getBuilding().getBuildingHeight()+" \r\n");
		}
		pl.getPlanInformation().setNumberOfStoreys(numberOfStoreys.toString());
		pl.getPlanInformation().setBuildingHeightExcludingMumty(buildingHeightExcludingMumty.toString());
		pl.getPlanInformation().setBuildingHeightIncludingMumty(buildingHeightExcludingMumty.toString());
		pl.getPlanInformation().setFloorAreaInSquareMetresStoreyWise(floorAreaInSquareMetresStoreyWise.toString());
	}

	private static void updateVirtualBuildingHeight(Plan pl) {
		BigDecimal buildingHeight = BigDecimal.ZERO;
		try {
			buildingHeight = pl.getBlocks().stream().map(block -> block.getBuilding().getBuildingHeight())
					.reduce(BigDecimal::max).get();
		} catch (Exception e) {
			e.printStackTrace();
		}
		pl.getVirtualBuilding().setBuildingHeight(buildingHeight);

		BigDecimal declaredBuildingHeight = BigDecimal.ZERO;
		try {
			declaredBuildingHeight = pl.getBlocks().stream()
					.map(block -> block.getBuilding().getDeclaredBuildingHeight()).reduce(BigDecimal::max).get();
		} catch (Exception e) {
			e.printStackTrace();
		}
		pl.getVirtualBuilding().setDeclaredBuildingHeight(declaredBuildingHeight);
	}

	private static void updateBusinessService(Plan pl) {

		try {
			Double buildingHeight = pl.getVirtualBuilding().getBuildingHeight().doubleValue();
			Double plotArea = pl.getPlot().getArea().doubleValue();
			boolean isSpecialBuilding = DxfFileConstants.YES.equals(pl.getPlanInformation().getSpecialBuilding()) ? true
					: false;
			if(ApplicationType.OCCUPANCY_CERTIFICATE.equals(pl.getApplicationType()))
				setBusinessServiceOC(pl, buildingHeight, plotArea, isSpecialBuilding);
			else
				setBusinessService(pl, buildingHeight, plotArea, isSpecialBuilding);
		} catch (Exception e) {
			e.printStackTrace();
		}

		if (pl.getPlanInformation().getBusinessService() == null
				|| pl.getPlanInformation().getBusinessService().trim().isEmpty()) {
			pl.addError("BusinessService", "Not able to find BusinessService Type.");

		}
	}
	
	private static void setBusinessService(Plan pl, Double buildingHeight, Double plotArea, boolean isSpecialBuilding) {
	    OdishaUlbs ulb = OdishaUlbs.getUlb(pl.getThirdPartyUserTenantld());

	    if (buildingHeight != null && plotArea != null) {
	        String buildingHeightService = determineServiceByBuildingHeight(buildingHeight, isSpecialBuilding);
	        String plotAreaService = determineServiceByPlotArea(plotArea, ulb, isSpecialBuilding);

	        String finalService = getHigherPriorityService(buildingHeightService, plotAreaService);
	        pl.getPlanInformation().setBusinessService(finalService);

	        if (!isSpecialBuilding && finalService.equals(DxfFileConstants.BPA_PA_MODULE_CODE)) {
	            if (BPA5_FLAGE && isPlanApplicableForAccreditedWorkflow(pl, buildingHeight, plotArea)) {
	                pl.getPlanInformation().setBusinessService(pl.getPlanInformation().getBusinessService()
	                        .concat("|" + DxfFileConstants.BPA_APPROVAL_BY_AN_ACCREDITED_PERSON));
	            }

	            if (pl.getPlanInformation().isLowRiskBuilding()
	                    && pl.getVirtualBuilding().getTotalBuitUpArea().compareTo(BigDecimal.valueOf(500)) < 0) {
	                pl.getPlanInformation().setBusinessService(pl.getPlanInformation().getBusinessService()
	                        .concat("|" + DxfFileConstants.BPA_CATEGORY_A_MODULE_CODE));
	            }
	        }
	    }
	}
	
	private static void setBusinessServiceOC(Plan pl, Double buildingHeight, Double plotArea, boolean isSpecialBuilding) {
	    OdishaUlbs ulb = OdishaUlbs.getUlb(pl.getThirdPartyUserTenantld());

	    if (buildingHeight != null && plotArea != null) {
	        String buildingHeightService = determineServiceByBuildingHeightOC(buildingHeight, isSpecialBuilding);
	        String plotAreaService = determineServiceByPlotAreaOC(plotArea, ulb, isSpecialBuilding);

	        String finalService = getHigherPriorityServiceOC(buildingHeightService, plotAreaService);
	        pl.getPlanInformation().setBusinessService(finalService);

	        if (!isSpecialBuilding && finalService.equals(DxfFileConstants.BPA_OC_PA_MODULE_CODE)) {
	            if (BPA5_FLAGE && isPlanApplicableForAccreditedWorkflow(pl, buildingHeight, plotArea)) {
	                pl.getPlanInformation().setBusinessService(pl.getPlanInformation().getBusinessService()
	                        .concat("|" + DxfFileConstants.BPA_APPROVAL_BY_AN_ACCREDITED_PERSON));
	            }
	        }
	    }
	}
	
	private static void updateSubOccupancy(Plan pl) {
		OccupancyTypeHelper occupancyTypeHelper=pl.getVirtualBuilding().getMostRestrictiveFarHelper();
		if(occupancyTypeHelper!=null && occupancyTypeHelper.getSubtype()!=null)
			pl.getPlanInformation().setSubOccupancy(occupancyTypeHelper.getSubtype().getName());
	}
	
	private static boolean isPlanApplicableForAccreditedWorkflow(Plan pl, Double buildingHeight, Double plotArea) {
		boolean flage=false;
		if(DxfFileConstants.YES.equals(pl.getPlanInformation().getApprovedLayoutDeclaration()) && (buildingHeight <= 10) && (plotArea <= 500) && !isBasmentPersent(pl)) {
			flage=true;
		}
		return flage;
	}
	
	private static boolean isBasmentPersent(Plan pl) {
		for(Block block:pl.getBlocks()) {
			if(block.getBuilding().getFloorNumber(-1)!=null)
				return true;
		}
		return false;
	}
	
	private static String determineServiceByBuildingHeight(Double buildingHeight, boolean isSpecialBuilding) {
	    if (isSpecialBuilding) {
	        if (buildingHeight <= 15) {
	            return DxfFileConstants.BPA_PO_MODULE_CODE;
	        } else if (buildingHeight > 15 && buildingHeight <= 30) {
	            return DxfFileConstants.BPA_PM_MODULE_CODE;
	        } else {
	            return DxfFileConstants.BPA_DP_BP_MODULE_CODE;
	        }
	    } else {
	        if (buildingHeight <= 10) {
	            return DxfFileConstants.BPA_PA_MODULE_CODE;
	        } else if (buildingHeight > 10 && buildingHeight <= 15) {
	            return DxfFileConstants.BPA_PO_MODULE_CODE;
	        } else if (buildingHeight > 15 && buildingHeight <= 30) {
	            return DxfFileConstants.BPA_PM_MODULE_CODE;
	        } else {
	            return DxfFileConstants.BPA_DP_BP_MODULE_CODE;
	        }
	    }
	}
	
	private static String determineServiceByPlotArea(Double plotArea, OdishaUlbs ulb, boolean isSpecialBuilding) {
		if (plotArea <= 500) {
			return DxfFileConstants.BPA_PA_MODULE_CODE;
		} else if (plotArea > 500 && plotArea <= 4047) {
			return DxfFileConstants.BPA_PO_MODULE_CODE;
		} else if (plotArea > 4047 && (ulb.isSparitFlag() ? plotArea <= 20000 : plotArea <= 10000)) {
			return DxfFileConstants.BPA_PM_MODULE_CODE;
		} else {
			return DxfFileConstants.BPA_DP_BP_MODULE_CODE;
		}

	}
	
	private static String getHigherPriorityService(String service1, String service2) {
	    // Business service priority mapping
	    Map<String, Integer> servicePriority = new HashMap<>();
	    servicePriority.put(DxfFileConstants.BPA_PA_MODULE_CODE, 1);
	    servicePriority.put(DxfFileConstants.BPA_PO_MODULE_CODE, 2);
	    servicePriority.put(DxfFileConstants.BPA_PM_MODULE_CODE, 3);
	    servicePriority.put(DxfFileConstants.BPA_DP_BP_MODULE_CODE, 4);

	    // Compare priorities and return the higher one
	    return servicePriority.get(service1) >= servicePriority.get(service2) ? service1 : service2;
	}
	
	
	private static String determineServiceByBuildingHeightOC(Double buildingHeight, boolean isSpecialBuilding) {
	    if (isSpecialBuilding) {
	        if (buildingHeight <= 15) {
	            return DxfFileConstants.BPA_OC_PO_MODULE_CODE;
	        } else if (buildingHeight > 15 && buildingHeight <= 30) {
	            return DxfFileConstants.BPA_OC_PM_MODULE_CODE;
	        } else {
	            return DxfFileConstants.BPA_OC_DP_BP_MODULE_CODE;
	        }
	    } else {
	        if (buildingHeight <= 10) {
	            return DxfFileConstants.BPA_OC_PA_MODULE_CODE;
	        } else if (buildingHeight > 10 && buildingHeight <= 15) {
	            return DxfFileConstants.BPA_OC_PO_MODULE_CODE;
	        } else if (buildingHeight > 15 && buildingHeight <= 30) {
	            return DxfFileConstants.BPA_OC_PM_MODULE_CODE;
	        } else {
	            return DxfFileConstants.BPA_OC_DP_BP_MODULE_CODE;
	        }
	    }
	}
	
	private static String determineServiceByPlotAreaOC(Double plotArea, OdishaUlbs ulb, boolean isSpecialBuilding) {

		if (plotArea <= 500) {
			return DxfFileConstants.BPA_OC_PA_MODULE_CODE;
		} else if (plotArea > 500 && plotArea <= 4047) {
			return DxfFileConstants.BPA_OC_PO_MODULE_CODE;
		} else if (plotArea > 4047 && (ulb.isSparitFlag() ? plotArea <= 20000 : plotArea <= 10000)) {
			return DxfFileConstants.BPA_OC_PM_MODULE_CODE;
		} else {
			return DxfFileConstants.BPA_OC_DP_BP_MODULE_CODE;
		}

	}

	private static String getHigherPriorityServiceOC(String service1, String service2) {
	    // Business service priority mapping
	    Map<String, Integer> servicePriority = new HashMap<>();
	    servicePriority.put(DxfFileConstants.BPA_OC_PA_MODULE_CODE, 1);
	    servicePriority.put(DxfFileConstants.BPA_OC_PO_MODULE_CODE, 2);
	    servicePriority.put(DxfFileConstants.BPA_OC_PM_MODULE_CODE, 3);
	    servicePriority.put(DxfFileConstants.BPA_OC_DP_BP_MODULE_CODE, 4);

	    // Compare priorities and return the higher one
	    return servicePriority.get(service1) >= servicePriority.get(service2) ? service1 : service2;
	}
}