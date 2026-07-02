package org.egov.bpa.web.model.issuefix;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import org.egov.common.contract.response.ResponseInfo;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IssueFixResponse {

    @JsonProperty("ResponseInfo")
    private ResponseInfo responseInfo;

    @JsonProperty("IssueFix")
    private IssueFix issueFix=null;

    @JsonProperty("Status")
    private String status=null;


}
