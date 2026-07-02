package org.egov.demand.custom;

import static org.egov.demand.util.Constants.BILL_GEN_MANDATORY_FIELDS_MISSING_KEY;
import static org.egov.demand.util.Constants.BILL_GEN_MANDATORY_FIELDS_MISSING_MSG;

import org.egov.common.contract.request.RequestInfo;
import org.egov.demand.util.Util;
import org.egov.tracer.model.CustomException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class CustomBillValidator {

	@Autowired
	private Util util;

	public void validateBillGenRequest(BillCriteria billCriteria, RequestInfo requestInfo) {

		if (StringUtils.isEmpty(billCriteria.getTenantId())) {
			throw new CustomException(BILL_GEN_MANDATORY_FIELDS_MISSING_KEY, "tenantId can't be null OR empty");
		} else if (StringUtils.isEmpty(billCriteria.getConsumerCode())) {
			throw new CustomException(BILL_GEN_MANDATORY_FIELDS_MISSING_KEY, "consumer code can't be null OR empty");
		} else if (StringUtils.isEmpty(billCriteria.getBusinessService())) {
			throw new CustomException(BILL_GEN_MANDATORY_FIELDS_MISSING_KEY, "business service can't be null OR empty");
		}

	}

}
