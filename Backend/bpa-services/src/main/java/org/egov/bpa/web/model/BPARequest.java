package org.egov.bpa.web.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import javax.validation.Valid;

import org.egov.common.contract.request.RequestInfo;
import org.springframework.validation.annotation.Validated;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

/**
 * Contract class to receive request. Array of Property items  are used in case of create . Where as single Property item is used for update
 */
@ApiModel(description = "Contract class to receive request. Array of Property items  are used in case of create . Where as single Property item is used for update")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.v3.generators.java.SpringCodegen", date = "2020-06-23T05:52:32.717Z[GMT]")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BPARequest   {
  @JsonProperty("RequestInfo")
  private RequestInfo requestInfo = null;

  @JsonProperty("BPA")
  private BPA BPA = null;
  
  @JsonProperty("revision")
  private Revision revision = null;
  
  @JsonProperty("revalidation")
  private Revalidation revalidation = null;
  
  @Builder.Default
  private Map<String, String> edcrResponse=null;

  public BPARequest (RequestInfo requestInfo, BPA bpa) {
	this.requestInfo = requestInfo;
	this.BPA = bpa;
	this.revision = null;
  }
  
  public BPARequest (RequestInfo requestInfo, BPA bpa, Revalidation revalidation) {
		this.requestInfo = requestInfo;
		this.BPA = bpa;
		this.revision = null;
		this.revalidation = revalidation;
	  }
  
  public BPARequest requestInfo(RequestInfo requestInfo) {
    this.requestInfo = requestInfo;
    return this;
  }

  /**
   * Get requestInfo
   * @return requestInfo
  **/
  @ApiModelProperty(value = "")
  
    @Valid
    public RequestInfo getRequestInfo() {
    return requestInfo;
  }

  public void setRequestInfo(RequestInfo requestInfo) {
    this.requestInfo = requestInfo;
  }

  public BPARequest BPA(BPA BPA) {
    this.BPA = BPA;
    return this;
  }
  
  public BPARequest revision(Revision revision) {
	this.revision = revision;
	return this;
  }
  
  public BPARequest revalidation(Revalidation revalidation) {
		this.revalidation = revalidation;
		return this;
	  }

  /**
   * Get BPA
   * @return BPA
  **/
  @ApiModelProperty(value = "")
  
    @Valid
    public BPA getBPA() {
    return BPA;
  }

  public void setBPA(BPA BPA) {
    this.BPA = BPA;
  }

  /**
   * Get Revision
   * @return Revision
  **/
  @ApiModelProperty(value = "")
  
    @Valid
    public Revision getRevision() {
    return revision;
  }

  public void setRevision(Revision revision) {
    this.revision = revision;
  }
  
  /**
   * Get edcr details
   * @return
   */
  public Map<String, String> getEdcrResponse() {
	if(edcrResponse==null)
		edcrResponse=new HashMap<>();
	return edcrResponse;
  }
  public void setEdcrResponse(Map<String, String> edcrResponse) {
	this.edcrResponse = edcrResponse;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    BPARequest bpARequest = (BPARequest) o;
    return Objects.equals(this.requestInfo, bpARequest.requestInfo) &&
        Objects.equals(this.BPA, bpARequest.BPA) &&
        Objects.equals(this.revision, bpARequest.revision)&&
    Objects.equals(this.revalidation, bpARequest.revalidation);
  }

  @Override
  public int hashCode() {
    return Objects.hash(requestInfo, BPA, revision, revalidation);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class BPARequest {\n");
    
    sb.append("    requestInfo: ").append(toIndentedString(requestInfo)).append("\n");
    sb.append("    BPA: ").append(toIndentedString(BPA)).append("\n");
    sb.append("    revision: ").append(toIndentedString(revision)).append("\n");
    sb.append("    revalidation: ").append(toIndentedString(revalidation)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(java.lang.Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }

public Revalidation getRevalidation() {
	return revalidation;
}

public void setRevalidation(Revalidation revalidation) {
	this.revalidation = revalidation;
}
}
