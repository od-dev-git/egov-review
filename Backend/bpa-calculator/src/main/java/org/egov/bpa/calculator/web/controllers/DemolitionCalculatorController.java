package org.egov.bpa.calculator.web.controllers;

import java.util.List;

import javax.validation.Valid;

import org.egov.bpa.calculator.services.DemolitionCalculationService;
import org.egov.bpa.calculator.web.models.demolition.DemolitionCalculation;
import org.egov.bpa.calculator.web.models.demolition.DemolitionCalculationRequest;
import org.egov.bpa.calculator.web.models.demolition.DemolitionCalculationResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;

@RestController
@Slf4j
@RequestMapping("/demolition")
public class DemolitionCalculatorController {
	
	@Autowired
	private DemolitionCalculationService calculationService;
	
	/**
	 * Controller to get Estimate for a Demolition
	 * 
	 * @param calculationRequest
	 * @return
	 */
	@RequestMapping(value = "/_estimate", method = RequestMethod.POST)
	public ResponseEntity<DemolitionCalculationResponse> estimate(
			@Valid @RequestBody DemolitionCalculationRequest calculationRequest) {

		log.info("CalculationRequest:: " + calculationRequest);
		List<DemolitionCalculation> calculations = calculationService.getEstimate(calculationRequest);
		DemolitionCalculationResponse response = DemolitionCalculationResponse.builder().calculations(calculations)
				.build();

		return new ResponseEntity<DemolitionCalculationResponse>(response, HttpStatus.OK);
	}
	
	/**
	 * Controller to generate demand for a Demolition
	 * 
	 * @param calculationRequest
	 * @return
	 */
	@RequestMapping(value = "/_calculate", method = RequestMethod.POST)
	public ResponseEntity<DemolitionCalculationResponse> calculate(
			@Valid @RequestBody DemolitionCalculationRequest calculationRequest) {
		log.debug("CalculationReaquest:: " + calculationRequest);
		List<DemolitionCalculation> calculations = calculationService.calculate(calculationRequest);
		DemolitionCalculationResponse calculationRes = DemolitionCalculationResponse.builder()
				.calculations(calculations).build();
		return new ResponseEntity<DemolitionCalculationResponse>(calculationRes, HttpStatus.OK);
	}

}
