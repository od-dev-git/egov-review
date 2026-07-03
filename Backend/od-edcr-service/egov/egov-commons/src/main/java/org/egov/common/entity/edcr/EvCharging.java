package org.egov.common.entity.edcr;

import java.util.ArrayList;
import java.util.List;

public class EvCharging {
	
	private List<Measurement> measurements = new ArrayList<>();
	
	public List<Measurement> getMeasurements() {
		return measurements;
	}
	public void setMeasurements(List<Measurement> measurements) {
		this.measurements = measurements;
	}
	
	
}
