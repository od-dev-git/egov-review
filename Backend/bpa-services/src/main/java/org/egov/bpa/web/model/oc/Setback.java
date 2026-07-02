package org.egov.bpa.web.model.oc;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.validation.annotation.Validated;

@Validated
@AllArgsConstructor
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@Setter
@ToString
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Setback {

    @JsonProperty("name")
    String name;

    @JsonProperty("asPerRecentNorms")
    String asPerRecentNorms;

    @JsonProperty("asPerApprovalLetter")
    String asPerApprovalLetter;

    @JsonProperty("asBuiltMeasurement")
    String asBuiltMeasurement;

    @JsonProperty("deviation")
    String deviation;

    @JsonProperty("status")
    String status;


}
