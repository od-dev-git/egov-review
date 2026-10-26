package org.egov.dsc.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Guards the token passphrase: it must be readable from a request but never written back out,
 * whether by Jackson (responses, body-logging middleware) or by toString (log statements).
 */
class DataSignRequestTest {

	private static final String PASSPHRASE = "s3cret-token-pin";

	private final ObjectMapper mapper = new ObjectMapper();

	@Test
	void passphraseIsDeserialized() throws Exception {
		DataSignRequest request = mapper.readValue("{\"keyStorePassPhrase\":\"" + PASSPHRASE + "\"}",
				DataSignRequest.class);

		assertEquals(PASSPHRASE, request.getKeyStorePassPhrase());
	}

	@Test
	void passphraseIsNeverSerialized() throws Exception {
		DataSignRequest request = new DataSignRequest();
		request.setKeyStorePassPhrase(PASSPHRASE);

		String json = mapper.writeValueAsString(request);

		assertFalse(json.contains(PASSPHRASE), json);
		assertFalse(json.contains("keyStorePassPhrase"), json);
	}

	@Test
	void bodyUserIdIsIgnoredSoSignerComesOnlyFromRequestInfo() throws Exception {
		DataSignRequest request = mapper.readValue(
				"{\"RequestInfo\":{\"userInfo\":{\"id\":7}},\"userId\":999,\"channelId\":\"ch1\"}",
				DataSignRequest.class);

		assertEquals(Long.valueOf(7), request.getRequestInfo().getUserInfo().getId());
		assertFalse(mapper.writeValueAsString(request).contains("999"));
	}

	@Test
	void passphraseIsExcludedFromToString() {
		DataSignRequest request = new DataSignRequest();
		request.setKeyStorePassPhrase(PASSPHRASE);

		assertFalse(request.toString().contains(PASSPHRASE), request.toString());
	}
}
