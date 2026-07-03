package org.egov.edcr.od;

import static org.egov.edcr.constants.DxfFileConstants.MIXED_USE_COLOR_CODE;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.egov.common.entity.bpa.SubOccupancy;
import org.egov.common.entity.bpa.Usage;
import org.egov.common.entity.dcr.helper.OccupancyHelperDetail;
import org.egov.common.entity.edcr.OccupancyPercentage;
import org.egov.common.entity.edcr.OccupancyTypeHelper;
import org.egov.common.entity.edcr.Plan;
import org.egov.edcr.constants.DxfFileConstants;
import org.egov.edcr.feature.ProvisionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class OdishaMixedUseUtill {

	@Autowired
	private ProvisionService provisionService;

	public OccupancyTypeHelper getMisxedUseOccupancyTypeHelper(Plan pl) {
		OccupancyTypeHelper oth = new OccupancyTypeHelper();
		if (!pl.getUsagesMaster().isEmpty() && pl.getUsagesMaster().containsKey(MIXED_USE_COLOR_CODE)) {
			Usage usage = pl.getUsagesMaster().get(MIXED_USE_COLOR_CODE);
			OccupancyHelperDetail usageTypeDtl = new OccupancyHelperDetail();
			usageTypeDtl.setColor(MIXED_USE_COLOR_CODE);
			usageTypeDtl.setCode(usage.getCode());
			usageTypeDtl.setName(usage.getName());
			oth.setUsage(usageTypeDtl);
			SubOccupancy subOcc = usage.getSubOccupancy();
			OccupancyHelperDetail occSubTypeDtl = new OccupancyHelperDetail();
			occSubTypeDtl.setCode(subOcc.getCode());
			occSubTypeDtl.setName(subOcc.getName());
			oth.setSubtype(occSubTypeDtl);
			OccupancyHelperDetail occTypeDtl = new OccupancyHelperDetail();
			org.egov.common.entity.bpa.Occupancy occ = subOcc.getOccupancy();
			occTypeDtl.setCode(occ.getCode());
			occTypeDtl.setName(occ.getName());
			oth.setType(occTypeDtl);
		}
		if (!pl.getSubOccupanciesMaster().isEmpty() && pl.getSubOccupanciesMaster().containsKey(MIXED_USE_COLOR_CODE)) {
			SubOccupancy subOcc = pl.getSubOccupanciesMaster().get(MIXED_USE_COLOR_CODE);
			OccupancyHelperDetail occSubTypeDtl = new OccupancyHelperDetail();
			occSubTypeDtl.setColor(MIXED_USE_COLOR_CODE);
			occSubTypeDtl.setCode(subOcc.getCode());
			occSubTypeDtl.setName(subOcc.getName());
			oth.setSubtype(occSubTypeDtl);
			OccupancyHelperDetail occTypeDtl = new OccupancyHelperDetail();
			org.egov.common.entity.bpa.Occupancy occ = subOcc.getOccupancy();
			occTypeDtl.setCode(occ.getCode());
			occTypeDtl.setName(occ.getName());
			oth.setType(occTypeDtl);
		}
		if (!pl.getOccupanciesMaster().isEmpty() && pl.getOccupanciesMaster().containsKey(MIXED_USE_COLOR_CODE)) {
			org.egov.common.entity.bpa.Occupancy occ = pl.getOccupanciesMaster().get(MIXED_USE_COLOR_CODE);
			OccupancyHelperDetail occTypeDtl = new OccupancyHelperDetail();
			occTypeDtl.setColor(MIXED_USE_COLOR_CODE);
			occTypeDtl.setCode(occ.getCode());
			occTypeDtl.setName(occ.getName());
			oth.setType(occTypeDtl);
		}

		return oth;
	}

	public OccupancyTypeHelper getOccupancyTypeHelper(int colorCode, Plan pl) {
		OccupancyTypeHelper oth = new OccupancyTypeHelper();
		if (!pl.getUsagesMaster().isEmpty() && pl.getUsagesMaster().containsKey(colorCode)) {
			Usage usage = pl.getUsagesMaster().get(colorCode);
			OccupancyHelperDetail usageTypeDtl = new OccupancyHelperDetail();
			usageTypeDtl.setColor(colorCode);
			usageTypeDtl.setCode(usage.getCode());
			usageTypeDtl.setName(usage.getName());
			oth.setUsage(usageTypeDtl);
			SubOccupancy subOcc = usage.getSubOccupancy();
			OccupancyHelperDetail occSubTypeDtl = new OccupancyHelperDetail();
			occSubTypeDtl.setCode(subOcc.getCode());
			occSubTypeDtl.setName(subOcc.getName());
			oth.setSubtype(occSubTypeDtl);
			OccupancyHelperDetail occTypeDtl = new OccupancyHelperDetail();
			org.egov.common.entity.bpa.Occupancy occ = subOcc.getOccupancy();
			occTypeDtl.setCode(occ.getCode());
			occTypeDtl.setName(occ.getName());
			oth.setType(occTypeDtl);
		}
		if (!pl.getSubOccupanciesMaster().isEmpty() && pl.getSubOccupanciesMaster().containsKey(colorCode)) {
			SubOccupancy subOcc = pl.getSubOccupanciesMaster().get(colorCode);
			OccupancyHelperDetail occSubTypeDtl = new OccupancyHelperDetail();
			occSubTypeDtl.setColor(colorCode);
			occSubTypeDtl.setCode(subOcc.getCode());
			occSubTypeDtl.setName(subOcc.getName());
			oth.setSubtype(occSubTypeDtl);
			OccupancyHelperDetail occTypeDtl = new OccupancyHelperDetail();
			org.egov.common.entity.bpa.Occupancy occ = subOcc.getOccupancy();
			occTypeDtl.setCode(occ.getCode());
			occTypeDtl.setName(occ.getName());
			oth.setType(occTypeDtl);
		}
		if (!pl.getOccupanciesMaster().isEmpty() && pl.getOccupanciesMaster().containsKey(colorCode)) {
			org.egov.common.entity.bpa.Occupancy occ = pl.getOccupanciesMaster().get(colorCode);
			OccupancyHelperDetail occTypeDtl = new OccupancyHelperDetail();
			occTypeDtl.setColor(colorCode);
			occTypeDtl.setCode(occ.getCode());
			occTypeDtl.setName(occ.getName());
			oth.setType(occTypeDtl);
		}

		return oth;
	}

	public boolean allowedProvision(Set<OccupancyTypeHelper> distinctOccupancyTypes, Plan pl) {
		boolean isUnderTheProvision = false;
		List<String> subOccupancyCode = distinctOccupancyTypes.stream().filter(ot -> ot!=null && ot.getType()!=null && !DxfFileConstants.OC_ADDITIONAL_FEATURE.equals(ot.getType().getCode()))
				.map(oth -> (oth != null && oth.getSubtype() != null) ? oth.getSubtype().getCode() : null)
				.collect(Collectors.toList());
		
		if(subOccupancyCode.size()==1) {
			isUnderTheProvision = true;
			return isUnderTheProvision;
		}
		
		//getMostOccupancyPercentage -> this method gives the occupancy with most builtup area.
		OccupancyPercentage occupancyPercentage = getMostOccupancyPercentage(
				pl.getPlanInformation().getOccupancyPercentages());

		if (DxfFileConstants.APARTMENT_BUILDING.equals(occupancyPercentage.getSubOccupancyCode())
				|| DxfFileConstants.HOUSING_PROJECT.equals(occupancyPercentage.getSubOccupancyCode())
				|| DxfFileConstants.STUDIO_APARTMENTS.equals(occupancyPercentage.getSubOccupancyCode())
				|| DxfFileConstants.LOW_INCOME_HOUSING.equals(occupancyPercentage.getSubOccupancyCode())
				|| DxfFileConstants.MEDIUM_INCOME_HOUSING.equals(occupancyPercentage.getSubOccupancyCode())
				|| DxfFileConstants.SHELTER_HOUSE.equals(occupancyPercentage.getSubOccupancyCode())
				|| DxfFileConstants.EWS.equals(occupancyPercentage.getSubOccupancyCode())) {
			
			isUnderTheProvision = provisionService.isCommercialActivityPermisibleForRes(pl);
			subOccupancyCode.remove(DxfFileConstants.APARTMENT_BUILDING);
			subOccupancyCode.remove(DxfFileConstants.HOUSING_PROJECT);
			subOccupancyCode.remove(DxfFileConstants.STUDIO_APARTMENTS);
			subOccupancyCode.remove(DxfFileConstants.LOW_INCOME_HOUSING);
			subOccupancyCode.remove(DxfFileConstants.MEDIUM_INCOME_HOUSING);
			subOccupancyCode.remove(DxfFileConstants.SHELTER_HOUSE);
			subOccupancyCode.remove(DxfFileConstants.EWS);
			subOccupancyCode.removeAll(provisionService.getCommercialSubOccupancies());
			if (isUnderTheProvision && subOccupancyCode.size() > 0)
				isUnderTheProvision = false;
			
		} else if (DxfFileConstants.PLOTTED_DETACHED_OR_INDIVIDUAL_RESIDENTIAL_BUILDING
				.equals(occupancyPercentage.getSubOccupancyCode())
				|| DxfFileConstants.ROW_HOUSING.equals(occupancyPercentage.getSubOccupancyCode())
				|| DxfFileConstants.SEMI_DETACHED.equals(occupancyPercentage.getSubOccupancyCode())
				|| DxfFileConstants.PROFESSIONAL_OFFICES.equals(occupancyPercentage.getSubOccupancyCode())) {

			Map<String, OccupancyPercentage> occupancyPercentages = pl.getPlanInformation().getOccupancyPercentages();

			if (occupancyPercentages.containsKey("Professional offices")
					&& (occupancyPercentages.containsKey("Residential Plotted")
							|| occupancyPercentages.containsKey("Semi-detached")
							|| occupancyPercentages.containsKey("Row housing"))) {

				if (pl.getPlanInformation().getIsProjectComingUnderWorkCumResidential() != null && 
						pl.getPlanInformation().getIsProjectComingUnderWorkCumResidential().equalsIgnoreCase("YES")) {

					isUnderTheProvision = true;
				}

			}
			
			subOccupancyCode.removeAll(provisionService.getWorkCumResidentialOccupancies());
			
		} else {
			if (DxfFileConstants.OC_COMMERCIAL.equals(occupancyPercentage.getOccupancyCode())) {
				isUnderTheProvision = provisionService.provisionForCommercial(pl, occupancyPercentage);

				if (DxfFileConstants.HOTEL.equals(occupancyPercentage.getSubOccupancyCode())
						|| DxfFileConstants.FIVE_STAR_HOTEL.equals(occupancyPercentage.getSubOccupancyCode())) {
					subOccupancyCode.removeAll(provisionService.getOtherAllowedSubOccupanciesInOcCommercial());
					subOccupancyCode.removeAll(provisionService.getCommercialOfficeAndRetailServiceList());
					subOccupancyCode.remove(DxfFileConstants.HOTEL);
					subOccupancyCode.remove(DxfFileConstants.FIVE_STAR_HOTEL);
				} else if(DxfFileConstants.CONVENIENCE_AND_NEIGHBORHOOD_SHOPPING
						.equals(occupancyPercentage.getSubOccupancyCode())
						|| DxfFileConstants.DEPARTMENTAL_STORE.equals(occupancyPercentage.getSubOccupancyCode())
						|| DxfFileConstants.LOCAL_RETAIL_SHOPPING.equals(occupancyPercentage.getSubOccupancyCode())
						|| DxfFileConstants.RESTAURANT.equals(occupancyPercentage.getSubOccupancyCode())) {
					
					subOccupancyCode.removeAll(provisionService.getShopsList());
					subOccupancyCode.removeAll(provisionService.getOtherAllowedSuboccupanciesWithShops());

				}
			} else if (DxfFileConstants.OC_PUBLIC_SEMI_PUBLIC_OR_INSTITUTIONAL
					.equals(occupancyPercentage.getOccupancyCode())) {
				isUnderTheProvision = provisionService.provisionsForPublicSemiOrInstitutional(pl, occupancyPercentage);
				if(DxfFileConstants.CINEMA.equals(occupancyPercentage.getSubOccupancyCode())) {
					subOccupancyCode.remove(DxfFileConstants.CINEMA);
					subOccupancyCode.removeAll(provisionService.getCommercialSubOccupancies());
				}
			} else if (DxfFileConstants.OC_AGRICULTURE.equals(occupancyPercentage.getOccupancyCode())) {
				isUnderTheProvision = provisionService.provisionsForAgriculture(pl, occupancyPercentage);
				if (DxfFileConstants.FARM_HOUSE.equals(occupancyPercentage.getSubOccupancyCode()) ||
						DxfFileConstants.COUNTRY_HOMES.equals(occupancyPercentage.getSubOccupancyCode())) {
					subOccupancyCode.remove(DxfFileConstants.FARM_HOUSE);
					subOccupancyCode.remove(DxfFileConstants.COUNTRY_HOMES);
					subOccupancyCode.removeAll(Arrays.asList(DxfFileConstants.ACCOMODATION_OF_WATCH_AND_WARD_MAINTENANCE_STAFF));
				} 
			}
			if (isUnderTheProvision && subOccupancyCode.size() > 0)
				isUnderTheProvision = false;
			else if(isUnderTheProvision && subOccupancyCode.size()==0)
				isUnderTheProvision = true;
				
		}
		return isUnderTheProvision;
	}

	public OccupancyPercentage getMostOccupancyPercentage(
			Map<String, OccupancyPercentage> occupancyPercentages) {
		OccupancyPercentage occupancyPercentage = new OccupancyPercentage();
		for (Map.Entry<String, OccupancyPercentage> e : occupancyPercentages.entrySet()) {
			if (occupancyPercentage.getPercentage() == null
					|| occupancyPercentage.getPercentage().compareTo(e.getValue().getPercentage()) < 0)
				occupancyPercentage = e.getValue();
		}
		return occupancyPercentage;
	}
	
	public boolean checkAdditionalAllowedProvision(Set<OccupancyTypeHelper> distinctOccupancyTypes, Plan pl) {
		boolean allowedProvision = true;
		
		Map<String, OccupancyPercentage> occupancyPercentages = pl.getPlanInformation().getOccupancyPercentages();
		if(occupancyPercentages.values()!=null && occupancyPercentages.values().size()>0) {
			Set<String> occSet = occupancyPercentages.values().stream().filter(oc -> !DxfFileConstants.OC_ADDITIONAL_FEATURE.equalsIgnoreCase(oc.getOccupancyCode())).map(oc -> oc.getOccupancy()).collect(Collectors.toSet());
			if(occSet != null && occSet.size()>1)
				allowedProvision = false;
		}
		return allowedProvision;
	}
}
