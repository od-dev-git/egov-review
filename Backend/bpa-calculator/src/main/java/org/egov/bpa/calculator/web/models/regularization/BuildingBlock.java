package org.egov.bpa.calculator.web.models.regularization;

import java.util.ArrayList;
import java.util.List;

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
public class BuildingBlock {

	@JsonProperty("height")
	private String height;
	
	@JsonProperty("isSpecialBuilding")
	private boolean isSpecialBuilding;
	
	@JsonProperty("floors")
	@Default
	private List<BlockFloor> floors = new ArrayList<>();
	
	@JsonProperty("setback")
	@Default
	private List<BlockSetback> setback = new ArrayList<>();

}
