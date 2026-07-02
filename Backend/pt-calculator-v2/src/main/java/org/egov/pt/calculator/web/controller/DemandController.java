package org.egov.pt.calculator.web.controller;

import java.util.List;

import javax.validation.Valid;

import org.egov.pt.calculator.service.DemandService;
import org.egov.pt.calculator.util.ResponseInfoFactory;
import org.egov.pt.calculator.web.models.demand.Demand;
import org.egov.pt.calculator.web.models.demand.DemandRequest;
import org.egov.pt.calculator.web.models.demand.DemandResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
@RestController
@RequestMapping("/demand")
public class DemandController {

	@Autowired
	private DemandService demandService;
	
	@Autowired
	private final ResponseInfoFactory responseInfoFactory;
	
	@PostMapping("/_modify")
	public ResponseEntity<DemandResponse> updateDemands(@RequestHeader HttpHeaders headers, @RequestBody @Valid DemandRequest demandRequest) {
		List<Demand> demands = demandService.modifyDemands(demandRequest);
		DemandResponse response = DemandResponse.builder().demands(demands)
				.responseInfo(
				responseInfoFactory.createResponseInfoFromRequestInfo(demandRequest.getRequestInfo(), true))
				.build();
		return new ResponseEntity<>(response, HttpStatus.OK);
	}
}
