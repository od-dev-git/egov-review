package org.egov.edcr.entity.dto.plan;

import java.math.BigDecimal;
import java.util.List;

public class BuildingDTO {
    private BigDecimal width;
    private BigDecimal area;
    private BigDecimal buildingHeight;
    private BigDecimal totalFloors;
    private List<FloorDTO> floors;
    private List<OccupancyTypeHelperDTO> occupancies;

    public BuildingDTO(BigDecimal width, BigDecimal area, BigDecimal buildingHeight, BigDecimal totalFloors, List<FloorDTO> floors,
                       List<OccupancyTypeHelperDTO> occupancies) {
        this.width = width;
        this.area = area;
        this.buildingHeight = buildingHeight;
        this.totalFloors = totalFloors;
        this.floors = floors;
        this.occupancies = occupancies;
    }

	public BigDecimal getWidth() {
		return width;
	}

	public void setWidth(BigDecimal width) {
		this.width = width;
	}

	public BigDecimal getArea() {
		return area;
	}

	public void setArea(BigDecimal area) {
		this.area = area;
	}

	public BigDecimal getBuildingHeight() {
		return buildingHeight;
	}

	public void setBuildingHeight(BigDecimal buildingHeight) {
		this.buildingHeight = buildingHeight;
	}

	public BigDecimal getTotalFloors() {
		return totalFloors;
	}

	public void setTotalFloors(BigDecimal totalFloors) {
		this.totalFloors = totalFloors;
	}

	public List<FloorDTO> getFloors() {
		return floors;
	}

	public void setFloors(List<FloorDTO> floors) {
		this.floors = floors;
	}

	public List<OccupancyTypeHelperDTO> getOccupancies() {
		return occupancies;
	}

	public void setOccupancies(List<OccupancyTypeHelperDTO> occupancies) {
		this.occupancies = occupancies;
	}
}
