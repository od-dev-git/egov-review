package org.egov.common.entity.edcr;

import java.math.BigDecimal;
import java.util.List;

public class AffectedLandArea {

	private int colorCode;
	private String name;
	private List<Measurement> measurements;
	private List<BigDecimal> widthDimensions;
	public int getColorCode() {
		return colorCode;
	}
	public void setColorCode(int colorCode) {
		this.colorCode = colorCode;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public List<Measurement> getMeasurements() {
		return measurements;
	}
	public void setMeasurements(List<Measurement> measurements) {
		this.measurements = measurements;
	}
	public List<BigDecimal> getWidthDimensions() {
		return widthDimensions;
	}
	public void setWidthDimensions(List<BigDecimal> widthDimensions) {
		this.widthDimensions = widthDimensions;
	}

	

}
