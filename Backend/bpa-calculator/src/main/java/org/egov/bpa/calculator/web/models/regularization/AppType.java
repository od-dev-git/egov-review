package org.egov.bpa.calculator.web.models.regularization;


import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum AppType {

	LAND("LAND"),

	BUILDING("BUILDING"),

	LAND_AND_BUILDING("LAND_AND_BULDING");

	private String value;

	AppType(String value) {
		this.value = value;
	}

	@Override
	@JsonValue
	public String toString() {
		return String.valueOf(value);
	}

	@JsonCreator
	public static AppType fromValue(String text) {
		for (AppType s : AppType.values()) {
			if (String.valueOf(s.value).equalsIgnoreCase(text)) {
				return s;
			}
		}
		return null;
	}
}
