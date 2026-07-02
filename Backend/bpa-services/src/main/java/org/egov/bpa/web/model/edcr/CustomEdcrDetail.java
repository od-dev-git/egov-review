package org.egov.bpa.web.model.edcr;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@AllArgsConstructor
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@Setter
@ToString
@Builder
public class CustomEdcrDetail {

    @JsonProperty("edcrDetail")
    private List<EdcrDetail> edcrDetail;
}
