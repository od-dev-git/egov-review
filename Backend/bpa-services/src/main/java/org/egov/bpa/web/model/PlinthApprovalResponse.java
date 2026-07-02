package org.egov.bpa.web.model;

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
public class PlinthApprovalResponse {

	@JsonProperty("ResponseInfo")
	private ResponseInfo responseInfo;

	@JsonProperty("plinthApproval")
	private List<PlinthApproval> plinthApproval;

	public PlinthApprovalResponse responseInfo(ResponseInfo responseInfo) {
		this.responseInfo = responseInfo;
		return this;
	}

	@ApiModelProperty(value = "")

	@Valid
	public ResponseInfo getResponseInfo() {
		return responseInfo;
	}

	public void setResponseInfo(ResponseInfo responseInfo) {
		this.responseInfo = responseInfo;
	}

	public PlinthApprovalResponse plinthApprovalResponse(List<PlinthApproval> plinthApproval) {
		this.plinthApproval = plinthApproval;
		return this;
	}

	@Override
	public boolean equals(java.lang.Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		PlinthApprovalResponse plinthApprovalResponse = (PlinthApprovalResponse) o;
		return Objects.equals(this.responseInfo, plinthApprovalResponse.responseInfo)
				&& Objects.equals(this.plinthApproval, plinthApprovalResponse.getPlinthApproval());
	}

	@Override
	public int hashCode() {
		return Objects.hash(responseInfo, plinthApproval);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("class PlinthApprovalResponse {\n");
		sb.append("    responseInfo: ").append(toIndentedString(responseInfo)).append("\n");
		sb.append("    plinthApproval: ").append(toIndentedString(plinthApproval)).append("\n");
		sb.append("}");
		return sb.toString();
	}

	/**
	 * Convert the given object to string with each line indented by 4 spaces
	 * (except the first line).
	 */
	private String toIndentedString(java.lang.Object o) {
		if (o == null) {
			return "null";
		}
		return o.toString().replace("\n", "\n    ");
	}

}
