package org.egov.bpa.web.model;

import java.util.List;

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
public class StageWiseReportSearchCriteria {

	@JsonProperty("ids")
	private List<String> ids;

	@JsonProperty("applicationNo")
	private String applicationNo;

	@JsonProperty("levelType")
	private String levelType;

	@JsonProperty("blockNo")
	private String blockNo;

	@JsonProperty("floorNo")
	private String floorNo;

	@JsonProperty("createdBy")
	private String createdBy;

	@JsonProperty("createdTime")
	private Long createdTime;

	@JsonProperty("offset")
	private Integer offset;

	@JsonProperty("limit")
	private Integer limit;

	@JsonProperty("fromDate")
	private Long fromDate;

	@JsonProperty("toDate")
	private Long toDate;

}
