package org.egov.bpa.web.model.landInfo;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class NewOwnerInfo {
	
	@JsonProperty("id")
	private String id;

	@JsonProperty("name")
	private String name = null;

	@JsonProperty("mobileNumber")
	private String mobileNumber = null;

	@JsonProperty("dob")
	private Long dob = null;

	@JsonProperty("emailId")
	private String emailId = null;

	@JsonProperty("fatherOrHusbandName")
	private String fatherOrHusbandName = null;

	@JsonProperty("relationship")
	private String relationship = null;

	@JsonProperty("pan")
	private String pan = null;
	
	@JsonProperty("gender")
	private String gender = null;

	@JsonProperty("correspondenceAddress")
	private String correspondenceAddress = null;

	@JsonProperty("isPrimaryOwner")
	private Boolean isPrimaryOwner;

	@JsonProperty("additionalDetails")
	private Object additionalDetails = null;

	@JsonProperty("createdBy")
	private String createdBy;

	@JsonProperty("lastModifiedBy")
	private String lastModifiedBy;

	@JsonProperty("createdDate")
	private Long createdDate;

	@JsonProperty("lastModifiedDate")
	private Long lastModifiedDate;

	@JsonProperty("landInfoId")
	private String landInfoId;
	
	@JsonProperty("newOwnerShipMajorType")
	private String newOwnerShipMajorType;
	
	@JsonProperty("newOwnershipCategory")
	private String newOwnershipCategory;


}
