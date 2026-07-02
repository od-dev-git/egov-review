package org.egov.bpa.web.model.oc;


public enum OccupancyEnum {

	A("Residential"),
	B("Educational"),
	C("Medical/Hospital"),
	D("Assembly"),
	I("Hazardous"),
	E("Office/Business"),
	F("Mercantile/Commercial"),
	H("Storage"),
	S("Socio"),
	G("Industrial");

	private final String value;

	OccupancyEnum(String value) {
		this.value = value;
	}

	public String getValue() {
		return value;
	}

	@Override
	public String toString() {
		return value;
	}

	public static OccupancyEnum fromString(String value) {
		for (OccupancyEnum occupancyEnum : OccupancyEnum.values()) {
			if (occupancyEnum.value.equals(value)) {
				return occupancyEnum;
			}
		}
		throw new IllegalArgumentException("No enum constant for value: " + value);
	}
}
