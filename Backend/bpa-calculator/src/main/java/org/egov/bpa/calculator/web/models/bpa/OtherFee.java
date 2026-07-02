package org.egov.bpa.calculator.web.models.bpa;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OtherFee {

	private String reason;
	private Integer order;
	private BigDecimal amount;
}
