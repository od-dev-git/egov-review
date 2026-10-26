package org.egov.dsc.model;


import javax.validation.Valid;
import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.groups.Default;

import org.egov.common.contract.request.RequestInfo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;


@JsonIgnoreProperties({ "userId" })
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DataSignRequest {

	public interface DataSignInput extends Default {}
	public interface DataSign extends Default {}
	public interface PdfSignInput extends Default {}
	public interface PdfSign extends Default {}
	public interface Deregister extends Default {}

	@NotNull
    @Valid
    @JsonProperty("RequestInfo")
    private RequestInfo requestInfo;

    @NotBlank(groups = { DataSignInput.class, PdfSignInput.class })
    @JsonProperty("tokenDisplayName")
	private String tokenDisplayName;

    @NotEmpty(groups = { DataSignInput.class, PdfSignInput.class })
    @JsonProperty(value = "keyStorePassPhrase", access = JsonProperty.Access.WRITE_ONLY)
    @ToString.Exclude
    private String keyStorePassPhrase;

    @NotBlank(groups = { DataSignInput.class, PdfSignInput.class })
    @JsonProperty("keyId")
	private String keyId;

    // Part of the emAS identity "<userId>~<channelId>"; a missing value would register under "null"
    @NotBlank(groups = { DataSignInput.class, DataSign.class, PdfSign.class, Deregister.class })
    @JsonProperty("channelId")
	private String channelId;

    @NotBlank(groups = { DataSign.class, PdfSign.class })
    @JsonProperty("responseData")
    private String responseData;

    // fileStoreId of the unsigned PDF, not its bytes
    @NotBlank(groups = PdfSignInput.class)
    @JsonProperty("file")
    private String fileBytes;

    @NotBlank(groups = { PdfSignInput.class, PdfSign.class })
    @JsonProperty("fileName")
    private String fileName;

    @NotBlank(groups = { PdfSignInput.class, PdfSign.class })
    @JsonProperty("tenantId")
    private String tenantId;

    @NotBlank(groups = PdfSign.class)
    @JsonProperty("moduleName")
    private String moduleName;

    @NotBlank(groups = PdfSign.class)
    @JsonProperty("tempFilePath")
	private String tempFilePath;

	/**
	 * The signer is always taken from RequestInfo.userInfo, never from the body. A null
	 * RequestInfo is reported by its own @NotNull, so it is not flagged twice here.
	 */
	@JsonIgnore
	@AssertTrue(message = "RequestInfo.userInfo.id is required", groups = { DataSignInput.class,
			DataSign.class, PdfSignInput.class, PdfSign.class, Deregister.class })
	public boolean isUserIdPresent() {
		return requestInfo == null || (requestInfo.getUserInfo() != null && requestInfo.getUserInfo().getId() != null);
	}
}
