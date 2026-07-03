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

package org.egov.common.entity.edcr;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class OcComparisonBlockDetail {

    private Long number;

    private Long noOfFloorsOc = 0L;

    private Long noOfFloorsPermit = 0L;

    private BigDecimal hghtFromGroundOc = BigDecimal.ZERO;

    private BigDecimal hgtFromGroundPermit = BigDecimal.ZERO;;

    private List<OcComparisonReportFloorDetail> comparisonReportFloorDetails = new ArrayList<>();
    
    private BigDecimal minFrontOc = BigDecimal.ZERO;
    
    private BigDecimal minFrontPermit = BigDecimal.ZERO;
    
    private BigDecimal minRearOc;
    
    private BigDecimal minRearPermit;

    private BigDecimal minSide1Oc;

    private BigDecimal minSide1Permit;
    
    private BigDecimal minSide2Oc;

    private BigDecimal minSide2Permit;

    private BigDecimal totalCumulativeFrontAndRearOc = BigDecimal.ZERO;

    private BigDecimal totalCumulativeFrontAndRearPermit = BigDecimal.ZERO;
    
    private BigDecimal totalCumulativeSideOc = BigDecimal.ZERO;
    
    private BigDecimal totalCumulativeSidePermit = BigDecimal.ZERO;

    public Long getNumber() {
        return number;
    }

    public void setNumber(Long number) {
        this.number = number;
    }

    public Long getNoOfFloorsOc() {
        return noOfFloorsOc;
    }

    public void setNoOfFloorsOc(Long noOfFloorsOc) {
        this.noOfFloorsOc = noOfFloorsOc;
    }

    public Long getNoOfFloorsPermit() {
        return noOfFloorsPermit;
    }

    public void setNoOfFloorsPermit(Long noOfFloorsPermit) {
        this.noOfFloorsPermit = noOfFloorsPermit;
    }

    public BigDecimal getHghtFromGroundOc() {
        return hghtFromGroundOc;
    }

    public void setHghtFromGroundOc(BigDecimal hghtFromGroundOc) {
        this.hghtFromGroundOc = hghtFromGroundOc;
    }

    public BigDecimal getHgtFromGroundPermit() {
        return hgtFromGroundPermit;
    }

    public void setHgtFromGroundPermit(BigDecimal hgtFromGroundPermit) {
        this.hgtFromGroundPermit = hgtFromGroundPermit;
    }

    public List<OcComparisonReportFloorDetail> getComparisonReportFloorDetails() {
        return comparisonReportFloorDetails;
    }

    public void setComparisonReportFloorDetails(List<OcComparisonReportFloorDetail> comparisonReportFloorDetails) {
        this.comparisonReportFloorDetails = comparisonReportFloorDetails;
    }

	public BigDecimal getMinFrontOc() {
		return minFrontOc;
	}

	public void setMinFrontOc(BigDecimal minFrontOc) {
		this.minFrontOc = minFrontOc;
	}

	public BigDecimal getMinFrontPermit() {
		return minFrontPermit;
	}

	public void setMinFrontPermit(BigDecimal minFrontPermit) {
		this.minFrontPermit = minFrontPermit;
	}

	public BigDecimal getTotalCumulativeFrontAndRearOc() {
		return totalCumulativeFrontAndRearOc;
	}

	public void setTotalCumulativeFrontAndRearOc(BigDecimal totalCumulativeFrontAndRearOc) {
		this.totalCumulativeFrontAndRearOc = totalCumulativeFrontAndRearOc;
	}

	public BigDecimal getTotalCumulativeFrontAndRearPermit() {
		return totalCumulativeFrontAndRearPermit;
	}

	public void setTotalCumulativeFrontAndRearPermit(BigDecimal totalCumulativeFrontAndRearPermit) {
		this.totalCumulativeFrontAndRearPermit = totalCumulativeFrontAndRearPermit;
	}

	public BigDecimal getTotalCumulativeSideOc() {
		return totalCumulativeSideOc;
	}

	public void setTotalCumulativeSideOc(BigDecimal totalCumulativeSideOc) {
		this.totalCumulativeSideOc = totalCumulativeSideOc;
	}

	public BigDecimal getTotalCumulativeSidePermit() {
		return totalCumulativeSidePermit;
	}

	public void setTotalCumulativeSidePermit(BigDecimal totalCumulativeSidePermit) {
		this.totalCumulativeSidePermit = totalCumulativeSidePermit;
	}

	public BigDecimal getMinRearOc() {
		return minRearOc;
	}

	public void setMinRearOc(BigDecimal minRearOc) {
		this.minRearOc = minRearOc;
	}

	public BigDecimal getMinRearPermit() {
		return minRearPermit;
	}

	public void setMinRearPermit(BigDecimal minRearPermit) {
		this.minRearPermit = minRearPermit;
	}

	public BigDecimal getMinSide1Oc() {
		return minSide1Oc;
	}

	public void setMinSide1Oc(BigDecimal minSide1Oc) {
		this.minSide1Oc = minSide1Oc;
	}

	public BigDecimal getMinSide1Permit() {
		return minSide1Permit;
	}

	public void setMinSide1Permit(BigDecimal minSide1Permit) {
		this.minSide1Permit = minSide1Permit;
	}

	public BigDecimal getMinSide2Oc() {
		return minSide2Oc;
	}

	public void setMinSide2Oc(BigDecimal minSide2Oc) {
		this.minSide2Oc = minSide2Oc;
	}

	public BigDecimal getMinSide2Permit() {
		return minSide2Permit;
	}

	public void setMinSide2Permit(BigDecimal minSide2Permit) {
		this.minSide2Permit = minSide2Permit;
	}

    
}
