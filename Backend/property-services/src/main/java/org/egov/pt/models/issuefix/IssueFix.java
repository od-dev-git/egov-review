package org.egov.pt.models.issuefix;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.egov.pt.models.Assessment;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IssueFix {

    @JsonProperty("tenant")
    @Default
    private String tenantId = null;

    @JsonProperty("issueName")
    @Default
    private String issueName=null;

    @JsonProperty("applicationNo")
    @Default
    private String applicationNo = null;

    @JsonProperty("propertyId")
    @Default
    private String propertyId = null;
    
    @JsonProperty("assessmentNo")
    @Default
    private String assessmentNo = null;
    
    @Valid
    @JsonProperty("Assessment")
    private Assessment assessment;

}
