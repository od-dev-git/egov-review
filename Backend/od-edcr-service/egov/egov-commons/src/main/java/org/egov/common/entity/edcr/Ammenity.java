package org.egov.common.entity.edcr;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Ammenity implements Serializable{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private List<Measurement> guardRooms=new ArrayList<>();
	private List<Measurement> electricCabins=new ArrayList<>();
	private List<Measurement> subStations=new ArrayList<>();
	private List<Measurement> AreaForGeneratorSet=new ArrayList<>();
	private List<Measurement> atms=new ArrayList<>();
	private List<Measurement> otherAmmenities=new ArrayList<>();
	public List<Measurement> getGuardRooms() {
		return guardRooms;
	}
	public void setGuardRooms(List<Measurement> guardRooms) {
		this.guardRooms = guardRooms;
	}
	public List<Measurement> getElectricCabins() {
		return electricCabins;
	}
	public void setElectricCabins(List<Measurement> electricCabins) {
		this.electricCabins = electricCabins;
	}
	public List<Measurement> getSubStations() {
		return subStations;
	}
	public void setSubStations(List<Measurement> subStations) {
		this.subStations = subStations;
	}
	public List<Measurement> getAreaForGeneratorSet() {
		return AreaForGeneratorSet;
	}
	public void setAreaForGeneratorSet(List<Measurement> areaForGeneratorSet) {
		AreaForGeneratorSet = areaForGeneratorSet;
	}
	public List<Measurement> getAtms() {
		return atms;
	}
	public void setAtms(List<Measurement> atms) {
		this.atms = atms;
	}
	public List<Measurement> getOtherAmmenities() {
		return otherAmmenities;
	}
	public void setOtherAmmenities(List<Measurement> otherAmmenities) {
		this.otherAmmenities = otherAmmenities;
	}
	

	
}
