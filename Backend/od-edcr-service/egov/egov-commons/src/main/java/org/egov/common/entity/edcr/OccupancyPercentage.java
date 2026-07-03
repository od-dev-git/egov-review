package org.egov.common.entity.edcr;

import java.math.BigDecimal;

public class OccupancyPercentage {
	
	private String occupancy;
	
	private String subOccupancy;
	
	private BigDecimal totalBuildUpArea;
	
	private BigDecimal percentage;
	
	private BigDecimal totalFloorArea;
	
	private BigDecimal totalCarpetArea;
	
	private String occupancyCode;
	
	private String subOccupancyCode;

	public String getOccupancy() {
		return occupancy;
	}

	public void setOccupancy(String occupancy) {
		this.occupancy = occupancy;
	}

	public String getSubOccupancy() {
		return subOccupancy;
	}

	public void setSubOccupancy(String subOccupancy) {
		this.subOccupancy = subOccupancy;
	}

	public BigDecimal getTotalBuildUpArea() {
		return totalBuildUpArea;
	}

	public void setTotalBuildUpArea(BigDecimal totalBuildUpArea) {
		this.totalBuildUpArea = totalBuildUpArea;
	}

	public BigDecimal getPercentage() {
		return percentage;
	}

	public void setPercentage(BigDecimal percentage) {
		this.percentage = percentage;
	}

	public BigDecimal getTotalFloorArea() {
		return totalFloorArea;
	}

	public void setTotalFloorArea(BigDecimal totalFloorArea) {
		this.totalFloorArea = totalFloorArea;
	}

	public BigDecimal getTotalCarpetArea() {
		return totalCarpetArea;
	}

	public void setTotalCarpetArea(BigDecimal totalCarpetArea) {
		this.totalCarpetArea = totalCarpetArea;
	}

	public String getOccupancyCode() {
		return occupancyCode;
	}

	public void setOccupancyCode(String occupancyCode) {
		this.occupancyCode = occupancyCode;
	}

	public String getSubOccupancyCode() {
		return subOccupancyCode;
	}

	public void setSubOccupancyCode(String subOccupancyCode) {
		this.subOccupancyCode = subOccupancyCode;
	}

	
	
}
