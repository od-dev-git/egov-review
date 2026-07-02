package org.egov.bpa.web.model.regularization;

import java.util.List;

import javax.validation.Valid;

import org.egov.common.contract.response.ResponseInfo;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RegularizationDigitalSignCertificateResponse {
	
	@JsonProperty("ResponseInfo")
	@Default
	private ResponseInfo responseInfo = null;

	@JsonProperty("dscDetails")
	@Valid
	@Default
	private List<RegularizationDscDetails> dscDetails = null;
}
