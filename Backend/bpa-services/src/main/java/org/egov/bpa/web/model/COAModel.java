package org.egov.bpa.web.model;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class COAModel {

	private String architectName;
	private String registrationNo;
	private String disciplinary;
	private String address;
	private String mobile;
	private String email;
	private Boolean active;

}
