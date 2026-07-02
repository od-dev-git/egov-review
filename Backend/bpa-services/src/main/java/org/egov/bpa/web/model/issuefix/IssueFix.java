package org.egov.bpa.web.model.issuefix;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IssueFix {

    @JsonProperty("tenant")
    private String tenantId = null;

    @JsonProperty("issueName")
    private String issueName=null;

    @JsonProperty("applicationNo")
    private String applicationNo = null;

    @JsonProperty("approvalNo")
    private String approvalNo = null;
        
    @JsonProperty("newApprover")
    private String newApprover = null;

    @JsonProperty("villages")
    private List<String> villages = null;
    
    @JsonProperty("issueSubtype")
    private String issueSubtype = null;
    
    @JsonProperty("reworkCount")
    private Integer reworkCount = null;

}
