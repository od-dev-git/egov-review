package org.egov.bpa.web.controller;

import javax.validation.Valid;

import org.egov.bpa.service.notification.BPANotificationService;
import org.egov.bpa.web.model.RequestInfoWrapper;
import org.egov.common.contract.request.RequestInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/bpa/notification")
public class NotificationController {
	
	@Autowired
	BPANotificationService notificationService;
	
	@PostMapping(value = "/_payment")
	public ResponseEntity<String> paymentNotification(@Valid @RequestBody RequestInfoWrapper requestInfoWrapper) {
		String response = notificationService.sendPaymentSMS(requestInfoWrapper.getRequestInfo());
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

}
