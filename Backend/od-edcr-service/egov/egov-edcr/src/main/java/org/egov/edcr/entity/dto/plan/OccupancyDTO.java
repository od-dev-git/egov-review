package org.egov.edcr.entity.dto.plan;

public class OccupancyDTO {
    private OccupancyTypeHelperDTO typeHelper;

    public OccupancyDTO(OccupancyTypeHelperDTO typeHelper) {
        this.setTypeHelper(typeHelper);
    }

	public OccupancyTypeHelperDTO getTypeHelper() {
		return typeHelper;
	}

	public void setTypeHelper(OccupancyTypeHelperDTO typeHelper) {
		this.typeHelper = typeHelper;
	}
}
