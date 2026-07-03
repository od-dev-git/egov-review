package org.egov.common.entity.edcr;

import java.util.ArrayList;
import java.util.List;

public class Plantation {

	private List<Measurement> plantations = new ArrayList<>();
	
	private int cutTreeCount;
	private int existingTreeCount;
	private int plantedTreeCount;

	public List<Measurement> getPlantations() {
		return plantations;
	}

	public void setPlantations(List<Measurement> plantations) {
		this.plantations = plantations;
	}

	public int getCutTreeCount() {
		return cutTreeCount;
	}

	public void setCutTreeCount(int cutTreeCount) {
		this.cutTreeCount = cutTreeCount;
	}

	public int getExistingTreeCount() {
		return existingTreeCount;
	}

	public void setExistingTreeCount(int existingTreeCount) {
		this.existingTreeCount = existingTreeCount;
	}

	public int getPlantedTreeCount() {
		return plantedTreeCount;
	}

	public void setPlantedTreeCount(int plantedTreeCount) {
		this.plantedTreeCount = plantedTreeCount;
	}

	

	
}
