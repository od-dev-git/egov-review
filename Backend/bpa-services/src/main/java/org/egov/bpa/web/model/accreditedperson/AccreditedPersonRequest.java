package org.egov.bpa.web.model.accreditedperson;

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
public class AccreditedPersonRequest {
	@JsonProperty("RequestInfo")
	private RequestInfo requestInfo = null;

	@JsonProperty("accreditedPerson")
	private AccreditedPerson accreditedPerson = null;

	public AccreditedPersonRequest requestInfo(RequestInfo requestInfo) {
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

	public AccreditedPersonRequest accreditedPersonRequest(AccreditedPerson accreditedPerson) {
		this.accreditedPerson = accreditedPerson;
		return this;
	}

	/**
	 * Get Accredited Person
	 * 
	 * @return Accredited Person
	 **/
	@ApiModelProperty(value = "")

	@Valid
	public AccreditedPerson getAccreditedPerson() {
		return accreditedPerson;
	}

	public void setAccreditedPerson(AccreditedPerson accreditedPerson) {
		this.accreditedPerson = accreditedPerson;
	}

	@Override
	public boolean equals(java.lang.Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		AccreditedPersonRequest accreditedPersonRequest = (AccreditedPersonRequest) o;
		return Objects.equals(this.requestInfo, accreditedPersonRequest.requestInfo)
				&& Objects.equals(this.accreditedPerson, accreditedPersonRequest.accreditedPerson);
	}

	@Override
	public int hashCode() {
		return Objects.hash(requestInfo, accreditedPerson);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("class AccreditedPersonRequest {\n");

		sb.append("    requestInfo: ").append(toIndentedString(requestInfo)).append("\n");
		sb.append("    accreditedPerson: ").append(toIndentedString(accreditedPerson)).append("\n");
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
