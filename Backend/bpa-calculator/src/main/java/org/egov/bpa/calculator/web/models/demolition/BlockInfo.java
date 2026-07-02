package org.egov.bpa.calculator.web.models.demolition;

import org.egov.bpa.calculator.web.models.AuditDetails;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.minidev.json.JSONObject;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class BlockInfo {

	@JsonProperty("id")
	@Default
	private String id = null;

	@JsonProperty("tenantId")
	@Default
	private String tenantId = null;

	@JsonProperty("demolitionLandInfoId")
	@Default
	private String demolitionLandInfoId = null;

	@JsonProperty("anyApprovedArea")
	@Default
	private String anyApprovedArea = null;

	@JsonProperty("occupancy")
	@Default
	private String occupancy = null;

	@JsonProperty("totalBUA")
	@Default
	private String totalBUA = null;

	@JsonProperty("noOfFloors")
	@Default
	private String noOfFloors = null;

	@JsonProperty("setbackDetails")
	@Default
	private Object setbackDetails = new JSONObject();

	@JsonProperty("buildingDistance")
	@Default
	private String buildingDistance = null;

	@JsonProperty("buildingHeight")
	@Default
	private String buildingHeight = null;

	@JsonProperty("auditDetails")
	@Default
	private AuditDetails auditDetails = null;

}
