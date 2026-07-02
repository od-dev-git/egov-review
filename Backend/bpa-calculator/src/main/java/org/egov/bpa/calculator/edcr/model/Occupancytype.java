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
public class Occupancytype {
	
	@JsonProperty("color")
	private String color = null;
	
	@JsonProperty("code")
	private String code = null;
	
	@JsonProperty("name")
	private String name = null;

}
