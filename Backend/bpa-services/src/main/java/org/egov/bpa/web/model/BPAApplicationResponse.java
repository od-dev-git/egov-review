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
public class BPAApplicationResponse {
	
	  @JsonProperty("ResponseInfo")
	  private ResponseInfo responseInfo;
	  
	  @JsonProperty("BpaSearch")
	  private List<BpaApplicationSearch> BpaApplicationSearch;
	  
	  @ApiModelProperty(value = "")
	  
	    @Valid
	    public ResponseInfo getResponseInfo() {
	    return responseInfo;
	  }

	  public void setResponseInfo(ResponseInfo responseInfo) {
	    this.responseInfo = responseInfo;
	  }
	  
	  public BPAApplicationResponse BpaApplicationSearch(List<BpaApplicationSearch> bpas) {
			this.BpaApplicationSearch = bpas;
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
	    BPAApplicationResponse bPAApplicationResponse =	 (BPAApplicationResponse) o ;
	    return Objects.equals(this.responseInfo, bPAApplicationResponse.responseInfo) &&
	            Objects.equals(this.BpaApplicationSearch, bPAApplicationResponse.BpaApplicationSearch);
	  }
	  
	  @Override
	  public int hashCode() {
	    return Objects.hash(responseInfo, BpaApplicationSearch);
	  }
	  
	  public String toString() {
		    StringBuilder sb = new StringBuilder();
		    sb.append("class BPAApplicationResponse {\n");
		    
		    sb.append("    responseInfo: ").append(toIndentedString(responseInfo)).append("\n");
		    sb.append("    BPASearch: ").append(toIndentedString(BpaApplicationSearch)).append("\n");
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
