package org.egov.bpa.web.model;

import java.util.List;
import java.util.Objects;

import javax.validation.Valid;

import org.egov.common.contract.request.RequestInfo;
import org.egov.common.contract.response.ResponseInfo;
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
public class NoticeRequest {
	
	@JsonProperty("RequestInfo")
	private RequestInfo requestInfo = null;
	
	@JsonProperty("Notice")
	private Notice notice;
	
	public  NoticeRequest responseInfo(RequestInfo requestInfo) {
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

	public NoticeRequest noticeRequest(Notice notice) {
		this.notice = notice;
		return this;
	}
	
	
	@ApiModelProperty(value = "")

	@Valid
	public Notice getnotice() {
		return notice;
	}

	public void notice(Notice notice) {
		this.notice = notice;
	}
	
	@Override
	public boolean equals(java.lang.Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		NoticeRequest noticeRequest = (NoticeRequest) o;
		return Objects.equals(this.requestInfo, noticeRequest.requestInfo)
				&& Objects.equals(this.notice, noticeRequest.notice);
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(requestInfo, notice);
	}
	
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("class NoticeRequest {\n");

		sb.append("    requestInfo: ").append(toIndentedString(requestInfo)).append("\n");
		sb.append("    notice: ").append(toIndentedString(notice)).append("\n");
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
