package org.egov.bpa.web.model;

import java.util.List;
import java.util.Objects;

import org.egov.common.contract.response.ResponseInfo;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class BPADraftResponse {

	@JsonProperty("ResponseInfo")
	private ResponseInfo responseInfo;

	@JsonProperty("bpaDraft")
	private List<BPADraft> bpaDraft;

	public BPADraftResponse responseInfo(ResponseInfo responseInfo) {
		this.responseInfo = responseInfo;
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
		BPADraftResponse bpaDraftResponse = (BPADraftResponse) o;
		return Objects.equals(this.responseInfo, bpaDraftResponse.responseInfo)
				&& Objects.equals(this.bpaDraft, bpaDraftResponse.bpaDraft);
	}

	@Override
	public int hashCode() {
		return Objects.hash(responseInfo, bpaDraft);
	}

}
