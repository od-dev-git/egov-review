package org.egov.edcr.contract.oc;

import com.fasterxml.jackson.annotation.JsonProperty;


import java.util.ArrayList;
import java.util.List;



public class OutsideOCDetails {

    @JsonProperty("scrutinyDetails")
    List<ScrutinyDetails> scrutinyDetails=new ArrayList<>();

	public List<ScrutinyDetails> getScrutinyDetails() {
		return scrutinyDetails;
	}

	public void setScrutinyDetails(List<ScrutinyDetails> scrutinyDetails) {
		this.scrutinyDetails = scrutinyDetails;
	}

	public OutsideOCDetails(List<ScrutinyDetails> scrutinyDetails) {
		super();
		this.scrutinyDetails = scrutinyDetails;
	}

	public OutsideOCDetails() {
		super();
	}
    
    


}
