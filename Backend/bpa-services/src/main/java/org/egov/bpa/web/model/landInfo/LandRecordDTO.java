package org.egov.bpa.web.model.landInfo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class LandRecordDTO {
	
    private String districtLgdCode;
    private String tehsilLgdCode;
    private String villageLgdCode;
   
    
    private String khata;
    private String kisam;
    private String tehsil;
    private String village;
    private String district;
    private String plotArea;
    private String plotNumber;
    private String applicantName;
    private String landOwnershipType;
    
    private String districtRevenueCode;
    private String tehsilRevenueCode;
    private String villageRevenueCode;
    private String perAcreBmvValue;
	

}
