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
public class Text {
	
	@JsonProperty("body")	
	private String body;

}
