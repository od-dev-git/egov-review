package org.egov.edcr.contract.oc;

import com.fasterxml.jackson.annotation.JsonProperty;


import java.math.BigDecimal;


public class PlotDetails {

    @JsonProperty("plotArea")
    BigDecimal plotArea;

    @JsonProperty("giftedLandArea")
    BigDecimal giftedLandArea;

    @JsonProperty("netPlotArea")
    BigDecimal netPlotArea;

	public BigDecimal getPlotArea() {
		return plotArea;
	}

	public void setPlotArea(BigDecimal plotArea) {
		this.plotArea = plotArea;
	}

	public BigDecimal getGiftedLandArea() {
		return giftedLandArea;
	}

	public void setGiftedLandArea(BigDecimal giftedLandArea) {
		this.giftedLandArea = giftedLandArea;
	}

	public BigDecimal getNetPlotArea() {
		return netPlotArea;
	}

	public void setNetPlotArea(BigDecimal netPlotArea) {
		this.netPlotArea = netPlotArea;
	}

	public PlotDetails(BigDecimal plotArea, BigDecimal giftedLandArea, BigDecimal netPlotArea) {
		super();
		this.plotArea = plotArea;
		this.giftedLandArea = giftedLandArea;
		this.netPlotArea = netPlotArea;
	}

	public PlotDetails() {
		super();
	}
    
    
}
