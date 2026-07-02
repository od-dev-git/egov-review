package org.egov.bpa.web.model.accreditedperson;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
public class AccreditedPersonSearchCriteria {

	@JsonProperty("ids")
	private List<String> ids;

	@JsonProperty("userUUID")
	private String userUUID;

	@JsonProperty("userId")
	private Long userId;

	@JsonProperty("personName")
	private String personName;

	@JsonProperty("firmName")
	private String firmName;

	@JsonProperty("accreditationNo")
	private String accreditationNo;

	@JsonProperty("certificateIssueDate")
	private Long certificateIssueDate;

	@JsonProperty("validTill")
	private Long validTill;

	@JsonProperty("offset")
	private Integer offset;

	@JsonProperty("limit")
	private Integer limit;

	@JsonProperty("fromDate")
	private Long fromDate;

	@JsonProperty("toDate")
	private Long toDate;

	@JsonIgnore
	private List<String> createdBy;

	public boolean isEmpty() {
		return (this.ids == null && this.userUUID == null && this.userId == null && this.personName == null
				&& this.firmName == null && this.accreditationNo == null && this.certificateIssueDate == null
				&& this.validTill == null && this.fromDate == null && this.toDate == null && this.createdBy == null);
	}
}
