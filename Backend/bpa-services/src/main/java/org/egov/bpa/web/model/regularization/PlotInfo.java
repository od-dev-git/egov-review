package org.egov.bpa.web.model.regularization;

import org.egov.bpa.web.model.AuditDetails;

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
public class PlotInfo {
	
	@JsonProperty("id")
	@Default
	private String id=null;
	
	@JsonProperty("tenantId")
	@Default
	private String tenantId=null;
	
	@JsonProperty("landInfoId")
	@Default
	private String landInfoId=null;
	
	@JsonProperty("district")
	@Default
	private String district=null;
	
	@JsonProperty("tehsil")
	@Default
	private String tehsil=null;
	
	@JsonProperty("village")
	@Default
	private String village=null;

	@JsonProperty("plotNo")
	@Default
	private String plotNo=null;
	
	@JsonProperty("subPlotNo")
	@Default
	private String subPlotNo=null;
	
	@JsonProperty("subSubPlotNo")
	@Default
	private String subSubPlotNo=null;
	
	@JsonProperty("plotArea")
	@Default
	private String plotArea=null;
	
	@JsonProperty("khata")
	@Default
	private String khata=null;
	
	@JsonProperty("kisam")
	@Default
	private String kisam=null;
	
	@JsonProperty("landOwnerName")
	@Default
	private String landOwnerName=null;
	
	@JsonProperty("gpaHolderName")
	@Default
	private String gpaHolderName=null;
	
	@JsonProperty("saleDeedNo")
	@Default
	private String saleDeedNo=null;
	
	@JsonProperty("saleDeedDate")
	@Default
	private Long saleDeedDate=null;
	
	@JsonProperty("bmvValue")
	@Default
	private String bmvValue=null;
	
	@JsonProperty("isPlotToBeGifted")
	@Default
	private String isPlotToBeGifted=null;
	
	@JsonProperty("areaToBeGifted")
	@Default
	private String areaToBeGifted=null;
	
	@JsonProperty("reasonForGift")
	@Default
	private String reasonForGift=null;
	
	@JsonProperty("auditDetails")
	@Default
	private AuditDetails auditDetails = null;
	
	@JsonProperty("isDeleted")
	@Default
	private Boolean isDeleted = Boolean.TRUE;

}
