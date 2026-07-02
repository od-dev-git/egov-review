package org.egov.collection.web.contract;

import java.util.List;

import org.egov.common.contract.response.ResponseInfo;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Setter
@Getter
@ToString
@AllArgsConstructor
public class RemittanceResponseDepositWorkDetails   {

  @JsonProperty("ResponseInfo")
  private ResponseInfo responseInfo;

  @JsonProperty("RemittanceDepositWorkDetail")
  private List<RemittanceDepositWorkDetail> RemittanceDepositWorkDetail;

}