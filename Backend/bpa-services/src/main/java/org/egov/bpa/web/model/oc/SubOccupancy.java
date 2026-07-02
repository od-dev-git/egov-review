package org.egov.bpa.web.model.oc;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;

@Validated
@AllArgsConstructor
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@Setter
@ToString
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SubOccupancy {

	@JsonProperty("label")
	private String label;

	@JsonProperty("value")
	private String value;

}
