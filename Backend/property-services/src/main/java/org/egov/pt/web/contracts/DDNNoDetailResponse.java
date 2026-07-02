package org.egov.pt.web.contracts;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import org.egov.common.contract.response.ResponseInfo;
import org.egov.pt.models.DDNNoDetail;

import java.util.List;

@ToString
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DDNNoDetailResponse {
    @JsonProperty("ResponseInfo")
    private ResponseInfo responseInfo;

    @JsonProperty("DDNNoDetails")
    private List<DDNNoDetail> ddnNoDetails;
}
