package org.egov.edcr.service;

import java.io.InputStream;
import java.util.LinkedHashMap;

import org.egov.common.entity.edcr.Plan;
import org.egov.infra.microservice.models.RequestInfo;
import org.springframework.stereotype.Service;

@Service
public class PermitOrderServiceBPA3 extends PermitOrderServiceBPA2 {

	@Override
	public InputStream generateReport(Plan plan, LinkedHashMap bpaApplication, RequestInfo requestInfo, Boolean isPreview) {
		return super.generateReport(plan, bpaApplication, requestInfo, isPreview);
	}
	
}
