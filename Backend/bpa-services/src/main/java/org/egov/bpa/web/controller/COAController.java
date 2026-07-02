package org.egov.bpa.web.controller;

import javax.validation.Valid;

import org.egov.bpa.service.COAService;
import org.egov.bpa.web.model.COAModel;
import org.egov.bpa.web.model.edcr.RequestInfoWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/COA")
public class COAController {

	@Autowired
	private COAService coaService;

	@PostMapping("/_search")
	public ResponseEntity<COAModel> getArchitectDetails(@Valid @RequestBody RequestInfoWrapper requestInfoWrapper,
			@RequestParam("regNo") String regNo) {
		COAModel coaModel = coaService.fetchCOAData(regNo);
		return new ResponseEntity<>(coaModel, HttpStatus.OK);
	}

}
