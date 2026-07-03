package org.egov.edcr.contract.oc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Floor {

    @JsonProperty("floorType")
    private String floorType;

    @JsonProperty("floorNumber")
    private String floorNumber;

    @JsonProperty("subOcuupancy")
    private SubOccupancy subOcuupancy;

    @JsonProperty("asBuiltBUA")
    private String asBuiltBUA;

    @JsonProperty("asBuiltFARArea")
    private String asBuiltFARArea;

    @JsonProperty("asBuiltCarpetArea")
    private String asBuiltCarpetArea;
    
    @JsonProperty("approvedBUA")
    private String approvedBUA;

	public String getFloorType() {
		return floorType;
	}

	public void setFloorType(String floorType) {
		this.floorType = floorType;
	}

	public String getFloorNumber() {
		return floorNumber;
	}

	public void setFloorNumber(String floorNumber) {
		this.floorNumber = floorNumber;
	}

	public SubOccupancy getSubOcuupancy() {
		return subOcuupancy;
	}

	public void setSubOcuupancy(SubOccupancy subOcuupancy) {
		this.subOcuupancy = subOcuupancy;
	}

	public String getAsBuiltBUA() {
		return asBuiltBUA;
	}

	public void setAsBuiltBUA(String asBuiltBUA) {
		this.asBuiltBUA = asBuiltBUA;
	}

	public String getAsBuiltFARArea() {
		return asBuiltFARArea;
	}

	public void setAsBuiltFARArea(String asBuiltFARArea) {
		this.asBuiltFARArea = asBuiltFARArea;
	}

	public String getAsBuiltCarpetArea() {
		return asBuiltCarpetArea;
	}

	public void setAsBuiltCarpetArea(String asBuiltCarpetArea) {
		this.asBuiltCarpetArea = asBuiltCarpetArea;
	}
	
	

	public String getApprovedBUA() {
		return approvedBUA;
	}

	public void setApprovedBUA(String approvedBUA) {
		this.approvedBUA = approvedBUA;
	}

	

	public Floor(String floorType, String floorNumber, SubOccupancy subOcuupancy, String asBuiltBUA, String asBuiltFARArea,
			String asBuiltCarpetArea, String approvedBUA) {
		super();
		this.floorType = floorType;
		this.floorNumber = floorNumber;
		this.subOcuupancy = subOcuupancy;
		this.asBuiltBUA = asBuiltBUA;
		this.asBuiltFARArea = asBuiltFARArea;
		this.asBuiltCarpetArea = asBuiltCarpetArea;
		this.approvedBUA = approvedBUA;
	}

	public Floor() {
		super();
	}
    
    

}
