package org.egov.bpa.web.model;

import java.util.List;
import java.util.Objects;

import javax.validation.Valid;

import org.egov.common.contract.response.ResponseInfo;
import org.springframework.validation.annotation.Validated;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@ApiModel(description = "Contains the ResponseHeader and the created/updated property")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.v3.generators.java.SpringCodegen", date = "2020-06-23T05:52:32.717Z[GMT]")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter

public class BPAApprovedByApplicationResponse {
	
	
	  @JsonProperty("ResponseInfo")
	  private ResponseInfo responseInfo;
	  
	  @JsonProperty("ApprovedBy")
	  private List<BpaApprovedByApplicationSearch> bpaApprovedByApplicationSearch;
	  
	  
	  @ApiModelProperty(value = "")
	  
	    @Valid
	    public ResponseInfo getResponseInfo() {
	    return responseInfo;
	  }

	  public void setResponseInfo(ResponseInfo responseInfo) {
	    this.responseInfo = responseInfo;
	  }
	 
//	  public BPAResponse bpaApprovedByApplicationSearch(
//				List<org.egov.bpa.web.model.BpaApprovedByApplicationSearch> bpas) {
//			// TODO Auto-generated method stub
//			return null;
//		} 
	  public BPAApprovedByApplicationResponse BpaApprovedByApplicationSearch(List<BpaApprovedByApplicationSearch> bpas) {
		  this.bpaApprovedByApplicationSearch=bpas;
		  return this;
	  }
	  
 @ApiModelProperty(value = "")
	  
	  @Override
	  public boolean equals(java.lang.Object o) {
	    if (this == o) {
	      return true;
	    }
	    if (o == null || getClass() != o.getClass()) {
	      return false;
	    }
	    
	    BPAApprovedByApplicationResponse bPAApprovedByApplicationResponse = (BPAApprovedByApplicationResponse) o;
	    return Objects.equals(this.responseInfo,bPAApprovedByApplicationResponse.responseInfo) &&
	    Objects.equals(this.bpaApprovedByApplicationSearch,bPAApprovedByApplicationResponse.bpaApprovedByApplicationSearch);
 }
 
 @Override
 public int hashCode() {
   return Objects.hash(responseInfo, bpaApprovedByApplicationSearch);
 }
 
 public String toString() {
	    StringBuilder sb = new StringBuilder();
	    sb.append("class bpaApprovedByApplicationSearch {\n");
	    
	    sb.append("    responseInfo: ").append(toIndentedString(responseInfo)).append("\n");
	    sb.append("    ApprovedBy: ").append(toIndentedString(bpaApprovedByApplicationSearch)).append("\n");
	    sb.append("}");
	    return sb.toString();
	  }
 private String toIndentedString(java.lang.Object o) {
	    if (o == null) {
	      return "null";
	    }
	    return o.toString().replace("\n", "\n    ");
	  }
 
}
