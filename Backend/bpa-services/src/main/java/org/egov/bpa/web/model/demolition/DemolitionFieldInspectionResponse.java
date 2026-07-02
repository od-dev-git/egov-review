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
public class DemolitionFieldInspectionResponse {

	@JsonProperty("ResponseInfo")
	private ResponseInfo responseInfo;

	@JsonProperty("fieldInspection")
	private List<DemolitionFieldInspection> fieldInspection;

	public DemolitionFieldInspectionResponse responseInfo(ResponseInfo responseInfo) {
		this.responseInfo = responseInfo;
		return this;
	}

	/**
	 * Get responseInfo
	 * 
	 * @return responseInfo
	 **/
	@ApiModelProperty(value = "")

	@Valid
	public ResponseInfo getResponseInfo() {
		return responseInfo;
	}

	public void setResponseInfo(ResponseInfo responseInfo) {
		this.responseInfo = responseInfo;
	}

	public DemolitionFieldInspectionResponse feildInspectionResponse(List<DemolitionFieldInspection> fieldInspection) {
		this.fieldInspection = fieldInspection;
		return this;
	}

	@Override
	public boolean equals(java.lang.Object object) {
		if (this == object) {
			return true;
		}
		if (object == null || getClass() != object.getClass()) {
			return false;
		}
		DemolitionFieldInspectionResponse fieldInspectionResponse = (DemolitionFieldInspectionResponse) object;
		return Objects.equals(this.responseInfo, fieldInspectionResponse.responseInfo)
				&& Objects.equals(this.fieldInspection, fieldInspectionResponse.fieldInspection);
	}

	@Override
	public int hashCode() {
		return Objects.hash(responseInfo, fieldInspection);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("class feildInspectionResponse {\n");

		sb.append("    responseInfo: ").append(toIndentedString(responseInfo)).append("\n");
		sb.append("    feildInspection: ").append(toIndentedString(fieldInspection)).append("\n");
		sb.append("}");
		return sb.toString();
	}

	/**
	 * Convert the given object to string with each line indented by 4 spaces
	 * (except the first line).
	 */
	private String toIndentedString(java.lang.Object object) {
		if (object == null) {
			return "null";
		}
		return object.toString().replace("\n", "\n    ");
	}

}