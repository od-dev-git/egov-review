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

package org.egov.bpa.calculator.web.models.bpa.edcr;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*All the details extracted from the plan are referred in this object*/
@JsonIgnoreProperties(ignoreUnknown = true)
public class Plan implements Serializable {

	private static final long serialVersionUID = 7276648029097296311L;



	/**
	 * decides on what date scrutiny should be done
	 */

	/**
	 * Planinformation captures the declarations of the plan.Plan information
	 * captures the boundary, building location details,surrounding building NOC's
	 * etc. User will assert the details about the plot. The same will be used to
	 * print in plan report.
	 */
	private PlanInformation planInformation;
	// Plot and Set back details.
	private Plot plot;

	// Single plan contain multiple block/building information. Records Existing and
	// proposed block information.
	private List<Block> blocks = new ArrayList<>();

	private VirtualBuilding virtualBuilding = new VirtualBuilding();

	// List of occupancies present in the plot including all the blocks.

	@JsonIgnore
	private transient Map<String, Map<String, Integer>> subFeatureColorCodesMaster = new HashMap<>();

	// coverage Overall Coverage of all the block. Total area of all the floor/plot
	// area.
	private BigDecimal coverage = BigDecimal.ZERO;

	// Calculated Permissible FSI and provided FSI details
	private FarDetails farDetails;

	private Boolean strictlyValidateDimension = false;

	private transient Map<String, List<Object>> mdmsMasterData;
	private transient Boolean mainDcrPassed = false;

	private BigDecimal totalEWSAreaInPlot = BigDecimal.ZERO;

	private BigDecimal totalEWSFeeEffectiveArea = BigDecimal.ZERO;
	
	private String architectInformation;

	private List<BigDecimal> depthCuttings = new ArrayList<>();

	public List<BigDecimal> getDepthCuttings() {
		return depthCuttings;
	}

	public void setDepthCuttings(List<BigDecimal> depthCuttings) {
		this.depthCuttings = depthCuttings;
	}

	public List<Block> getBlocks() {
		return blocks;
	}

	public void setBlocks(List<Block> blocks) {
		this.blocks = blocks;
	}

	public Block getBlockByName(String blockName) {
		for (Block block : getBlocks()) {
			if (block.getName().equalsIgnoreCase(blockName))
				return block;
		}
		return null;
	}

	public PlanInformation getPlanInformation() {
		return planInformation;
	}

	public void setPlanInformation(PlanInformation planInformation) {
		this.planInformation = planInformation;
	}

	public Plot getPlot() {
		return plot;
	}

	public void setPlot(Plot plot) {
		this.plot = plot;
	}

	public VirtualBuilding getVirtualBuilding() {
		return virtualBuilding;
	}

	public void setVirtualBuilding(VirtualBuilding virtualBuilding) {
		this.virtualBuilding = virtualBuilding;
	}

	public BigDecimal getCoverage() {
		return coverage;
	}

	public void setCoverage(BigDecimal coverage) {
		this.coverage = coverage;
	}


	public Map<String, Map<String, Integer>> getSubFeatureColorCodesMaster() {
		return subFeatureColorCodesMaster;
	}

	public void setSubFeatureColorCodesMaster(Map<String, Map<String, Integer>> subFeatureColorCodesMaster) {
		this.subFeatureColorCodesMaster = subFeatureColorCodesMaster;
	}

	public FarDetails getFarDetails() {
		return farDetails;
	}

	public void setFarDetails(FarDetails farDetails) {
		this.farDetails = farDetails;
	}


	public Boolean getStrictlyValidateDimension() {
		return strictlyValidateDimension;
	}

	public void setStrictlyValidateDimension(Boolean strictlyValidateDimension) {
		this.strictlyValidateDimension = strictlyValidateDimension;
	}
	public Map<String, List<Object>> getMdmsMasterData() {
		return mdmsMasterData;
	}

	public void setMdmsMasterData(Map<String, List<Object>> mdmsMasterData) {
		this.mdmsMasterData = mdmsMasterData;
	}

	public Boolean getMainDcrPassed() {
		return mainDcrPassed;
	}

	public void setMainDcrPassed(Boolean mainDcrPassed) {
		this.mainDcrPassed = mainDcrPassed;
	}

	public BigDecimal getTotalEWSAreaInPlot() {
		return totalEWSAreaInPlot;
	}

	public void setTotalEWSAreaInPlot(BigDecimal totalEWSAreaInPlot) {
		this.totalEWSAreaInPlot = totalEWSAreaInPlot;
	}

	public BigDecimal getTotalEWSFeeEffectiveArea() {
		return totalEWSFeeEffectiveArea;
	}

	public void setTotalEWSFeeEffectiveArea(BigDecimal totalEWSFeeEffectiveArea) {
		this.totalEWSFeeEffectiveArea = totalEWSFeeEffectiveArea;
	}

	public String getArchitectInformation() {
		return architectInformation;
	}

	public void setArchitectInformation(String architectInformation) {
		this.architectInformation = architectInformation;
	}

	private HashMap<String, String> initPlanInfo() {
		HashMap<String, String> planInfo = new HashMap<String, String>();

		planInfo.put("AREA_TYPE", "NEW");
		return planInfo;
	}
	
}
