package org.egov.edcr.entity.dto.plan;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

public class VirtualBuildingDTO {
    private BigDecimal buildingHeight;
    private Set<OccupancyTypeDTO> occupancyTypes;
    private BigDecimal totalBuitUpArea;
    private OccupancyTypeHelperDTO mostRestrictiveFarHelper;

    public VirtualBuildingDTO(BigDecimal buildingHeight, Set<OccupancyTypeDTO> occupancyTypes, BigDecimal totalBuitUpArea,
                              OccupancyTypeHelperDTO mostRestrictiveFarHelper) {
        this.buildingHeight = buildingHeight;
        this.occupancyTypes = occupancyTypes;
        this.totalBuitUpArea = totalBuitUpArea;
        this.mostRestrictiveFarHelper = mostRestrictiveFarHelper;
    }

	public BigDecimal getBuildingHeight() {
		return buildingHeight;
	}

	public void setBuildingHeight(BigDecimal buildingHeight) {
		this.buildingHeight = buildingHeight;
	}

	public Set<OccupancyTypeDTO> getOccupancyTypes() {
		return occupancyTypes;
	}

	public void setOccupancyTypes(Set<OccupancyTypeDTO> occupancyTypes) {
		this.occupancyTypes = occupancyTypes;
	}

	public BigDecimal getTotalBuitUpArea() {
		return totalBuitUpArea;
	}

	public void setTotalBuitUpArea(BigDecimal totalBuitUpArea) {
		this.totalBuitUpArea = totalBuitUpArea;
	}

	public OccupancyTypeHelperDTO getMostRestrictiveFarHelper() {
		return mostRestrictiveFarHelper;
	}

	public void setMostRestrictiveFarHelper(OccupancyTypeHelperDTO mostRestrictiveFarHelper) {
		this.mostRestrictiveFarHelper = mostRestrictiveFarHelper;
	}
}
