package org.egov.bpa.web.model;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonCreator;
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
public class StageWiseReport {

	@JsonProperty("id")
	private String id = null;

	@JsonProperty("applicationNo")
	@NotNull(message = "Application number is mandatory")
	@NotBlank(message = "Application number cannot be empty")
	private String applicationNo;

	@JsonProperty("levelType")
	@NotNull(message = "Level Type is mandatory")
	@NotBlank(message = "Level Type cannot be empty")
	private LevelType levelType;

	@JsonProperty("blockNo")
	@NotNull(message = "BlockNo is mandatory")
	@NotBlank(message = "BlockNo Type cannot be empty")
	private String blockNo;

	@JsonProperty("floorNo")
	@NotNull(message = "FloorNo Type is mandatory")
	@NotBlank(message = "FloorNo Type cannot be empty")
	private String floorNo;

	@JsonProperty("documentDetails")
	private Object documentDetails = null;

	@JsonProperty("additionalDetails")
	private Object additionalDetails = null;

	@JsonProperty("status")
	private String status = null;

	@JsonProperty("approvalNo")
	private String approvalNo = null;

	@JsonProperty("auditDetails")
	private AuditDetails auditDetails;

	public enum LevelType {
		FOUNDATION, PLINTH, FLOOR;
	}

	@JsonCreator
	public static LevelType fromValue(String value) {
		for (LevelType type : LevelType.values()) {
			if (type.name().equalsIgnoreCase(value)) {
				return type;
			}
		}
		throw new IllegalArgumentException("Invalid levelType. Allowed values: FOUNDATION, PLINTH, FLOOR");
	}

}
