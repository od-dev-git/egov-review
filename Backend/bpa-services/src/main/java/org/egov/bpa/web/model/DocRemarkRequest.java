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
public class DocRemarkRequest {
	
	@JsonProperty("RequestInfo")
	private RequestInfo requestInfo = null;
	
	@JsonProperty("DocRemark")
	private DocRemark docRemark;
	
	public  DocRemarkRequest responseInfo(RequestInfo requestInfo) {
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

	public DocRemarkRequest docRemarkRequest(DocRemark docRemark) {
		this.docRemark = docRemark;
		return this;
	}
	
	
	@ApiModelProperty(value = "")

	@Valid
	public DocRemark getDocRemark() {
		return docRemark;
	}

	public void notice(DocRemark docRemark) {
		this.docRemark = docRemark;
	}
	
	@Override
	public boolean equals(java.lang.Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		DocRemarkRequest docRemarkRequest = (DocRemarkRequest) o;
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
	private String toIndentedString(java.lang.Object o) {
		if (o == null) {
			return "null";
		}
		return o.toString().replace("\n", "\n    ");
	}

}

