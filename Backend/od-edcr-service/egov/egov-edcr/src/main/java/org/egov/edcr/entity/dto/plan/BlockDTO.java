package org.egov.edcr.entity.dto.plan;

public class BlockDTO {
    private BuildingDTO building;

    public BlockDTO(BuildingDTO building) {
        this.building = building;
    }

    public BlockDTO() {
		// TODO Auto-generated constructor stub
	}

	// Getters and setters
    public BuildingDTO getBuilding() {
        return building;
    }

    public void setBuilding(BuildingDTO building) {
        this.building = building;
    }
}
