package org.egov.edcr.entity.dto.plan;

import java.util.Date;
import java.util.List;

import org.egov.common.entity.ApplicationType;
import org.egov.common.entity.edcr.PlanInformation;

public class PlanResponseDTO {
	private Date applicationDate;
	private ApplicationType applicationType;
	private PlanInformation planInformation;
	private PlotDTO plot;
	private List<BlockDTO> blocks;
	private VirtualBuildingDTO virtualBuilding;
	private List<OccupancyDTO> occupancies;
	private Boolean isDxfToPdfEnabled;

	public PlanResponseDTO(Date applicationDate, ApplicationType applicationType, PlanInformation planInformation,
			PlotDTO plot, List<BlockDTO> blocks, VirtualBuildingDTO virtualBuilding, List<OccupancyDTO> occupancies,
			Boolean isDxfToPdfEnabled) {
		this.applicationDate = applicationDate;
		this.applicationType = applicationType;
		this.planInformation = planInformation;
		this.plot = plot;
		this.blocks = blocks;
		this.virtualBuilding = virtualBuilding;
		this.occupancies = occupancies;
		this.isDxfToPdfEnabled = isDxfToPdfEnabled;
	}

	public PlanResponseDTO() {
	}

	public Date getApplicationDate() {
		return applicationDate;
	}

	public void setApplicationDate(Date applicationDate) {
		this.applicationDate = applicationDate;
	}

	public ApplicationType getApplicationType() {
		return applicationType;
	}

	public void setApplicationType(ApplicationType applicationType) {
		this.applicationType = applicationType;
	}

	public PlanInformation getPlanInformation() {
		return planInformation;
	}

	public void setPlanInformation(PlanInformation planInformation) {
		this.planInformation = planInformation;
	}

	public PlotDTO getPlot() {
		return plot;
	}

	public void setPlot(PlotDTO plot) {
		this.plot = plot;
	}

	public List<BlockDTO> getBlocks() {
		return blocks;
	}

	public void setBlocks(List<BlockDTO> blocks) {
		this.blocks = blocks;
	}

	public VirtualBuildingDTO getVirtualBuilding() {
		return virtualBuilding;
	}

	public void setVirtualBuilding(VirtualBuildingDTO virtualBuilding) {
		this.virtualBuilding = virtualBuilding;
	}

	public List<OccupancyDTO> getOccupancies() {
		return occupancies;
	}

	public void setOccupancies(List<OccupancyDTO> occupancies) {
		this.occupancies = occupancies;
	}

	public Boolean getIsDxfToPdfEnabled() {
		return isDxfToPdfEnabled;
	}

	public void setIsDxfToPdfEnabled(Boolean isDxfToPdfEnabled) {
		this.isDxfToPdfEnabled = isDxfToPdfEnabled;
	}
}