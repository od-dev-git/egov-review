package org.egov.bpa.web.model.revalidation;

import java.math.BigDecimal;

import org.springframework.validation.annotation.Validated;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Validated
@AllArgsConstructor
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@Setter
@ToString
@Builder
public class Occupancy {

	@JsonProperty("type")
	private String type;

	@JsonProperty("subType")
	private String subType;

	@JsonProperty("typeCode")
	private String typeCode;

	@JsonProperty("subTypeCode")
	private String subTypeCode;

	@JsonProperty("buildupArea")
	private BigDecimal buildupArea;

	@JsonProperty("floorArea")
	private BigDecimal floorArea;

	@JsonProperty("carpetArea")
	private BigDecimal carpetArea;

	@JsonProperty("AreaType")
	private String AreaType;// Existing/Proposed
}
