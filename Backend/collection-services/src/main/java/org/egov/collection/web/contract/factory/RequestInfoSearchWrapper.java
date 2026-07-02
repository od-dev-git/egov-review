package org.egov.collection.web.contract.factory;

import java.util.Set;

import org.egov.common.contract.request.RequestInfo;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RequestInfoSearchWrapper {

	@JsonProperty(value="RequestInfo")
	private RequestInfo requestInfo;

	@JsonProperty(value="searchIds")
	private Set<String> ids;	
	
	@JsonProperty(value="receiptNumbers")
	private Set<String> receiptNumbers;
}
