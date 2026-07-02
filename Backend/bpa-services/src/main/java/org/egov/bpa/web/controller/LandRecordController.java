package org.egov.bpa.web.controller;

import org.egov.bpa.service.LandRecordService;
import org.egov.bpa.util.ResponseInfoFactory;
import org.egov.bpa.web.model.LandRecordResponse;
import org.egov.bpa.web.model.RequestInfoWrapper;
import org.egov.bpa.web.model.landInfo.LandRecordDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1")
public class LandRecordController {

	@Autowired
	private LandRecordService landRecordService;
	
	@Autowired
	private ResponseInfoFactory responseInfoFactory;

	@PostMapping(value = "/bhulekh/_search")
	public ResponseEntity<LandRecordResponse> searchLandRecords(@RequestBody RequestInfoWrapper requestInfoWrapper,
			 @ModelAttribute LandRecordDTO landRecord) {
		
		//Input will be LGD codes and plotNumber
		LandRecordDTO landrecord = landRecordService.searchLandRecords(landRecord);
		LandRecordResponse response = LandRecordResponse.builder().landRecord(landrecord).responseInfo(
				responseInfoFactory.createResponseInfoFromRequestInfo(requestInfoWrapper.getRequestInfo(), true))
				.build();
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	@PostMapping(value = "/eregistration/_search")
	public ResponseEntity<LandRecordResponse> searchLandRevenueDetail(@RequestBody RequestInfoWrapper requestInfoWrapper,
			 @ModelAttribute LandRecordDTO landRecord)  {

		//Input will be revenue codes and plotNumber
		LandRecordDTO landrecord = landRecordService.searchLandRevenueDetail(landRecord);
		LandRecordResponse response = LandRecordResponse.builder().landRecord(landrecord).responseInfo(
				responseInfoFactory.createResponseInfoFromRequestInfo(requestInfoWrapper.getRequestInfo(), true))
				.build();
		return new ResponseEntity<>(response, HttpStatus.OK);
	}
}
			
		


