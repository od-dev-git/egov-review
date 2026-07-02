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
public class Message {
	
	@JsonProperty("id")
	private String id;
	
	@JsonProperty("from")
    private String from;
	
	@JsonProperty("type")
    private String type;
	
	@JsonProperty("timestamp")
    private String timestamp;
	
	@JsonProperty("text")
    private Text text;

}
