package org.egov.bpa.web.model;

import java.util.Objects;

import javax.validation.Valid;

import org.egov.common.contract.request.RequestInfo;
import org.springframework.validation.annotation.Validated;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

@Validated
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RevalidationRequest {
	@JsonProperty("RequestInfo")
	private RequestInfo requestInfo = null;

	@JsonProperty("revalidation")
	private Revalidation revalidation = null;

	public RevalidationRequest requestInfo(RequestInfo requestInfo) {
		this.requestInfo = requestInfo;
		return this;
	}

	/**
	 * Get requestInfo
	 * 
	 * @return requestInfo
	 **/
	@ApiModelProperty(value = "")

	@Valid
	public RequestInfo getRequestInfo() {
		return requestInfo;
	}

	public void setRequestInfo(RequestInfo requestInfo) {
		this.requestInfo = requestInfo;
	}

	public RevalidationRequest revision(Revalidation revalidation) {
		this.revalidation = revalidation;
		return this;
	}

	/**
	 * Get BPA
	 * 
	 * @return BPA
	 **/
	@ApiModelProperty(value = "")

	@Valid
	public Revalidation getRevalidation() {
		return revalidation;
	}

	public void setRevalidation(Revalidation revalidation) {
		this.revalidation = revalidation;
	}

	@Override
	public boolean equals(java.lang.Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		RevalidationRequest revalidationRequest = (RevalidationRequest) o;
		return Objects.equals(this.requestInfo, revalidationRequest.requestInfo)
				&& Objects.equals(this.revalidation, revalidationRequest.revalidation);
	}

	@Override
	public int hashCode() {
		return Objects.hash(requestInfo, revalidation);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("class revalidationRequest {\n");

		sb.append("    requestInfo: ").append(toIndentedString(requestInfo)).append("\n");
		sb.append("    revalidation: ").append(toIndentedString(revalidation)).append("\n");
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
