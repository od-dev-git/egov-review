package org.egov.bpa.web.model.oc;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Validated
@AllArgsConstructor
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@Setter
@ToString
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Floor {

    @JsonProperty("floorType")
    private String floorType;

    @JsonProperty("floorNumber")
    private String floorNumber;

    @JsonProperty("subOcuupancy")
    private SubOccupancy subOcuupancy;

    @JsonProperty("asBuiltBUA")
    private String asBuiltBUA;

    @JsonProperty("asBuiltFARArea")
    private String asBuiltFARArea;

    @JsonProperty("asBuiltCarpetArea")
    private String asBuiltCarpetArea;

}
