package org.egov.edcr.entity.dto.plan;

public class OccupancyTypeHelperDTO {
	private OccupancyHelperDetailDTO type;
	private OccupancyHelperDetailDTO subtype;
	private OccupancyHelperDetailDTO usage;

	public OccupancyTypeHelperDTO(OccupancyHelperDetailDTO type, OccupancyHelperDetailDTO subtype,
			OccupancyHelperDetailDTO usage) {
		this.type = type;
		this.subtype = subtype;
		this.usage = usage;
	}

	public OccupancyHelperDetailDTO getType() {
		return type;
	}

	public void setType(OccupancyHelperDetailDTO type) {
		this.type = type;
	}

	public OccupancyHelperDetailDTO getSubtype() {
		return subtype;
	}

	public void setSubtype(OccupancyHelperDetailDTO subtype) {
		this.subtype = subtype;
	}

	public OccupancyHelperDetailDTO getUsage() {
		return usage;
	}

	public void setUsage(OccupancyHelperDetailDTO usage) {
		this.usage = usage;
	}
}
