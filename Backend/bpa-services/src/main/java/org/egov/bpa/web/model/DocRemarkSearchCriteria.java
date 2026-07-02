package org.egov.bpa.web.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DocRemarkSearchCriteria {
	
	@JsonProperty("id")
	private List<String> ids;

	@JsonProperty("documentCode")
    private List<String> documentCode;

	@JsonProperty("businessId")
	private String businessId;

	@JsonProperty("additionalDetails")
	private Object additionalDetails;

	@JsonProperty("auditDetails")
	private AuditDetails auditDetails;

//	@JsonProperty("isUpdatable")
//	private Boolean isUpdatable;
	
    @JsonProperty("offset")
    private Integer offset;

    @JsonProperty("limit")
    private Integer limit; 
    
    @JsonProperty("fromDate")
    private Long fromDate;

    @JsonProperty("toDate")
    private Long toDate;
	
	
    public boolean isEmpty() {
        return (this.ids == null && this.businessId == null && this.additionalDetails == null &&
                 this.auditDetails == null && this.documentCode == null);
    }

}
