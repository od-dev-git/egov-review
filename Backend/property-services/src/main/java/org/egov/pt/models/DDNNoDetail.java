package org.egov.pt.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@ToString
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DDNNoDetail {

    @JsonProperty("propertyId")
    private String propertyId;

    @JsonProperty("ddnNo")
    private String ddnNo;

    @JsonProperty("ddnNoUpdatedBy")
    private String ddnNoUpdatedBy="DDN_Authority";
}
