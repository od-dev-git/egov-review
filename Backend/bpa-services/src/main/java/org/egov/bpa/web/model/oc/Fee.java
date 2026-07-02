package org.egov.bpa.web.model.oc;

import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;

@Validated
@AllArgsConstructor
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@Setter
@ToString
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Fee {

    String feeType;

    BigDecimal feeAmount;

    BigDecimal paidAmount;

    BigDecimal amountNotPaid;
}
