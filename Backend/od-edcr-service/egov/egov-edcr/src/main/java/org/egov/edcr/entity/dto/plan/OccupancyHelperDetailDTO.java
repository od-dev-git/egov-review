package org.egov.edcr.entity.dto.plan;

public class OccupancyHelperDetailDTO {
	private Integer color;
	private String code;
	private String name;

	public OccupancyHelperDetailDTO(Integer color, String code, String name) {
		this.color = color;
		this.code = code;
		this.name = name;
	}

	public Integer getColor() {
		return color;
	}

	public void setColor(Integer color) {
		this.color = color;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
