package org.egov.pt.models.issuefix;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StatusMismatchIssueFix {

	private String tenantId;

	private String applicationNo;

	private String actionInProcessInstance;

	private String currentStatus;

	private String expectedStatus;

}
