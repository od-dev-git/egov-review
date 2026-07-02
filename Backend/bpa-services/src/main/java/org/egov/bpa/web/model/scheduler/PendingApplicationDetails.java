package org.egov.bpa.web.model.scheduler;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class PendingApplicationDetails {

	private String id;
	private String tenantId;
	private String applicationNo;
}
