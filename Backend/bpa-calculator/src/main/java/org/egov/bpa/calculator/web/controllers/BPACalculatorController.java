package org.egov.bpa.calculator.web.controllers;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import org.egov.bpa.calculator.services.CalculationService;
import org.egov.bpa.calculator.services.DemandService;
import org.egov.bpa.calculator.web.models.Calculation;
import org.egov.bpa.calculator.web.models.CalculationReq;
import org.egov.bpa.calculator.web.models.CalculationRes;
import org.egov.bpa.calculator.web.models.InstallmentRequest;
import org.egov.bpa.calculator.web.models.RequestInfoWrapper;
import org.egov.bpa.calculator.web.models.demand.Demand;
import org.egov.common.contract.request.RequestInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@RestController
//@RequestMapping("/v1")
@Slf4j
public class BPACalculatorController {

	private ObjectMapper objectMapper;

	private HttpServletRequest request;

	private CalculationService calculationService;

	private DemandService demandService;

	@Autowired
	public BPACalculatorController(ObjectMapper objectMapper, HttpServletRequest request,
								CalculationService calculationService,DemandService demandService) {
		this.objectMapper = objectMapper;
		this.request = request;
		this.calculationService=calculationService;
		this.demandService=demandService;
	}

	/**
	 * Calulates the tradeLicense fee and creates Demand
	 * @param calculationReq The calculation Request
	 * @return Calculation Response
	 */
	@RequestMapping(value = "/_calculate", method = RequestMethod.POST)
	public ResponseEntity<CalculationRes> calculate(@Valid @RequestBody CalculationReq calculationReq) {
		log.debug("CalculationReaquest:: " + calculationReq);
		 List<Calculation> calculations = calculationService.calculate(calculationReq);
		 CalculationRes calculationRes = CalculationRes.builder().calculations(calculations).build();
		 return new ResponseEntity<CalculationRes>(calculationRes,HttpStatus.OK);
	}
	
	/**
	 * Calculates the BPA fee without creating Demand
	 * 
	 * @param calculationReq The calculation Request
	 * @return Calculation Response
	 */
	@RequestMapping(value = "/_estimate", method = RequestMethod.POST)
	public ResponseEntity<CalculationRes> estimate(@Valid @RequestBody CalculationReq calculationReq) {
		log.debug("CalculationReaquest:: " + calculationReq);
		List<Calculation> calculations = calculationService.getEstimate(calculationReq);
		CalculationRes calculationRes = CalculationRes.builder().calculations(calculations).build();
		return new ResponseEntity<CalculationRes>(calculationRes, HttpStatus.OK);
	}
	
	/**
	 * Calculates the BPA fee and stores in installments
	 * 
	 * @param calculationReq The calculation Request
	 * @return Calculation Response
	 */
	@RequestMapping(value = "/_calculateInInstallments", method = RequestMethod.POST)
	public ResponseEntity<CalculationRes> calculateInInstallments(@Valid @RequestBody CalculationReq calculationReq) {
		log.debug("calculateInInstallments request:: " + calculationReq);
		List<Calculation> calculations = calculationService.calculateInInstallments(calculationReq);
		CalculationRes calculationRes = CalculationRes.builder().calculations(calculations).build();
		return new ResponseEntity<CalculationRes>(calculationRes, HttpStatus.OK);
	}
	
	/**
	 * Fetch the installments
	 * 
	 * @param calculationReq The calculation Request
	 * @return Calculation Response
	 */
	@RequestMapping(value = "/_getAllInstallments", method = RequestMethod.POST)
	public ResponseEntity<Object> getAllInstallments(@Valid @RequestBody InstallmentRequest installmentRequest) {
		log.debug("inside _getAllInstallments");
		Object installments = calculationService.getAllInstallmentsV2(installmentRequest);
		return new ResponseEntity<Object>(installments, HttpStatus.OK);
	}
	
	/**
	 * Fetch the installments
	 * 
	 * @param calculationReq The calculation Request
	 * @return Calculation Response
	 */
	@RequestMapping(value = "/_generateDemandFromInstallments", method = RequestMethod.POST)
	public ResponseEntity<Object> generateDemandFromInstallments(@Valid @RequestBody InstallmentRequest installmentRequest) {
		log.debug("inside generateDemandFromInstallments");
		Object demands = calculationService.generateDemandsFromInstallment(installmentRequest);
		return new ResponseEntity<Object>(demands, HttpStatus.OK);
	}


}
