package org.egov.bpa.web.model;
import java.util.List;

import org.egov.common.contract.request.RequestInfo;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class StageWiseReportRequest {
	
	@JsonProperty("RequestInfo")
	private RequestInfo requestInfo = null;

	@JsonProperty("stageWiseReports")
	private List<StageWiseReport> stageWiseReports = null;

}

