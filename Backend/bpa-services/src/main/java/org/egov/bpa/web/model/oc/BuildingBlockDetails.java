package org.egov.bpa.web.model.oc;

import lombok.*;
import lombok.experimental.FieldDefaults;



import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BuildingBlockDetails {

	@JsonProperty("buildingHeight")
    BigDecimal buildingHeight;

	@JsonProperty("floors")
    List<Floor> floors;

	@JsonProperty("setbacks")
    List<Setback> setbacks;

}
