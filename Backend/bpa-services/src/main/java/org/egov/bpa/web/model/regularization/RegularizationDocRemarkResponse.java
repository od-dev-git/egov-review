package org.egov.bpa.web.model.regularization;

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
public class RegularizationDocRemarkResponse {
	
	@JsonProperty("ResponseInfo")
	private ResponseInfo responseInfo;
	
	@JsonProperty("DocRemarks")
	private List<RegularizationDocRemark> docRemarks;
	
	public RegularizationDocRemarkResponse responseInfo(ResponseInfo responseInfo) {
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
	
	public RegularizationDocRemarkResponse response(List<RegularizationDocRemark> docRemarks) {
		this.docRemarks = docRemarks;
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
		
		RegularizationDocRemarkResponse response = (RegularizationDocRemarkResponse) object;
		return Objects.equals(this.responseInfo, response.responseInfo)
				&& Objects.equals(this.docRemarks, response.docRemarks);
	}
	@Override
	public int hashCode() {
		return Objects.hash(responseInfo, docRemarks);
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("class ScnResponse {\n");

		sb.append("    responseInfo: ").append(toIndentedString(responseInfo)).append("\n");
		sb.append("    docRemarks: ").append(toIndentedString(docRemarks)).append("\n");
		sb.append("}");
		return sb.toString();
	}


	private String toIndentedString(java.lang.Object object) {
		if (object == null) {
			return "null";
		}
		return object.toString().replace("\n", "\n    ");
	}


}
