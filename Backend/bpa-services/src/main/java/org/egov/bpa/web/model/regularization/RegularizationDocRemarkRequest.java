package org.egov.bpa.web.model.regularization;

import java.util.Objects;

import javax.validation.Valid;

import org.egov.common.contract.request.RequestInfo;
import org.springframework.validation.annotation.Validated;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.NoArgsConstructor;

@Validated
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegularizationDocRemarkRequest {
	
	@JsonProperty("RequestInfo")
	@Default
	private RequestInfo requestInfo = null;
	
	@JsonProperty("DocRemark")
	private RegularizationDocRemark docRemark;
	
	public  RegularizationDocRemarkRequest responseInfo(RequestInfo requestInfo) {
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

	public RegularizationDocRemarkRequest docRemarkRequest(RegularizationDocRemark docRemark) {
		this.docRemark = docRemark;
		return this;
	}
	
	
	@ApiModelProperty(value = "")
	@Valid
	public RegularizationDocRemark getDocRemark() {
		return docRemark;
	}

	public void notice(RegularizationDocRemark docRemark) {
		this.docRemark = docRemark;
	}
	
	@Override
	public boolean equals(java.lang.Object object) {
		if (this == object) {
			return true;
		}
		if (object == null || getClass() != object.getClass()) {
			return false;
		}
		RegularizationDocRemarkRequest docRemarkRequest = (RegularizationDocRemarkRequest) object;
		return Objects.equals(this.requestInfo, docRemarkRequest.requestInfo)
				&& Objects.equals(this.docRemark, docRemarkRequest.docRemark);
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(requestInfo, docRemark);
	}
	
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("class DocRemarkRequest {\n");

		sb.append("    requestInfo: ").append(toIndentedString(requestInfo)).append("\n");
		sb.append("    docRemark: ").append(toIndentedString(docRemark)).append("\n");
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

