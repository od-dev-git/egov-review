/*
 * eGov  SmartCity eGovernance suite aims to improve the internal efficiency,transparency,
 * accountability and the service delivery of the government  organizations.
 *
 *  Copyright (C) <2019>  eGovernments Foundation
 *
 *  The updated version of eGov suite of products as by eGovernments Foundation
 *  is available at http://www.egovernments.org
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program. If not, see http://www.gnu.org/licenses/ or
 *  http://www.gnu.org/licenses/gpl.html .
 *
 *  In addition to the terms of the GPL license to be adhered to in using this
 *  program, the following additional terms are to be complied with:
 *
 *      1) All versions of this program, verbatim or modified must carry this
 *         Legal Notice.
 *      Further, all user interfaces, including but not limited to citizen facing interfaces,
 *         Urban Local Bodies interfaces, dashboards, mobile applications, of the program and any
 *         derived works should carry eGovernments Foundation logo on the top right corner.
 *
 *      For the logo, please refer http://egovernments.org/html/logo/egov_logo.png.
 *      For any further queries on attribution, including queries on brand guidelines,
 *         please contact contact@egovernments.org
 *
 *      2) Any misrepresentation of the origin of the material is prohibited. It
 *         is required that all modified versions of this material be marked in
 *         reasonable ways as different from the original version.
 *
 *      3) This license does not grant any rights to any user of the program
 *         with regards to rights under trademark law for use of the trade names
 *         or trademarks of eGovernments Foundation.
 *
 *  In case of any queries, you can reach eGovernments Foundation at contact@egovernments.org.
 */

package org.egov.bpa.web.model.edcr;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

//These are the declarations of the applicant in the plan using PLAN_INFO layer.
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanInformation implements Serializable {

    private static final String NA = "NA";
    public static final String SEQ_EDCR_PLANINFO = "SEQ_EDCR_PLANINFO";
    private static final long serialVersionUID = 4L;

    private BigDecimal plotArea = BigDecimal.ZERO;


    private String occupancy;
    //Temporary field used for service type.
    private String serviceType;


    private String plotNo;
    //Extracted from Plan info.Khata number.
    private String khataNo;
    //Extracted from Plan info.Mauza number.

    
    private String riskType;

    
    //PER_ACRE_BENCHMARK_VALUE_OF_LAND_NEEDED_IF_PROJECT_IS_HAVING_PURCHASABLE_FAR_COMPONENT = number
    private BigDecimal benchmarkValuePerAcre=BigDecimal.ZERO;
    
    private boolean shelterFeeRequired;
    
    private long totalNoOfDwellingUnits;
    
    private List<String> additionalDocuments =new ArrayList<String>();//2,4,7
    
    private boolean isSecurityDepositRequired;
    
    private BigDecimal totalPlotArea=BigDecimal.ZERO;

	private BigDecimal projectValueForEIDP;

	private Boolean isRetentionFeeApplicable=Boolean.FALSE;

	private BigDecimal numberOfTemporaryStructures=BigDecimal.ZERO;

	private String isProjectUndertakingByGovt;

	private String businessService;
	
	private List<String> requiredNOCs;


    public BigDecimal getPlotArea() {
        return plotArea;
    }

    public void setPlotArea(BigDecimal plotArea) {
        this.plotArea = plotArea;
    }

    public String getServiceType() {
        return serviceType;
    }

    public void setServiceType(String serviceType) {
        this.serviceType = serviceType;
    }

    public String getOccupancy() {
        return occupancy;
    }

    public void setOccupancy(String occupancy) {
        this.occupancy = occupancy;
    }





    public String getPlotNo() {
        return plotNo;
    }

    public void setPlotNo(String plotNo) {
        this.plotNo = plotNo;
    }

    public String getKhataNo() {
        return khataNo;
    }

    public void setKhataNo(String khataNo) {
        this.khataNo = khataNo;
    }


	public BigDecimal getBenchmarkValuePerAcre() {
		return benchmarkValuePerAcre;
	}

	public void setBenchmarkValuePerAcre(BigDecimal benchmarkValuePerAcre) {
		this.benchmarkValuePerAcre = benchmarkValuePerAcre;
	}


	public boolean isShelterFeeRequired() {
		return shelterFeeRequired;
	}

	public void setShelterFeeRequired(boolean shelterFeeRequired) {
		this.shelterFeeRequired = shelterFeeRequired;
	}

	public long getTotalNoOfDwellingUnits() {
		return totalNoOfDwellingUnits;
	}

	public void setTotalNoOfDwellingUnits(long totalNoOfDwellingUnits) {
		this.totalNoOfDwellingUnits = totalNoOfDwellingUnits;
	}

		public String getRiskType() {
		return riskType;
	}

	public void setRiskType(String riskType) {
		this.riskType = riskType;
	}

	public List<String> getAdditionalDocuments() {
		return additionalDocuments;
	}

	public void setAdditionalDocuments(List<String> additionalDocuments) {
		this.additionalDocuments = additionalDocuments;
	}

	public boolean isSecurityDepositRequired() {
		return isSecurityDepositRequired;
	}

	public void setSecurityDepositRequired(boolean isSecurityDepositRequired) {
		this.isSecurityDepositRequired = isSecurityDepositRequired;
	}


	public String getBusinessService() {
		return businessService;
	}

	public void setBusinessService(String businessService) {
		this.businessService = businessService;
	}

	public BigDecimal getTotalPlotArea() {
		return totalPlotArea;
	}

	public void setTotalPlotArea(BigDecimal totalPlotArea) {
		this.totalPlotArea = totalPlotArea;
	}

	public BigDecimal getProjectValueForEIDP() {
		return projectValueForEIDP;
	}

	public void setProjectValueForEIDP(BigDecimal projectValueForEIDP) {
		this.projectValueForEIDP = projectValueForEIDP;
	}

	public Boolean getRetentionFeeApplicable() {
		return isRetentionFeeApplicable;
	}

	public void setRetentionFeeApplicable(Boolean retentionFeeApplicable) {
		isRetentionFeeApplicable = retentionFeeApplicable;
	}

	public BigDecimal getNumberOfTemporaryStructures() {
		return numberOfTemporaryStructures;
	}

	public void setNumberOfTemporaryStructures(BigDecimal numberOfTemporaryStructures) {
		this.numberOfTemporaryStructures = numberOfTemporaryStructures;
	}

	public String getIsProjectUndertakingByGovt() {
		return isProjectUndertakingByGovt;
	}

	public void setIsProjectUndertakingByGovt(String isProjectUndertakingByGovt) {
		this.isProjectUndertakingByGovt = isProjectUndertakingByGovt;
	}

	public List<String> getRequiredNOCs() {
		return requiredNOCs;
	}

	public void setRequiredNOCs(List<String> requiredNOCs) {
		this.requiredNOCs = requiredNOCs;
	}
	
	
}
