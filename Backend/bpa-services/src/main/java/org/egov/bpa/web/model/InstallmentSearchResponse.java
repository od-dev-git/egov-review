package org.egov.bpa.web.model;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class InstallmentSearchResponse {
	
	private List<Installment> fullPayment;
	private List<List<Installment>> installments;

}
