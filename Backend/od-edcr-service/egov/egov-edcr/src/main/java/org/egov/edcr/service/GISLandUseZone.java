package org.egov.edcr.service;

public class GISLandUseZone implements Cloneable {
	
	private String byeLaw1;
	private String byeLaw2;
	private String landUseZoneAsPerDeclaration;
	private String landUseZoneAsPerGIS;
	private String dpbpCommitteeReviewOc;
	private String dpbpCommitteeReviewDecription;

	public String getLandUseZoneAsPerDeclaration() {
		return landUseZoneAsPerDeclaration;
	}

	public void setLandUseZoneAsPerDeclaration(String landUseZoneAsPerDeclaration) {
		this.landUseZoneAsPerDeclaration = landUseZoneAsPerDeclaration;
	}

	public String getLandUseZoneAsPerGIS() {
		return landUseZoneAsPerGIS;
	}

	public void setLandUseZoneAsPerGIS(String landUseZoneAsPerGIS) {
		this.landUseZoneAsPerGIS = landUseZoneAsPerGIS;
	}

	public String getDpbpCommitteeReviewOc() {
		return dpbpCommitteeReviewOc;
	}

	public void setDpbpCommitteeReviewOc(String dpbpCommitteeReviewOc) {
		this.dpbpCommitteeReviewOc = dpbpCommitteeReviewOc;
	}

	public String getDpbpCommitteeReviewDecription() {
		return dpbpCommitteeReviewDecription;
	}

	public void setDpbpCommitteeReviewDecription(String dpbpCommitteeReviewDecription) {
		this.dpbpCommitteeReviewDecription = dpbpCommitteeReviewDecription;
	}

	@Override
	protected GISLandUseZone clone() throws CloneNotSupportedException {
		// TODO Auto-generated method stub
		return (GISLandUseZone) super.clone();
	}

	public String getByeLaw1() {
		return byeLaw1;
	}

	public void setByeLaw1(String byeLaw1) {
		this.byeLaw1 = byeLaw1;
	}

	public String getByeLaw2() {
		return byeLaw2;
	}

	public void setByeLaw2(String byeLaw2) {
		this.byeLaw2 = byeLaw2;
	}

}