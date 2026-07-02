package org.egov.bpa.web.model.demolition;

import java.util.List;
import java.util.Objects;

import javax.validation.Valid;

import org.egov.common.contract.response.ResponseInfo;
import org.springframework.validation.annotation.Validated;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.annotations.ApiModelProperty;
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
public class DemolitionApprovedByApplicationResponse {
	
	@JsonProperty("ResponseInfo")
	private ResponseInfo responseInfo;

	@JsonProperty("ApprovedBy")
	private List<DemolitionApprovedByApplicationSearch> demolitionApprovedByApplicationSearch;

	@ApiModelProperty(value = "")

	@Valid
	public ResponseInfo getResponseInfo() {
		return responseInfo;
	}

	public void setResponseInfo(ResponseInfo responseInfo) {
		this.responseInfo = responseInfo;
	}

	public DemolitionApprovedByApplicationResponse DemolitionApprovedByApplicationSearch(
			List<DemolitionApprovedByApplicationSearch> apps) {
		this.demolitionApprovedByApplicationSearch = apps;
		return this;
	}

	@ApiModelProperty(value = "")

	@Override
	public boolean equals(java.lang.Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		DemolitionApprovedByApplicationResponse	DemolitionApprovedByApplicationResponse = (DemolitionApprovedByApplicationResponse) o;
		return Objects.equals(this.responseInfo, DemolitionApprovedByApplicationResponse.responseInfo)
				&& Objects.equals(this.demolitionApprovedByApplicationSearch,
						DemolitionApprovedByApplicationResponse.demolitionApprovedByApplicationSearch);
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("class DemolitionApprovedByApplicationSearch {\n");

		sb.append("    responseInfo: ").append(toIndentedString(responseInfo)).append("\n");
		sb.append("    ApprovedBy: ").append(toIndentedString(demolitionApprovedByApplicationSearch)).append("\n");
		sb.append("}");
		return sb.toString();
	}

	private String toIndentedString(java.lang.Object o) {
		if (o == null) {
			return "null";
		}
		return o.toString().replace("\n", "\n    ");
	}

}
