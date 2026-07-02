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
public class RegularizationDraftResponse {

	@JsonProperty("ResponseInfo")
	private ResponseInfo responseInfo;

	@JsonProperty("regularizationDraft")
	private List<RegularizationDraft> regularizationDraft;

	public RegularizationDraftResponse responseInfo(ResponseInfo responseInfo) {
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
		RegularizationDraftResponse regularizationDraftResponse = (RegularizationDraftResponse) o;
		return Objects.equals(this.responseInfo, regularizationDraftResponse.responseInfo)
				&& Objects.equals(this.regularizationDraft, regularizationDraftResponse.regularizationDraft);
	}

	@Override
	public int hashCode() {
		return Objects.hash(responseInfo, regularizationDraft);
	}

}
