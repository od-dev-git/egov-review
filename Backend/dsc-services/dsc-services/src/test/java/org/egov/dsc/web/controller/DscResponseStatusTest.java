package org.egov.dsc.web.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.egov.dsc.model.DataSignResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class DscResponseStatusTest {

	private final DscController controller = new DscController();

	@Test
	void dataSignFailureUsesNonSuccessHttpAndResponseInfoStatus() {
		ResponseEntity<DataSignResponse> response = controller.buildDataSignResponse("", null, null,
				"DSC_ERR_18", null);

		assertEquals(HttpStatus.BAD_GATEWAY, response.getStatusCode());
		assertEquals(HttpStatus.BAD_GATEWAY.toString(), response.getBody().getResponseInfo().getStatus());
	}

	@Test
	void successfulDataSignKeepsHttpOk() {
		ResponseEntity<DataSignResponse> response = controller.buildDataSignResponse("Success", "file-id", null,
				null, null);

		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertEquals(HttpStatus.OK.toString(), response.getBody().getResponseInfo().getStatus());
	}
}
