package org.egov.collection.model.whatsapp;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Setter
@Getter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class ExtraPayload {

	@JsonProperty("receiptNumbers")
	private String receiptNumbers;
	
	@JsonProperty("tenantId")
    private String tenantId;
	
	@JsonProperty("consumerCode")
    private String consumerCode;

}
