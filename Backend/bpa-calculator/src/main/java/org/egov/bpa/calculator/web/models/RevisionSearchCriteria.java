package org.egov.bpa.calculator.web.models;

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
public class RevisionSearchCriteria {

	@JsonProperty("ids")
	private List<String> ids;

	@JsonProperty("tenantId")
	private String tenantId;

	@JsonProperty("isSujogExistingApplication")
	private boolean isSujogExistingApplication;

	@JsonProperty("bpaApplicationNo")
	private String bpaApplicationNo;
	
	@JsonProperty("bpaApplicationId")
	private String bpaApplicationId;

	@JsonProperty("refBpaApplicationNo")
	private String refBpaApplicationNo;

	@JsonProperty("refPermitNo")
	private String refPermitNo;

	@JsonProperty("refPermitDate")
	private Long refPermitDate;

	@JsonProperty("refPermitExpiryDate")
	private Long refPermitExpiryDate;

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
		return (this.ids == null && this.tenantId == null && this.bpaApplicationNo == null
				&& this.bpaApplicationId == null && this.refBpaApplicationNo == null && this.refPermitNo == null
				&& this.refPermitDate == null && this.refPermitExpiryDate == null && this.fromDate == null
				&& this.toDate == null && this.createdBy == null);
	}
}
