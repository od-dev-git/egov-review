package org.egov.demand.web.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class Sms {

	private String mobileNumber;
	private String message;
	private Category category;
	private Long expiryTime;

}
