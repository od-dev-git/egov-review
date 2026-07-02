package org.egov.bpa.calculator.web.controllers;

import java.util.List;

import javax.validation.Valid;

import org.egov.bpa.calculator.services.RegularizationCalculationService;
import org.egov.bpa.calculator.web.models.regularization.CalculationRequest;
import org.egov.bpa.calculator.web.models.regularization.CalculationResponse;
import org.egov.bpa.calculator.web.models.regularization.RegularizationCalculation;
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
@RequestMapping("/regularization")
public class RegularizationCalculatorController {
	
	
	@Autowired
	private RegularizationCalculationService calculationService;

	/**
	 * Controller to get Estimate for a Regularization
	 * 
	 * @param calculationRequest
	 * @return
	 */
	@RequestMapping(value = "/_estimate", method = RequestMethod.POST)
	public ResponseEntity<CalculationResponse> estimate(@Valid @RequestBody CalculationRequest calculationRequest) {
		
		log.info("CalculationRequest:: " + calculationRequest);
		List<RegularizationCalculation> calculations = calculationService.getEstimate(calculationRequest);
		CalculationResponse response = CalculationResponse.builder().calculations(calculations).build();

		return new ResponseEntity<CalculationResponse>(response, HttpStatus.OK);
	}

	/**
	 * Controller to generate demand for a Regularization
	 * 
	 * @param calculationRequest
	 * @return
	 */
	@RequestMapping(value = "/_calculate", method = RequestMethod.POST)
	public ResponseEntity<CalculationResponse> calculate(@Valid @RequestBody CalculationRequest calculationRequest) {
		 log.debug("CalculationReaquest:: " + calculationRequest);
		 List<RegularizationCalculation> calculations = calculationService.calculate(calculationRequest);
		 CalculationResponse calculationRes = CalculationResponse.builder().calculations(calculations).build();
		 return new ResponseEntity<CalculationResponse>(calculationRes,HttpStatus.OK);
	}
}
