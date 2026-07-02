package org.egov.bpa.web.model.oc;

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
    @Builder.Default
    List<ScrutinyDetails> scrutinyDetails=new ArrayList<>();


}
