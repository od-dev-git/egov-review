package org.egov.dsc.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import java.util.TreeSet;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;

import org.egov.common.contract.request.RequestInfo;
import org.egov.common.contract.request.User;
import org.junit.jupiter.api.Test;

/**
 * Each endpoint validates DataSignRequest with its own group; these pin which fields each
 * operation requires so a change to one group can't silently loosen or break another.
 */
class DataSignRequestValidationTest {

	private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

	@Test
	void fullyPopulatedRequestPassesEveryGroup() {
		DataSignRequest request = complete();
		for (Class<?> group : new Class<?>[] { DataSignRequest.DataSignInput.class, DataSignRequest.DataSign.class,
				DataSignRequest.PdfSignInput.class, DataSignRequest.PdfSign.class, DataSignRequest.Deregister.class }) {
			assertEquals(new TreeSet<String>(), violations(request, group), group.getSimpleName());
		}
	}

	@Test
	void missingUserIdIsRejectedWithoutNpe() {
		DataSignRequest request = complete();
		request.getRequestInfo().setUserInfo(null);

		assertEquals(set("userIdPresent"), violations(request, DataSignRequest.Deregister.class));
	}

	@Test
	void missingRequestInfoIsReportedOnce() {
		DataSignRequest request = complete();
		request.setRequestInfo(null);

		assertEquals(set("requestInfo"), violations(request, DataSignRequest.DataSign.class));
	}

	@Test
	void pdfSignRequiresEchoedTempFileAndUploadFields() {
		DataSignRequest request = complete();
		request.setTempFilePath(null);
		request.setModuleName(" ");

		assertEquals(set("moduleName", "tempFilePath"), violations(request, DataSignRequest.PdfSign.class));
	}

	@Test
	void groupsOnlyRequireWhatTheirEndpointUses() {
		DataSignRequest request = new DataSignRequest();
		request.setRequestInfo(complete().getRequestInfo());
		request.setChannelId("ch1");

		// Deregister needs only the user and channel; DataSign also needs the emBridge reply
		assertTrue(violations(request, DataSignRequest.Deregister.class).isEmpty());
		assertEquals(set("responseData"), violations(request, DataSignRequest.DataSign.class));
	}

	private Set<String> violations(DataSignRequest request, Class<?> group) {
		Set<String> fields = new TreeSet<>();
		for (ConstraintViolation<DataSignRequest> v : validator.validate(request, group)) {
			fields.add(v.getPropertyPath().toString());
		}
		return fields;
	}

	private static Set<String> set(String... fields) {
		Set<String> set = new TreeSet<>();
		for (String f : fields) {
			set.add(f);
		}
		return set;
	}

	private static DataSignRequest complete() {
		User user = new User();
		user.setId(7L);
		RequestInfo requestInfo = new RequestInfo();
		requestInfo.setUserInfo(user);

		DataSignRequest request = new DataSignRequest();
		request.setRequestInfo(requestInfo);
		request.setTokenDisplayName("token");
		request.setKeyStorePassPhrase("pin");
		request.setKeyId("key");
		request.setChannelId("ch1");
		request.setResponseData("reply");
		request.setFileBytes("fileStoreId");
		request.setFileName("doc");
		request.setTenantId("od");
		request.setModuleName("module");
		request.setTempFilePath("/opt/egov/DS/Temp/x.sig");
		return request;
	}
}
