package org.egov.edcr.entity.dto.plan;

import org.egov.common.entity.dcr.helper.OccupancyHelperDetail;

public class OccupancyTypeDTO {
    private OccupancyHelperDetailDTO type;
    private OccupancyHelperDetailDTO subtype;
    private OccupancyHelperDetailDTO usage;
    private OccupancyHelperDetailDTO convertedType;
    private OccupancyHelperDetailDTO convertedSubtype;
    private OccupancyHelperDetailDTO convertedUsage;

    public OccupancyTypeDTO(OccupancyHelperDetailDTO type, OccupancyHelperDetailDTO subtype,
                            OccupancyHelperDetailDTO usage,  OccupancyHelperDetailDTO convertedType,
                            OccupancyHelperDetailDTO convertedSubtype, OccupancyHelperDetailDTO convertedUsage) {
        this.type = type;
        this.subtype = subtype;
        this.usage = usage;
        this.convertedType=convertedType;
        this.convertedSubtype=convertedSubtype;
        this.convertedUsage = convertedUsage;
        
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

	public OccupancyHelperDetailDTO getConvertedType() {
		return convertedType;
	}

	public void setConvertedType(OccupancyHelperDetailDTO convertedType) {
		this.convertedType = convertedType;
	}

	public OccupancyHelperDetailDTO getConvertedSubtype() {
		return convertedSubtype;
	}

	public void setConvertedSubtype(OccupancyHelperDetailDTO convertedSubtype) {
		this.convertedSubtype = convertedSubtype;
	}

	public OccupancyHelperDetailDTO getConvertedUsage() {
		return convertedUsage;
	}

	public void setConvertedUsage(OccupancyHelperDetailDTO convertedUsage) {
		this.convertedUsage = convertedUsage;
	}

}
