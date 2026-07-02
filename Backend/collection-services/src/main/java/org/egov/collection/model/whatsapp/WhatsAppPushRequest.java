package org.egov.collection.model.whatsapp;


import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;

@Setter
@Getter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class WhatsAppPushRequest {
	
	@JsonProperty("messages")
	private List<Message> messages;
	
	@JsonProperty("contacts")
	private List<Contact> contacts;
	
	@JsonProperty("brand_msisdn")
    private String brand_msisdn;
    
	@JsonProperty("request_id")
    private String request_id;
    
	@JsonProperty("extra_payload")
    private ExtraPayload extra_payload;

}
