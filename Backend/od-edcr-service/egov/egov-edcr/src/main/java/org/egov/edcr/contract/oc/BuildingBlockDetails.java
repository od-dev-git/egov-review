package org.egov.edcr.contract.oc;



import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.List;



public class BuildingBlockDetails {

	@JsonProperty("buildingHeight")
    BigDecimal buildingHeight;

	@JsonProperty("floors")
    List<Floor> floors;

	@JsonProperty("setbacks")
    List<Setback> setbacks;

    @JsonProperty("isSpecialBuilding")
    Boolean isSpecialBuilding;

	public BigDecimal getBuildingHeight() {
		return buildingHeight;
	}

	public void setBuildingHeight(BigDecimal buildingHeight) {
		this.buildingHeight = buildingHeight;
	}

	public List<Floor> getFloors() {
		return floors;
	}

	public void setFloors(List<Floor> floors) {
		this.floors = floors;
	}

	public List<Setback> getSetbacks() {
		return setbacks;
	}

	public void setSetbacks(List<Setback> setbacks) {
		this.setbacks = setbacks;
	}

	public Boolean getIsSpecialBuilding() {
		return isSpecialBuilding;
	}

	public void setIsSpecialBuilding(Boolean isSpecialBuilding) {
		this.isSpecialBuilding = isSpecialBuilding;
	}

	public BuildingBlockDetails() {
		super();
	}

	public BuildingBlockDetails(BigDecimal buildingHeight, List<Floor> floors, List<Setback> setbacks,
			Boolean isSpecialBuilding) {
		super();
		this.buildingHeight = buildingHeight;
		this.floors = floors;
		this.setbacks = setbacks;
		this.isSpecialBuilding = isSpecialBuilding;
	}
    
    

}
