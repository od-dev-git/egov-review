package org.egov.dsc.model;




import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.groups.Default;

import org.egov.common.contract.request.RequestInfo;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CertificateRequest {

	public interface Decrypt extends Default {}

	@NotNull
    @Valid
    @JsonProperty("RequestInfo")
    private RequestInfo requestInfo;

	@NotNull
    @JsonProperty("tokenDisplayName")
	private String tokenDisplayName;

	@NotBlank(groups = Decrypt.class)
	@JsonProperty("responseData")
	private String responseData;

    

}
