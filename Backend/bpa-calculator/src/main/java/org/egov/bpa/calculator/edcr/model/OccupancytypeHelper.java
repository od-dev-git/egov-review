package org.egov.bpa.calculator.edcr.model;

import org.springframework.validation.annotation.Validated;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;




@Validated
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class OccupancytypeHelper {
	
	@JsonProperty("type")
	private Occupancytype  occupancytype=null;
	
	@JsonProperty("subtype")
	private OccupancySubType  occupancySubType=null;
	
	@JsonProperty("usage")
	private String usage = null;
	
	@JsonProperty("convertedType")
	private String convertedType = null;
	
	@JsonProperty("convertedSubtype")
	private String convertedSubtype = null;
	
	@JsonProperty("convertedUsage")
	private String convertedUsage = null;
 

}
