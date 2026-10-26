package org.egov.dsc;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "dsc.pdf.sig.shared-path=${user.dir}/target/test-pdf-signatures")
class DigitalSignatureApplicationTests {

	@Test
	void contextLoads() {
	}

}
