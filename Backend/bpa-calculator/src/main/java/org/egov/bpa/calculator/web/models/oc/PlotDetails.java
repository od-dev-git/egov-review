package org.egov.bpa.calculator.web.models.oc;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PlotDetails {

    @JsonProperty("plotArea")
    BigDecimal plotArea;

    @JsonProperty("giftedLandArea")
    BigDecimal giftedLandArea;

    @JsonProperty("netPlotArea")
    BigDecimal netPlotArea;
}
