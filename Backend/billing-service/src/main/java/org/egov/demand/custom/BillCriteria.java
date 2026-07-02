package org.egov.demand.custom;

import java.util.Set;

import javax.validation.constraints.NotNull;

import org.egov.demand.model.GenerateBillCriteria;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class BillCriteria {

	@NotNull
	private String tenantId;

	@NotNull
	private String consumerCode;
	
	@NotNull
	private String businessService;
	
	private String whatsAppNumber;
	
	private String whatsAppNumberv2;
}
