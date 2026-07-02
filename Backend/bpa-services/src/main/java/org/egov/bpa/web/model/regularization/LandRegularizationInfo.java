package org.egov.bpa.web.model.regularization;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.egov.bpa.web.model.AuditDetails;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class LandRegularizationInfo {
	
	@JsonProperty("id")
	@Default
	private String id=null;
	
	@JsonProperty("tenantId")
	@Default
	private String tenantId=null;
	
	@JsonProperty("regularizationId")
	@Default
	private String regularizationId=null;
	
	@JsonProperty("landRegularizationType")
	@Default
	private String landRegularizationType=null;
	
	@JsonProperty("plotInfo")
	@Default
	private List<PlotInfo> plotInfo=null;
	
	@JsonProperty("newPlotInfo")
	@Default
	private List<PlotInfo> newPlotInfo=null;
	
	@JsonProperty("totalPlotArea")
	@Default
	private BigDecimal totalPlotArea=null;

	@JsonProperty("accessRoadWidth")
	@Default
	private BigDecimal accessRoadWidth=null;
	
	@JsonProperty("additionalDetails")
	@Default
	private Object additionalDetails=null;

	@JsonProperty("auditDetails")
	@Default
	private AuditDetails auditDetails = null;
	
	public LandRegularizationInfo addPlotInfo(PlotInfo plot) {
        if(CollectionUtils.isEmpty(this.plotInfo)) {
            this.plotInfo = new ArrayList<>();
        }

        if (!ObjectUtils.isEmpty(plot))
            this.plotInfo.add(plot);
        return this;
    }

}
