package org.egov.bpa.web.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class BPAVillage {
	
	@JsonProperty("applicatioNo")
	private String applicatioNo;
	
	@JsonProperty("status")
	private String status;
	
	@JsonProperty("village")
	private String village;

}
