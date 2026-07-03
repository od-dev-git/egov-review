package org.egov.edcr.entity.dto.plan;

import java.math.BigDecimal;

public class PlotDTO {
    private BigDecimal area;
    
    public PlotDTO(BigDecimal area) {
        this.area = area;
    }

	public BigDecimal getArea() {
		return area;
	}

	public void setArea(BigDecimal area) {
		this.area = area;
	}
}
