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

import java.io.Serializable;
import java.math.BigDecimal;

public class OdishaParkingHelper implements Serializable {

    private static final long serialVersionUID = 580L;
   
    public BigDecimal eWSOrLigParkingProvided=BigDecimal.ZERO;
    public BigDecimal mIGParkingProvided=BigDecimal.ZERO;
    public BigDecimal visitorParkingProvided=BigDecimal.ZERO;
    public BigDecimal stiltParkingProvided=BigDecimal.ZERO;
    public BigDecimal basementParkingProvided=BigDecimal.ZERO;
    public BigDecimal openParkingProvided=BigDecimal.ZERO;
    public BigDecimal daPARKINGProvided=BigDecimal.ZERO;
    public BigDecimal daPARKINGWidthProvided=BigDecimal.ZERO;
    public int daPARKINGCountProvided=0;
    public BigDecimal twoWheelerParkingProvided=BigDecimal.ZERO;
    public BigDecimal bicycleParkingProvided=BigDecimal.ZERO;
    public BigDecimal roofTopParkingProvided=BigDecimal.ZERO;
    public BigDecimal staffParkingProvided=BigDecimal.ZERO;
    public BigDecimal totalParkingProvided=BigDecimal.ZERO;
    public BigDecimal distFromDAToMainEntranceProvided=BigDecimal.ZERO;
    public BigDecimal offSiteParkingprovisionsProvided=BigDecimal.ZERO;
    public BigDecimal evChargingPointProvided=BigDecimal.ZERO;
    public BigDecimal stackParkingProvided=BigDecimal.ZERO;
    public int stackParkingCountProvided=0;
    public int totalParkingUnitsProvided=0;  
    public int openParkingUnits=0;
    public int basementParkingUnits=0;
    public int stiltParkingUnits=0;
    public int rooftopParkingUnits=0;
    
    public BigDecimal eWSOrLigParkingRequired=BigDecimal.ZERO;
    public BigDecimal mIGParkingRequired=BigDecimal.ZERO;
    public BigDecimal visitorParkingRequired=BigDecimal.ZERO;
    public BigDecimal stiltParkingRequired=BigDecimal.ZERO;
    public BigDecimal basementParkingRequired=BigDecimal.ZERO;
    public BigDecimal openParkingRequired=BigDecimal.ZERO;
    public BigDecimal daPARKINGRequired=BigDecimal.ZERO;
    public BigDecimal daPARKINGWidthRequired=BigDecimal.ZERO;
    public int daPARKINGCountRequired=0;
    public BigDecimal twoWheelerParkingRequired=BigDecimal.ZERO;
    public BigDecimal bicycleParkingRequired=BigDecimal.ZERO;
    public BigDecimal roofTopParkingRequired=BigDecimal.ZERO;
    public BigDecimal staffParkingRequired=BigDecimal.ZERO;
    public BigDecimal totalParkingRequired=BigDecimal.ZERO;
    public BigDecimal distFromDAToMainEntranceRequired=BigDecimal.ZERO;
    public BigDecimal offSiteParkingprovisionsRequired=BigDecimal.ZERO;
    public BigDecimal evChargingPointRequired=BigDecimal.ZERO;
    public BigDecimal stackParkingRequired=BigDecimal.ZERO;
    public int stackParkingCountRequired=0;
    
    
}
