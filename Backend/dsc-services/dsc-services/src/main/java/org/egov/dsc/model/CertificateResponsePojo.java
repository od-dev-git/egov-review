package org.egov.dsc.model;


import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CertificateResponsePojo {

	@JsonProperty("keyId")
    private String keyId;
    
	@JsonProperty("commonName")
    private String commonName;
	
	
	@JsonProperty("certificateDate")
    private String certificateDate;
    

}
