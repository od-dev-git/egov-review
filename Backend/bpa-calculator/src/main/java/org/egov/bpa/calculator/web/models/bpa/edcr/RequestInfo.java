package org.egov.bpa.calculator.web.models.bpa.edcr;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import org.springframework.validation.annotation.Validated;

@Data
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode
@Validated
public class RequestInfo {
	@JsonProperty("apiId")
	private String apiId;

	@JsonProperty("action")
	private String action;
	
	@JsonProperty("ver")
	private String ver;

	@JsonProperty("ts")
	private String ts;
	
	@JsonProperty("did")
	private String did;

	@JsonProperty("key")
	private String key;
	
	@JsonProperty("msgId")
	private String msgId;

	@JsonProperty("authToken")
	private String authToken;
	
	@JsonProperty("correlationId")
	private String correlationId;
}
