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
public class Contact {

	@JsonProperty("profile")
	private Profile profile;
	
	@JsonProperty("wa_id")
    private String wa_id;
}
