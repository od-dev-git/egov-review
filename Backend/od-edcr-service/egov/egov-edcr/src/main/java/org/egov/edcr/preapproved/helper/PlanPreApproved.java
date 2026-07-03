package org.egov.edcr.preapproved.helper;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class PlanPreApproved implements Serializable {

	private static final long serialVersionUID = 1235517727309489492L;

	@JsonProperty("plotNo")
	private String plotNo;

	@JsonProperty("khataNo")
	private String khataNo;

	@JsonProperty("plotArea")
	private BigDecimal 	plotArea;

	@JsonProperty("serviceType")
	private String 	serviceType;


	@JsonProperty("floorDescription")
	private String 	floorInfo;

	@JsonProperty("subOccupancy")
	private String 	subOccupancy;


	@JsonProperty("roadWidth")
	private BigDecimal 	roadWidth;

	@JsonProperty("totalBuitUpArea")
	private BigDecimal 	totalBuitUpArea;


	@JsonProperty("totalfloorArea")
	private BigDecimal 	totalfloorArea;


	@JsonProperty("providedFar")
	private double	providedFar;

	

	public String getPlotNo() {
		return plotNo;
	}

	public void setPlotNo(String plotNo) {
		this.plotNo = plotNo;
	}

	public String getKhataNo() {
		return khataNo;
	}

	public void setKhataNo(String khataNo) {
		this.khataNo = khataNo;
	}

	public BigDecimal getPlotArea() {
		return plotArea;
	}

	public void setPlotArea(BigDecimal plotArea) {
		this.plotArea = plotArea;
	}

	public String getServiceType() {
		return serviceType;
	}

	public void setServiceType(String serviceType) {
		this.serviceType = serviceType;
	}

	public String getFloorInfo() {
		return floorInfo;
	}

	public void setFloorInfo(String floorInfo) {
		this.floorInfo = floorInfo;
	}

	public String getSubOccupancy() {
		return subOccupancy;
	}

	public void setSubOccupancy(String subOccupancy) {
		this.subOccupancy = subOccupancy;
	}

	public BigDecimal getRoadWidth() {
		return roadWidth;
	}

	public void setRoadWidth(BigDecimal roadWidth) {
		this.roadWidth = roadWidth;
	}

	public BigDecimal getTotalBuitUpArea() {
		return totalBuitUpArea;
	}

	public void setTotalBuitUpArea(BigDecimal totalBuitUpArea) {
		this.totalBuitUpArea = totalBuitUpArea;
	}

	public BigDecimal getTotalfloorArea() {
		return totalfloorArea;
	}

	public void setTotalfloorArea(BigDecimal totalfloorArea) {
		this.totalfloorArea = totalfloorArea;
	}



	public double getProvidedFar() {
		return providedFar;
	}

	public void setProvidedFar(double providedFar) {
		this.providedFar = providedFar;
	}

	







}
