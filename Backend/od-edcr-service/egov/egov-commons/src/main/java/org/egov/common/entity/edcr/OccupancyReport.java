package org.egov.common.entity.edcr;

import java.math.BigDecimal;

public class OccupancyReport {
	
	private String byeLaw;
	
	private String occupancy;
	
	private String subOccupancy;
	
	private BigDecimal percentage;
	
	private BigDecimal buildUpArea;
	
	private BigDecimal floorArea;
	
	private BigDecimal carpetArea;


	public BigDecimal getPercentage() {
		return percentage;
	}

	public void setPercentage(BigDecimal percentage) {
		this.percentage = percentage;
	}

	public String getSubOccupancy() {
		return subOccupancy;
	}

	public void setSubOccupancy(String subOccupancy) {
		this.subOccupancy = subOccupancy;
	}

	public String getOccupancy() {
		return occupancy;
	}

	public void setOccupancy(String occupancy) {
		this.occupancy = occupancy;
	}

	public BigDecimal getBuildUpArea() {
		return buildUpArea;
	}

	public void setBuildUpArea(BigDecimal buildUpArea) {
		this.buildUpArea = buildUpArea;
	}

	public BigDecimal getFloorArea() {
		return floorArea;
	}

	public void setFloorArea(BigDecimal floorArea) {
		this.floorArea = floorArea;
	}

	public BigDecimal getCarpetArea() {
		return carpetArea;
	}

	public void setCarpetArea(BigDecimal carpetArea) {
		this.carpetArea = carpetArea;
	}

	public String getByeLaw() {
		return byeLaw;
	}

	public void setByeLaw(String byeLaw) {
		this.byeLaw = byeLaw;
	}

	
}
