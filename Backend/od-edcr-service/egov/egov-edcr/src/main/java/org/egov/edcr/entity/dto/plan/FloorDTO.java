package org.egov.edcr.entity.dto.plan;

import java.util.List;

public class FloorDTO {

	private List<OccupancyDTO> occupancies;

	public FloorDTO(List<OccupancyDTO> occupancies) {
		this.setOccupancies(occupancies);
	}

	public List<OccupancyDTO> getOccupancies() {
		return occupancies;
	}

	public void setOccupancies(List<OccupancyDTO> occupancies) {
		this.occupancies = occupancies;
	}

}
