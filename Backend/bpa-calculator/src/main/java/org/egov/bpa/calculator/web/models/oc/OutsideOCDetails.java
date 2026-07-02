package org.egov.bpa.calculator.web.models.oc;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OutsideOCDetails {

    @JsonProperty("scrutinyDetails")
    List<ScrutinyDetails> scrutinyDetails=new ArrayList<>();


}
