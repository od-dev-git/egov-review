package org.egov.commons.payments.model;

public class CalculationCriteria {

	private String applicationNo;
	
	private String applicationType;

	private String feeType;

	private String riskType;

	private String serviceType;

	private String tenantId;
	
	private Boolean isOCOutsideSujogApplication=Boolean.FALSE;

	public Boolean getIsOCOutsideSujogApplication() {
		return isOCOutsideSujogApplication;
	}

	public void setIsOCOutsideSujogApplication(Boolean isOCOutsideSujogApplication) {
		this.isOCOutsideSujogApplication = isOCOutsideSujogApplication;
	}

	public String getApplicationNo() {
		return applicationNo;
	}

	public void setApplicationNo(String applicationNo) {
		this.applicationNo = applicationNo;
	}

	public String getApplicationType() {
		return applicationType;
	}

	public void setApplicationType(String applicationType) {
		this.applicationType = applicationType;
	}

	public String getFeeType() {
		return feeType;
	}

	public void setFeeType(String feeType) {
		this.feeType = feeType;
	}

	public String getRiskType() {
		return riskType;
	}

	public void setRiskType(String riskType) {
		this.riskType = riskType;
	}

	public String getServiceType() {
		return serviceType;
	}

	public void setServiceType(String serviceType) {
		this.serviceType = serviceType;
	}

	public String getTenantId() {
		return tenantId;
	}

	public void setTenantId(String tenantId) {
		this.tenantId = tenantId;
	}

}
